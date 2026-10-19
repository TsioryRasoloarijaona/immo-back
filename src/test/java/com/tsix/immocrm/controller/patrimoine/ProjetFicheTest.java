package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheFiche;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ChampsAViderJson.avecChampsAVider;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Programme complet — FEATURE-patrimoine-tranche-fiche.md §1, §3, §6 (ProjetFicheTest), recalé par §8. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjetFicheTest {

    private static final String PROJETS = "/api/projets";
    private static final String PROJET = "/api/projets/{id}";
    private static final String ZONES = "/api/projets/{id}/zones";
    private static final String ZONE = "/api/projets/{id}/zones/{zoneId}";
    private static final String DEFAUTS = "/api/projets/defauts-entite/{entiteId}";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String DETAIL = "$.detail";
    private static final String CHAMPS_A_VIDER = "champsAVider";
    private static final String PARENT_ID = "parentId";
    private static final String SEARCH = "search";
    private static final String SIZE = "size";
    private static final String TOUS = "1000";
    private static final String CONTENT_ID = "$.content[?(@.id=='%s')]";
    private static final String COMPTE_ZONES = "SELECT COUNT(*) FROM CfgProjectZone WHERE PKey = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheFiche fiche;
    @Autowired private CfgProjectRepository projectRepository;

    private AdmUser admin;
    private CfgCompany entite;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1A" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        entite = fixture.entite("ENT-" + jeton, true);
    }

    private CfgProject programme(String suffixe) {
        return fixture.projet("PRJ-" + jeton + "-" + suffixe, entite);
    }

    private CorpsJson corpsMinimal(String suffixe) {
        return corps().avec("entiteId", entite.getId()).avec(CODE, "PRJ-" + jeton + "-" + suffixe)
                .avec(DESCRIPTION, "Programme " + suffixe);
    }

    private ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(PROJETS).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions modifier(UUID id, String json) throws Exception {
        return mockMvc.perform(patch(PROJET, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions creerZone(UUID projetId, CorpsJson corps) throws Exception {
        return mockMvc.perform(post(ZONES, projetId).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions lister(String recherche) throws Exception {
        return mockMvc.perform(get(PROJETS).param(SEARCH, recherche).param(SIZE, TOUS).with(connecte(admin)));
    }

    private static String ligneDe(UUID id) {
        return CONTENT_ID.formatted(id);
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    // ------------------------------------------------------------------ §3.1 signataire et responsables

    @Test
    void r3_1_signataireEtResponsables_ecritsEtRelus() throws Exception {
        CfgTier signataire = fiche.employe("EMP-SIG-" + jeton);
        CfgTier technique = fiche.employe("EMP-TEC-" + jeton);
        CfgTier commercial = fiche.employe("EMP-COM-" + jeton);

        String json = creer(corpsMinimal("RESP")
                .avec("signataireId", signataire.getId())
                .avec("responsableTechniqueId", technique.getId())
                .avec("responsableCommercialId", commercial.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.signataireId").value(signataire.getId().toString()))
                .andExpect(jsonPath("$.responsableTechniqueId").value(technique.getId().toString()))
                .andExpect(jsonPath("$.responsableCommercialId").value(commercial.getId().toString()))
                .andReturn().getResponse().getContentAsString();

        Tuple ligne = fixture.ligneProjet(UUID.fromString(JsonPath.read(json, "$.id")));
        assertAll(
                () -> assertThat(guid(ligne.get("AdmSignatoryId"))).isEqualTo(guid(signataire.getId())),
                () -> assertThat(guid(ligne.get("CfgTechnicalManagerId"))).isEqualTo(guid(technique.getId())),
                () -> assertThat(guid(ligne.get("CfgSalesManagerId"))).isEqualTo(guid(commercial.getId())));
    }

    @Test
    void r3_1_detail_exposeSignataireEtResponsables() throws Exception {
        CfgTier signataire = fiche.employe("EMP-SIG-D-" + jeton);
        CfgProject projet = programme("DET-RESP");
        projet.setAdmSignatory(signataire);
        projectRepository.save(projet);

        mockMvc.perform(get(PROJET, projet.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signataireId").value(signataire.getId().toString()))
                .andExpect(jsonPath("$.signataireLibelle").exists())
                .andExpect(jsonPath("$.responsableTechniqueId").hasJsonPath())
                .andExpect(jsonPath("$.responsableCommercialId").hasJsonPath());
    }

    /** §1.2 : référence inconnue → 400 nommant le champ (§8.4 : dans {@code detail}). */
    @ParameterizedTest
    @ValueSource(strings = {"signataireId", "responsableTechniqueId", "responsableCommercialId"})
    void r1_2_personnelInconnu_400_detailNommeLeChamp(String champ) throws Exception {
        creer(corpsMinimal("INC-" + champ.length()).avec(champ, UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(champ)));
    }

    // ------------------------------------------------------------------ §3.2 défauts de l'entité

    @Test
    void r3_2_defautsEntite_copieCodeDescriptionEtCoordonnees() throws Exception {
        CfgTier signataire = fiche.employe("EMP-DEF-" + jeton);
        fiche.coordonneesEntite(entite.getId(), signataire.getId());

        mockMvc.perform(get(DEFAUTS, entite.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entiteId").value(entite.getId().toString()))
                .andExpect(jsonPath("$.code").value("ENT-" + jeton))
                .andExpect(jsonPath("$.description").value("Entite ENT-" + jeton))
                .andExpect(jsonPath("$.adresse").value("7 bd P1a"))
                .andExpect(jsonPath("$.ville").value("Marrakech"))
                .andExpect(jsonPath("$.villeSignature").value("Agadir"))
                .andExpect(jsonPath("$.email").value("entite@p1a.ma"))
                .andExpect(jsonPath("$.telephone").value("0524000000"))
                .andExpect(jsonPath("$.descriptionLongue").value("Longue P1a"))
                .andExpect(jsonPath("$.signataireId").value(signataire.getId().toString()));
    }

    @Test
    void r3_2_defautsEntite_entiteInconnue_404() throws Exception {
        mockMvc.perform(get(DEFAUTS, UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ §1.1 champsAVider

    @Test
    void r1_1_champsAVider_paysRemisANull() throws Exception {
        CfgProject projet = programme("VIDER-PAYS");
        projet.setAdmCountry(fixture.pays("PAYS-" + jeton));
        projectRepository.save(projet);

        modifier(projet.getId(), avecChampsAVider(corps(), "paysId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paysId").doesNotExist());

        assertThat(fixture.ligneProjet(projet.getId()).get("AdmCountryId")).isNull();
    }

    @Test
    void r1_1_champsAVider_dateRemiseANull() throws Exception {
        CfgProject projet = programme("VIDER-DATE");
        projet.setWorkStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        projectRepository.save(projet);

        modifier(projet.getId(), avecChampsAVider(corps(), "dateDebutTravaux")).andExpect(status().isOk());

        assertThat(fixture.ligneProjet(projet.getId()).get("WorkStartDate")).isNull();
    }

    @Test
    void r1_1_champsAVider_signataireRemisANull() throws Exception {
        CfgProject projet = programme("VIDER-SIG");
        projet.setAdmSignatory(fiche.employe("EMP-VID-" + jeton));
        projectRepository.save(projet);

        modifier(projet.getId(), avecChampsAVider(corps(), "signataireId")).andExpect(status().isOk());

        assertThat(fixture.ligneProjet(projet.getId()).get("AdmSignatoryId")).isNull();
    }

    @Test
    void r1_1_champsAVider_lesAutresChampsInchanges() throws Exception {
        CfgProject projet = programme("VIDER-AUTRES");
        projet.setAdmCountry(fixture.pays("PAYS-A-" + jeton));
        projet.setWorkStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        projet.setCity("Fes");
        projectRepository.save(projet);

        modifier(projet.getId(), avecChampsAVider(corps(), "paysId")).andExpect(status().isOk());

        Tuple ligne = fixture.ligneProjet(projet.getId());
        assertAll(
                () -> assertThat(ligne.get("WorkStartDate")).isNotNull(),
                () -> assertThat(ligne.get("City")).isEqualTo("Fes"));
    }

    @ParameterizedTest
    @ValueSource(strings = {CODE, DESCRIPTION, "entiteId"})
    void r1_1_champsAVider_champObligatoire_400(String champ) throws Exception {
        CfgProject projet = programme("VIDER-OBL-" + champ.length());

        modifier(projet.getId(), avecChampsAVider(corps(), champ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(CHAMPS_A_VIDER)));
    }

    @Test
    void r1_1_champsAVider_nomInconnu_400() throws Exception {
        CfgProject projet = programme("VIDER-INC");

        modifier(projet.getId(), avecChampsAVider(corps(), "pasUnChamp"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(CHAMPS_A_VIDER)));
    }

    @Test
    void r1_1_champsAVider_champAussiPresentDansLeCorps_400() throws Exception {
        CfgProject projet = programme("VIDER-DOUBLE");
        UUID pays = fixture.pays("PAYS-D-" + jeton).getId();

        modifier(projet.getId(), avecChampsAVider(corps().avec("paysId", pays), "paysId"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(anyOf(containsString(CHAMPS_A_VIDER), containsString("paysId"))));
    }

    // ------------------------------------------------------------------ §3.3 zones

    @Test
    void r3_3_zone_creation_201_ecriteSurLeProgramme() throws Exception {
        CfgProject projet = programme("ZONE-CRE");
        UUID parent = fiche.zone(projet.getId(), null, "Z-PARENT", 1);

        String json = creerZone(projet.getId(), corps().avec(CODE, "Z-ENFANT").avec(DESCRIPTION, "Zone enfant")
                .avec(PARENT_ID, parent))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("Z-ENFANT"))
                .andExpect(jsonPath("$.parentId").value(parent.toString()))
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));
        assertThat(fiche.compter("SELECT COUNT(*) FROM CfgProjectZone WHERE PKey = :id AND Code = N'Z-ENFANT'", id))
                .isEqualTo(1);
        assertThat(fiche.compter("SELECT COUNT(*) FROM CfgProjectZone WHERE CfgProjectId = :id", projet.getId()))
                .isEqualTo(2);
    }

    @Test
    void r3_3_zones_listeAPlatTrieeSurSorting() throws Exception {
        CfgProject projet = programme("ZONE-LST");
        UUID b = fiche.zone(projet.getId(), null, "Z-B", 2);
        UUID a = fiche.zone(projet.getId(), b, "Z-A", 1);

        mockMvc.perform(get(ZONES, projet.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(a.toString()))
                .andExpect(jsonPath("$[0].parentId").value(b.toString()))
                .andExpect(jsonPath("$[1].id").value(b.toString()))
                .andExpect(jsonPath("$[1].aUneImage").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {CODE, DESCRIPTION})
    void r3_3_zone_codeOuDescriptionManquant_400(String champ) throws Exception {
        CfgProject projet = programme("ZONE-OBL-" + champ.length());

        creerZone(projet.getId(), corps().avec(CODE, "Z-OBL").avec(DESCRIPTION, "Zone").sans(champ))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r3_3_zone_memeCodeMemeParentMemeProgramme_409() throws Exception {
        CfgProject projet = programme("ZONE-UNI");
        UUID parent = fiche.zone(projet.getId(), null, "Z-P", 1);
        fiche.zone(projet.getId(), parent, "Z-DOUBLON", 1);

        creerZone(projet.getId(), corps().avec(CODE, "Z-DOUBLON").avec(DESCRIPTION, "Bis").avec(PARENT_ID, parent))
                .andExpect(status().isConflict());
    }

    @Test
    void r3_3_zone_memeCodeSousUnAutreParent_201() throws Exception {
        CfgProject projet = programme("ZONE-UNI-OK");
        UUID parent1 = fiche.zone(projet.getId(), null, "Z-P1", 1);
        UUID parent2 = fiche.zone(projet.getId(), null, "Z-P2", 2);
        fiche.zone(projet.getId(), parent1, "Z-COMMUN", 1);

        creerZone(projet.getId(), corps().avec(CODE, "Z-COMMUN").avec(DESCRIPTION, "Bis").avec(PARENT_ID, parent2))
                .andExpect(status().isCreated());
    }

    @Test
    void r3_3_zone_memeCodeDansUnAutreProgramme_201() throws Exception {
        CfgProject autre = programme("ZONE-AUTRE");
        fiche.zone(autre.getId(), null, "Z-RACINE", 1);
        CfgProject projet = programme("ZONE-CIBLE");

        creerZone(projet.getId(), corps().avec(CODE, "Z-RACINE").avec(DESCRIPTION, "Racine"))
                .andExpect(status().isCreated());
    }

    @Test
    void r3_3_zone_modification_200() throws Exception {
        CfgProject projet = programme("ZONE-MOD");
        UUID zone = fiche.zone(projet.getId(), null, "Z-MOD", 1);

        mockMvc.perform(patch(ZONE, projet.getId(), zone).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps().avec(DESCRIPTION, "Renommee").json()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Renommee"));

        assertThat(fiche.compter("SELECT COUNT(*) FROM CfgProjectZone WHERE PKey = :id AND Description = N'Renommee'",
                zone)).isEqualTo(1);
    }

    @Test
    void r3_3_zone_modification_zoneInconnue_404() throws Exception {
        CfgProject projet = programme("ZONE-MOD-404");

        mockMvc.perform(patch(ZONE, projet.getId(), UUID.randomUUID()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps().avec(DESCRIPTION, "x").json()))
                .andExpect(status().isNotFound());
    }

    @Test
    void r3_3_zone_suppressionFeuille_204() throws Exception {
        CfgProject projet = programme("ZONE-DEL");
        UUID zone = fiche.zone(projet.getId(), null, "Z-DEL", 1);

        mockMvc.perform(delete(ZONE, projet.getId(), zone).with(connecte(admin)))
                .andExpect(status().isNoContent());

        assertThat(fiche.compter(COMPTE_ZONES, zone)).isZero();
    }

    @Test
    void r3_3_zone_suppressionAvecEnfant_409() throws Exception {
        CfgProject projet = programme("ZONE-DEL-ENF");
        UUID parent = fiche.zone(projet.getId(), null, "Z-PERE", 1);
        fiche.zone(projet.getId(), parent, "Z-FILS", 1);

        mockMvc.perform(delete(ZONE, projet.getId(), parent).with(connecte(admin)))
                .andExpect(status().isConflict());

        assertThat(fiche.compter(COMPTE_ZONES, parent)).isEqualTo(1);
    }

    @Test
    void r3_3_zone_creation_nonAdmin_403() throws Exception {
        CfgProject projet = programme("ZONE-403");

        mockMvc.perform(post(ZONES, projet.getId()).with(commercial(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(CODE, "Z-403").avec(DESCRIPTION, "Zone").json()))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ §3.4 suppression

    @Test
    void r3_4_suppression_sansDependance_204_cascadesOmranSupprimees() throws Exception {
        CfgProject projet = programme("DEL-OK");
        UUID zone = fiche.zone(projet.getId(), null, "Z-CASCADE", 1);
        fiche.habilitationProjet(admin.getId(), projet.getId());
        fiche.pieceJointe(projet.getId());
        fiche.message(projet.getId());

        mockMvc.perform(delete(PROJET, projet.getId()).with(connecte(admin)))
                .andExpect(status().isNoContent());

        UUID id = projet.getId();
        assertAll(
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM CfgProject WHERE PKey = :id", id)).isZero(),
                () -> assertThat(fiche.compter(COMPTE_ZONES, zone)).isZero(),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM AdmUserHabilitation WHERE CfgProjectId = :id", id))
                        .isZero(),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM AdmAttachedFile WHERE CategoryId = :id", id))
                        .isZero(),
                () -> assertThat(fiche.compter("SELECT COUNT(*) FROM AdmMessage WHERE CategoryId = :id", id)).isZero());
    }

    @Test
    void r3_4_suppression_avecTranche_409_rienSupprime() throws Exception {
        CfgProject projet = programme("DEL-TR");
        fixture.tranche("TR-" + jeton + "-DEL", projet);

        mockMvc.perform(delete(PROJET, projet.getId()).with(connecte(admin)))
                .andExpect(status().isConflict());

        assertThat(fiche.compter("SELECT COUNT(*) FROM CfgProject WHERE PKey = :id", projet.getId())).isEqualTo(1);
    }

    @Test
    void r3_4_suppression_avecArticle_409() throws Exception {
        CfgProject projet = programme("DEL-ART");
        StkKind nature = fixture.natureBat();
        StkItemCategory categorie = fixture.categorie("CAT-" + jeton, nature, false);
        StkItemType type = fixture.type("TYP-" + jeton, nature, false);
        fiche.articleDuProjet(projet.getId(), type.getId(), categorie.getId(), fixture.vocationVente().getId(),
                nature.getId());

        mockMvc.perform(delete(PROJET, projet.getId()).with(connecte(admin)))
                .andExpect(status().isConflict());
    }

    @Test
    void r3_4_suppression_idInconnu_404() throws Exception {
        mockMvc.perform(delete(PROJET, UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void r3_4_suppression_nonAdmin_403() throws Exception {
        CfgProject projet = programme("DEL-403");

        mockMvc.perform(delete(PROJET, projet.getId()).with(commercial(admin)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ §3.5 liste

    @Test
    void r3_5_search_surLeCode_insensibleALaCasse() throws Exception {
        CfgProject cible = programme("SRCH-CODE");
        CfgProject autre = fixture.projet("PRJ-AUTRE-" + UUID.randomUUID().toString().substring(0, 8), entite);

        lister(cible.getCode().toLowerCase(Locale.ROOT))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligneDe(cible.getId())).isNotEmpty())
                .andExpect(jsonPath(ligneDe(autre.getId())).isEmpty());
    }

    @Test
    void r3_5_search_surLaDescription() throws Exception {
        CfgProject cible = programme("SRCH-DESC");
        cible.setDescription("Residence " + jeton + " Palmiers");
        projectRepository.save(cible);
        CfgProject autre = programme("SRCH-DESC-AUTRE");

        lister(jeton.toLowerCase(Locale.ROOT) + " palm")
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligneDe(cible.getId())).isNotEmpty())
                .andExpect(jsonPath(ligneDe(autre.getId())).isEmpty());
    }

    @Test
    void r3_5_search_surLEntite() throws Exception {
        CfgProject cible = fixture.projet("PRJ-SANS-JETON-" + UUID.randomUUID().toString().substring(0, 8), entite);
        CfgCompany autreEntite = fixture.entite("ENT-AUTRE-" + UUID.randomUUID().toString().substring(0, 8), true);
        CfgProject autre = fixture.projet("PRJ-AUTRE-ENT-" + UUID.randomUUID().toString().substring(0, 8), autreEntite);

        lister("ENT-" + jeton)
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligneDe(cible.getId())).isNotEmpty())
                .andExpect(jsonPath(ligneDe(autre.getId())).isEmpty());
    }

    @Test
    void r3_5_search_surLaVille() throws Exception {
        CfgProject cible = fixture.projet("PRJ-VILLE-" + UUID.randomUUID().toString().substring(0, 8), entite);
        cible.setCity("Ville" + jeton);
        projectRepository.save(cible);
        CfgProject autre = programme("SRCH-VILLE-AUTRE");

        lister("ville" + jeton.toLowerCase(Locale.ROOT))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligneDe(cible.getId())).isNotEmpty())
                .andExpect(jsonPath(ligneDe(autre.getId())).isEmpty());
    }

    /** §3.5 : colonnes du tableau Liste de l'aide, sous les noms déjà publiés par la fiche ({@code ProjetDetailDto}). */
    @Test
    void r3_5_liste_exposeLesColonnesDeLAideSousLesNomsDeLaFiche() throws Exception {
        CfgProject cible = programme("COLS");
        cible.setAddress("3 rue P1a");
        cible.setEmail("prg@p1a.ma");
        cible.setFax("0522111111");
        cible.setMobile("0600000000");
        cible.setPhone("0522222222");
        cible.setWebSite("p1a.ma");
        cible.setDescriptionLong("Detaillee");
        cible.setWorkStartDate(LocalDateTime.of(2026, 1, 1, 0, 0));
        cible.setWorkEndDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        cible.setMarketingStartDate(LocalDateTime.of(2026, 2, 1, 0, 0));
        cible.setMarketingEndDate(LocalDateTime.of(2027, 2, 1, 0, 0));
        cible.setUserCreate("Createur P1a");
        cible.setUserCreateDateTime(LocalDateTime.of(2026, 1, 1, 0, 0));
        cible.setUserUpdate("Modificateur P1a");
        cible.setUserUpdateDateTime(LocalDateTime.of(2026, 3, 1, 0, 0));
        projectRepository.save(cible);
        String ligne = ligneDe(cible.getId());

        lister(cible.getCode())
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne + ".adresse").value("3 rue P1a"))
                .andExpect(jsonPath(ligne + ".email").value("prg@p1a.ma"))
                .andExpect(jsonPath(ligne + ".fax").value("0522111111"))
                .andExpect(jsonPath(ligne + ".mobile").value("0600000000"))
                .andExpect(jsonPath(ligne + ".telephone").value("0522222222"))
                .andExpect(jsonPath(ligne + ".siteWeb").value("p1a.ma"))
                .andExpect(jsonPath(ligne + ".descriptionLongue").value("Detaillee"))
                .andExpect(jsonPath(ligne + ".dateDebutTravaux").value("2026-01-01T00:00:00"))
                .andExpect(jsonPath(ligne + ".dateFinTravaux").value("2027-01-01T00:00:00"))
                .andExpect(jsonPath(ligne + ".dateDebutCommercialisation").value("2026-02-01T00:00:00"))
                .andExpect(jsonPath(ligne + ".dateFinCommercialisation").value("2027-02-01T00:00:00"))
                .andExpect(jsonPath(ligne + ".creePar").value("Createur P1a"))
                .andExpect(jsonPath(ligne + ".creeLe").value("2026-01-01T00:00:00"))
                .andExpect(jsonPath(ligne + ".modifiePar").value("Modificateur P1a"))
                .andExpect(jsonPath(ligne + ".modifieLe").value("2026-03-01T00:00:00"))
                .andExpect(jsonPath(ligne + ".entiteCode").value("ENT-" + jeton));
    }
}
