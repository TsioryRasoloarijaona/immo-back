package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsReunions.Reponse;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureReunionsChantier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.REUNIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Lignes filles d'une réunion : R5 (report), R6 (unicité maître d'ouvrage), R7 (suppression), R9, R10, R2 décision. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReunionsChantierLignesTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 10);
    private static final LocalDateTime AVANT = LocalDateTime.of(2026, 3, 3, 9, 0);
    private static final String MAITRES = "/maitres-ouvrage";
    private static final String DECISIONS_DE = "SELECT COUNT(*) FROM BulMeetingLine WHERE BulMeetingId = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReunionsChantier fixture;

    private AppelsReunions appels;
    private String jeton;
    private UUID projet;
    private UUID type;
    private UUID ouvert;
    private UUID ferme;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        fixture.tables();
        appels = new AppelsReunions(mockMvc, fixture);
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PRL-" + jeton);
        type = fixture.type("RL-" + jeton);
        ouvert = fixture.statut("OUV-" + jeton, false);
        ferme = fixture.statut("FER-" + jeton, true);
        admin = fixture.admin(jeton, projet);
    }

    private UUID nouvelle() throws Exception {
        Reponse r = appels.creer(projet, type, null, DATE, null, admin);
        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        return r.id();
    }

    private Reponse ajouterMaitres(UUID reunion, UUID... contacts) throws Exception {
        String ids = String.join("\",\"", List.of(contacts).stream().map(UUID::toString).toList());
        return appels.envoyer(post(REUNIONS + "/" + reunion + MAITRES), "{\"contactIds\":[\"" + ids + "\"]}", admin);
    }

    // ------------------------------------------------------------------ R5 report du P.V. précédent

    @Test
    void r5_decisionFermee_nonCopiee_ouverteCopieeAvecLienVersOrigine() throws Exception {
        UUID precedente = fixture.reunion(projet, type, 1, "R1-" + jeton, AVANT);
        UUID decisionOuverte = fixture.decision(precedente, 1, "Ouverte", ouvert, null);
        fixture.decision(precedente, 2, "Fermée", ferme, null);

        Reponse r = appels.creer(projet, type, null, DATE, null, admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat(fixture.compter(DECISIONS_DE, r.id())).isEqualTo(1);
        assertThat(fixture.lire("SELECT CAST(BulMeetingLineId AS varchar(36)) FROM BulMeetingLine WHERE BulMeetingId = ?",
                r.id())).isEqualToIgnoringCase(decisionOuverte.toString());
        assertThat(r.<List<Object>>lire("$.decisions")).hasSize(1);
    }

    @Test
    void r5_intervenantsEtLeursContacts_copies() throws Exception {
        UUID precedente = fixture.reunion(projet, type, 1, "R1-" + jeton, AVANT);
        UUID fournisseur = fixture.tiers("FOU-" + jeton);
        UUID contact = fixture.tiers("CTI-" + jeton);
        fixture.contactIntervenant(fixture.intervenant(precedente, fournisseur), contact);

        Reponse r = appels.creer(projet, type, null, DATE, null, admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulDelegate WHERE BulMeetingId = ? AND CfgSupplierId = ?",
                r.id(), fournisseur)).isEqualTo(1);
        assertThat(fixture.compter("""
                SELECT COUNT(*) FROM BulDelegateContact c JOIN BulDelegate d ON d.PKey = c.BulDelegateId
                WHERE d.BulMeetingId = ? AND c.ContactId = ?""", r.id(), contact)).isEqualTo(1);
    }

    @Test
    void r5_maitresOuvrage_copies() throws Exception {
        UUID precedente = fixture.reunion(projet, type, 1, "R1-" + jeton, AVANT);
        UUID contact = fixture.tiers("MO-" + jeton);
        fixture.maitreOuvrage(precedente, contact);

        Reponse r = appels.creer(projet, type, null, DATE, null, admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeetingContact WHERE BulMeetingId = ? AND ContactId = ?",
                r.id(), contact)).isEqualTo(1);
    }

    @Test
    void r5_avancements_copies() throws Exception {
        UUID precedente = fixture.reunion(projet, type, 1, "R1-" + jeton, AVANT);
        UUID critere = fixture.critere(projet, "Gros oeuvre " + jeton);
        UUID niveau = fixture.niveau(projet);
        fixture.avancement(precedente, critere, niveau, new BigDecimal("0.4"));

        Reponse r = appels.creer(projet, type, null, DATE, null, admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat(fixture.compter("""
                SELECT COUNT(*) FROM BulMeetingPrjProjectProgressCriteriaLine WHERE BulMeetingId = ?
                AND PrjProjectProgressCriteriaLineId = ? AND CfgTrancheLevelingdefinitionId = ? AND Achievement = 0.4""",
                r.id(), critere, niveau)).isEqualTo(1);
    }

    @Test
    void r5_source_plusGrandOrdreDuMemeProjetEtType() throws Exception {
        UUID ancienne = fixture.reunion(projet, type, 1, "R1-" + jeton, AVANT.minusDays(7));
        fixture.decision(ancienne, 1, "De la première", ouvert, null);
        UUID derniere = fixture.reunion(projet, type, 2, "R2-" + jeton, AVANT);
        UUID retenue = fixture.decision(derniere, 1, "De la dernière", ouvert, null);
        UUID autreType = fixture.reunion(projet, fixture.type("RX-" + jeton), 9, "RX-" + jeton, AVANT);
        fixture.decision(autreType, 1, "D'un autre type", ouvert, null);

        Reponse r = appels.creer(projet, type, null, DATE, null, admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat(fixture.compter(DECISIONS_DE, r.id())).isEqualTo(1);
        assertThat(fixture.lire("SELECT CAST(BulMeetingLineId AS varchar(36)) FROM BulMeetingLine WHERE BulMeetingId = ?",
                r.id())).isEqualToIgnoringCase(retenue.toString());
    }

    // ------------------------------------------------------------------ R10

    @Test
    void r10_decisionFermee_egaleIsColsedDuStatut() throws Exception {
        UUID reunion = fixture.reunion(projet, type, 1, "R1-" + jeton, AVANT);
        UUID decisionFermee = fixture.decision(reunion, 1, "Fermée", ferme, null);
        UUID decisionOuverte = fixture.decision(reunion, 2, "Ouverte", ouvert, null);

        Reponse r = appels.appeler(get(REUNIONS + "/" + reunion), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.<List<Boolean>>lire("$.decisions[?(@.id == '" + decisionFermee + "')].fermee")).containsExactly(true);
        assertThat(r.<List<Boolean>>lire("$.decisions[?(@.id == '" + decisionOuverte + "')].fermee")).containsExactly(false);
    }

    // ------------------------------------------------------------------ R2 décision

    @Test
    void r2_decisionSansObservation_400() throws Exception {
        UUID reunion = nouvelle();

        Reponse r = appels.envoyer(post(REUNIONS + "/" + reunion + "/decisions"), "{\"description\":\"Sans observation\"}",
                admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(fixture.compter(DECISIONS_DE, reunion)).isZero();
    }

    // ------------------------------------------------------------------ R4 lignes / R6 maître d'ouvrage

    @Test
    void r4_maitresOuvrage_ordreNombreDeLignesPlus1() throws Exception {
        UUID reunion = nouvelle();
        UUID c1 = fixture.tiers("MO1-" + jeton);
        UUID c2 = fixture.tiers("MO2-" + jeton);

        Reponse ajout = ajouterMaitres(reunion, c1, c2);
        Reponse fiche = appels.appeler(get(REUNIONS + "/" + reunion), admin);

        assertThat(ajout.statut()).as(ajout.corps()).isBetween(200, 299);
        assertThat(fiche.<List<Integer>>lire("$.maitresOuvrage[*].ordre")).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void r6_maitreOuvrageDejaPresent_ignore() throws Exception {
        UUID reunion = nouvelle();
        UUID c1 = fixture.tiers("MO1-" + jeton);
        UUID c2 = fixture.tiers("MO2-" + jeton);
        Reponse premier = ajouterMaitres(reunion, c1);

        Reponse second = ajouterMaitres(reunion, c1, c2);

        assertThat(premier.statut()).as(premier.corps()).isBetween(200, 299);
        assertThat(second.statut()).as(second.corps()).isBetween(200, 299);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeetingContact WHERE BulMeetingId = ? AND ContactId = ?",
                reunion, c1)).isEqualTo(1);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeetingContact WHERE BulMeetingId = ?", reunion)).isEqualTo(2);
    }

    // ------------------------------------------------------------------ R9

    @Test
    void r9_routeFille_reunionInexistante_404() throws Exception {
        UUID contact = fixture.tiers("MO-" + jeton);

        Reponse r = ajouterMaitres(UUID.randomUUID(), contact);
        Reponse temoin = ajouterMaitres(nouvelle(), contact);

        assertThat(temoin.statut()).as("témoin : " + temoin.corps()).isBetween(200, 299);
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    // ------------------------------------------------------------------ R7 suppression d'une réunion

    @Test
    void r7_suppression_supprimeToutesLesLignesFilles() throws Exception {
        UUID reunion = fixture.reunion(projet, type, 1, "R1-" + jeton, AVANT);
        fixture.decision(reunion, 1, "Décision", ouvert, null);
        fixture.maitreOuvrage(reunion, fixture.tiers("MO-" + jeton));
        UUID intervenant = fixture.intervenant(reunion, fixture.tiers("FOU-" + jeton));
        fixture.contactIntervenant(intervenant, fixture.tiers("CTI-" + jeton));
        fixture.avancement(reunion, fixture.critere(projet, "Critère " + jeton), fixture.niveau(projet), BigDecimal.ONE);

        Reponse r = appels.appeler(delete(REUNIONS + "/" + reunion), admin);

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        assertThat(List.of(
                fixture.compter("SELECT COUNT(*) FROM BulMeeting WHERE PKey = ?", reunion),
                fixture.compter(DECISIONS_DE, reunion),
                fixture.compter("SELECT COUNT(*) FROM BulMeetingContact WHERE BulMeetingId = ?", reunion),
                fixture.compter("SELECT COUNT(*) FROM BulDelegate WHERE BulMeetingId = ?", reunion),
                fixture.compter("SELECT COUNT(*) FROM BulDelegateContact WHERE BulDelegateId = ?", intervenant),
                fixture.compter("SELECT COUNT(*) FROM BulMeetingPrjProjectProgressCriteriaLine WHERE BulMeetingId = ?", reunion)))
                .containsOnly(0);
    }

    @Test
    void r7_suppression_decisionRepriseParUneReunionSuivante_409() throws Exception {
        UUID reunion = fixture.reunion(projet, type, 1, "R1-" + jeton, AVANT);
        UUID decision = fixture.decision(reunion, 1, "Décision", ouvert, null);
        UUID suivante = fixture.reunion(projet, type, 2, "R2-" + jeton, AVANT.plusDays(7));
        fixture.decision(suivante, 1, "Décision", ouvert, decision);

        Reponse r = appels.appeler(delete(REUNIONS + "/" + reunion), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeeting WHERE PKey = ?", reunion)).isEqualTo(1);
        assertThat(fixture.compter(DECISIONS_DE, reunion)).isEqualTo(1);
    }
}
