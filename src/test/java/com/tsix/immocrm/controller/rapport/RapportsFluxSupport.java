package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Rapports de flux du lot R1a (FEATURE-tdb-vente-rapports-flux.md §4) : codes, lignes {@code AdmReportData}, lecture. */
final class RapportsFluxSupport {

    static final String ETAT_VENTES = "ComTdbSalesState";
    static final String ACTIVITE_PAR_PROJET = "ComTdbActiviteParProjet";
    static final String ACTIVITE_PAR_MOIS = "ComTdbActiviteParMois";
    static final String COMPARAISON = "ComTdbSaleComparaison";
    static final List<String> CODES = List.of(ETAT_VENTES, ACTIVITE_PAR_PROJET, ACTIVITE_PAR_MOIS, COMPARAISON);

    static final String TRANCHE_ID = "trancheId";
    static final String ANNEE = "annee";
    static final String PREVU_NON_SAISI = "Prévu non saisi";

    private RapportsFluxSupport() {
    }

    /** Lignes {@code AdmReportData} des quatre rapports (groupe et ordre de la prod, §4) ; sans rôle lié. */
    static Map<String, UUID> installer(FixtureRapports fixture) {
        fixture.tables();
        return Map.of(
                ETAT_VENTES, fixture.rapportVente(ETAT_VENTES, "ETAT DES VENTE", "", 1200),
                ACTIVITE_PAR_PROJET, fixture.rapportVente(ACTIVITE_PAR_PROJET,
                        "ACTIVITES COMMERCIALES PAR PROJET et PAR ANNEE", "Suivi ANNUEL", 300),
                ACTIVITE_PAR_MOIS, fixture.rapportVente(ACTIVITE_PAR_MOIS, "Objectifs (ENC, CONC, CH) PAR MOIS",
                        "Suivi MENSUEL", 200),
                COMPARAISON, fixture.rapportVente(COMPARAISON, "COMPARAISON DES VENTES", "VENTE", 100));
    }

    /** {@code GET …/{code}/resultat?annee=…&trancheId=…} ; {@code annee} nulle = défaut du rapport. */
    static MockHttpServletRequestBuilder demande(String code, RequestPostProcessor qui, Integer annee,
                                                 CfgTranche... tranches) {
        MockHttpServletRequestBuilder requete = get(resultat(VENTE, code)).with(qui);
        if (annee != null) {
            requete.param(ANNEE, String.valueOf(annee));
        }
        for (CfgTranche t : tranches) {
            requete.param(TRANCHE_ID, t.getId().toString());
        }
        return requete;
    }

    /** La seule ligne dont la colonne {@code nom} vaut {@code valeur} (comparée en texte). */
    static Map<String, Object> ligne(Map<String, Object> section, String nom, Object valeur) {
        List<Map<String, Object>> trouvees = lignes(section).stream()
                .filter(l -> String.valueOf(valeur).equals(String.valueOf(l.get(nom)))).toList();
        if (trouvees.size() != 1) {
            throw new AssertionError("%s : %d ligne(s) où %s = %s".formatted(section.get("id"), trouvees.size(), nom,
                    valeur));
        }
        return trouvees.get(0);
    }

    static List<String> noms(Map<String, Object> section) {
        return colonnes(section).stream().map(c -> (String) c.get("nom")).toList();
    }

    static Map<String, Object> colonne(Map<String, Object> section, String nom) {
        return colonnes(section).stream().filter(c -> nom.equals(c.get("nom"))).findFirst()
                .orElseThrow(() -> new AssertionError(section.get("id") + " : colonne absente " + nom));
    }

    /** N° 5 : nom de la colonne « Encaissements (*) » réel de la section {@code mensuel} (groupe d'en-tête du spec §4.1). */
    static String encaissementsReel(Map<String, Object> mensuel) {
        List<String> trouves = colonnes(mensuel).stream()
                .filter(c -> "Encaissements (*)".equals(c.get("groupe")) && String.valueOf(c.get("nom")).endsWith("Reel"))
                .map(c -> (String) c.get("nom")).toList();
        if (trouves.size() != 1) {
            throw new AssertionError("colonne Encaissements (*) …Reel : " + trouves);
        }
        return trouves.get(0);
    }

    /** Somme de toutes les valeurs numériques non nulles des lignes, colonnes MONTANT et NOMBRE. */
    static BigDecimal sommeDesChiffres(Map<String, Object> section) {
        BigDecimal somme = BigDecimal.ZERO;
        for (Map<String, Object> c : colonnes(section)) {
            if ("MONTANT".equals(c.get("type")) || "NOMBRE".equals(c.get("type"))) {
                for (Map<String, Object> l : lignes(section)) {
                    BigDecimal v = nombre(l.get(c.get("nom")));
                    somme = v == null ? somme : somme.add(v.abs());
                }
            }
        }
        return somme;
    }
}
