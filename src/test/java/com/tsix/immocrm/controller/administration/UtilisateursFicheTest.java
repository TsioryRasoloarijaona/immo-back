package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureRoles.Restriction;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs;
import com.tsix.immocrm.fixtures.FixtureUtilisateurs.Etat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.administration.AppelsUtilisateurs.acceptee;
import static com.tsix.immocrm.controller.administration.AppelsUtilisateurs.statut;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.CODE;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.INACTIF;
import static com.tsix.immocrm.fixtures.FixtureUtilisateurs.REFERENCES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fiche (noms du fil, restrictions lues), PATCH (R4, R5, R11), {@code /references}, 404, R18 pour les chaînes vides. */
@SpringBootTest
@AutoConfigureMockMvc
class UtilisateursFicheTest {

    private static final UUID INCONNU = UUID.fromString("5B6C7D8E-9FA0-4B1C-82D3-E4F506172839");
    private static final String EMAIL = "email";
    private static final String TELEPHONE = "telephone";
    private static final String MOBILE = "mobile";
    private static final String PERSONNEL = "personnelId";
    private static final String CHEMIN_EMAIL = "$.email";
    private static final String CHEMIN_RESTRICTIONS = "$.restrictions.";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureUtilisateurs fixture;

    private String jeton;
    private AppelsUtilisateurs appels;
    private AdmUser cible;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        appels = new AppelsUtilisateurs(mockMvc, connecte(fixture.compte()));
        cible = fixture.compte();
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    private void aucunChangement(Etat avant) {
        assertThat(fixture.etat(cible.getId())).isEqualTo(avant);
    }

    // ------------------------------------------------------------------ lecture

    @Test
    void fiche_nomsDuFil_colonnesCompte() throws Exception {
        UUID id = cible.getId();
        fixture.poser(id, "Email", "a@example.com");
        fixture.poser(id, "Phone", "0522");
        fixture.poser(id, "Mobile", "0600");
        fixture.poser(id, "IsVisibleForChat", true);
        fixture.poser(id, "CanDisapearNotification", false);
        fixture.poser(id, "IsMobileUser", true);
        fixture.poser(id, "EmailSMTPServer", "smtp.example.com");
        fixture.poser(id, "EmailPortNumber", 25);
        fixture.poser(id, "EmailSSLActive", true);
        fixture.poser(id, "UserCreate", "Createur");
        fixture.poser(id, "UserUpdate", "Modificateur");

        appels.fiche(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.code").value(cible.getCode()))
                .andExpect(jsonPath("$.description").value(cible.getDescription()))
                .andExpect(jsonPath(CHEMIN_EMAIL).value("a@example.com"))
                .andExpect(jsonPath("$.telephone").value("0522"))
                .andExpect(jsonPath("$.mobile").value("0600"))
                .andExpect(jsonPath("$.inactif").value(false))
                .andExpect(jsonPath("$.avecNotification").value(true))
                .andExpect(jsonPath("$.peutMasquerNotification").value(false))
                .andExpect(jsonPath("$.accesMobile").value(true))
                .andExpect(jsonPath("$.smtpServeur").value("smtp.example.com"))
                .andExpect(jsonPath("$.smtpPort").value(25))
                .andExpect(jsonPath("$.smtpSsl").value(true))
                .andExpect(jsonPath("$.creePar").value("Createur"))
                .andExpect(jsonPath("$.modifiePar").value("Modificateur"))
                .andExpect(jsonPath("$.compteWeb").value(false));
    }

    @Test
    void fiche_identifiantsLies() throws Exception {
        UUID id = cible.getId();
        UUID role = fixture.role("ROL-" + jeton);
        UUID roleWeb = fixture.role("WEB-" + jeton);
        UUID depot = fixture.depot("DEP-" + jeton);
        UUID culture = fixture.culture();
        UUID personnel = fixture.personnel("Nom-" + jeton);
        fixture.lierRole(id, role);
        fixture.lierDepot(id, depot);
        fixture.poser(id, "WebRoleId", roleWeb.toString());
        fixture.poser(id, "WebCultureId", culture.toString());
        fixture.poser(id, "CfgEmployeeId", personnel.toString());

        appels.fiche(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.roleIds[0]", equalToIgnoringCase(role.toString())))
                .andExpect(jsonPath("$.compteDepotIds[0]", equalToIgnoringCase(depot.toString())))
                .andExpect(jsonPath("$.roleWebId", equalToIgnoringCase(roleWeb.toString())))
                .andExpect(jsonPath("$.cultureWebId", equalToIgnoringCase(culture.toString())))
                .andExpect(jsonPath("$.personnelId", equalToIgnoringCase(personnel.toString())))
                .andExpect(jsonPath("$.personnelNom", containsString("Nom-" + jeton)));
    }

    @Test
    void fiche_compteWeb_vrai_avecIdentifiants() throws Exception {
        fixture.credential(cible.getId(), 0);

        appels.fiche(cible.getId()).andExpect(status().isOk()).andExpect(jsonPath("$.compteWeb").value(true));
    }

    @Test
    void fiche_inconnue_404() throws Exception {
        appels.fiche(INCONNU).andExpect(status().isNotFound());
    }

    @Test
    void ecritures_compteInconnu_404() throws Exception {
        appels.modifier(INCONNU, corps().avec(DESCRIPTION, "X")).andExpect(status().isNotFound());
        appels.roles(INCONNU).andExpect(status().isNotFound());
        appels.comptesDepot(INCONNU).andExpect(status().isNotFound());
        appels.habilitations(INCONNU).andExpect(status().isNotFound());
        appels.lire(FixtureUtilisateurs.HABILITATIONS.replace("{id}", INCONNU.toString())).andExpect(status().isNotFound());
    }

    @Test
    void r18_chainesVides_luesNull() throws Exception {
        UUID id = cible.getId();
        fixture.poser(id, "Email", "");
        fixture.poser(id, "Phone", "");
        fixture.poser(id, "Mobile", "");

        appels.fiche(id).andExpect(status().isOk())
                .andExpect(jsonPath(CHEMIN_EMAIL).value((Object) null))
                .andExpect(jsonPath("$.telephone").value((Object) null))
                .andExpect(jsonPath("$.mobile").value((Object) null));
    }

    // ------------------------------------------------------------------ décision 5 : restrictions lues

    /** Un rôle à {@code false} sur cette seule colonne, un autre à {@code true} partout : seule celle-ci est levée. */
    @ParameterizedTest(name = "{0}")
    @EnumSource(Restriction.class)
    void restrictions_leveeSiUnRoleAFalse_lesAutresRestent(Restriction levee) throws Exception {
        UUID leve = fixture.role("LEV-" + jeton);
        UUID strict = fixture.role("STR-" + jeton);
        for (Restriction r : Restriction.values()) {
            fixture.restriction(leve, r, r != levee);
            fixture.restriction(strict, r, true);
        }
        fixture.lierRole(cible.getId(), leve);
        fixture.lierRole(cible.getId(), strict);

        ResultActions fiche = appels.fiche(cible.getId()).andExpect(status().isOk());

        for (Restriction r : Restriction.values()) {
            fiche.andExpect(jsonPath(CHEMIN_RESTRICTIONS + r.champ()).value(r != levee));
        }
    }

    /** {@code NULL} n'est pas {@code false} : la restriction reste. */
    @Test
    void restrictions_null_neLevePas() throws Exception {
        fixture.lierRole(cible.getId(), fixture.role("NUL-" + jeton));

        appels.fiche(cible.getId()).andExpect(status().isOk())
                .andExpect(jsonPath(CHEMIN_RESTRICTIONS + Restriction.RAPPORTS.champ()).value(true));
    }

    @Test
    void restrictions_sansRole_toutesPresentes() throws Exception {
        ResultActions fiche = appels.fiche(cible.getId()).andExpect(status().isOk());

        for (Restriction r : Restriction.values()) {
            fiche.andExpect(jsonPath(CHEMIN_RESTRICTIONS + r.champ()).value(true));
        }
    }

    // ------------------------------------------------------------------ références

    @Test
    void references_rolesCulturesPersonnelsDepots() throws Exception {
        UUID role = fixture.role("ROL-" + jeton);
        UUID culture = fixture.culture();
        UUID personnel = fixture.personnel("Nom-" + jeton);
        UUID depot = fixture.depot("DEP-" + jeton);

        String corps = appels.corpsLu(REFERENCES);

        assertThat(corps.toUpperCase(java.util.Locale.ROOT)).contains(
                Arrays.stream(new UUID[]{role, culture, personnel, depot}).map(u -> u.toString().toUpperCase(java.util.Locale.ROOT)).toList());
    }

    @Test
    void references_clientNonPropose() throws Exception {
        UUID client = fixture.client("Client-" + jeton);

        assertThat(appels.corpsLu(REFERENCES)).doesNotContainIgnoringCase(client.toString());
    }

    // ------------------------------------------------------------------ PATCH

    @Test
    void patch_null_inchange_vide_vide_absent_inchange() throws Exception {
        UUID id = cible.getId();
        fixture.poser(id, "Email", "avant@example.com");
        fixture.poser(id, "Phone", "0522");
        fixture.poser(id, "Mobile", "0600");

        acceptee(appels.modifier(id, corps().avec(EMAIL, null).avec(TELEPHONE, "")));

        appels.fiche(id).andExpect(status().isOk())
                .andExpect(jsonPath(CHEMIN_EMAIL).value("avant@example.com"))
                .andExpect(jsonPath("$.telephone").value((Object) null))
                .andExpect(jsonPath("$.mobile").value("0600"));
    }

    @Test
    void patch_champsModifiables_ecrits() throws Exception {
        UUID id = cible.getId();
        UUID culture = fixture.culture();
        UUID roleWeb = fixture.role("WEB-" + jeton);
        CorpsJson corps = corps().avec(DESCRIPTION, "Nouveau").avec(EMAIL, "n@example.com").avec(TELEPHONE, "0523").avec(MOBILE, "0601")
                .avec("avecNotification", true).avec("peutMasquerNotification", true).avec("accesMobile", true)
                .avec("smtpServeur", "mail.example.com").avec("smtpPort", 465).avec("smtpSsl", true)
                .avec("cultureWebId", culture).avec("roleWebId", roleWeb);

        acceptee(appels.modifier(id, corps));

        assertThat(fixture.ligne(id))
                .containsEntry("Description", "Nouveau")
                .containsEntry("Email", "n@example.com")
                .containsEntry("Phone", "0523")
                .containsEntry("Mobile", "0601")
                .containsEntry("IsVisibleForChat", true)
                .containsEntry("CanDisapearNotification", true)
                .containsEntry("IsMobileUser", true)
                .containsEntry("EmailSMTPServer", "mail.example.com")
                .containsEntry("EmailPortNumber", 465)
                .containsEntry("EmailSSLActive", true);
    }

    @Test
    void patch_auteurDeModification_renseigne() throws Exception {
        UUID id = cible.getId();

        acceptee(appels.modifier(id, corps().avec(DESCRIPTION, "Nouveau")));

        assertThat(fixture.ligne(id).get("UserUpdate")).isNotNull();
        assertThat(fixture.ligne(id).get("UserUpdateDateTime")).isNotNull();
    }

    /** R4 : ignoré ou refusé, au choix du spec ; 405 = route absente, donc pas un refus. */
    @Test
    void r4_codeEnvoye_jamaisEcrit() throws Exception {
        int statut = statut(appels.modifier(cible.getId(), corps().avec(CODE, "Pirate." + jeton)));

        assertThat(statut).isIn(200, 204, 400, 409);
        assertThat(fixture.ligne(cible.getId())).containsEntry("Code", cible.getCode());
    }

    @Test
    void r5_patchDescription256_400() throws Exception {
        Etat avant = fixture.etat(cible.getId());

        appels.modifier(cible.getId(), corps().avec(DESCRIPTION, "d".repeat(256))).andExpect(status().isBadRequest());

        aucunChangement(avant);
    }

    @Test
    void r11_patchPersonnelEmploye_lie() throws Exception {
        UUID personnel = fixture.personnel("Nom-" + jeton);

        acceptee(appels.modifier(cible.getId(), corps().avec(PERSONNEL, personnel)));

        appels.fiche(cible.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.personnelId", equalToIgnoringCase(personnel.toString())));
    }

    /** « Rien n'est écrit avant Enregistrer » : lier un personnel ne recopie rien côté serveur, le front envoie les champs. */
    @Test
    void r11_patchPersonnel_neCopieRien() throws Exception {
        UUID personnel = fixture.personnel("Nom-" + jeton);
        fixture.poser(cible.getId(), "Email", "garde@example.com");

        acceptee(appels.modifier(cible.getId(), corps().avec(PERSONNEL, personnel)));

        assertThat(fixture.ligne(cible.getId()))
                .containsEntry("Email", "garde@example.com").containsEntry("Description", cible.getDescription());
    }

    @Test
    void r11_patchPersonnelInconnu_400() throws Exception {
        Etat avant = fixture.etat(cible.getId());

        appels.modifier(cible.getId(), corps().avec(PERSONNEL, INCONNU)).andExpect(status().isBadRequest());

        aucunChangement(avant);
    }

    @Test
    void r11_patchPersonnelHorsTypeEmploye_400() throws Exception {
        UUID client = fixture.client("Client-" + jeton);
        Etat avant = fixture.etat(cible.getId());

        appels.modifier(cible.getId(), corps().avec(PERSONNEL, client)).andExpect(status().isBadRequest());

        aucunChangement(avant);
    }

    /** Réponse 12 : R5 prime sur « {@code ""} = vidé ». */
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void reponse12_patchDescriptionVideOuBlanche_400(String description) throws Exception {
        Etat avant = fixture.etat(cible.getId());

        appels.modifier(cible.getId(), corps().avec(DESCRIPTION, description)).andExpect(status().isBadRequest());

        aucunChangement(avant);
    }

    @ParameterizedTest
    @ValueSource(strings = {"roleWebId", "cultureWebId"})
    void reponse13_patchRoleWebOuCultureInconnu_400(String champ) throws Exception {
        Etat avant = fixture.etat(cible.getId());

        appels.modifier(cible.getId(), corps().avec(champ, INCONNU)).andExpect(status().isBadRequest());

        aucunChangement(avant);
    }

    @Test
    void reponse13_patch_200_avecLaFiche() throws Exception {
        appels.modifier(cible.getId(), corps().avec(DESCRIPTION, "Nouveau")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cible.getId().toString()))
                .andExpect(jsonPath("$.code").value(cible.getCode()))
                .andExpect(jsonPath("$.description").value("Nouveau"));
    }

    /** Réponse 15 : {@code etat} couvre {@code UserUpdate}, {@code UserUpdateDateTime} et les traces. */
    @Test
    void reponse15_patchSansEffet_rienEcrit_aucuneTrace() throws Exception {
        UUID id = cible.getId();
        fixture.poser(id, "Email", "garde@example.com");
        fixture.poser(id, "UserUpdate", "Dernier auteur");
        Etat avant = fixture.etat(id);

        acceptee(appels.modifier(id, corps().avec(DESCRIPTION, cible.getDescription()).avec(EMAIL, "garde@example.com")
                .avec(INACTIF, false)));

        aucunChangement(avant);
    }

    /** Point 6 des décisions : la validation ne joue que si {@code personnelId} change ; un lien hérité invalide reste enregistrable. */
    @Test
    void r11_patchMemePersonnelHorsType_accepte() throws Exception {
        UUID client = fixture.client("Client-" + jeton);
        fixture.poser(cible.getId(), "CfgEmployeeId", client.toString());

        acceptee(appels.modifier(cible.getId(), corps().avec(PERSONNEL, client).avec(DESCRIPTION, "Autre " + jeton)));

        assertThat(fixture.ligne(cible.getId())).containsEntry("Description", "Autre " + jeton);
    }

    @ParameterizedTest
    @ValueSource(strings = {"roleIds", "compteDepotIds"})
    void fiche_listesVides_sansLien(String champ) throws Exception {
        appels.fiche(cible.getId()).andExpect(status().isOk()).andExpect(jsonPath("$." + champ).isEmpty());
    }

    @Test
    void fiche_plusieursRoles_tousListes() throws Exception {
        UUID a = fixture.role("A-" + jeton);
        UUID b = fixture.role("B-" + jeton);
        fixture.lierRole(cible.getId(), a);
        fixture.lierRole(cible.getId(), b);

        appels.fiche(cible.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.roleIds", containsInAnyOrder(List.of(a, b).stream().map(UUID::toString).toArray())));
    }
}
