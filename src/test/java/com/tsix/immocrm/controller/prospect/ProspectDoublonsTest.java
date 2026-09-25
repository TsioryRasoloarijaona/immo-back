package com.tsix.immocrm.controller.prospect;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règle 1 de {@code instructions/FEATURE-doublons-prospect-projet.md} : {@code GET
 * /api/prospects/doublons}, le 409 de {@code POST /api/prospects} et de {@code POST
 * /api/pre-prospections/{id}/creer-prospect}, et {@code POST .../lier-prospect}.
 *
 * <p>Chaque test tire un nom unique : la recherche porte sur toute la table, et la base de test
 * est partagée entre les classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectDoublonsTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private CfgTierTypeRepository tierTypeRepository;
    @Autowired
    private ComProspectionRepository prospectionRepository;

    private CfgTierType typePro;
    private CfgTierType typeCli;
    private CfgTierType typeAutre;

    @BeforeEach
    void setUp() {
        typePro = type("PRO", "PROSPECT");
        typeCli = type("CLI", "CLIENT");
        typeAutre = type("FOU-DBL-" + suffixe(), "FOURNISSEUR");
    }

    private CfgTierType type(String code, String description) {
        return tierTypeRepository.findByCode(code).orElseGet(() -> {
            CfgTierType t = new CfgTierType();
            t.setId(UUID.randomUUID());
            t.setCode(code);
            t.setDescription(description);
            return tierTypeRepository.save(t);
        });
    }

    private static String suffixe() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private CfgTier fiche(CfgTierType type, String prenom, String nom, String mobile, String email) {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode("DBL-" + suffixe());
        t.setCfgTierType(type);
        t.setFirstName(prenom);
        t.setLastName(nom);
        t.setDescription(((prenom == null ? "" : prenom + " ") + nom).trim());
        t.setMobile(mobile);
        t.setEMail(email);
        return tierRepository.save(t);
    }

    private ResultActions doublons(String... params) throws Exception {
        var requete = get("/api/prospects/doublons");
        for (int i = 0; i < params.length; i += 2) {
            requete = requete.param(params[i], params[i + 1]);
        }
        return mockMvc.perform(requete);
    }

    private static String filtreId(CfgTier t) {
        return "$[?(@.id=='" + t.getId() + "')]";
    }

    // ---------------------------------------------------------------- GET /api/prospects/doublons

    @Test
    void doublons_memeNom_sansCasseNiEspaces_etDansLesDeuxOrdres() throws Exception {
        String nom = "LAHLOU" + suffixe();
        CfgTier fiche = fiche(typePro, "El Mehdi", nom, null, null);

        doublons("prenom", "EL MEHDI", "nom", nom.toLowerCase())
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtreId(fiche) + ".criteres[*]", containsInAnyOrder("NOM")))
                .andExpect(jsonPath(filtreId(fiche) + ".nom").value("El Mehdi " + nom));
        // Prénom et nom inversés : la règle d'OMRAN compare les deux ordres.
        doublons("prenom", nom, "nom", "El Mehdi")
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtreId(fiche)).exists());
        // Nom complet dans `nom` seul, sans prénom : Description sans espaces.
        doublons("nom", "el mehdi " + nom)
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtreId(fiche)).exists());
    }

    @Test
    void doublons_memeTelephone_quelQueSoitLeFormat() throws Exception {
        CfgTier fiche = fiche(typePro, "Badr", "ARH" + suffixe(), "6 65 68 09 64", null);

        doublons("mobile", "+212 665680964")
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtreId(fiche) + ".criteres[*]", containsInAnyOrder("TELEPHONE")));
        doublons("telephone", "0665-68-09-64")
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtreId(fiche)).exists());
        doublons("mobile", "0665680965")
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtreId(fiche)).doesNotExist());
    }

    @Test
    void doublons_memeEmail_sansCasse() throws Exception {
        String email = "charifa." + suffixe() + "@gmail.com";
        CfgTier fiche = fiche(typeCli, "Charifa", "ELBACHIRI" + suffixe(), null, email);

        doublons("email", "  " + email.toUpperCase() + " ")
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtreId(fiche) + ".criteres[*]", containsInAnyOrder("EMAIL")))
                .andExpect(jsonPath(filtreId(fiche) + ".type").value("CLI"));
    }

    @Test
    void doublons_plusDeCriteres_enPremier() throws Exception {
        String nom = "DAHBI" + suffixe();
        CfgTier nomSeul = fiche(typePro, "Aicha", nom, "6 12 00 00 01", null);
        CfgTier nomEtTel = fiche(typePro, "Aicha", nom, "387734929", null);

        doublons("prenom", "Aicha", "nom", nom, "mobile", "387734929")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(nomEtTel.getId().toString()))
                .andExpect(jsonPath("$[0].criteres[*]", containsInAnyOrder("NOM", "TELEPHONE")))
                .andExpect(jsonPath("$[1].id").value(nomSeul.getId().toString()))
                .andExpect(jsonPath("$[1].criteres[*]", containsInAnyOrder("NOM")));
    }

    @Test
    void doublons_valeursBidon_neSontPasDesCriteres() throws Exception {
        CfgTier bidon = fiche(typePro, "Zmaimita", "HASSANIYA" + suffixe(), "0 00 00 00 00", "nd");

        // Seul le téléphone bidon : aucun critère utilisable.
        doublons("mobile", "000000000").andExpect(status().isBadRequest());
        doublons("email", "nd").andExpect(status().isBadRequest());
        doublons("telephone", "12345").andExpect(status().isBadRequest());
        // Un nom qui ne correspond pas + un téléphone bidon : la fiche bidon n'apparaît pas.
        doublons("nom", "Autre" + suffixe(), "mobile", "000000000", "email", "nd")
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtreId(bidon)).doesNotExist());
    }

    @Test
    void doublons_sansAucunParametre_400() throws Exception {
        doublons().andExpect(status().isBadRequest());
    }

    @Test
    void doublons_seulsLesProspectsEtClients() throws Exception {
        String nom = "BENYAICH" + suffixe();
        CfgTier fournisseur = fiche(typeAutre, "Buabid", nom, null, null);
        CfgTier sansType = fiche(null, "Buabid", nom, null, null);
        CfgTier prospect = fiche(typePro, "Buabid", nom, null, null);

        doublons("prenom", "Buabid", "nom", nom)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath(filtreId(prospect)).exists())
                .andExpect(jsonPath(filtreId(fournisseur)).doesNotExist())
                .andExpect(jsonPath(filtreId(sansType)).doesNotExist());
    }

    // ---------------------------------------------------------------- POST /api/prospects

    @Test
    void creerProspect_homonyme_409_puisIgnorerDoublons_201() throws Exception {
        String nom = "TAZI" + suffixe();
        CfgTier existant = fiche(typePro, "Rachid", nom, "6 65 13 65 65", null);
        long avant = tierRepository.count();

        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON).content("""
                        {"prenom":"Rachid","nom":"%s","mobile":"0661182247","indicatifMobile":"212"}"""
                        .formatted(nom)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.doublons", hasSize(1)))
                .andExpect(jsonPath("$.doublons[0].id").value(existant.getId().toString()))
                .andExpect(jsonPath("$.doublons[0].criteres[*]", containsInAnyOrder("NOM")));
        assertThat(tierRepository.count()).as("rien n'est écrit sur un 409").isEqualTo(avant);

        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON).content("""
                        {"prenom":"Rachid","nom":"%s","ignorerDoublons":true}""".formatted(nom)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value(nom));
        assertThat(tierRepository.count()).isEqualTo(avant + 1);
    }

    @Test
    void creerProspect_sansDoublon_201_commeAvant() throws Exception {
        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON).content("""
                        {"prenom":"Unique","nom":"%s"}""".formatted("NOM" + suffixe())))
                .andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- pré-prospection

    private String preProspection(String nom) throws Exception {
        String json = mockMvc.perform(post("/api/pre-prospections").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nom":"%s","telephone":"0612345678"}""".formatted(nom)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.preProspection.id");
    }

    @Test
    void creerProspectDePreProspection_409_puisLierProspect_200() throws Exception {
        String nom = "SLAOUI" + suffixe();
        CfgTier existant = fiche(typePro, "Houda", nom, "6 61 22 72 29", "slaoui." + suffixe() + "@gmail.com");
        String id = preProspection("Houda " + nom);

        mockMvc.perform(post("/api/pre-prospections/{id}/creer-prospect", id)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"prenom":"Nourelhouda","nom":"%s","mobile":"0661227229","indicatifMobile":"212"}"""
                                .formatted(nom)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.doublons[0].id").value(existant.getId().toString()))
                .andExpect(jsonPath("$.doublons[0].criteres[*]", containsInAnyOrder("TELEPHONE")));

        mockMvc.perform(post("/api/pre-prospections/{id}/lier-prospect", id)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"prospectId":"%s"}""".formatted(existant.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prospectId").value(existant.getId().toString()))
                .andExpect(jsonPath("$.prospectNom").value("Houda " + nom));

        ComProspection ligne = prospectionRepository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(ligne.getCfgProspect().getId()).isEqualTo(existant.getId());

        mockMvc.perform(get("/api/prospections/{id}/historique", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.description=='Prospect existant rattaché : Houda " + nom + "')]").exists());

        // Déjà lié : même 400 que creer-prospect.
        mockMvc.perform(post("/api/pre-prospections/{id}/lier-prospect", id)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"prospectId":"%s"}""".formatted(existant.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creerProspectDePreProspection_ignorerDoublons_201() throws Exception {
        String nom = "BENNANI" + suffixe();
        fiche(typePro, "Houda", nom, null, null);
        String id = preProspection("Houda " + nom);

        mockMvc.perform(post("/api/pre-prospections/{id}/creer-prospect", id)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"prenom":"Houda","nom":"%s","ignorerDoublons":true}""".formatted(nom)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prospectNom").value("Houda " + nom));
    }

    @Test
    void lierProspect_prospectIdManquantOuInconnu_400() throws Exception {
        String id = preProspection("Lead " + suffixe());
        mockMvc.perform(post("/api/pre-prospections/{id}/lier-prospect", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/pre-prospections/{id}/lier-prospect", id)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"prospectId":"%s"}""".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lierProspect_tiersNiProspectNiClient_400() throws Exception {
        String id = preProspection("Lead " + suffixe());
        CfgTier fournisseur = fiche(typeAutre, "Four", "NISSEUR" + suffixe(), null, null);
        mockMvc.perform(post("/api/pre-prospections/{id}/lier-prospect", id)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"prospectId":"%s"}""".formatted(fournisseur.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lierProspect_preProspectionInconnue_404() throws Exception {
        CfgTier prospect = fiche(typePro, "Sara", "HABAFI" + suffixe(), null, null);
        mockMvc.perform(post("/api/pre-prospections/{id}/lier-prospect", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"prospectId":"%s"}""".formatted(prospect.getId())))
                .andExpect(status().isNotFound());
    }
}
