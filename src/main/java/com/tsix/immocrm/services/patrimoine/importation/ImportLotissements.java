package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.config.audit.EtatAudite;
import com.tsix.immocrm.dto.patrimoine.LotissementEcritureDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto.ResultatImport;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.ChampImport;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.LigneImport;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.LigneLue;
import com.tsix.immocrm.services.patrimoine.terrain.LotissementService;
import com.tsix.immocrm.services.patrimoine.terrain.LotissementsVisibles;
import com.tsix.immocrm.web.export.FeuilleExport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.NATURE;
import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.PIVOT;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.LOTISSEMENTS;
import static com.tsix.immocrm.services.patrimoine.importation.LectureChamps.texte;

/**
 * Import de lots dans la tranche choisie ({@code ImportLotLand}, FEATURE-patrimoine-lotissements.md §1.5) : pivot = code
 * dans la tranche, écrit par {@link LotissementService} ; un pivot qui désigne un article de la tranche est refusé.
 */
@Component
class ImportLotissements implements ImportParType<ImportLotissements.Contexte> {

    private static final String DE_LA_TRANCHE = "select i from StkItem i where i.stkHierarchy.id = :tranche";
    private static final String TRANCHE = "tranche";
    private static final String CODE = "code";
    private static final Libelles LIBELLES = new Libelles("Création du lot : %s", "Mise à jour du lot : %s",
            "Le lot : %s n'a pas été modifié", "Problèmes d'import du lot : %s");

    private final LotissementService lotissements;
    private final LotissementsVisibles lots;
    private final EntityManager em;
    private final EtatAudite etat;
    private final JsonMapper json;

    ImportLotissements(LotissementService lotissements, LotissementsVisibles lots, EntityManager em, EtatAudite etat,
                       JsonMapper json) {
        this.lotissements = lotissements;
        this.lots = lots;
        this.em = em;
        this.etat = etat;
        this.json = json;
    }

    record Contexte(CfgTranche tranche, ResolutionCodes codes, Set<String> entetes, List<String> avertissements) {
    }

    record LigneLot(int numero, String pivot, Map<String, Object> champs, List<String> erreurs) implements LigneImport {

        @Override
        public String code() {
            Object code = champs.get(CODE);
            return code == null ? pivot : code.toString();
        }
    }

    @Override
    public Set<String> types() {
        return Set.of(LOTISSEMENTS);
    }

    @Override
    public boolean trancheExigee() {
        return true;
    }

    @Override
    public Contexte contexte(String type, CfgTranche tranche, Set<String> entetes, List<String> avertissements) {
        return new Contexte(tranche, new ResolutionCodes(em, tranche.getId()), entetes, avertissements);
    }

    @Override
    public Set<String> entetesConnus(Contexte ctx) {
        return ChampsLotissement.CHAMPS.stream().map(ChampImport::entete).collect(Collectors.toSet());
    }

    @Override
    public List<LigneBilanDto> ecrire(List<LigneLue> lignes, Contexte ctx) {
        return EcritureParLigne.ecrire(em, lignes.stream().map(l -> traduire(l, ctx)).toList(), l -> ecrireLigne(l, ctx),
                LIBELLES);
    }

    /** OMRAN {@code importLotLand.ListItems} : les lots de la tranche. */
    @Override
    public Modele modele(String type, CfgTranche tranche) {
        List<Map<String, Object>> lignes = em.createQuery(DE_LA_TRANCHE + " order by i.code", StkItem.class)
                .setParameter(TRANCHE, tranche.getId()).getResultList().stream().filter(lots.lots())
                .map(i -> ModeleImport.ligne(i, List.of(), ChampsLotissement.CHAMPS)).toList();
        String nom = tranche.getCode() + "_LOTISSEMENTS";
        List<FeuilleExport> feuilles = new ArrayList<>();
        feuilles.add(ModeleImport.donnees(nom, ChampsLotissement.CHAMPS, lignes));
        feuilles.addAll(ModeleImport.aides(ChampsLotissement.CHAMPS, new ResolutionCodes(em, tranche.getId())));
        return new Modele(nom, feuilles);
    }

    private LigneLot traduire(LigneLue l, Contexte ctx) {
        Map<String, Object> v = l.valeurs();
        List<String> erreurs = new ArrayList<>();
        Map<String, Object> champs = Objects.requireNonNullElseGet(
                LectureChamps.lire(ChampsLotissement.CHAMPS, l, ctx.codes(), ctx.avertissements(), erreurs).get(0),
                HashMap::new);
        String pivot = texte(v.get(PIVOT));
        if (pivot == null) {
            erreurs.add("Manque le pivot");
        }
        ImportArticles.controlerTranche(v, ctx.tranche(), ctx.entetes(), erreurs);
        LectureChamps.controlerNature(v.get(NATURE), ctx.codes(), "TER", "Terrain", erreurs);
        return new LigneLot(l.numero(), pivot, champs, erreurs);
    }

    private ResultatImport ecrireLigne(LigneLot l, Contexte ctx) {
        List<StkItem> trouves = em.createQuery(DE_LA_TRANCHE + " and i.code = :code", StkItem.class)
                .setFlushMode(FlushModeType.COMMIT).setParameter(TRANCHE, ctx.tranche().getId())
                .setParameter(CODE, l.pivot()).getResultList();
        if (trouves.size() > 1) {
            throw new InvalidRequestException("Il existe plusieurs lots avec le même code");
        }
        Map<String, Object> champs = new HashMap<>(l.champs());
        if (trouves.isEmpty()) {
            exiger(champs, "vocationId", "Manque le code de la vocation");
            exiger(champs, "categorieId", "Manque le code de la catégorie");
            exiger(champs, "typeId", "Manque le code du type");
            champs.put("trancheId", ctx.tranche().getId());
            champs.putIfAbsent(CODE, l.pivot());
            lotissements.creer(dto(champs));
            return ResultatImport.CREATION;
        }
        StkItem lot = trouves.get(0);
        if (!lots.estLot(lot)) {
            throw new InvalidRequestException("Ce code désigne un article : utilisez l'import des articles");
        }
        Object[] avant = etat.etat(lot);
        lotissements.modifier(lot.getId(), dto(champs));
        return etat.modifie(lot, avant) ? ResultatImport.MISE_A_JOUR : ResultatImport.INCHANGEE;
    }

    private static void exiger(Map<String, Object> champs, String champ, String message) {
        if (champs.get(champ) == null) {
            throw new InvalidRequestException(message);
        }
    }

    private LotissementEcritureDto dto(Map<String, Object> champs) {
        return json.convertValue(champs, LotissementEcritureDto.class);
    }
}
