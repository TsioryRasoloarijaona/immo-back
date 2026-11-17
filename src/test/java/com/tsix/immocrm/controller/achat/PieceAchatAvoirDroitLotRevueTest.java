package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
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

import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAvoirAchat.Contexte;
import static com.tsix.immocrm.controller.achat.AppelsAvoirAchat.NOUVEL_AVOIR_LOT;
import static com.tsix.immocrm.controller.achat.AppelsAvoirAchat.corpsSansLigne;
import static com.tsix.immocrm.controller.achat.AppelsAvoirAchat.facture;
import static com.tsix.immocrm.controller.achat.AppelsAvoirAchat.preparer;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.avecSources;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.modele;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.creer;
import static com.tsix.immocrm.fixtures.FixtureAchat.AVOIR;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue FEATURE-achat-avoirs.md D10 / R8 / R5 : le droit d'un avoir se lit sur le lot de sa facture source. Le modèle
 * le fait ; le POST lit {@code lotId} du corps : un compte au seul code Projet › Lot crée un avoir sur une facture
 * sans lot en ajoutant un lot quelconque au corps.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatAvoirDroitLotRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureGenerationAchat generation;

    private Contexte x;
    private UUID factureSansLot;
    private UUID lot;
    private AdmUser compteLot;

    @BeforeEach
    void setUp() {
        x = preparer(fixture, generation, "RDL");
        lot = fixture.lot("LOT-" + x.c().jeton(), x.c().projet());
        factureSansLot = facture(fixture, x, "-FSL");
        compteLot = fixture.compteHabilite(x.c(), "RDL-LOT", ROLE_JWT, NOUVEL_AVOIR_LOT);
        fixture.sansPersonnel(compteLot);
    }

    private MvcResult parCompteLot(MockHttpServletRequestBuilder requete) throws Exception {
        return appeler(mockMvc, requete, connecte(compteLot, ROLE_JWT));
    }

    /** Facture sans lot, compte au seul code Lot : modèle 403 ; le POST avec un lotId inventé doit l'être aussi. */
    @Test
    void post_factureSansLot_lotAjouteAuCorps_compteCodeLotSeul_403CommeLeModele() throws Exception {
        MvcResult modeleRefuse = parCompteLot(modele(AVOIR, factureSansLot));
        MvcResult post = parCompteLot(creer(avecSources(corpsSansLigne(x, "100", "20", false).avec("lotId", lot),
                factureSansLot)));

        assertThat(statut(modeleRefuse)).as(corps(modeleRefuse)).isEqualTo(403);
        assertThat(statut(post)).as("POST avoir avec lotId sur une facture sans lot : " + corps(post)).isEqualTo(403);
    }
}
