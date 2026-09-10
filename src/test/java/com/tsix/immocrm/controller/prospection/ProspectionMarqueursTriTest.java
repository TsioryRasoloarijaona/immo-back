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
 * Marqueurs de tri et pointeur de doublon sur la fiche — voir
 * {@code instructions/FEATURE-marqueurs-tri-pre-prospection.md}.
 *
 * <p>Deux propriétés sont verrouillées ici, et ce sont les deux qui peuvent faire mentir l'API :
 *
 * <ol>
 *   <li><b>Le brut et le dérivé ne disent pas la même chose.</b> {@code noteQualification} porte
 *       toujours le texte ; {@code marqueurTri} ne vaut quelque chose que si ce texte est un
 *       marqueur reconnu. 1 239 lignes réelles ont l'un sans l'autre — si les deux se mettaient à
 *       varier ensemble, c'est que la reconnaissance serait devenue trop généreuse.</li>
 *   <li><b>Un pointeur non résolu reste affichable.</b> {@code CfgTier.Code} n'étant pas unique,
 *       la résolution peut échouer ; {@code doublonDeCode} doit survivre à cet échec, sinon
 *       l'information disparaît silencieusement de la fiche.</li>
 * </ol>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionMarqueursTriTest {

    /** {@code StrEntity.PKey} de « Pré-Prospection » — la population où le tri est utilisé. */
    private static final UUID ENTITE_PRE_PROSPECTION =
            UUID.fromString("b92df3d8-158c-4e1e-827a-70feb172a264");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    private static String suffixe() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private ComProspection preProspection(String tempField1, String tempField10) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-TRI-" + suffixe());
        p.setDocDate(LocalDateTime.now());
        p.setStrEntityId(ENTITE_PRE_PROSPECTION);
        p.setTempField1(tempField1);
        p.setTempField10(tempField10);
        return prospectionRepository.saveAndFlush(p);
    }

    private CfgTier tier(String code, String nom) {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode(code);
        t.setDescription(nom);
        return tierRepository.saveAndFlush(t);
    }

    @Test
    void doublon_exposeLeMarqueurEtResoutLeTiersPointe() throws Exception {
        CfgTier original = tier("TRI-" + suffixe(), "Tiers Original");
        ComProspection p = preProspection("Doublon", original.getCode());

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noteQualification").value("Doublon"))
                .andExpect(jsonPath("$.marqueurTri").value("DOUBLON"))
                .andExpect(jsonPath("$.doublonDeCode").value(original.getCode()))
                .andExpect(jsonPath("$.doublonDeId").value(original.getId().toString()))
                .andExpect(jsonPath("$.doublonDeNom").value("Tiers Original"));
    }

    /** La casse et les accents varient en base ; le marqueur normalisé ne doit pas. */
    @Test
    void marqueurTri_estNormalise() throws Exception {
        ComProspection p = preProspection("  TRAITÉ  ", null);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noteQualification").value("TRAITÉ"))
                .andExpect(jsonPath("$.marqueurTri").value("TRAITE"));
    }

    /**
     * Le cas majoritaire, et celui qui compte : côté prospection la colonne porte la typologie
     * recherchée. Le texte doit être servi, le marqueur doit rester absent.
     */
    @Test
    void texteLibreDeQualification_estServiSansEtreEtiquete() throws Exception {
        ComProspection p = preProspection("salon_+_2_chambres", null);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noteQualification").value("salon_+_2_chambres"))
                .andExpect(jsonPath("$.marqueurTri").doesNotExist());
    }

    /**
     * {@code CfgTier.Code} n'est pas unique (14 516 tiers pour 14 504 codes réels). Sur une
     * correspondance multiple, désigner un tiers reviendrait à en choisir un au hasard : la
     * résolution abandonne, mais **le code brut reste servi** pour que le front puisse au moins
     * l'afficher.
     */
    @Test
    void codeAmbigu_neResoutPasMaisResteAffichable() throws Exception {
        String codePartage = "TRI-DUP-" + suffixe();
        tier(codePartage, "Premier homonyme");
        tier(codePartage, "Second homonyme");
        ComProspection p = preProspection("Doublon", codePartage);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marqueurTri").value("DOUBLON"))
                .andExpect(jsonPath("$.doublonDeCode").value(codePartage))
                .andExpect(jsonPath("$.doublonDeId").doesNotExist())
                .andExpect(jsonPath("$.doublonDeNom").doesNotExist());
    }

    @Test
    void codeInconnu_neResoutPasMaisResteAffichable() throws Exception {
        String codeOrphelin = "TRI-ABSENT-" + suffixe();
        ComProspection p = preProspection("Doublon", codeOrphelin);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doublonDeCode").value(codeOrphelin))
                .andExpect(jsonPath("$.doublonDeId").doesNotExist());
    }

    /** Sans champ libre, les cinq champs sont absents — jamais une chaîne vide. */
    @Test
    void sansChampLibre_aucunDesCinqChampsNEstServi() throws Exception {
        ComProspection p = preProspection("   ", "");

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.noteQualification").doesNotExist())
                .andExpect(jsonPath("$.marqueurTri").doesNotExist())
                .andExpect(jsonPath("$.doublonDeCode").doesNotExist())
                .andExpect(jsonPath("$.doublonDeId").doesNotExist());
    }

    /**
     * Le marqueur est indépendant de l'étape : en base réelle, 795 des 798 leads marqués doublon
     * ou spam sont encore à « 1 A TRAITER ». L'API expose le marqueur **sans** filtrer ni changer
     * l'étape — la fiche est servie normalement, en 200.
     */
    @Test
    void spam_estServiNormalement_lEndpointNeFiltreRien() throws Exception {
        ComProspection p = preProspection("spam", null);

        mockMvc.perform(get("/api/prospections/{id}", p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p.getId().toString()))
                .andExpect(jsonPath("$.marqueurTri").value("SPAM"));
    }
}
