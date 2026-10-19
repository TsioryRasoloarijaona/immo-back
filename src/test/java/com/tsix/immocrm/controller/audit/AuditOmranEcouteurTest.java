package com.tsix.immocrm.controller.audit;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheStkItem;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheStkItemRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.ENTITE_STK_ITEM;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.NATURE_BAT;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.VOCATION_PROJET;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.VOCATION_VENTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Écouteur d'audit OMRAN — FEATURE-patrimoine-audit-actions.md §2.1, §2.2, §8 (AuditOmranEcouteurTest).
 *
 * <p>Sans {@code @Transactional} : le spec autorise un tampon vidé en {@code beforeCommit}, la ligne d'audit
 * n'existe qu'après validation. Chaque test valide donc ses écritures et les efface en {@code @AfterEach}
 * (SQL natif, invisible de l'écouteur). Codes suffixés d'un aléa : yesRemsTest est partagée avec d'autres lots.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditOmranEcouteurTest {

    private static final Logger log = LoggerFactory.getLogger(AuditOmranEcouteurTest.class);

    private static final String PROJETS = "/api/projets";
    private static final String PROJET = "/api/projets/{id}";
    private static final String ARTICLES = "/api/articles";
    private static final String ARTICLE = "/api/articles/{id}";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String ADDED = "Added";
    private static final String MODIFIED = "Modified";
    private static final String ACTION = "Action";
    private static final String DETAILS = "Details";
    private static final String OBJECT_TYPE = "ObjectType";
    private static final String TYPE_PROGRAMME = "Yes.Cfg.CfgProject";
    private static final String TYPE_TRANCHE = "Yes.Cfg.CfgTranche";
    private static final String PRIX_PROVISOIRE_HT = "prixM2ProvisoireHT";
    private static final String PRIX_DEFINITIF_HT = "prixM2DefinitifHT";
    private static final String TABLE_STK_ITEM = "StkItem";
    private static final String PROP_DESCRIPTION = "Description";
    private static final String OBJECT_LABEL = "ObjectLabel";
    private static final String OBJECT_TYPE_LABEL = "ObjectTypeLabel";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheStkItemRepository trancheArticleRepository;
    @Autowired private ComActionMessageRepository actionRepository;

    private final String suffixe = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    /** Lignes validées par le test, effacées en ordre inverse (enfant avant parent). */
    private final Deque<LigneCreee> creees = new ArrayDeque<>();
    private final List<UUID> objets = new ArrayList<>();

    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private AdmUser admin;
    private CfgCompany entite;
    private StkFloor etage;

    private record LigneCreee(String table, UUID id) {
    }

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        admin = fixture.administrateur();
        suivre("AdmUser", admin.getId());
        entite = commeAdmin(() -> fixture.entite("ENT-AUD-" + suffixe, true));
        suivre("CfgCompany", entite.getId());
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        objets.forEach(id -> {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", id, id);
            effacer("DELETE FROM AdmMessage WHERE CategoryId = ?", id);
        });
        if (admin != null) {
            effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId());
        }
        while (!creees.isEmpty()) {
            LigneCreee ligne = creees.pop();
            effacer("DELETE FROM " + ligne.table() + " WHERE PKey = ?", ligne.id());
        }
    }

    /** Nettoyage au mieux : une table annexe inconnue (FK en CI) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, Arrays.stream(parametres).map(Object::toString).toArray());
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private void suivre(String table, UUID id) {
        creees.push(new LigneCreee(table, id));
        objets.add(id);
    }

    // ------------------------------------------------------------------ outils

    private Authentication authentificationAdmin() {
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        return new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur")));
    }

    /** Écriture JPA validée, au nom de l'admin (MockMvc vide le contexte de sécurité après chaque requête). */
    private <T> T commeAdmin(Supplier<T> ecriture) {
        SecurityContextHolder.getContext().setAuthentication(authentificationAdmin());
        return transactions.execute(statut -> ecriture.get());
    }

    private ResultActions envoyer(MockHttpServletRequestBuilder requete, CorpsJson corps) throws Exception {
        return mockMvc.perform(requete.with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private UUID creer(String url, CorpsJson corps, String table) throws Exception {
        String json = envoyer(post(url), corps).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));
        suivre(table, id);
        return id;
    }

    private UUID creerProgramme(String code) throws Exception {
        return creer(PROJETS, corps().avec("entiteId", entite.getId()).avec(CODE, code)
                .avec(DESCRIPTION, "Programme " + code), "CfgProject");
    }

    private CfgProject programmeFixture(String code) {
        CfgProject projet = commeAdmin(() -> fixture.projet(code, entite));
        suivre("CfgProject", projet.getId());
        return projet;
    }

    private List<Map<String, Object>> lignes(UUID objetId) {
        return jdbc.queryForList("""
                        SELECT Action, ObjectType, ObjectTypeLabel, ObjectLabel, Description,
                               CAST(Details AS nvarchar(max)) AS Details, ParentObjectType,
                               CONVERT(varchar(36), ParentObjectId) AS ParentObjectId, UserDescription, SystemVersion,
                               ActionDateTime, AdmWebSessionsId, LockEntityId
                        FROM AdmWebHistorics WHERE ObjectId = ?""", objetId.toString());
    }

    private List<Map<String, Object>> lignes(UUID objetId, String action) {
        return lignes(objetId).stream().filter(l -> action.equals(l.get(ACTION))).toList();
    }

    private Map<String, Object> seuleLigne(UUID objetId, String action) {
        List<Map<String, Object>> trouvees = lignes(objetId, action);
        assertThat(trouvees).as("lignes %s de %s", action, objetId).hasSize(1);
        return trouvees.get(0);
    }

    /** {@code [propriété, ancienne, nouvelle]} ; le 4ᵉ élément facultatif (code de ressource) est ignoré. */
    private static List<List<String>> triplets(Object details) {
        List<List<Object>> brut = JsonPath.read((String) details, "$");
        return brut.stream().map(e -> List.of(String.valueOf(e.get(0)), String.valueOf(e.get(1)), String.valueOf(e.get(2))))
                .toList();
    }

    private static boolean memeValeur(String a, String b) {
        try {
            return new BigDecimal(a).compareTo(new BigDecimal(b)) == 0;
        } catch (NumberFormatException e) {
            return a.equals(b);
        }
    }

    // ------------------------------------------------------------------ §2.2.2 création de programme

    @Test
    void r2_2_creationProgramme_uneLigneAdded_colonnesOmran() throws Exception {
        String code = "PRJ-AUD-ADD-" + suffixe;
        UUID id = creerProgramme(code);

        assertThat(lignes(id)).singleElement().satisfies(l -> assertAll(
                () -> assertThat(l.get(ACTION)).isEqualTo(ADDED),
                () -> assertThat(l.get(OBJECT_TYPE)).isEqualTo(TYPE_PROGRAMME),
                () -> assertThat(l.get(OBJECT_TYPE_LABEL)).isEqualTo("Programme immobilier"),
                () -> assertThat(l.get(OBJECT_LABEL)).isEqualTo(code),
                () -> assertThat(l.get(PROP_DESCRIPTION)).isEqualTo(code),
                () -> assertThat(l.get("UserDescription")).isEqualTo(AUTEUR),
                () -> assertThat(l.get("SystemVersion")).isEqualTo("Web: AyadERP"),
                () -> assertThat(l.get("ActionDateTime")).isNotNull(),
                () -> assertThat(l.get("ParentObjectType")).isNull(),
                () -> assertThat(l.get("ParentObjectId")).isNull(),
                () -> assertThat(l.get("AdmWebSessionsId")).isNull(),
                () -> assertThat(l.get("LockEntityId")).isNull()));
    }

    @Test
    void r2_2_3_added_detailsAvecLeCode_ancienneValeurVide_proprietesNonVides() throws Exception {
        String code = "PRJ-AUD-DET-" + suffixe;
        UUID id = creerProgramme(code);

        List<List<String>> details = triplets(seuleLigne(id, ADDED).get(DETAILS));
        assertThat(details)
                .contains(List.of("Code", "", code))
                .allSatisfy(t -> assertAll(
                        () -> assertThat(t.get(1)).as("ancienne de %s", t.get(0)).isEmpty(),
                        () -> assertThat(t.get(2)).as("nouvelle de %s", t.get(0)).isNotEmpty(),
                        () -> assertThat(t.get(0)).isNotEqualTo("PKey").doesNotStartWith("User")));
    }

    /** Ligne prod : {@code ["WorkStartDate","","01/06/2025 00:00:00"]}. */
    @Test
    void r2_2_3_added_dateAuFormatOmran() throws Exception {
        UUID id = creer(PROJETS, corps().avec("entiteId", entite.getId()).avec(CODE, "PRJ-AUD-DATE-" + suffixe)
                .avec(DESCRIPTION, "Programme date").avec("dateDebutTravaux", "2026-01-15T00:00:00"), "CfgProject");

        assertThat(triplets(seuleLigne(id, ADDED).get(DETAILS)))
                .contains(List.of("WorkStartDate", "", "15/01/2026 00:00:00"));
    }

    @Test
    void r2_2_3_cleEtrangere_libelleDeLaCibleEtJamaisSonGuid() throws Exception {
        UUID id = creerProgramme("PRJ-AUD-FK-" + suffixe);

        List<List<String>> details = triplets(seuleLigne(id, ADDED).get(DETAILS));
        assertThat(details).contains(List.of("CfgCompany", "", entite.getDescription()));
        assertThat(details).flatExtracting(t -> t)
                .noneMatch(v -> v.equalsIgnoreCase(entite.getId().toString()));
    }

    // ------------------------------------------------------------------ §2.2.3 modification

    @Test
    void r2_2_3_patchSansChangement_aucuneLigne() throws Exception {
        String code = "PRJ-AUD-NOP-" + suffixe;
        UUID id = creerProgramme(code);

        envoyer(patch(PROJET, id), corps().avec(DESCRIPTION, "Programme " + code)).andExpect(status().isOk());

        assertThat(lignes(id, MODIFIED)).isEmpty();
    }

    @Test
    void r2_2_3_patchReel_uneLigneModified_seulementLaProprieteChangee() throws Exception {
        String code = "PRJ-AUD-MOD-" + suffixe;
        UUID id = creerProgramme(code);

        envoyer(patch(PROJET, id), corps().avec(DESCRIPTION, "Nouveau nom")).andExpect(status().isOk());

        Map<String, Object> ligne = seuleLigne(id, MODIFIED);
        assertAll(
                () -> assertThat(ligne.get(OBJECT_TYPE)).isEqualTo(TYPE_PROGRAMME),
                () -> assertThat(ligne.get(OBJECT_LABEL)).isEqualTo(code),
                () -> assertThat(triplets(ligne.get(DETAILS)))
                        .containsExactly(List.of(PROP_DESCRIPTION, "Programme " + code, "Nouveau nom")));
    }

    @Test
    void r2_2_3_modificationDeCleEtrangere_ancienEtNouveauLibelles() throws Exception {
        UUID id = creerProgramme("PRJ-AUD-FKMOD-" + suffixe);
        CfgCompany autre = commeAdmin(() -> fixture.entite("ENT-AUD-AUTRE-" + suffixe, true));
        suivre("CfgCompany", autre.getId());

        envoyer(patch(PROJET, id), corps().avec("entiteId", autre.getId())).andExpect(status().isOk());

        assertThat(triplets(seuleLigne(id, MODIFIED).get(DETAILS)))
                .contains(List.of("CfgCompany", entite.getDescription(), autre.getDescription()));
    }

    // ------------------------------------------------------------------ §2.2.4 une ligne par objet et par transaction

    @Test
    void r2_2_4_deuxEcrituresDansUneTransaction_uneSeuleLigne_ancienEtatDuDebut() {
        CfgProject projet = programmeFixture("PRJ-AUD-2SAVE-" + suffixe);
        String initiale = projet.getDescription();

        commeAdmin(() -> {
            CfgProject p = projectRepository.findById(projet.getId()).orElseThrow();
            p.setDescription("Etape 1");
            projectRepository.saveAndFlush(p);
            p.setDescription("Etape 2");
            return projectRepository.saveAndFlush(p);
        });

        assertThat(triplets(seuleLigne(projet.getId(), MODIFIED).get(DETAILS)))
                .containsExactly(List.of(PROP_DESCRIPTION, initiale, "Etape 2"));
    }

    // ------------------------------------------------------------------ §2.2.1 même transaction

    @Test
    void r2_2_1_transactionAnnulee_aucuneLigne() {
        CfgProject projet = programmeFixture("PRJ-AUD-RB-" + suffixe);

        SecurityContextHolder.getContext().setAuthentication(authentificationAdmin());
        transactions.executeWithoutResult(statut -> {
            CfgProject p = projectRepository.findById(projet.getId()).orElseThrow();
            p.setDescription("Jamais validee");
            projectRepository.saveAndFlush(p);
            statut.setRollbackOnly();
        });

        assertThat(lignes(projet.getId(), MODIFIED)).isEmpty();
    }

    @Test
    void r2_2_1_ecritureRefusee409_aucuneLigne() throws Exception {
        CfgProject pris = programmeFixture("PRJ-AUD-PRIS-" + suffixe);
        UUID id = creerProgramme("PRJ-AUD-409-" + suffixe);

        envoyer(patch(PROJET, id), corps().avec(DESCRIPTION, "Ne doit pas rester").avec(CODE, pris.getCode()))
                .andExpect(status().isConflict());

        assertThat(lignes(id, MODIFIED)).isEmpty();
    }

    // ------------------------------------------------------------------ §2.2.3 suppression

    /** {@code AdmWebHistorics.cs:HistorizeDelete} : {@code [[NomDeClasse, "", ObjectLabel]]}. */
    @Test
    void r2_2_3_suppression_ligneDeleted_nomDeClasseEtLibelle() {
        String code = "PRJ-AUD-DEL-" + suffixe;
        CfgProject projet = programmeFixture(code);

        commeAdmin(() -> {
            projectRepository.deleteById(projet.getId());
            return null;
        });

        Map<String, Object> ligne = seuleLigne(projet.getId(), "Deleted");
        assertAll(
                () -> assertThat(ligne.get(OBJECT_TYPE)).isEqualTo(TYPE_PROGRAMME),
                () -> assertThat(ligne.get(OBJECT_LABEL)).isEqualTo(code),
                () -> assertThat(triplets(ligne.get(DETAILS))).containsExactly(List.of("CfgProject", "", code)));
    }

    // ------------------------------------------------------------------ §2.1 registre : tranche, enfants, hors registre

    @Test
    void r2_1_creationTranche_typeTranche_decimalAvecPoint() throws Exception {
        UUID projet = creerProgramme("PRJ-AUD-TR-" + suffixe);
        String code = "TR-AUD-" + suffixe;

        UUID id = creer("/api/tranches", corps().avec("projetId", projet).avec(CODE, code)
                .avec(DESCRIPTION, "Tranche " + code).avec("advancePercent", new BigDecimal("0.3")), "CfgTranche");

        Map<String, Object> ligne = seuleLigne(id, ADDED);
        assertAll(
                () -> assertThat(ligne.get(OBJECT_TYPE)).isEqualTo(TYPE_TRANCHE),
                () -> assertThat(ligne.get(OBJECT_TYPE_LABEL)).isEqualTo("Tranche"),
                () -> assertThat(ligne.get(OBJECT_LABEL)).isEqualTo(code),
                () -> assertThat(triplets(ligne.get(DETAILS)))
                        .filteredOn(t -> "AdvancePercent".equals(t.get(0)))
                        .singleElement().satisfies(t -> assertThat(t.get(2)).matches("0\\.30*")));
    }

    @Test
    void r2_1_ligneEnfantTrancheArticle_parentEstLaTranche() {
        CfgProject projet = programmeFixture("PRJ-AUD-ENF-" + suffixe);
        CfgTranche tranche = commeAdmin(() -> fixture.tranche("TR-AUD-ENF-" + suffixe, projet));
        suivre("CfgTranche", tranche.getId());

        CfgTrancheStkItem lien = commeAdmin(() -> {
            CfgTrancheStkItem l = new CfgTrancheStkItem();
            l.setId(UUID.randomUUID());
            l.setCfgTranche(tranche);
            l.setSorting(1);
            return trancheArticleRepository.save(l);
        });
        suivre("CfgTrancheStkItem", lien.getId());

        Map<String, Object> ligne = seuleLigne(lien.getId(), ADDED);
        assertAll(
                () -> assertThat(ligne.get(OBJECT_TYPE)).isEqualTo("Yes.Cfg.CfgTrancheStkItem"),
                () -> assertThat(ligne.get(OBJECT_TYPE_LABEL)).isEqualTo("Tranche"),
                () -> assertThat(ligne.get("ParentObjectType")).isEqualTo(TYPE_TRANCHE),
                () -> assertThat(FixturePatrimoine.guid(ligne.get("ParentObjectId")))
                        .isEqualTo(FixturePatrimoine.guid(tranche.getId())));
    }

    @Test
    void r2_1_actionHorsRegistre_aucuneLigne() {
        ComActionMessage action = commeAdmin(() -> {
            ComActionMessage m = new ComActionMessage();
            m.setId(UUID.randomUUID());
            m.setAlertMessage("Action hors registre");
            return actionRepository.save(m);
        });
        suivre("ComActionMessage", action.getId());

        assertThat(lignes(action.getId())).isEmpty();
    }

    @Test
    void r2_1_noteHorsRegistre_aucuneLigne() throws Exception {
        UUID projet = creerProgramme("PRJ-AUD-NOTE-" + suffixe);

        envoyer(post("/api/notes"), corps().avec("porteur", "projet").avec("id", projet).avec("texte", "Note"))
                .andExpect(status().isCreated());

        List<String> notes = jdbc.queryForList("SELECT CONVERT(varchar(36), PKey) FROM AdmMessage WHERE CategoryId = ?",
                String.class, projet.toString());
        assertThat(notes).singleElement()
                .satisfies(note -> assertThat(lignes(UUID.fromString(note))).isEmpty());
    }

    // ------------------------------------------------------------------ §1 / §8 articles

    private CorpsJson preparerArticle(String code) {
        boolean[] absents = {
                jdbc.queryForObject("SELECT COUNT(*) FROM StrEntity WHERE PKey = ?", Integer.class,
                        ENTITE_STK_ITEM.toString()) == 0,
                jdbc.queryForObject("SELECT COUNT(*) FROM StkKind WHERE PKey = ?", Integer.class,
                        NATURE_BAT.toString()) == 0,
                jdbc.queryForObject("SELECT COUNT(*) FROM StkVocation WHERE PKey = ?", Integer.class,
                        VOCATION_VENTE.toString()) == 0,
                jdbc.queryForObject("SELECT COUNT(*) FROM StkVocation WHERE PKey = ?", Integer.class,
                        VOCATION_PROJET.toString()) == 0};
        commeAdmin(() -> {
            fixture.referentielArticle();
            return null;
        });
        if (absents[0]) {
            creees.push(new LigneCreee("StrEntity", ENTITE_STK_ITEM));
        }
        if (absents[1]) {
            creees.push(new LigneCreee("StkKind", NATURE_BAT));
        }
        if (absents[2]) {
            creees.push(new LigneCreee("StkVocation", VOCATION_VENTE));
        }
        if (absents[3]) {
            creees.push(new LigneCreee("StkVocation", VOCATION_PROJET));
        }
        CfgProject projet = programmeFixture("PRJ-AUD-ART-" + suffixe);
        CfgTranche tranche = commeAdmin(() -> fixture.tranche("TR-AUD-ART-" + suffixe, projet));
        suivre("CfgTranche", tranche.getId());
        StkBuilding immeuble = commeAdmin(() -> fixture.immeuble("IMM-AUD-" + suffixe));
        suivre("StkBuilding", immeuble.getId());
        etage = commeAdmin(() -> fixture.etage("ETG-AUD-" + suffixe));
        suivre("StkFloor", etage.getId());
        StkItemCategory categorie = commeAdmin(() -> fixture.categorie("CAT-AUD-" + suffixe, fixture.natureBat(), false));
        suivre("StkItemCategory", categorie.getId());
        StkItemType type = commeAdmin(() -> fixture.type("TYP-AUD-" + suffixe, fixture.natureBat(), false));
        suivre("StkItemType", type.getId());
        return corps().avec("trancheId", tranche.getId()).avec(CODE, code).avec(DESCRIPTION, "Article " + code)
                .avec("vocationId", VOCATION_VENTE).avec("categorieId", categorie.getId()).avec("typeId", type.getId())
                .avec("immeubleId", immeuble.getId()).avec("etageId", etage.getId())
                .avec("surfaceProvisoire", new BigDecimal("63")).avec("surfaceDefinitive", new BigDecimal("63"))
                .avec(PRIX_PROVISOIRE_HT, new BigDecimal("10000")).avec(PRIX_DEFINITIF_HT, new BigDecimal("10000"))
                .avec("tauxTva", new BigDecimal("0.20"));
    }

    @Test
    void r1_creationArticle_typeItem() throws Exception {
        String code = "ART-AUD-" + suffixe;
        UUID id = creer(ARTICLES, preparerArticle(code), TABLE_STK_ITEM);

        Map<String, Object> ligne = seuleLigne(id, ADDED);
        assertAll(
                () -> assertThat(ligne.get(OBJECT_TYPE)).isEqualTo("Yes.Stk.Building.Item"),
                () -> assertThat(ligne.get(OBJECT_TYPE_LABEL)).isEqualTo("Article"),
                () -> assertThat(ligne.get(OBJECT_LABEL)).isEqualTo(code));
    }

    /** §8 : prix stocké {@code 10000.000000}, PATCH {@code 10000} → égal après normalisation, aucune ligne. */
    @Test
    void r2_2_3_patchPrixArticleMemeValeurAutreEcriture_aucuneLigne() throws Exception {
        UUID id = creer(ARTICLES, preparerArticle("ART-AUD-NORM-" + suffixe), TABLE_STK_ITEM);

        envoyer(patch(ARTICLE, id), corps().avec(PRIX_PROVISOIRE_HT, new BigDecimal("10000"))
                .avec(PRIX_DEFINITIF_HT, new BigDecimal("10000"))).andExpect(status().isOk());

        assertThat(lignes(id, MODIFIED)).isEmpty();
    }

    @Test
    void r2_2_3_patchPrixArticleReel_uneLigne_seulementDesProprietesChangees() throws Exception {
        UUID id = creer(ARTICLES, preparerArticle("ART-AUD-PRIX-" + suffixe), TABLE_STK_ITEM);

        envoyer(patch(ARTICLE, id), corps().avec(PRIX_DEFINITIF_HT, new BigDecimal("11000"))).andExpect(status().isOk());

        assertThat(triplets(seuleLigne(id, MODIFIED).get(DETAILS)))
                .isNotEmpty()
                .allSatisfy(t -> assertAll(
                        () -> assertThat(memeValeur(t.get(1), t.get(2))).as("%s inchangée listée", t.get(0)).isFalse(),
                        () -> assertThat(t.get(0)).isNotIn("Code", PROP_DESCRIPTION, "PKey").doesNotStartWith("User")));
    }

    @Test
    void r1_creationSousArticle_typeUnderItem() throws Exception {
        UUID parent = creer(ARTICLES, preparerArticle("ART-AUD-PARENT-" + suffixe), TABLE_STK_ITEM);
        StkItemCategory categorieSous = commeAdmin(() -> fixture.categorie("CAT-AUD-SOUS-" + suffixe,
                fixture.natureBat(), true));
        suivre("StkItemCategory", categorieSous.getId());
        StkItemType typeSous = commeAdmin(() -> fixture.type("TYP-AUD-SOUS-" + suffixe, fixture.natureBat(), true));
        suivre("StkItemType", typeSous.getId());
        String code = "SA-AUD-" + suffixe;

        UUID id = creer("/api/articles/" + parent + "/sous-articles", corps().avec(CODE, code)
                .avec(DESCRIPTION, "Parking " + code).avec("categorieId", categorieSous.getId())
                .avec("typeId", typeSous.getId()).avec("etageId", etage.getId())
                .avec("surfaceProvisoire", BigDecimal.TEN).avec("surfaceDefinitive", BigDecimal.TEN)
                .avec(PRIX_PROVISOIRE_HT, new BigDecimal("5000")).avec(PRIX_DEFINITIF_HT, new BigDecimal("5000"))
                .avec("tauxTva", new BigDecimal("0.20")), TABLE_STK_ITEM);

        Map<String, Object> ligne = seuleLigne(id, ADDED);
        assertAll(
                () -> assertThat(ligne.get(OBJECT_TYPE)).isEqualTo("Yes.Stk.Building.UnderItem"),
                () -> assertThat(ligne.get(OBJECT_TYPE_LABEL)).isEqualTo("Sous article"),
                () -> assertThat(ligne.get(OBJECT_LABEL)).isEqualTo(code));
    }
}
