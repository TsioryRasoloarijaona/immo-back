package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.CorpsJson;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Routes, corps et codes de droits OMRAN du lot A2b-1 (FEATURE-achat-fiche-ecritures.md), noms du fil du spec. */
final class AppelsEcrituresAchat {

    static final String DOCUMENT = "/api/achat/documents/{id}";
    static final String STATUT = "/api/achat/documents/{id}/statut";
    static final String BON_COMMANDE = "/api/achat/documents/{id}/bon-commande";

    static final String INTROUVABLE = "Pièce introuvable";
    static final String BC_INTROUVABLE = "Bon de commande introuvable";
    static final String ROLE_JWT = "Commercial";
    static final String ADMINISTRATEUR = "Administrateur";

    // Droits de suppression (E-B1) : code Achat, code Projet › Lot (pièce à lot seulement).
    static final String SUPPR_DEVIS = "Yes.Buy.Front.Quote.QuoteList.toolDelete";
    static final String SUPPR_DEVIS_LOT = "Yes.Prj.Front.Market.QuoteList.toolDelete";
    static final String SUPPR_BC = "Yes.Buy.Front.Order.OrderList.toolDelete";
    static final String SUPPR_BC_LOT = "Yes.Prj.Front.Market.OrderList.toolDelete";
    static final String SUPPR_BL = "Yes.Buy.Front.Delivery.DeliveryList.toolDelete";
    static final String SUPPR_BL_LOT = "Yes.Prj.Front.Market.DeliveryList.toolDelete";
    static final String SUPPR_FACTURE = "Yes.Stl.Front.Debit.InvoiceList.toolDelete";
    static final String SUPPR_AVOIR = "Yes.Stl.Front.Debit.RefundList.toolDelete";
    static final String SUPPR_AVOIR_LOT = "Yes.Prj.Front.Market.RefundList.toolDelete";
    /** Q-D : existe en prod, ignoré pour la facture. */
    static final String SUPPR_FACTURE_LOT_IGNORE = "Yes.Prj.Front.Market.InvoiceList.toolDelete";
    /** Droit de modification de la facture, exigé par Affecter (Q-H). */
    static final String MODIF_FACTURE = "Yes.Stl.Front.Debit.InvoiceList.toolNew";
    static final String MODIF_FACTURE_LOT_IGNORE = "Yes.Prj.Front.Market.InvoiceList.toolNew";

    private AppelsEcrituresAchat() {
    }

    static MockHttpServletRequestBuilder transition(UUID piece, UUID statut, String note) {
        CorpsJson corps = CorpsJson.corps().avec("statutId", statut);
        if (note != null) {
            corps.avec("note", note);
        }
        return patch(STATUT, piece).contentType(MediaType.APPLICATION_JSON).content(corps.json());
    }

    static MockHttpServletRequestBuilder transitionCorps(UUID piece, String json) {
        return patch(STATUT, piece).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    static MockHttpServletRequestBuilder affecter(UUID facture, UUID bonCommande) {
        return put(BON_COMMANDE, facture).contentType(MediaType.APPLICATION_JSON)
                .content(CorpsJson.corps().avec("bonCommandeId", bonCommande).json());
    }

    static MockHttpServletRequestBuilder affecterCorps(UUID facture, String json) {
        return put(BON_COMMANDE, facture).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    static MockHttpServletRequestBuilder supprimer(UUID piece) {
        return delete(DOCUMENT, piece);
    }

    static MockHttpServletRequestBuilder fiche(UUID piece) {
        return get(DOCUMENT, piece);
    }

    static MvcResult appeler(MockMvc mockMvc, MockHttpServletRequestBuilder requete, RequestPostProcessor qui)
            throws Exception {
        return mockMvc.perform(requete.with(qui)).andReturn();
    }

    static int statut(MvcResult r) {
        return r.getResponse().getStatus();
    }

    static String corps(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString();
    }

    static String detail(MvcResult r) throws Exception {
        return JsonPath.read(corps(r), "$.detail");
    }

    /** {@code actions.transitions[*].statutId} de la fiche, en majuscules. */
    static List<String> statutsPermis(String ficheJson) {
        List<String> ids = JsonPath.read(ficheJson, "$.actions.transitions[*].statutId");
        return ids.stream().map(String::toUpperCase).toList();
    }

    static String id(UUID id) {
        return id.toString().toUpperCase();
    }
}
