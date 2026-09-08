package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
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
 * Contact et dernière alerte sur {@code GET /api/prospections} — voir
 * {@code instructions/FEATURE-prospection-contact-alerte.md}.
 *
 * <p>Deux points mesurés en base guident ces tests. Le contact a <b>deux sources</b> et aucune
 * ne suffit seule : sur 8 613 prospections ouvertes, 5 034 sont joignables par la fiche, 7 596
 * par le champ de premier contact, 8 591 par l'une ou l'autre. Et l'alerte vient
 * <b>exclusivement</b> des interactions — la colonne d'alerte de {@code ComProspection} est
 * morte, 0 ligne sur 8 613.
 *
 * <p>Chaque test filtre sur son propre {@code search} : la base de test partage ses tables avec
 * les fixtures des autres classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionListeContactAlerteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private ComActionMessageRepository actionMessageRepository;

    private CfgTier newProspect(String mobile, String phone, String email) {
        CfgTier t = new CfgTier();
        t.setId(UUID.randomUUID());
        t.setCode("PL-TIER-" + UUID.randomUUID());
        t.setFirstName("Contact");
        t.setLastName("TEST");
        t.setMobile(mobile);
        t.setPhone(phone);
        t.setEMail(email);
        return tierRepository.save(t);
    }

    private ComProspection newProspection(String code, CfgTier prospect, String tempTel, String tempEmail) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setCfgProspect(prospect);
        p.setTempTel(tempTel);
        p.setTempEMail(tempEmail);
        p.setDocDate(LocalDateTime.now());
        return prospectionRepository.save(p);
    }

    private ComActionMessage newInteractionAvecAlerte(ComProspection p, LocalDateTime docDate,
                                                      LocalDateTime dateAlerte, String message) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setComProspection(p);
        m.setDocDate(docDate);
        m.setAlertValidityDate(dateAlerte);
        m.setAlertMessage(message);
        m.setDescription("libelle genere");
        // saveAndFlush, pas save : la dernière alerte est résolue par une requête NATIVE, et
        // Hibernate ne connaît pas ses espaces de requête — il ne déclenche donc pas
        // l'auto-flush avant de l'exécuter. Sans ce flush, la ligne n'existe pas encore en base
        // au moment où la requête part et le test échoue en « No results for path ». Même piège
        // que celui rencontré sur les @Formula des pièces jointes.
        return actionMessageRepository.saveAndFlush(m);
    }

    // --- Contact ---

    @Test
    void contact_vientDeLaFicheQuandElleEstRenseignee() throws Exception {
        String code = "PL-FICHE-" + UUID.randomUUID();
        newProspection(code, newProspect("0611111111", "0522222222", "fiche@exemple.ma"),
                "0699999999", "temp@exemple.ma");

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                // Le mobile de la fiche prime sur le fixe ET sur le champ temporaire.
                .andExpect(jsonPath("$.content[0].telephone").value("0611111111"))
                .andExpect(jsonPath("$.content[0].email").value("fiche@exemple.ma"));
    }

    @Test
    void contact_replieSurLeFixeQuandLeMobileManque() throws Exception {
        String code = "PL-FIXE-" + UUID.randomUUID();
        newProspection(code, newProspect(null, "0522222222", null), null, null);

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].telephone").value("0522222222"));
    }

    /**
     * Le repli qui compte : 7 596 des 8 613 prospections ouvertes n'ont de numéro que là.
     */
    @Test
    void contact_replieSurLePremierContactQuandLaFicheEstMuette() throws Exception {
        String code = "PL-TEMP-" + UUID.randomUUID();
        newProspection(code, newProspect(null, null, null), "0699999999", "temp@exemple.ma");

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].telephone").value("0699999999"))
                .andExpect(jsonPath("$.content[0].email").value("temp@exemple.ma"));
    }

    @Test
    void contact_absentDesDeuxCotes_renvoieNull() throws Exception {
        String code = "PL-VIDE-" + UUID.randomUUID();
        newProspection(code, newProspect(null, null, null), null, null);

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].telephone").doesNotExist())
                .andExpect(jsonPath("$.content[0].email").doesNotExist());
    }

    // --- Alerte ---

    @Test
    void alerte_sansInteraction_renvoieNull() throws Exception {
        String code = "PL-SANSALERTE-" + UUID.randomUUID();
        newProspection(code, newProspect(null, null, null), "0600000000", null);

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].derniereAlerteDate").doesNotExist())
                .andExpect(jsonPath("$.content[0].derniereAlerteMessage").doesNotExist());
    }

    @Test
    void alerte_remonteCelleDeLInteraction() throws Exception {
        String code = "PL-ALERTE-" + UUID.randomUUID();
        ComProspection p = newProspection(code, newProspect(null, null, null), "0600000000", null);
        newInteractionAvecAlerte(p, LocalDateTime.now(),
                LocalDateTime.now().plusDays(3), "rappeler la cliente lundi");

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].derniereAlerteDate").exists())
                .andExpect(jsonPath("$.content[0].derniereAlerteMessage").value("rappeler la cliente lundi"));
    }

    /**
     * « Dernière » = celle de l'interaction la plus récente, pas celle dont l'échéance est la
     * plus lointaine. C'est la dernière décision du commercial qui fait foi : ici l'ancienne
     * interaction porte pourtant une échéance plus éloignée.
     */
    @Test
    void alerte_plusieursInteractions_prendLaPlusRecente() throws Exception {
        String code = "PL-MULTI-" + UUID.randomUUID();
        ComProspection p = newProspection(code, newProspect(null, null, null), "0600000000", null);
        LocalDateTime maintenant = LocalDateTime.now();

        newInteractionAvecAlerte(p, maintenant.minusDays(10), maintenant.plusDays(90), "ancienne alerte");
        newInteractionAvecAlerte(p, maintenant.minusDays(1), maintenant.plusDays(2), "alerte recente");

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].derniereAlerteMessage").value("alerte recente"));
    }

    /** Une interaction sans date d'alerte ne doit pas être prise pour une alerte. */
    @Test
    void alerte_interactionSansDateDAlerte_estIgnoree() throws Exception {
        String code = "PL-NOALERT-" + UUID.randomUUID();
        ComProspection p = newProspection(code, newProspect(null, null, null), "0600000000", null);
        newInteractionAvecAlerte(p, LocalDateTime.now(), null, "simple note sans relance");

        mockMvc.perform(get("/api/prospections").param("search", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].derniereAlerteMessage").doesNotExist());
    }

    /** Page vide : la requête groupée ne doit pas partir avec un {@code IN ()} vide. */
    @Test
    void pageVide_neDeclencheAucuneErreur() throws Exception {
        mockMvc.perform(get("/api/prospections").param("search", "PL-INTROUVABLE-" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}
