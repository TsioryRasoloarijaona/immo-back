package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat;
import com.tsix.immocrm.fixtures.FixtureDemandesAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureDemandesAchatB;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchat.qui;
import static com.tsix.immocrm.fixtures.FixtureDemandesAchatB.sauf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Points N6b laissés au développeur : prix d'une offre, bordereau entier, export, droits des modalités (FEATURE-n6-demandes-achat.md). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DemandesAchatOngletsSaisieTest {

    private static final String RACINE = "/api/achat/demandes-achat/";
    private static final String MODALITES = "/modalites";
    private static final String LIGNES_OFFRE = "SELECT * FROM BuyDocumentLine WHERE PKey = ?";
    private static final String LIGNES_DIA = "SELECT * FROM BuySelectionLine WHERE BuySelectionId = ? ORDER BY Sorting ASC";
    private static final String MODALITES_DIA = "SELECT * FROM BuySelectionPaymentTerm WHERE BuySelectionId = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDemandesAchat demandes;
    @Autowired private FixtureDemandesAchatB b;
    @Autowired private FixtureAchat achat;

    private DemandesAchatAppels appels;
    private Cadre c;
    private RequestPostProcessor qLibre;

    @BeforeEach
    void setUp() {
        appels = new DemandesAchatAppels(mockMvc);
        c = b.cadre("OS");
        qLibre = qui(b.libre(c, "n6b.os.libre." + c.jeton()));
    }

    private MockHttpServletResponse remplacer(String chemin, String json) throws Exception {
        return mockMvc.perform(put(RACINE + chemin).with(qLibre).contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn().getResponse();
    }

    private UUID offreAvecLigne(UUID dia, String suffixe) {
        UUID tiers = achat.fournisseur("T" + suffixe + "-" + c.jeton(), "Fournisseur " + suffixe);
        b.preselection(dia, tiers, LocalDate.of(2026, 3, 20));
        return b.offre(dia, tiers, c.code("O" + suffixe), false);
    }

    @Test
    void modifierOffre_prixEtRemise_ecritsEtPrixApresRemiseRendu() throws Exception {
        UUID dia = demandes.demande(c, c.code("PX"), c.prep());
        UUID offre = offreAvecLigne(dia, "PX");
        UUID ligne = b.ligneOffre(offre, 1, "A1", "0", "0");

        MockHttpServletResponse r = remplacer(dia + "/offres/" + offre,
                "[{\"ligneId\":\"%s\",\"prixUnitaireHT\":100,\"remise\":0.1}]".formatted(ligne));

        String prixApresRemise = r.getContentAsString(StandardCharsets.UTF_8);
        assertThat(r.getStatus()).as(prixApresRemise).isEqualTo(200);
        Map<String, Object> l = b.lignes(LIGNES_OFFRE, ligne.toString()).getFirst();
        SoftAssertions.assertSoftly(s -> {
            s.assertThat((BigDecimal) l.get("UnitPriceHT")).isEqualByComparingTo("100");
            s.assertThat((BigDecimal) l.get("DiscountRatio")).isEqualByComparingTo("0.1");
            s.assertThat(new BigDecimal(JsonPath.read(prixApresRemise, "$.lignes[0].prixApresRemiseHT").toString()))
                    .isEqualByComparingTo("90");
        });
    }

    @Test
    void modifierOffre_remiseHorsBornes_400_rienNEstEcrit() throws Exception {
        UUID dia = demandes.demande(c, c.code("RB"), c.prep());
        UUID offre = offreAvecLigne(dia, "RB");
        UUID ligne = b.ligneOffre(offre, 1, "A1", "5", "0");

        MockHttpServletResponse r = remplacer(dia + "/offres/" + offre,
                "[{\"ligneId\":\"%s\",\"prixUnitaireHT\":100,\"remise\":1.5}]".formatted(ligne));

        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(400);
        assertThat((BigDecimal) b.lignesApresRefus(LIGNES_OFFRE, ligne.toString()).getFirst().get("UnitPriceHT"))
                .isEqualByComparingTo("5");
    }

    @Test
    void enregistrerBordereau_codeEnDouble_400_bordereauIntact() throws Exception {
        UUID dia = demandes.demande(c, c.code("BD"), c.prep());
        UUID categorie = achat.categorie("CATBD-" + c.jeton());
        b.ligne(dia, null, 1, "X1", "10", categorie);
        String ligne = "{\"id\":\"%s\",\"ordre\":%d,\"code\":\"D1\",\"designation\":\"L\",\"unite\":\"U\",\"quantite\":1,"
                + "\"prixUnitaireHT\":1,\"tauxTVA\":20,\"tauxFODEC\":0,\"categorieId\":\"" + categorie + "\"}";

        MockHttpServletResponse r = remplacer(dia + "/bordereau", "{\"groupes\":[],\"lignes\":[%s,%s]}".formatted(
                ligne.formatted(UUID.randomUUID(), 1), ligne.formatted(UUID.randomUUID(), 2)));

        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(400);
        assertThat(b.lignesApresRefus(LIGNES_DIA, dia.toString())).extracting(m -> m.get("Code")).containsExactly("X1");
    }

    @Test
    void enregistrerBordereau_remplaceLesLignes_nouveauxIds() throws Exception {
        UUID dia = demandes.demande(c, c.code("BR"), c.prep());
        UUID categorie = achat.categorie("CATBR-" + c.jeton());
        UUID ancienne = b.ligne(dia, null, 1, "X1", "10", categorie);
        String corps = ("{\"groupes\":[],\"lignes\":[{\"id\":\"%s\",\"ordre\":1,\"code\":\"N1\",\"designation\":\"L\",\"unite\":\"U\","
                + "\"quantite\":3,\"prixUnitaireHT\":10,\"tauxTVA\":20,\"tauxFODEC\":0,\"categorieId\":\"%s\"}]}")
                .formatted(ancienne, categorie);

        MockHttpServletResponse r = remplacer(dia + "/bordereau", corps);

        assertThat(r.getStatus()).as(r.getContentAsString()).isEqualTo(200);
        List<Map<String, Object>> lignes = b.lignes(LIGNES_DIA, dia.toString());
        assertThat(lignes).singleElement().satisfies(l -> {
            assertThat(l).containsEntry("Code", "N1");
            assertThat(l.get("PKey").toString()).isNotEqualToIgnoringCase(ancienne.toString());
            assertThat((BigDecimal) l.get("UnitPriceTTC")).isEqualByComparingTo("12");
        });
    }

    @Test
    void exporterBordereau_classeur() throws Exception {
        UUID dia = demandes.demande(c, c.code("EX"), c.prep());
        b.ligne(dia, null, 1, "X1", "10", achat.categorie("CATEX-" + c.jeton()));

        MockHttpServletResponse r = mockMvc.perform(get(RACINE + dia + "/bordereau/export").with(qLibre)).andReturn().getResponse();

        assertThat(r.getStatus()).isEqualTo(200);
        assertThat(r.getContentType()).contains("spreadsheetml");
    }

    @Test
    void ficheLot_peutAjouterDia_suitLeStatutDuLot() throws Exception {
        UUID lot = demandes.lot(c, "LDIA-" + c.jeton(), false);
        b.lotAvecDia(lot, false);
        String ferme = mockMvc.perform(get("/api/projet/lots/" + lot).with(qLibre)).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
        b.lotAvecDia(lot, true);
        String ouvert = mockMvc.perform(get("/api/projet/lots/" + lot).with(qLibre)).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(JsonPath.<Boolean>read(ferme, "$.peutAjouterDia")).as(ferme).isFalse();
            s.assertThat(JsonPath.<Boolean>read(ouvert, "$.peutAjouterDia")).as(ouvert).isTrue();
        });
    }

    @Test
    void modalites_sansDroitNouveau_403_avecDroit_cree_codeAbsent_400() throws Exception {
        UUID dia = demandes.demande(c, c.code("MD"), c.prep());
        RequestPostProcessor sansNouveau = qui(demandes.libre(c, "n6b.os.sansnew." + c.jeton(), sauf(FixtureDemandesAchat.DROIT_NOUVEAU)));

        DemandesAchatAppels.Reponse refus = appels.poster(sansNouveau, dia, MODALITES, corps().avec("ordre", 1).avec("code", "AV"));
        int apresRefus = b.lignesApresRefus(MODALITES_DIA, dia.toString()).size();
        DemandesAchatAppels.Reponse sansCode = appels.poster(qLibre, dia, MODALITES, corps().avec("ordre", 1));
        DemandesAchatAppels.Reponse cree = appels.poster(qLibre, dia, MODALITES,
                corps().avec("ordre", 1).avec("code", "AV").avec("ratio", new BigDecimal("0.3")));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(refus.statut()).as(refus.corps()).isEqualTo(403);
            s.assertThat(apresRefus).isZero();
            s.assertThat(sansCode.statut()).as(sansCode.corps()).isEqualTo(400);
            s.assertThat(cree.statut()).as(cree.corps()).isBetween(200, 299);
            s.assertThat(b.lignes(MODALITES_DIA, dia.toString())).singleElement()
                    .satisfies(m -> assertThat((BigDecimal) m.get("Ratio")).isEqualByComparingTo("0.3"));
        });
    }
}
