package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
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

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Revue A2a (règle 6) : {@code avoirsMontant} ne somme que les avoirs visibles ; un avoir hors périmètre ne fuit pas. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatAvoirsMontantRevueTest {

    private static final String ROLE = "Commercial";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID autreSociete;
    private UUID projet;
    private UUID projetHors;
    private UUID fournisseur;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ARV");
        societe = fixture.societe("SOC-" + jeton);
        autreSociete = fixture.societe("SOC2-" + jeton);
        projet = fixture.projet("A-" + jeton, societe);
        projetHors = fixture.projet("B-" + jeton, autreSociete);
        fournisseur = fixture.fournisseur("FOU-" + jeton, "Fournisseur " + jeton);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);
        fixture.habiliter(compte, projet);
    }

    private UUID piece(String suffixe, String nature, boolean horsPerimetre, String ttc) {
        return fixture.piece(Piece.facture(jeton + suffixe, LocalDate.of(2026, 4, 2), fournisseur, "Fournisseur " + jeton,
                horsPerimetre ? autreSociete : societe, horsPerimetre ? projetHors : projet, new BigDecimal(ttc))
                .nature(nature));
    }

    @Test
    void avoirsMontant_avoirHorsPerimetre_nEntrePasDansLaSomme() throws Exception {
        UUID facture = piece("-FAC", FixtureAchat.FACTURE, false, "1200");
        fixture.poser(piece("-AV1", FixtureAchat.AVOIR, false, "300"), "InvoiceSourceId", facture);
        fixture.poser(piece("-AV-HORS", FixtureAchat.AVOIR, true, "50"), "InvoiceSourceId", facture);

        String json = corps(mockMvc, get(AppelsAchat.DOCUMENT, facture).with(connecte(compte, ROLE)));

        assertThat(new BigDecimal(JsonPath.read(json, "$.avoirsMontant").toString())).isEqualByComparingTo("300");
    }
}
