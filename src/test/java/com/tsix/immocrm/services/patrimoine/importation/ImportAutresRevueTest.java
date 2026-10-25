package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.fixtures.FixtureImportAutres;
import com.tsix.immocrm.fixtures.FixtureImportAutres.Autres;
import com.tsix.immocrm.fixtures.FixtureImportAutres.EnTete;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ERREUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MISE_A_JOUR;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.corps;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ecrit;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.resultat;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.ANNEE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.CODE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.ENTITE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.MOIS;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.PAYS;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.PIVOT;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.POURCENTAGE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.PRESENTATIONS;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.PREVISIONS;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.QTE_CONCRETISATION;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.QTE_FINALISATION;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.TERRAINS;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.TRANCHE;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.colonnes;
import static com.tsix.immocrm.fixtures.FixtureImportAutres.fichier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tout ou rien des imports P9b — FEATURE-patrimoine-import-autres.md §0 (I5 : une erreur → rien en base, <b>0 ligne
 * {@code AdmWebHistorics}</b>), §2 (refus {@code Verif} dans la transaction du moteur : bilan 200, jamais
 * {@code UnexpectedRollbackException}). Chaque cas a son témoin : le même fichier sans la ligne fautive écrit et trace.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ImportAutresRevueTest {

    private static final String AVANT = "Description avant";
    private static final String APRES = "Description apres";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureImportAutres fixture;

    private Autres a;

    @BeforeEach
    void setUp() {
        a = fixture.preparer("P9BR");
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer(a);
    }

    private String code(String suffixe) {
        return suffixe + "-" + a.jeton();
    }

    private String importer(String type, UUID trancheId, String feuille, List<Map<String, Object>> lignes)
            throws Exception {
        return corps(mockMvc.perform(requete(type, trancheId, false, fichier(feuille, lignes), connecte(a.i().admin())))
                .andExpect(status().isOk())
                .andReturn());
    }

    // ------------------------------------------------------------------ présentations

    private Map<String, Object> presentation(String code, String description) {
        return colonnes(TRANCHE, a.i().tranche().getCode(), CODE, code, DESCRIPTION, description);
    }

    @Test
    void i5_presentations_ligneEnErreurApresUneMiseAJour_rienEcrit_aucunAudit() throws Exception {
        String code = code("PR");
        UUID id = fixture.presentation(a.i().trancheId(), code, AVANT);
        int auditAvant = fixture.audit(id.toString());

        String bilan = importer(PRESENTATIONS, null, "TYPES_PRESENTATION",
                List.of(presentation(code, APRES), presentation(code("PRKO"), null)));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(resultat(bilan, 3)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.presentations(a.i().trancheId(), code)).singleElement()
                        .satisfies(p -> assertThat(p).containsEntry("Description", AVANT)),
                () -> assertThat(fixture.audit(id.toString())).as("AdmWebHistorics").isEqualTo(auditAvant));
    }

    @Test
    void temoin_presentations_memeMiseAJourSeule_ecriteEtTracee() throws Exception {
        String code = code("PRT");
        UUID id = fixture.presentation(a.i().trancheId(), code, AVANT);
        int auditAvant = fixture.audit(id.toString());

        String bilan = importer(PRESENTATIONS, null, "TYPES_PRESENTATION", List.of(presentation(code, APRES)));

        assertAll(
                () -> assertThat(ecrit(bilan)).as(bilan).isTrue(),
                () -> assertThat(fixture.audit(id.toString())).as("AdmWebHistorics").isGreaterThan(auditAvant));
    }

    // ------------------------------------------------------------------ terrains

    private Map<String, Object> terrain(FixtureTerrains.Contexte c, String code, String description) {
        return colonnes(PIVOT, code, ENTITE, c.entite().getCode(), DESCRIPTION, description, PAYS, c.pays().getCode());
    }

    @Test
    void i5_terrains_ligneEnErreurApresUneMiseAJour_rienEcrit_aucunAudit() throws Exception {
        FixtureTerrains.Contexte c = fixture.terrains(a);
        String code = code("TR");
        UUID id = fixture.terrain(a, c, code);
        int auditAvant = fixture.audit(id.toString());
        Map<String, Object> fautive = terrain(c, code("TRKO"), APRES);
        fautive.put(PAYS, "PAYS-INCONNU-" + a.jeton());

        String bilan = importer(TERRAINS, null, "STOCK_TERRAINS", List.of(terrain(c, code, APRES), fautive));

        assertAll(
                () -> assertThat(resultat(bilan, 2)).as(bilan).isEqualTo(MISE_A_JOUR),
                () -> assertThat(resultat(bilan, 3)).as(bilan).isEqualTo(ERREUR),
                () -> assertThat(ecrit(bilan)).isFalse(),
                () -> assertThat(fixture.article(id)).containsEntry("Description", "Terrain " + code),
                () -> assertThat(fixture.audit(id.toString())).as("AdmWebHistorics").isEqualTo(auditAvant));
    }

    @Test
    void temoin_terrains_memeMiseAJourSeule_ecriteEtTracee() throws Exception {
        FixtureTerrains.Contexte c = fixture.terrains(a);
        String code = code("TRT");
        UUID id = fixture.terrain(a, c, code);
        int auditAvant = fixture.audit(id.toString());

        String bilan = importer(TERRAINS, null, "STOCK_TERRAINS", List.of(terrain(c, code, APRES)));

        assertAll(
                () -> assertThat(ecrit(bilan)).as(bilan).isTrue(),
                () -> assertThat(fixture.article(id)).containsEntry("Description", APRES),
                () -> assertThat(fixture.audit(id.toString())).as("AdmWebHistorics").isGreaterThan(auditAvant));
    }

    // ------------------------------------------------------------------ prévisions : refus Verif dans la transaction

    @Test
    void i16_previsions_refusVerifApresSuperpositionDeLEnTete_bilan200_enTeteEtLignesIntacts() throws Exception {
        UUID tranche = a.i().trancheId();
        fixture.enTete(tranche, new EnTete(3, new BigDecimal("3000000"), new BigDecimal("0.4"),
                LocalDateTime.of(2027, 1, 1, 0, 0), LocalDateTime.of(2027, 3, 1, 0, 0), LocalDateTime.of(2027, 3, 1, 0, 0),
                LocalDateTime.of(2027, 5, 1, 0, 0), BigDecimal.ONE));
        fixture.lignePrevision(tranche, 2026, 5, 3, 3);

        String bilan = importer(PREVISIONS, tranche, "PREVISIONS", List.of(colonnes(ANNEE, 2027, MOIS, 1,
                QTE_CONCRETISATION, 1, QTE_FINALISATION, 1, POURCENTAGE, 25)));

        Map<String, Object> enTete = fixture.enTete(tranche);
        assertAll(
                () -> assertThat(ecrit(bilan)).as(bilan).isFalse(),
                () -> assertThat((BigDecimal) enTete.get("BaseConcretisationPercent")).isEqualByComparingTo("0.4"),
                () -> assertThat(enTete).containsEntry("CountItemBasePrevision", 3),
                () -> assertThat(fixture.lignesPrevision(tranche)).singleElement()
                        .satisfies(l -> assertThat(l).containsEntry("Annee", 2026)));
    }
}
