package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.DELAIS_LIVRAISON;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.LISTE_DOSSIERS;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.NOTAIRES_CREDITS;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.REMISE_PIECES;
import static com.tsix.immocrm.fixtures.FixtureRapportsDossiers.VERIF_ATTACHEMENTS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits et enregistrement des cinq rapports (FEATURE-tdb-vente-rapports-dossiers.md §1 « Droits », §9 RD-R4,
 * « Transversal ») : n° 1, 28, 29, 32 liés au rôle « chef » (restreint) ; n° 33 sans aucune ligne de rôle, visible des
 * seuls rôles libres ; « commercial » restreint et lié à rien.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsDossiersDroitsTest {

    private static final int OK = 200;
    private static final int INTERDIT = 403;
    private static final List<String> A_ROLES = List.of(LISTE_DOSSIERS, NOTAIRES_CREDITS, DELAIS_LIVRAISON,
            REMISE_PIECES);
    private static final List<String> CINQ = List.of(LISTE_DOSSIERS, NOTAIRES_CREDITS, DELAIS_LIVRAISON,
            REMISE_PIECES, VERIF_ATTACHEMENTS);
    private static final String PARAMETRE = "$.parametres[?(@.nom == '%s')].%s";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;

    private AppelsRapports appels;
    private String prefixe;
    private AdmRole roleLibre;
    private AdmRole roleChef;
    private AdmRole roleCommercial;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("RDD");
        fixture.tables();
        roleLibre = fixture.role("Libre " + prefixe, false);
        roleChef = fixture.role("Commercial Chef " + prefixe, true);
        roleCommercial = fixture.role("Commercial " + prefixe, true);
        UUID n1 = fixture.rapportVente(LISTE_DOSSIERS, "LISTE DOSSIERS", null, 0);
        UUID n28 = fixture.rapportVente(NOTAIRES_CREDITS, "**ÉTAT SUIVI NOTAIRES CRÉDITS**", "VENTE", 2100);
        UUID n29 = fixture.rapportVente(DELAIS_LIVRAISON, "**ANALYSE DELAIS DE LIVRAISONS**", "VENTE", 2120);
        UUID n32 = fixture.rapportVente(REMISE_PIECES, "REMISE DE PIÈCES BANCAIRES", "VENTE", 2100);
        fixture.rapportVente(VERIF_ATTACHEMENTS, "VERIFICATION DES ATTACHEMENTS", "DOSSIERS DE VENTE", 10000);
        for (UUID rapport : List.of(n1, n28, n29, n32)) {
            fixture.lier(rapport, roleChef);
        }
        fixture.relire();
    }

    private RequestPostProcessor qui(String code, AdmRole role) {
        return connecte(fixture.compte(code + "." + prefixe, role));
    }

    // ------------------------------------------------------------------ liste

    @Test
    void rd_r4_roleLibre_voitLesCinq() throws Exception {
        assertThat(appels.codesVisibles(qui("libre", roleLibre))).containsAll(CINQ);
    }

    @Test
    void rd_r4_commercialChef_voitLesQuatreLies_pasLeN33() throws Exception {
        List<String> codes = appels.codesVisibles(qui("chef", roleChef));

        assertThat(codes).isNotEmpty().containsAll(A_ROLES).doesNotContain(VERIF_ATTACHEMENTS);
    }

    @Test
    void rd_r4_commercialNonLie_aucunDesCinq() throws Exception {
        List<String> codes = appels.codesVisibles(qui("com", roleCommercial));

        assertThat(codes.stream().filter(CINQ::contains).toList()).isEmpty();
    }

    // ------------------------------------------------------------------ exécution

    @Test
    void rd_r4_commercialNonLie_403SurLesCinq() throws Exception {
        RequestPostProcessor commercial = qui("com2", roleCommercial);

        SoftAssertions soft = new SoftAssertions();
        for (String code : CINQ) {
            soft.assertThat(appels.statut(get(descripteur(VENTE, code)).with(commercial))).as("descripteur %s", code)
                    .isEqualTo(INTERDIT);
            soft.assertThat(appels.statut(get(resultat(VENTE, code)).with(commercial))).as("resultat %s", code)
                    .isEqualTo(INTERDIT);
            soft.assertThat(appels.statut(get(export(VENTE, code)).param("format", "xlsx").with(commercial)))
                    .as("export %s", code).isEqualTo(INTERDIT);
        }
        soft.assertAll();
    }

    @Test
    void rd_r4_commercialChef_200SurLesQuatre_403SurLeN33() throws Exception {
        RequestPostProcessor chef = qui("chef2", roleChef);

        SoftAssertions soft = new SoftAssertions();
        for (String code : A_ROLES) {
            soft.assertThat(appels.statut(get(resultat(VENTE, code)).with(chef))).as("resultat %s", code)
                    .isEqualTo(OK);
        }
        soft.assertThat(appels.statut(get(descripteur(VENTE, VERIF_ATTACHEMENTS)).with(chef))).isEqualTo(INTERDIT);
        soft.assertThat(appels.statut(get(resultat(VENTE, VERIF_ATTACHEMENTS)).with(chef))).isEqualTo(INTERDIT);
        soft.assertAll();
    }

    /** Les cinq classes sont enregistrées (pas de 404 « code sans classe ») et répondent avec leurs défauts. */
    @Test
    void transversal_cinqClassesEnregistrees_resultatParDefaut200() throws Exception {
        RequestPostProcessor libre = qui("libre2", roleLibre);

        SoftAssertions soft = new SoftAssertions();
        for (String code : CINQ) {
            soft.assertThat(appels.statut(get(resultat(VENTE, code)).with(libre))).as(code).isEqualTo(OK);
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ descripteurs : paramètres du spec

    @Test
    void descripteur_n1_trancheIdMultiple() throws Exception {
        mockMvc.perform(get(descripteur(VENTE, LISTE_DOSSIERS)).with(qui("d1", roleLibre)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value(VENTE))
                .andExpect(jsonPath(PARAMETRE.formatted("trancheId", "type")).value("TRANCHES"))
                .andExpect(jsonPath(PARAMETRE.formatted("trancheId", "multiple")).value(true));
    }

    @Test
    void descripteur_n28_anneeTranchesNotairesMultiple() throws Exception {
        mockMvc.perform(get(descripteur(VENTE, NOTAIRES_CREDITS)).with(qui("d28", roleLibre)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PARAMETRE.formatted("annee", "type")).value("ANNEE"))
                .andExpect(jsonPath(PARAMETRE.formatted("trancheId", "type")).value("TRANCHES"))
                .andExpect(jsonPath(PARAMETRE.formatted("notaireId", "type")).value("CHOIX"))
                .andExpect(jsonPath(PARAMETRE.formatted("notaireId", "multiple")).value(true));
    }

    @Test
    void descripteur_n29_trancheId() throws Exception {
        mockMvc.perform(get(descripteur(VENTE, DELAIS_LIVRAISON)).with(qui("d29", roleLibre)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PARAMETRE.formatted("trancheId", "type")).value("TRANCHES"));
    }

    @Test
    void descripteur_n32_duAuTranches() throws Exception {
        mockMvc.perform(get(descripteur(VENTE, REMISE_PIECES)).with(qui("d32", roleLibre)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(PARAMETRE.formatted("du", "type")).value("DATE"))
                .andExpect(jsonPath(PARAMETRE.formatted("au", "type")).value("DATE"))
                .andExpect(jsonPath(PARAMETRE.formatted("trancheId", "type")).value("TRANCHES"));
    }

    @Test
    void descripteur_n33_moduleVente_tranchesEtStatutTexte() throws Exception {
        mockMvc.perform(get(descripteur(VENTE, VERIF_ATTACHEMENTS)).with(qui("d33", roleLibre)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module").value(VENTE))
                .andExpect(jsonPath(PARAMETRE.formatted("trancheId", "type")).value("TRANCHES"))
                .andExpect(jsonPath(PARAMETRE.formatted("statut", "type")).value("TEXTE"));
    }
}
