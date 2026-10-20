package com.tsix.immocrm.controller.patrimoine;

import com.sun.net.httpserver.HttpServer;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureArticleFiche;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seconde revue P3 (règle 6) : DTD / entité paramètre externes jamais chargées par le parseur (aucune requête réseau),
 * ETag / 304 soumis au périmètre (un If-None-Match juste ne révèle pas l'existence d'un plan hors périmètre).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlansSecondeRevueTest {

    private static final String IMPORT = "/api/tranches/{id}/plans/import";
    private static final String COMMERCIAL = "Commercial";
    private static final String D = "M 0,0 h 10 v 10 z";
    private static final String PLANS_DE_LA_TRANCHE = "SELECT COUNT(*) FROM StkHierarchyPlan WHERE StkHierarchyId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureArticleFiche lecture;

    private AdmUser admin;
    private String jeton;
    private StkFloor etage;
    private CfgTranche tranche;
    private StkItem article;
    private HttpServer serveur;
    private final AtomicInteger appels = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        admin = patrimoine.administrateur();
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        etage = plans.etage("SR-" + jeton);
        tranche = plans.tranche("SR-" + jeton, true);
        article = plans.article(tranche, "ART-SR-" + jeton, false, etage);
        serveur = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        serveur.createContext("/", echange -> {
            appels.incrementAndGet();
            byte[] corps = "<!ENTITY fuite \"FUITE\">".getBytes(StandardCharsets.UTF_8);
            echange.sendResponseHeaders(200, corps.length);
            echange.getResponseBody().write(corps);
            echange.close();
        });
        serveur.start();
    }

    @AfterEach
    void arreter() {
        serveur.stop(0);
    }

    private String url(String nom) {
        return "http://127.0.0.1:" + serveur.getAddress().getPort() + "/" + nom;
    }

    private String svg(String prologue) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                %s<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink">
                <image width="100" height="100" x="0" y="0" xlink:href="%s"/>
                <path id="%s" d="%s"/>
                </svg>""".formatted(prologue, FixturePlans.dataUriPng(), article.getCode(), D);
    }

    private MockMultipartHttpServletRequestBuilder importer(String svg) {
        MockMultipartHttpServletRequestBuilder requete = multipart(IMPORT, tranche.getId());
        requete.file(new MockMultipartFile("fichier", "plan.svg", "image/svg+xml", svg.getBytes(StandardCharsets.UTF_8)))
                .param("etageId", etage.getId().toString());
        requete.with(connecte(admin));
        return requete;
    }

    @Test
    void dtdExterne_systeme_400_sansAucuneRequeteReseau() throws Exception {
        mockMvc.perform(importer(svg("<!DOCTYPE svg SYSTEM \"" + url("dtd") + "\">\n")))
                .andExpect(status().isBadRequest());

        assertThat(appels.get()).as("requêtes vers la DTD externe").isZero();
        assertThat(lecture.compter(PLANS_DE_LA_TRANCHE, tranche.getId())).isZero();
    }

    @Test
    void entiteParametreExterne_400_sansAucuneRequeteReseau() throws Exception {
        String prologue = "<!DOCTYPE svg [<!ENTITY % distante SYSTEM \"" + url("pe") + "\"> %distante;]>\n";

        mockMvc.perform(importer(svg(prologue))).andExpect(status().isBadRequest());

        assertThat(appels.get()).as("requêtes vers l'entité paramètre").isZero();
        assertThat(lecture.compter(PLANS_DE_LA_TRANCHE, tranche.getId())).isZero();
    }

    @Test
    void image_ifNoneMatchJuste_horsPerimetre_404_et_pas_304() throws Exception {
        plans.trace(article.getId(), etage.getId(), D);
        UUID plan = plans.plan(tranche.getId(), etage.getId(), "Etage", FixturePlans.dataUriPng());
        String etag = mockMvc.perform(get("/api/plans/{planId}/image", plan).with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(etag).isNotBlank();

        CfgTranche autre = plans.tranche("SR-B-" + jeton, true);
        AdmUser compte = vente.compte("SR-COM-" + jeton, COMMERCIAL);
        plans.habiliter(compte, autre.getCfgProject());
        RequestPostProcessor commercial = FixtureDossierVente.connecte(compte, COMMERCIAL);

        mockMvc.perform(get("/api/plans/{planId}/image", plan).with(commercial).header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotFound());
    }
}
