package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.CorpsJson;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Appels et corps des écritures du lot L3b-2a (FEATURE-lot-avenants-avances.md §4) : avenants, échéances de RG, réceptions.
 * Noms du fil exacts du spec.
 */
final class AvenantsLotSupport {

    static final String AVENANTS = "avenants";
    static final String ECHEANCES_RG = "echeances-rg";
    static final String RECEPTIONS = "receptions";
    static final String REMBOURSEMENTS = "remboursements";

    static final String DROIT_NOUVEL_AVENANT = "Yes.Prj.Front.Market.ClauseList.toolNew";
    static final String DROIT_SUPPRIMER_AVENANT = "Yes.Prj.Front.Market.ClauseList.toolDelete";
    static final String DROIT_RETENUES_PAR_MONTANT = "Yes.Prj.Front.Market.MarketMonitoringList.CanEditWorkingOutRetainByAmount";

    // champs d'avenant
    static final String ID = "id";
    static final String DATE = "date";
    static final String REFERENCE_INTERNE = "referenceInterne";
    static final String NOTES = "notes";
    static final String GARANTIE = "garantie";
    static final String PRORATA = "prorata";
    static final String ASSURANCE_DEC = "assuranceDEC";
    static final String ASSURANCE_TRC = "assuranceTRC";
    static final String PAR_MONTANT = "parMontant";
    static final String TAUX = "taux";
    static final String MONTANT = "montant";
    static final String LIGNES = "lignes";
    static final String ORDRE = "ordre";
    static final String CODE = "code";
    static final String DESIGNATION = "designation";
    static final String UNITE = "unite";
    static final String QUANTITE = "quantite";
    static final String PU_HT = "prixUnitaireHT";
    static final String TVA = "tauxTVA";
    static final String FODEC = "tauxFODEC";
    static final String CATEGORIE = "categorieId";
    static final String LIGNE_BORDEREAU = "ligneBordereauId";

    // champs d'échéance
    static final String DESCRIPTION = "description";
    static final String DELAI = "delaiJours";
    static final String ECHEANCE = "echeance";
    static final String POURCENTAGE = "pourcentage";

    // champs de réception
    static final String DATE_RECEPTION = "dateReception";
    static final String LIEU = "lieu";
    static final String DEMANDEE = "demandeeParFournisseur";
    static final String TYPE = "typeId";
    static final String GENRE = "genreId";
    static final String RESERVES = "reserves";
    static final String TIERS_INVITES = "tiersInvites";
    static final String LEVEE = "levee";
    static final String TIERS = "tiersId";

    private final MockMvc mockMvc;

    AvenantsLotSupport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private static String url(UUID lot, String route) {
        return AppelsProjet.LOTS + "/" + lot + "/" + route;
    }

    private static String url(UUID lot, String route, UUID id) {
        return url(lot, route) + "/" + id;
    }

    private Reponse envoyer(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requete, String corps) {
        return requete.contentType(MediaType.APPLICATION_JSON).content(corps);
    }

    /** GET /lots/{id}/{route}. */
    Reponse lire(RequestPostProcessor qui, UUID lot, String route) throws Exception {
        return envoyer(get(url(lot, route)), qui);
    }

    /** GET /lots/{id}/{route}/{fiche}. */
    Reponse lire(RequestPostProcessor qui, UUID lot, String route, UUID fiche) throws Exception {
        return envoyer(get(url(lot, route, fiche)), qui);
    }

    /** POST /lots/{id}/{route}. */
    Reponse creer(RequestPostProcessor qui, UUID lot, String route, CorpsJson corps) throws Exception {
        return envoyer(json(post(url(lot, route)), corps.json()), qui);
    }

    /** PUT /lots/{id}/{route}/{fiche}. */
    Reponse modifier(RequestPostProcessor qui, UUID lot, String route, UUID fiche, CorpsJson corps) throws Exception {
        return envoyer(json(put(url(lot, route, fiche)), corps.json()), qui);
    }

    /** DELETE /lots/{id}/{route}/{fiche}. */
    Reponse supprimer(RequestPostProcessor qui, UUID lot, String route, UUID fiche) throws Exception {
        return envoyer(delete(url(lot, route, fiche)), qui);
    }

    /** PUT /lots/{id}/echeances-rg, corps = la grille entière (tableau JSON). */
    Reponse enregistrerEcheances(RequestPostProcessor qui, UUID lot, List<CorpsJson> echeances) throws Exception {
        StringJoiner tableau = new StringJoiner(",", "[", "]");
        echeances.forEach(e -> tableau.add(e.json()));
        return envoyer(json(put(url(lot, ECHEANCES_RG)), tableau.toString()), qui);
    }

    // ------------------------------------------------------------------ corps

    /** Retenue d'avenant ; {@code taux} en pourcentage. */
    static CorpsJson retenue(boolean parMontant, String taux, String montant) {
        return corps().avec(PAR_MONTANT, parMontant).avec(TAUX, decimal(taux)).avec(MONTANT, decimal(montant));
    }

    static CorpsJson sansRetenue() {
        return retenue(false, "0", null);
    }

    /** Avenant : référence « REF », sans notes, 4 retenues à 0 au taux. */
    static CorpsJson avenant(LocalDate date, List<CorpsJson> lignes) {
        return corps().avec(DATE, date).avec(REFERENCE_INTERNE, "REF").avec(NOTES, null)
                .avec(GARANTIE, sansRetenue()).avec(PRORATA, sansRetenue()).avec(ASSURANCE_DEC, sansRetenue())
                .avec(ASSURANCE_TRC, sansRetenue()).avec(LIGNES, lignes);
    }

    /** Ligne d'avenant : désignation « ART code », unité U, sans ligne de bordereau ni notes ; taux en pourcentage. */
    static CorpsJson ligne(UUID id, int ordre, String code, String quantite, String puHT, String tva, String fodec, UUID categorie) {
        return corps().avec(ID, id).avec(ORDRE, ordre).avec(CODE, code).avec(DESIGNATION, "ART " + code).avec(UNITE, "U")
                .avec(QUANTITE, decimal(quantite)).avec(PU_HT, decimal(puHT)).avec(TVA, decimal(tva)).avec(FODEC, decimal(fodec))
                .avec(CATEGORIE, categorie).avec(LIGNE_BORDEREAU, null).avec(NOTES, null);
    }

    static CorpsJson echeance(UUID id, Integer ordre, String description, Integer delai, LocalDate echeance, String pourcentage) {
        return corps().avec(ID, id).avec(ORDRE, ordre).avec(DESCRIPTION, description).avec(DELAI, delai).avec(ECHEANCE, echeance)
                .avec(POURCENTAGE, decimal(pourcentage));
    }

    /** Réception : description « Réception code », lieu « Chantier », non demandée par le fournisseur. */
    static CorpsJson reception(int ordre, String code, LocalDate date, UUID type, UUID genre, List<CorpsJson> reserves,
                               List<CorpsJson> tiers) {
        return corps().avec(ORDRE, ordre).avec(CODE, code).avec(DESCRIPTION, "Réception " + code).avec(DATE_RECEPTION, date)
                .avec(LIEU, "Chantier").avec(DEMANDEE, false).avec(TYPE, type).avec(GENRE, genre).avec(RESERVES, reserves)
                .avec(TIERS_INVITES, tiers);
    }

    static CorpsJson reserve(UUID id, int ordre, String description, boolean levee) {
        return corps().avec(ID, id).avec(ORDRE, ordre).avec(DESCRIPTION, description).avec(LEVEE, levee);
    }

    static CorpsJson tiersInvite(UUID id, int ordre, UUID tiers) {
        return corps().avec(ID, id).avec(ORDRE, ordre).avec(TIERS, tiers);
    }

    static BigDecimal decimal(Object valeur) {
        return valeur == null ? null : new BigDecimal(String.valueOf(valeur));
    }
}
