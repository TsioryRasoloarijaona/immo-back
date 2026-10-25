package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.domain.ReferentielAdmin;
import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto.ResultatImport;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkContenance;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.LigneImport;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.LigneLue;
import com.tsix.immocrm.services.patrimoine.importation.ModeleImport.Colonne;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import com.tsix.immocrm.web.export.FeuilleExport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.PRESENTATIONS;
import static com.tsix.immocrm.services.patrimoine.importation.LectureChamps.texte;

/**
 * Import des types de présentation, format {@code TYPES_PRESENTATION} (FEATURE-patrimoine-import-autres.md §3.1) :
 * rapprochement (tranche, code), écriture par {@link ReferentielAdminService} avec la liste complète des contenances.
 */
@Component
class ImportPresentations implements ImportParType<ImportPresentations.Contexte> {

    private static final String SLUG = ReferentielAdmin.TYPES_PRESENTATION.slug();
    private static final String TRANCHE = "Tranche";
    private static final String CODE = "Code";
    private static final String DESCRIPTION = "Description";
    private static final String CHAMP_CODE = "code";
    private static final String CHAMP_DESCRIPTION = "description";
    private static final String CONTENANCES = "contenances";
    private static final String CONTENANCE_ID = "contenanceId";
    private static final String NOMBRE = "nombre";
    private static final String PAR_CODE = "select p.id from StkPresentationType p where p.stkHierarchy.id = :tranche "
            + "and p.code = :code";
    private static final String TRANCHES_VISIBLES = "SELECT t FROM CfgTranche t LEFT JOIN t.cfgProject pr "
            + "LEFT JOIN pr.cfgCompany soc WHERE " + VisibiliteEntites.CLAUSE_JPQL + " ORDER BY t.code ASC";
    private static final Libelles LIBELLES = new Libelles("Création : %s", "Mise à jour : %s",
            "Pas de modification : %s", "Problèmes dimport : %s");

    private final ReferentielAdminService referentiels;
    private final EntityManager em;

    ImportPresentations(ReferentielAdminService referentiels, EntityManager em) {
        this.referentiels = referentiels;
        this.em = em;
    }

    /** {@code colonnes} : en-tête du fichier → contenance de ce libellé. */
    record Contexte(List<CfgTranche> tranches, Map<String, StkContenance> colonnes) {
    }

    /** {@code nombres} : contenance → nombre lu (0 = cellule vide), pour les seules colonnes présentes. */
    record LignePresentation(int numero, String pivot, UUID trancheId, String description, Map<UUID, Integer> nombres,
                             List<String> erreurs) implements LigneImport {

        @Override
        public String code() {
            return pivot;
        }

        @Override
        public String cle() {
            return trancheId + "|" + pivot.toLowerCase(Locale.ROOT);
        }
    }

    // ------------------------------------------------------------------ ImportParType

    @Override
    public Set<String> types() {
        return Set.of(PRESENTATIONS);
    }

    @Override
    public boolean trancheExigee() {
        return false;
    }

    @Override
    public Contexte contexte(String type, CfgTranche tranche, Set<String> entetes, List<String> avertissements) {
        Map<String, StkContenance> colonnes = new LinkedHashMap<>();
        List<StkContenance> contenances = contenances();
        entetes.forEach(e -> contenances.stream().filter(c -> e.equalsIgnoreCase(c.getDescription())).findFirst()
                .ifPresent(c -> colonnes.put(e, c)));
        return new Contexte(tranchesVisibles(), colonnes);
    }

    @Override
    public Set<String> entetesConnus(Contexte ctx) {
        return Stream.concat(Stream.of(TRANCHE, CODE, DESCRIPTION), ctx.colonnes().keySet().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public List<LigneBilanDto> ecrire(List<LigneLue> lignes, Contexte ctx) {
        return EcritureParLigne.ecrire(em, lignes.stream().map(l -> traduire(l, ctx)).toList(), this::ecrireLigne,
                LIBELLES);
    }

    /** Présentations des tranches visibles, une colonne par contenance ; feuille PROJETS = tranches visibles. */
    @Override
    public Modele modele(String type, CfgTranche tranche) {
        List<CfgTranche> visibles = tranchesVisibles();
        Map<Object, String> codes = new HashMap<>();
        visibles.forEach(t -> codes.put(t.getId(), t.getCode()));
        List<StkContenance> contenances = contenances();
        List<Colonne> colonnes = Stream.concat(
                Stream.of(TRANCHE, CODE, DESCRIPTION).map(nom -> new Colonne(nom, TypeExport.TEXTE)),
                contenances.stream().map(StkContenance::getDescription).filter(Objects::nonNull).distinct()
                        .map(nom -> new Colonne(nom, TypeExport.NOMBRE)))
                .toList();
        List<Map<String, Object>> lignes = referentiels.lignes(SLUG, null, null, false).stream()
                .map(p -> ligneDuModele(p, codes, contenances)).toList();
        return new Modele("TYPES_PRESENTATION", List.of(new FeuilleExport("TYPES_PRESENTATION", colonnes, lignes, null),
                ModeleImport.aide("PROJETS", visibles)));
    }

    private static Map<String, Object> ligneDuModele(Map<String, Object> p, Map<Object, String> codes,
                                                     List<StkContenance> contenances) {
        Map<String, Object> ligne = new HashMap<>();
        ligne.put(TRANCHE, codes.get(p.get("trancheId")));
        ligne.put(CODE, p.get(CHAMP_CODE));
        ligne.put(DESCRIPTION, p.get(CHAMP_DESCRIPTION));
        Map<Object, Object> nombres = nombres(enfants(p));
        contenances.stream().filter(c -> c.getDescription() != null && nombres.get(c.getId()) != null)
                .forEach(c -> ligne.putIfAbsent(c.getDescription(), nombres.get(c.getId())));
        return ligne;
    }

    private List<StkContenance> contenances() {
        return em.createQuery("select c from StkContenance c order by c.code asc", StkContenance.class).getResultList();
    }

    private List<CfgTranche> tranchesVisibles() {
        return em.createQuery(TRANCHES_VISIBLES, CfgTranche.class).getResultList();
    }

    // ------------------------------------------------------------------ traduction (messages de PresentationType.ImportVerif)

    private static LignePresentation traduire(LigneLue l, Contexte ctx) {
        Map<String, Object> v = l.valeurs();
        List<String> erreurs = new ArrayList<>();
        String tranche = texte(v.get(TRANCHE));
        String code = texte(v.get(CODE));
        UUID trancheId = tranche == null ? null : ctx.tranches().stream()
                .filter(t -> tranche.equalsIgnoreCase(t.getCode()) || tranche.equalsIgnoreCase(t.getDescription()))
                .map(CfgTranche::getId).findFirst().orElse(null);
        if (tranche == null) {
            erreurs.add("Manque le tranche");
        } else if (trancheId == null) {
            erreurs.add("La tranche saisie est introuvable dans notre référenciel");
        }
        if (code == null) {
            erreurs.add("Manque le code");
        } else if (trancheId == null) {
            erreurs.add("Il est impossible de vérifier le code si la tranche n'est pas vérifiée");
        }
        Map<UUID, Integer> nombres = new HashMap<>();
        ctx.colonnes().forEach((entete, c) -> {
            Object brut = v.get(entete);
            try {
                nombres.put(c.getId(), brut == null ? 0 : (Integer) TypeCellule.ENTIER.lire(brut));
            } catch (TypeCellule.Illisible e) {
                erreurs.add(LectureChamps.illisible(entete, brut));
            }
        });
        return new LignePresentation(l.numero(), code, trancheId, texte(v.get(DESCRIPTION)), nombres, erreurs);
    }

    // ------------------------------------------------------------------ écriture (dans la transaction de l'import)

    private ResultatImport ecrireLigne(LignePresentation l) {
        List<UUID> ids = em.createQuery(PAR_CODE, UUID.class).setFlushMode(FlushModeType.COMMIT)
                .setParameter("tranche", l.trancheId()).setParameter(CHAMP_CODE, l.pivot()).getResultList();
        if (ids.size() > 1) {
            throw new InvalidRequestException("Il existe plusieurs enregistrement avec le même code");
        }
        if (ids.isEmpty()) {
            if (l.description() == null) {
                throw new InvalidRequestException("Manque la description");
            }
            Map<String, Object> corps = new HashMap<>(Map.of("trancheId", l.trancheId(), CHAMP_CODE, l.pivot(),
                    CHAMP_DESCRIPTION, l.description(), CONTENANCES, contenances(List.of(), l.nombres())));
            referentiels.creer(SLUG, corps);
            return ResultatImport.CREATION;
        }
        Map<String, Object> actuelle = referentiels.ligne(SLUG, ids.get(0));
        List<Map<String, Object>> avant = enfants(actuelle);
        List<Map<String, Object>> apres = contenances(avant, l.nombres());
        Map<String, Object> corps = new HashMap<>();
        if (l.description() != null && !l.description().equals(actuelle.get(CHAMP_DESCRIPTION))) {
            corps.put(CHAMP_DESCRIPTION, l.description());
        }
        if (!nombres(avant).equals(nombres(apres))) {
            corps.put(CONTENANCES, apres);
        }
        if (corps.isEmpty()) {
            // Appeler le service recréerait les contenances (et leurs traces d'audit) sans rien changer.
            return ResultatImport.INCHANGEE;
        }
        referentiels.modifier(SLUG, ids.get(0), corps);
        return ResultatImport.MISE_A_JOUR;
    }

    /** Liste complète : contenances hors fichier gardées, nombre &gt; 0 du fichier posé (autres champs gardés), 0 retirée. */
    private static List<Map<String, Object>> contenances(List<Map<String, Object>> avant, Map<UUID, Integer> nombres) {
        Map<Object, Map<String, Object>> parContenance = avant.stream()
                .collect(Collectors.toMap(c -> c.get(CONTENANCE_ID), HashMap::new, (a, b) -> a, LinkedHashMap::new));
        nombres.forEach((id, n) -> {
            if (n > 0) {
                Map<String, Object> c = parContenance.computeIfAbsent(id, k -> new HashMap<>());
                c.put(CONTENANCE_ID, id);
                c.put(NOMBRE, n);
            } else {
                parContenance.remove(id);
            }
        });
        return new ArrayList<>(parContenance.values());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> enfants(Map<String, Object> presentation) {
        return (List<Map<String, Object>>) presentation.getOrDefault(CONTENANCES, List.of());
    }

    private static Map<Object, Object> nombres(List<Map<String, Object>> contenances) {
        Map<Object, Object> nombres = new HashMap<>();
        contenances.forEach(c -> nombres.put(c.get(CONTENANCE_ID), c.get(NOMBRE)));
        return nombres;
    }
}
