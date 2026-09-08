package com.tsix.immocrm.controller.hotline;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.hotline.HotlineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recherche hotline par nom — voir {@code instructions/FEATURE-hotline-recherche-nom.md}.
 *
 * <p>Chaque test utilise un marqueur unique dans le nom recherché : la base de test partage ses
 * tables avec les fixtures des autres classes, et chercher « ALAMI » y ramènerait des lignes
 * qu'on n'a pas créées.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class HotlineRechercheNomTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    private CfgTier newTier(String description, String prenom, String nom) {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode("HL-NOM-" + UUID.randomUUID());
        t.setDescription(description);
        t.setFirstName(prenom);
        t.setLastName(nom);
        return tierRepository.save(t);
    }

    private ComProspection newPreProspection(String tempDescription) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("HL-PRE-" + UUID.randomUUID());
        p.setTempDescription(tempDescription);
        return prospectionRepository.save(p);
    }

    // --- Recherche nominale ---

    @Test
    void parNom_trouveLaFicheEtRenseigneLeCritere() throws Exception {
        String marqueur = "ZZTESTNOM" + System.nanoTime();
        newTier("Ines " + marqueur, "Ines", marqueur);

        mockMvc.perform(get("/api/hotline/recherche").param("nom", marqueur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.critere").value("NOM"))
                .andExpect(jsonPath("$.nomRecherche").value(marqueur))
                .andExpect(jsonPath("$.connu").value(true))
                .andExpect(jsonPath("$.tronque").value(false))
                .andExpect(jsonPath("$.fiches.length()").value(1))
                // Les champs propres au téléphone n'ont pas de sens ici.
                .andExpect(jsonPath("$.telephoneRecherche").doesNotExist())
                .andExpect(jsonPath("$.telephoneNormalise").doesNotExist());
    }

    @Test
    void parNom_insensibleALaCasse() throws Exception {
        String marqueur = "ZzTestCasse" + System.nanoTime();
        newTier("Client " + marqueur, "Client", marqueur);

        mockMvc.perform(get("/api/hotline/recherche").param("nom", marqueur.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches.length()").value(1));
    }

    /**
     * Le test qui justifie le {@code COLLATE French_CI_AI}. La base est en
     * {@code French_CI_AS} — sensible aux accents — et contient de vrais noms accentués
     * (« Héjér F'H »). Sans le COLLATE explicite, ce test échoue et la recherche serait
     * inutilisable en production tout en paraissant correcte sur des jeux de test ASCII.
     */
    @Test
    void parNom_insensibleAuxAccents() throws Exception {
        String marqueur = "ZZACCENT" + System.nanoTime();
        newTier("Héjér " + marqueur, "Héjér", marqueur);

        mockMvc.perform(get("/api/hotline/recherche").param("nom", "Hejer " + marqueur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches.length()").value(1));
    }

    @Test
    void parNom_trouveAussiSurLeNomDeFamilleSeul() throws Exception {
        String marqueur = "ZZFAMILLE" + System.nanoTime();
        // Description volontairement différente : seul LastName porte le marqueur.
        newTier("Client anonyme", "Prenom", marqueur);

        mockMvc.perform(get("/api/hotline/recherche").param("nom", marqueur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fiches.length()").value(1));
    }

    @Test
    void parNom_trouveLesAppelsSansFiche() throws Exception {
        String marqueur = "ZZSANSFICHE" + System.nanoTime();
        newPreProspection("Appelant " + marqueur);

        mockMvc.perform(get("/api/hotline/recherche").param("nom", marqueur))
                .andExpect(status().isOk())
                // Personne n'a de fiche : `connu` reste false, mais l'appel est retrouvé.
                .andExpect(jsonPath("$.connu").value(false))
                .andExpect(jsonPath("$.appelsSansFiche.length()").value(1));
    }

    @Test
    void parNom_inconnu_repond200AvecListesVides() throws Exception {
        mockMvc.perform(get("/api/hotline/recherche").param("nom", "ZZINTROUVABLE" + System.nanoTime()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connu").value(false))
                .andExpect(jsonPath("$.fiches.length()").value(0))
                .andExpect(jsonPath("$.tronque").value(false));
    }

    // --- Plafond ---

    /**
     * Le plafond n'est pas théorique : « MOHAMED » remonte 555 fiches en base réelle, et chaque
     * fiche coûte cinq requêtes.
     */
    @Test
    void parNom_auDelaDuPlafond_tronqueEtLimiteLesFiches() throws Exception {
        String marqueur = "ZZPLAFOND" + System.nanoTime();
        for (int i = 0; i < HotlineService.FICHES_MAX_PAR_NOM + 3; i++) {
            newTier("Homonyme " + i + " " + marqueur, "Homonyme" + i, marqueur);
        }

        mockMvc.perform(get("/api/hotline/recherche").param("nom", marqueur))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tronque").value(true))
                .andExpect(jsonPath("$.fiches.length()").value(HotlineService.FICHES_MAX_PAR_NOM));
    }

    // --- Contrat d'entrée ---

    @Test
    void aucunCritere_retourne400() throws Exception {
        mockMvc.perform(get("/api/hotline/recherche"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lesDeuxCriteres_retourne400() throws Exception {
        mockMvc.perform(get("/api/hotline/recherche")
                        .param("telephone", "0612345678").param("nom", "alami"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nomTropCourt_retourne400() throws Exception {
        mockMvc.perform(get("/api/hotline/recherche").param("nom", "a"))
                .andExpect(status().isBadRequest());
    }

    // --- Non-régression du chemin téléphone ---

    @Test
    void parTelephone_resteInchangeEtRenseigneLeCritere() throws Exception {
        CfgTier tier = newTier("Amina TELTEST", "Amina", "TELTEST");
        tier.setMobile("+212 6 55 44 33 22");
        tierRepository.save(tier);

        mockMvc.perform(get("/api/hotline/recherche").param("telephone", "0655443322"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.critere").value("TELEPHONE"))
                .andExpect(jsonPath("$.telephoneNormalise").value("655443322"))
                .andExpect(jsonPath("$.tronque").value(false))
                .andExpect(jsonPath("$.nomRecherche").doesNotExist())
                .andExpect(jsonPath("$.connu").value(true));
    }
}
