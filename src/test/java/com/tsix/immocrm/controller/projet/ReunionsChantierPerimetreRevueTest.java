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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.REUNIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Revue règle 6 (2e tour) : écritures sur une réunion hors périmètre et lignes filles d'un projet non habilité. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReunionsChantierPerimetreRevueTest {

    private static final LocalDateTime LE_10_MARS = LocalDateTime.of(2026, 3, 10, 9, 30);
    private static final String DECISIONS = "/decisions";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReunionsChantier fixture;

    private AppelsReunions appels;
    private String jeton;
    private UUID projet;
    private UUID projetNonHabilite;
    private UUID type;
    private UUID ouvert;
    private UUID visible;
    private UUID invisible;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        fixture.tables();
        appels = new AppelsReunions(mockMvc, fixture);
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PPA-" + jeton);
        projetNonHabilite = fixture.projet("PPN-" + jeton);
        type = fixture.type("RP-" + jeton);
        ouvert = fixture.statut("OPP-" + jeton, false);
        visible = fixture.reunion(projet, type, 1, "PVI-" + jeton, LE_10_MARS);
        invisible = fixture.reunion(projetNonHabilite, type, 1, "PIN-" + jeton, LE_10_MARS);
        admin = fixture.admin(jeton, projet);
    }

    private void introuvable(Reponse r) {
        assertThat(r.statut()).as(r.corps()).isEqualTo(404);
    }

    @Test
    void creation_projetNonHabilite_404_sansLigne() throws Exception {
        Reponse r = appels.creer(projetNonHabilite, type, "PCR-" + jeton, LocalDate.of(2026, 3, 12), null, admin);

        introuvable(r);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeeting WHERE Code = ?", "PCR-" + jeton)).isZero();
    }

    @Test
    void modificationEtSuppression_reunionHorsPerimetre_404_inchangee() throws Exception {
        Reponse maj = appels.envoyer(put(REUNIONS + "/" + invisible),
                "{\"reference\":\"PIR-" + jeton + "\",\"date\":\"2026-03-11\"}", admin);
        Reponse suppression = appels.appeler(delete(REUNIONS + "/" + invisible), admin);

        introuvable(maj);
        introuvable(suppression);
        assertThat(fixture.lire("SELECT Code FROM BulMeeting WHERE PKey = ?", invisible)).isEqualTo("PIN-" + jeton);
    }

    @Test
    void onglets_reunionHorsPerimetre_404_sansEcriture() throws Exception {
        UUID decision = fixture.decision(invisible, 1, "Hors périmètre", ouvert, null);
        UUID tiers = fixture.tiers("PTI-" + jeton);
        UUID maitre = fixture.maitreOuvrage(invisible, tiers);
        String base = REUNIONS + "/" + invisible;

        introuvable(appels.envoyer(post(base + DECISIONS),
                "{\"description\":\"Injectée\",\"observationId\":\"" + ouvert + "\"}", admin));
        introuvable(appels.envoyer(put(base + DECISIONS + "/" + decision),
                "{\"description\":\"Détournée\",\"observationId\":\"" + ouvert + "\"}", admin));
        introuvable(appels.appeler(delete(base + DECISIONS + "/" + decision), admin));
        introuvable(appels.envoyer(post(base + "/intervenants"), "{\"fournisseurId\":\"" + tiers + "\"}", admin));
        introuvable(appels.envoyer(post(base + "/maitres-ouvrage"), "{\"contactIds\":[\"" + tiers + "\"]}", admin));
        introuvable(appels.envoyer(patch(base + "/maitres-ouvrage/" + maitre), "{\"aNotifier\":true}", admin));

        assertThat(fixture.lire("SELECT Description FROM BulMeetingLine WHERE PKey = ?", decision))
                .isEqualTo("Hors périmètre");
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeetingLine WHERE BulMeetingId = ?", invisible)).isEqualTo(1);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulDelegate WHERE BulMeetingId = ?", invisible)).isZero();
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeetingContact WHERE BulMeetingId = ? AND Notify = 1",
                invisible)).isZero();
    }

    /** IDOR : ligne fille d'une réunion hors périmètre visée par la route d'une réunion visible. */
    @Test
    void ligneDUneReunionHorsPerimetre_viaReunionVisible_404() throws Exception {
        UUID decision = fixture.decision(invisible, 1, "Hors périmètre", ouvert, null);
        UUID intervenant = fixture.intervenant(invisible, fixture.tiers("PFI-" + jeton));

        introuvable(appels.envoyer(put(REUNIONS + "/" + visible + DECISIONS + "/" + decision),
                "{\"description\":\"Détournée\",\"observationId\":\"" + ouvert + "\"}", admin));
        introuvable(appels.appeler(delete(REUNIONS + "/" + visible + "/intervenants/" + intervenant), admin));

        assertThat(fixture.lire("SELECT Description FROM BulMeetingLine WHERE PKey = ?", decision))
                .isEqualTo("Hors périmètre");
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulDelegate WHERE PKey = ?", intervenant)).isEqualTo(1);
    }

    @Test
    void avancement_critereDUnProjetNonHabilite_400() throws Exception {
        UUID critere = fixture.critere(projetNonHabilite, "Gros œuvre " + jeton);
        UUID niveau = fixture.niveau(projetNonHabilite);

        Reponse r = appels.envoyer(put(REUNIONS + "/" + visible + "/avancements"),
                "{\"critereId\":\"" + critere + "\",\"niveauId\":\"" + niveau + "\",\"pourcentage\":50}", admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeetingPrjProjectProgressCriteriaLine WHERE BulMeetingId = ?",
                visible)).isZero();
    }
}
