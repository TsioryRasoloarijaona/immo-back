package com.tsix.immocrm.controller.prospect;

import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.repositories.adm.AdmCountryRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Indicatif obligatoire et formatage des numéros — voir
 * {@code instructions/FEATURE-indicatif-telephone.md}.
 *
 * <p>Le sujet de ces tests tient en une phrase : <b>un numéro ne s'écrit jamais sans son
 * indicatif, et il s'écrit au format que la base porte déjà</b>. C'est ce qui rend le WhatsApp
 * fiable en aval — un numéro sans indicatif est inenvoyable, un indicatif fautif expédie le
 * message à un inconnu.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectIndicatifTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AdmCountryRepository countryRepository;
    @Autowired private CfgTierRepository tierRepository;

    /**
     * Le référentiel de la base de test ne porte pas forcément d'indicatif : on garantit la
     * présence du 212, sur lequel s'appuient les cas nominaux.
     */
    @BeforeEach
    void garantirLeMaroc() {
        boolean maroc = countryRepository.findAll().stream()
                .anyMatch(c -> "212".equals(c.getPhoneCode()));
        if (!maroc) {
            AdmCountry pays = new AdmCountry();
            pays.setId(UUID.randomUUID());
            pays.setCode("MA-" + UUID.randomUUID());
            pays.setDescription("MAROC");
            pays.setPhoneCode("212");
            countryRepository.saveAndFlush(pays);
        }
    }

    private String creer(String champsTelephone) {
        return """
                {"prenom":"Tsiory","nom":"NAHOMA"%s}""".formatted(champsTelephone);
    }

    private CfgTier relire(String id) {
        return tierRepository.findById(UUID.fromString(id)).orElseThrow();
    }

    // ------------------------------------------------------------------ V1 : l'indicatif suit

    @Test
    void mobileSansIndicatif_refuse() throws Exception {
        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON)
                        .content(creer(",\"mobile\":\"6 98 29 80 95\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void telephoneSansIndicatif_refuse() throws Exception {
        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON)
                        .content(creer(",\"telephone\":\"5 22 27 34 96\"")))
                .andExpect(status().isBadRequest());
    }

    /** L'indicatif seul ne qualifie rien : le refuser évite de croire un numéro enregistré. */
    @Test
    void indicatifSansNumero_refuse() throws Exception {
        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON)
                        .content(creer(",\"indicatifMobile\":\"212\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mobileAvecIndicatif_accepteEtEnregistreLesDeux() throws Exception {
        String reponse = mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON)
                        .content(creer(",\"mobile\":\"6 98 29 80 95\",\"indicatifMobile\":\"212\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mobile").value("6 98 29 80 95"))
                .andExpect(jsonPath("$.indicatifMobile").value("212"))
                .andReturn().getResponse().getContentAsString();

        CfgTier enBase = relire(com.jayway.jsonpath.JsonPath.parse(reponse).read("$.id"));
        assertThat(enBase.getMobile()).isEqualTo("6 98 29 80 95");
        assertThat(enBase.getMobileIndicatif()).isEqualTo("212");
    }

    // ------------------------------------------------------------------ V2 : le référentiel

    /**
     * {@code 1483} est une valeur réellement présente en base — une faute de frappe parmi les 30
     * que cette règle rend désormais impossibles à créer.
     */
    @Test
    void indicatifHorsReferentiel_refuse() throws Exception {
        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON)
                        .content(creer(",\"mobile\":\"6 98 29 80 95\",\"indicatifMobile\":\"1483\"")))
                .andExpect(status().isBadRequest());
    }

    /** Le front peut envoyer « +212 » : c'est ce qu'affiche un sélecteur. Stocké sans le +. */
    @Test
    void indicatifPrefixePlus_estAccepteEtStockeSansLePlus() throws Exception {
        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON)
                        .content(creer(",\"mobile\":\"6 98 29 80 95\",\"indicatifMobile\":\"+212\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.indicatifMobile").value("212"));
    }

    // ------------------------------------------------------------------ V3 : le formatage

    /**
     * La forme de stockage est celle de 14 285 des 14 458 fiches existantes. Nos écritures
     * restent ainsi indiscernables de celles de l'application Windows.
     */
    @Test
    void numeroNationalAvecZeroInitial_estRamenAuFormatDeLaBase() throws Exception {
        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON)
                        .content(creer(",\"mobile\":\"0698298095\",\"indicatifMobile\":\"212\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mobile").value("6 98 29 80 95"));
    }

    @Test
    void numeroColleSansEspace_estEspace() throws Exception {
        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON)
                        .content(creer(",\"mobile\":\"698298095\",\"indicatifMobile\":\"212\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mobile").value("6 98 29 80 95"));
    }

    /**
     * Un mobile nord-américain fait 10 chiffres : il ne suit pas le découpage marocain et reste
     * en chiffres nus, exactement comme les 47 fiches réelles qui en portent un. C'est la raison
     * pour laquelle la validation ne fige pas la longueur à 9.
     */
    @Test
    void numeroA10Chiffres_resteEnChiffresNus() throws Exception {
        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON)
                        .content(creer(",\"mobile\":\"5145829397\",\"indicatifMobile\":\"212\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mobile").value("5145829397"));
    }

    // ------------------------------------------------------------------ V4 : longueur

    @Test
    void numeroTropCourt_refuse() throws Exception {
        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON)
                        .content(creer(",\"mobile\":\"123\",\"indicatifMobile\":\"212\"")))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ V5 : PATCH partiel

    /**
     * Sans cette porte, les 7 fiches réelles portant un mobile sans indicatif deviendraient
     * immodifiables — on ne pourrait plus corriger leur nom sans d'abord réparer leur téléphone.
     */
    @Test
    void modificationSansChampTelephone_passeSurUneFicheSansIndicatif() throws Exception {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("TIER-IND-" + UUID.randomUUID());
        tier.setDescription("Fiche Legacy");
        tier.setMobile("6 61 22 48 57");
        tier.setMobileIndicatif(null);
        tierRepository.saveAndFlush(tier);

        mockMvc.perform(patch("/api/prospects/{id}", tier.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prenom\":\"Corrige\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prenom").value("Corrige"));

        // Le numéro n'a pas été touché : la règle ne s'applique qu'à ce que la requête fournit.
        assertThat(relire(tier.getId().toString()).getMobile()).isEqualTo("6 61 22 48 57");
    }

    @Test
    void modificationDuMobileSansIndicatif_refuseMemeSiLaFicheEnAvaitUn() throws Exception {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("TIER-IND-" + UUID.randomUUID());
        tier.setDescription("Deja Renseignee");
        tier.setMobile("6 61 22 48 57");
        tier.setMobileIndicatif("212");
        tierRepository.saveAndFlush(tier);

        // Pas de reprise implicite : un numéro qui change peut changer de pays.
        mockMvc.perform(patch("/api/prospects/{id}", tier.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobile\":\"6 98 29 80 95\"}"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ lecture

    @Test
    void laFicheRendSesDeuxIndicatifs() throws Exception {
        String reponse = mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON)
                        .content(creer(",\"mobile\":\"6 98 29 80 95\",\"indicatifMobile\":\"212\","
                                + "\"telephone\":\"5 22 27 34 96\",\"indicatifTelephone\":\"212\"")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.parse(reponse).read("$.id");
        mockMvc.perform(get("/api/prospects/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicatifMobile").value("212"))
                .andExpect(jsonPath("$.indicatifTelephone").value("212"))
                .andExpect(jsonPath("$.telephone").value("5 22 27 34 96"));
    }

    /**
     * Un fixe marocain commence par 5 : la validation à l'écriture ne doit surtout pas appliquer
     * la règle « commence par 6 ou 7 » de {@code NumeroWhatsApp}, qui ne vaut que pour décider
     * d'un envoi.
     */
    @Test
    void fixeCommencantPar5_estAccepte() throws Exception {
        mockMvc.perform(post("/api/prospects").contentType(MediaType.APPLICATION_JSON)
                        .content(creer(",\"telephone\":\"5 22 27 34 96\",\"indicatifTelephone\":\"212\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telephone").value("5 22 27 34 96"));
    }
}
