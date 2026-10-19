package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheRegles;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * « Mettre à jour les schémas de financement » (argent, règle 6) — FEATURE-patrimoine-tranche-regles.md §4, §7
 * (MiseAJourSchemasTest), §9.5-§9.7. Sélection OMRAN ({@code Tranche.cs:776-804}) : articles de la tranche ayant un
 * dossier ({@code StkItem.ComFolderId}), échéances dont la catégorie a {@code IsConfermentBasedDate},
 * {@code DocDate < StartDateFinalisation} et reste {@code > 0} — reste calculé ({@code MontantRestant}, règlements
 * annulés exclus), jamais la colonne {@code AmountRest}. Les dossiers sortis (annulés, désistés) n'occupent plus
 * l'article : exclus. Aperçu et exécution partagent la sélection.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MiseAJourSchemasTest {

    private static final String EXECUTER = "/api/tranches/{id}/mettre-a-jour-schemas";
    private static final String APERCU = EXECUTER + "/apercu";
    private static final String IDS = "$..echeanceId";
    private static final String ANNULE = "CANCELED";
    private static final LocalDateTime DEBUT_FINALISATIONS = LocalDateTime.of(2027, 6, 1, 0, 0);
    private static final LocalDateTime AVANT = LocalDateTime.of(2027, 1, 15, 0, 0);
    private static final BigDecimal MILLE = new BigDecimal("1000");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureTrancheRegles regles;

    private AdmUser admin;
    private CfgTranche tranche;
    private ComSettlementCategory conferment;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1CS" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = regles.debutFinalisations(vente.tranche("TR-" + jeton, true), DEBUT_FINALISATIONS);
        conferment = regles.categorie("CONF-" + jeton, true);
    }

    // ------------------------------------------------------------------ outils

    private ComFolder dossier(String suffixe) {
        return regles.dossierCourant("DOS-" + suffixe + "-" + jeton, tranche);
    }

    private ComDocument echeance(ComFolder dossier, LocalDateTime date) {
        return regles.echeance(dossier, conferment, MILLE, date);
    }

    private String apercu() throws Exception {
        return mockMvc.perform(get(APERCU, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String executer() throws Exception {
        return mockMvc.perform(post(EXECUTER, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static List<String> idsApercu(String json) {
        return JsonPath.read(json, IDS);
    }

    private static BigDecimal resteApercu(String json, ComDocument echeance) {
        List<Object> restes = JsonPath.read(json, "$..[?(@.echeanceId == '" + echeance.getId() + "')].reste");
        assertThat(restes).as("reste de %s dans l'aperçu", echeance.getId()).hasSize(1);
        return new BigDecimal(String.valueOf(restes.get(0)));
    }

    private void assertDeplacee(ComDocument echeance) {
        assertThat(regles.dateEcheance(echeance.getId())).as("DocDate de %s", echeance.getId())
                .isEqualTo(DEBUT_FINALISATIONS);
    }

    private void assertInchangee(ComDocument echeance, LocalDateTime date) {
        assertThat(regles.dateEcheance(echeance.getId())).as("DocDate de %s", echeance.getId()).isEqualTo(date);
    }

    // ------------------------------------------------------------------ refus

    @Test
    void r4_sansDateDebutFinalisations_400_messageOmran_rienNEstDeplace() throws Exception {
        tranche = regles.debutFinalisations(tranche, null);
        ComDocument e = echeance(dossier("A"), AVANT);

        mockMvc.perform(post(EXECUTER, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Vous devez saisir la date de début des finalisations")));

        assertInchangee(e, AVANT);
    }

    // ------------------------------------------------------------------ sélection

    @Test
    void r4_echeanceEligible_dateDeplaceeAuDebutDesFinalisations() throws Exception {
        ComDocument e = echeance(dossier("A"), AVANT);

        executer();

        assertDeplacee(e);
    }

    @Test
    void r4_echeanceSoldee_resteNul_nonTouchee() throws Exception {
        ComDocument soldee = echeance(dossier("A"), AVANT);
        regles.reglement(soldee, MILLE, null);

        executer();

        assertInchangee(soldee, AVANT);
    }

    /** Le reste est calculé : un règlement annulé ne solde pas l'échéance. */
    @Test
    void r4_echeanceRegleeParUnReglementAnnule_resteEntier_deplacee() throws Exception {
        ComDocument e = echeance(dossier("A"), AVANT);
        regles.reglement(e, MILLE, ANNULE);

        executer();

        assertDeplacee(e);
    }

    @Test
    void r4_categorieSansIsConfermentBasedDate_nonTouchee() throws Exception {
        ComSettlementCategory autre = regles.categorie("AUTRE-" + jeton, false);
        ComDocument e = regles.echeance(dossier("A"), autre, MILLE, AVANT);

        executer();

        assertInchangee(e, AVANT);
    }

    @Test
    void r4_echeancePosterieureOuEgaleAuDebut_nonTouchee() throws Exception {
        ComFolder d = dossier("A");
        LocalDateTime apres = LocalDateTime.of(2027, 7, 1, 0, 0);
        ComDocument posterieure = echeance(d, apres);
        ComDocument egale = echeance(d, DEBUT_FINALISATIONS);

        String json = apercu();
        executer();

        assertAll(
                () -> assertThat(idsApercu(json)).doesNotContain(posterieure.getId().toString(), egale.getId().toString()),
                () -> assertInchangee(posterieure, apres));
    }

    /** Dossier annulé : son {@code StkItemId} vise encore l'article, que le dossier courant occupe. */
    @Test
    void r4_dossierAnnule_exclu() throws Exception {
        ComFolder courant = dossier("A");
        ComFolder annule = regles.dossierSorti("ANN-" + jeton, courant.getStkItem(), StatutDossier.ANNULATION);
        ComDocument duCourant = echeance(courant, AVANT);
        ComDocument deLAnnule = echeance(annule, AVANT);

        executer();

        assertAll(
                () -> assertDeplacee(duCourant),
                () -> assertInchangee(deLAnnule, AVANT));
    }

    /** Dossier désisté dont l'article a été libéré ({@code StkItem.ComFolderId} nul). */
    @Test
    void r4_dossierDesiste_articleLibere_exclu() throws Exception {
        ComFolder ancien = dossier("A");
        ComFolder desiste = regles.dossierSorti("DES-" + jeton, ancien.getStkItem(), StatutDossier.DESISTEMENT);
        regles.libererArticle(ancien.getStkItem().getId());
        ComDocument e = echeance(desiste, AVANT);

        executer();

        assertInchangee(e, AVANT);
    }

    @Test
    void r4_dossierDUneAutreTranche_exclu() throws Exception {
        CfgTranche autre = regles.debutFinalisations(vente.tranche("TR-AUT-" + jeton, true), DEBUT_FINALISATIONS);
        ComDocument e = echeance(regles.dossierCourant("DOS-AUT-" + jeton, autre), AVANT);

        executer();

        assertInchangee(e, AVANT);
    }

    // ------------------------------------------------------------------ écriture : seulement DocDate, tracée

    @Test
    void r4_montantsEtReglementsInchanges() throws Exception {
        ComDocument e = echeance(dossier("A"), AVANT);
        regles.reglement(e, new BigDecimal("400"), null);

        executer();

        Tuple ligne = regles.ligneEcheance(e.getId());
        assertAll(
                () -> assertThat(FixturePatrimoine.decimal(ligne.get("Amount"))).isEqualByComparingTo(MILLE),
                () -> assertThat(FixturePatrimoine.decimal(ligne.get("AmountHT"))).isEqualByComparingTo(MILLE),
                () -> assertThat(FixturePatrimoine.decimal(ligne.get("AmountTTC"))).isEqualByComparingTo(MILLE),
                () -> assertThat(regles.compter("SELECT COUNT(*) FROM StlSettlementLine WHERE ComDocumentId = :id "
                        + "AND Amount = 400", e.getId())).isEqualTo(1));
    }

    @Test
    void r4_echeanceDeplacee_userUpdateTamponne() throws Exception {
        ComDocument e = echeance(dossier("A"), AVANT);

        executer();

        Tuple ligne = regles.ligneEcheance(e.getId());
        assertAll(
                () -> assertThat(ligne.get("UserUpdate")).isEqualTo(AUTEUR),
                () -> assertThat(ligne.get("UserUpdateDateTime")).isNotNull());
    }

    @Test
    void r4_reponse_documentsModifiesEtDossiersConcernes() throws Exception {
        ComFolder a = dossier("A");
        ComFolder b = dossier("B");
        echeance(a, AVANT);
        echeance(a, LocalDateTime.of(2027, 2, 15, 0, 0));
        echeance(b, AVANT);
        echeance(b, LocalDateTime.of(2027, 8, 1, 0, 0));

        mockMvc.perform(post(EXECUTER, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentsModifies").value(3))
                .andExpect(jsonPath("$.dossiersConcernes").value(2));
    }

    // ------------------------------------------------------------------ aperçu = exécution

    @Test
    void r4_apercuEgalExecution_memesEcheances() throws Exception {
        ComFolder a = dossier("A");
        ComFolder b = dossier("B");
        ComDocument e1 = echeance(a, AVANT);
        ComDocument e2 = echeance(b, LocalDateTime.of(2027, 3, 10, 0, 0));
        ComDocument soldee = echeance(b, AVANT);
        regles.reglement(soldee, MILLE, null);
        ComDocument posterieure = echeance(a, LocalDateTime.of(2027, 9, 1, 0, 0));
        ComDocument autreCategorie = regles.echeance(a, regles.categorie("AUTRE-" + jeton, false), MILLE, AVANT);

        List<String> apercus = idsApercu(apercu());
        String resultat = executer();

        List<String> deplacees = Stream.of(e1, e2, soldee, posterieure, autreCategorie)
                .filter(e -> DEBUT_FINALISATIONS.equals(regles.dateEcheance(e.getId())))
                .map(e -> e.getId().toString())
                .toList();
        assertAll(
                () -> assertThat(apercus).containsExactlyInAnyOrder(e1.getId().toString(), e2.getId().toString()),
                () -> assertThat(deplacees).containsExactlyInAnyOrderElementsOf(apercus),
                () -> assertThat((Integer) JsonPath.read(resultat, "$.documentsModifies")).isEqualTo(apercus.size()));
    }

    @Test
    void r4_apercu_neModifieRien() throws Exception {
        ComDocument e = echeance(dossier("A"), AVANT);

        assertThat(idsApercu(apercu())).contains(e.getId().toString());
        assertInchangee(e, AVANT);
    }

    /** Reste = montant − règlements non annulés ({@code MontantRestant.echeance}). */
    @Test
    void r4_apercu_resteCalcule_reglementsDeduits_annulesNon() throws Exception {
        ComFolder d = dossier("A");
        ComDocument partielle = echeance(d, AVANT);
        regles.reglement(partielle, new BigDecimal("400"), null);
        ComDocument regleeAnnulee = echeance(d, LocalDateTime.of(2027, 2, 1, 0, 0));
        regles.reglement(regleeAnnulee, MILLE, ANNULE);

        String json = apercu();

        assertAll(
                () -> assertThat(resteApercu(json, partielle)).isEqualByComparingTo("600"),
                () -> assertThat(resteApercu(json, regleeAnnulee)).isEqualByComparingTo(MILLE));
    }

    @Test
    void r4_apercu_ancienneEtNouvelleDate_dossier() throws Exception {
        ComFolder d = dossier("A");
        ComDocument e = echeance(d, AVANT);
        String filtre = "$..[?(@.echeanceId == '" + e.getId() + "')]";

        mockMvc.perform(get(APERCU, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre + ".ancienneDate").value(hasItem(containsString("2027-01-15"))))
                .andExpect(jsonPath(filtre + ".nouvelleDate").value(hasItem(containsString("2027-06-01"))))
                .andExpect(jsonPath(filtre + ".dossierId").value(hasItem(d.getId().toString())));
    }
}
