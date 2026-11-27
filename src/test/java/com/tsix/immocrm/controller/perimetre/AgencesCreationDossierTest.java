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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Création de dossier et de prospect par un compte agence — FEATURE-agences-vente.md §5 R-D1 à R-D7, §8 « Création »,
 * décisions du manager A2. Agences A (vendeurs VA, VA2) et B (VB) sur le projet de la tranche P ; tranche Q sur un
 * autre projet, non affecté ; chaque refus a son témoin siège (règle 6).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesCreationDossierTest {

    private static final String DOSSIERS = "/api/dossiers";
    private static final String PROSPECTS = "/api/prospects";
    private static final String DETAIL = "$.detail";
    private static final String MESSAGE_FERMEE = "Cette fonction n'est pas ouverte aux agences";
    private static final String MESSAGE_REMISE = "Une remise passe par une demande au siège";
    private static final BigDecimal PRIX_LOT = new BigDecimal("1000000.00");
    private static final BigDecimal PRIX_LOT_HT = new BigDecimal("833333.333333");

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private DataSource dataSource;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;
    @Autowired private FixtureStatutsDossier statuts;

    private JdbcTemplate jdbc;
    private String jeton;
    private RequestPostProcessor siege;
    private CfgTranche trancheP;
    private CfgTranche trancheQ;
    private UUID agenceA;
    private UUID vendeurA;
    private UUID vendeurA2;
    private UUID vendeurB;
    private UUID vendeurSiege;
    private UUID modePaie;
    private RequestPostProcessor compteA;
    private RequestPostProcessor compteA2;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        statuts.reservation();
        siege = connecte(patrimoine.administrateur());
        trancheP = plans.tranche("CDP-" + jeton, true);
        trancheQ = plans.tranche("CDQ-" + jeton, true);

        agenceA = agences.agence("CDA-" + jeton);
        UUID agenceB = agences.agence("CDB-" + jeton);
        vendeurA = agences.vendeur("CDVA-" + jeton);
        vendeurA2 = agences.vendeur("CDVA2-" + jeton);
        vendeurB = agences.vendeur("CDVB-" + jeton);
        vendeurSiege = agences.vendeur("CDVS-" + jeton);
        agences.lier(agenceA, vendeurA);
        agences.lier(agenceA, vendeurA2);
        agences.lier(agenceB, vendeurB);

        modePaie = UUID.randomUUID();
        em.flush();
        jdbc.update("INSERT INTO ComPaymentMethod (PKey, Code) VALUES (?, ?)", modePaie.toString(), "CDM-" + jeton);

        compteA = compteAgence("CDC", vendeurA);
        compteA2 = compteAgence("CDC2", vendeurA2);
    }

    // ------------------------------------------------------------------ fixtures

    private RequestPostProcessor compteAgence(String prefixe, UUID vendeur, String... autresRoles) {
        AdmUser compte = agences.compteAgence(prefixe + "-" + jeton, vendeur, autresRoles);
        plans.habiliter(compte, trancheP.getCfgProject());
        String[] roles = new String[autresRoles.length + 1];
        roles[0] = FixtureAgences.ROLE_AGENCE;
        System.arraycopy(autresRoles, 0, roles, 1, autresRoles.length);
        return FixtureDossierVente.connecte(compte, roles);
    }

    /** Lot libre de la tranche, prix de vente TTC 1 000 000 (plancher R-D4). */
    private UUID lotLibre(CfgTranche tranche, String suffixe) {
        StkItem article = plans.article(tranche, "CDL-" + suffixe + "-" + jeton, false, null);
        article.setVat(new BigDecimal("0.20"));
        article.setComSalePrice(PRIX_LOT);
        article.setComSalePriceHT(PRIX_LOT_HT);
        em.flush();
        return article.getId();
    }

    /** Prospection ouverte sur la tranche P, de commercial {@code vendeur}, pour un prospect neuf. */
    private UUID prospection(String suffixe, UUID vendeur) {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CDT-" + suffixe + "-" + jeton);
        prospect.setDescription("Prospect " + suffixe + " " + jeton);
        em.persist(prospect);
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("CDPS-" + suffixe + "-" + jeton);
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

    private String corps(UUID prospectionId, UUID lot, String prix, String remise) {
        return """
                {"prospectionId":"%s","articleIds":["%s"],"modePaieId":"%s","cin":"CIN-%s","prix":%s,"remisePercent":%s,\
                "commercialId":"%s"}""".formatted(prospectionId, lot, modePaie, jeton, prix, remise, vendeurSiege);
    }

    private ResultActions creer(String corps, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(post(DOSSIERS).with(qui).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private UUID creerDossier(String corps, RequestPostProcessor qui) throws Exception {
        String reponse = creer(corps, qui).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        em.flush();
        return UUID.fromString(JsonPath.read(reponse, "$.id"));
    }

    private UUID guid(String sql, Object... parametres) {
        em.flush();
        String valeur = jdbc.queryForObject(sql, String.class, parametres);
        return valeur == null ? null : UUID.fromString(valeur);
    }

    private UUID commercialDuDossier(UUID dossier) {
        return guid("SELECT CAST(CfgCommercialId AS NVARCHAR(36)) FROM ComFolder WHERE PKey = ?", dossier.toString());
    }

    private UUID intermediaire(UUID prospection) {
        return guid("SELECT CAST(CfgIntermediateId AS NVARCHAR(36)) FROM ComProspection WHERE PKey = ?",
                prospection.toString());
    }

    // ------------------------------------------------------------------ R-D2 : commercial forcé

    @Test
    void rD2_commercialForceAuVendeurDuCompte_ignoreCorpsEtProspection() throws Exception {
        UUID prospection = prospection("D2", vendeurA2);

        UUID dossier = creerDossier(corps(prospection, lotLibre(trancheP, "D2"), "1000000.00", "0"), compteA);

        assertThat(commercialDuDossier(dossier)).isEqualTo(vendeurA);
    }

    @Test
    void rD2_prospectionDeB_404() throws Exception {
        creer(corps(prospection("D2B", vendeurB), lotLibre(trancheP, "D2B"), "1000000.00", "0"), compteA)
                .andExpect(status().isNotFound());
    }

    @Test
    void rD2_temoinSiege_prospectionDeB_201() throws Exception {
        creer(corps(prospection("D2S", vendeurB), lotLibre(trancheP, "D2S"), "1000000.00", "0"), siege)
                .andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ R-D1 : lot libre dans un projet affecté

    @Test
    void rD1_lotHorsProjetAffecte_404() throws Exception {
        creer(corps(prospection("D1Q", vendeurA), lotLibre(trancheQ, "D1Q"), "1000000.00", "0"), compteA)
                .andExpect(status().isNotFound());
    }

    @Test
    void rD1_temoinSiege_lotHorsProjetAffecte_201() throws Exception {
        creer(corps(prospection("D1S", vendeurSiege), lotLibre(trancheQ, "D1S"), "1000000.00", "0"), siege)
                .andExpect(status().isCreated());
    }

    // Décision manager A2 (1) : deux comptes agence sur le même lot, appels séquentiels → un 201, un 409.
    @Test
    void rD1_deuxComptesAgenceMemeLot_un201_un409() throws Exception {
        UUID lot = lotLibre(trancheP, "D1C");
        creer(corps(prospection("D1C1", vendeurA), lot, "1000000.00", "0"), compteA).andExpect(status().isCreated());
        em.flush();

        creer(corps(prospection("D1C2", vendeurA2), lot, "1000000.00", "0"), compteA2)
                .andExpect(status().isConflict());
    }

    // Décision manager A2 (1) : le siège garde son 400 actuel sur un lot occupé.
    @Test
    void rD1_temoinSiege_lotOccupe_400Inchange() throws Exception {
        UUID lot = lotLibre(trancheP, "D1O");
        creer(corps(prospection("D1O1", vendeurSiege), lot, "1000000.00", "0"), siege).andExpect(status().isCreated());
        em.flush();

        creer(corps(prospection("D1O2", vendeurSiege), lot, "1000000.00", "0"), siege)
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ R-D3 : Réservation, lot bloqué

    @Test
    void rD3_statutReservation_etStkItemComFolderIdPose() throws Exception {
        UUID lot = lotLibre(trancheP, "D3");
        String reponse = creer(corps(prospection("D3", vendeurA), lot, "1000000.00", "0"), compteA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("RESERVATION"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID dossier = UUID.fromString(JsonPath.read(reponse, "$.id"));

        assertThat(guid("SELECT CAST(ComFolderId AS NVARCHAR(36)) FROM StkItem WHERE PKey = ?", lot.toString()))
                .isEqualTo(dossier);
    }

    // ------------------------------------------------------------------ R-D4 : plancher, pas de remise

    @Test
    void rD4_remiseNonNulle_400() throws Exception {
        creer(corps(prospection("D4R", vendeurA), lotLibre(trancheP, "D4R"), "1000000.00", "5"), compteA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(MESSAGE_REMISE));
    }

    @Test
    void rD4_temoinSiege_remiseNonNulle_201() throws Exception {
        creer(corps(prospection("D4RS", vendeurSiege), lotLibre(trancheP, "D4RS"), "1000000.00", "5"), siege)
                .andExpect(status().isCreated());
    }

    @Test
    void rD4_prixSousLePlancher_400() throws Exception {
        creer(corps(prospection("D4P", vendeurA), lotLibre(trancheP, "D4P"), "900000.00", "0"), compteA)
                .andExpect(status().isBadRequest());
    }

    @Test
    void rD4_temoinSiege_prixSousLePlancher_201() throws Exception {
        creer(corps(prospection("D4PS", vendeurSiege), lotLibre(trancheP, "D4PS"), "900000.00", "0"), siege)
                .andExpect(status().isCreated());
    }

    @Test
    void rD4_prixAuDessusDuPlancher_201() throws Exception {
        creer(corps(prospection("D4H", vendeurA), lotLibre(trancheP, "D4H"), "1100000.00", "0"), compteA)
                .andExpect(status().isCreated());
    }

    // Décision manager A2 (2) : plancher = total de référence de la création (lot + annexe 200 000).
    @Test
    void rD4_plancherInclutLAnnexe_400() throws Exception {
        UUID lot = lotLibre(trancheP, "D4A");
        StkItem annexe = plans.article(trancheP, "CDL-D4A-PARK-" + jeton, false, null);
        annexe.setComSalePrice(new BigDecimal("200000.00"));
        annexe.setComSalePriceHT(new BigDecimal("166666.666667"));
        em.flush();
        String corps = """
                {"prospectionId":"%s","articleIds":["%s","%s"],"modePaieId":"%s","cin":"CIN-%s","prix":1100000.00,\
                "remisePercent":0}""".formatted(prospection("D4A", vendeurA), lot, annexe.getId(), modePaie, jeton);

        creer(corps, compteA).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ R-D5 : CfgIntermediateId

    @Test
    void rD5_prospectionRecoitLAgenceCommeIntermediaire() throws Exception {
        UUID prospection = prospection("D5", vendeurA);

        creerDossier(corps(prospection, lotLibre(trancheP, "D5"), "1000000.00", "0"), compteA);

        assertThat(intermediaire(prospection)).isEqualTo(agenceA);
    }

    @Test
    void rD5_temoinSiege_intermediaireNonPose() throws Exception {
        UUID prospection = prospection("D5S", vendeurSiege);

        creerDossier(corps(prospection, lotLibre(trancheP, "D5S"), "1000000.00", "0"), siege);

        assertThat(intermediaire(prospection)).isNull();
    }

    // Réponse du manager (27/11) n°4 : toute prospection créée par un compte agence reçoit l'agence.
    @Test
    void rD5_postProspectionsParAgence_intermediaireAgence() throws Exception {
        UUID prospect = guid("SELECT CAST(CfgProspectId AS NVARCHAR(36)) FROM ComProspection WHERE PKey = ?",
                prospection("D5P", vendeurA).toString());

        UUID creee = creerProspection(prospect, compteA);

        assertThat(intermediaire(creee)).isEqualTo(agenceA);
    }

    @Test
    void rD5_temoinSiege_postProspections_intermediaireNonPose() throws Exception {
        UUID prospect = guid("SELECT CAST(CfgProspectId AS NVARCHAR(36)) FROM ComProspection WHERE PKey = ?",
                prospection("D5PS", vendeurSiege).toString());

        UUID creee = creerProspection(prospect, siege);

        assertThat(intermediaire(creee)).isNull();
    }

    /** La prospection d'origine (qui rend le prospect visible à l'agence) est clôturée : sinon 409 doublon ouvert. */
    private UUID creerProspection(UUID prospect, RequestPostProcessor qui) throws Exception {
        em.flush();
        jdbc.update("UPDATE ComProspection SET IsClosed = 1 WHERE CfgProspectId = ?", prospect.toString());
        em.clear();
        ComProspectionKind etape = new ComProspectionKind();
        etape.setId(UUID.randomUUID());
        etape.setCode("CDK-" + UUID.randomUUID().toString().substring(0, 8));
        etape.setDescription("Qualifie");
        em.persist(etape);
        em.flush();
        String corps = """
                {"prospectId":"%s","trancheId":"%s","origineId":"%s","typeContactId":"%s","etapeId":"%s",\
                "notes":"Interesse"}""".formatted(prospect, trancheP.getId(), origine().getId(), typeContact().getId(),
                etape.getId());
        String reponse = mockMvc.perform(post("/api/prospections").with(qui).contentType(MediaType.APPLICATION_JSON)
                        .content(corps))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        em.flush();
        return UUID.fromString(JsonPath.read(reponse, "$.id"));
    }

    // ------------------------------------------------------------------ manquants du lot A2 (réponses du manager)

    @Test
    void rD2_prospectionIdAbsent_400() throws Exception {
        String corps = """
                {"articleIds":["%s"],"modePaieId":"%s","cin":"CIN-%s","prix":1000000.00,"remisePercent":0}"""
                .formatted(lotLibre(trancheP, "D2N"), modePaie, jeton);

        creer(corps, compteA).andExpect(status().isBadRequest());
    }

    @Test
    void rD1_compteAgenceSansProjet_404() throws Exception {
        AdmUser sansProjet = agences.compteAgence("CDSP-" + jeton, vendeurA);
        RequestPostProcessor qui = FixtureDossierVente.connecte(sansProjet, FixtureAgences.ROLE_AGENCE);

        creer(corps(prospection("D1SP", vendeurA), lotLibre(trancheP, "D1SP"), "1000000.00", "0"), qui)
                .andExpect(status().isNotFound());
    }

    // §8 : un compte Agence + Administrateur est traité comme agence (remise refusée).
    @Test
    void rD4_compteAgenceAdministrateur_traiteCommeAgence_remise400() throws Exception {
        RequestPostProcessor agenceAdmin = compteAgence("CDADM", vendeurA, "Administrateur");

        creer(corps(prospection("D4AD", vendeurA), lotLibre(trancheP, "D4AD"), "1000000.00", "5"), agenceAdmin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(MESSAGE_REMISE));
    }

    // ------------------------------------------------------------------ R-INS3 : compte incomplet

    @Test
    void rIns3_compteSansVendeur_403QuiDitQuoiCompleter() throws Exception {
        RequestPostProcessor sansVendeur = compteAgence("CDN", null);

        String detail = JsonPath.read(creer(corps(prospection("I3", vendeurA), lotLibre(trancheP, "I3"), "1000000.00",
                        "0"), sansVendeur)
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), DETAIL);

        assertThat(detail).isNotBlank().isNotEqualTo(MESSAGE_FERMEE);
    }

    // ------------------------------------------------------------------ R-D7 : prospect créé avec sa prospection

    private ComProspectionOrigin origine() {
        ComProspectionOrigin origine = new ComProspectionOrigin();
        origine.setId(UUID.randomUUID());
        origine.setCode("CDO-" + UUID.randomUUID().toString().substring(0, 8));
        origine.setDescription("Facebook");
        em.persist(origine);
        return origine;
    }

    private ComActionMessageType typeContact() {
        ComActionMessageType type = new ComActionMessageType();
        type.setId(UUID.randomUUID());
        type.setCode("CDY-" + UUID.randomUUID().toString().substring(0, 8));
        type.setDescription("Message entrant");
        em.persist(type);
        return type;
    }

    private String corpsProspect(String nom, String prospection) {
        return prospection == null
                ? "{\"nom\":\"%s\"}".formatted(nom)
                : "{\"nom\":\"%s\",\"prospection\":%s}".formatted(nom, prospection);
    }

    private ResultActions creerProspect(String corps, RequestPostProcessor qui) throws Exception {
        return mockMvc.perform(post(PROSPECTS).with(qui).contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    @Test
    void rD7_prospectAgence_prospectionCreeeVendeurEtAgence_ficheVisible() throws Exception {
        // Réponse du manager (27/11) n°1 : etapeId absent, le back prend l'étape initiale.
        String reponse = creerProspect(corpsProspect(jeton + "-D7",
                        "{\"trancheId\":\"%s\",\"origineId\":\"%s\",\"typeContactId\":\"%s\",\"notes\":\"Agence\"}"
                                .formatted(trancheP.getId(), origine().getId(), typeContact().getId())), compteA)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID prospect = UUID.fromString(JsonPath.read(reponse, "$.id"));
        em.flush();

        Integer prospections = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ComProspection
                WHERE CfgProspectId = ? AND CfgCommercialId = ? AND CfgIntermediateId = ?""", Integer.class,
                prospect.toString(), vendeurA.toString(), agenceA.toString());
        assertThat(prospections).isEqualTo(1);
        mockMvc.perform(get(PROSPECTS + "/{id}", prospect).with(compteA)).andExpect(status().isOk());
    }

    @Test
    void rD7_prospectAgenceSansProspection_400() throws Exception {
        creerProspect(corpsProspect(jeton + "-D7N", null), compteA).andExpect(status().isBadRequest());
    }

    @Test
    void rD7_prospectAgenceProspectionSansTranche_400() throws Exception {
        creerProspect(corpsProspect(jeton + "-D7V", "{}"), compteA).andExpect(status().isBadRequest());
    }

    @Test
    void rD7_prospectAgenceTrancheHorsProjetsAffectes_404() throws Exception {
        creerProspect(corpsProspect(jeton + "-D7Q", "{\"trancheId\":\"%s\"}".formatted(trancheQ.getId())), compteA)
                .andExpect(status().isNotFound());
    }

    @Test
    void rD7_temoinSiege_prospectSansProspection_201() throws Exception {
        creerProspect(corpsProspect(jeton + "-D7S", null), siege).andExpect(status().isCreated());
    }
}
