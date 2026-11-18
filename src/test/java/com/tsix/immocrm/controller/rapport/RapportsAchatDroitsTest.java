package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import com.tsix.immocrm.web.export.ExportTableur;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.liste;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.ACHAT;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.ANNEE;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.AU;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.DU;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.FORMAT;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.PDF;
import static com.tsix.immocrm.controller.rapport.RapportsAchatSupport.XLSX;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.BONS_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.CODES;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.DEPENSES_PAR_MOIS;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.ECHEANCIER_CREDITS;
import static com.tsix.immocrm.fixtures.FixtureRapportsAchat.RETENUES_LOTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Module {@code achat} du socle des rapports (FEATURE-n7-etats-achat.md N7b) : liste, descripteur, droits
 * {@code AdmReportDataRole}, export xlsx seul (Q3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsAchatDroitsTest {

    private static final int OK = 200;
    private static final int INTERDIT = 403;
    private static final String ANNEE_TEST = String.valueOf(FixtureRapportsAchat.ANNEE);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureRapportsAchat fixture;

    private AppelsRapports appels;
    private String jeton;
    private Map<String, UUID> rapports;
    private UUID societe;
    private UUID projet;
    private AdmUser libre;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        jeton = FixtureAchat.jeton("N7BD");
        rapports = fixture.rapports();
        societe = achat.societe("S-" + jeton);
        projet = achat.projet("P-" + jeton, societe);
        libre = fixture.compte("LIB-" + jeton, false, societe, projet);
    }

    private static RequestPostProcessor qui(AdmUser compte) {
        return connecte(compte, FixtureRapportsAchat.role());
    }

    /** Paramètres obligatoires du rapport (R12, R15 : année ; R14 : période). */
    private static MockHttpServletRequestBuilder avecParametres(MockHttpServletRequestBuilder requete, String code) {
        switch (code) {
            case DEPENSES_PAR_MOIS, ECHEANCIER_CREDITS -> requete.param(ANNEE, ANNEE_TEST);
            case BONS_COMMANDE -> requete.param(DU, FixtureRapportsAchat.DU).param(AU, FixtureRapportsAchat.AU);
            default -> {
                // R13 : aucun paramètre obligatoire
            }
        }
        return requete;
    }

    private List<String> codesVisibles(AdmUser compte) throws Exception {
        return JsonPath.read(appels.corps(get(liste(ACHAT)).with(qui(compte))), "$[*].rapports[*].code");
    }

    private MockHttpServletResponse exporter(String code, String format, AdmUser compte) throws Exception {
        return appels.exporter(avecParametres(get(export(ACHAT, code)), code).param(FORMAT, format).with(qui(compte)))
                .getResponse();
    }

    @Test
    void liste_compteLibre_lesQuatreRapportsAchat() throws Exception {
        assertThat(codesVisibles(libre)).containsExactlyInAnyOrderElementsOf(CODES);
    }

    @ParameterizedTest
    @ValueSource(strings = {DEPENSES_PAR_MOIS, RETENUES_LOTS, BONS_COMMANDE, ECHEANCIER_CREDITS})
    void q3_descripteur_moduleAchat_exportXlsxSeul(String code) throws Exception {
        String corps = appels.corps(get(descripteur(ACHAT, code)).with(qui(libre)));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat((String) JsonPath.read(corps, "$.code")).isEqualTo(code);
            s.assertThat((String) JsonPath.read(corps, "$.module")).isEqualTo(ACHAT);
            s.assertThat((List<String>) JsonPath.read(corps, "$.formatsExport")).containsExactly(XLSX);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {DEPENSES_PAR_MOIS, RETENUES_LOTS, BONS_COMMANDE, ECHEANCIER_CREDITS})
    void resultat_compteLibre_200(String code) throws Exception {
        assertThat(appels.statut(avecParametres(get(resultat(ACHAT, code)), code).with(qui(libre)))).isEqualTo(OK);
    }

    @Test
    void compteRestreintSansLien_listeVide_et403Partout() throws Exception {
        AdmUser restreint = fixture.compte("RES-" + jeton, true, societe, projet);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(codesVisibles(libre)).as("témoin libre").containsAll(CODES);
        soft.assertThat(codesVisibles(restreint)).as("liste du restreint").isEmpty();
        for (String code : CODES) {
            soft.assertThat(appels.statut(get(descripteur(ACHAT, code)).with(qui(restreint)))).as("descripteur %s", code)
                    .isEqualTo(INTERDIT);
            soft.assertThat(appels.statut(avecParametres(get(resultat(ACHAT, code)), code).with(qui(restreint))))
                    .as("résultat %s", code).isEqualTo(INTERDIT);
            soft.assertThat(exporter(code, XLSX, restreint).getStatus()).as("export %s", code).isEqualTo(INTERDIT);
            soft.assertThat(appels.statut(avecParametres(get(resultat(ACHAT, code)), code).with(qui(libre))))
                    .as("témoin libre %s", code).isEqualTo(OK);
        }
        soft.assertAll();
    }

    @Test
    void compteRestreintLieAUnRapport_neVoitQueCeluiLa() throws Exception {
        AdmUser restreint = fixture.compte("LIE-" + jeton, true, societe, projet);
        fixture.lier(restreint, rapports.get(RETENUES_LOTS));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(codesVisibles(restreint)).containsExactly(RETENUES_LOTS);
        soft.assertThat(appels.statut(get(resultat(ACHAT, RETENUES_LOTS)).with(qui(restreint)))).as("lié")
                .isEqualTo(OK);
        soft.assertThat(appels.statut(avecParametres(get(resultat(ACHAT, BONS_COMMANDE)), BONS_COMMANDE)
                .with(qui(restreint)))).as("non lié").isEqualTo(INTERDIT);
        soft.assertAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {DEPENSES_PAR_MOIS, RETENUES_LOTS, BONS_COMMANDE, ECHEANCIER_CREDITS})
    void export_xlsx_200_classeur(String code) throws Exception {
        MockHttpServletResponse reponse = exporter(code, XLSX, libre);

        assertThat(reponse.getStatus()).isEqualTo(OK);
        assertThat(reponse.getContentType()).isEqualTo(ExportTableur.XLSX.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {DEPENSES_PAR_MOIS, RETENUES_LOTS, BONS_COMMANDE, ECHEANCIER_CREDITS})
    void q3_export_pdf_refuse_400ou404(String code) throws Exception {
        int xlsx = exporter(code, XLSX, libre).getStatus();
        int pdf = exporter(code, PDF, libre).getStatus();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(xlsx).as("témoin xlsx").isEqualTo(OK);
            s.assertThat(pdf).as("pdf").isIn(400, 404);
        });
    }
}
