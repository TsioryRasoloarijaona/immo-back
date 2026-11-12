package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.codes;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.NATURES;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Revue C8 (règle 6) : {@code natures=ORDER&natures=ORDER} est la même sélection que {@code natures=ORDER} (écran Bons de
 * commande) ; la restriction « mes BC » ne doit pas tomber parce que la valeur est répétée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatBonsCommandeRevueTest {

    private static final String ROLE = "Commercial";
    private static final String ORDER = "ORDER";
    private static final String TRIGGER = "CfgTierTriggerId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("REV");
        UUID societe = fixture.societe("SOC-" + jeton);
        String nom = "Fournisseur " + jeton;
        UUID fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        UUID autrePersonnel = fixture.fournisseur("EMP2-" + jeton, "Autre personnel " + jeton);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);
        fixture.poser(bc("-BC-MOI", fournisseur, nom, societe), TRIGGER, compte.getCfgEmployee().getId());
        fixture.poser(bc("-BC-AUTRE", fournisseur, nom, societe), TRIGGER, autrePersonnel);
    }

    private UUID bc(String suffixe, UUID fournisseur, String nom, UUID societe) {
        return fixture.piece(Piece.facture(jeton + suffixe, LocalDate.of(2026, 3, 10), fournisseur, nom, societe, null,
                new BigDecimal("100")).nature(ORDER));
    }

    @Test
    void natureOrderRepetee_resteRestreinte() throws Exception {
        String json = corps(mockMvc, liste(connecte(compte, ROLE), NATURES, ORDER, NATURES, ORDER));

        assertThat(codes(json)).containsExactly(jeton + "-BC-MOI");
    }
}
