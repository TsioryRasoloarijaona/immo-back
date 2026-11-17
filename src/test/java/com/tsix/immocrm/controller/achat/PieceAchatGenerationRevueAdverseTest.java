package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat.LigneSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.champDesLignesDeCode;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.decimal;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.modele;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BC;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_BL;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.NOUVEAU_FACTURE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_LIVRAISON;
import static com.tsix.immocrm.fixtures.FixtureAchat.FACTURE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/** Revue adverse A2b-2b (FEATURE-achat-generation.md G4) : une source répétée ne doit pas compter deux fois. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatGenerationRevueAdverseTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureGenerationAchat generation;

    private Cadre c;
    private Saisie s;
    private AdmUser redacteur;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("AGR");
        s = fixture.configurationSaisie(c.jeton());
        redacteur = fixture.compteHabilite(c, "AGR", ROLE_JWT, NOUVEAU_BC, NOUVEAU_BL, NOUVEAU_FACTURE);
        fixture.sansPersonnel(redacteur);
        generation.parametres(false, false, false);
        generation.tableBuyOrderInvoice();
    }

    /** Facture depuis [BL, BL] (même id deux fois) : le BL de 3 unités ne doit pas être proposé à 6 (argent doublé). */
    @Test
    void g4_sourceRepetee_neDoublePasLesQuantites() throws Exception {
        UUID bc = generation.source(c, "-BC", BON_COMMANDE, s.gestion(FixtureAchat.GESTION_BC), s.valideDg());
        UUID bl = generation.source(c, "-BL", BON_LIVRAISON, s.gestion(FixtureAchat.GESTION_BL), s.valideDg());
        fixture.poser(bl, "BuyOrderId", bc);
        fixture.poser(bl, "AmountRegul", new BigDecimal("10"));
        generation.ligne(bl, LigneSource.simple(1, "A", "3", s.categorie()));

        MvcResult r = appeler(mockMvc, modele(FACTURE, bl, bl), connecte(redacteur, ROLE_JWT));

        if (statut(r) == 200) {
            List<Object> quantites = champDesLignesDeCode(corps(r), "A", "quantite");
            assertThat(quantites).as(corps(r)).hasSize(1);
            assertThat(decimal(quantites.getFirst())).as(corps(r)).isEqualByComparingTo("3");
        } else {
            assertThat(statut(r)).as(corps(r)).isIn(400, 409);
        }
    }
}
