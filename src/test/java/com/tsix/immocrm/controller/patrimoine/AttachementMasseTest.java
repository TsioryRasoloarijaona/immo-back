package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureArticleOnglets;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import com.tsix.immocrm.storage.FixturePieceJointe;
import com.tsix.immocrm.storage.StockageMemoire;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Attachement en masse — FEATURE-patrimoine-article-onglets.md §4, §7, §8.5 :
 * {@code POST /api/tranches/{id}/pieces-jointes/masse} (parties {@code fichiers}, {@code typeId} obligatoire,
 * {@code groupe}, {@code description}) ; nom sans extension = code d'un article <b>Bâtiment</b> de la tranche, égalité
 * stricte sensible à la casse ; même type déjà présent → contenu remplacé (même {@code AdmAttachedFile.PKey}, nouvelle clé
 * S3) ; sinon nouvelle pièce {@code Yes.Stk.Building.Item} ; bilan par fichier {@code {nomFichier, resultat, message,
 * pieceId}} ; une transaction par fichier (un refus n'annule pas les autres).
 *
 * <p>Sans {@code @Transactional} : la boucle par fichier peut ouvrir ses propres transactions ; les lignes du test sont
 * validées puis effacées en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AttachementMasseTest {

    private static final Logger log = LoggerFactory.getLogger(AttachementMasseTest.class);

    private static final String MASSE = "/api/tranches/{id}/pieces-jointes/masse";
    private static final String CATEGORIE_ARTICLE = "Yes.Stk.Building.Item";
    private static final String ATTACHE = "ATTACHE";
    private static final String INTROUVABLE = "CODE_INTROUVABLE";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureArticleOnglets onglets;
    @Autowired private FixturePieceJointe fixturePieceJointe;
    @Autowired private StockageMemoire stockage;
    @Autowired private AdmAttachedFileTypeRepository typeRepository;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String jeton = "P2BM" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    private final Deque<String[]> creees = new ArrayDeque<>();
    private final Deque<UUID> porteurs = new ArrayDeque<>();

    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private AdmUser admin;
    private CfgTranche tranche;
    private StkItem a1;
    private StkItem a2;
    private StkItem terrain;
    private StkItem horsTranche;
    private AdmAttachedFileType type;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        executer(() -> {
            patrimoine.referentielArticle();
            return null;
        });
        admin = executer(patrimoine::administrateur);
        suivre("AdmUser", admin.getId());
        CfgCompany entite = executer(() -> patrimoine.entite("ENT-" + jeton, true));
        suivre("CfgCompany", entite.getId());
        CfgProject projet = executer(() -> patrimoine.projet("PRJ-" + jeton, entite));
        suivre("CfgProject", projet.getId());
        tranche = executer(() -> patrimoine.tranche("TR-" + jeton, projet));
        suivre("CfgTranche", tranche.getId());
        CfgTranche autre = executer(() -> patrimoine.tranche("TRX-" + jeton, projet));
        suivre("CfgTranche", autre.getId());
        a1 = article("A1-" + jeton, tranche, false);
        a2 = article("A2-" + jeton, tranche, false);
        terrain = article("T1-" + jeton, tranche, true);
        horsTranche = article("X1-" + jeton, autre, false);
        type = executer(() -> {
            AdmAttachedFileType t = new AdmAttachedFileType();
            t.setId(UUID.randomUUID());
            t.setCode("PLAN-" + jeton);
            t.setDescription("Plan " + jeton);
            return typeRepository.save(t);
        });
        suivre("AdmAttachedFileType", type.getId());
    }

    @AfterEach
    void nettoyer() {
        porteurs.forEach(id -> {
            String pieces = "SELECT PKey FROM AdmAttachedFile WHERE CategoryId = ?";
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (" + pieces + ")", id);
            effacer("DELETE FROM AppFileObject WHERE AdmAttachedFileId IN (" + pieces + ")", id);
            effacer("DELETE FROM AdmAttachedFile WHERE CategoryId = ?", id);
        });
        while (!creees.isEmpty()) {
            String[] ligne = creees.pop();
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", ligne[1], ligne[1]);
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
        }
    }

    // ------------------------------------------------------------------ outils

    private <T> T executer(Supplier<T> ecriture) {
        return transactions.execute(statut -> ecriture.get());
    }

    private <T> T suivre(String table, T id) {
        creees.push(new String[]{table, id.toString()});
        return id;
    }

    /** Nettoyage au mieux : une ligne annexe inconnue (FK en CI) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, Arrays.stream(parametres).map(Object::toString).toArray());
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private StkItem article(String code, CfgTranche dans, boolean natureTerrain) {
        StkItem item = executer(() -> natureTerrain
                ? onglets.article(code, dans, onglets.natureTerrain(), null)
                : onglets.article(code, dans));
        suivre("StkItemCategory", item.getStkItemCategory().getId());
        suivre("StkItemType", item.getStkItemType().getId());
        suivre("StkItem", item.getId());
        porteurs.push(item.getId());
        return item;
    }

    private static MockMultipartFile fichier(String nom, String typeMime, String contenu) {
        return new MockMultipartFile("fichiers", nom, typeMime, contenu.getBytes(StandardCharsets.UTF_8));
    }

    private static MockMultipartFile pdf(String nom) {
        return fichier(nom, MediaType.APPLICATION_PDF_VALUE, "PDF-" + nom);
    }

    private ResultActions masse(UUID trancheId, RequestPostProcessor qui, MockMultipartFile... fichiers)
            throws Exception {
        var requete = multipart(MASSE, trancheId);
        for (MockMultipartFile f : fichiers) {
            requete.file(f);
        }
        return mockMvc.perform(requete.param("typeId", type.getId().toString())
                .param("groupe", "GRP-" + jeton)
                .param("description", "Plan masse " + jeton)
                .with(qui));
    }

    private String bilan(MockMultipartFile... fichiers) throws Exception {
        return masse(tranche.getId(), connecte(admin), fichiers)
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<Object> champ(String bilan, String nomFichier, String champ) {
        return JsonPath.read(bilan, "$..[?(@.nomFichier=='" + nomFichier + "')]." + champ);
    }

    private List<Map<String, Object>> pieces(StkItem article) {
        return jdbc.queryForList("""
                        SELECT CONVERT(varchar(36), f.PKey) AS PKey, f.Category, f.Extention, f.FoderGroup, f.Description,
                               CONVERT(varchar(36), f.AdmAttachedFileTypeId) AS TypeId, o.StorageKey
                        FROM AdmAttachedFile f LEFT JOIN AppFileObject o ON o.AdmAttachedFileId = f.PKey
                        WHERE f.CategoryId = ?""", article.getId().toString());
    }

    // ------------------------------------------------------------------ §4 attaché

    @Test
    void r4_codeDArticleDeLaTranche_nouvellePieceArticle_attache() throws Exception {
        String nom = a1.getCode() + ".pdf";

        String bilan = bilan(pdf(nom));

        List<Map<String, Object>> lignes = pieces(a1);
        assertAll(
                () -> assertThat(champ(bilan, nom, "resultat")).containsExactly(ATTACHE),
                () -> assertThat(lignes).hasSize(1),
                () -> assertThat(lignes.get(0)).containsEntry("Category", CATEGORIE_ARTICLE)
                        .containsEntry("FoderGroup", "GRP-" + jeton),
                () -> assertThat(guid(lignes.get(0).get("TypeId"))).isEqualTo(guid(type.getId())),
                () -> assertThat(champ(bilan, nom, "pieceId")).containsExactly(lignes.get(0).get("PKey").toString()
                        .toLowerCase(Locale.ROOT)));
    }

    // ------------------------------------------------------------------ §4 / §8.3 remplacé

    @Test
    void r4_memeTypeDejaPresent_contenuRemplace_memePKey_nouvelleCleS3() throws Exception {
        AdmAttachedFile existante = executer(() -> {
            AdmAttachedFile f = new AdmAttachedFile();
            f.setId(UUID.randomUUID());
            f.setCategoryId(a1.getId());
            f.setCategory(CATEGORIE_ARTICLE);
            f.setDescription("Ancien plan");
            f.setExtension(".pdf");
            f.setAdmAttachedFileTypeId(type.getId());
            return fixturePieceJointe.deposer(f, "ANCIEN".getBytes(StandardCharsets.UTF_8));
        });
        String ancienneCle = (String) pieces(a1).get(0).get("StorageKey");
        String nom = a1.getCode() + ".png";

        String bilan = bilan(fichier(nom, MediaType.IMAGE_PNG_VALUE, "NOUVEAU"));

        List<Map<String, Object>> lignes = pieces(a1);
        assertThat(lignes).hasSize(1);
        Map<String, Object> ligne = lignes.get(0);
        String nouvelleCle = (String) ligne.get("StorageKey");
        assertAll(
                () -> assertThat(champ(bilan, nom, "resultat")).containsExactly("REMPLACE"),
                () -> assertThat(champ(bilan, nom, "pieceId")).containsExactly(existante.getId().toString()),
                () -> assertThat(guid(ligne.get("PKey"))).isEqualTo(guid(existante.getId())),
                () -> assertThat(ligne).containsEntry("Extention", ".png").containsEntry("FoderGroup", "GRP-" + jeton)
                        .containsEntry("Description", "Plan masse " + jeton),
                () -> assertThat(nouvelleCle).isNotEqualTo(ancienneCle),
                () -> assertThat(stockage.contenu(nouvelleCle)).isEqualTo("NOUVEAU".getBytes(StandardCharsets.UTF_8)));
    }

    // ------------------------------------------------------------------ §4 / §8.5 code introuvable

    @Test
    void r4_codeInconnu_codeArticleIntrouvable() throws Exception {
        String nom = "INCONNU-" + jeton + ".pdf";

        String bilan = bilan(pdf(nom));

        assertAll(
                () -> assertThat(champ(bilan, nom, "resultat")).containsExactly(INTROUVABLE),
                () -> assertThat(champ(bilan, nom, "message").toString()).contains("Code article introuvable"));
    }

    /** §8.5 : {@code Equals} d'OMRAN, sensible à la casse (la collation SQL Server, elle, ne l'est pas). */
    @Test
    void r8_5_casseDifferente_codeIntrouvable_rienAttache() throws Exception {
        String nom = a1.getCode().toLowerCase(Locale.ROOT) + ".pdf";

        String bilan = bilan(pdf(nom));

        assertAll(
                () -> assertThat(champ(bilan, nom, "resultat")).containsExactly(INTROUVABLE),
                () -> assertThat(pieces(a1)).isEmpty());
    }

    @Test
    void r8_5_articleDUneAutreTranche_codeIntrouvable_rienAttache() throws Exception {
        String nom = horsTranche.getCode() + ".pdf";

        String bilan = bilan(pdf(nom));

        assertAll(
                () -> assertThat(champ(bilan, nom, "resultat")).containsExactly(INTROUVABLE),
                () -> assertThat(pieces(horsTranche)).isEmpty());
    }

    /** §8.5 : seuls les articles de nature Bâtiment sont visés ({@code StkKind.Building}). */
    @Test
    void r8_5_articleDeNatureTerrain_codeIntrouvable_rienAttache() throws Exception {
        String nom = terrain.getCode() + ".pdf";

        String bilan = bilan(pdf(nom));

        assertAll(
                () -> assertThat(champ(bilan, nom, "resultat")).containsExactly(INTROUVABLE),
                () -> assertThat(pieces(terrain)).isEmpty());
    }

    // ------------------------------------------------------------------ §4 une transaction par fichier

    @Test
    void r4_unFichierRefuse_nAnnulePasLesAutres() throws Exception {
        String refuse = a1.getCode() + ".exe";
        String admis = a2.getCode() + ".pdf";

        String bilan = bilan(fichier(refuse, MediaType.APPLICATION_OCTET_STREAM_VALUE, "EXE"), pdf(admis));

        assertAll(
                () -> assertThat(champ(bilan, refuse, "resultat")).containsExactly("REFUSE"),
                () -> assertThat(champ(bilan, admis, "resultat")).containsExactly(ATTACHE),
                () -> assertThat(pieces(a1)).isEmpty(),
                () -> assertThat(pieces(a2)).hasSize(1));
    }

    // ------------------------------------------------------------------ refus de la requête

    /** §8.5 : {@code typeId} obligatoire en masse (le remplacement est défini par le type). */
    @Test
    void r8_5_typeIdAbsent400() throws Exception {
        mockMvc.perform(multipart(MASSE, tranche.getId()).file(pdf(a1.getCode() + ".pdf")).with(connecte(admin)))
                .andExpect(status().isBadRequest());

        assertThat(pieces(a1)).isEmpty();
    }

    @Test
    void r8_1_7_trancheInconnue404() throws Exception {
        masse(UUID.randomUUID(), connecte(admin), pdf(a1.getCode() + ".pdf"))
                .andExpect(status().isNotFound());
    }

    @Test
    void r0_commercial403() throws Exception {
        AdmUser compte = executer(patrimoine::administrateur);
        suivre("AdmUser", compte.getId());

        masse(tranche.getId(), FixtureDossierVente.connecte(compte, "Commercial"), pdf(a1.getCode() + ".pdf"))
                .andExpect(status().isForbidden());

        assertThat(pieces(a1)).isEmpty();
    }
}
