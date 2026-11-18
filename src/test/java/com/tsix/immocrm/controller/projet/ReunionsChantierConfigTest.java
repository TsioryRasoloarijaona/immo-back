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

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.PLAGES;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.STATUTS;
import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.TYPES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Configuration des réunions : R2 (obligatoires) et R7 (suppression d'un type ou statut cité refusée, plage libre). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReunionsChantierConfigTest {

    private static final LocalDateTime LE_10_MARS = LocalDateTime.of(2026, 3, 10, 9, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReunionsChantier fixture;

    private AppelsReunions appels;
    private String jeton;
    private UUID projet;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        fixture.tables();
        appels = new AppelsReunions(mockMvc, fixture);
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PRG-" + jeton);
        admin = fixture.admin(jeton, projet);
    }

    // ------------------------------------------------------------------ R2

    @Test
    void r2_typeReunion_sansCodeNiDescription_400() throws Exception {
        Reponse r = appels.envoyer(post(TYPES), "{}", admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    @Test
    void r2_plageCouleurs_sansCouleurNiDescription_400() throws Exception {
        Reponse r = appels.envoyer(post(PLAGES), "{}", admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ R7

    @Test
    void r7_typeReunionCiteParUneReunion_409() throws Exception {
        UUID type = fixture.type("RG-" + jeton);
        fixture.reunion(projet, type, 1, "R1-" + jeton, LE_10_MARS);

        Reponse r = appels.appeler(delete(TYPES + "/" + type), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeetingType WHERE PKey = ?", type)).isEqualTo(1);
    }

    @Test
    void r7_temoin_typeReunionLibre_supprime() throws Exception {
        UUID type = fixture.type("RL-" + jeton);

        Reponse r = appels.appeler(delete(TYPES + "/" + type), admin);

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeetingType WHERE PKey = ?", type)).isZero();
    }

    @Test
    void r7_statutReserveCiteParUneDecision_409() throws Exception {
        UUID statut = fixture.statut("SG-" + jeton, false);
        UUID reunion = fixture.reunion(projet, fixture.type("RG-" + jeton), 1, "R1-" + jeton, LE_10_MARS);
        fixture.decision(reunion, 1, "Décision", statut, null);

        Reponse r = appels.appeler(delete(STATUTS + "/" + statut), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(409);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulObservationStatus WHERE PKey = ?", statut)).isEqualTo(1);
    }

    @Test
    void r7_temoin_statutReserveLibre_supprime() throws Exception {
        UUID statut = fixture.statut("SL-" + jeton, true);

        Reponse r = appels.appeler(delete(STATUTS + "/" + statut), admin);

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulObservationStatus WHERE PKey = ?", statut)).isZero();
    }

    @Test
    void r7_plageCouleurs_toujoursSupprimable() throws Exception {
        UUID plage = fixture.plage("Plage " + jeton);

        Reponse r = appels.appeler(delete(PLAGES + "/" + plage), admin);

        assertThat(r.statut()).as(r.corps()).isBetween(200, 299);
        assertThat(fixture.compter("SELECT COUNT(*) FROM PrjMeetingConfigColor WHERE PKey = ?", plage)).isZero();
    }
}
