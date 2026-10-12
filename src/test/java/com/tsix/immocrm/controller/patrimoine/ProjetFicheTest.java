package com.tsix.immocrm.controller.patrimoine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tsix.immocrm.domain.TypeTiersPatrimoine;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.fixtures.FixtureFichePatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectZoneRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-tranche-fiche.md §3 et §6. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjetFicheTest {

    private static final String PROJETS = "/api/projets";
    private static final String UN_PROJET = "/api/projets/{id}";
    private static final String ZONES = "/api/projets/{id}/zones";
    private static final String UNE_ZONE = "/api/projets/{id}/zones/{zoneId}";
    private static final String CHAMP = "$.champ";
    private static final String CHAMPS_A_VIDER = "champsAVider";
    private static final String SIGNATAIRE = "signataireId";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final JsonMapper JSON = JsonMapper.builder().findAndAddModules().build();

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureFichePatrimoine fiche;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgProjectZoneRepository zoneRepository;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private AdmUserHabilitationRepository habilitationRepository;
    @Autowired private EntityManager em;

    private AdmUser admin;
    private CfgCompany entite;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-PRJ-FICHE");
        entite = fixture.entite("ENT-PRJ-FICHE", true);
    }

    private Map<String, Object> corps(String code) {
        Map<String, Object> corps = new HashMap<>();
        corps.put("entiteId", entite.getId());
        corps.put(CODE, code);
        corps.put(DESCRIPTION, "Programme " + code);
        return corps;
    }

    private ResultActions ecrire(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requete,
                                 Map<String, ?> corps, String... roles) throws Exception {
        return mockMvc.perform(requete.with(connecte(admin, roles))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    private UUID idDe(ResultActions resultat) throws Exception {
        JsonNode noeud = JSON.readTree(resultat.andReturn().getResponse().getContentAsString());
        return UUID.fromString(noeud.get("id").asText());
    }

    // ------------------------------------------------------------------ signataire et responsables

    @Test
    void creation_signataireEtResponsables_ecritsEtRelus() throws Exception {
        CfgTier signataire = fiche.tiers(TypeTiersPatrimoine.PERSONNEL, "EMP-PRJ-SIG");
        CfgTier technique = fiche.tiers(TypeTiersPatrimoine.PERSONNEL, "EMP-PRJ-TEC");
        CfgTier commercial = fiche.tiers(TypeTiersPatrimoine.PERSONNEL, "EMP-PRJ-COM");
        Map<String, Object> corps = corps("PRJ-FICHE-RESP");
        corps.put(SIGNATAIRE, signataire.getId());
        corps.put("responsableTechniqueId", technique.getId());
        corps.put("responsableCommercialId", commercial.getId());

        ecrire(post(PROJETS), corps, ROLE_ADMIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.signataireId").value(signataire.getId().toString()))
                .andExpect(jsonPath("$.signataireLibelle").value("Tiers EMP-PRJ-SIG"))
                .andExpect(jsonPath("$.responsableTechniqueLibelle").value("Tiers EMP-PRJ-TEC"))
                .andExpect(jsonPath("$.responsableCommercialLibelle").value("Tiers EMP-PRJ-COM"));
    }

    @Test
    void creation_400_signataireInconnu() throws Exception {
        Map<String, Object> corps = corps("PRJ-FICHE-SIG404");
        corps.put(SIGNATAIRE, UUID.randomUUID());
        ecrire(post(PROJETS), corps, ROLE_ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value(SIGNATAIRE));
    }

    @Test
    void creation_400_responsableQuiNEstPasUnPersonnel() throws Exception {
        Map<String, Object> corps = corps("PRJ-FICHE-PRO");
        corps.put("responsableCommercialId", fiche.prospect("PRO-PRJ-FICHE").getId());
        ecrire(post(PROJETS), corps, ROLE_ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("responsableCommercialId"))
                .andExpect(jsonPath("$.detail").value(containsString("n'est pas un personnel")));
    }

    // ------------------------------------------------------------------ défauts de l'entité

    @Test
    void defautsEntite_codeDescriptionEtCoordonneesCopies() throws Exception {
        AdmCountry pays = fixture.pays("MA-PRJ-DEF");
        CfgTier signataire = fiche.tiers(TypeTiersPatrimoine.PERSONNEL, "EMP-PRJ-DEF");
        entite.setPhone("0522000000");
        entite.setCity("Rabat");
        entite.setSignatureCity("Casablanca");
        entite.setAdmCountry(pays);
        entite.setAdmSignatory(signataire);
        companyRepository.save(entite);

        mockMvc.perform(get(PROJETS + "/defauts-entite/{entiteId}", entite.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entiteId").value(entite.getId().toString()))
                .andExpect(jsonPath("$.code").value("ENT-PRJ-FICHE"))
                .andExpect(jsonPath("$.description").value("Entité ENT-PRJ-FICHE"))
                .andExpect(jsonPath("$.telephone").value("0522000000"))
                .andExpect(jsonPath("$.ville").value("Rabat"))
                .andExpect(jsonPath("$.villeSignature").value("Casablanca"))
                .andExpect(jsonPath("$.paysId").value(pays.getId().toString()))
                .andExpect(jsonPath("$.signataireId").value(signataire.getId().toString()));
    }

    @Test
    void defautsEntite_404_entiteInconnue() throws Exception {
        mockMvc.perform(get(PROJETS + "/defauts-entite/{entiteId}", UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ champsAVider

    @Test
    void champsAVider_paysEtDateRemisANull_texteAVide() throws Exception {
        CfgProject projet = fixture.projet("PRJ-FICHE-VIDER", entite);
        projet.setAdmCountry(fixture.pays("MA-PRJ-VID"));
        projet.setWorkStartDate(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
        projet.setCity("Fès");
        projectRepository.save(projet);

        ecrire(patch(UN_PROJET, projet.getId()),
                Map.of(CHAMPS_A_VIDER, List.of("paysId", "dateDebutTravaux", "ville")), ROLE_ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paysId").doesNotExist())
                .andExpect(jsonPath("$.dateDebutTravaux").doesNotExist())
                .andExpect(jsonPath("$.ville").value(""));

        em.flush();
        em.clear();
        CfgProject relu = projectRepository.findById(projet.getId()).orElseThrow();
        assertThat(relu.getAdmCountry()).isNull();
    }

    @Test
    void champsAVider_400_champObligatoire() throws Exception {
        CfgProject projet = fixture.projet("PRJ-FICHE-OBL", entite);
        ecrire(patch(UN_PROJET, projet.getId()), Map.of(CHAMPS_A_VIDER, List.of(CODE)), ROLE_ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value(CHAMPS_A_VIDER));
    }

    @Test
    void champsAVider_400_nomInconnu() throws Exception {
        CfgProject projet = fixture.projet("PRJ-FICHE-INC", entite);
        ecrire(patch(UN_PROJET, projet.getId()), Map.of(CHAMPS_A_VIDER, List.of("pasUnChamp")), ROLE_ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value(CHAMPS_A_VIDER));
    }

    @Test
    void champsAVider_400_champRenseigneEtListe() throws Exception {
        CfgProject projet = fixture.projet("PRJ-FICHE-DBL", entite);
        Map<String, Object> corps = new HashMap<>();
        corps.put("ville", "Tanger");
        corps.put(CHAMPS_A_VIDER, List.of("ville"));
        ecrire(patch(UN_PROJET, projet.getId()), corps, ROLE_ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value(CHAMPS_A_VIDER));
    }

    // ------------------------------------------------------------------ zones

    private UUID zone(UUID projetId, String code, UUID parentId) throws Exception {
        Map<String, Object> corps = new HashMap<>();
        corps.put(CODE, code);
        corps.put(DESCRIPTION, "Zone " + code);
        corps.put("parentId", parentId);
        return idDe(ecrire(post(ZONES, projetId), corps, ROLE_ADMIN).andExpect(status().isCreated()));
    }

    @Test
    void zones_arbreNiveauEtOrdreCalcules() throws Exception {
        CfgProject projet = fixture.projet("PRJ-FICHE-ZON", entite);
        UUID racine = zone(projet.getId(), "Z1", null);
        zone(projet.getId(), "Z2", null);
        UUID enfant = zone(projet.getId(), "Z1-A", racine);

        mockMvc.perform(get(ZONES, projet.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.code=='Z2')].ordre").value(2))
                .andExpect(jsonPath("$[?(@.id=='" + enfant + "')].niveau").value(1))
                .andExpect(jsonPath("$[?(@.id=='" + enfant + "')].parentId").value(racine.toString()))
                .andExpect(jsonPath("$[?(@.id=='" + racine + "')].aUneImage").value(false));
    }

    @Test
    void zones_409_codeDejaPrisSousLeMemeParent_libreAilleurs() throws Exception {
        CfgProject projet = fixture.projet("PRJ-FICHE-ZUN", entite);
        UUID racine = zone(projet.getId(), "BLOC", null);
        zone(projet.getId(), "ETAGE", racine);

        Map<String, Object> doublon = new HashMap<>(Map.of(CODE, "etage", DESCRIPTION, "Doublon"));
        doublon.put("parentId", racine);
        ecrire(post(ZONES, projet.getId()), doublon, ROLE_ADMIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath(CHAMP).value(CODE));
        zone(projet.getId(), "ETAGE", null);
    }

    @Test
    void zones_modification_200_etParentNonModifiable() throws Exception {
        CfgProject projet = fixture.projet("PRJ-FICHE-ZMO", entite);
        UUID racine = zone(projet.getId(), "A", null);
        UUID autre = zone(projet.getId(), "B", null);

        ecrire(patch(UNE_ZONE, projet.getId(), racine), Map.of(DESCRIPTION, "Bloc A"), ROLE_ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Bloc A"));
        ecrire(patch(UNE_ZONE, projet.getId(), racine), Map.of("parentId", autre), ROLE_ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(CHAMP).value("parentId"));
    }

    @Test
    void zones_suppression_409_avecEnfant_204_feuille() throws Exception {
        CfgProject projet = fixture.projet("PRJ-FICHE-ZSU", entite);
        UUID racine = zone(projet.getId(), "R", null);
        UUID feuille = zone(projet.getId(), "R-1", racine);

        mockMvc.perform(delete(UNE_ZONE, projet.getId(), racine).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("sous-zone")));
        mockMvc.perform(delete(UNE_ZONE, projet.getId(), feuille).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNoContent());
        assertThat(zoneRepository.findById(feuille)).isEmpty();
    }

    @Test
    void zones_403_nonAdministrateur() throws Exception {
        CfgProject projet = fixture.projet("PRJ-FICHE-Z403", entite);
        ecrire(post(ZONES, projet.getId()), Map.of(CODE, "X", DESCRIPTION, "X"), "Commercial")
                .andExpect(status().isForbidden());
    }

    @Test
    void zones_404_programmeInconnu() throws Exception {
        mockMvc.perform(get(ZONES, UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ suppression

    @Test
    void suppression_409_avecTranche() throws Exception {
        CfgProject projet = fixture.projet("PRJ-FICHE-DEL409", entite);
        fixture.tranche("TRA-FICHE-DEL409", projet);
        em.flush();

        mockMvc.perform(delete(UN_PROJET, projet.getId()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("1 tranche(s)")));
        assertThat(projectRepository.findById(projet.getId())).isPresent();
    }

    @Test
    void suppression_204_habilitationsEtZonesSupprimees() throws Exception {
        UUID id = idDe(ecrire(post(PROJETS), corps("PRJ-FICHE-DEL"), ROLE_ADMIN).andExpect(status().isCreated()));
        UUID zoneId = zone(id, "ZD", null);
        em.flush();
        assertThat(habilitationRepository.findByCfgProjectId(id)).isNotEmpty();

        mockMvc.perform(delete(UN_PROJET, id).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNoContent());

        em.flush();
        em.clear();
        assertThat(projectRepository.findById(id)).isEmpty();
        assertThat(zoneRepository.findById(zoneId)).isEmpty();
        assertThat(habilitationRepository.findByCfgProjectId(id)).isEmpty();
    }

    @Test
    void suppression_404_idInconnu() throws Exception {
        mockMvc.perform(delete(UN_PROJET, UUID.randomUUID()).with(connecte(admin, ROLE_ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void suppression_403_nonAdministrateur() throws Exception {
        CfgProject projet = fixture.projet("PRJ-FICHE-DEL403", entite);
        mockMvc.perform(delete(UN_PROJET, projet.getId()).with(connecte(admin, "Commercial")))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ liste

    @Test
    void liste_rechercheServeurEtColonnesOmran() throws Exception {
        CfgTier representant = fiche.tiers(TypeTiersPatrimoine.PERSONNEL, "EMP-PRJ-REP");
        entite.setCfgTier(representant);
        companyRepository.save(entite);
        CfgProject cible = fixture.projet("PRJ-FICHE-QZW", entite);
        cible.setCity("Agadir");
        cible.setAddress(" 12 rue X ");
        cible.setAdmSignatory(fiche.tiers(TypeTiersPatrimoine.PERSONNEL, "EMP-PRJ-LSIG"));
        projectRepository.save(cible);
        fixture.projet("PRJ-FICHE-AUTRE", entite);

        mockMvc.perform(get(PROJETS).param("search", "qzw").with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").value("PRJ-FICHE-QZW"))
                .andExpect(jsonPath("$.content[0].entiteLibelle").value("Entité ENT-PRJ-FICHE"))
                .andExpect(jsonPath("$.content[0].adresseComplete").value("Agadir 12 rue X"))
                .andExpect(jsonPath("$.content[0].representantLegal").value("Tiers EMP-PRJ-REP"))
                .andExpect(jsonPath("$.content[0].signataireLibelle").value("Tiers EMP-PRJ-LSIG"));
    }
}
