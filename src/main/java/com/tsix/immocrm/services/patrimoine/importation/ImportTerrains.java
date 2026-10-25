package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.config.audit.EtatAudite;
import com.tsix.immocrm.dto.patrimoine.FiltresTerrains;
import com.tsix.immocrm.dto.patrimoine.TerrainEcritureDto;
import com.tsix.immocrm.dto.patrimoine.TerrainListeDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto.ResultatImport;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.ChampImport;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.LigneImport;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.LigneLue;
import com.tsix.immocrm.services.patrimoine.terrain.TerrainService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.web.export.FeuilleExport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.NATURE;
import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.PIVOT;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.TERRAINS;
import static com.tsix.immocrm.services.patrimoine.importation.LectureChamps.texte;

/**
 * Import des terrains, format {@code STOCK_TERRAINS} (FEATURE-patrimoine-import-autres.md §3.2) : pivot = code d'un
 * article de l'entité de la ligne, écrit par {@link TerrainService} ; un pivot qui n'est pas un terrain est refusé.
 */
@Component
class ImportTerrains implements ImportParType<ImportTerrains.Contexte> {

    private static final String NATURE_TERRAIN = "TER";
    private static final String CODE = "code";
    private static final String ENTITE_ID = "entiteId";
    private static final String FEUILLE = "STOCK_TERRAINS";
    private static final int MODELE_MAX = LectureClasseur.LIGNES_MAX;
    private static final String PAR_CODE = "select i from StkItem i where i.cfgCompany.id = :entite and i.code = :code";
    private static final Libelles LIBELLES = new Libelles("Création du terrain : %s", "Mise à jour du terrain : %s",
            "le terrain : %s n'a pas été modifié", "Problèmes d'import du terrain : %s");
    /** Références hors {@link ResolutionCodes} : code ou libellé (collation sans casse), catégorie et type de nature Terrain. */
    private static final Map<String, Requete> PAR_REQUETE = Map.of(
            ChampsTerrain.PAYS_ID, new Requete("select p.id from AdmCountry p where p.code = :v or p.description = :v "
                    + "order by p.code", "Le pays du tier est introuvable"),
            ChampsTerrain.VILLE_ID, new Requete("select v.id from AdmCity v where v.code = :v or v.description = :v "
                    + "order by v.code", "La ville du tier est introuvable"),
            ChampsTerrain.CATEGORIE_ID, new Requete("select c.id from StkItemCategory c where (c.code = :v or "
                    + "c.description = :v) and c.stkKind.code = 'TER' order by c.code", null),
            ChampsTerrain.TYPE_ID, new Requete("select t.id from StkItemType t where (t.code = :v or "
                    + "t.description = :v) and t.stkKind.code = 'TER' order by t.code", null));

    private final TerrainService terrains;
    private final EntityManager em;
    private final EtatAudite etat;
    private final JsonMapper json;

    ImportTerrains(TerrainService terrains, EntityManager em, EtatAudite etat, JsonMapper json) {
        this.terrains = terrains;
        this.em = em;
        this.etat = etat;
        this.json = json;
    }

    /** {@code introuvable} nul = message générique du code inconnu. */
    private record Requete(String jpql, String introuvable) {
    }

    record Contexte(ResolutionCodes codes, List<String> avertissements, Map<String, Optional<UUID>> resolues) {
    }

    record LigneTerrain(int numero, String pivot, Map<String, Object> champs, boolean natureLue, List<String> erreurs)
            implements LigneImport {

        UUID entiteId() {
            return (UUID) champs.get(ENTITE_ID);
        }

        @Override
        public String code() {
            Object code = champs.get(CODE);
            return code == null ? pivot : code.toString();
        }

        @Override
        public String cle() {
            return pivot == null ? null : entiteId() + "|" + pivot.toLowerCase(Locale.ROOT);
        }
    }

    // ------------------------------------------------------------------ ImportParType

    @Override
    public Set<String> types() {
        return Set.of(TERRAINS);
    }

    @Override
    public boolean trancheExigee() {
        return false;
    }

    @Override
    public Contexte contexte(String type, CfgTranche tranche, Set<String> entetes, List<String> avertissements) {
        return new Contexte(new ResolutionCodes(em, null), avertissements, new HashMap<>());
    }

    @Override
    public Set<String> entetesConnus(Contexte ctx) {
        return ChampsTerrain.CHAMPS.stream().map(ChampImport::entete).collect(Collectors.toSet());
    }

    @Override
    public List<LigneBilanDto> ecrire(List<LigneLue> lignes, Contexte ctx) {
        return EcritureParLigne.ecrire(em, lignes.stream().map(l -> traduire(l, ctx)).toList(), this::ecrireLigne,
                LIBELLES);
    }

    /** Terrains visibles de la liste « terrains » ; au-delà de 1 000, en-têtes seuls (le fichier ne serait pas réimportable). */
    @Override
    public Modele modele(String type, CfgTranche tranche) {
        Page<TerrainListeDto> visibles = terrains.lister(new FiltresTerrains(null, null, null, null), false,
                PageRequest.of(0, MODELE_MAX + 1, Sort.by(CODE)));
        List<UUID> ids = visibles.getTotalElements() > MODELE_MAX ? List.of()
                : visibles.stream().map(TerrainListeDto::id).toList();
        List<Map<String, Object>> lignes = ids.isEmpty() ? List.of()
                : em.createQuery("select i from StkItem i where i.id in :ids order by i.code", StkItem.class)
                .setParameter("ids", ids).getResultList().stream()
                .map(i -> ModeleImport.ligne(i, List.of(), ChampsTerrain.CHAMPS)).toList();
        List<FeuilleExport> feuilles = new ArrayList<>();
        feuilles.add(ModeleImport.donnees(FEUILLE, ChampsTerrain.CHAMPS, lignes));
        feuilles.addAll(ModeleImport.aides(ChampsTerrain.CHAMPS, new ResolutionCodes(em, null)));
        return new Modele(FEUILLE, feuilles);
    }

    // ------------------------------------------------------------------ traduction

    private LigneTerrain traduire(LigneLue l, Contexte ctx) {
        Map<String, Object> v = l.valeurs();
        List<String> erreurs = new ArrayList<>();
        Map<String, Object> champs = Objects.requireNonNullElseGet(
                LectureChamps.lire(ChampsTerrain.CHAMPS, l, ctx.codes(), ctx.avertissements(), erreurs).get(0),
                HashMap::new);
        PAR_REQUETE.forEach((champ, r) -> {
            if (champs.get(champ) instanceof String saisi) {
                Optional<UUID> id = resoudre(ctx, r, saisi);
                id.ifPresentOrElse(i -> champs.put(champ, i), () -> {
                    champs.remove(champ);
                    erreurs.add(r.introuvable() == null ? LectureChamps.introuvable(saisi) : r.introuvable());
                });
            }
        });
        String pivot = v.containsKey(PIVOT) ? texte(v.get(PIVOT)) : (String) champs.get(CODE);
        if (pivot == null) {
            erreurs.add("Manque le code de l'article");
        }
        boolean natureLue = LectureChamps.controlerNature(v.get(NATURE), ctx.codes(), NATURE_TERRAIN, "Terrain", erreurs);
        return new LigneTerrain(l.numero(), pivot, champs, natureLue, erreurs);
    }

    private Optional<UUID> resoudre(Contexte ctx, Requete r, String saisi) {
        return ctx.resolues().computeIfAbsent(r.jpql() + "|" + saisi.toLowerCase(Locale.ROOT),
                k -> em.createQuery(r.jpql(), UUID.class).setFlushMode(FlushModeType.COMMIT).setParameter("v", saisi)
                        .setMaxResults(1).getResultList().stream().findFirst());
    }

    // ------------------------------------------------------------------ écriture (dans la transaction de l'import)

    private ResultatImport ecrireLigne(LigneTerrain l) {
        // Une entité inactive est invisible (R4) : même refus que si son code n'existait pas.
        if (l.entiteId() != null && !Boolean.TRUE.equals(em.find(CfgCompany.class, l.entiteId()).getIsActive())) {
            throw new InvalidRequestException("L'entité saisie est introuvable dans notre référenciel");
        }
        List<StkItem> trouves = l.entiteId() == null ? List.of()
                : em.createQuery(PAR_CODE, StkItem.class).setFlushMode(FlushModeType.COMMIT)
                .setParameter("entite", l.entiteId()).setParameter(CODE, l.pivot()).getResultList();
        if (trouves.size() > 1) {
            throw new InvalidRequestException("Il existe plusieurs article avec le même code");
        }
        if (trouves.isEmpty()) {
            return creer(l);
        }
        StkItem terrain = trouves.get(0);
        if (!NATURE_TERRAIN.equalsIgnoreCase(Champs.de(terrain.getStkKind(), StkKind::getCode))) {
            throw new InvalidRequestException("L'article " + l.pivot() + " de l'entité n'est pas un terrain : seuls les "
                    + "terrains s'importent ici.");
        }
        if (ArticleService.estOccupe(terrain)) {
            throw new InvalidRequestException("Pas de mise à jour pour un article ayant un dossier de vente");
        }
        Object[] avant = etat.etat(terrain);
        terrains.modifier(terrain.getId(), dto(l.champs()));
        return etat.modifie(terrain, avant) ? ResultatImport.MISE_A_JOUR : ResultatImport.INCHANGEE;
    }

    /** Messages OMRAN d'{@code ImportLands.ImportVerif} : ils ne valent qu'en création (une mise à jour garde les champs). */
    private ResultatImport creer(LigneTerrain l) {
        Map<String, Object> champs = new HashMap<>(l.champs());
        exiger(l.natureLue(), "Manque le code de la nature");
        exiger(champs.get(ENTITE_ID) != null, "Manque le code de l'entité");
        exiger(champs.get("vocationId") != null, "Manque le code de la vocation");
        exiger(champs.get(ChampsTerrain.CATEGORIE_ID) != null, "Manque le code de la catégorie");
        exiger(champs.get(ChampsTerrain.TYPE_ID) != null, "Manque le code du type");
        champs.putIfAbsent(CODE, l.pivot());
        terrains.creer(dto(champs));
        return ResultatImport.CREATION;
    }

    private static void exiger(boolean present, String message) {
        if (!present) {
            throw new InvalidRequestException(message);
        }
    }

    private TerrainEcritureDto dto(Map<String, Object> champs) {
        return json.convertValue(champs, TerrainEcritureDto.class);
    }
}
