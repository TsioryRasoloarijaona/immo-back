package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAvoirAchat.Contexte;
import static com.tsix.immocrm.controller.achat.AppelsAvoirAchat.avoirExistant;
import static com.tsix.immocrm.controller.achat.AppelsAvoirAchat.corpsSansLigne;
import static com.tsix.immocrm.controller.achat.AppelsAvoirAchat.facture;
import static com.tsix.immocrm.controller.achat.AppelsAvoirAchat.preparer;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.avecSources;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.creer;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.guidDe;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.modifier;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/** Revue FEATURE-achat-avoirs.md R14 : fournisseur et société d'un avoir = ceux de sa facture, au POST et figés au PUT. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatAvoirTiersSocieteRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureGenerationAchat generation;

    private Contexte x;
    private UUID facture;
    private UUID autreFournisseur;
    private UUID autreSociete;

    @BeforeEach
    void setUp() {
        x = preparer(fixture, generation, "RTS");
        facture = facture(fixture, x, "-FTS");
        autreFournisseur = fixture.fournisseur("RTS-AUTRE-" + x.c().jeton(), "Autre fournisseur");
        autreSociete = fixture.societe("RTS-AUTRE-" + x.c().jeton());
    }

    private MvcResult par(MockHttpServletRequestBuilder requete) throws Exception {
        return appeler(mockMvc, requete, connecte(x.redacteur(), ROLE_JWT));
    }

    @Test
    void post_autreFournisseurEtSocieteDansLeCorps_lAvoirPrendCeuxDeLaFacture() throws Exception {
        MvcResult r = par(creer(avecSources(corpsSansLigne(x, "100", "20", false)
                .avec("fournisseurId", autreFournisseur).avec("societeId", autreSociete), facture)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        String json = corps(r);
        String fournisseur = JsonPath.read(json, "$.fournisseurId");
        String societe = JsonPath.read(json, "$.societeId");
        assertThat(fournisseur).isEqualToIgnoringCase(id(x.c().fournisseur()));
        assertThat(societe).isEqualToIgnoringCase(id(x.c().societe()));
    }

    @Test
    void put_autreFournisseurEtSocieteDansLeCorps_ignores_200() throws Exception {
        UUID avoir = avoirExistant(fixture, x, facture, false, "100", "120");
        fixture.poser(avoir, "CfgTierId", x.c().fournisseur());
        fixture.poser(avoir, "CfgCompanyId", x.c().societe());

        MvcResult r = par(modifier(avoir, corpsSansLigne(x, "100", "20", false)
                .avec("fournisseurId", autreFournisseur).avec("societeId", autreSociete)));

        fixture.oublier();
        Map<String, Object> ligne = fixture.pieceEnBase(avoir);
        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        assertThat(guidDe(ligne, "CfgTierId")).isEqualToIgnoringCase(id(x.c().fournisseur()));
        assertThat(guidDe(ligne, "CfgCompanyId")).isEqualToIgnoringCase(id(x.c().societe()));
    }
}
