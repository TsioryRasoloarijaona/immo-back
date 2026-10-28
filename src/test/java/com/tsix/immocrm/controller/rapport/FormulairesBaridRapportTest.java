package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid;
import com.tsix.immocrm.fixtures.FixtureFormulairesBarid.Jeu;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureFormulairesBarid.CODE_RAPPORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * N° 35 FORMULAIRES DE BARID AL-MAGHRIB, section du hub (FEATURE-vente-formulaire-barid.md §4, §6 « Périmètre », §8.1
 * « Lot / rapport ») : une ligne par page imprimable, dans l'ordre d'impression. Jeu : {@link FixtureFormulairesBarid#jeu}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FormulairesBaridRapportTest {

    private static final String TRANCHE_ID = "trancheId";
    private static final String STATUT_ID = "statutId";
    private static final String SECTION = "destinataires";
    private static final String DOSSIER = "dossier";
    private static final String DESTINATAIRE = "destinataire";
    private static final String CODE_POSTAL = "codePostal";
    private static final String PARAMETRE = "$.parametres[?(@.nom == '%s')].%s";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureEtatVentes ev;
    @Autowired private FixtureFormulairesBarid fixture;

    private AppelsRapports appels;
    private AdmRole roleLibre;
    private RequestPostProcessor libre;
    private Jeu jeu;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        String p = FixtureEtatVentes.prefixe("BAMH");
        fixture.rapport();
        roleLibre = rapports.role("Libre " + p, false);
        libre = connecte(rapports.compte("bam.libre." + p, roleLibre));
        jeu = fixture.jeu(p);
    }

    private MockHttpServletRequestBuilder sur(RequestPostProcessor qui, Object... tranches) {
        MockHttpServletRequestBuilder requete = get(resultat(VENTE, CODE_RAPPORT)).with(qui);
        for (Object t : tranches) {
            requete.param(TRANCHE_ID, t.toString());
        }
        return requete;
    }

    private MockHttpServletRequestBuilder t1t2() {
        return sur(libre, jeu.t1().getId(), jeu.t2().getId());
    }

    private Map<String, Object> destinataires(MockHttpServletRequestBuilder requete) throws Exception {
        return section(appels.sections(requete), SECTION);
    }

    private static List<String> colonne(Map<String, Object> section, String nom) {
        return lignes(section).stream().map(l -> Objects.toString(l.get(nom), "")).toList();
    }

    private static String blancs(Object valeur) {
        return Objects.toString(valeur, "").replaceAll("\\s+", " ").trim();
    }

    private Map<String, Object> ligneDe(Map<String, Object> section, String racine) {
        List<Map<String, Object>> trouvees = lignes(section).stream()
                .filter(l -> Objects.toString(l.get(DESTINATAIRE), "").contains(jeu.nom(racine))).toList();
        assertThat(trouvees).as("ligne de %s", racine).hasSize(1);
        return trouvees.get(0);
    }

    // ------------------------------------------------------------------ forme

    @Test
    void r35_sectionDestinataires_huitColonnes() throws Exception {
        Map<String, Object> section = destinataires(t1t2());

        assertThat(colonnes(section).stream().map(c -> c.get("nom")).toList()).containsExactlyInAnyOrder("tranche",
                "statut", DOSSIER, "role", DESTINATAIRE, "adresse", CODE_POSTAL, "societe");
    }

    // ------------------------------------------------------------------ population et ordre

    @Test
    void r35_unePageParAcquereurEtCoAcquereur_triParNomTousDossiersConfondus() throws Exception {
        Map<String, Object> section = destinataires(t1t2());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(colonne(section, DESTINATAIRE)).hasSize(5);
        List<String> noms = colonne(section, DESTINATAIRE);
        for (int i = 0; i < FixtureFormulairesBarid.ORDRE.size() && i < noms.size(); i++) {
            soft.assertThat(noms.get(i)).as("ligne %d", i).endsWith(jeu.nom(FixtureFormulairesBarid.ORDRE.get(i)));
        }
        soft.assertThat(colonne(section, DOSSIER)).containsExactly(jeu.d2().getCode(), jeu.d1().getCode(),
                jeu.d2().getCode(), jeu.d5().getCode(), jeu.d1().getCode());
        soft.assertAll();
    }

    @Test
    void r35_role_acquereurOuCoAcquereur() throws Exception {
        Map<String, Object> section = destinataires(t1t2());

        assertThat(colonne(section, "role")).containsExactly("Acquéreur", "Acquéreur", "Co-acquéreur", "Acquéreur",
                "Co-acquéreur");
    }

    @Test
    void r35_dossiersAnnulesEtDesistes_absents() throws Exception {
        assertThat(colonne(destinataires(t1t2()), DOSSIER)).isNotEmpty()
                .doesNotContain(jeu.annule().getCode(), jeu.desiste().getCode());
    }

    @Test
    void r35_dossierSansStatut_present() throws Exception {
        assertThat(colonne(destinataires(t1t2()), DOSSIER)).contains(jeu.d5().getCode());
    }

    // ------------------------------------------------------------------ valeurs

    @Test
    void r35_valeurs_ligneAmrani() throws Exception {
        Map<String, Object> ligne = ligneDe(destinataires(t1t2()), "AMRANI");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(ligne).containsEntry("tranche", jeu.t2().getDescription())
                .containsEntry("statut", "S2-" + jeu.p())
                .containsEntry("societe", jeu.t2().getCfgProject().getCfgCompany().getDescription());
        soft.assertThat(blancs(ligne.get(DESTINATAIRE))).isEqualTo("Mr" + jeu.p() + " " + jeu.nom("AMRANI"));
        soft.assertThat(blancs(ligne.get("adresse")))
                .isEqualTo("1 RUE AMRANI 20250 VILLE" + jeu.p() + " PAYS-" + jeu.p());
        soft.assertThat(blancs(ligne.get(CODE_POSTAL))).isEqualTo("2 0 2 5 0");
        soft.assertAll();
    }

    @Test
    void r35_valeurs_codePostalNonNumeriqueTelQuel_etVideSiAbsent() throws Exception {
        Map<String, Object> section = destinataires(t1t2());

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(blancs(ligneDe(section, "ZIANI").get(CODE_POSTAL))).isEqualTo("B12");
        soft.assertThat(blancs(ligneDe(section, "BENNANI").get(CODE_POSTAL))).isEmpty();
        soft.assertAll();
    }

    // ------------------------------------------------------------------ filtres

    @Test
    void r35_filtreTranche() throws Exception {
        Map<String, Object> section = destinataires(sur(libre, jeu.t2().getId()));

        assertThat(colonne(section, DOSSIER)).containsExactly(jeu.d2().getCode(), jeu.d2().getCode());
    }

    @Test
    void r35_filtreStatut_unSeul() throws Exception {
        Map<String, Object> section = destinataires(t1t2().param(STATUT_ID, jeu.s1().getId().toString()));

        assertThat(colonne(section, DOSSIER)).containsExactly(jeu.d1().getCode(), jeu.d1().getCode());
    }

    @Test
    void r35_filtreStatut_plusieurs() throws Exception {
        Map<String, Object> section = destinataires(t1t2().param(STATUT_ID, jeu.s1().getId().toString())
                .param(STATUT_ID, jeu.s2().getId().toString()));

        assertThat(colonne(section, DOSSIER)).containsExactly(jeu.d2().getCode(), jeu.d1().getCode(),
                jeu.d2().getCode(), jeu.d1().getCode());
    }

    // ------------------------------------------------------------------ visibilité (règle 6, RV1)

    @Test
    void r35_trancheDUneSocieteInactive_vide() throws Exception {
        assertThat(lignes(destinataires(sur(libre, jeu.ti().getId())))).isEmpty();
    }

    @Test
    void r35_sansFiltre_societeInactiveAbsente() throws Exception {
        assertThat(colonne(destinataires(sur(libre)), DOSSIER)).contains(jeu.d1().getCode())
                .doesNotContain(jeu.inactif().getCode());
    }

    @Test
    void r35_trancheIdInconnuSeul_vide_jamaisToutes() throws Exception {
        assertThat(lignes(destinataires(sur(libre, UUID.randomUUID())))).isEmpty();
    }

    @Test
    void r35_trancheHorsPerimetreSeule_vide_jamaisToutes() throws Exception {
        AdmUser compte = rapports.compte("bam.perim." + jeu.p(), roleLibre);
        ev.affecter(compte, jeu.t2().getCfgProject());
        rapports.relire();

        assertThat(lignes(destinataires(sur(connecte(compte), jeu.t1().getId())))).isEmpty();
    }

    @Test
    void r35_perimetreCommercial_trancheHorsPerimetreIgnoree() throws Exception {
        AdmUser compte = rapports.compte("bam.perim2." + jeu.p(), roleLibre);
        ev.affecter(compte, jeu.t2().getCfgProject());
        rapports.relire();

        Map<String, Object> section = destinataires(sur(connecte(compte), jeu.t1().getId(), jeu.t2().getId()));

        assertThat(colonne(section, DOSSIER)).containsExactly(jeu.d2().getCode(), jeu.d2().getCode());
    }

    // ------------------------------------------------------------------ avertissements

    @Test
    void r35_avertissements_adresseCodePostalTitre() throws Exception {
        List<String> avertissements = JsonPath.read(appels.corps(t1t2()), "$.avertissements");

        assertThat(avertissements).contains("1 page(s) sans adresse", "3 page(s) sans code postal",
                "1 page(s) sans titre");
    }

    @Test
    void r35_avertissement_fondNonInstalle() throws Exception {
        fixture.semerModeles();

        List<String> avertissements = JsonPath.read(appels.corps(t1t2()), "$.avertissements");

        assertThat(avertissements).contains(
                "Le fond du formulaire n'est pas installé : l'impression sortira sans le fond pré-imprimé");
    }

    // ------------------------------------------------------------------ descripteur et export

    @Test
    void r35_descripteur_tranchesEtStatutsMultiples() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, CODE_RAPPORT)).with(libre));

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(JsonPath.<List<String>>read(corps, PARAMETRE.formatted(TRANCHE_ID, "type")))
                .containsExactly("TRANCHES");
        soft.assertThat(JsonPath.<List<Boolean>>read(corps, PARAMETRE.formatted(TRANCHE_ID, "multiple")))
                .containsExactly(true);
        soft.assertThat(JsonPath.<List<String>>read(corps, PARAMETRE.formatted(STATUT_ID, "type")))
                .containsExactly("CHOIX");
        soft.assertThat(JsonPath.<List<Boolean>>read(corps, PARAMETRE.formatted(STATUT_ID, "multiple")))
                .containsExactly(true);
        soft.assertAll();
    }

    @Test
    void r35_descripteur_statutsProposes_ni_annules_ni_desistes() throws Exception {
        String corps = appels.corps(get(descripteur(VENTE, CODE_RAPPORT)).with(libre));

        List<String> valeurs = JsonPath.read(corps, PARAMETRE.formatted(STATUT_ID, "options[*].valeur"));
        assertThat(valeurs).contains(jeu.s1().getId().toString(), jeu.s2().getId().toString())
                .doesNotContain(jeu.annulation().getId().toString(), jeu.desistement().getId().toString());
    }

    @Test
    void r35_export_uneFeuille() throws Exception {
        MvcResult reponse = appels.exporter(get(export(VENTE, CODE_RAPPORT)).param("format", "xlsx")
                .param(TRANCHE_ID, jeu.t1().getId().toString()).with(libre));

        assertThat(reponse.getResponse().getStatus()).isEqualTo(200);
        try (XSSFWorkbook classeur = new XSSFWorkbook(
                new ByteArrayInputStream(reponse.getResponse().getContentAsByteArray()))) {
            assertThat(classeur.getNumberOfSheets()).isEqualTo(1);
        }
    }
}
