package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.controller.projet.LotsPariteSupport.Creation;
import com.tsix.immocrm.fixtures.CorpsJson;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Appels des routes du lot L2a (FEATURE-lot-fiche-parite.md E1, E5, E6, E8), noms du fil exacts. */
final class LotFicheSupport {

    static final String DESCRIPTION = "description";
    static final String NOTES = "notes";

    private final MockMvc mockMvc;
    private final AppelsProjet appels;

    LotFicheSupport(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
        this.appels = new AppelsProjet(mockMvc);
    }

    /** GET /lots/{id} (E1). */
    Reponse fiche(RequestPostProcessor qui, UUID lot) throws Exception {
        return appels.lire(qui, AppelsProjet.LOTS + "/" + lot);
    }

    /** GET /lots/{id}/demandes-achat (E5). */
    Reponse demandesAchat(RequestPostProcessor qui, UUID lot) throws Exception {
        return appels.lire(qui, AppelsProjet.LOTS + "/" + lot + "/demandes-achat");
    }

    /** PATCH /lots/{id} (E6). */
    Reponse modifier(RequestPostProcessor qui, UUID lot, CorpsJson corps) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(patch(AppelsProjet.LOTS + "/" + lot).with(qui)
                .contentType(MediaType.APPLICATION_JSON).content(corps.json())).andReturn().getResponse();
        return new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8));
    }

    /** POST /lots/{id}/copie (E8), sans corps ; renvoie aussi l'en-tête Location. */
    Creation copier(RequestPostProcessor qui, UUID lot) throws Exception {
        MockHttpServletResponse r = mockMvc.perform(post(AppelsProjet.LOTS + "/" + lot + "/copie").with(qui))
                .andReturn().getResponse();
        return new Creation(new Reponse(r.getStatus(), r.getContentAsString(StandardCharsets.UTF_8)), r.getHeader("Location"));
    }
}
