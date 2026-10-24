package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureExportPatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.LectureExport.executer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * P9c §3.2 : la fiche PDF suit {@code GET /api/articles/{id}}. Un article sans tranche (5 lignes réelles de StkItem)
 * est lisible par le détail ; sa fiche ne doit donc pas répondre 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FichePdfArticleSansTrancheTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureExportPatrimoine articles;

    @Test
    void articleSansTranche_fichePdf_memeStatutQueLeDetail() throws Exception {
        AdmUser admin = fixture.administrateur();
        String jeton = FixtureExportPatrimoine.jeton();
        StkItem article = articles.article(articles.referentiel(jeton), "ART-ST-" + jeton, null, null);
        em.flush();
        em.clear();

        int detail = mockMvc.perform(get("/api/articles/{id}", article.getId()).with(connecte(admin)))
                .andReturn().getResponse().getStatus();
        int pdf = executer(mockMvc, get("/api/articles/{id}/fiche.pdf", article.getId()).with(connecte(admin)))
                .getResponse().getStatus();

        assertThat(detail).isEqualTo(200);
        assertThat(pdf).isEqualTo(detail);
    }
}
