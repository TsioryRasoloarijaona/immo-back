package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFichiersAttaches;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSuiviAffectations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.ARTICLE;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.CATEGORIE_LIBELLE;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.DOSSIER;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.EXTENSION;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.FACTURE_ACHAT;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.GROUPE;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.MODIFIE_LE;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.MODIFIE_PAR;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.OBJET_ID;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.REGLEMENT_CREDIT;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.TYPE_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Liste des fichiers attachés — FEATURE-admin-fichiers-attaches.md, règles testables 1 et 3, décision 5, endpoint
 * (champs de la ligne, {@code objetCle} du brief front), réponses R4 et R5.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FichiersAttachesListeTest {

    private static final String URL = "/api/administration/fichiers-attaches";
    private static final String P_RECHERCHE = "recherche";
    private static final String P_PAGE = "page";
    private static final String P_TAILLE = "taille";
    private static final String TOTAL = "$.totalElements";
    private static final String PREMIERE_LIGNE = "$.content[0].";
    private static final String OBJET_CLE = "objetCle";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFichiersAttaches fixture;
    @Autowired private FixtureEtatVentes etatVentes;
    @Autowired private FixtureSuiviAffectations affectations;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = "FA" + jeton();
    }

    private ResultActions lire(String... parametres) throws Exception {
        MockHttpServletRequestBuilder requete = get(URL).with(connecte(admin));
        for (int i = 0; i < parametres.length; i += 2) {
            requete.param(parametres[i], parametres[i + 1]);
        }
        return mockMvc.perform(requete);
    }

    private List<String> ids(String... parametres) throws Exception {
        String corps = lire(parametres).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(corps, "$.content[*].id");
    }

    private UUID piece(LocalDateTime date) {
        return fixture.piece(jeton, date).inserer();
    }

    // ------------------------------------------------------------------ règle 1 : total, cochées sans fichier comprises

    @Test
    void r1_total_egalNombreDeLignesDuFiltre_cocheesSansFichierComprises() throws Exception {
        piece(DATE);
        piece(DATE.minusDays(1));
        fixture.piece(jeton, DATE.minusDays(2)).cocheeSansFichier().inserer();

        lire(P_RECHERCHE, jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(3));
    }

    // R5 : la ligne cochée sans fichier reste dans la liste, sans aucun filtre de fichier.
    @Test
    void r5_ligneCocheeSansFichier_resteDansLaListe() throws Exception {
        UUID cochee = fixture.piece(jeton, DATE).cocheeSansFichier().inserer();

        assertThat(ids(P_RECHERCHE, jeton)).containsExactly(cochee.toString());
    }

    // ------------------------------------------------------------------ règle 3 / décision 5 : tri, pagination

    @Test
    void r3_triParDateDeCreationDecroissante() throws Exception {
        UUID ancien = piece(DATE.minusDays(2));
        UUID recent = piece(DATE);
        UUID milieu = piece(DATE.minusDays(1));

        assertThat(ids(P_RECHERCHE, jeton)).containsExactly(recent.toString(), milieu.toString(), ancien.toString());
    }

    // Le sens du départage par PKey n'est pas écrit au spec : seul un ordre par PKey, stable entre pages, est figé.
    @Test
    void r3_aDateEgale_ordreParPKey_stableEntreLesPages() throws Exception {
        for (int i = 0; i < 5; i++) {
            piece(DATE);
        }
        List<String> parPKey = fixture.idsParPKey(jeton);
        List<String> inverse = new ArrayList<>(parPKey);
        Collections.reverse(inverse);

        List<String> lus = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            lus.addAll(ids(P_RECHERCHE, jeton, P_TAILLE, "2", P_PAGE, String.valueOf(page)));
        }

        assertThat(lus).hasSize(5).isIn(parPKey, inverse);
    }

    @Test
    void d5_pagination_parDefautPage0Taille50() throws Exception {
        lire()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    void r3_pagination_totalExact_etPageDemandee() throws Exception {
        piece(DATE);
        UUID milieu = piece(DATE.minusDays(1));
        piece(DATE.minusDays(2));

        lire(P_RECHERCHE, jeton, P_PAGE, "1", P_TAILLE, "1")
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOTAL).value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath(PREMIERE_LIGNE + "id").value(milieu.toString()));
    }

    // ------------------------------------------------------------------ endpoint : champs de la ligne

    @Test
    void ligne_exposeLesChampsDuSpec() throws Exception {
        UUID type = fixture.type("PLAN " + jeton);
        UUID objet = UUID.randomUUID();
        UUID piece = fixture.piece(jeton, DATE)
                .avec(CATEGORIE_LIBELLE, "Dossier de vente N° 2026/001 du 10/03/2026")
                .avec(OBJET_ID, objet)
                .avec(TYPE_ID, type)
                .avec(EXTENSION, ".docx")
                .avec(GROUPE, "PLAN")
                .avec(MODIFIE_PAR, "Amina Test")
                .avec(MODIFIE_LE, DATE.plusDays(1))
                .inserer();

        lire(P_RECHERCHE, jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(PREMIERE_LIGNE + "id").value(piece.toString()))
                .andExpect(jsonPath(PREMIERE_LIGNE + "description").value(jeton))
                .andExpect(jsonPath(PREMIERE_LIGNE + "categorieLibelle")
                        .value("Dossier de vente N° 2026/001 du 10/03/2026"))
                .andExpect(jsonPath(PREMIERE_LIGNE + "objetId").value(objet.toString()))
                .andExpect(jsonPath(PREMIERE_LIGNE + "type").value("PLAN " + jeton))
                .andExpect(jsonPath(PREMIERE_LIGNE + "extension").value(".docx"))
                .andExpect(jsonPath(PREMIERE_LIGNE + "groupe").value("PLAN"))
                .andExpect(jsonPath(PREMIERE_LIGNE + "creePar").value("Karim Test"))
                .andExpect(jsonPath(PREMIERE_LIGNE + "creeLe", startsWith("2026-03-10T09:15:30")))
                .andExpect(jsonPath(PREMIERE_LIGNE + "modifiePar").value("Amina Test"))
                .andExpect(jsonPath(PREMIERE_LIGNE + "modifieLe", startsWith("2026-03-11T09:15:30")));
    }

    @Test
    void ligne_categorie_codeNormaliseNonVide() throws Exception {
        piece(DATE);

        String corps = lire(P_RECHERCHE, jeton).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        String categorie = JsonPath.read(corps, PREMIERE_LIGNE + "categorie");

        assertThat(categorie).isNotBlank();
    }

    // ------------------------------------------------------------------ objetCle (brief front) et R4

    private String objetCle(UUID piece) throws Exception {
        String corps = lire(P_RECHERCHE, jeton).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> lignes = JsonPath.read(corps, "$.content");
        Map<String, Object> ligne = lignes.stream().filter(l -> piece.toString().equals(l.get("id"))).findFirst()
                .orElseThrow(() -> new AssertionError("pièce absente de la liste : " + piece));
        return (String) ligne.get(OBJET_CLE);
    }

    @Test
    void objetCle_pieceDeDossier_dossier() throws Exception {
        ComFolder dossier = patrimoine.dossier("DOS-" + jeton);
        UUID piece = fixture.piece(jeton, DATE).avec(CATEGORIE, DOSSIER).avec(OBJET_ID, dossier.getId()).inserer();

        assertThat(objetCle(piece)).isEqualTo("dossier");
    }

    // Catégorie en texte libre « Dossier de vente N° … du … » : même objet que Yes.Com.Sales.Sale.
    @Test
    void objetCle_libelleDossierDeVente_dossier() throws Exception {
        ComFolder dossier = patrimoine.dossier("DOS-" + jeton);
        UUID piece = fixture.piece(jeton, DATE)
                .avec(CATEGORIE, "Dossier de vente N° 2026/001 du 10/03/2026")
                .avec(OBJET_ID, dossier.getId())
                .inserer();

        assertThat(objetCle(piece)).isEqualTo("dossier");
    }

    @Test
    void objetCle_pieceDArticle_article() throws Exception {
        CfgTranche tranche = etatVentes.tranche("TR-" + jeton, etatVentes.projet("PR-" + jeton, true), true);
        StkItem article = etatVentes.article("ART-" + jeton, tranche, etatVentes.categorie("CAT-" + jeton),
                etatVentes.type("TYP-" + jeton));
        UUID piece = fixture.piece(jeton, DATE).avec(CATEGORIE, ARTICLE).avec(OBJET_ID, article.getId()).inserer();

        assertThat(objetCle(piece)).isEqualTo("article");
    }

    @Test
    void objetCle_pieceDEncaissement_reglement() throws Exception {
        StlSettlement reglement = affectations.piece("REG-" + jeton, true, null, DATE, null, BigDecimal.TEN);
        UUID piece = fixture.piece(jeton, DATE).avec(CATEGORIE, REGLEMENT_CREDIT).avec(OBJET_ID, reglement.getId())
                .inserer();

        assertThat(objetCle(piece)).isEqualTo("reglement");
    }

    // R4 : facture d'achat, aucun écran Arpent → pas de lien.
    @Test
    void r4_factureDAchat_objetCleNul() throws Exception {
        UUID piece = fixture.piece(jeton, DATE).avec(CATEGORIE, FACTURE_ACHAT).inserer();

        assertThat(objetCle(piece)).isNull();
    }
}
