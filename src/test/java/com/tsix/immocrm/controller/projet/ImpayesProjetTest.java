package com.tsix.immocrm.controller.projet;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Projet › Dépenses : Documents à régler, Avoirs, Impayés = liste {@code GET /api/achat/documents} avec {@code natures},
 * {@code nonSoldes} et {@code moduleProjet=true} ; impayé = {@code ROUND(reste, 2) > 0} (Q1) — FEATURE-achat-impayes-soldes.md §5, §6.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImpayesProjetTest {

    private static final String DOCUMENTS = "/api/achat/documents";
    private static final String ROLE = "Commercial";
    private static final String NATURES = "natures";
    private static final String MODULE_PROJET = "moduleProjet";
    private static final String NON_SOLDES = "nonSoldes";
    private static final String VRAI = "true";
    private static final String MODULE = "AdmModuleId";
    private static final LocalDate DATE = LocalDate.of(2026, 3, 10);
    private static final LocalDate DATE_REGLEMENT = LocalDate.of(2026, 3, 20);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID fournisseur;
    private String nom;
    private UUID typeReglement;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("IPJ");
        societe = fixture.societe("SOC-" + jeton);
        projet = fixture.projet("P-" + jeton, societe);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        typeReglement = fixture.typeReglement("VIR-" + jeton);
        compte = fixture.compte("IPJ-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);
        fixture.habiliter(compte, projet);
    }

    private String code(String suffixe) {
        return jeton + suffixe;
    }

    /** Pièce du projet habilité, rattachée au module {@code module} (PRJ ou BUY). */
    private UUID piece(String suffixe, String nature, String module, String aPayer) {
        return piece(suffixe, nature, module, projet, aPayer);
    }

    private UUID piece(String suffixe, String nature, String module, UUID projetPiece, String aPayer) {
        UUID id = fixture.piece(Piece.facture(code(suffixe), DATE, fournisseur, nom, societe, projetPiece,
                new BigDecimal(aPayer)).nature(nature));
        fixture.poser(id, MODULE, fixture.module(module));
        return id;
    }

    private void regler(UUID piece, String suffixe, String montant) {
        fixture.reglement(piece, "RG" + suffixe + "-" + jeton, new BigDecimal(montant), FixtureAchat.EN_COURS,
                DATE_REGLEMENT, typeReglement);
    }

    private List<String> codes(String... params) throws Exception {
        MockHttpServletRequestBuilder requete = get(DOCUMENTS).param("size", "200").with(connecte(compte, ROLE));
        for (int i = 0; i + 1 < params.length; i += 2) {
            requete.param(params[i], params[i + 1]);
        }
        MvcResult r = mockMvc.perform(requete).andReturn();
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return JsonPath.read(r.getResponse().getContentAsString(), "$.content[*].code");
    }

    @Test
    void s6_documentsARegler_projet_seulementModulePrj() throws Exception {
        piece("-DP", FixtureAchat.DOCUMENT_A_REGLER, "PRJ", "100");
        piece("-DB", FixtureAchat.DOCUMENT_A_REGLER, "BUY", "100");
        piece("-FP", FixtureAchat.FACTURE, "PRJ", "100");

        List<String> codes = codes(NATURES, FixtureAchat.DOCUMENT_A_REGLER, MODULE_PROJET, VRAI);

        assertThat(codes).containsExactly(code("-DP"));
    }

    @Test
    void s6_avoirs_projet_seulementModulePrj() throws Exception {
        piece("-AP", FixtureAchat.AVOIR, "PRJ", "100");
        piece("-AB", FixtureAchat.AVOIR, "BUY", "100");
        piece("-FP", FixtureAchat.FACTURE, "PRJ", "100");

        List<String> codes = codes(NATURES, FixtureAchat.AVOIR, MODULE_PROJET, VRAI);

        assertThat(codes).containsExactly(code("-AP"));
    }

    /** Q1 : reste 0,03 impayé (ROUND(reste, 2) > 0) ; soldé, trop réglé, module BUY et projet hors périmètre exclus. */
    @Test
    void s6_q1_impayes_projet_facturesEtDocuments_resteArrondiPositif() throws Exception {
        UUID facture = piece("-FI", FixtureAchat.FACTURE, "PRJ", "1000");
        regler(facture, "1", "100");
        UUID document = piece("-DI", FixtureAchat.DOCUMENT_A_REGLER, "PRJ", "500");
        regler(document, "2", "499.97");
        UUID solde = piece("-SO", FixtureAchat.FACTURE, "PRJ", "800");
        regler(solde, "3", "800");
        UUID tropRegle = piece("-TR", FixtureAchat.FACTURE, "PRJ", "300");
        regler(tropRegle, "4", "320");
        piece("-AV", FixtureAchat.AVOIR, "PRJ", "50");
        piece("-BUY", FixtureAchat.FACTURE, "BUY", "700");
        UUID horsPerimetre = fixture.projet("PH-" + jeton, societe);
        piece("-HP", FixtureAchat.FACTURE, "PRJ", horsPerimetre, "900");

        List<String> codes = codes(NATURES, FixtureAchat.FACTURE, NATURES, FixtureAchat.DOCUMENT_A_REGLER,
                NON_SOLDES, VRAI, MODULE_PROJET, VRAI);

        assertThat(codes).containsExactlyInAnyOrder(code("-FI"), code("-DI"));
    }

    @Test
    void s6_q1_impayes_projet_resteArrondiAZero_exclu() throws Exception {
        UUID presque = piece("-PR", FixtureAchat.FACTURE, "PRJ", "500");
        regler(presque, "1", "499.996");
        piece("-FI", FixtureAchat.FACTURE, "PRJ", "100");

        List<String> codes = codes(NATURES, FixtureAchat.FACTURE, NON_SOLDES, VRAI, MODULE_PROJET, VRAI);

        assertThat(codes).containsExactly(code("-FI"));
    }

    @Test
    void s6_impayes_projet_nonConnecte_401() throws Exception {
        mockMvc.perform(get(DOCUMENTS).param(NATURES, FixtureAchat.FACTURE).param(NON_SOLDES, VRAI)
                        .param(MODULE_PROJET, VRAI))
                .andExpect(status().isUnauthorized());
    }
}
