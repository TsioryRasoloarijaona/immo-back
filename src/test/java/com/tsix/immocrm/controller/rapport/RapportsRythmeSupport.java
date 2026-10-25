package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Rapports de rythme du lot R2a (FEATURE-tdb-vente-rapports-rythme.md §1, §4.1, §4.4, §4.6) : codes, lignes, lecture. */
final class RapportsRythmeSupport {

    static final String RYTHME_VENTES = "ComTdbSalesRythme";
    static final String PAR_COMMERCIAL = "ComTdbConcretPerCommercial";
    static final String CONCRET_ENCAISSEMENT = "ComTdbConcretAndSetlement";
    static final List<String> CODES_R2A = List.of(RYTHME_VENTES, PAR_COMMERCIAL, CONCRET_ENCAISSEMENT);

    static final String TRANCHE_ID = "trancheId";
    static final String ANNEE = "annee";
    static final String DATE = "date";
    static final String COEFFICIENT = "coefficient";
    static final String NON_AFFECTE = "nonAffecte";
    static final String TOTAL = "total";

    private RapportsRythmeSupport() {
    }

    /** Lignes {@code AdmReportData} des trois rapports (libellé, groupe, ordre de la prod, §1) ; sans rôle lié. */
    static Map<String, UUID> installer(FixtureRapports fixture) {
        fixture.tables();
        return Map.of(
                RYTHME_VENTES, fixture.rapportVente(RYTHME_VENTES, "RYTHME DES VENTES*", "Suivi MENSUEL", 500),
                PAR_COMMERCIAL, fixture.rapportVente(PAR_COMMERCIAL, "CONCRETISATIONS PAR PERIODES", "", 2010),
                CONCRET_ENCAISSEMENT, fixture.rapportVente(CONCRET_ENCAISSEMENT, "CONCRETISATION ET ENCAISSEMENT*", "",
                        2030));
    }

    /** {@code GET …/{code}/resultat} avec {@code trancheId} répétés et des paires nom / valeur. */
    static MockHttpServletRequestBuilder demande(String code, RequestPostProcessor qui, List<CfgTranche> tranches,
                                                 String... parametres) {
        MockHttpServletRequestBuilder requete = get(resultat(VENTE, code)).with(qui);
        for (CfgTranche t : tranches) {
            requete.param(TRANCHE_ID, t.getId().toString());
        }
        for (int i = 0; i + 1 < parametres.length; i += 2) {
            requete.param(parametres[i], parametres[i + 1]);
        }
        return requete;
    }

    /** Lignes dont la colonne {@code nom} contient {@code jeton} (code de tranche ou de projet : libellé ou code). */
    static List<Map<String, Object>> lignesContenant(Map<String, Object> section, String nom, String jeton) {
        return lignes(section).stream().filter(l -> Objects.toString(l.get(nom), "").contains(jeton)).toList();
    }

    /** La seule ligne dont la colonne {@code nom} contient {@code jeton}. */
    static Map<String, Object> ligneContenant(Map<String, Object> section, String nom, String jeton) {
        List<Map<String, Object>> trouvees = lignesContenant(section, nom, jeton);
        if (trouvees.size() != 1) {
            throw new AssertionError("%s : %d ligne(s) où %s contient %s".formatted(section.get("id"), trouvees.size(),
                    nom, jeton));
        }
        return trouvees.get(0);
    }

    /** Noms des colonnes dont le libellé contient {@code jeton}. */
    static List<String> colonnesLibellees(Map<String, Object> section, String jeton) {
        return colonnes(section).stream().filter(c -> Objects.toString(c.get("libelle"), "").contains(jeton))
                .map(c -> (String) c.get("nom")).toList();
    }

    /** Le nom de la seule colonne dont le libellé contient {@code jeton}. */
    static String colonneLibellee(Map<String, Object> section, String jeton) {
        List<String> noms = colonnesLibellees(section, jeton);
        if (noms.size() != 1) {
            throw new AssertionError("%s : colonnes libellées %s = %s".formatted(section.get("id"), jeton, noms));
        }
        return noms.get(0);
    }

    /** Noms des colonnes {@code commercialNN}. */
    static List<String> colonnesCommerciales(Map<String, Object> section) {
        return colonnes(section).stream().map(c -> (String) c.get("nom")).filter(n -> n.matches("commercial\\d{2}"))
                .toList();
    }
}
