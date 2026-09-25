package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règle 2 de {@code instructions/FEATURE-doublons-prospect-projet.md} : une seule prospection
 * <b>ouverte</b> par couple prospect × projet, contrôlée à la création, à la transformation, au
 * changement de tranche et au rattachement d'un prospect existant.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionUniciteProjetTest {

    /** {@code StrEntity} « Prospection vente », identique dans les trois bases du projet. */
    private static final UUID TYPE_VENTE = UUID.fromString("E7C73F70-1111-40D9-AA78-A2825C42B601");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private CfgTierTypeRepository tierTypeRepository;
    @Autowired
    private CfgProjectRepository projectRepository;
    @Autowired
    private CfgTrancheRepository trancheRepository;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private ComProspectionOriginRepository prospectionOriginRepository;
    @Autowired
    private ComActionMessageTypeRepository actionMessageTypeRepository;
    @Autowired
    private ComProspectionKindRepository prospectionKindRepository;

    private CfgTier prospect;
    private CfgTier commercial;
    private CfgProject projetA;
    private CfgTranche trancheA1;
    private CfgTranche trancheA2;
    private CfgTranche trancheB1;
    private ComProspectionOrigin origine;
    private ComActionMessageType typeContact;
    private ComProspectionKind nature;

    @BeforeEach
    void setUp() {
        CfgTierType typePro = tierTypeRepository.findByCode("PRO").orElseGet(() -> {
            CfgTierType t = new CfgTierType();
            t.setId(UUID.randomUUID());
            t.setCode("PRO");
            t.setDescription("PROSPECT");
            return tierTypeRepository.save(t);
        });

        prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("PRO-UPP-" + suffixe());
        prospect.setFirstName("Yassine");
        prospect.setLastName("MOUNIR " + suffixe());
        prospect.setCfgTierType(typePro);
        prospect = tierRepository.save(prospect);

        commercial = new CfgTier();
        commercial.setId(UUID.randomUUID());
        commercial.setCode("COMM-UPP-" + suffixe());
        commercial.setLastName("Othman DIOURI");
        commercial = tierRepository.save(commercial);

        projetA = projet("APF");
        CfgProject projetB = projet("ART");
        trancheA1 = tranche(projetA, "A1");
        trancheA2 = tranche(projetA, "A2");
        trancheB1 = tranche(projetB, "B1");

        origine = new ComProspectionOrigin();
        origine.setId(UUID.randomUUID());
        origine.setCode("ORI-UPP-" + suffixe());
        origine.setDescription("Facebook");
        prospectionOriginRepository.save(origine);

        typeContact = new ComActionMessageType();
        typeContact.setId(UUID.randomUUID());
        typeContact.setCode("TYP-UPP-" + suffixe());
        typeContact.setDescription("Message entrant");
        actionMessageTypeRepository.save(typeContact);

        nature = new ComProspectionKind();
        nature.setId(UUID.randomUUID());
        nature.setCode("NAT-UPP-" + suffixe());
        nature.setDescription("3 QUALIFIÉ");
        prospectionKindRepository.save(nature);
    }

    private static String suffixe() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private CfgProject projet(String code) {
        CfgProject p = new CfgProject();
        p.setId(UUID.randomUUID());
        p.setCode(code + "-UPP-" + suffixe());
        return projectRepository.save(p);
    }

    private CfgTranche tranche(CfgProject projet, String code) {
        CfgTranche t = new CfgTranche();
        t.setId(UUID.randomUUID());
        t.setCode("TR-UPP-" + code + "-" + suffixe());
        t.setCfgProject(projet);
        // Reprendre l'instance renvoyée : l'id étant assigné, save() passe par merge().
        return trancheRepository.save(t);
    }

    /** Une prospection existante ; {@code fermee} null = colonne NULL, ce qui n'est pas « archivée ». */
    private ComProspection existante(CfgTier prospect, CfgTranche tranche, Boolean fermee, boolean preProspection) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("UPP-" + suffixe());
        p.setDocDate(LocalDateTime.now().minusDays(1));
        p.setCfgProspect(prospect);
        p.setStkHierarchy(tranche);
        p.setIsClosed(fermee);
        p.setStrEntityId(preProspection ? DiscriminantPreProspection.ID_PRE_PROSPECTION : TYPE_VENTE);
        p.setComProspectionKind(nature);
        // Une pré-prospection n'a pas encore de commercial : c'est `transformer` qui le pose.
        p.setCfgCommercial(preProspection ? null : commercial);
        return prospectionRepository.save(p);
    }

    private String corpsCreation(CfgTranche tranche) {
        return "{\"prospectId\":\"" + prospect.getId() + "\""
                + ",\"trancheId\":\"" + tranche.getId() + "\""
                + ",\"origineId\":\"" + origine.getId() + "\""
                + ",\"typeContactId\":\"" + typeContact.getId() + "\""
                + ",\"etapeId\":\"" + nature.getId() + "\""
                + ",\"notes\":\"Intéressé par un T3\"}";
    }

    private ResultActions creer(CfgTranche tranche) throws Exception {
        return mockMvc.perform(post("/api/prospections").contentType(MediaType.APPLICATION_JSON)
                .content(corpsCreation(tranche)));
    }

    private ResultActions attendre409(ResultActions resultat, ComProspection existante) throws Exception {
        return resultat.andExpect(status().isConflict())
                .andExpect(jsonPath("$.prospectionExistante.id").value(existante.getId().toString()))
                .andExpect(jsonPath("$.prospectionExistante.code").value(existante.getCode()))
                .andExpect(jsonPath("$.prospectionExistante.projetCode").value(projetA.getCode()))
                .andExpect(jsonPath("$.prospectionExistante.trancheCode").value(existante.getStkHierarchy().getCode()))
                .andExpect(jsonPath("$.prospectionExistante.etapeLibelle").value("3 QUALIFIÉ"))
                .andExpect(jsonPath("$.prospectionExistante.commercialNom").value("Othman DIOURI"));
    }

    // ---------------------------------------------------------------- POST /api/prospections

    @Test
    void creer_409_siUneProspectionOuverteExisteSurLeMemeProjet_memeSurUneAutreTranche() throws Exception {
        ComProspection ouverte = existante(prospect, trancheA1, false, false);
        long avant = prospectionRepository.count();

        attendre409(creer(trancheA2), ouverte);
        assertThat(prospectionRepository.count()).as("rien n'est écrit sur un 409").isEqualTo(avant);
    }

    @Test
    void creer_409_siIsClosedNull_uneLigneNullNEstPasArchivee() throws Exception {
        ComProspection ouverte = existante(prospect, trancheA1, null, false);
        attendre409(creer(trancheA1), ouverte);
    }

    @Test
    void creer_201_siLaProspectionExistanteEstFermee() throws Exception {
        existante(prospect, trancheA1, true, false);
        creer(trancheA1).andExpect(status().isCreated());
    }

    @Test
    void creer_201_surUnAutreProjet() throws Exception {
        existante(prospect, trancheA1, false, false);
        creer(trancheB1).andExpect(status().isCreated());
    }

    @Test
    void creer_201_unePreProspectionNeCompteJamais() throws Exception {
        existante(prospect, trancheA1, false, true);
        creer(trancheA1).andExpect(status().isCreated());
    }

    @Test
    void creer_201_pourUnAutreProspect() throws Exception {
        CfgTier autre = new CfgTier();
        autre.setId(UUID.randomUUID());
        autre.setCode("PRO-UPP-" + suffixe());
        autre.setLastName("Autre " + suffixe());
        autre = tierRepository.save(autre);
        existante(autre, trancheA1, false, false);

        creer(trancheA1).andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- POST .../transformer

    private ResultActions transformer(ComProspection p, CfgTranche tranche) throws Exception {
        String corps = tranche == null
                ? "{\"commercialId\":\"" + commercial.getId() + "\"}"
                : "{\"commercialId\":\"" + commercial.getId() + "\",\"trancheId\":\"" + tranche.getId() + "\"}";
        return mockMvc.perform(post("/api/prospections/{id}/transformer", p.getId())
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    @Test
    void transformer_409_siLeProspectAUneProspectionOuverteSurLeProjetDeLaTrancheFournie() throws Exception {
        ComProspection ouverte = existante(prospect, trancheA1, false, false);
        ComProspection pre = existante(prospect, null, null, true);

        attendre409(transformer(pre, trancheA2), ouverte);
        assertThat(prospectionRepository.findById(pre.getId()).orElseThrow().getCfgCommercial())
                .as("la transformation n'a rien écrit").isNull();
    }

    @Test
    void transformer_409_siLaTrancheDejaPorteeEstSurCeProjet() throws Exception {
        ComProspection ouverte = existante(prospect, trancheA1, false, false);
        ComProspection pre = existante(prospect, trancheA1, null, true);

        attendre409(transformer(pre, null), ouverte);
    }

    @Test
    void transformer_200_laLigneElleMemeNeSeBloquePas() throws Exception {
        ComProspection pre = existante(prospect, trancheA1, null, true);

        transformer(pre, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialId").value(commercial.getId().toString()))
                .andExpect(jsonPath("$.typeProspectionCode").value("ComProspection"));
    }

    @Test
    void transformer_200_siLExistanteEstFermee() throws Exception {
        existante(prospect, trancheA1, true, false);
        ComProspection pre = existante(prospect, null, null, true);

        transformer(pre, trancheA1).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- PATCH .../tranche-principale

    private ResultActions changerTranche(ComProspection p, CfgTranche tranche) throws Exception {
        return mockMvc.perform(patch("/api/prospections/{id}/tranche-principale", p.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"trancheId\":\"" + tranche.getId() + "\"}"));
    }

    @Test
    void changerTranche_409_versUnProjetOuLeProspectAUneProspectionOuverte() throws Exception {
        ComProspection ouverte = existante(prospect, trancheA1, false, false);
        ComProspection surB = existante(prospect, trancheB1, false, false);

        attendre409(changerTranche(surB, trancheA2), ouverte);
        assertThat(prospectionRepository.findById(surB.getId()).orElseThrow().getStkHierarchy().getId())
                .isEqualTo(trancheB1.getId());
    }

    @Test
    void changerTranche_200_auSeinDuMemeProjet() throws Exception {
        ComProspection ouverte = existante(prospect, trancheA1, false, false);

        changerTranche(ouverte, trancheA2)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trancheId").value(trancheA2.getId().toString()));
    }

    @Test
    void changerTranche_200_versUnProjetLibre() throws Exception {
        ComProspection ouverte = existante(prospect, trancheA1, false, false);

        changerTranche(ouverte, trancheB1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trancheId").value(trancheB1.getId().toString()));
    }

    @Test
    void changerTranche_200_sansProspectLie() throws Exception {
        existante(prospect, trancheA1, false, false);
        ComProspection sansProspect = existante(null, trancheB1, false, false);

        changerTranche(sansProspect, trancheA2).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- POST .../lier-prospect

    private ResultActions lier(ComProspection pre) throws Exception {
        return mockMvc.perform(post("/api/pre-prospections/{id}/lier-prospect", pre.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"prospectId\":\"" + prospect.getId() + "\"}"));
    }

    @Test
    void lierProspect_409_siLaPreProspectionEstSurUnProjetOuLeProspectAUneProspectionOuverte() throws Exception {
        ComProspection ouverte = existante(prospect, trancheA1, false, false);
        ComProspection pre = existante(null, trancheA2, null, true);

        attendre409(lier(pre), ouverte);
        assertThat(prospectionRepository.findById(pre.getId()).orElseThrow().getCfgProspect()).isNull();
    }

    @Test
    void lierProspect_200_sansTranche_ouSurUnAutreProjet() throws Exception {
        existante(prospect, trancheA1, false, false);

        lier(existante(null, null, null, true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prospectId").value(prospect.getId().toString()));
        lier(existante(null, trancheB1, null, true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prospectId").value(prospect.getId().toString()));
    }
}
