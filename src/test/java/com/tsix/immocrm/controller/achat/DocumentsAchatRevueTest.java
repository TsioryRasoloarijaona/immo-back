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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.codes;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsAchat.total;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue du lot A1 : bords non couverts par les tests verrouillés (FEATURE-achat-lecture-factures.md §2-§3). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatRevueTest {

    private static final String COMMERCIAL = "Commercial";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID fournisseurA;
    private UUID fournisseurB;
    private String codeA1;
    private String codeA2;
    private UUID pieceA1;
    private AdmUser habiliteA;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ARV");
        UUID societe = fixture.societe("SOC-" + jeton);
        UUID projetA = fixture.projet("PA-" + jeton, societe);
        UUID projetB = fixture.projet("PB-" + jeton, societe);
        String nomA = "Fournisseur A " + jeton;
        String nomB = "Fournisseur B " + jeton;
        fournisseurA = fixture.fournisseur("FA-" + jeton, nomA);
        fournisseurB = fixture.fournisseur("FB-" + jeton, nomB);
        codeA1 = jeton + "-A1";
        codeA2 = jeton + "-A2";
        pieceA1 = fixture.piece(Piece.facture(codeA1, LocalDate.of(2026, 3, 10), fournisseurA, nomA, societe, projetA,
                new BigDecimal("1200")));
        UUID pieceA2 = fixture.piece(Piece.facture(codeA2, LocalDate.of(2026, 3, 11), fournisseurA, nomA, societe,
                projetA, new BigDecimal("500")));
        fixture.reglement(pieceA2, "R-" + jeton, new BigDecimal("500"), FixtureAchat.EN_COURS,
                LocalDate.of(2026, 3, 12), null);
        fixture.piece(Piece.facture(jeton + "-B", LocalDate.of(2026, 3, 11), fournisseurB, nomB, societe, projetB,
                new BigDecimal("2400")));
        habiliteA = fixture.compte("ACH-RV-" + jeton, COMMERCIAL);
        fixture.habiliterSociete(habiliteA, societe);
        fixture.habiliter(habiliteA, projetA);
    }

    private RequestPostProcessor qui() {
        return connecte(habiliteA, COMMERCIAL);
    }

    @Test
    void referentiels_fournisseurs_seulementCeuxDesPiecesVisibles() throws Exception {
        String json = corps(mockMvc, get(AppelsAchat.REFERENTIELS).with(qui()));

        List<String> ids = JsonPath.read(json, "$.fournisseurs[*].id");
        assertThat(ids).contains(fournisseurA.toString()).doesNotContain(fournisseurB.toString());
    }

    @Test
    void periode_auInclusMemeAvecUneHeure() throws Exception {
        fixture.poser(pieceA1, "DocDate", LocalDate.of(2026, 3, 10).atTime(15, 30));

        String json = corps(mockMvc, liste(qui(), "du", "2026-03-10", "au", "2026-03-10"));

        assertThat(codes(json)).containsExactly(codeA1);
    }

    @Test
    void recherche_jokerSqlEchappe() throws Exception {
        String json = corps(mockMvc, liste(qui(), "search", "%"));

        assertThat(total(json)).isZero();
    }

    @Test
    void triInconnu_400() throws Exception {
        mockMvc.perform(liste(qui(), "sort", "inexistant,asc")).andExpect(status().isBadRequest());
    }

    @Test
    void pagination_secondePage_triParDefaut() throws Exception {
        String json = corps(mockMvc, get(AppelsAchat.DOCUMENTS).param("page", "1").param("size", "1").with(qui()));

        assertThat(codes(json)).containsExactly(codeA1);
        assertThat(total(json)).isEqualTo(2);
    }

    @Test
    void export_nonSoldes_memeFiltreQueLaListe() throws Exception {
        List<String> textes = AppelsAchat.textesExport(mockMvc, get(AppelsAchat.EXPORT).param("format", "xlsx")
                .param("nonSoldes", "true").with(qui()));

        assertThat(textes).contains(codeA1).doesNotContain(codeA2);
    }
}
