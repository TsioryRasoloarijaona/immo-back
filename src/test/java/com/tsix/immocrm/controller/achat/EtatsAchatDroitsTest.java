package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureEtatsAchat;
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

import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.AU;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.AVANCES;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.AVANCES_EXPORT;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.DU;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.FORMAT;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.RETENUES;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.RETENUES_EXPORT;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.TVA;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.TVA_EXPORT;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.XLSX;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.annee;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.brut;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.statut;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Droits et paramètres des états Achat — FEATURE-n7-etats-achat.md D6, R11 (403), R8 (400 période), D5 (400 format), 401.
 * Les témoins 200 prouvent que le 403 vient du droit manquant et non d'une route absente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtatsAchatDroitsTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureEtatsAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID projet;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("N7D");
        societe = achat.societe("SOC-" + jeton);
        projet = achat.projet("PRJ-" + jeton, societe);
    }

    private RequestPostProcessor avecDroits(String... droits) {
        AdmUser compte = fixture.compte("N7D-" + UUID.randomUUID().toString().substring(0, 6) + "-" + jeton,
                societe, projet, droits);
        return connecte(compte, FixtureEtatsAchat.role());
    }

    private RequestPostProcessor tousLesDroits() {
        return avecDroits(FixtureEtatsAchat.TOUS_LES_DROITS);
    }

    // ------------------------------------------------------------------ 401

    @ParameterizedTest
    @ValueSource(strings = {TVA, AVANCES, RETENUES})
    void sansSession_401(String route) throws Exception {
        int code = statut(mockMvc, get(route).param(DU, FixtureEtatsAchat.DU).param(AU, FixtureEtatsAchat.AU));

        assertThat(code).isEqualTo(401);
    }

    // ------------------------------------------------------------------ R11 : 403

    @ParameterizedTest
    @ValueSource(strings = {TVA, AVANCES, RETENUES})
    void r11_sansLeDroitDeLEtat_403(String route) throws Exception {
        assertThat(statut(mockMvc, annee(route, avecDroits()))).isEqualTo(403);
    }

    @Test
    void r11_droitAvancesSeul_neDonnePasLaTva_403() throws Exception {
        assertThat(statut(mockMvc, annee(TVA, avecDroits(FixtureEtatsAchat.DROIT_AVANCES)))).isEqualTo(403);
    }

    @Test
    void r11_droitTva_200() throws Exception {
        assertThat(statut(mockMvc, annee(TVA, avecDroits(FixtureEtatsAchat.DROIT_TVA)))).isEqualTo(200);
    }

    @Test
    void r11_droitAvances_200() throws Exception {
        assertThat(statut(mockMvc, annee(AVANCES, avecDroits(FixtureEtatsAchat.DROIT_AVANCES)))).isEqualTo(200);
    }

    @Test
    void r11_droitRetenues_200() throws Exception {
        assertThat(statut(mockMvc, annee(RETENUES, avecDroits(FixtureEtatsAchat.DROIT_RETENUES)))).isEqualTo(200);
    }

    @Test
    void r11_exportTva_sansLeDroit_403() throws Exception {
        assertThat(statut(mockMvc, annee(TVA_EXPORT, avecDroits()))).isEqualTo(403);
    }

    @Test
    void r11_exportTva_avecLeDroitTva_200() throws Exception {
        assertThat(statut(mockMvc, annee(TVA_EXPORT, avecDroits(FixtureEtatsAchat.DROIT_TVA)))).isEqualTo(200);
    }

    @Test
    void r11_exportAvances_sansToolExport_403() throws Exception {
        assertThat(statut(mockMvc, annee(AVANCES_EXPORT, avecDroits(FixtureEtatsAchat.DROIT_AVANCES)))).isEqualTo(403);
    }

    @Test
    void r11_exportAvances_avecToolExport_200() throws Exception {
        RequestPostProcessor qui = avecDroits(FixtureEtatsAchat.DROIT_AVANCES, FixtureEtatsAchat.DROIT_AVANCES_EXPORT);

        assertThat(statut(mockMvc, annee(AVANCES_EXPORT, qui))).isEqualTo(200);
    }

    @Test
    void r11_exportRetenues_sansToolExport_403() throws Exception {
        assertThat(statut(mockMvc, annee(RETENUES_EXPORT, avecDroits(FixtureEtatsAchat.DROIT_RETENUES)))).isEqualTo(403);
    }

    @Test
    void r11_exportRetenues_avecToolExport_200() throws Exception {
        RequestPostProcessor qui = avecDroits(FixtureEtatsAchat.DROIT_RETENUES, FixtureEtatsAchat.DROIT_RETENUES_EXPORT);

        assertThat(statut(mockMvc, annee(RETENUES_EXPORT, qui))).isEqualTo(200);
    }

    // ------------------------------------------------------------------ R8 / D5 : 400

    @ParameterizedTest
    @ValueSource(strings = {TVA, AVANCES, RETENUES})
    void r8_duAbsent_400(String route) throws Exception {
        assertThat(statut(mockMvc, brut(route, tousLesDroits(), AU, FixtureEtatsAchat.AU))).isEqualTo(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {TVA, AVANCES, RETENUES})
    void r8_auAbsent_400(String route) throws Exception {
        assertThat(statut(mockMvc, brut(route, tousLesDroits(), DU, FixtureEtatsAchat.DU))).isEqualTo(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {TVA, AVANCES, RETENUES})
    void r8_duApresAu_400(String route) throws Exception {
        int code = statut(mockMvc, brut(route, tousLesDroits(), DU, "2093-12-31", AU, "2093-01-01"));

        assertThat(code).isEqualTo(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {TVA, AVANCES, RETENUES})
    void parametreMalForme_400(String route) throws Exception {
        int code = statut(mockMvc, brut(route, tousLesDroits(), DU, "31/12/2093", AU, FixtureEtatsAchat.AU));

        assertThat(code).isEqualTo(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {TVA, AVANCES, RETENUES})
    void r8_duEgalAu_200(String route) throws Exception {
        int code = statut(mockMvc, brut(route, tousLesDroits(), DU, "2093-06-01", AU, "2093-06-01"));

        assertThat(code).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {TVA_EXPORT, AVANCES_EXPORT, RETENUES_EXPORT})
    void d5_formatAutreQueXlsx_400(String route) throws Exception {
        int code = statut(mockMvc, brut(route, tousLesDroits(), DU, FixtureEtatsAchat.DU, AU, FixtureEtatsAchat.AU,
                FORMAT, "pdf"));

        assertThat(code).isEqualTo(400);
    }

    @ParameterizedTest
    @ValueSource(strings = {TVA_EXPORT, AVANCES_EXPORT, RETENUES_EXPORT})
    void d5_formatXlsx_200(String route) throws Exception {
        int code = statut(mockMvc, brut(route, tousLesDroits(), DU, FixtureEtatsAchat.DU, AU, FixtureEtatsAchat.AU,
                FORMAT, XLSX));

        assertThat(code).isEqualTo(200);
    }
}
