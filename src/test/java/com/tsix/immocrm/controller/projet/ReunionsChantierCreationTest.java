package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsReunions.Reponse;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureReunionsChantier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureReunionsChantier.REUNIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Création d'une réunion : R1 (prochaine date), R2 (obligatoires), D2/R3 (Réf. P.V.), R4 (O.D.T.). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReunionsChantierCreationTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 10);
    private static final String MESSAGE_R1 =
            "La date de la prochaine réunion doit être supérieure à la date de cette réunion.";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureReunionsChantier fixture;

    private AppelsReunions appels;
    private String jeton;
    private UUID projet;
    private UUID type;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        fixture.tables();
        appels = new AppelsReunions(mockMvc, fixture);
        jeton = FixtureProjetLots.jeton();
        projet = fixture.projet("PRC-" + jeton);
        type = fixture.type("RC-" + jeton);
        admin = fixture.admin(jeton, projet);
    }

    // ------------------------------------------------------------------ R1

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void r1_prochaineDate_memeJourOuAvant_400(int decalage) throws Exception {
        Reponse r = appels.creer(projet, type, null, DATE, DATE.plusDays(decalage), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(400);
        assertThat(r.corps()).contains(MESSAGE_R1);
    }

    @Test
    void r1_prochaineDate_lendemain_acceptee() throws Exception {
        Reponse r = appels.creer(projet, type, null, DATE, DATE.plusDays(1), admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeeting WHERE PKey = ? AND NextDate IS NOT NULL", r.id()))
                .isEqualTo(1);
    }

    @Test
    void r1_prochaineDate_vide_acceptee() throws Exception {
        Reponse r = appels.creer(projet, type, null, DATE, null, admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
    }

    // ------------------------------------------------------------------ R2

    @ParameterizedTest
    @ValueSource(strings = {"projetId", "typeId", "date"})
    void r2_reunion_champObligatoireManquant_400(String champ) throws Exception {
        String complet = "{\"projetId\":\"%s\",\"typeId\":\"%s\",\"date\":\"%s\"}".formatted(projet, type, DATE);
        String json = complet.replaceFirst("\"" + champ + "\":\"[^\"]*\",?", "").replace(",}", "}");

        Reponse r = appels.envoyer(post(REUNIONS), json, admin);

        assertThat(r.statut()).as(json + " → " + r.corps()).isEqualTo(400);
    }

    @Test
    void r2_temoin_reunionComplete_201() throws Exception {
        Reponse r = appels.creer(projet, type, null, DATE, null, admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeeting WHERE PKey = ? AND PrjProjectId = ? AND BulMeetingTypeId = ?",
                r.id(), projet, type)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ D2 / R3

    @Test
    void d2_referenceVide_generee_yy_numeroSuivantSur5Chiffres_puisSuivant() throws Exception {
        LocalDate aujourdhui = LocalDate.now(ZoneId.of("Africa/Casablanca"));
        String annee = "%02d".formatted(aujourdhui.getYear() % 100);
        int dernier = dernierNumero(annee);

        Reponse premiere = appels.creer(projet, type, null, aujourdhui, null, admin);
        Reponse seconde = appels.creer(projet, type, "", aujourdhui, null, admin);

        assertThat(premiere.statut()).as(premiere.corps()).isEqualTo(201);
        assertThat(seconde.statut()).as(seconde.corps()).isEqualTo(201);
        assertThat((String) premiere.lire("$.reference")).isEqualTo("%s/%05d".formatted(annee, dernier + 1));
        assertThat((String) seconde.lire("$.reference")).isEqualTo("%s/%05d".formatted(annee, dernier + 2));
    }

    @Test
    void d2_referenceSaisie_gardee() throws Exception {
        Reponse r = appels.creer(projet, type, "PV-" + jeton, DATE, null, admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat((String) r.lire("$.reference")).isEqualTo("PV-" + jeton);
        assertThat(fixture.lire("SELECT Code FROM BulMeeting WHERE PKey = ?", r.id())).isEqualTo("PV-" + jeton);
    }

    @Test
    void d2_referenceEnDoublon_409() throws Exception {
        Reponse premiere = appels.creer(projet, type, "PV-" + jeton, DATE, null, admin);

        Reponse doublon = appels.creer(projet, type, "PV-" + jeton, DATE.plusDays(7), null, admin);

        assertThat(premiere.statut()).as("témoin").isEqualTo(201);
        assertThat(doublon.statut()).as(doublon.corps()).isEqualTo(409);
        assertThat(fixture.compter("SELECT COUNT(*) FROM BulMeeting WHERE Code = ?", "PV-" + jeton)).isEqualTo(1);
    }

    private int dernierNumero(String annee) {
        String n = fixture.lire("""
                SELECT CAST(MAX(CAST(SUBSTRING(Code, 4, 5) AS int)) AS varchar(10)) FROM BulMeeting
                WHERE Code LIKE ? AND LEN(Code) = 8""", annee + "/[0-9][0-9][0-9][0-9][0-9]");
        return n == null ? 0 : Integer.parseInt(n);
    }

    // ------------------------------------------------------------------ R4

    @Test
    void r4_premiereReunionDuProjetEtDuType_ordre1() throws Exception {
        Reponse r = appels.creer(projet, type, null, DATE, null, admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat((Integer) r.lire("$.ordre")).isEqualTo(1);
    }

    @Test
    void r4_ordre_dernierDuMemeProjetEtType_plus1() throws Exception {
        fixture.reunion(projet, type, 7, "R7-" + jeton, LocalDateTime.of(2026, 2, 1, 9, 0));
        fixture.reunion(projet, type, 3, "R3-" + jeton, LocalDateTime.of(2026, 1, 1, 9, 0));

        Reponse r = appels.creer(projet, type, null, DATE, null, admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat((Integer) r.lire("$.ordre")).isEqualTo(8);
    }

    @Test
    void r4_ordre_autreTypeOuAutreProjet_nonCompte() throws Exception {
        UUID autreType = fixture.type("RX-" + jeton);
        UUID autreProjet = fixture.projet("PRX-" + jeton);
        fixture.reunion(projet, autreType, 5, "RT-" + jeton, LocalDateTime.of(2026, 2, 1, 9, 0));
        fixture.reunion(autreProjet, type, 9, "RP-" + jeton, LocalDateTime.of(2026, 2, 1, 9, 0));

        Reponse r = appels.creer(projet, type, null, DATE, null, admin);

        assertThat(r.statut()).as(r.corps()).isEqualTo(201);
        assertThat((Integer) r.lire("$.ordre")).isEqualTo(1);
    }
}
