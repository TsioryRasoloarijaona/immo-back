package com.tsix.immocrm.controller.perimetre;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.entities.stl.StlQuoteItem;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue 1 du lot A2 — FEATURE-agences-vente.md « Décisions du manager après revue 1 » (8, 9) et trous des tests
 * verrouillés : bornes du plancher, 409 sans écriture, refus de R-D7 sans prospect créé, POST /api/prospections.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesA2RevueTest {

    private static final String DOSSIERS = "/api/dossiers";
    private static final String PROSPECTS = "/api/prospects";
    private static final String DETAIL = "$.detail";
    private static final String SANS_PRIX = "Ce lot n'a pas de prix de référence : la vente passe par le siège";
    private static final BigDecimal PRIX_LOT = new BigDecimal("1000000.00");
    private static final BigDecimal PRIX_LOT_HT = new BigDecimal("833333.333333");
    private static final String COMPTE_PROSPECTION = "SELECT COUNT(*) FROM ComFolder WHERE ComProspectionId = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private DataSource dataSource;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;
    @Autowired private FixtureStatutsDossier statuts;

    private JdbcTemplate jdbc;
    private String jeton;
    private CfgTranche trancheP;
    private CfgTranche trancheQ;
    private UUID agenceA;
    private UUID vendeurA;
    private UUID vendeurA2;
    private UUID modePaie;
    private RequestPostProcessor compteA;
    private RequestPostProcessor compteA2;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        statuts.reservation();
        trancheP = plans.tranche("RVP-" + jeton, true);
        trancheQ = plans.tranche("RVQ-" + jeton, true);

        agenceA = agences.agence("RVA-" + jeton);
        vendeurA = agences.vendeur("RVVA-" + jeton);
        vendeurA2 = agences.vendeur("RVVA2-" + jeton);
        agences.lier(agenceA, vendeurA);
        agences.lier(agenceA, vendeurA2);

        modePaie = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO ComPaymentMethod (PKey, Code) VALUES (?, ?)", modePaie.toString(), "RVM-" + jeton);

        compteA = compteAgence("RVC", vendeurA);
        compteA2 = compteAgence("RVC2", vendeurA2);
    }

    // ------------------------------------------------------------------ fixtures

    private RequestPostProcessor compteAgence(String prefixe, UUID vendeur) {
        AdmUser compte = agences.compteAgence(prefixe + "-" + jeton, vendeur);
        plans.habiliter(compte, trancheP.getCfgProject());
        return FixtureDossierVente.connecte(compte, FixtureAgences.ROLE_AGENCE);
    }

    private StkItem lot(String suffixe, BigDecimal ttc, BigDecimal ht) {
        StkItem article = plans.article(trancheP, "RVL-" + suffixe + "-" + jeton, false, null);
        article.setVat(new BigDecimal("0.20"));
        article.setComSalePrice(ttc);
        article.setComSalePriceHT(ht);
        em.flush();
        return article;
    }

    private StkItem annexe(String suffixe, String ttc, String ht) {
        StkItem annexe = plans.article(trancheP, "RVA-" + suffixe + "-" + jeton, false, null);
        annexe.setComSalePrice(new BigDecimal(ttc));
        annexe.setComSalePriceHT(new BigDecimal(ht));
        em.flush();
        return annexe;
    }

    private UUID prospection(String suffixe, UUID vendeur) {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("RVT-" + suffixe + "-" + jeton);
        prospect.setDescription("Prospect " + suffixe + " " + jeton);
        em.persist(prospect);
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("RVPS-" + suffixe + "-" + jeton);
        p.setDocDate(LocalDateTime.of(2026, 11, 1, 10, 0));
        p.setUserCreateDateTime(LocalDateTime.of(2026, 11, 1, 10, 0));
        p.setIsClosed(false);
        p.setCfgProspect(prospect);
        p.setCfgCommercial(em.getReference(CfgTier.class, vendeur));
        p.setStkHierarchy(em.getReference(CfgTranche.class, trancheP.getId()));
        em.persist(p);
        em.flush();
        return p.getId();
    }

    /** Offre de la prospection sur le lot, avec l'annexe à son prix d'offre (plus bas que le catalogue). */
    private UUID offre(UUID prospection, StkItem lot, StkItem annexe, String prixAnnexe) {
        StlQuote q = new StlQuote();
        q.setId(UUID.randomUUID());
        q.setCode("RVO-" + jeton);
        q.setDocDate(LocalDateTime.of(2026, 11, 2, 0, 0));
        q.setComProspection(em.getReference(ComProspection.class, prospection));
        q.setCfgTranche(trancheP);
        q.setStkItem(lot);
        q.setStkVocation(lot.getStkVocation());
        em.persist(q);
        StlQuoteItem ligne = new StlQuoteItem();
        ligne.setId(UUID.randomUUID());
        ligne.setStlQuote(q);
        ligne.setStkItem(annexe);
        ligne.setCode("RVOL-" + jeton);
        ligne.setVatRatio(new BigDecimal("0.2"));
        ligne.setPriceBeforeDiscount(new BigDecimal(prixAnnexe));
        ligne.setPriceBeforeDiscountHT(new BigDecimal(prixAnnexe).divide(new BigDecimal("1.2"), 6,
                java.math.RoundingMode.HALF_UP));
        ligne.setDiscount(BigDecimal.ZERO);
        ligne.setPriceAfterDiscount(new BigDecimal(prixAnnexe));
        ligne.setPriceAfterDiscountHT(new BigDecimal(prixAnnexe).divide(new BigDecimal("1.2"), 6,
                java.math.RoundingMode.HALF_UP));
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

    private ResultActions creer(String corps, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(post(DOSSIERS).with(qui).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private int dossiersDeLaProspection(UUID prospection) {
        em.flush();
        Integer n = jdbc.queryForObject(COMPTE_PROSPECTION, Integer.class, prospection.toString());
        return n == null ? 0 : n;
    }

    private boolean lotLibre(UUID lot) {
        em.flush();
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM StkItem WHERE PKey = ? AND ComFolderId IS NULL",
                Integer.class, lot.toString());
        return n != null && n == 1;
    }

    private String intermediaire(UUID prospection) {
        em.flush();
        return jdbc.queryForObject("SELECT CAST(CfgIntermediateId AS NVARCHAR(36)) FROM ComProspection WHERE PKey = ?",
                String.class, prospection.toString());
    }

    // ------------------------------------------------------------------ décision 8 : lot sans prix catalogue

    @Test
    void d8_lotSansPrixCatalogue_400_aucunDossier_lotToujoursLibre() throws Exception {
        UUID lot = lot("D8", null, null).getId();
        UUID prospection = prospection("D8", vendeurA);

        creer(corps(prospection, "500000.00", null, lot), compteA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(SANS_PRIX));

        assertThat(dossiersDeLaProspection(prospection)).isZero();
        assertThat(lotLibre(lot)).isTrue();
    }

    @Test
    void d8_lotAPrixZero_400() throws Exception {
        UUID lot = lot("D8Z", BigDecimal.ZERO, BigDecimal.ZERO).getId();

        creer(corps(prospection("D8Z", vendeurA), "500000.00", null, lot), compteA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(SANS_PRIX));
    }

    // ------------------------------------------------------------------ décision 9 : l'offre n'abaisse pas le plancher

    @Test
    void d9_offreAnnexeMoinsChere_prixSousLeCatalogue_400() throws Exception {
        StkItem lot = lot("D9", PRIX_LOT, PRIX_LOT_HT);
        StkItem annexe = annexe("D9", "200000.00", "166666.666667");
        UUID prospection = prospection("D9", vendeurA);
        UUID offre = offre(prospection, lot, annexe, "50000.00");

        // total offre 1 050 000 < prix 1 100 000 < total catalogue 1 200 000
        creer(corps(prospection, "1100000.00", offre, lot.getId(), annexe.getId()), compteA)
                .andExpect(status().isBadRequest());

        assertThat(dossiersDeLaProspection(prospection)).isZero();
    }

    // ------------------------------------------------------------------ bornes du plancher

    @Test
    void plancher_unCentSous_400() throws Exception {
        StkItem lot = lot("PM", PRIX_LOT, PRIX_LOT_HT);

        creer(corps(prospection("PM", vendeurA), "999999.99", null, lot.getId()), compteA)
                .andExpect(status().isBadRequest());
    }

    @Test
    void plancher_exact_201() throws Exception {
        StkItem lot = lot("PE", PRIX_LOT, PRIX_LOT_HT);

        creer(corps(prospection("PE", vendeurA), "1000000.00", null, lot.getId()), compteA)
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ 409 même agence : aucune écriture

    @Test
    void conflit_lotDejaPrisParLaMemeAgence_aucunDossier_intermediaireNonPose() throws Exception {
        UUID lot = lot("C", PRIX_LOT, PRIX_LOT_HT).getId();
        creer(corps(prospection("C1", vendeurA), "1000000.00", null, lot), compteA).andExpect(status().isCreated());
        UUID seconde = prospection("C2", vendeurA2);

        creer(corps(seconde, "1000000.00", null, lot), compteA2).andExpect(status().isConflict());

        assertThat(dossiersDeLaProspection(seconde)).isZero();
        assertThat(intermediaire(seconde)).isNull();
    }

    // ------------------------------------------------------------------ R-D7 : refus sans prospect créé

    @Test
    void rD7_trancheHorsProjets_404_etAucunProspectCree() throws Exception {
        String nom = "RVN-" + jeton;

        mockMvc.perform(post(PROSPECTS).with(compteA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"%s\",\"prospection\":{\"trancheId\":\"%s\"}}".formatted(nom,
                                trancheQ.getId())))
                .andExpect(status().isNotFound());

        em.flush();
        Integer prospects = jdbc.queryForObject("SELECT COUNT(*) FROM CfgTier WHERE Description LIKE ?",
                Integer.class, "%" + nom + "%");
        assertThat(prospects).isZero();
    }

    // ------------------------------------------------------------------ R-D5 : POST /api/prospections

    @Test
    void rD5_postProspections_agence_intermediaireEstLAgence() throws Exception {
        UUID existante = prospection("PP", vendeurA);
        UUID prospect = UUID.fromString(jdbc.queryForObject(
                "SELECT CAST(CfgProspectId AS NVARCHAR(36)) FROM ComProspection WHERE PKey = ?", String.class,
                existante.toString()));
        jdbc.update("UPDATE ComProspection SET IsClosed = 1 WHERE CfgProspectId = ?", prospect.toString());
        em.clear();
        ComProspectionKind etape = new ComProspectionKind();
        etape.setId(UUID.randomUUID());
        etape.setCode("RVK-" + jeton);
        etape.setDescription("Qualifie");
        em.persist(etape);
        ComProspectionOrigin origine = new ComProspectionOrigin();
        origine.setId(UUID.randomUUID());
        origine.setCode("RVO-" + jeton);
        origine.setDescription("Facebook");
        em.persist(origine);
        ComActionMessageType type = new ComActionMessageType();
        type.setId(UUID.randomUUID());
        type.setCode("RVY-" + jeton);
        type.setDescription("Message entrant");
        em.persist(type);
        em.flush();

        String reponse = mockMvc.perform(post("/api/prospections").with(compteA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"prospectId":"%s","trancheId":"%s","origineId":"%s","typeContactId":"%s",\
                                "etapeId":"%s","notes":"x"}""".formatted(prospect, trancheP.getId(), origine.getId(),
                                type.getId(), etape.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        UUID creee = UUID.fromString(JsonPath.read(reponse, "$.id"));
        assertThat(intermediaire(creee)).isEqualToIgnoringCase(agenceA.toString());
    }
}
