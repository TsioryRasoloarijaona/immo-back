package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat.LigneSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.avecSources;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BC;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.corpsValide;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.creer;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.guidDe;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.idCree;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.DEVIS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/** BC généré depuis l'offre d'une DIA : rattaché à la DIA comme OMRAN {@code GenateDocFromDocs} (FEATURE-n6-demandes-achat.md). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatBonCommandeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureGenerationAchat generation;
    @Autowired private FixtureDemandesAchat demandes;

    @Test
    void bcDepuisOffre_rattacheALaDemande() throws Exception {
        FixtureAchat.Cadre c = fixture.cadre("DBC");
        FixtureAchat.Saisie s = fixture.configurationSaisie(c.jeton());
        AdmUser redacteur = fixture.compteHabilite(c, "DBC", ROLE_JWT, NOUVEAU_BC);
        fixture.sansPersonnel(redacteur);
        generation.parametres(true, false, false);
        FixtureDemandesAchat.Cadre cd = demandes.cadre("DBC");
        UUID dia = demandes.demande(cd, cd.code("BC"), cd.prep());
        UUID offre = generation.source(c, "-OF", DEVIS, s.gestion(FixtureAchat.GESTION_DEVIS), null);
        generation.ligne(offre, LigneSource.simple(1, "ART-1", "10", s.categorie()));
        fixture.poser(offre, "BuySelectionId", dia);

        MvcResult r = appeler(mockMvc, creer(avecSources(corpsValide(c, s, BON_COMMANDE), offre)), connecte(redacteur, ROLE_JWT));

        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        assertThat(guidDe(fixture.pieceEnBase(idCree(r)), "BuySelectionId")).isEqualTo(id(dia));
    }
}
