package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.dto.prospection.EnvoyerOffrePrixRequest;
import com.tsix.immocrm.dto.prospection.ResultatEnvoiOffreDto;
import com.tsix.immocrm.dto.prospection.ResultatEnvoiVisiteDto;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.mail.EnvoiEmailMemoire;
import com.tsix.immocrm.mail.MessageEmail;
import com.tsix.immocrm.mail.VisiteEffectuee;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.services.prospection.OffrePrixService;
import com.tsix.immocrm.services.prospection.RemerciementVisiteService;
import com.tsix.immocrm.whatsapp.EnvoiWhatsAppMemoire;
import com.tsix.immocrm.whatsapp.MessageWhatsApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Remerciement d'après-visite et offre de prix — voir
 * {@code instructions/FEATURE-offre-apres-visite.md}.
 *
 * <p><b>Mêmes deux pièges que {@link ConfirmationVisiteTest}.</b> Un
 * {@code @TransactionalEventListener(AFTER_COMMIT)} ne se déclenche jamais sous cette suite, qui
 * est {@code @Transactional} donc annulée : la publication de {@link VisiteEffectuee} se vérifie
 * avec {@code @RecordApplicationEvents}, et l'envoi en appelant les services directement.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
@RecordApplicationEvents
class OffreApresVisiteTest {

    private static final String ETAPE_APRES_VISITE = "4 APRES VISITES";

    /** Forme dominante en base : indicatif sans « + », numéro national espacé. */
    private static final String MOBILE_VALIDE = "6 61 22 48 57";
    private static final String ATTENDU_E164 = "+212661224857";

    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationEvents evenements;
    @Autowired private EnvoiEmailMemoire envoiEmail;
    @Autowired private EnvoiWhatsAppMemoire envoiWhatsApp;
    @Autowired private RemerciementVisiteService remerciementVisiteService;
    @Autowired private OffrePrixService offrePrixService;

    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComProspectionKindRepository kindRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgProjectRepository projectRepository;

    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private StkVocationRepository vocationRepository;
    @Autowired private StkItemTypeRepository itemTypeRepository;
    @Autowired private StkItemCategoryRepository itemCategoryRepository;
    @Autowired private StkItemRepository itemRepository;

    @BeforeEach
    void viderLesEnvois() {
        envoiEmail.vider();
        envoiWhatsApp.vider();
    }

    // ------------------------------------------------------------------ fixtures

    private CfgProject newProjet() {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-OAV-" + UUID.randomUUID());
        projet.setDescription("AYAD RESIDENCE BEL AIR");
        return projectRepository.save(projet);
    }

    private CfgTranche newTranche(CfgProject projet) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-OAV-" + UUID.randomUUID());
        tranche.setDescription("Tranche 1");
        tranche.setCfgProject(projet);
        return trancheRepository.save(tranche);
    }

    private CfgTier newTier(String nom, String email, String mobile, String indicatif) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("TIER-OAV-" + UUID.randomUUID());
        tier.setDescription(nom);
        tier.setEMail(email);
        tier.setMobile(mobile);
        tier.setMobileIndicatif(indicatif);
        return tierRepository.save(tier);
    }

    private ComProspectionKind newEtape(String code) {
        ComProspectionKind kind = new ComProspectionKind();
        kind.setId(UUID.randomUUID());
        kind.setCode(code);
        kind.setDescription(code);
        return kindRepository.save(kind);
    }

    /** Article valorisé, sans quoi l'offre n'aurait aucun prix à annoncer. */
    private StkItem newArticle() {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-OAV-" + UUID.randomUUID());
        vocationRepository.save(vocation);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-OAV-" + UUID.randomUUID());
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);

        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("CAT-OAV-" + UUID.randomUUID());
        categorie.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(categorie);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("ART-OAV-" + UUID.randomUUID());
        article.setStkItemType(type);
        article.setStkItemCategory(categorie);
        article.setStkVocation(vocation);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        article.setArea(new BigDecimal("100.000000"));
        article.setComSalePriceHT(new BigDecimal("1000000.000000"));
        article.setComSalePrice(new BigDecimal("1200000.000000"));
        article.setVat(new BigDecimal("0.200000"));
        return itemRepository.save(article);
    }

    private ComProspection newProspection(String etapeCode, boolean avecArticle,
                                          String email, String mobile) {
        CfgTranche tranche = newTranche(newProjet());
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-OAV-" + UUID.randomUUID());
        p.setCfgProspect(newTier("Ahmed BENNANI", email, mobile, mobile == null ? null : "212"));
        p.setStkHierarchy(tranche);
        p.setCfgCommercial(newTier("Khadija ELKORRI", null, "0600000000", "212"));
        if (etapeCode != null) {
            p.setComProspectionKind(newEtape(etapeCode));
        }
        if (avecArticle) {
            p.setStkItem(newArticle());
        }
        return prospectionRepository.save(p);
    }

    /** Le cas nominal : après visite, lot rattaché, joignable sur les deux canaux. */
    private ComProspection prospectionNominale() {
        return newProspection(ETAPE_APRES_VISITE, true, "ahmed@exemple.ma", MOBILE_VALIDE);
    }

    // ------------------------------------------------------------------ publication

    @Test
    void passageApresVisite_publieLevenement() throws Exception {
        ComProspection p = newProspection("1 A TRAITER", false, "ahmed@exemple.ma", MOBILE_VALIDE);
        ComProspectionKind apresVisite = newEtape(ETAPE_APRES_VISITE);

        mockMvc.perform(patch("/api/prospections/{id}/etape", p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etapeId":"%s"}""".formatted(apresVisite.getId())))
                .andExpect(status().isOk());

        assertThat(evenements.stream(VisiteEffectuee.class)).hasSize(1);
    }

    /**
     * Le déclencheur ne s'élargit pas : toute autre étape reste muette. Sans ce test, ajouter un
     * code au référentiel suffirait à faire partir des remerciements imprévus.
     */
    @Test
    void passageAutreEtape_nePubliERien() throws Exception {
        ComProspection p = newProspection("1 A TRAITER", false, "ahmed@exemple.ma", MOBILE_VALIDE);
        ComProspectionKind autre = newEtape("3 QUALIFIÉ");

        mockMvc.perform(patch("/api/prospections/{id}/etape", p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"etapeId":"%s"}""".formatted(autre.getId())))
                .andExpect(status().isOk());

        assertThat(evenements.stream(VisiteEffectuee.class)).isEmpty();
    }

    // ------------------------------------------------------------------ remerciement

    @Test
    void remerciement_partSurLesDeuxCanaux() {
        ComProspection p = prospectionNominale();

        ResultatEnvoiVisiteDto resultat = remerciementVisiteService.envoyer(p.getId());

        assertThat(resultat.envoye()).isTrue();
        assertThat(resultat.email().envoye()).isTrue();
        assertThat(resultat.whatsapp().envoye()).isTrue();
        assertThat(resultat.whatsapp().destinataire()).isEqualTo(ATTENDU_E164);

        MessageEmail mail = envoiEmail.messages().getFirst();
        assertThat(mail.sujet()).contains("AYAD RESIDENCE BEL AIR");
        assertThat(mail.piecesJointes()).isEmpty();
    }

    /** Sans email, le WhatsApp reste le canal utile — c'est 80 % des cas réels à cette étape. */
    @Test
    void remerciement_sansEmail_partQuandMemeEnWhatsApp() {
        ComProspection p = newProspection(ETAPE_APRES_VISITE, false, null, MOBILE_VALIDE);

        ResultatEnvoiVisiteDto resultat = remerciementVisiteService.envoyer(p.getId());

        assertThat(resultat.envoye()).isTrue();
        assertThat(resultat.email().envoye()).isFalse();
        assertThat(resultat.whatsapp().envoye()).isTrue();
    }

    @Test
    void remerciement_sansAucunDestinataire_neLevePas() {
        ComProspection p = newProspection(ETAPE_APRES_VISITE, false, null, null);

        ResultatEnvoiVisiteDto resultat = remerciementVisiteService.envoyer(p.getId());

        assertThat(resultat.envoye()).isFalse();
        assertThat(envoiEmail.messages()).isEmpty();
        assertThat(envoiWhatsApp.messages()).isEmpty();
    }

    // ------------------------------------------------------------------ offre de prix

    @Test
    void offre_cheminNominal() throws Exception {
        ComProspection p = prospectionNominale();

        mockMvc.perform(post("/api/prospections/{id}/offre-prix", p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"remisePercent":5}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.envoye").value(true))
                .andExpect(jsonPath("$.offreCode").isNotEmpty())
                .andExpect(jsonPath("$.offreReutilisee").value(false))
                .andExpect(jsonPath("$.email.envoye").value(true))
                .andExpect(jsonPath("$.whatsapp.envoye").value(true));

        // 1 200 000 TTC − 5 % = 1 140 000
        MessageEmail mail = envoiEmail.messages().getFirst();
        assertThat(mail.piecesJointes()).hasSize(1);
        assertThat(mail.piecesJointes().getFirst().nom()).startsWith("Offre-").endsWith(".pdf");
        assertThat(mail.piecesJointes().getFirst().contenu()).isNotEmpty();
    }

    /**
     * Le numéro d'offre porte une barre oblique ({@code 26/00314}). La reprendre telle quelle
     * fabriquerait un nom de pièce jointe invalide, voire un chemin.
     */
    @Test
    void offre_nomDePieceJointeSansBarreOblique() {
        ComProspection p = prospectionNominale();

        offrePrixService.envoyer(p.getId(), new EnvoyerOffrePrixRequest(null, null, null, null));

        assertThat(envoiEmail.messages().getFirst().piecesJointes().getFirst().nom())
                .doesNotContain("/").doesNotContain("\\");
    }

    /**
     * Le message WhatsApp porte le montant : à cette étape, 80 % des prospects n'ont pas d'email
     * et n'auront jamais le PDF. C'est la décision centrale du chantier.
     */
    @Test
    void offre_whatsAppPorteLeMontant() {
        ComProspection p = prospectionNominale();

        ResultatEnvoiOffreDto resultat = offrePrixService.envoyer(
                p.getId(), new EnvoyerOffrePrixRequest(new BigDecimal("5"), null, null, null));

        MessageWhatsApp message = envoiWhatsApp.messages().getFirst();
        assertThat(message.texte()).contains("1 140 000.00 DH");
        assertThat(message.texte()).contains(resultat.offreCode());
    }

    /** Sans email parti, le WhatsApp n'annonce pas un mail inexistant. */
    @Test
    void offre_sansEmail_leWhatsAppNeMentionnePasDeMail() {
        ComProspection p = newProspection(ETAPE_APRES_VISITE, true, null, MOBILE_VALIDE);

        offrePrixService.envoyer(p.getId(), new EnvoyerOffrePrixRequest(null, null, null, null));

        assertThat(envoiWhatsApp.messages().getFirst().texte())
                .doesNotContain("email").doesNotContain("mail");
    }

    /** Avec un email parti, il l'annonce. */
    @Test
    void offre_avecEmail_leWhatsAppMentionneLeMail() {
        ComProspection p = prospectionNominale();

        offrePrixService.envoyer(p.getId(), new EnvoyerOffrePrixRequest(null, null, null, null));

        assertThat(envoiWhatsApp.messages().getFirst().texte()).contains("email");
    }

    @Test
    void offre_reutiliseeQuandAucuneConditionNestFournie() {
        ComProspection p = prospectionNominale();

        ResultatEnvoiOffreDto premiere = offrePrixService.envoyer(
                p.getId(), new EnvoyerOffrePrixRequest(null, null, null, null));
        ResultatEnvoiOffreDto seconde = offrePrixService.envoyer(
                p.getId(), new EnvoyerOffrePrixRequest(null, null, null, null));

        assertThat(premiere.offreReutilisee()).isFalse();
        assertThat(seconde.offreReutilisee()).isTrue();
        assertThat(seconde.offreId()).isEqualTo(premiere.offreId());
    }

    /** Une remise nouvelle ne réécrit jamais une offre déjà expédiée : elle en crée une autre. */
    @Test
    void offre_nouvelleQuandUneRemiseEstFournie() {
        ComProspection p = prospectionNominale();

        ResultatEnvoiOffreDto premiere = offrePrixService.envoyer(
                p.getId(), new EnvoyerOffrePrixRequest(null, null, null, null));
        ResultatEnvoiOffreDto seconde = offrePrixService.envoyer(
                p.getId(), new EnvoyerOffrePrixRequest(new BigDecimal("10"), null, null, null));

        assertThat(seconde.offreReutilisee()).isFalse();
        assertThat(seconde.offreId()).isNotEqualTo(premiere.offreId());
    }

    // ------------------------------------------------------------------ refus

    @Test
    void offre_sansArticle_rend409EtNommeLaSortie() throws Exception {
        ComProspection p = newProspection(ETAPE_APRES_VISITE, false, "ahmed@exemple.ma", MOBILE_VALIDE);

        mockMvc.perform(post("/api/prospections/{id}/offre-prix", p.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("tranche-principale")));
    }

    @Test
    void offre_horsEtapeApresVisite_rend409() throws Exception {
        ComProspection p = newProspection("1 A TRAITER", true, "ahmed@exemple.ma", MOBILE_VALIDE);

        mockMvc.perform(post("/api/prospections/{id}/offre-prix", p.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    void offre_prospectionInconnue_rend404() throws Exception {
        mockMvc.perform(post("/api/prospections/{id}/offre-prix", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void remerciement_prospectionInconnue_leve() {
        assertThatThrownBy(() -> remerciementVisiteService.envoyer(UUID.randomUUID()))
                .hasMessageContaining("Prospection introuvable");
    }
}
