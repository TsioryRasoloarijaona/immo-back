package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Programme en écriture — FEATURE-patrimoine-ecriture.md §2, §3, §11 (ProjetEcritureTest). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjetEcritureTest {

    private static final String PROJETS = "/api/projets";
    private static final String PROJET = "/api/projets/{id}";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String ENTITE_ID = "entiteId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private CfgProjectRepository projectRepository;

    private AdmUser admin;
    private CfgCompany entiteActive;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        entiteActive = fixture.entite("ENT-PRJ-ECR", true);
    }

    private CorpsJson corpsMinimal(String code) {
        return corps().avec(ENTITE_ID, entiteActive.getId()).avec(CODE, code).avec(DESCRIPTION, "Programme " + code);
    }

    private ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(PROJETS).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions modifier(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(PROJET, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private UUID creerOk(CorpsJson corps) throws Exception {
        String json = creer(corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    // ------------------------------------------------------------------ §3.1 / §2.3 création

    @Test
    void r3_1_creation_201_avecTraceCreateur() throws Exception {
        UUID id = creerOk(corpsMinimal("PRJ-ECR-TRACE"));

        Tuple ligne = fixture.ligneProjet(id);
        assertAll(
                () -> assertThat(ligne.get("Code")).isEqualTo("PRJ-ECR-TRACE"),
                () -> assertThat(guid(ligne.get("CfgCompanyId"))).isEqualTo(guid(entiteActive.getId())),
                () -> assertThat(ligne.get("UserCreate")).isEqualTo(AUTEUR),
                () -> assertThat(ligne.get("UserUpdate")).isEqualTo(AUTEUR),
                () -> assertThat(ligne.get("UserCreateDateTime")).isNotNull(),
                () -> assertThat(ligne.get("UserUpdateDateTime")).isEqualTo(ligne.get("UserCreateDateTime")));
    }

    @Test
    void r3_3_creation_reponseExposeEntiteEtTrace() throws Exception {
        AdmCountry pays = fixture.pays("PAYS-PRJ-ECR");

        creer(corpsMinimal("PRJ-ECR-REPONSE").avec("paysId", pays.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("PRJ-ECR-REPONSE"))
                .andExpect(jsonPath("$.entiteId").value(entiteActive.getId().toString()))
                .andExpect(jsonPath("$.entiteCode").value("ENT-PRJ-ECR"))
                .andExpect(jsonPath("$.entiteLibelle").value("Entite ENT-PRJ-ECR"))
                .andExpect(jsonPath("$.paysId").value(pays.getId().toString()))
                .andExpect(jsonPath("$.paysLibelle").value("Pays PAYS-PRJ-ECR"))
                .andExpect(jsonPath("$.creePar").value(AUTEUR))
                .andExpect(jsonPath("$.modifiePar").value(AUTEUR))
                .andExpect(jsonPath("$.creeLe").isNotEmpty())
                .andExpect(jsonPath("$.modifieLe").isNotEmpty());
    }

    @Test
    void r3_2_creation_paysEcritDansAdmCountryId() throws Exception {
        AdmCountry pays = fixture.pays("PAYS-PRJ-COL");

        UUID id = creerOk(corpsMinimal("PRJ-ECR-PAYS").avec("paysId", pays.getId()));

        assertThat(guid(fixture.ligneProjet(id).get("AdmCountryId"))).isEqualTo(guid(pays.getId()));
    }

    /** §3.4 + §2.4 : textes non saisis à "" (ligne modèle LAZ), jamais NULL. */
    @Test
    void r3_4_creation_textesNonSaisisEcritsVides() throws Exception {
        UUID id = creerOk(corpsMinimal("PRJ-ECR-VIDES"));

        Tuple ligne = fixture.ligneProjet(id);
        assertThat(List.of("Address", "Address2", "Address3", "ZipCode", "City", "SignatureCity", "Phone",
                "Mobile", "Fax", "Email", "WebSite", "DescriptionLong"))
                .isNotEmpty()
                .allSatisfy(colonne -> assertThat(ligne.get(colonne)).as(colonne).isEqualTo(""));
    }

    @Test
    void r3_2_creation_champsSaisisEcritsDansLesColonnes() throws Exception {
        UUID id = creerOk(corpsMinimal("PRJ-ECR-CHAMPS")
                .avec("descriptionLongue", "Longue").avec("adresse", "1 rue A").avec("ville", "Casablanca")
                .avec("villeSignature", "Rabat").avec("telephone", "0522000000").avec("email", "a@b.ma")
                .avec("dateDebutTravaux", "2026-01-01T00:00:00").avec("dateFinTravaux", "2027-01-01T00:00:00"));

        Tuple ligne = fixture.ligneProjet(id);
        assertAll(
                () -> assertThat(ligne.get("DescriptionLong")).isEqualTo("Longue"),
                () -> assertThat(ligne.get("Address")).isEqualTo("1 rue A"),
                () -> assertThat(ligne.get("City")).isEqualTo("Casablanca"),
                () -> assertThat(ligne.get("SignatureCity")).isEqualTo("Rabat"),
                () -> assertThat(ligne.get("Phone")).isEqualTo("0522000000"),
                () -> assertThat(ligne.get("Email")).isEqualTo("a@b.ma"),
                () -> assertThat(ligne.get("WorkStartDate")).isNotNull(),
                () -> assertThat(ligne.get("WorkEndDate")).isNotNull());
    }

    /** §2.6 : le code est comparé après trim et écrit sans les espaces. */
    @Test
    void r2_6_creation_codeEcritApresTrim() throws Exception {
        UUID id = creerOk(corpsMinimal("  PRJ-ECR-TRIM  "));

        assertThat(fixture.ligneProjet(id).get("Code")).isEqualTo("PRJ-ECR-TRIM");
    }

    /** §2.7 : SetDefalutHabilitation(null, PKey, null, null) — une ligne projet seule pour le créateur. */
    @Test
    void r2_7_creation_habilitationDuCreateurSurLeProgramme() throws Exception {
        UUID id = creerOk(corpsMinimal("PRJ-ECR-HABIL"));

        assertThat(fixture.habilitations(admin.getId())).singleElement().satisfies(h -> assertAll(
                () -> assertThat(guid(h.get("CfgProjectId"))).isEqualTo(guid(id)),
                () -> assertThat(h.get("CfgCompanyId")).isNull(),
                () -> assertThat(h.get("CfgTrancheId")).isNull(),
                () -> assertThat(h.get("PrjProjectId")).isNull()));
    }

    // ------------------------------------------------------------------ §3.2 / §2.8 refus 400

    @Test
    void r3_2_codeManquant_400() throws Exception {
        creer(corpsMinimal("X").sans(CODE)).andExpect(status().isBadRequest());
    }

    @Test
    void r3_2_codeBlanc_400() throws Exception {
        creer(corpsMinimal("   ")).andExpect(status().isBadRequest());
    }

    @Test
    void r3_2_descriptionManquante_400() throws Exception {
        creer(corpsMinimal("PRJ-ECR-SANS-DESC").sans(DESCRIPTION)).andExpect(status().isBadRequest());
    }

    @Test
    void r3_2_entiteManquante_400() throws Exception {
        creer(corpsMinimal("PRJ-ECR-SANS-ENT").sans(ENTITE_ID)).andExpect(status().isBadRequest());
    }

    @Test
    void r3_2_entiteInactive_400() throws Exception {
        CfgCompany inactive = fixture.entite("ENT-PRJ-INACTIVE", false);

        creer(corpsMinimal("PRJ-ECR-ENT-INACT").avec(ENTITE_ID, inactive.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r2_8_entiteInconnue_400() throws Exception {
        creer(corpsMinimal("PRJ-ECR-ENT-INC").avec(ENTITE_ID, UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r2_8_paysInconnu_400() throws Exception {
        creer(corpsMinimal("PRJ-ECR-PAYS-INC").avec("paysId", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r3_2_finTravauxAvantDebut_400() throws Exception {
        creer(corpsMinimal("PRJ-ECR-DATES-TRV")
                .avec("dateDebutTravaux", "2026-06-01T00:00:00").avec("dateFinTravaux", "2026-01-01T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r3_2_finCommercialisationAvantDebut_400() throws Exception {
        creer(corpsMinimal("PRJ-ECR-DATES-COM")
                .avec("dateDebutCommercialisation", "2026-06-01T00:00:00")
                .avec("dateFinCommercialisation", "2026-01-01T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r2_8_codeTropLong_400() throws Exception {
        creer(corpsMinimal("C".repeat(256))).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ §2.6 unicité 409

    @Test
    void r2_6_codeExistantAvecUneCasseDifferente_409_avecCodeEtNomExistants() throws Exception {
        CfgProject existant = fixture.projet("PRJ-ECR-DOUBLON", entiteActive);

        creer(corpsMinimal("prj-ecr-doublon"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(existant.getCode())))
                .andExpect(jsonPath("$.detail").value(containsString(existant.getDescription())));
    }

    @Test
    void r2_6_codeExistantAvecEspaces_409() throws Exception {
        fixture.projet("PRJ-ECR-DOUBLON-TRIM", entiteActive);

        creer(corpsMinimal(" PRJ-ECR-DOUBLON-TRIM ")).andExpect(status().isConflict());
    }

    @Test
    void r3_1_patch_codeDUnAutreProgramme_409() throws Exception {
        fixture.projet("PRJ-ECR-PRIS", entiteActive);
        CfgProject cible = fixture.projet("PRJ-ECR-CIBLE", entiteActive);

        modifier(cible.getId(), corps().avec(CODE, "PRJ-ECR-PRIS")).andExpect(status().isConflict());
    }

    @Test
    void r2_6_patch_garderSonPropreCode_200() throws Exception {
        CfgProject cible = fixture.projet("PRJ-ECR-MEME-CODE", entiteActive);

        modifier(cible.getId(), corps().avec(CODE, "PRJ-ECR-MEME-CODE")).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ §3.1 / §2.9 modification

    @Test
    void r3_1_patch_idInconnu_404() throws Exception {
        modifier(UUID.randomUUID(), corps().avec(DESCRIPTION, "x")).andExpect(status().isNotFound());
    }

    @Test
    void r2_9_patch_champAbsentInchange_chaineVideVidee() throws Exception {
        CfgProject cible = fixture.projet("PRJ-ECR-PATCH", entiteActive);
        cible.setAddress("12 rue B");
        cible.setCity("Rabat");
        projectRepository.save(cible);

        modifier(cible.getId(), corps().avec("ville", "")).andExpect(status().isOk())
                .andExpect(jsonPath("$.ville").value(""))
                .andExpect(jsonPath("$.adresse").value("12 rue B"));

        Tuple ligne = fixture.ligneProjet(cible.getId());
        assertAll(
                () -> assertThat(ligne.get("City")).isEqualTo(""),
                () -> assertThat(ligne.get("Address")).isEqualTo("12 rue B"),
                () -> assertThat(ligne.get("Description")).isEqualTo("Programme PRJ-ECR-PATCH"));
    }

    @Test
    void r2_9_patch_champNullInchange() throws Exception {
        CfgProject cible = fixture.projet("PRJ-ECR-PATCH-NULL", entiteActive);

        modifier(cible.getId(), corps().avec(DESCRIPTION, null)).andExpect(status().isOk());

        assertThat(fixture.ligneProjet(cible.getId()).get("Description")).isEqualTo("Programme PRJ-ECR-PATCH-NULL");
    }

    @Test
    void r2_3_patch_posteUserUpdateSansToucherUserCreate() throws Exception {
        CfgProject cible = fixture.projet("PRJ-ECR-PATCH-TRACE", entiteActive);
        cible.setUserCreate("Auteur OMRAN");
        projectRepository.save(cible);

        modifier(cible.getId(), corps().avec(DESCRIPTION, "Nouveau nom")).andExpect(status().isOk());

        Tuple ligne = fixture.ligneProjet(cible.getId());
        assertAll(
                () -> assertThat(ligne.get("Description")).isEqualTo("Nouveau nom"),
                () -> assertThat(ligne.get("UserUpdate")).isEqualTo(AUTEUR),
                () -> assertThat(ligne.get("UserUpdateDateTime")).isNotNull(),
                () -> assertThat(ligne.get("UserCreate")).isEqualTo("Auteur OMRAN"));
    }

    @Test
    void r3_2_patch_entiteModifiable() throws Exception {
        CfgProject cible = fixture.projet("PRJ-ECR-PATCH-ENT", entiteActive);
        CfgCompany autre = fixture.entite("ENT-PRJ-AUTRE", true);

        modifier(cible.getId(), corps().avec(ENTITE_ID, autre.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.entiteId").value(autre.getId().toString()));
    }

    @Test
    void r3_2_patch_entiteInactive_400() throws Exception {
        CfgProject cible = fixture.projet("PRJ-ECR-PATCH-INACT", entiteActive);
        CfgCompany inactive = fixture.entite("ENT-PRJ-PATCH-INACT", false);

        modifier(cible.getId(), corps().avec(ENTITE_ID, inactive.getId())).andExpect(status().isBadRequest());
    }

    /** §14.5 : la règle fin ≥ début porte sur la valeur résultante (corps, sinon base). */
    @Test
    void r14_5_patch_finTravauxAvantLeDebutEnBase_400() throws Exception {
        CfgProject cible = fixture.projet("PRJ-ECR-PATCH-DATES", entiteActive);
        cible.setWorkStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        projectRepository.save(cible);

        modifier(cible.getId(), corps().avec("dateFinTravaux", "2026-01-01T00:00:00")).andExpect(status().isBadRequest());
    }

    @Test
    void r14_5_patch_debutCommercialisationApresLaFinEnBase_400() throws Exception {
        CfgProject cible = fixture.projet("PRJ-ECR-PATCH-DATES-COM", entiteActive);
        cible.setMarketingEndDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        projectRepository.save(cible);

        modifier(cible.getId(), corps().avec("dateDebutCommercialisation", "2026-06-01T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r14_5_patch_finApresLeDebutEnBase_200() throws Exception {
        CfgProject cible = fixture.projet("PRJ-ECR-PATCH-DATES-OK", entiteActive);
        cible.setWorkStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        projectRepository.save(cible);

        modifier(cible.getId(), corps().avec("dateFinTravaux", "2027-01-01T00:00:00")).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ §3.3 liste

    @Test
    void r3_3_listeExposeEntiteCode() throws Exception {
        CfgProject projet = fixture.projet("PRJ-ECR-LISTE", entiteActive);

        mockMvc.perform(get(PROJETS).param("size", "1000").with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + projet.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + projet.getId() + "')].entiteCode").value("ENT-PRJ-ECR"));
    }
}
