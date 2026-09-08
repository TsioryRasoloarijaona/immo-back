package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.domain.MotifEnvoiVisite;
import com.tsix.immocrm.dto.prospection.ResultatEnvoiVisiteDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.app.AppFileObject;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.mail.EnvoiEmailMemoire;
import com.tsix.immocrm.mail.MessageEmail;
import com.tsix.immocrm.mail.VisitePlanifiee;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmEventRepository;
import com.tsix.immocrm.repositories.app.AppFileObjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageSubCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.services.prospection.ConfirmationVisiteService;
import com.tsix.immocrm.storage.FixturePieceJointe;
import com.tsix.immocrm.whatsapp.EnvoiWhatsAppMemoire;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Confirmation de visite, sur ses deux canaux — voir
 * {@code instructions/FEATURE-mail-confirmation-visite.md}.
 *
 * <p><b>Deux pièges de ce dépôt, contournés ici de façon délibérée.</b> Un
 * {@code @TransactionalEventListener(AFTER_COMMIT)} ne se déclenche <b>jamais</b> sous cette
 * suite, qui est {@code @Transactional} donc systématiquement annulée : la publication de
 * {@link VisitePlanifiee} se vérifie avec {@code @RecordApplicationEvents}, et l'envoi lui-même
 * en appelant {@link ConfirmationVisiteService} directement. Passer par l'écouteur injecté ne
 * prouverait rien de plus — c'est un proxy {@code @Async} — et ne prouverait surtout rien du
 * tout, puisqu'il ne serait pas appelé.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
@RecordApplicationEvents
class ConfirmationVisiteTest {

    private static final String RESULTAT_VISITE = "Le prospect veut faire une visite du projet";

    /** Forme dominante en base : indicatif sans « + », numéro national espacé. */
    private static final String MOBILE_VALIDE = "6 61 22 48 57";
    private static final String ATTENDU_E164 = "+212661224857";

    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationEvents evenements;
    @Autowired private EnvoiEmailMemoire envoiEmail;
    @Autowired private EnvoiWhatsAppMemoire envoiWhatsApp;
    @Autowired private ConfirmationVisiteService confirmationVisiteService;

    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComActionMessageRepository actionMessageRepository;
    @Autowired private ComActionMessageCategoryRepository categoryRepository;
    @Autowired private ComActionMessageSubCategoryRepository subCategoryRepository;
    @Autowired private AdmEventRepository admEventRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private AdmAttachedFileTypeRepository attachedFileTypeRepository;
    @Autowired private AppFileObjectRepository fileObjectRepository;
    @Autowired private FixturePieceJointe fixturePieceJointe;

    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private StkVocationRepository vocationRepository;
    @Autowired private StkItemTypeRepository itemTypeRepository;
    @Autowired private StkItemCategoryRepository itemCategoryRepository;
    @Autowired private StkItemRepository itemRepository;

    /**
     * Les deux doubles d'envoi sont des singletons du contexte : leur état survivrait au rollback
     * de la transaction, contrairement aux données.
     */
    @BeforeEach
    void viderLesEnvois() {
        envoiEmail.vider();
        envoiWhatsApp.vider();
    }

    // ------------------------------------------------------------------ fixtures

    private CfgProject newProjet(String description, String adresse, String ville) {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-CV-" + UUID.randomUUID());
        projet.setDescription(description);
        projet.setAddress(adresse);
        projet.setCity(ville);
        projet.setZipCode(ville == null ? null : "20000");
        return projectRepository.save(projet);
    }

    private CfgTranche newTranche(CfgProject projet, String description, String adresse) {
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-CV-" + UUID.randomUUID());
        tranche.setDescription(description);
        tranche.setAddress(adresse);
        tranche.setCfgProject(projet);
        return trancheRepository.save(tranche);
    }

    private CfgTier newTier(String nom, String email, String mobile, String indicatif) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode("TIER-CV-" + UUID.randomUUID());
        tier.setDescription(nom);
        tier.setEMail(email);
        tier.setMobile(mobile);
        tier.setMobileIndicatif(indicatif);
        return tierRepository.save(tier);
    }

    /** Prospect joignable par les deux canaux — le cas nominal. */
    private CfgTier newProspectComplet() {
        return newTier("Ahmed BENNANI", "ahmed@exemple.ma", MOBILE_VALIDE, "212");
    }

    private ComProspection newProspection(CfgTier prospect, CfgTranche tranche) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-CV-" + UUID.randomUUID());
        p.setCfgProspect(prospect);
        p.setStkHierarchy(tranche);
        p.setCfgCommercial(newTier("Khadija ELKORRI", null, "0600000000", "212"));
        return prospectionRepository.save(p);
    }

    private ComActionMessageSubCategory newResultat(String code) {
        ComActionMessageCategory categorie = new ComActionMessageCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("Prospection-CV-" + UUID.randomUUID());
        categoryRepository.save(categorie);

        ComActionMessageSubCategory resultat = new ComActionMessageSubCategory();
        resultat.setId(UUID.randomUUID());
        resultat.setCode(code);
        resultat.setDescription(code);
        resultat.setComActionMessageCategory(categorie);
        return subCategoryRepository.save(resultat);
    }

    /** Interaction portant un rendez-vous, telle que l'API la crée. */
    private ComActionMessage newInteractionAvecRdv(ComProspection p, LocalDateTime quand) {
        AdmEvent evenement = new AdmEvent();
        evenement.setId(UUID.randomUUID());
        evenement.setSubject("Visite du projet");
        evenement.setStartHour(quand);
        admEventRepository.save(evenement);

        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setComProspection(p);
        m.setDocDate(LocalDateTime.now());
        m.setAlertMessage("visite programmée");
        m.setAdmEvent(evenement);
        return actionMessageRepository.save(m);
    }

    private LocalDateTime leRdv() {
        return LocalDateTime.of(2026, 9, 15, 15, 30);
    }

    private StkItem newArticle() {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-CV-" + UUID.randomUUID());
        vocationRepository.save(vocation);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-CV-" + UUID.randomUUID());
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);

        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("CAT-CV-" + UUID.randomUUID());
        categorie.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(categorie);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("ART-CV-" + UUID.randomUUID());
        article.setStkItemType(type);
        article.setStkItemCategory(categorie);
        article.setStkVocation(vocation);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        return itemRepository.save(article);
    }

    private AdmAttachedFileType newTypePiece(String code) {
        AdmAttachedFileType type = new AdmAttachedFileType();
        type.setId(UUID.randomUUID());
        type.setCode(code);
        type.setDescription(code);
        return attachedFileTypeRepository.save(type);
    }

    private AdmAttachedFile newPiece(UUID porteurId, String description, String typeCode) {
        AdmAttachedFile piece = new AdmAttachedFile();
        piece.setId(UUID.randomUUID());
        piece.setCategoryId(porteurId);
        piece.setCategory("Yes.Stk.Building.Item");
        piece.setDescription(description);
        piece.setExtension(".pdf");
        piece.setAdmAttachedFileTypeId(newTypePiece(typeCode).getId());
        return fixturePieceJointe.deposer(piece, "PLAN-PDF".getBytes(StandardCharsets.UTF_8));
    }

    /** Prospection nominale : prospect complet, projet adressé, tranche sans adresse. */
    private ComProspection prospectionNominale() {
        CfgProject projet = newProjet("AYAD RESIDENCE BEL AIR", "100 BD ZERKTOUNI", "Casablanca");
        return newProspection(newProspectComplet(), newTranche(projet, "Tranche 1", null));
    }

    // ------------------------------------------------------------------ publication

    @Test
    void priseDeRendezVous_publieLevenement() throws Exception {
        ComProspection p = prospectionNominale();
        ComActionMessageSubCategory resultat = newResultat(RESULTAT_VISITE);
        String body = """
                {"description":"le prospect veut visiter","resultatId":"%s","dateRdv":"2026-09-15T15:30:00"}"""
                .formatted(resultat.getId());

        mockMvc.perform(post("/api/prospections/{id}/interactions", p.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dateRdv").value("2026-09-15T15:30:00"));

        assertThat(evenements.stream(VisitePlanifiee.class)).hasSize(1);
    }

    /**
     * Une interaction ordinaire ne convoque personne. C'est la garantie que la fonctionnalité
     * n'élargit pas l'ensemble déclencheur : seul ce qui crée déjà une entrée de calendrier
     * envoie quelque chose.
     */
    @Test
    void interactionSansRendezVous_nePublieRien() throws Exception {
        ComProspection p = prospectionNominale();

        mockMvc.perform(post("/api/prospections/{id}/interactions", p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"simple appel de suivi\"}"))
                .andExpect(status().isCreated());

        assertThat(evenements.stream(VisitePlanifiee.class)).isEmpty();
    }

    // ------------------------------------------------------------------ R1 destinataires

    @Test
    void prospectSansCoordonnees_nEnvoieRien() {
        ComProspection p = newProspection(newTier("Sans Rien", null, null, null), null);
        ComActionMessage interaction = newInteractionAvecRdv(p, leRdv());

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(interaction.getId());

        assertThat(r.envoye()).isFalse();
        assertThat(r.email().motif()).isEqualTo(MotifEnvoiVisite.DESTINATAIRE_ABSENT);
        assertThat(r.whatsapp().motif()).isEqualTo(MotifEnvoiVisite.DESTINATAIRE_ABSENT);
        assertThat(envoiEmail.nombreMessages()).isZero();
        assertThat(envoiWhatsApp.nombreMessages()).isZero();
    }

    /**
     * 33 lignes réelles portent une valeur non vide qui n'est pas une adresse — un numéro de
     * téléphone, le plus souvent. Le serveur SMTP la refuserait, et l'échec serait invisible
     * depuis l'application.
     */
    @Test
    void emailSansArobase_estTraiteCommeAbsent() {
        ComProspection p = newProspection(newTier("Faux Email", "0612345678", null, null), null);

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.email().motif()).isEqualTo(MotifEnvoiVisite.DESTINATAIRE_ABSENT);
        assertThat(envoiEmail.nombreMessages()).isZero();
    }

    /** Le repli n'est pas une commodité : aucune des deux sources d'email ne suffit seule. */
    @Test
    void sansEmailSurLaFiche_replieSurCeluiDuPremierContact() {
        ComProspection p = newProspection(newTier("Fiche Sans Email", null, null, null), null);
        p.setTempEMail("  contact@exemple.ma  ");
        prospectionRepository.save(p);

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.email().envoye()).isTrue();
        assertThat(r.email().destinataire()).isEqualTo("contact@exemple.ma");
        assertThat(envoiEmail.dernier().destinataire()).isEqualTo("contact@exemple.ma");
    }

    // ------------------------------------------------------------------ canal WhatsApp

    @Test
    void prospectAvecMobile_recoitAussiUnWhatsApp() {
        ComProspection p = prospectionNominale();

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.whatsapp().envoye()).isTrue();
        assertThat(r.whatsapp().destinataire()).isEqualTo(ATTENDU_E164);
        assertThat(envoiWhatsApp.dernier().destinataire()).isEqualTo(ATTENDU_E164);
        // Les deux canaux ont servi : c'est le cas nominal, pas une exclusivité.
        assertThat(r.email().envoye()).isTrue();
        assertThat(envoiEmail.nombreMessages()).isEqualTo(1);
    }

    /**
     * 19 fiches réelles portent un fixe dans la colonne mobile. Le motif doit les distinguer de
     * l'absence de numéro : l'action corrective n'est pas la même.
     */
    @Test
    void numeroFixe_donneNumeroInexploitable() {
        ComProspection p = newProspection(
                newTier("Fixe Seulement", "a@exemple.ma", "5 22 27 34 96", "212"), null);

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.whatsapp().motif()).isEqualTo(MotifEnvoiVisite.NUMERO_INEXPLOITABLE);
        assertThat(envoiWhatsApp.nombreMessages()).isZero();
        // Le mail, lui, part : l'échec d'un canal n'empêche pas l'autre.
        assertThat(r.email().envoye()).isTrue();
    }

    @Test
    void sansAucunMobile_donneDestinataireAbsentEtPasNumeroInexploitable() {
        ComProspection p = newProspection(newTier("Sans Mobile", "a@exemple.ma", null, "212"), null);

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.whatsapp().motif()).isEqualTo(MotifEnvoiVisite.DESTINATAIRE_ABSENT);
    }

    /**
     * UltraMsg répond 200 même pour un refus. Le confondre avec un succès ferait mentir le champ
     * {@code envoye} au commercial, qui croirait son prospect prévenu.
     */
    @Test
    void refusDeLApi_donneEchecEnvoiEtPasUnFauxSucces() {
        ComProspection p = prospectionNominale();
        envoiWhatsApp.definirAccepte(false);

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.whatsapp().envoye()).isFalse();
        assertThat(r.whatsapp().motif()).isEqualTo(MotifEnvoiVisite.ECHEC_ENVOI);
        assertThat(r.envoye()).isTrue(); // le mail a compensé
    }

    /**
     * La propriété centrale du choix « les deux canaux » : chacun tombe sans entraîner l'autre.
     * C'est ce qui fait passer la couverture de 57 % à ~99 %.
     */
    @Test
    void emailInactif_nEmpechePasLeWhatsApp() {
        ComProspection p = prospectionNominale();
        envoiEmail.definirActif(false);

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.email().motif()).isEqualTo(MotifEnvoiVisite.SERVICE_INACTIF);
        assertThat(r.whatsapp().envoye()).isTrue();
        assertThat(r.envoye()).isTrue();
    }

    @Test
    void whatsAppInactif_nEmpechePasLeMail() {
        ComProspection p = prospectionNominale();
        envoiWhatsApp.definirActif(false);

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.whatsapp().motif()).isEqualTo(MotifEnvoiVisite.SERVICE_INACTIF);
        assertThat(r.email().envoye()).isTrue();
        assertThat(envoiWhatsApp.nombreMessages()).isZero();
    }

    /** Le WhatsApp porte la même information que le mail, dans une forme adaptée au canal. */
    @Test
    void leTexteWhatsAppReprendLesInformationsEssentielles() {
        ComProspection p = prospectionNominale();

        confirmationVisiteService.envoyer(newInteractionAvecRdv(p, leRdv()).getId());

        String texte = envoiWhatsApp.dernier().texte();
        assertThat(texte).contains("15/09/2026", "15h30", "100 BD ZERKTOUNI",
                "AYAD RESIDENCE BEL AIR", "Khadija ELKORRI", "0600000000", "pièce d'identité");
        // Mise en gras propre à WhatsApp.
        assertThat(texte).contains("*Date*");
    }

    /**
     * Le WhatsApp ne doit pas annoncer de plan : il n'en porte aucun, faute d'URL publique
     * durable. Promettre une pièce jointe absente serait pire que se taire.
     */
    @Test
    void leTexteWhatsAppNAnnoncePasDePlanMemeQuandLeMailEnPorte() {
        CfgProject projet = newProjet("AYAD RESIDENCE BEL AIR", "100 BD ZERKTOUNI", "Casablanca");
        CfgTranche tranche = newTranche(projet, "Tranche 1", null);
        ComProspection p = newProspection(newProspectComplet(), tranche);
        newPiece(projet.getId(), "PLAN DE MASSE", "PLAN");

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.email().nombrePiecesJointes()).isEqualTo(1);
        assertThat(r.whatsapp().nombrePiecesJointes()).isZero();
        assertThat(envoiWhatsApp.dernier().texte()).doesNotContain("pièce jointe");
    }

    // ------------------------------------------------------------------ R3/R4 lieu et adresse

    @Test
    void adresseAbsenteDeLaTranche_vientDuProjet() {
        ComProspection p = prospectionNominale();

        confirmationVisiteService.envoyer(newInteractionAvecRdv(p, leRdv()).getId());

        MessageEmail message = envoiEmail.dernier();
        assertThat(message.variables()).containsEntry("adresse", "100 BD ZERKTOUNI");
        assertThat(message.variables()).containsEntry("ville", "20000 Casablanca");
        assertThat(message.variables()).containsEntry("lieu", "AYAD RESIDENCE BEL AIR — Tranche 1");
        assertThat(message.variables()).containsEntry("date", "15/09/2026");
        assertThat(message.variables()).containsEntry("heure", "15h30");
    }

    /**
     * Cas réel {@code MBR} : {@code Address} recopie le nom du projet. Le bloc adresse doit
     * disparaître, et le message partir quand même — une confirmation sans adresse reste utile,
     * une confirmation qui envoie au mauvais endroit ne l'est pas.
     */
    @Test
    void adresseEgaleAuNomDuPorteur_estOmiseMaisLeMessagePart() {
        CfgProject projet = newProjet("MANSOURIA BEACH RESORT", "Mansouria Beach Resort", "Ben Slimane");
        CfgTranche tranche = newTranche(projet, "MANSOURIA BEACH RESORT", null);
        ComProspection p = newProspection(newProspectComplet(), tranche);

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.envoye()).isTrue();
        assertThat(envoiEmail.dernier().variables()).containsEntry("adresse", null);
        // Le lieu, lui, reste : c'est bien un nom de projet, à sa place.
        assertThat(envoiEmail.dernier().variables()).containsEntry("lieu", "MANSOURIA BEACH RESORT");
        // Et le WhatsApp suit la même règle, puisqu'il lit le même contenu résolu.
        assertThat(envoiWhatsApp.dernier().texte()).doesNotContain("*Adresse*");
    }

    /** Cas réel {@code YESTEST} : la colonne contient littéralement le mot « adresse ». */
    @Test
    void adresseTropCourte_estOmise() {
        CfgProject projet = newProjet("YESTEST", "adresse", "VIlle");
        CfgTranche tranche = newTranche(projet, "Tranche test", null);
        ComProspection p = newProspection(newProspectComplet(), tranche);

        confirmationVisiteService.envoyer(newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(envoiEmail.dernier().variables()).containsEntry("adresse", null);
    }

    @Test
    void leCommercialEtSonMobileSontDansLeMessage() {
        ComProspection p = prospectionNominale();

        confirmationVisiteService.envoyer(newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(envoiEmail.dernier().variables()).containsEntry("mobileCommercial", "0600000000");
        assertThat(envoiEmail.dernier().variables()).containsEntry("nomCommercial", "Khadija ELKORRI");
    }

    // ------------------------------------------------------------------ R6/R7 pièces jointes

    @Test
    void planDeLarticle_estJoint() {
        CfgProject projet = newProjet("AYAD RESIDENCE BEL AIR", "100 BD ZERKTOUNI", "Casablanca");
        CfgTranche tranche = newTranche(projet, "Tranche 1", null);
        ComProspection p = newProspection(newProspectComplet(), tranche);
        StkItem article = newArticle();
        p.setStkItem(article);
        prospectionRepository.save(p);
        newPiece(article.getId(), "PLAN RDC", "PLAN");

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.email().nombrePiecesJointes()).isEqualTo(1);
        assertThat(envoiEmail.dernier().piecesJointes()).hasSize(1);
        assertThat(envoiEmail.dernier().piecesJointes().get(0).nom()).isEqualTo("PLAN RDC.pdf");
        assertThat(envoiEmail.dernier().piecesJointes().get(0).contenu())
                .isEqualTo("PLAN-PDF".getBytes(StandardCharsets.UTF_8));
    }

    /**
     * La protection qui compte : sans le filtre par type, le niveau projet enverrait au prospect
     * n'importe quel document déposé, contrats signés compris — et il existe précisément une
     * telle ligne en base réelle.
     */
    @Test
    void pieceQuiNestPasUnPlan_estIgnoree() {
        CfgProject projet = newProjet("AYAD RESIDENCE BEL AIR", "100 BD ZERKTOUNI", "Casablanca");
        CfgTranche tranche = newTranche(projet, "Tranche 1", null);
        ComProspection p = newProspection(newProspectComplet(), tranche);
        StkItem article = newArticle();
        p.setStkItem(article);
        prospectionRepository.save(p);
        newPiece(article.getId(), "CONTRAT SIGNE", "CONTRAT");

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.email().envoye()).isTrue();
        assertThat(r.email().nombrePiecesJointes()).isZero();
    }

    /** Les plans déposés au niveau du projet servent quand l'article n'est pas connu (97 % des cas). */
    @Test
    void sansArticle_lesPlansDuProjetSontJoints() {
        CfgProject projet = newProjet("AYAD RESIDENCE BEL AIR", "100 BD ZERKTOUNI", "Casablanca");
        CfgTranche tranche = newTranche(projet, "Tranche 1", null);
        ComProspection p = newProspection(newProspectComplet(), tranche);
        newPiece(projet.getId(), "PLAN DE MASSE", "PLAN");

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.email().nombrePiecesJointes()).isEqualTo(1);
        assertThat(envoiEmail.dernier().piecesJointes().get(0).nom()).isEqualTo("PLAN DE MASSE.pdf");
    }

    /**
     * R7 — un plan trop lourd est abandonné, le message part sans lui. Un {@code PLAN_ETAGE}
     * réel monte à 5 Mo ; deux suffisent presque à saturer le plafond de 10 Mo.
     */
    @Test
    void planAuDelaDuPlafond_estAbandonneEtLeMessagePart() {
        CfgProject projet = newProjet("AYAD RESIDENCE BEL AIR", "100 BD ZERKTOUNI", "Casablanca");
        CfgTranche tranche = newTranche(projet, "Tranche 1", null);
        ComProspection p = newProspection(newProspectComplet(), tranche);
        AdmAttachedFile piece = newPiece(projet.getId(), "PLAN ENORME", "PLAN_ETAGE");

        // La taille déclarée est ce que consulte la règle — c'est délibéré : plafonner après
        // lecture obligerait à charger en mémoire le fichier qu'on cherche justement à écarter.
        AppFileObject objet = fileObjectRepository.findById(piece.getId()).orElseThrow();
        objet.setSizeBytes(64L * 1024 * 1024);
        fileObjectRepository.saveAndFlush(objet);

        ResultatEnvoiVisiteDto r = confirmationVisiteService.envoyer(
                newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(r.email().envoye()).isTrue();
        assertThat(r.email().nombrePiecesJointes()).isZero();
    }

    // ------------------------------------------------------------------ endpoint de renvoi

    @Test
    void renvoi_nominal_renvoie200EtExpedieSurLesDeuxCanaux() throws Exception {
        ComProspection p = prospectionNominale();
        ComActionMessage interaction = newInteractionAvecRdv(p, leRdv());

        mockMvc.perform(post("/api/prospections/{id}/interactions/{interactionId}/email-visite",
                        p.getId(), interaction.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.envoye").value(true))
                .andExpect(jsonPath("$.email.envoye").value(true))
                .andExpect(jsonPath("$.email.motif").value("ENVOYE"))
                .andExpect(jsonPath("$.email.destinataire").value("ahmed@exemple.ma"))
                .andExpect(jsonPath("$.whatsapp.envoye").value(true))
                .andExpect(jsonPath("$.whatsapp.destinataire").value(ATTENDU_E164));

        assertThat(envoiEmail.nombreMessages()).isEqualTo(1);
        assertThat(envoiWhatsApp.nombreMessages()).isEqualTo(1);
    }

    /** Sans coordonnées, l'issue est un 200 explicite — le commercial doit voir pourquoi. */
    @Test
    void renvoi_sansCoordonnees_renvoie200AvecLesMotifs() throws Exception {
        ComProspection p = newProspection(newTier("Sans Rien", null, null, null), null);
        ComActionMessage interaction = newInteractionAvecRdv(p, leRdv());

        mockMvc.perform(post("/api/prospections/{id}/interactions/{interactionId}/email-visite",
                        p.getId(), interaction.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.envoye").value(false))
                .andExpect(jsonPath("$.email.motif").value("DESTINATAIRE_ABSENT"))
                .andExpect(jsonPath("$.whatsapp.motif").value("DESTINATAIRE_ABSENT"));
    }

    /**
     * Sans ce contrôle, l'endpoint permettrait de faire expédier le rendez-vous d'autrui en
     * énumérant les identifiants d'interaction sous une prospection qu'on a le droit de voir.
     */
    @Test
    void renvoi_interactionDuneAutreProspection_renvoie404() throws Exception {
        ComProspection sienne = prospectionNominale();
        ComProspection autre = prospectionNominale();
        ComActionMessage interactionDeLautre = newInteractionAvecRdv(autre, leRdv());

        mockMvc.perform(post("/api/prospections/{id}/interactions/{interactionId}/email-visite",
                        sienne.getId(), interactionDeLautre.getId()))
                .andExpect(status().isNotFound());

        assertThat(envoiEmail.nombreMessages()).isZero();
        assertThat(envoiWhatsApp.nombreMessages()).isZero();
    }

    @Test
    void renvoi_interactionSansRendezVous_renvoie409() throws Exception {
        ComProspection p = prospectionNominale();
        ComActionMessage sansRdv = new ComActionMessage();
        sansRdv.setId(UUID.randomUUID());
        sansRdv.setComProspection(p);
        sansRdv.setDocDate(LocalDateTime.now());
        sansRdv.setAlertMessage("simple note");
        actionMessageRepository.save(sansRdv);

        mockMvc.perform(post("/api/prospections/{id}/interactions/{interactionId}/email-visite",
                        p.getId(), sansRdv.getId()))
                .andExpect(status().isConflict());

        assertThat(envoiEmail.nombreMessages()).isZero();
        assertThat(envoiWhatsApp.nombreMessages()).isZero();
    }

    @Test
    void renvoi_interactionInconnue_renvoie404() throws Exception {
        ComProspection p = prospectionNominale();

        mockMvc.perform(post("/api/prospections/{id}/interactions/{interactionId}/email-visite",
                        p.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    /** Le corps texte est l'alternative des clients sans HTML : il doit porter la même information. */
    @Test
    void leCorpsTexteReprendLesInformationsEssentielles() {
        ComProspection p = prospectionNominale();

        confirmationVisiteService.envoyer(newInteractionAvecRdv(p, leRdv()).getId());

        String texte = envoiEmail.dernier().corpsTexte();
        assertThat(texte).contains("15/09/2026", "15h30", "100 BD ZERKTOUNI",
                "Khadija ELKORRI", "0600000000", "pièce d'identité");
    }

    /** L'objet du message doit être lisible dans une boîte encombrée, sans l'ouvrir. */
    @Test
    void lObjetDuMessagePorteLaDateEtLeProjet() {
        CfgProject projet = newProjet("AYAD RESIDENCE BEL AIR", "100 BD ZERKTOUNI", "Casablanca");
        CfgTranche tranche = newTranche(projet, "AYAD RESIDENCE BEL AIR", null);
        ComProspection p = newProspection(newProspectComplet(), tranche);

        confirmationVisiteService.envoyer(newInteractionAvecRdv(p, leRdv()).getId());

        assertThat(envoiEmail.dernier().sujet())
                .isEqualTo("Votre visite du 15/09/2026 — AYAD RESIDENCE BEL AIR");
        assertThat(envoiEmail.dernier().template()).isEqualTo("mail/confirmation-visite");
    }
}
