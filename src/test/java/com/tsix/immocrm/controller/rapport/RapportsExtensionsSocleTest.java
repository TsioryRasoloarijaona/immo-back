package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.services.equipe.RolesEquipe;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.colonnes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportExtensionsDeTest.CODE;
import static com.tsix.immocrm.controller.rapport.RapportExtensionsDeTest.COMMERCIAL_ID;
import static com.tsix.immocrm.controller.rapport.RapportExtensionsDeTest.ETAT;
import static com.tsix.immocrm.controller.rapport.RapportExtensionsDeTest.SERIE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ajouts au socle B2–B5 (FEATURE-tdb-vente-rapports-socle.md, « Ajouts au socle pour les lots suivants », font foi), via
 * le rapport de test {@link RapportExtensionsDeTest}. B1 est testé sur les rapports réels (n° 12 et n° 13).
 * <pre>
 *   commercial A : rôle « Commercial » + rôle libre pour les rapports — ne voit que lui-même (CommerciauxVisibles)
 *   commercial B : rôle « Commercial », hors de la portée de A
 * </pre>
 * Clé d'un commercial sur le fil = son employé ({@code CommerciauxVisibles.cle}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(RapportExtensionsDeTest.class)
class RapportsExtensionsSocleTest {

    private static final String DYNAMIQUE = "dynamique";
    private static final String COMMERCIAUX = "commerciaux";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;

    private AppelsRapports appels;
    private String prefixe;
    private AdmRole roleLibre;
    private AdmUser commercialA;
    private AdmUser commercialB;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("RXS");
        fixture.tables();
        fixture.rapportVente(CODE, "TEST SOCLE EXTENSIONS", "", 1);
        roleLibre = fixture.role("Libre " + prefixe, false);
        commercialA = fixture.compte("xs.a." + prefixe, roleLibre, fixture.role(RolesEquipe.COMMERCIAL, true));
        commercialB = fixture.compte("xs.b." + prefixe, fixture.role(RolesEquipe.COMMERCIAL, true));
        fixture.relire();
    }

    private static String cle(AdmUser commercial) {
        return commercial.getCfgEmployee().getId().toString();
    }

    private List<String> options(RequestPostProcessor qui, String parametre) throws Exception {
        return JsonPath.read(appels.corps(get(descripteur(VENTE, CODE)).with(qui)),
                "$.parametres[?(@.nom == '" + parametre + "')].options[*].valeur");
    }

    private MockHttpServletRequestBuilder etats(String... valeurs) {
        return get(resultat(VENTE, CODE)).param(ETAT, valeurs).with(connecte(commercialA));
    }

    private static List<Object> nomsDeColonnes(Map<String, Object> section) {
        return colonnes(section).stream().map(c -> c.get("nom")).toList();
    }

    // ------------------------------------------------------------------ B2 : paramètre COMMERCIAUX

    @Test
    void b2_descripteur_typeCommerciauxMultiple_optionsDeLaPorteeDeLAppelant() throws Exception {
        RequestPostProcessor a = connecte(commercialA);

        mockMvc.perform(get(descripteur(VENTE, CODE)).with(a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametres[?(@.nom == 'commercialId')].type").value("COMMERCIAUX"))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'commercialId')].multiple").value(true));
        assertThat(options(a, COMMERCIAL_ID)).contains(cle(commercialA)).doesNotContain(cle(commercialB));
    }

    @Test
    void b2_exempte_optionsTousLesCommerciaux() throws Exception {
        AdmUser direction = fixture.compte("xs.dg." + prefixe, fixture.role("Direction Générale", false));
        fixture.relire();

        assertThat(options(connecte(direction), COMMERCIAL_ID)).contains(cle(commercialA), cle(commercialB));
    }

    @Test
    void b2_idCommercialHorsPortee_ignoreSansErreur_absentDesDonnees() throws Exception {
        List<Map<String, Object>> sections = appels.sections(get(resultat(VENTE, CODE))
                .param(COMMERCIAL_ID, cle(commercialB)).with(connecte(commercialA)));

        assertThat(lignes(section(sections, COMMERCIAUX))).extracting(l -> l.get(COMMERCIAL_ID))
                .doesNotContain(cle(commercialB));
    }

    @Test
    void b2_contexte_commerciauxVisibles_porteeDeLAppelant() throws Exception {
        List<Map<String, Object>> sections = appels.sections(get(resultat(VENTE, CODE)).with(connecte(commercialA)));

        assertThat(lignes(section(sections, COMMERCIAUX))).extracting(l -> l.get(COMMERCIAL_ID))
                .contains(cle(commercialA)).doesNotContain(cle(commercialB));
    }

    // ------------------------------------------------------------------ B4 : CHOIX multiple, options en base

    @Test
    void b4_choixMultiple_optionsChargeesParLeRapport() throws Exception {
        RequestPostProcessor a = connecte(commercialA);

        mockMvc.perform(get(descripteur(VENTE, CODE)).with(a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parametres[?(@.nom == 'etat')].type").value("CHOIX"))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'etat')].multiple").value(true));
        assertThat(options(a, ETAT)).containsExactly("A", "B");
    }

    @Test
    void b4_choixMultiple_valeursRecuesParLeContexte_etAppliquees() throws Exception {
        String corps = appels.corps(etats("A", "B"));

        List<String> appliquees = JsonPath.read(corps, "$.parametres.etat");
        List<Map<String, Object>> sections = JsonPath.read(corps, "$.sections");
        assertThat(appliquees).containsExactly("A", "B");
        assertThat(nomsDeColonnes(section(sections, DYNAMIQUE))).containsExactly(SERIE, "A", "B");
    }

    @Test
    void b4_choixHorsOptions_400() throws Exception {
        mockMvc.perform(etats("A", "Z")).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ B3 : graphique LIGNES

    @Test
    void b3_graphiqueLignes() throws Exception {
        String corps = appels.corps(etats("A", "B"));

        List<String> types = JsonPath.read(corps, "$.sections[?(@.id == 'dynamique')].graphiques[*].type");
        List<List<String>> series = JsonPath.read(corps,
                "$.sections[?(@.id == 'dynamique')].graphiques[*].colonnesValeurs");
        assertThat(types).containsExactly("LIGNES");
        assertThat(series).containsExactly(List.of("A", "B"));
    }

    // ------------------------------------------------------------------ B5 : colonnes dynamiques par résultat

    @Test
    void b5_colonnesPropresAChaqueResultat_etTotauxSurCesColonnes() throws Exception {
        Map<String, Object> uneColonne = section(appels.sections(etats("A")), DYNAMIQUE);
        Map<String, Object> deuxColonnes = section(appels.sections(etats("A", "B")), DYNAMIQUE);

        assertThat(nomsDeColonnes(uneColonne)).containsExactly(SERIE, "A");
        assertThat(nomsDeColonnes(deuxColonnes)).containsExactly(SERIE, "A", "B");
        assertThat(nombre(totaux(deuxColonnes).get("B"))).isEqualByComparingTo("1");
    }

    @Test
    void b5_exportSuitLesColonnesRecues() throws Exception {
        MockHttpServletResponse reponse = appels.exporter(get(export(VENTE, CODE)).param("format", "xlsx")
                .param(ETAT, "A", "B").with(connecte(commercialA))).getResponse();

        assertThat(reponse.getStatus()).isEqualTo(200);
        try (XSSFWorkbook classeur = new XSSFWorkbook(new ByteArrayInputStream(reponse.getContentAsByteArray()))) {
            Row entete = classeur.getSheet("Section dynamique").getRow(0);
            List<String> libelles = new ArrayList<>();
            for (Cell cell : entete) {
                libelles.add(cell.toString());
            }
            assertThat(libelles).containsExactly("Colonne serie", "Colonne A", "Colonne B");
        }
    }
}
