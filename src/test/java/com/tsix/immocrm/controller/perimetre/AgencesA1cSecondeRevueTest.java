package com.tsix.immocrm.controller.perimetre;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureAgences;
import com.tsix.immocrm.fixtures.FixtureArticleOnglets;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seconde revue du lot A1c — FEATURE-agences-vente.md §4 (invariant : un compte agence ne voit rien qui ne soit
 * rattaché à son agence) et R-V8. Projet P affecté à l'agence A : dossier de A (vendeur vA), de B (vB), du siège (vS).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgencesA1cSecondeRevueTest {

    private static final String AGENCE = FixtureAgences.ROLE_AGENCE;
    private static final String CLIENTS = "/api/clients";
    private static final String DOSSIERS_ARTICLE = "/api/articles/{id}/dossiers";
    private static final String PROSPECTIONS = "/api/prospections";
    private static final String MILLE = "1000";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureAgences agences;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureArticleOnglets onglets;
    @Autowired private ComDocumentRepository documents;
    @Autowired private ComProspectionOriginRepository origines;
    @Autowired private ComActionMessageTypeRepository typesContact;
    @Autowired private ComProspectionKindRepository natures;

    private String jeton;
    private RequestPostProcessor siege;
    private CfgTranche trancheP;
    private CfgTranche trancheQ;
    private UUID vendeurA;
    private UUID vendeurB;
    private Dossier dossierA;
    private Dossier dossierSiege;
    private RequestPostProcessor compteA;

    /** Id et code gardés : {@code commercialDuDossier} vide le contexte de persistance. */
    private record Dossier(UUID id, String code) {
    }

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        siege = connecte(patrimoine.administrateur());
        trancheP = plans.tranche("AGP-" + jeton, true);
        trancheQ = plans.tranche("AGQ-" + jeton, true);

        UUID agenceA = agences.agence("AGA-" + jeton);
        UUID agenceB = agences.agence("AGB-" + jeton);
        vendeurA = agences.vendeur("VA-" + jeton);
        vendeurB = agences.vendeur("VB-" + jeton);
        UUID vendeurSiege = agences.vendeur("VS-" + jeton);
        agences.lier(agenceA, vendeurA);
        agences.lier(agenceB, vendeurB);

        dossierA = dossierVif(article(trancheP, "LA"), vendeurA);
        dossierVif(article(trancheP, "LB"), vendeurB);
        dossierSiege = dossierVif(article(trancheP, "LS"), vendeurSiege);

        compteA = compteAgence("CA", vendeurA);
    }

    // ------------------------------------------------------------------ fixtures

    private StkItem article(CfgTranche tranche, String prefixe) {
        return plans.article(tranche, prefixe + "-" + jeton, false, null);
    }

    private Dossier dossierVif(StkItem article, UUID vendeur) {
        ComFolder cree = plans.dossier(article, StatutDossier.RESERVATION, "Magenta", "Réservation");
        Dossier dossier = new Dossier(cree.getId(), cree.getCode());
        agences.commercialDuDossier(dossier.id(), vendeur);
        plans.comFolderId(article.getId(), dossier.id());
        return dossier;
    }

    private RequestPostProcessor compteAgence(String prefixe, UUID vendeur) {
        AdmUser compte = agences.compteAgence(prefixe + "-" + jeton, vendeur);
        plans.habiliter(compte, trancheP.getCfgProject());
        plans.habiliter(compte, trancheQ.getCfgProject());
        return FixtureDossierVente.connecte(compte, AGENCE);
    }

    private void echeance(UUID dossierId) {
        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(em.getReference(ComFolder.class, dossierId));
        echeance.setAmount(new BigDecimal(MILLE));
        documents.saveAndFlush(echeance);
    }

    private void client(UUID dossierId, UUID clientId) {
        em.find(ComFolder.class, dossierId).setCfgCustomer(em.find(CfgTier.class, clientId));
        em.flush();
    }

    private ComProspection prospection(String prefixe, UUID vendeur) {
        CfgTier prospect = vente.tier("PR" + prefixe + "-" + jeton);
        // Projet Q : une prospection ouverte sur P bloquerait la création (409 « déjà une prospection ouverte »).
        ComProspection p = onglets.prospection("PRO" + prefixe + "-" + jeton, article(trancheQ, "LP" + prefixe),
                trancheQ, prospect, FixtureArticleOnglets.PROSPECTION_VENTE);
        p.setCfgCommercial(em.find(CfgTier.class, vendeur));
        em.flush();
        return p;
    }

    private String corpsCreation(UUID prospectId) {
        String suffixe = UUID.randomUUID().toString().substring(0, 8);
        ComProspectionOrigin origine = new ComProspectionOrigin();
        origine.setId(UUID.randomUUID());
        origine.setCode("ORI-A1C-" + suffixe);
        origine.setDescription("Facebook");
        origines.save(origine);
        ComActionMessageType typeContact = new ComActionMessageType();
        typeContact.setId(UUID.randomUUID());
        typeContact.setCode("TYP-A1C-" + suffixe);
        typeContact.setDescription("Message entrant");
        typesContact.save(typeContact);
        ComProspectionKind nature = new ComProspectionKind();
        nature.setId(UUID.randomUUID());
        nature.setCode("NAT-A1C-" + suffixe);
        nature.setDescription("Qualifie");
        natures.save(nature);
        return corps().avec("prospectId", prospectId).avec("trancheId", trancheP.getId())
                .avec("commercialId", vendeurA).avec("origineId", origine.getId())
                .avec("typeContactId", typeContact.getId()).avec("etapeId", nature.getId())
                .avec("notes", "Interesse par un T3").json();
    }

    private long prospectionsDuProspect(UUID prospectId) {
        em.flush();
        em.clear();
        return em.createQuery("select count(p) from ComProspection p where p.cfgProspect.id = :id", Long.class)
                .setParameter("id", prospectId).getSingleResult();
    }

    // ------------------------------------------------------------------ lecture

    private String corps200(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private <T> List<T> champDuClient(String corps, UUID clientId, String champ) {
        return JsonPath.read(corps, "$.content[?(@.id == '%s')].%s".formatted(id(clientId), champ));
    }

    private String id(UUID id) {
        return id.toString().toLowerCase(Locale.ROOT);
    }

    private String clients(RequestPostProcessor qui, String recherche) throws Exception {
        return corps200(get(CLIENTS).param("search", recherche).with(qui));
    }

    private ResultActions creer(RequestPostProcessor qui, UUID prospectId) throws Exception {
        return mockMvc.perform(post(PROSPECTIONS).with(qui).contentType(MediaType.APPLICATION_JSON)
                .content(corpsCreation(prospectId)));
    }

    // ------------------------------------------------------------------ §4 / R-V8 : agrégats de la liste clients

    /** §4 : le dossier du siège du même client n'entre ni dans nbDossiers ni dans totalAPayer pour l'agence A. */
    @Test
    void listeClients_agregatsSansLeDossierHorsAgence() throws Exception {
        CfgTier acquereur = vente.tier("CLA-" + jeton);
        UUID clientId = acquereur.getId();
        String code = acquereur.getCode();
        client(dossierA.id(), clientId);
        client(dossierSiege.id(), clientId);
        echeance(dossierA.id());
        echeance(dossierSiege.id());

        String corps = clients(compteA, code);

        assertThat(champDuClient(corps, clientId, "nbDossiers")).singleElement()
                .satisfies(n -> assertThat(((Number) n).intValue()).isEqualTo(1));
        assertThat(champDuClient(corps, clientId, "totalAPayer")).singleElement()
                .satisfies(t -> assertThat(new BigDecimal(t.toString())).isEqualByComparingTo(MILLE));
    }

    @Test
    void listeClients_temoinSiege_deuxDossiersEtDeuxMille() throws Exception {
        CfgTier acquereur = vente.tier("CLS-" + jeton);
        UUID clientId = acquereur.getId();
        client(dossierA.id(), clientId);
        client(dossierSiege.id(), clientId);
        echeance(dossierA.id());
        echeance(dossierSiege.id());

        String corps = clients(siege, acquereur.getCode());

        assertThat(champDuClient(corps, clientId, "nbDossiers")).singleElement()
                .satisfies(n -> assertThat(((Number) n).intValue()).isEqualTo(2));
        assertThat(champDuClient(corps, clientId, "totalAPayer")).singleElement()
                .satisfies(t -> assertThat(new BigDecimal(t.toString())).isEqualByComparingTo("2000"));
    }

    // ------------------------------------------------------------------ §4 : onglet Dossiers d'un lot

    /** §4 : un dossier de location (ou syndic) n'est rattaché à aucune agence, donc absent pour un compte agence. */
    @Test
    void ongletDossiersLot_sansLocationNiSyndicPourUneAgence() throws Exception {
        StkItem lot = article(trancheP, "LL");
        String code = "LOC-" + jeton;
        onglets.location(code, lot.getId(), vente.statut(StatutDossier.RESERVATION).getId(),
                vente.tier("LOC-" + jeton).getId());

        assertThat(corps200(get(DOSSIERS_ARTICLE, lot.getId()).with(compteA))).doesNotContain(code);
    }

    @Test
    void ongletDossiersLot_temoinSiege_voitLaLocation() throws Exception {
        StkItem lot = article(trancheP, "LT");
        String code = "LOT-LOC-" + jeton;
        onglets.location(code, lot.getId(), vente.statut(StatutDossier.RESERVATION).getId(),
                vente.tier("LOT-" + jeton).getId());

        assertThat(corps200(get(DOSSIERS_ARTICLE, lot.getId()).with(siege))).contains(code);
    }

    // ------------------------------------------------------------------ §4 V6 : création sur un prospect invisible

    @Test
    void creerProspection_surUnProspectInvisible_404() throws Exception {
        UUID prospectB = prospection("B", vendeurB).getCfgProspect().getId();

        creer(compteA, prospectB).andExpect(status().isNotFound());

        assertThat(prospectionsDuProspect(prospectB)).isEqualTo(1);
    }

    @Test
    void creerProspection_temoinSurSonProspect_201() throws Exception {
        UUID prospectA = prospection("A", vendeurA).getCfgProspect().getId();

        creer(compteA, prospectA).andExpect(status().isCreated());

        assertThat(prospectionsDuProspect(prospectA)).isEqualTo(2);
    }
}
