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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exposition du type de document légataire ({@code StrEntity}) — voir
 * {@code instructions/FEATURE-strentity-prospection.md}.
 *
 * <p><b>Ce lot est additif et doit le rester.</b> Il expose le discriminant réel du logiciel de
 * bureau sans reclasser aucune ligne : les 830 pré-prospections que l'heuristique
 * ({@code CfgProspectId}/{@code CfgCommercialId}) laisse dans la liste de vente y restent. Le test
 * {@link #listeDeVente_contientEncoreLesPreProspections()} fige précisément cet écart — s'il se met
 * à échouer, c'est que quelqu'un a corrigé le bug #44 sans le décider.
 *
 * <p><b>Piège de sérialisation.</b> Sur la fiche, {@code jsonPath(...).exists()} <em>échoue</em>
 * sur une valeur {@code null} : Spring traite « clé présente à null » comme absente. Les
 * assertions de nullité passent donc par {@code doesNotExist()}, et non par une négation
 * d'{@code exists()}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionTypeStrEntityTest {

    /** {@code StrEntity.PKey} de « Prospection vente » — semé par {@code ci/schema/050_views.sql}. */
    private static final UUID ENTITE_PROSPECTION =
            UUID.fromString("e7c73f70-1111-40d9-aa78-a2825c42b601");

    /** {@code StrEntity.PKey} de « Pré-Prospection ». Même {@code PKey} qu'en base réelle. */
    private static final UUID ENTITE_PRE_PROSPECTION =
            UUID.fromString("b92df3d8-158c-4e1e-827a-70feb172a264");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    /**
     * Prospection visible de {@code GET /api/prospections}, quel que soit son type légataire.
     *
     * <p>Le prospect rattaché reproduit le cas des ~830 : une ligne de type
     * {@code ComPreProspection} qui porte déjà un prospect. L'ancienne heuristique la classait en
     * vente ; le discriminant {@code StrEntity} la garde en pré-prospection (décision #44).
     *
     * <p>Le code porté par la ligne sert de {@code ?search=} pour que {@code content[0]} soit
     * déterministe — la base de test est partagée entre les classes.
     *
     * @param entite valeur de {@code StrEntityId}, ou {@code null} pour reproduire les 4 lignes
     *               réelles qui n'en portent pas.
     */
    private ComProspection prospectionVisible(UUID entite) {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("TYPE-TIER-" + UUID.randomUUID());
        prospect.setFirstName("Type");
        prospect.setLastName("TEST");
        tierRepository.save(prospect);

        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-TYPE-" + UUID.randomUUID());
        p.setDocDate(LocalDateTime.now());
        p.setStrEntityId(entite);
        p.setCfgProspect(prospect);
        return prospectionRepository.saveAndFlush(p);
    }

    // ------------------------------------------------------------------
    // Fiche
    // ------------------------------------------------------------------

    @Test
    void fiche_exposeLeTypeDeDocument() throws Exception {
        UUID id = prospectionVisible(ENTITE_PROSPECTION).getId();

        mockMvc.perform(get("/api/prospections/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeProspectionId").value(ENTITE_PROSPECTION.toString()))
                // Le code porte le sens, le libellé n'est qu'un affichage.
                .andExpect(jsonPath("$.typeProspectionCode").value("ComProspection"))
                .andExpect(jsonPath("$.typeProspection").value("Prospection vente"));
    }

    @Test
    void fiche_distingueLaPreProspection() throws Exception {
        UUID id = prospectionVisible(ENTITE_PRE_PROSPECTION).getId();

        mockMvc.perform(get("/api/prospections/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeProspectionCode").value("ComPreProspection"));
    }

    /**
     * Les 4 lignes réelles sans {@code StrEntityId} : invisibles du logiciel de bureau, dont toutes
     * les vues font un {@code INNER JOIN} sur {@code StrEntity}. La fiche doit répondre 200 avec
     * les trois champs à {@code null} — surtout pas échouer sur la résolution du référentiel.
     */
    @Test
    void fiche_sansTypeRepondSansEchouer() throws Exception {
        UUID id = prospectionVisible(null).getId();

        mockMvc.perform(get("/api/prospections/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.typeProspectionId").doesNotExist())
                .andExpect(jsonPath("$.typeProspectionCode").doesNotExist())
                .andExpect(jsonPath("$.typeProspection").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Liste — sélection de colonnes
    // ------------------------------------------------------------------

    /** Le défaut ne bouge pas : trois clés de plus au record, zéro clé de plus dans la réponse. */
    @Test
    void liste_sansSelection_nExposePasLeType() throws Exception {
        String code = prospectionVisible(ENTITE_PROSPECTION).getCode();

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].code").exists())
                .andExpect(jsonPath("$.content[0].typeProspectionId").doesNotExist())
                .andExpect(jsonPath("$.content[0].typeProspectionCode").doesNotExist())
                .andExpect(jsonPath("$.content[0].typeProspection").doesNotExist());
    }

    @Test
    void liste_avecSelection_exposeLeType() throws Exception {
        String code = prospectionVisible(ENTITE_PROSPECTION).getCode();

        mockMvc.perform(get("/api/prospections")
                        .param("colonnes", "code,typeProspectionId,typeProspectionCode,typeProspection")
                        .param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].typeProspectionId").value(ENTITE_PROSPECTION.toString()))
                .andExpect(jsonPath("$.content[0].typeProspectionCode").value("ComProspection"))
                .andExpect(jsonPath("$.content[0].typeProspection").value("Prospection vente"))
                // Le type vient des entités, pas de la vue : le demander ne doit pas ramener le
                // bloc d'activité, dont le coût est celui d'une requête supplémentaire.
                .andExpect(jsonPath("$.content[0].joursInactivite").doesNotExist());
    }

    /**
     * Demander uniquement le libellé ne doit pas faire apparaître les deux autres champs du
     * groupe : la sélection est champ par champ, pas par bloc.
     */
    @Test
    void liste_selectionPartielle_nExposeQueLeChampDemande() throws Exception {
        String code = prospectionVisible(ENTITE_PROSPECTION).getCode();

        mockMvc.perform(get("/api/prospections")
                        .param("colonnes", "code,typeProspectionCode")
                        .param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].typeProspectionCode").value("ComProspection"))
                .andExpect(jsonPath("$.content[0].typeProspectionId").doesNotExist())
                .andExpect(jsonPath("$.content[0].typeProspection").doesNotExist());
    }

    // ------------------------------------------------------------------
    // Liste — filtre, et l'écart qu'il rend visible
    // ------------------------------------------------------------------

    /**
     * <b>Décision #44, appliquée.</b> Une pré-prospection OMRAN qui porte déjà un prospect — le cas
     * des ~830 lignes réelles — quitte la liste de vente pour celle des pré-prospections. Voir
     * {@code FEATURE-creation-pre-prospection.md} §4.
     */
    @Test
    void preProspectionAvecProspect_resteDansLaListeDesPreProspections() throws Exception {
        String code = prospectionVisible(ENTITE_PRE_PROSPECTION).getCode();

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/api/pre-prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].prospectNom").value("Type TEST"));
    }

    @Test
    void filtre_typePreProspection_neRendPlusRienSurLaListeDeVente() throws Exception {
        String code = prospectionVisible(ENTITE_PRE_PROSPECTION).getCode();

        mockMvc.perform(get("/api/prospections")
                        .param("typeProspection", "ComPreProspection")
                        .param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    /**
     * Une ligne sans {@code StrEntityId} n'a aucun type : elle sort dès que le filtre est actif,
     * quel que soit le code demandé. {@code NULL IN (…)} est inconnu en SQL, et c'est le
     * comportement voulu — pas un effet de bord à corriger.
     */
    @Test
    void filtre_ecarteLesLignesSansType() throws Exception {
        String code = prospectionVisible(null).getCode();

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/prospections")
                        .param("typeProspection", "ComProspection")
                        .param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    /** Un code inconnu ne provoque pas de 400 : il ne correspond à rien, le résultat est vide. */
    @Test
    void filtre_codeInconnuRendUnResultatVideSansErreur() throws Exception {
        String code = prospectionVisible(ENTITE_PROSPECTION).getCode();

        mockMvc.perform(get("/api/prospections")
                        .param("typeProspection", "EntiteQuiNexistePas")
                        .param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    /** Le catalogue alimente le sélecteur du front : les trois colonnes doivent y figurer. */
    @Test
    void catalogue_publieLesTroisColonnes() throws Exception {
        mockMvc.perform(get("/api/referentiels/colonnes/prospections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom == 'typeProspectionCode')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.nom == 'typeProspectionId')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.nom == 'typeProspection')]").isNotEmpty());
    }
}
