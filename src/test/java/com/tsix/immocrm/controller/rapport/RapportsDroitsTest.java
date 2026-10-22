package com.tsix.immocrm.controller.rapport;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.fixtures.FixtureEtatVentes;
import com.tsix.immocrm.fixtures.FixtureRapports;
import org.assertj.core.api.SoftAssertions;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.VENTE;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.descripteur;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.export;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.liste;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.resultat;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureRapports.AUTRES_RAPPORTS;
import static com.tsix.immocrm.fixtures.FixtureRapports.REPARTITION_CLIENTS;
import static com.tsix.immocrm.fixtures.FixtureRapports.SUIVI_GLOBAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Droits par rôle (FEATURE-tdb-vente-rapports-socle.md §2 RD1–RD6, §4, §9.1 « Droits », « 404 »).
 * <pre>
 *   n° 12 (SUIVI_GLOBAL)        lié deux fois au rôle « chef » (restreint) et une fois au rôle NULL
 *   n° 13 (REPARTITION_CLIENTS) aucune ligne de rôle
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsDroitsTest {

    private static final int OK = 200;
    private static final int INTERDIT = 403;
    private static final int INTROUVABLE = 404;
    private static final String FORMAT = "format";
    private static final String XLSX = "xlsx";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureRapports fixture;

    private AppelsRapports appels;
    private String prefixe;
    private UUID rapport13;
    private AdmRole roleLibre;
    private AdmRole roleChef;
    private AdmRole roleSansLien;
    private AdmRole roleNull;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        prefixe = FixtureEtatVentes.prefixe("RDR");
        fixture.tables();
        UUID rapport12 = fixture.rapportVente(SUIVI_GLOBAL, "SUIVI GLOBAL DE VENTE*", "", 810);
        rapport13 = fixture.rapportVente(REPARTITION_CLIENTS, "REPARTITION DES CLIENTS", "", 1100);

        // RD6 : rôle libre de nom quelconque, aucun cas particulier « Administrateur ».
        roleLibre = fixture.role("Profil quelconque " + prefixe, false);
        roleChef = fixture.role("Chef " + prefixe, true);
        roleSansLien = fixture.role("Sans lien " + prefixe, true);
        roleNull = fixture.role("Null " + prefixe, null);
        fixture.lier(rapport12, roleChef);
        fixture.lier(rapport12, roleChef);
        fixture.lier(rapport12, roleNull);
        fixture.relire();
    }

    private RequestPostProcessor qui(String code, AdmRole... roles) {
        return connecte(fixture.compte(code + "." + prefixe, roles));
    }

    // ------------------------------------------------------------------ RD3 : liste

    @Test
    void rd3_roleLibre_voitTousLesRapportsImplementes() throws Exception {
        assertThat(appels.codesVisibles(qui("libre", roleLibre)))
                .containsExactlyInAnyOrder(SUIVI_GLOBAL, REPARTITION_CLIENTS);
    }

    @Test
    void rd3_restreintLie_voitSeulementSesRapports() throws Exception {
        assertThat(appels.codesVisibles(qui("chef", roleChef))).containsExactly(SUIVI_GLOBAL);
    }

    @Test
    void rd3_restreintSansLien_listeVide() throws Exception {
        mockMvc.perform(get(liste(VENTE)).with(qui("sanslien", roleSansLien)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rd3_sansRole_listeVide() throws Exception {
        mockMvc.perform(get(liste(VENTE)).with(qui("sansrole")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rd3_unRoleLibreParmiPlusieurs_rendLibre() throws Exception {
        assertThat(appels.codesVisibles(qui("mixte", roleSansLien, roleLibre)))
                .containsExactlyInAnyOrder(SUIVI_GLOBAL, REPARTITION_CLIENTS);
    }

    @Test
    void rd3_rapportSansLigneDeRole_invisibleRestreint_visibleLibre() throws Exception {
        assertThat(appels.codesVisibles(qui("chef2", roleChef))).doesNotContain(REPARTITION_CLIENTS);
        assertThat(appels.codesVisibles(qui("libre2", roleLibre))).contains(REPARTITION_CLIENTS);
    }

    @Test
    void rd3_withReportRestrictionNull_estRestreint() throws Exception {
        assertThat(appels.codesVisibles(qui("null", roleNull))).containsExactly(SUIVI_GLOBAL);
    }

    @Test
    void rd3_doublonDeLigneDeRole_pasDeDoublonDansLaListe() throws Exception {
        List<String> codes = appels.codesVisibles(qui("chef3", roleChef));

        assertThat(codes).filteredOn(SUIVI_GLOBAL::equals).hasSize(1);
    }

    // ------------------------------------------------------------------ RD2 : exécution

    @Test
    void rd2_restreintSansLien_403SurDescripteurResultatExport() throws Exception {
        RequestPostProcessor sansLien = qui("sanslien2", roleSansLien);

        assertTroisRoutesInterdites(sansLien, SUIVI_GLOBAL);
    }

    @Test
    void rd2_sansRole_403SurDescripteurResultatExport() throws Exception {
        RequestPostProcessor sansRole = qui("sansrole2");

        assertTroisRoutesInterdites(sansRole, SUIVI_GLOBAL);
    }

    @Test
    void rd2_restreintLie_403SurLeRapportNonLie_200SurLeSien() throws Exception {
        RequestPostProcessor chef = qui("chef4", roleChef);

        assertTroisRoutesInterdites(chef, REPARTITION_CLIENTS);
        assertThat(appels.statut(get(descripteur(VENTE, SUIVI_GLOBAL)).with(chef))).isEqualTo(OK);
        assertThat(appels.statut(get(resultat(VENTE, SUIVI_GLOBAL)).with(chef))).isEqualTo(OK);
    }

    private void assertTroisRoutesInterdites(RequestPostProcessor qui, String code) throws Exception {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(appels.statut(get(descripteur(VENTE, code)).with(qui))).as("descripteur").isEqualTo(INTERDIT);
        soft.assertThat(appels.statut(get(resultat(VENTE, code)).with(qui))).as("resultat").isEqualTo(INTERDIT);
        soft.assertThat(appels.statut(get(export(VENTE, code)).param(FORMAT, XLSX).with(qui))).as("export")
                .isEqualTo(INTERDIT);
        soft.assertAll();
    }

    /** RD2 : « absent de la liste ⇔ 403 à l'exécution », pour chaque profil et chaque rapport. */
    @Test
    void rd2_invariant_absentDeLaListe_ssi_403() throws Exception {
        List<RequestPostProcessor> profils = List.of(qui("i.libre", roleLibre), qui("i.chef", roleChef),
                qui("i.sanslien", roleSansLien), qui("i.sansrole"), qui("i.null", roleNull),
                qui("i.mixte", roleChef, roleSansLien));
        SoftAssertions soft = new SoftAssertions();
        for (int i = 0; i < profils.size(); i++) {
            RequestPostProcessor profil = profils.get(i);
            List<String> visibles = appels.codesVisibles(profil);
            for (String code : List.of(SUIVI_GLOBAL, REPARTITION_CLIENTS)) {
                int attendu = visibles.contains(code) ? OK : INTERDIT;
                soft.assertThat(appels.statut(get(descripteur(VENTE, code)).with(profil)))
                        .as("profil %d, %s", i, code).isEqualTo(attendu);
            }
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------ RD4 : rôles relus à chaque appel

    @Test
    void rd4_roleRetireEnBase_effetALAppelSuivant_sansReconnexion() throws Exception {
        AdmUser compte = fixture.compte("retrait." + prefixe);
        AdmUserRole attribution = fixture.attribuer(compte, roleLibre);
        RequestPostProcessor session = connecte(compte);
        assertThat(appels.codesVisibles(session)).contains(SUIVI_GLOBAL);

        fixture.retirer(attribution);

        assertThat(appels.codesVisibles(session)).isEmpty();
        assertThat(appels.statut(get(descripteur(VENTE, SUIVI_GLOBAL)).with(session))).isEqualTo(INTERDIT);
    }

    @Test
    void rd4_rolesDuJetonIgnores_seulAdmUserRoleCompte() throws Exception {
        AdmUser compte = fixture.compte("jeton." + prefixe, roleSansLien);
        RequestPostProcessor jetonAdministrateur = connecte(compte, "Administrateur", "Direction Générale");

        assertThat(appels.codesVisibles(jetonAdministrateur)).isEmpty();
        assertThat(appels.statut(get(resultat(VENTE, SUIVI_GLOBAL)).with(jetonAdministrateur)))
                .isEqualTo(INTERDIT);
    }

    // ------------------------------------------------------------------ RD1 / RD2 : 404

    @ParameterizedTest
    @ValueSource(strings = {"", "/" + SUIVI_GLOBAL, "/" + SUIVI_GLOBAL + "/resultat",
            "/" + SUIVI_GLOBAL + "/export?format=xlsx"})
    void rd2_moduleInconnu_404(String suite) throws Exception {
        assertThat(appels.statut(get("/api/rapports/inconnu" + suite).with(qui("m404", roleLibre))))
                .isEqualTo(INTROUVABLE);
    }

    @Test
    void rd2_codeInconnu_404() throws Exception {
        RequestPostProcessor libre = qui("c404", roleLibre);

        assertThat(appels.statut(get(descripteur(VENTE, "CodeInexistant" + prefixe)).with(libre)))
                .isEqualTo(INTROUVABLE);
        assertThat(appels.statut(get(resultat(VENTE, "CodeInexistant" + prefixe)).with(libre)))
                .isEqualTo(INTROUVABLE);
    }

    @Test
    void rd1_codeEnBaseSansClasse_absentDeLaListe_et404() throws Exception {
        String sansClasse = "ComSansClasse" + prefixe;
        fixture.rapportVente(sansClasse, "SANS CLASSE", "", 1);
        RequestPostProcessor libre = qui("sc", roleLibre);

        assertThat(appels.codesVisibles(libre)).doesNotContain(sansClasse);
        assertThat(appels.statut(get(descripteur(VENTE, sansClasse)).with(libre))).isEqualTo(INTROUVABLE);
    }

    @Test
    void rd1_classeSansLigneEnBase_absenteDeLaListe_et404() throws Exception {
        fixture.supprimerRapport(rapport13);
        RequestPostProcessor libre = qui("sl", roleLibre);

        assertThat(appels.codesVisibles(libre)).containsExactly(SUIVI_GLOBAL);
        assertThat(appels.statut(get(descripteur(VENTE, REPARTITION_CLIENTS)).with(libre))).isEqualTo(INTROUVABLE);
        assertThat(appels.statut(get(resultat(VENTE, REPARTITION_CLIENTS)).with(libre))).isEqualTo(INTROUVABLE);
    }

    @Test
    void rd1_cleModuleEtCode_ligneDUnAutreType_404() throws Exception {
        fixture.supprimerRapport(rapport13);
        fixture.rapport("Yes.Autre.Type.Reports", REPARTITION_CLIENTS, "AUTRE TYPE", "", 1);
        RequestPostProcessor libre = qui("type", roleLibre);

        assertThat(appels.codesVisibles(libre)).doesNotContain(REPARTITION_CLIENTS);
        assertThat(appels.statut(get(descripteur(VENTE, REPARTITION_CLIENTS)).with(libre))).isEqualTo(INTROUVABLE);
    }

    @Test
    void rd1_rapportVenteDemandeSousLeModulePatrimoine_404() throws Exception {
        RequestPostProcessor libre = qui("patri", roleLibre);

        assertThat(appels.statut(get(descripteur("patrimoine", SUIVI_GLOBAL)).with(libre))).isEqualTo(INTROUVABLE);
        List<String> codesPatrimoine = JsonPath.read(appels.corps(get(liste("patrimoine")).with(libre)),
                "$[*].rapports[*].code");
        assertThat(codesPatrimoine).doesNotContain(SUIVI_GLOBAL, REPARTITION_CLIENTS);
    }

    // ------------------------------------------------------------------ RD2 : ordre des contrôles

    @Test
    void rd2_ordre_moduleInconnuAvantDroit_404() throws Exception {
        assertThat(appels.statut(get("/api/rapports/inconnu/" + SUIVI_GLOBAL).with(qui("o1"))))
                .isEqualTo(INTROUVABLE);
    }

    @Test
    void rd2_ordre_codeInconnuAvantDroit_404() throws Exception {
        assertThat(appels.statut(get(resultat(VENTE, "Inexistant" + prefixe)).with(qui("o2"))))
                .isEqualTo(INTROUVABLE);
    }

    @Test
    void rd2_ordre_droitAvantParametres_403() throws Exception {
        RequestPostProcessor chef = qui("o3", roleChef);

        assertThat(appels.statut(get(resultat(VENTE, REPARTITION_CLIENTS)).param("du", "pas-une-date")
                .param("inconnu", "1").with(chef))).isEqualTo(INTERDIT);
        assertThat(appels.statut(get(export(VENTE, REPARTITION_CLIENTS)).param(FORMAT, "csv").with(chef)))
                .isEqualTo(INTERDIT);
    }

    @Test
    void sansSession_401() throws Exception {
        assertThat(appels.statut(get(liste(VENTE)))).isEqualTo(401);
    }

    // ------------------------------------------------------------------ §4.1 : groupement et tri

    @Test
    void liste_groupeVideOuNull_autresRapports_trimEtOrdreParSorting() throws Exception {
        fixture.supprimerRapport(rapport13);
        fixture.rapportVente(REPARTITION_CLIENTS, "  REPARTITION DES CLIENTS  ", "Suivi HEBDOMADAIRE ", 5);

        String corps = appels.corps(get(liste(VENTE)).with(qui("grp", roleLibre)));

        List<String> groupes = JsonPath.read(corps, "$[*].nom");
        List<Integer> ordres = JsonPath.read(corps, "$[*].ordre");
        assertThat(groupes).containsExactly("Suivi HEBDOMADAIRE", AUTRES_RAPPORTS);
        assertThat(ordres).containsExactly(5, 810);
        List<String> libelles = JsonPath.read(corps, "$[0].rapports[*].libelle");
        assertThat(libelles).containsExactly("REPARTITION DES CLIENTS");
    }

    @Test
    void liste_groupeNull_autresRapports_memeSortingDepartageParLibelle() throws Exception {
        fixture.supprimerRapport(rapport13);
        fixture.rapportVente(REPARTITION_CLIENTS, "A REPARTITION", null, 810);

        String corps = appels.corps(get(liste(VENTE)).with(qui("tri", roleLibre)));

        List<String> groupes = JsonPath.read(corps, "$[*].nom");
        List<String> codes = JsonPath.read(corps, "$[0].rapports[*].code");
        assertThat(groupes).containsExactly(AUTRES_RAPPORTS);
        assertThat(codes).containsExactly(REPARTITION_CLIENTS, SUIVI_GLOBAL);
    }

    @Test
    void liste_resume_libelleTrimeAsteriqueConserve_sortingNullVautZero_avecParametres() throws Exception {
        fixture.supprimerRapport(rapport13);
        fixture.rapportVente(REPARTITION_CLIENTS, " REPARTITION ", "", null);

        String corps = appels.corps(get(liste(VENTE)).with(qui("res", roleLibre)));

        List<Map<String, Object>> rapports = JsonPath.read(corps, "$[*].rapports[*]");
        Map<String, Object> suivi = rapports.stream().filter(r -> SUIVI_GLOBAL.equals(r.get("code"))).findFirst()
                .orElseThrow();
        Map<String, Object> repartition = rapports.stream()
                .filter(r -> REPARTITION_CLIENTS.equals(r.get("code"))).findFirst().orElseThrow();
        assertThat(suivi).containsEntry("libelle", "SUIVI GLOBAL DE VENTE*").containsEntry("ordre", 810)
                .containsEntry("avecParametres", true);
        assertThat(repartition).containsEntry("libelle", "REPARTITION").containsEntry("ordre", 0);
    }

    @Test
    void descripteur_n12_codeModuleLibelleFormats() throws Exception {
        mockMvc.perform(get(descripteur(VENTE, SUIVI_GLOBAL)).with(qui("desc", roleLibre)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(SUIVI_GLOBAL))
                .andExpect(jsonPath("$.module").value(VENTE))
                .andExpect(jsonPath("$.libelle").value("SUIVI GLOBAL DE VENTE*"))
                .andExpect(jsonPath("$.formatsExport[0]").value(XLSX))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'trancheId')].type").value("TRANCHES"))
                .andExpect(jsonPath("$.parametres[?(@.nom == 'trancheId')].multiple").value(true));
    }
}
