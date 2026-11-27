package com.tsix.immocrm.controller.perimetre;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.entities.stl.StlQuoteItem;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.services.modele.InitialiseurModeles;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Revue 2 du lot A2 (FEATURE-agences-vente.md R-C1, décision 9) : chemins que la revue 1 n'a pas couverts. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesA2SecondeRevueTest {

    private static final BigDecimal PRIX_LOT = new BigDecimal("1000000.00");
    private static final BigDecimal PRIX_LOT_HT = new BigDecimal("833333.333333");
    private static final BigDecimal TVA = new BigDecimal("1.2");

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private DataSource dataSource;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;
    @Autowired private FixtureStatutsDossier statuts;
    @Autowired private InitialiseurModeles initialiseur;
    @Autowired private JsonMapper json;

    private JdbcTemplate jdbc;
    private String jeton;
    private CfgTranche tranche;
    private UUID vendeur;
    private UUID modePaie;
    private RequestPostProcessor compteAgence;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        statuts.reservation();
        tranche = plans.tranche("SRP-" + jeton, true);
        UUID agence = agences.agence("SRA-" + jeton);
        vendeur = agences.vendeur("SRV-" + jeton);
        agences.lier(agence, vendeur);
        modePaie = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO ComPaymentMethod (PKey, Code) VALUES (?, ?)", modePaie.toString(), "SRM-" + jeton);
        AdmUser compte = agences.compteAgence("SRC-" + jeton, vendeur);
        plans.habiliter(compte, tranche.getCfgProject());
        compteAgence = FixtureDossierVente.connecte(compte, FixtureAgences.ROLE_AGENCE);
    }

    private StkItem article(String suffixe, String ttc, String ht, BigDecimal tva) {
        StkItem article = plans.article(tranche, "SRL-" + suffixe + "-" + jeton, false, null);
        if (tva != null) {
            article.setVat(tva);
        }
        article.setComSalePrice(new BigDecimal(ttc));
        article.setComSalePriceHT(new BigDecimal(ht));
        em.flush();
        return article;
    }

    private UUID prospection(String suffixe) {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("SRT-" + suffixe + "-" + jeton);
        prospect.setDescription("Prospect " + suffixe + " " + jeton);
        em.persist(prospect);
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("SRPS-" + suffixe + "-" + jeton);
        p.setDocDate(LocalDateTime.of(2026, 11, 1, 10, 0));
        p.setUserCreateDateTime(LocalDateTime.of(2026, 11, 1, 10, 0));
        p.setIsClosed(false);
        p.setCfgProspect(prospect);
        p.setCfgCommercial(em.getReference(CfgTier.class, vendeur));
        p.setStkHierarchy(em.getReference(CfgTranche.class, tranche.getId()));
        em.persist(p);
        em.flush();
        return p.getId();
    }

    /** Offre sans échéancier, l'annexe à un prix d'offre plus bas que le catalogue. */
    private UUID offre(UUID prospection, StkItem lot, StkItem annexe, String prixAnnexe) {
        StlQuote q = new StlQuote();
        q.setId(UUID.randomUUID());
        q.setCode("SRO-" + jeton);
        q.setDocDate(LocalDateTime.of(2026, 11, 2, 0, 0));
        q.setComProspection(em.getReference(ComProspection.class, prospection));
        q.setCfgTranche(tranche);
        q.setStkItem(lot);
        q.setStkVocation(lot.getStkVocation());
        em.persist(q);
        BigDecimal ttc = new BigDecimal(prixAnnexe);
        BigDecimal ht = ttc.divide(TVA, 6, RoundingMode.HALF_UP);
        StlQuoteItem ligne = new StlQuoteItem();
        ligne.setId(UUID.randomUUID());
        ligne.setStlQuote(q);
        ligne.setStkItem(annexe);
        ligne.setCode("SROL-" + jeton);
        ligne.setVatRatio(new BigDecimal("0.2"));
        ligne.setPriceBeforeDiscount(ttc);
        ligne.setPriceBeforeDiscountHT(ht);
        ligne.setDiscount(BigDecimal.ZERO);
        ligne.setPriceAfterDiscount(ttc);
        ligne.setPriceAfterDiscountHT(ht);
        em.persist(ligne);
        em.flush();
        return q.getId();
    }

    private String corps(UUID prospection, String prix, UUID offre, UUID... articles) {
        StringBuilder ids = new StringBuilder();
        for (UUID article : articles) {
            ids.append(ids.isEmpty() ? "" : ",").append('"').append(article).append('"');
        }
        String champOffre = offre == null ? "" : ",\"offreId\":\"%s\"".formatted(offre);
        return """
                {"prospectionId":"%s","articleIds":[%s],"modePaieId":"%s","cin":"CIN-%s","prix":%s,\
                "remisePercent":0%s}""".formatted(prospection, ids, modePaie, jeton, prix, champOffre);
    }

    // ------------------------------------------------------------------ décision 9, sans prix saisi

    /**
     * Sans {@code prix}, le dossier prend « lot + annexes » aux prix de l'offre : 1 050 000 pour un catalogue de
     * 1 200 000. Décision 9 : une offre d'agence n'abaisse jamais le plancher (400 ou prix ≥ catalogue).
     */
    @Test
    void d9_sansPrix_offreAnnexeMoinsChere_aucunDossierSousLeCatalogue() throws Exception {
        StkItem lot = article("LOT", "1000000.00", PRIX_LOT_HT.toPlainString(), new BigDecimal("0.20"));
        StkItem annexe = article("ANX", "200000.00", "166666.666667", null);
        UUID prospection = prospection("D9N");
        UUID offre = offre(prospection, lot, annexe, "50000.00");

        mockMvc.perform(post("/api/dossiers").with(compteAgence).contentType(MediaType.APPLICATION_JSON)
                .content(corps(prospection, "null", offre, lot.getId(), annexe.getId())));

        em.flush();
        Integer sousLeCatalogue = jdbc.queryForObject("SELECT COUNT(*) FROM ComFolder WHERE ComProspectionId = ? "
                + "AND PriceAfterDiscount < ?", Integer.class, prospection.toString(), new BigDecimal("1200000.00"));
        assertThat(sousLeCatalogue).isZero();
    }

    // ------------------------------------------------------------------ R-C1, générateur de modèles

    /**
     * Réponse (2) : 409 pour tout appelant sur un dossier d'agence non pointé. Le modèle actif ACTE_RESERVATION se
     * génère aussi par {@code GET /api/modeles-documents/{id}/generer} : même contrat, sans le contrôle R-C1.
     */
    @Test
    void rC1_siege_acteParLeGenerateurDeModeles_409SansPointage() throws Exception {
        StkItem lot = article("RC1", PRIX_LOT.toPlainString(), PRIX_LOT_HT.toPlainString(), new BigDecimal("0.20"));
        String reponse = mockMvc.perform(post("/api/dossiers").with(compteAgence)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps(prospection("RC1"), "1000000.00", null, lot.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String dossier = JsonPath.read(reponse, "$.id");

        RequestPostProcessor siege = user("siege").roles("Administrateur");
        initialiseur.initialiserActeReservation();
        JsonNode acte = json.readTree(mockMvc.perform(get("/api/modeles-documents/admin")
                                .param("categorie", "DOSSIER_VENTE").with(siege))
                        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
                .findParents("code").stream()
                .filter(n -> "ACTE_RESERVATION".equals(n.get("code").asString()))
                .findFirst().orElseThrow();

        mockMvc.perform(get("/api/dossiers/{id}/documents/acte-reservation", dossier).with(siege))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/modeles-documents/{id}/generer", acte.get("id").asString())
                        .param("ficheId", dossier).with(siege))
                .andExpect(status().isConflict());
    }
}
