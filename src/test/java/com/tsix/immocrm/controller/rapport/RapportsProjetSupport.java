package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import com.tsix.immocrm.fixtures.FixtureRapportsProjet;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Cadre commun des tests N5b-a (FEATURE-n5b-rapports-projet.md) : société S, projets PA1 / PA2 habilités, PB non habilité,
 * compte libre (rôle à {@code WithReportRestriction = 0}), fournisseur, statuts de lot mono et multi.
 */
final class RapportsProjetSupport {

    static final String PROJET = "projet";
    static final String PROJETS = "projets";
    static final String CODE = "code";
    static final String RETENUE = "retenue";
    static final String RESTITUEE = "restituee";
    static final String RESTE = "resteARestituer";
    static final String PENALITE = "penalite";
    static final String FORMAT = "format";
    static final String MESSAGE_SANS_PROJET = "Veuillez sélectionner au moins un projet !";

    private RapportsProjetSupport() {
    }

    record Cadre(String jeton, UUID pa1, UUID pa2, UUID pb, UUID fournisseur, String nom, UUID statutMono, UUID statutMulti,
                 AdmUser compte, UUID societe, Map<String, UUID> ids) {

        String code(String prefixe) {
            return prefixe + "-" + jeton;
        }
    }

    static Cadre cadre(FixtureAchat achat, FixtureRapportsAchat comptes, FixtureRapportsProjet fixture,
                       FixtureLotsParite parite, FixtureLotFiche fiche) {
        String jeton = FixtureAchat.jeton("N5B");
        Map<String, UUID> ids = fixture.rapports();
        parite.genresDeLot();
        fiche.entitesPiecesLot();
        UUID societe = achat.societe("S-" + jeton);
        UUID pa1 = achat.projet("PA1-" + jeton, societe);
        UUID pa2 = achat.projet("PA2-" + jeton, societe);
        UUID pb = achat.projet("PB-" + jeton, societe);
        String nom = "Fournisseur N5B " + jeton;
        UUID fournisseur = achat.fournisseur("FOU-" + jeton, nom);
        UUID mono = parite.statut(ENTREPRENEURS, "N5S-" + jeton);
        UUID multi = parite.statut(FOURNISSEURS, "N5M-" + jeton);
        AdmUser compte = comptes.compte("N5B-" + jeton, false, societe, pa1, pa2);
        return new Cadre(jeton, pa1, pa2, pb, fournisseur, nom, mono, multi, compte, societe, ids);
    }

    private static MockHttpServletRequestBuilder avecProjets(MockHttpServletRequestBuilder r, AdmUser compte, UUID... projets) {
        r.with(connecte(compte, FixtureRapportsAchat.role()));
        for (UUID projet : projets) {
            r.param(PROJETS, projet.toString());
        }
        return r;
    }

    static MockHttpServletRequestBuilder requete(String code, AdmUser compte, UUID... projets) {
        return avecProjets(get(resultat(PROJET, code)), compte, projets);
    }

    static MockHttpServletRequestBuilder requeteExport(String code, String format, AdmUser compte, UUID... projets) {
        return avecProjets(get(export(PROJET, code)).param(FORMAT, format), compte, projets);
    }

    /** Section unique des rapports R1-R3 (le spec ne nomme pas son id). */
    static Map<String, Object> premiere(List<Map<String, Object>> sections) {
        return sections.getFirst();
    }

    static List<Object> codes(Map<String, Object> section) {
        return AppelsRapports.lignes(section).stream().map(l -> l.get(CODE)).toList();
    }

    /** La ligne du lot de code donné (exactement une exigée). */
    static Map<String, Object> ligne(Map<String, Object> section, String code) {
        List<Map<String, Object>> l = AppelsRapports.lignesOu(section, CODE, code);
        if (l.size() != 1) {
            throw new AssertionError(l.size() + " ligne(s) pour le lot " + code + " : " + codes(section));
        }
        return l.getFirst();
    }

    static BigDecimal montant(Map<String, Object> ligne, String colonne) {
        return Objects.requireNonNullElse(nombre(ligne.get(colonne)), BigDecimal.ZERO);
    }
}
