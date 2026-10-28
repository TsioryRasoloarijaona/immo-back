package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.fixtures.FixtureLotissements;
import com.tsix.immocrm.fixtures.FixtureLotissements.Jeu;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureLotissements.ADDED;
import static com.tsix.immocrm.fixtures.FixtureLotissements.DELETED;
import static com.tsix.immocrm.fixtures.FixtureLotissements.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureLotissements.LOTISSEMENT;
import static com.tsix.immocrm.fixtures.FixtureLotissements.LOTISSEMENTS;
import static com.tsix.immocrm.fixtures.FixtureLotissements.MODIFIED;
import static com.tsix.immocrm.fixtures.FixtureLotissements.OBJET_LOT;
import static com.tsix.immocrm.fixtures.FixtureLotissements.corpsLot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit d'un lot — FEATURE-patrimoine-lotissements.md §1.9, règles L4 (ligne Added), L17 ; décision Q4 : type
 * {@code Yes.Stk.Land.Land} « Lot de terrain », libellé = Description. Sans {@code @Transactional} : la ligne d'audit
 * n'existe qu'après validation ; tout est effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LotissementAuditTest {

    private static final String AUDIT = "/api/audit";
    private static final String NOTES = "/api/notes";
    private static final String OBJET_ID = "objetId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotissements fixture;

    private Jeu j;

    @BeforeEach
    void setUp() {
        j = fixture.preparer("P7CA");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(j);
    }

    private UUID creer(String code) throws Exception {
        mockMvc.perform(post(LOTISSEMENTS).with(connecte(j.admin()))
                        .contentType(MediaType.APPLICATION_JSON).content(corpsLot(j, code).json()))
                .andExpect(status().isCreated());
        List<UUID> ids = fixture.ids(j.tranche(), code);
        assertThat(ids).as("lot %s en base", code).hasSize(1);
        return ids.get(0);
    }

    private Map<String, Object> seuleLigne(UUID objet, String action) {
        List<Map<String, Object>> lignes = fixture.lignesAudit(objet, action);
        assertThat(lignes).as("lignes %s de %s", action, objet).hasSize(1);
        return lignes.get(0);
    }

    /** L4 / Q4 : une ligne Added, type et libellé OMRAN du lot, libellé d'objet = Description. */
    @Test
    void l4_creation_uneLigneAdded_typeLot() throws Exception {
        String code = j.code("ACR");

        UUID lot = creer(code);

        assertThat(seuleLigne(lot, ADDED))
                .containsEntry("ObjectType", OBJET_LOT)
                .containsEntry("ObjectTypeLabel", FixtureLotissements.LIBELLE_OBJET_LOT)
                .containsEntry("ObjectLabel", FixtureLotissements.description(code));
    }

    @Test
    void l1_9_modification_uneLigneModified_typeLot() throws Exception {
        UUID lot = fixture.lot(j, j.code("AMO"));

        mockMvc.perform(patch(LOTISSEMENT, lot).with(connecte(j.admin())).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, "Lot audite " + j.jeton()).json()))
                .andExpect(status().isOk());

        assertThat(seuleLigne(lot, MODIFIED)).containsEntry("ObjectType", OBJET_LOT);
    }

    @Test
    void l1_9_suppression_uneLigneDeleted_typeLot() throws Exception {
        UUID lot = fixture.lot(j, j.code("ASU"));

        mockMvc.perform(delete(LOTISSEMENT, lot).with(connecte(j.admin()))).andExpect(status().isNoContent());

        assertThat(fixture.audit(lot, DELETED)).isEqualTo(1);
    }

    /** L17 : GET /api/audit?objetId= d'un lot rend ses lignes (porteur article). */
    @Test
    void l17_auditDUnLotCree_rendu() throws Exception {
        UUID lot = creer(j.code("AAU"));

        mockMvc.perform(get(AUDIT).param(OBJET_ID, lot.toString()).with(connecte(j.admin())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(OBJET_LOT)));
    }

    /** L17 : notes d'un lot par la route article. */
    @Test
    void l17_notesDUnLot_200() throws Exception {
        UUID lot = fixture.lot(j, j.code("ANO"));
        fixture.note(lot);

        mockMvc.perform(get(NOTES).param("porteur", "article").param("id", lot.toString()).with(connecte(j.admin())))
                .andExpect(status().isOk());
    }

    /** L17 : lot d'une entité inactive ⇒ 404 sur l'audit et les notes. */
    @Test
    void l17_lotDUneEntiteInactive_auditEtNotes_404() throws Exception {
        UUID lot = fixture.lot(j, j.code("AIN"));
        fixture.desactiverEntite(j);

        mockMvc.perform(get(AUDIT).param(OBJET_ID, lot.toString()).with(connecte(j.admin())))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(NOTES).param("porteur", "article").param("id", lot.toString()).with(connecte(j.admin())))
                .andExpect(status().isNotFound());
    }
}
