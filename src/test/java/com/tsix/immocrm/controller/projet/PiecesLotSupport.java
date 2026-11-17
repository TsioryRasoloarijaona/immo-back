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
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Routes, corps et messages du lot L3b-2b (FEATURE-lot-pieces-a-payer.md §3, §4), noms du fil exacts. */
final class PiecesLotSupport {

    static final String PIECES = "pieces-a-payer";
    static final String AVOIRS = "avoirs";
    static final String AVANCES = "avances";
    static final String REMBOURSEMENTS = "remboursements";

    static final String DROIT_AVANCE = "Yes.Prj.Front.Market.MarketMonitoringList.toolAdvance";
    static final String DROIT_SUPPRIMER_AVOIR = "Yes.Prj.Front.Market.RefundList.toolDelete";

    // champs de PieceLotSaisieDto / PieceLotModeleDto / PieceLotDetailDto
    static final String NATURE = "nature";
    static final String DATE = "date";
    static final String ECHEANCE = "echeance";
    static final String MONTANT_TTC = "montantTTC";
    static final String NOTES = "notes";
    static final String PENALITE_RG = "penaliteRg";

    // messages (R3, R4, R10, R11, R13)
    static final String MSG_FOURNISSEUR = "Veuillez d'abord selectionnez un fournisseur";
    static final String MSG_ETAT_AVA = "Vous ne pouvez pas ajouter une avance sur marché dans l'état actuel !";
    static final String MSG_ETAT_WAVA = "Vous ne pouvez pas ajouter une avance sur décompte dans l'état actuel !";
    static final String MSG_ETAT_RRG = "Vous ne pouvez pas ajouter une restitution de retenue de garantie dans l'état actuel !";
    static final String MSG_RG_BLOQUEE = "La restitution de la retenue de garantie a été bloquée !";
    static final String MSG_MODIFICATION = "Vous ne pouvez pas modifier cette pièce dans l'état actuel du lot !";
    static final String MSG_SUPPRESSION = "Vous n'avez pas le droit de supprimer un Documents à régler avec un statut ";
    static final String MSG_REGLEMENTS = "Des règlements sont liés à cette pièce : supprimez-les d'abord.";
    static final String MSG_AVOIR = "Vous n'avez pas le droit de supprimer un avoir avec un statut ";

    private final MockMvc mockMvc;

    PiecesLotSupport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private static String url(UUID lot, String route) {
        return AppelsProjet.LOTS + "/" + lot + "/" + route;
    }

    private Reponse envoyer(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requete, CorpsJson corps) {
        return requete.contentType(MediaType.APPLICATION_JSON).content(corps.json());
    }

    /** GET /lots/{id}/pieces-a-payer/modele?nature=… */
    Reponse modele(RequestPostProcessor qui, UUID lot, String nature) throws Exception {
        return envoyer(get(url(lot, PIECES) + "/modele").param(NATURE, nature), qui);
    }

    Reponse creer(RequestPostProcessor qui, UUID lot, CorpsJson corps) throws Exception {
        return envoyer(json(post(url(lot, PIECES)), corps), qui);
    }

    Reponse lire(RequestPostProcessor qui, UUID lot, UUID piece) throws Exception {
        return envoyer(get(url(lot, PIECES) + "/" + piece), qui);
    }

    Reponse modifier(RequestPostProcessor qui, UUID lot, UUID piece, CorpsJson corps) throws Exception {
        return envoyer(json(put(url(lot, PIECES) + "/" + piece), corps), qui);
    }

    Reponse supprimer(RequestPostProcessor qui, UUID lot, UUID piece) throws Exception {
        return envoyer(delete(url(lot, PIECES) + "/" + piece), qui);
    }

    Reponse supprimerAvoir(RequestPostProcessor qui, UUID lot, UUID avoir) throws Exception {
        return envoyer(delete(url(lot, AVOIRS) + "/" + avoir), qui);
    }

    /** GET /lots/{id}/{route} (lectures existantes : avances, remboursements, avoirs). */
    Reponse lecture(RequestPostProcessor qui, UUID lot, String route) throws Exception {
        return envoyer(get(url(lot, route)), qui);
    }

    /** Corps de saisie : notes « Notes de test », sans pénalité. */
    static CorpsJson saisie(String nature, LocalDate date, LocalDate echeance, String montant) {
        return corps().avec(NATURE, nature).avec(DATE, date).avec(ECHEANCE, echeance).avec(MONTANT_TTC, decimal(montant))
                .avec(NOTES, "Notes de test").avec(PENALITE_RG, null);
    }

    static BigDecimal decimal(String valeur) {
        return valeur == null ? null : new BigDecimal(valeur);
    }

    static UUID idCree(Reponse r) {
        return UUID.fromString(String.valueOf(r.valeur("$.id")));
    }
}
