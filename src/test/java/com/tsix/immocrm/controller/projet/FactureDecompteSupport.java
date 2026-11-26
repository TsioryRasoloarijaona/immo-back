package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Routes, droits et messages de FEATURE-lc8-facture-decompte.md (Contrat, R4, R6), noms du fil exacts. */
final class FactureDecompteSupport {

    static final String FACTURE = "/facture";
    static final String AVOIR = "/avoir";

    /** D4 : droits OMRAN lus par {@code DroitsOmran.exiger}. */
    static final String DROIT_FACTURE = "Yes.Stl.Front.Debit.InvoiceList.toolNew";
    static final String DROIT_AVOIR = "Yes.Prj.Front.Market.RefundList.toolNew";

    static final String MSG_DEJA_FACTURE = "Une facture est déjà générée sur ce décompte";
    static final String MSG_SANS_FACTURE = "Impossible de générer un avoir sur un décompte sans facture";

    // FactureGenereeDto
    static final String FACTURE_ID = "$.factureId";
    static final String CODE = "$.code";
    static final String NET = "$.netAPayer";
    static final String DETAIL = "$.detail";

    private final MockMvc mockMvc;

    FactureDecompteSupport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private Reponse envoyer(MockHttpServletRequestBuilder requete, RequestPostProcessor qui) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(requete.with(qui)).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    /** POST /api/projet/decomptes/{id}/facture ou /avoir, sans corps. */
    Reponse generer(RequestPostProcessor qui, UUID decompte, String route) throws Exception {
        return envoyer(post(AppelsProjet.DECOMPTES + "/" + decompte + route), qui);
    }

    Reponse supprimer(RequestPostProcessor qui, UUID decompte) throws Exception {
        return envoyer(delete(AppelsProjet.DECOMPTES + "/" + decompte), qui);
    }

    static UUID factureId(Reponse r) {
        return UUID.fromString(String.valueOf(r.valeur(FACTURE_ID)));
    }
}
