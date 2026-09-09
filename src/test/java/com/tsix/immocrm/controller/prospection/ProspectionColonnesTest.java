package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sélection de colonnes de {@code GET /api/prospections} — voir
 * {@code instructions/FEATURE-selection-colonnes-listes.md}.
 *
 * <p>Le point le plus important tenu ici est le <b>défaut</b> : sans {@code ?colonnes=}, la
 * réponse doit être exactement celle d'avant ce lot. C'est ce qui rend le lot additif, et c'est
 * aussi ce qui est le plus facile à casser — il a suffi d'ajouter 14 composants au record pour
 * que 14 clés à {@code null} apparaissent partout tant que la sélection par défaut n'était pas
 * réellement appliquée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionColonnesTest {

    /** {@code StrEntity.PKey} de « Prospection vente » — semé par {@code ci/schema/050_views.sql}. */
    private static final UUID ENTITE_PROSPECTION =
            UUID.fromString("e7c73f70-1111-40d9-aa78-a2825c42b601");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    /**
     * Prospection visible de {@code GET /api/prospections}, et elle seule.
     *
     * <p>Deux conditions, faciles à manquer : la liste <b>exclut</b> les prospections sans
     * prospect ni commercial (ce sont les pré-prospections), d'où le tiers rattaché ; et
     * {@code StrEntityId} doit être posé, sans quoi la ligne n'existe dans aucune vue et les
     * colonnes d'activité resteraient nulles.
     *
     * <p>Le code retourné sert de {@code ?search=} pour que {@code content[0]} soit
     * déterministe — la base de test est partagée entre les classes.
     */
    private String prospectionVisible() {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("COL-TIER-" + UUID.randomUUID());
        prospect.setFirstName("Colonne");
        prospect.setLastName("TEST");
        tierRepository.save(prospect);

        String code = "PSN-COL-" + UUID.randomUUID();
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setDocDate(LocalDateTime.now());
        p.setStrEntityId(ENTITE_PROSPECTION);
        p.setCfgProspect(prospect);
        // Un budget non nul, alors qu'il est mort sur 14 110 des 14 114 lignes réelles : c'est
        // ce qui rend le test du défaut probant — la colonne est omise bien qu'elle ait une
        // valeur, et non parce qu'elle est vide.
        p.setPrice(new BigDecimal("1350000"));
        prospectionRepository.saveAndFlush(p);
        return code;
    }

    @Test
    void sansParametre_laReponseEstCelleDavant() throws Exception {
        String code = prospectionVisible();

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                // Colonnes historiques : présentes.
                .andExpect(jsonPath("$.content[0].code").exists())
                .andExpect(jsonPath("$.content[0].prospectNom").exists())
                // Colonnes d'activité : absentes, pas nulles. Une clé à null signifierait que la
                // vue a été interrogée pour rien, ou que la sélection par défaut ne s'applique pas.
                .andExpect(jsonPath("$.content[0].joursInactivite").doesNotExist())
                .andExpect(jsonPath("$.content[0].nbInteractions").doesNotExist())
                .andExpect(jsonPath("$.content[0].dossierCode").doesNotExist());
    }

    /**
     * Budget et surface sont mesurés morts (4 lignes sur 14 114 pour `Price`, 0 pour `Area`) et
     * valent {@code 0} plutôt que {@code null} : les servir par défaut faisait afficher
     * « 0,00 DH » de budget sur tout le portefeuille. Ils restent demandables — ce test tient les
     * deux moitiés de la règle.
     */
    @Test
    void budgetEtSurface_horsDefautMaisToujoursDemandables() throws Exception {
        String code = prospectionVisible();

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].prix").doesNotExist())
                .andExpect(jsonPath("$.content[0].surface").doesNotExist());

        mockMvc.perform(get("/api/prospections").param("colonnes", "code,prix").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].prix").value(1350000));
    }

    @Test
    void avecColonnes_neRenvoieQueCellesDemandees() throws Exception {
        String code = prospectionVisible();

        mockMvc.perform(get("/api/prospections")
                        .param("colonnes", "code,joursInactivite")
                        .param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").exists())
                .andExpect(jsonPath("$.content[0].joursInactivite").exists())
                // Non demandées : absentes, même si elles ont une valeur.
                .andExpect(jsonPath("$.content[0].prospectNom").doesNotExist())
                .andExpect(jsonPath("$.content[0].telephone").doesNotExist());
    }

    /** {@code id} n'est jamais retirable : sans lui le front ne peut plus ouvrir la fiche. */
    @Test
    void idEstToujoursRenvoye_memeNonDemande() throws Exception {
        String code = prospectionVisible();

        mockMvc.perform(get("/api/prospections").param("colonnes", "code").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].code").exists());
    }

    /** Le paramètre répété doit valoir la liste séparée par des virgules. */
    @Test
    void parametreRepete_equivautALaListeSepareeParVirgules() throws Exception {
        String code = prospectionVisible();

        mockMvc.perform(get("/api/prospections")
                        .param("colonnes", "code")
                        .param("colonnes", "nbInteractions")
                        .param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").exists())
                .andExpect(jsonPath("$.content[0].nbInteractions").exists())
                .andExpect(jsonPath("$.content[0].prospectNom").doesNotExist());
    }

    /**
     * Un nom hors catalogue est une erreur d'appel, pas une colonne vide : 400, et le corps porte
     * de quoi se corriger sans ouvrir la doc.
     */
    @Test
    void colonneInconnue_renvoie400EtLaListeDesNomsValides() throws Exception {
        mockMvc.perform(get("/api/prospections").param("colonnes", "code,joursDinactivite"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.colonne").value("joursDinactivite"))
                .andExpect(jsonPath("$.colonnesValides").isArray())
                .andExpect(jsonPath("$.colonnesValides[?(@ == 'joursInactivite')]").exists());
    }

    /**
     * Le tri reste appliqué sur {@code ComProspection}, qui ne porte pas les colonnes d'activité.
     * Ce test constate le comportement réel plutôt que de le supposer — une limite documentée
     * doit l'être avec le bon code de retour.
     */
    @Test
    void triSurUneColonneDactivite_quelComportement() throws Exception {
        String code = prospectionVisible();

        mockMvc.perform(get("/api/prospections")
                        .param("colonnes", "code,joursInactivite")
                        .param("sort", "joursInactivite,desc")
                        .param("search", code))
                .andExpect(status().isBadRequest());
    }

    @Test
    void referentiel_publieLeCatalogueDesColonnes() throws Exception {
        mockMvc.perform(get("/api/referentiels/colonnes/prospections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom == 'joursInactivite')].groupe").value("ACTIVITE"))
                .andExpect(jsonPath("$[?(@.nom == 'joursInactivite')].type").value("NOMBRE"))
                .andExpect(jsonPath("$[?(@.nom == 'joursInactivite')].parDefaut").value(false))
                // Les colonnes historiques sont, elles, dans la sélection par défaut.
                .andExpect(jsonPath("$[?(@.nom == 'code')].parDefaut").value(true))
                .andExpect(jsonPath("$[?(@.nom == 'code')].libelle").value("Code"));
    }

    /**
     * La sélection ne doit pas fuir sur les autres réponses qui embarquent le même DTO. Sans le
     * filtre par défaut enregistré globalement, cet appel tomberait même en 500 : Jackson refuse
     * un {@code @JsonFilter} dont l'identifiant n'est enregistré nulle part.
     */
    @Test
    void preProspections_neSontPasAffecteesParLaSelection() throws Exception {
        mockMvc.perform(get("/api/pre-prospections").param("size", "5"))
                .andExpect(status().isOk());
    }
}
