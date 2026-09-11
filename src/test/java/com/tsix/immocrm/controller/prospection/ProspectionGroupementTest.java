package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
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
 * {@code GET /api/prospections/groupes} et {@code GET /api/pre-prospections/groupes} — voir
 * {@code instructions/FEATURE-groupement-listes.md}.
 *
 * <p>Ne rejoue pas ce que {@code DossierGroupementTest} couvre déjà (granularité des dates,
 * libellé des tiers, substitution du groupe nul) : le moteur est partagé. Ce qui est testé ici,
 * c'est ce qui est <strong>propre à ce catalogue</strong> — le discriminant
 * prospection/pré-prospection, et le refus des colonnes servies par la vue légataire.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionGroupementTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private ComProspectionKindRepository kindRepository;
    @Autowired
    private ComProspectionOriginRepository originRepository;
    @Autowired
    private CfgTierRepository tierRepository;

    private ComProspectionKind newEtape(String code) {
        ComProspectionKind kind = new ComProspectionKind();
        kind.setId(UUID.randomUUID());
        kind.setCode(code);
        return kindRepository.save(kind);
    }

    private ComProspectionOrigin newOrigine(String code) {
        ComProspectionOrigin origin = new ComProspectionOrigin();
        origin.setId(UUID.randomUUID());
        origin.setCode(code);
        return originRepository.save(origin);
    }

    private CfgTier newProspect(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setLastName("Nom-" + code);
        return tierRepository.save(tier);
    }

    /** Qualifiée : porte un prospect, donc relève de {@code /api/prospections}. */
    private ComProspection newProspection(String code, ComProspectionKind etape, CfgTier prospect) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setComProspectionKind(etape);
        p.setCfgProspect(prospect);
        return prospectionRepository.save(p);
    }

    /** Brute : ni prospect ni commercial, donc relève de {@code /api/pre-prospections}. */
    private ComProspection newPreProspection(String code, ComProspectionOrigin origine) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setComProspectionOrigin(origine);
        return prospectionRepository.save(p);
    }

    // ------------------------------------------------------------------
    // Prospections
    // ------------------------------------------------------------------

    @Test
    void groupeParEtape_rendUnGroupeParEtape() throws Exception {
        ComProspectionKind contact = newEtape("GRP-P-CONTACT");
        ComProspectionKind visite = newEtape("GRP-P-VISITE");
        CfgTier prospect = newProspect("TIER-GRP-P");

        newProspection("GRPP-1", contact, prospect);
        newProspection("GRPP-2", contact, prospect);
        newProspection("GRPP-3", visite, prospect);

        mockMvc.perform(get("/api/prospections/groupes")
                        .param("groupBy", "etape")
                        .param("search", "GRPP-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-P-CONTACT')].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-P-VISITE')].nombre").value(1));
    }

    @Test
    void sommeDesEffectifs_egaleLeTotalDeLaListe() throws Exception {
        ComProspectionKind etape = newEtape("GRP-P-INV");
        CfgTier prospect = newProspect("TIER-GRP-P-INV");
        newProspection("GRPQ-1", etape, prospect);
        newProspection("GRPQ-2", etape, prospect);
        // Sans étape : le groupe à clé nulle, que COUNT(DISTINCT) ne compte pas de lui-même.
        newProspection("GRPQ-3", null, prospect);

        mockMvc.perform(get("/api/prospections").param("search", "GRPQ-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/prospections/groupes")
                        .param("groupBy", "etape")
                        .param("search", "GRPQ-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-P-INV')].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle == null)].nombre").value(1));
    }

    @Test
    void depliage_rendExactementLesLignesComptees() throws Exception {
        ComProspectionKind vise = newEtape("GRP-P-DEPLI-VISE");
        ComProspectionKind autre = newEtape("GRP-P-DEPLI-AUTRE");
        CfgTier prospect = newProspect("TIER-GRP-P-DEPLI");
        ComProspection attendue = newProspection("GRPR-1", vise, prospect);
        ComProspection exclue = newProspection("GRPR-2", autre, prospect);

        mockMvc.perform(get("/api/prospections")
                        .param("search", "GRPR-")
                        .param("groupeColonne", "etape")
                        .param("groupeValeur", "GRP-P-DEPLI-VISE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[?(@.id=='" + attendue.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + exclue.getId() + "')]").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Ce qui est propre à ce catalogue : les colonnes de la vue légataire
    // ------------------------------------------------------------------

    /**
     * {@code joursInactivite} est {@code Source.VUE} : sa valeur vient de
     * {@code ComSaleProspectionView}, chargée par une requête distincte. Elle n'existe pas dans le
     * graphe d'entités de {@code ComProspection} et ne peut donc pas servir de clé de
     * {@code GROUP BY}. Le refus doit être explicite — la laisser passer produirait une
     * {@code SQLGrammarException} en 500 au lieu d'un 400 lisible.
     */
    @Test
    void groupBy_surColonneDeLaVue_rend400() throws Exception {
        mockMvc.perform(get("/api/prospections/groupes").param("groupBy", "joursInactivite"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void groupBy_surColonneDeConversion_rend400() throws Exception {
        mockMvc.perform(get("/api/prospections/groupes").param("groupBy", "statutDossier"))
                .andExpect(status().isBadRequest());
    }

    /**
     * {@code typeProspection} vient de {@code ComProspection.StrEntityId}, un {@code UUID} brut et
     * non une association : il n'y a rien à joindre vers {@code StrEntity} pour en tirer un
     * libellé. Exclue pour une raison différente des colonnes de vue, même refus.
     */
    @Test
    void groupBy_surTypeProspection_rend400() throws Exception {
        mockMvc.perform(get("/api/prospections/groupes").param("groupBy", "typeProspection"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Pré-prospections : même moteur, discriminant inverse
    // ------------------------------------------------------------------

    @Test
    void preProspections_groupeParOrigine() throws Exception {
        ComProspectionOrigin facebook = newOrigine("GRP-PP-FACEBOOK");
        ComProspectionOrigin siteWeb = newOrigine("GRP-PP-SITE");

        newPreProspection("GRPS-1", facebook);
        newPreProspection("GRPS-2", facebook);
        newPreProspection("GRPS-3", siteWeb);

        mockMvc.perform(get("/api/pre-prospections/groupes")
                        .param("groupBy", "origine")
                        .param("search", "GRPS-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-PP-FACEBOOK')].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-PP-SITE')].nombre").value(1));
    }

    /**
     * Le discriminant doit valoir des deux côtés : une prospection qualifiée ne doit pas être
     * comptée dans les groupes des pré-prospections, sinon les effectifs annoncent des lignes que
     * le dépliage ne rendra jamais.
     */
    @Test
    void preProspections_neComptentPasLesProspectionsQualifiees() throws Exception {
        ComProspectionOrigin origine = newOrigine("GRP-PP-DISCRIMINANT");
        newPreProspection("GRPT-1", origine);

        ComProspection qualifiee = newProspection("GRPT-2", null, newProspect("TIER-GRPT"));
        qualifiee.setComProspectionOrigin(origine);
        prospectionRepository.save(qualifiee);

        mockMvc.perform(get("/api/pre-prospections/groupes")
                        .param("groupBy", "origine")
                        .param("search", "GRPT-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-PP-DISCRIMINANT')].nombre").value(1));

        mockMvc.perform(get("/api/prospections/groupes")
                        .param("groupBy", "origine")
                        .param("search", "GRPT-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='GRP-PP-DISCRIMINANT')].nombre").value(1));
    }

    @Test
    void preProspections_depliage() throws Exception {
        ComProspectionOrigin visee = newOrigine("GRP-PP-DEPLI-VISEE");
        ComProspectionOrigin autre = newOrigine("GRP-PP-DEPLI-AUTRE");
        ComProspection attendue = newPreProspection("GRPU-1", visee);
        ComProspection exclue = newPreProspection("GRPU-2", autre);

        mockMvc.perform(get("/api/pre-prospections")
                        .param("search", "GRPU-")
                        .param("groupeColonne", "origine")
                        .param("groupeValeur", "GRP-PP-DEPLI-VISEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[?(@.id=='" + attendue.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + exclue.getId() + "')]").doesNotExist());
    }
}
