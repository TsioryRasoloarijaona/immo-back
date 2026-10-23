package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.ClasseurDeTest;
import com.tsix.immocrm.fixtures.FixtureImportPatrimoine;
import com.tsix.immocrm.fixtures.FixtureImportPatrimoine.Import;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ARTICLES;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.CREATION;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.IMPORT;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.INCHANGEE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MODELE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.SOUS_ARTICLES;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.entier;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.lignes;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.message;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.ARTICLE_PARENT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.ETAGE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.IMMEUBLE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.NATURE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PIVOT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PRIX_DEFI_HT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.PRIX_PROV_HT;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.SURFACE_DEFI;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.SURFACE_PROV;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TRANCHE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TVA;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.TYPE;
import static com.tsix.immocrm.fixtures.FixtureImportPatrimoine.VOCATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue P9a (FEATURE-patrimoine-import-export.md §0.1, §1.1, §1.4, §1.5, §7) : 403 avant tout 400, aller-retour
 * modèle → import (« réimporter le même fichier → INCHANGEE »), aucune ligne n'atteint une autre tranche.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportPatrimoineRevueTest {

    private static final String TRANCHE_ID = "trancheId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportPatrimoine fixture;
    @Autowired private DataSource dataSource;

    private Import i;
    private final List<UUID> horsTranche = new ArrayList<>();

    @BeforeEach
    void setUp() {
        i = fixture.preparer("P9R");
    }

    @AfterEach
    void nettoyer() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        horsTranche.forEach(id -> {
            jdbc.update("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id.toString());
            jdbc.update("DELETE FROM StkItem WHERE PKey = ?", id.toString());
        });
        fixture.nettoyer(i);
    }

    private String code(String suffixe) {
        return suffixe + "-" + i.jeton();
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    private byte[] modele(String type) throws Exception {
        return mockMvc.perform(get(MODELE, type).param(TRANCHE_ID, i.trancheId().toString()).with(connecte(i.admin())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private String importer(String type, byte[] fichier, boolean simulation) throws Exception {
        return corps(mockMvc.perform(requete(type, i.trancheId(), simulation, fichier, connecte(i.admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    private static byte[] fichier(Map<String, Object> ligne) {
        List<String> entetes = new ArrayList<>(ligne.keySet());
        return ClasseurDeTest.xlsx(entetes, List.of(entetes.stream().map(ligne::get).toList()));
    }

    private static List<String> resultats(String bilan) {
        return lignes(bilan).stream().map(l -> (String) l.get("resultat")).toList();
    }

    // ------------------------------------------------------------------ §0.1 403 avant tout 400

    /** Partie {@code fichier} absente : le 400 de liaison Spring ne doit pas passer avant le 403. */
    @Test
    void i1_commercial_importSansFichier_403() throws Exception {
        mockMvc.perform(multipart(IMPORT, ARTICLES).param(TRANCHE_ID, i.trancheId().toString())
                        .with(commercial(i.admin())))
                .andExpect(status().isForbidden());
    }

    @Test
    void i1_commercial_modeleTrancheIdMalForme_403() throws Exception {
        mockMvc.perform(get(MODELE, ARTICLES).param(TRANCHE_ID, "pas-un-uuid").with(commercial(i.admin())))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ §7 aller-retour du modèle

    @Test
    void modeleArticles_reimporteTelQuel_toutInchange() throws Exception {
        StkItem libre = fixture.principal(i, code("RTA"));
        fixture.sousArticle(i, code("RTS"), libre);
        fixture.occupe(i, code("RTO"));

        String bilan = importer(ARTICLES, modele(ARTICLES), false);

        assertAll(
                () -> assertThat(entier(bilan, "erreurs")).as(bilan).isZero(),
                () -> assertThat(resultats(bilan)).as(bilan).hasSize(2).containsOnly(INCHANGEE),
                () -> assertThat(fixture.auditDuTest(i, "Modified")).isZero());
    }

    @Test
    void modeleSousArticles_reimporteTelQuel_toutInchange() throws Exception {
        StkItem parent = fixture.principal(i, code("RTP"));
        fixture.sousArticle(i, code("RTR"), parent);
        fixture.sousArticle(i, code("RTL"), null);

        String bilan = importer(SOUS_ARTICLES, modele(SOUS_ARTICLES), false);

        assertAll(
                () -> assertThat(entier(bilan, "erreurs")).as(bilan).isZero(),
                () -> assertThat(resultats(bilan)).as(bilan).hasSize(2).containsOnly(INCHANGEE),
                () -> assertThat(fixture.auditDuTest(i, "Modified")).isZero());
    }

    // ------------------------------------------------------------------ R4 : rien hors de la tranche choisie

    @Test
    void pivotDUnArticleDUneAutreTranche_creationDansLaTrancheChoisie_autreIntact() throws Exception {
        CfgTranche autre = fixture.autreTranche(i);
        String commun = code("XTR");
        StkItem ailleurs = fixture.principal(i, commun, item -> item.setStkHierarchy(autre));
        horsTranche.add(ailleurs.getId());
        Map<String, Object> l = new LinkedHashMap<>();
        l.put(PIVOT, commun);
        l.put(TRANCHE, i.tranche().getCode());
        l.put(NATURE, fixture.codeNatureBatiment(i));
        l.put(VOCATION, fixture.codeVocationVente(i));
        l.put(CODE, commun);
        l.put(DESCRIPTION, "Nouvelle description " + commun);
        l.put(IMMEUBLE, i.c().immeuble().getCode());
        l.put(ETAGE, i.c().etage().getCode());
        l.put(CATEGORIE, i.c().categorie().getCode());
        l.put(TYPE, i.c().type().getCode());
        l.put(PRIX_PROV_HT, new BigDecimal("10000"));
        l.put(PRIX_DEFI_HT, new BigDecimal("10000"));
        l.put(TVA, new BigDecimal("0.2"));
        l.put(SURFACE_PROV, new BigDecimal("63"));
        l.put(SURFACE_DEFI, new BigDecimal("63"));

        String bilan = importer(ARTICLES, fichier(l), false);

        String description = new JdbcTemplate(dataSource).queryForObject(
                "SELECT Description FROM StkItem WHERE PKey = ?", String.class, ailleurs.getId().toString());
        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(CREATION),
                () -> assertThat(fixture.articles(i, commun)).hasSize(1),
                () -> assertThat(description).isEqualTo(ailleurs.getDescription()));
    }

    @Test
    void articleParentDUneAutreTranche_erreurParentOmran() throws Exception {
        CfgTranche autre = fixture.autreTranche(i);
        StkItem ailleurs = fixture.principal(i, code("XPA"), item -> item.setStkHierarchy(autre));
        horsTranche.add(ailleurs.getId());
        Map<String, Object> l = new LinkedHashMap<>();
        l.put(PIVOT, code("XSA"));
        l.put(TRANCHE, i.tranche().getCode());
        l.put(CODE, code("XSA"));
        l.put(DESCRIPTION, "Sous-article " + code("XSA"));
        l.put(CATEGORIE, i.c().categorieSous().getCode());
        l.put(TYPE, i.c().typeSous().getCode());
        l.put(ARTICLE_PARENT, ailleurs.getCode());

        String bilan = importer(SOUS_ARTICLES, fichier(l), true);

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(message(bilan, 2)).contains("Il y a un problème avec le code article parent"));
    }
}
