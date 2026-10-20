package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit des dépôts — FEATURE-patrimoine-article-onglets.md §8.3, §10 Q3, §11 : une ligne {@code AdmWebHistorics}
 * {@code Added} / {@code Deleted} par dépôt, tous porteurs (dossier compris) ; {@code ObjectId} = PKey de la pièce,
 * {@code ObjectLabel} = code du porteur, sans parent. Le type d'objet n'est pas asserté (voir rapport).
 *
 * <p>Sans {@code @Transactional} : la ligne d'audit n'est écrite qu'à la validation. Chaque test valide ses écritures et
 * les efface en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PiecesJointesAuditTest {

    private static final Logger log = LoggerFactory.getLogger(PiecesJointesAuditTest.class);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String jeton = "P2BAU" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    private final Deque<String[]> creees = new ArrayDeque<>();

    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private AdmUser admin;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        admin = executer(patrimoine::administrateur);
        suivre("AdmUser", admin.getId());
        CfgCompany entite = executer(() -> patrimoine.entite("ENT-" + jeton, true));
        suivre("CfgCompany", entite.getId());
        CfgProject projet = executer(() -> patrimoine.projet("PRJ-" + jeton, entite));
        suivre("CfgProject", projet.getId());
        tranche = executer(() -> patrimoine.tranche("TR-" + jeton, projet));
        suivre("CfgTranche", tranche.getId());
    }

    @AfterEach
    void nettoyer() {
        String pieces = "SELECT PKey FROM AdmAttachedFile WHERE CategoryId = ?";
        creees.forEach(ligne -> {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (" + pieces + ")", ligne[1]);
            effacer("DELETE FROM AppFileObject WHERE AdmAttachedFileId IN (" + pieces + ")", ligne[1]);
            effacer("DELETE FROM AdmAttachedFile WHERE CategoryId = ?", ligne[1]);
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", ligne[1], ligne[1]);
        });
        while (!creees.isEmpty()) {
            String[] ligne = creees.pop();
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
        }
    }

    private <T> T executer(Supplier<T> ecriture) {
        return transactions.execute(statut -> ecriture.get());
    }

    private void suivre(String table, UUID id) {
        creees.push(new String[]{table, id.toString()});
    }

    /** Nettoyage au mieux : une ligne annexe inconnue (FK en CI) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, Arrays.stream(parametres).map(Object::toString).toArray());
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private UUID deposer(String url, UUID porteurId) throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("fichier", "plan.pdf", MediaType.APPLICATION_PDF_VALUE,
                "PDF".getBytes(StandardCharsets.UTF_8));
        String json = mockMvc.perform(multipart(url, porteurId).file(pdf).with(connecte(admin)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private List<Map<String, Object>> lignesAudit(UUID pieceId, String action) {
        return jdbc.queryForList("""
                        SELECT Action, ObjectLabel, CONVERT(varchar(36), ParentObjectId) AS ParentObjectId
                        FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ?""", pieceId.toString(), action);
    }

    // ------------------------------------------------------------------ Q3 Added / Deleted

    @Test
    void q3_depotTranche_uneLigneAdded_labelCodeDeLaTranche_sansParent() throws Exception {
        UUID piece = deposer("/api/tranches/{id}/pieces-jointes", tranche.getId());

        List<Map<String, Object>> lignes = lignesAudit(piece, "Added");
        assertThat(lignes).hasSize(1);
        assertThat(lignes.get(0)).containsEntry("ObjectLabel", tranche.getCode()).containsEntry("ParentObjectId", null);
    }

    @Test
    void q3_suppressionTranche_uneLigneDeleted() throws Exception {
        UUID piece = deposer("/api/tranches/{id}/pieces-jointes", tranche.getId());

        mockMvc.perform(delete("/api/tranches/{id}/pieces-jointes/{pj}", tranche.getId(), piece).with(connecte(admin)))
                .andExpect(status().isNoContent());

        assertThat(lignesAudit(piece, "Deleted")).hasSize(1);
    }

    /** Q3 « tous porteurs » : le dépôt de dossier, non journalisé jusqu'ici, l'est aussi. */
    @Test
    void q3_depotDossier_uneLigneAdded_labelCodeDuDossier() throws Exception {
        ComFolder dossier = executer(() -> patrimoine.dossier("DOS-" + jeton));
        suivre("ComFolder", dossier.getId());

        UUID piece = deposer("/api/dossiers/{id}/pieces-jointes", dossier.getId());

        List<Map<String, Object>> lignes = lignesAudit(piece, "Added");
        assertThat(lignes).hasSize(1);
        assertThat(lignes.get(0)).containsEntry("ObjectLabel", dossier.getCode());
    }
}
