package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.dto.patrimoine.LignePrevisionDto;
import com.tsix.immocrm.dto.patrimoine.PrevisionsBaseDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto.ResultatImport;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.PrevisionsBaseService;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.ChampImport;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.Portee;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.LigneLue;
import com.tsix.immocrm.services.patrimoine.importation.ModeleImport.Colonne;
import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.web.export.FeuilleExport;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.PREVISIONS;
import static com.tsix.immocrm.services.patrimoine.importation.TypeCellule.DATE;
import static com.tsix.immocrm.services.patrimoine.importation.TypeCellule.ENTIER;
import static com.tsix.immocrm.services.patrimoine.importation.TypeCellule.NOMBRE;
import static com.tsix.immocrm.services.patrimoine.importation.TypeCellule.TEXTE;
import static com.tsix.immocrm.services.patrimoine.importation.TypeCellule.TVA;

/**
 * Import des prévisions de base de la tranche (FEATURE-patrimoine-import-autres.md §3.3) : « remplace tout » par un seul
 * {@link PrevisionsBaseService#enregistrerPrevisions}, l'en-tête existant surchargé par les colonnes de la 1re ligne.
 */
@Component
class ImportPrevisions implements ImportParType<ImportPrevisions.Contexte> {

    private static final String FEUILLE = "PREVISIONS";
    private static final int LIGNE = 0;
    private static final int EN_TETE = 1;
    private static final String NOMBRE_ARTICLES = "nombreArticles";
    private static final int NOTES_MAX = 1000;
    /** Rang {@link #LIGNE} = colonnes d'une ligne mensuelle ; {@link #EN_TETE} = en-tête, lu sur la 1re ligne seule. */
    static final List<ChampImport> CHAMPS = List.of(
            champ("Année", "annee", ENTIER, LIGNE), champ("Mois", "mois", ENTIER, LIGNE),
            champ("Quantité concrétisation", "quantiteConcretisation", ENTIER, LIGNE),
            champ("Quantité finalisation", "quantiteFinalisation", ENTIER, LIGNE), champ("Notes", "notes", TEXTE, LIGNE),
            champ("Nombre d'articles", NOMBRE_ARTICLES, ENTIER, EN_TETE),
            champ("% concrétisation", "pourcentageConcretisation", TVA, EN_TETE),
            champ("Début concrétisation", "debutConcretisation", DATE, EN_TETE),
            champ("Fin concrétisation", "finConcretisation", DATE, EN_TETE),
            champ("Début finalisation", "debutFinalisation", DATE, EN_TETE),
            champ("Fin finalisation", "finFinalisation", DATE, EN_TETE),
            champ("Écart type concrétisation", "ecartTypeConcretisation", NOMBRE, EN_TETE),
            champ("Écart type finalisation", "ecartTypeFinalisation", NOMBRE, EN_TETE));
    private static final Libelles LIBELLES = new Libelles("Création des prévisions de la tranche : %s",
            "Mise à jour des prévisions de la tranche : %s", "%s", "Problèmes d'import des prévisions de la tranche : %s");

    private final PrevisionsBaseService previsions;
    private final EntityManager em;
    private final JsonMapper json;

    ImportPrevisions(PrevisionsBaseService previsions, EntityManager em, JsonMapper json) {
        this.previsions = previsions;
        this.em = em;
        this.json = json;
    }

    record Contexte(CfgTranche tranche, List<String> avertissements) {
    }

    private static ChampImport champ(String entete, String champ, TypeCellule type, int rang) {
        return new ChampImport(entete, champ, type, null, null, Portee.TOUS, rang);
    }

    // ------------------------------------------------------------------ ImportParType

    @Override
    public Set<String> types() {
        return Set.of(PREVISIONS);
    }

    @Override
    public boolean trancheExigee() {
        return true;
    }

    @Override
    public Contexte contexte(String type, CfgTranche tranche, Set<String> entetes, List<String> avertissements) {
        return new Contexte(tranche, avertissements);
    }

    @Override
    public Set<String> entetesConnus(Contexte ctx) {
        return CHAMPS.stream().map(ChampImport::entete).collect(Collectors.toSet());
    }

    /** Erreurs de lecture par ligne Excel, puis une ligne globale (pivot = code de la tranche, n° 1 = en-têtes). */
    @Override
    public List<LigneBilanDto> ecrire(List<LigneLue> lignes, Contexte ctx) {
        List<LigneBilanDto> bilan = new ArrayList<>();
        List<LignePrevisionDto> mensuelles = new ArrayList<>();
        Map<String, Object> enTete = new HashMap<>();
        for (int i = 0; i < lignes.size(); i++) {
            LigneLue l = lignes.get(i);
            List<String> erreurs = new ArrayList<>();
            Map<Integer, Map<String, Object>> lus = LectureChamps.lire(CHAMPS, l, null, ctx.avertissements(), erreurs);
            // CfgPrevision.Notes est borné par @Size : sans ce contrôle, le flush lèverait une validation hors bilan (500).
            if (lus.getOrDefault(LIGNE, Map.of()).get("notes") instanceof String notes && notes.length() > NOTES_MAX) {
                erreurs.add("Colonne Notes : " + NOTES_MAX + " caractères au plus");
            }
            if (i == 0) {
                enTete.putAll(lus.getOrDefault(EN_TETE, Map.of()));
            }
            if (erreurs.isEmpty()) {
                mensuelles.add(json.convertValue(lus.getOrDefault(LIGNE, Map.of()), LignePrevisionDto.class));
            } else {
                bilan.add(new LigneBilanDto(l.numero(), null, null, ResultatImport.ERREUR,
                        "Problèmes d'import de la ligne : " + String.join(" ; ", erreurs)));
            }
        }
        String code = ctx.tranche().getCode();
        if (!bilan.isEmpty()) {
            bilan.add(new LigneBilanDto(1, code, code, ResultatImport.NON_VERIFIEE, "Non vérifiée : erreurs de lecture"));
            return bilan;
        }
        PrevisionsBaseDto actuelles = previsions.previsions(ctx.tranche().getId());
        ResultatImport resultat = actuelles.lignes().isEmpty() ? ResultatImport.CREATION : ResultatImport.MISE_A_JOUR;
        try {
            previsions.enregistrerPrevisions(ctx.tranche().getId(), corps(actuelles, enTete, mensuelles, ctx));
            em.flush();
            bilan.add(new LigneBilanDto(1, code, code, resultat, LIBELLES.message(resultat, code)));
        } catch (InvalidRequestException e) {
            em.clear();
            bilan.add(new LigneBilanDto(1, code, code, ResultatImport.ERREUR,
                    LIBELLES.message(ResultatImport.ERREUR, code) + " : " + e.getMessage()));
        }
        return bilan;
    }

    /** En-tête existant, cellule vide = intact ; nombre d'articles : colonne, sinon tranche &gt; 0, sinon stock (décision 1). */
    private PrevisionsBaseDto corps(PrevisionsBaseDto a, Map<String, Object> enTete, List<LignePrevisionDto> lignes,
                                    Contexte ctx) {
        Integer nombre = (Integer) enTete.get(NOMBRE_ARTICLES);
        if (nombre == null && a.nombreArticles() != null && a.nombreArticles() > 0) {
            nombre = a.nombreArticles();
        } else if (nombre == null) {
            nombre = Math.toIntExact(previsions.stock(ctx.tranche().getId()).nombreArticles());
            ctx.avertissements().add("Nombre d'articles absent du fichier et de la tranche : stock de la tranche retenu ("
                    + nombre + ").");
        }
        return new PrevisionsBaseDto(nombre, a.valorisationStock(),
                ou(enTete, "pourcentageConcretisation", a.pourcentageConcretisation()),
                ou(enTete, "debutConcretisation", a.debutConcretisation()),
                ou(enTete, "finConcretisation", a.finConcretisation()),
                ou(enTete, "debutFinalisation", a.debutFinalisation()),
                ou(enTete, "finFinalisation", a.finFinalisation()),
                ou(enTete, "ecartTypeConcretisation", a.ecartTypeConcretisation()),
                ou(enTete, "ecartTypeFinalisation", a.ecartTypeFinalisation()), lignes);
    }

    /** Valeur lue (type de {@link #CHAMPS}, donc celui du champ du DTO), sinon {@code actuelle}. */
    @SuppressWarnings("unchecked")
    private static <T> T ou(Map<String, Object> enTete, String champ, T actuelle) {
        Object lue = enTete.get(champ);
        return lue == null ? actuelle : (T) lue;
    }

    /**
     * Lignes existantes de la tranche ; l'en-tête reste vide (réimporté tel quel, il est donc gardé). Colonnes en texte :
     * le format nombre du classeur grouperait les milliers de l'année (« 2 027 »).
     */
    @Override
    public Modele modele(String type, CfgTranche tranche) {
        List<Map<String, Object>> lignes = previsions.previsions(tranche.getId()).lignes().stream().map(l -> {
            Map<String, Object> valeurs = new HashMap<>();
            Map<?, ?> dto = json.convertValue(l, Map.class);
            CHAMPS.stream().filter(c -> c.rang() == LIGNE).forEach(c -> valeurs.put(c.entete(), dto.get(c.champ())));
            return valeurs;
        }).toList();
        List<Colonne> colonnes = CHAMPS.stream().map(c -> new Colonne(c.entete(), TypeExport.TEXTE)).toList();
        return new Modele(tranche.getCode() + "_" + FEUILLE, List.of(new FeuilleExport(FEUILLE, colonnes, lignes, null)));
    }
}
