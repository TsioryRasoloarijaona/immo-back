package com.tsix.immocrm.config.audit;

import com.tsix.immocrm.domain.audit.ActionAudit;
import com.tsix.immocrm.domain.audit.ChangementAudit;
import com.tsix.immocrm.domain.audit.DetailsAudit;
import com.tsix.immocrm.domain.audit.RegistreAuditOmran;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheStkItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine.Chaine;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheStkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import org.hibernate.engine.spi.SessionImplementor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FEATURE-patrimoine-audit-actions.md §2 et §8. La suite tourne dans une transaction annulée, que l'audit ne voit
 * jamais : {@link #simulerCommit()} rejoue ce que fait le commit (flush, puis rappels « avant fin de transaction »).
 * Deux tests passent par un vrai commit / un vrai rollback pour prouver le mécanisme complet.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditOmranEcouteurTest {

    private static final String AUTEUR = "Compte ADM-AUDIT";
    private static final String AJOUT = ActionAudit.AJOUT.code();
    private static final String SUPPRESSION = ActionAudit.SUPPRESSION.code();
    private static final String DESCRIPTION = "description";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private EntityManager em;
    @Autowired private AdmWebHistoricsRepository historiques;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private StkItemRepository itemRepository;
    @Autowired private CfgTrancheStkItemRepository trancheStkItemRepository;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private JdbcTemplate jdbc;

    private AdmUser admin;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-AUDIT");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Ce que la préparation a écrit est « déjà validé » : seul l'acte testé doit produire des lignes. */
    private void preparationValidee() {
        em.flush();
        em.clear();
        EcouteurAuditOmran.viderTampons();
    }

    private void simulerCommit() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + ROLE_ADMIN))));
        em.flush();
        em.unwrap(SessionImplementor.class).getActionQueue().beforeTransactionCompletion();
    }

    private List<AdmWebHistorics> lignes(UUID objetId) {
        return historiques.findByObjectId(objetId);
    }

    private static List<ChangementAudit> details(AdmWebHistorics ligne) {
        return DetailsAudit.lire(ligne.getDetails());
    }

    @Test
    void creationProgramme_uneLigneAdded_codeEtLibelleDeLEntite() throws Exception {
        CfgCompany entite = fixture.entite("ENT-AUDIT", true);
        preparationValidee();

        mockMvc.perform(post("/api/projets").with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("entiteId", entite.getId(), "code", "PRJ-AUDIT",
                                DESCRIPTION, "Programme audité"))))
                .andExpect(status().isCreated());
        simulerCommit();

        CfgProject cree = projectRepository.findFirstByCodeAndIdNot("PRJ-AUDIT", UUID.randomUUID()).orElseThrow();
        assertThat(lignes(cree.getId())).singleElement().satisfies(l -> {
            assertThat(l.getAction()).isEqualTo(AJOUT);
            assertThat(l.getObjectType()).isEqualTo("Yes.Cfg.CfgProject");
            assertThat(l.getObjectTypeLabel()).isEqualTo("Programme immobilier");
            assertThat(l.getObjectLabel()).isEqualTo("PRJ-AUDIT");
            assertThat(l.getDescription()).isEqualTo("PRJ-AUDIT");
            assertThat(l.getSystemVersion()).isEqualTo("Web: AyadERP");
            assertThat(l.getUserDescription()).isEqualTo(AUTEUR);
            assertThat(l.getActionDateTime()).isNotNull();
            assertThat(l.getParentObjectId()).isNull();
            assertThat(l.getAdmWebSessionsId()).isNull();
            assertThat(details(l))
                    .isNotEmpty()
                    .contains(new ChangementAudit("Code", "", "PRJ-AUDIT"))
                    // Clé étrangère : nom de navigation et libellé de la cible, jamais son GUID.
                    .contains(new ChangementAudit("CfgCompany", "", "Entité ENT-AUDIT"))
                    .noneMatch(c -> c.propriete().startsWith("User"));
        });
    }

    @Test
    void patchProgrammeSansChangement_aucuneLigne() throws Exception {
        CfgProject projet = fixture.projet("PRJ-AUDIT-SC", fixture.entite("ENT-AUDIT-SC", true));
        preparationValidee();

        mockMvc.perform(patch("/api/projets/{id}", projet.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(DESCRIPTION, "Programme PRJ-AUDIT-SC"))))
                .andExpect(status().isOk());
        simulerCommit();

        assertThat(lignes(projet.getId())).isEmpty();
    }

    @Test
    void patchProgrammeReel_uneLigneModified_seulementLesProprietesChangees() throws Exception {
        CfgProject projet = fixture.projet("PRJ-AUDIT-MOD", fixture.entite("ENT-AUDIT-MOD", true));
        preparationValidee();

        mockMvc.perform(patch("/api/projets/{id}", projet.getId()).with(connecte(admin, ROLE_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(DESCRIPTION, "Nouveau libellé"))))
                .andExpect(status().isOk());
        simulerCommit();

        assertThat(lignes(projet.getId())).singleElement().satisfies(l -> {
            assertThat(l.getAction()).isEqualTo("Modified");
            assertThat(details(l)).containsExactly(
                    new ChangementAudit("Description", "Programme PRJ-AUDIT-MOD", "Nouveau libellé"));
        });
    }

    @Test
    void prixArticleSeulementReformate_aucuneLigne() {
        StkItem article = fixture.article(fixture.chaine("AUDIT-PX"), "ART-AUDIT-PX");
        article.setComSalePriceHT(new BigDecimal("845000.000000"));
        itemRepository.save(article);
        preparationValidee();

        StkItem relu = itemRepository.findById(article.getId()).orElseThrow();
        relu.setComSalePriceHT(new BigDecimal("845000"));
        // Seule colonne réellement modifiée : exclue de l'audit (User*), elle force pourtant l'UPDATE.
        relu.setUserUpdate("quelqu'un");
        simulerCommit();

        assertThat(lignes(article.getId())).isEmpty();
    }

    @Test
    void prixArticleChange_ligneModifiedAvecValeursInvariantes() {
        StkItem article = fixture.article(fixture.chaine("AUDIT-PX2"), "ART-AUDIT-PX2");
        article.setComSalePriceHT(BigDecimal.ZERO);
        itemRepository.save(article);
        preparationValidee();

        StkItem relu = itemRepository.findById(article.getId()).orElseThrow();
        relu.setComSalePriceHT(new BigDecimal("845000.50"));
        simulerCommit();

        assertThat(lignes(article.getId())).singleElement().satisfies(l -> {
            assertThat(l.getObjectType()).isEqualTo("Yes.Stk.Building.Item");
            assertThat(l.getObjectTypeLabel()).isEqualTo("Article");
            assertThat(l.getObjectLabel()).isEqualTo("ART-AUDIT-PX2");
            assertThat(details(l)).singleElement().satisfies(c -> {
                assertThat(c.propriete()).isEqualTo("ComSalePriceHT");
                assertThat(new BigDecimal(c.ancienneValeur())).isEqualByComparingTo("0");
                assertThat(new BigDecimal(c.nouvelleValeur())).isEqualByComparingTo("845000.50");
            });
        });
    }

    @Test
    void sousArticle_typeUnderItemEtLibelleDescription() {
        Chaine chaine = fixture.chaine("AUDIT-SA");
        StkItem parent = fixture.article(chaine, "ART-AUDIT-SA");
        preparationValidee();

        StkItem sous = fixture.article(chaine, "SA-AUDIT");
        sous.setIsUnderItem(true);
        sous.setDescription("Parking SA-AUDIT");
        sous.setParent(itemRepository.getReferenceById(parent.getId()));
        itemRepository.save(sous);
        simulerCommit();

        assertThat(lignes(sous.getId())).singleElement().satisfies(l -> {
            assertThat(l.getAction()).isEqualTo(AJOUT);
            assertThat(l.getObjectType()).isEqualTo("Yes.Stk.Building.UnderItem");
            assertThat(l.getObjectTypeLabel()).isEqualTo("Sous article");
            assertThat(l.getObjectLabel()).isEqualTo("Parking SA-AUDIT");
            assertThat(details(l)).contains(new ChangementAudit("Parent", "", "ART-AUDIT-SA"));
        });
    }

    @Test
    void terrainDeTranche_ligneEnfantRattacheeALaTranche() {
        Chaine chaine = fixture.chaine("AUDIT-TT");
        StkItem terrain = fixture.article(chaine, "TER-AUDIT");
        CfgTranche tranche = chaine.tranche();
        preparationValidee();

        CfgTrancheStkItem lien = new CfgTrancheStkItem();
        lien.setId(UUID.randomUUID());
        lien.setCfgTranche(em.getReference(CfgTranche.class, tranche.getId()));
        lien.setStkItem(em.getReference(StkItem.class, terrain.getId()));
        lien.setSorting(1);
        trancheStkItemRepository.save(lien);
        simulerCommit();

        assertThat(lignes(lien.getId())).singleElement().satisfies(l -> {
            assertThat(l.getObjectType()).isEqualTo("Yes.Cfg.CfgTrancheStkItem");
            assertThat(l.getObjectTypeLabel()).isEqualTo("Tranche");
            assertThat(l.getParentObjectType()).isEqualTo("Yes.Cfg.CfgTranche");
            assertThat(l.getParentObjectId()).isEqualTo(tranche.getId());
        });
        assertThat(historiques.duObjet(tranche.getId(), RegistreAuditOmran.objectTypes(), Pageable.unpaged()))
                .extracting(AdmWebHistorics::getObjectId)
                .containsExactly(lien.getId());
    }

    @Test
    void suppressionArticle_detailAuNomCourtDeLaClasse() {
        StkItem article = fixture.article(fixture.chaine("AUDIT-DEL"), "ART-AUDIT-DEL");
        preparationValidee();

        itemRepository.deleteById(article.getId());
        simulerCommit();

        assertThat(lignes(article.getId())).singleElement().satisfies(l -> {
            assertThat(l.getAction()).isEqualTo(SUPPRESSION);
            assertThat(details(l)).containsExactly(new ChangementAudit("Item", "", "ART-AUDIT-DEL"));
        });
    }

    @Test
    void entiteHorsRegistre_aucuneLigne() {
        AdmCountry pays = fixture.pays("PAYS-AUDIT");
        simulerCommit();

        assertThat(lignes(pays.getId())).isEmpty();
    }

    @Test
    void creeEtModifieDansLaMemeTransaction_uneSeuleLigneAdded() {
        CfgCompany entite = fixture.entite("ENT-AUDIT-1T", true);
        em.flush();
        entite.setDescription("Libellé final");
        simulerCommit();

        assertThat(lignes(entite.getId())).singleElement().satisfies(l -> {
            assertThat(l.getAction()).isEqualTo(AJOUT);
            assertThat(details(l)).contains(new ChangementAudit("Description", "", "Libellé final"));
        });
    }

    // ------------------------------------------------------------ vrai commit, vrai rollback

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void vraiCommit_ecritLaLigneDansLaMemeTransaction_puisLaSuppression() {
        UUID id = UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(transactions);
        try {
            tx.executeWithoutResult(s -> companyRepository.save(entite(id, "ENT-AUDIT-COMMIT")));
            tx.executeWithoutResult(s -> companyRepository.deleteById(id));

            List<AdmWebHistorics> ecrites = lignes(id);
            assertThat(ecrites).extracting(AdmWebHistorics::getAction)
                    .containsExactlyInAnyOrder(AJOUT, SUPPRESSION);
            assertThat(ecrites).filteredOn(l -> SUPPRESSION.equals(l.getAction())).singleElement()
                    .satisfies(l -> assertThat(details(l))
                            .containsExactly(new ChangementAudit("CfgCompany", "", "Entité ENT-AUDIT-COMMIT")));
        } finally {
            jdbc.update("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id);
            jdbc.update("DELETE FROM CfgCompany WHERE PKey = ?", id);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void ecritureAnnulee_aucuneLigne() {
        UUID id = UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(transactions);
        try {
            tx.executeWithoutResult(s -> {
                companyRepository.save(entite(id, "ENT-AUDIT-RB"));
                em.flush();
                s.setRollbackOnly();
            });

            assertThat(lignes(id)).isEmpty();
            assertThat(companyRepository.existsById(id)).isFalse();
        } finally {
            jdbc.update("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id);
        }
    }

    private static CfgCompany entite(UUID id, String code) {
        CfgCompany entite = new CfgCompany();
        entite.setId(id);
        entite.setCode(code);
        entite.setDescription("Entité " + code);
        entite.setIsActive(true);
        return entite;
    }
}
