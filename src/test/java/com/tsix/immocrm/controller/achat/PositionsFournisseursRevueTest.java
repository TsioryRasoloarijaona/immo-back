package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixturePositionsFournisseurs;
import com.tsix.immocrm.fixtures.FixturePositionsFournisseurs.Reglement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsPositions.MASQUER;
import static com.tsix.immocrm.controller.achat.AppelsPositions.PROJET_IDS;
import static com.tsix.immocrm.controller.achat.AppelsPositions.REGLE;
import static com.tsix.immocrm.controller.achat.AppelsPositions.SOCIETE_ID;
import static com.tsix.immocrm.controller.achat.AppelsPositions.SOCIETE_IDS;
import static com.tsix.immocrm.controller.achat.AppelsPositions.SOLDE_FIN;
import static com.tsix.immocrm.controller.achat.AppelsPositions.detail;
import static com.tsix.immocrm.controller.achat.AppelsPositions.lignesDe;
import static com.tsix.immocrm.controller.achat.AppelsPositions.montant;
import static com.tsix.immocrm.controller.achat.AppelsPositions.positions;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/** Revue du lot A3 : chemins de visibilité du détail et règlement sur-ventilé — FEATURE-achat-impayes-soldes.md §3.1, §4. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PositionsFournisseursRevueTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String FACTURE = FixtureAchat.FACTURE;
    private static final String FAUX = "false";
    private static final LocalDate DATE = LocalDate.of(2025, 3, 1);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixturePositionsFournisseurs fixture;

    private String jeton;
    private UUID societe;
    private UUID societeT;
    private UUID projetA;
    private UUID projetB;
    private UUID entite;
    private UUID x;
    private String nomX;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("APR");
        societe = achat.societe("SOC-" + jeton);
        societeT = achat.societe("SOCT-" + jeton);
        projetA = achat.projet("PA-" + jeton, societe);
        projetB = achat.projet("PB-" + jeton, societe);
        entite = fixture.entiteReglement(jeton);
        nomX = "Fournisseur X " + jeton;
        x = fixture.fournisseur("FOUX-" + jeton, nomX);
        compte = achat.compte("APR-" + jeton, COMMERCIAL);
        achat.habiliterSociete(compte, societe);
        achat.habiliter(compte, projetA);
    }

    private RequestPostProcessor qui() {
        return connecte(compte, COMMERCIAL);
    }

    private List<Object> ids(String json) {
        return JsonPath.read(json, "$[*].id");
    }

    @Test
    void detail_projetHorsPerimetre_rienMemeLesMouvementsSansProjet() throws Exception {
        fixture.piece(jeton + "-A", FACTURE, DATE, x, nomX, societe, projetA, "100");
        fixture.piece(jeton + "-0", FACTURE, DATE, x, nomX, societe, null, "50");
        fixture.reglement(Reglement.debit("RG1-" + jeton, DATE, societe, x, entite, "30"));

        String json = corps(mockMvc, detail(qui(), x, SOCIETE_ID, societe.toString(), PROJET_IDS, projetB.toString()));

        assertThat(ids(json)).isEmpty();
    }

    @Test
    void detail_societeIdsNeRelargitPasLaSocieteDemandee() throws Exception {
        UUID projetT = achat.projet("PT-" + jeton, societeT);
        achat.habiliter(compte, projetT);
        fixture.piece(jeton + "-T", FACTURE, DATE, x, nomX, societeT, projetT, "100");
        fixture.piece(jeton + "-A", FACTURE, DATE, x, nomX, societe, projetA, "100");

        String json = corps(mockMvc, detail(qui(), x, SOCIETE_ID, societeT.toString(),
                SOCIETE_IDS, societe.toString()));

        assertThat(ids(json)).isEmpty();
    }

    @Test
    void detail_tiersClient_vide200() throws Exception {
        UUID client = fixture.client("CLI-" + jeton, "Client " + jeton);
        fixture.piece(jeton + "-C", FACTURE, DATE, client, "Client " + jeton, societe, projetA, "100");

        String json = corps(mockMvc, detail(qui(), client, SOCIETE_ID, societe.toString()));

        assertThat(ids(json)).isEmpty();
    }

    @Test
    void reglementDUneSocieteNonHabilitee_surPieceVisible_nonCompte() throws Exception {
        UUID a = fixture.piece(jeton + "-A", FACTURE, DATE, x, nomX, societe, projetA, "100");
        UUID rg = fixture.reglement(Reglement.debit("RGT-" + jeton, DATE, societeT, x, entite, "60"));
        fixture.ligne(rg, a, "60");

        String json = corps(mockMvc, positions(qui(), MASQUER, FAUX));

        assertAll(
                () -> assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("0"),
                () -> assertThat(lignesDe(json, x, societeT)).isEmpty());
    }

    @Test
    void lignesSuperieuresALaBase_partNonRattacheeNegative_regleEgalBase() throws Exception {
        UUID a = fixture.piece(jeton + "-A", FACTURE, DATE, x, nomX, societe, projetA, "1000");
        UUID rg = fixture.reglement(Reglement.debit("RG2-" + jeton, DATE, societe, x, entite, "100"));
        fixture.ligne(rg, a, "150");

        String json = corps(mockMvc, positions(qui(), MASQUER, FAUX));
        String lignes = corps(mockMvc, detail(qui(), x, SOCIETE_ID, societe.toString()));

        assertAll(
                () -> assertThat(montant(json, x, societe, REGLE)).isEqualByComparingTo("100"),
                () -> assertThat(montant(json, x, societe, SOLDE_FIN)).isEqualByComparingTo("900"),
                () -> assertThat(JsonPath.<List<Object>>read(lignes,
                        "$[?(@.type == 'REGLEMENT_NON_RATTACHE')].reglement")).hasSize(1));
    }
}
