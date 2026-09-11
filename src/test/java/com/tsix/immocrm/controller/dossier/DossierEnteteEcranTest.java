package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkPresentationTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Les champs d'en-tête de l'écran « Dossier de vente » et les deux colonnes manquantes de
 * l'échéancier — voir {@code instructions/FEATURE-fiche-dossier-alignement-ecran.md} §3 et §4.
 *
 * <p>Les valeurs reprennent le dossier réel de la capture (`ARS 2025/00357`) : prix du m²
 * 13 000, total HT 747 500, prix de vente HT 776 250, présentation « C », surface 69 m².
 *
 * <p><b>Le piège verrouillé ici est `QuittanceCode`.</b> En base la colonne n'est jamais
 * {@code NULL} — 0 ligne sur 921 — mais vide sur 83,5 % d'entre elles. Sans normalisation, l'API
 * répondrait {@code ""} et le client afficherait un décompte vide plutôt que de masquer le champ.
 * C'est le quatrième champ de ce dépôt à porter ce piège.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierEnteteEcranTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private StkItemRepository itemRepository;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private StkPresentationTypeRepository presentationTypeRepository;
    @Autowired
    private StkItemTypeRepository itemTypeRepository;
    @Autowired
    private StkItemCategoryRepository itemCategoryRepository;
    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;

    private static String suffixe() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private StkVocation vocation() {
        StkVocation v = new StkVocation();
        v.setId(UUID.randomUUID());
        v.setCode("VOC-ENT-" + suffixe());
        return vocationRepository.save(v);
    }

    private ComFolder dossierDeLaCapture() {
        ComFolder f = new ComFolder();
        f.setId(UUID.randomUUID());
        f.setCode("DOS-ENT-" + suffixe());
        f.setStkVocation(vocation());
        f.setUnitCost(new BigDecimal("13000"));
        f.setPriceHT(new BigDecimal("776250"));
        f.setPriceBeforeDiscountHT(new BigDecimal("747500"));
        f.setVATRatio(new BigDecimal("0.20"));
        f.setStatusValidityDate(LocalDateTime.of(2025, 12, 17, 0, 0));
        f.setPartPercent(BigDecimal.ONE);
        return folderRepository.save(f);
    }

    @Test
    void ficheDossier_exposeLesCasesDEnteteDeLEcran() throws Exception {
        ComFolder f = dossierDeLaCapture();

        mockMvc.perform(get("/api/dossiers/{id}", f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixM2").value(13000))
                .andExpect(jsonPath("$.prixHT").value(776250))
                .andExpect(jsonPath("$.prixAvantRemiseHT").value(747500))
                .andExpect(jsonPath("$.tauxTVA").value(0.20))
                .andExpect(jsonPath("$.dateValiditeStatut").value("2025-12-17T00:00:00"));
    }

    /**
     * La quote-part est une <b>fraction</b> : 1,00 signifie 100 %. L'écran légataire affiche
     * « 100.00 % » pour cette même valeur. Le test fige la fraction, pour que personne ne
     * « corrige » l'API en multipliant par cent côté serveur.
     */
    @Test
    void quotePart_estServieEnFractionPasEnPourcentage() throws Exception {
        ComFolder f = dossierDeLaCapture();

        mockMvc.perform(get("/api/dossiers/{id}", f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quotePart").value(1.0));
    }

    @Test
    void article_porteSonTitreFoncierEtSaPresentation() throws Exception {
        StkPresentationType presentation = new StkPresentationType();
        presentation.setId(UUID.randomUUID());
        presentation.setCode("C");
        presentation.setDescription("Présentation C");
        presentationTypeRepository.save(presentation);

        ComFolder f = dossierDeLaCapture();

        // StkItemType et StkItemCategory sont NOT NULL en base : un lot appartient toujours à une
        // typologie et à une catégorie. La fiche n'en dit rien ici, mais la fixture doit les poser.
        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-ENT-" + suffixe());
        type.setDescription("2 CHAMBRES + SALON");
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);

        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("CAT-ENT-" + suffixe());
        categorie.setDescription("APPARTEMENT");
        categorie.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(categorie);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("ART-ENT-" + suffixe());
        article.setDescription("Lot de la capture");
        article.setArea(new BigDecimal("69"));
        article.setTitleNumber("R/12345");
        article.setStkPresentationType(presentation);
        article.setStkItemType(type);
        article.setStkItemCategory(categorie);
        article.setStkVocation(vocation());
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        article.setComFolderId(f.getId());
        itemRepository.save(article);

        mockMvc.perform(get("/api/dossiers/{id}", f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.articles[0].titreFoncier").value("R/12345"))
                .andExpect(jsonPath("$.articles[0].presentationCode").value("C"))
                .andExpect(jsonPath("$.articles[0].presentationLibelle").value("Présentation C"));
    }

    @Test
    void echeancier_exposeLeDecompteEtLaDateDeCreation() throws Exception {
        ComFolder f = dossierDeLaCapture();

        ComDocument avecDecompte = new ComDocument();
        avecDecompte.setId(UUID.randomUUID());
        avecDecompte.setComFolder(f);
        avecDecompte.setAmount(new BigDecimal("16800"));
        avecDecompte.setQuittanceCode("25/00167");
        avecDecompte.setUserCreateDateTime(LocalDateTime.of(2025, 12, 10, 9, 0));
        documentRepository.saveAndFlush(avecDecompte);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier", f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].decompte").value("25/00167"))
                .andExpect(jsonPath("$.content[0].creeLe").value("2025-12-10T09:00:00"));
    }

    /**
     * Le piège de ce lot : en base, {@code QuittanceCode} est une chaîne vide, jamais {@code NULL}.
     * L'API doit rendre {@code null} — le logiciel de bureau affiche « N.A. », ce qui est un choix
     * d'affichage, pas une donnée.
     */
    /**
     * `POST /echeancier` ne posait pas `UserCreateDateTime` — nos propres échéances seraient donc
     * sorties avec un `creeLe` nul, alors que les 921 échéances du logiciel de bureau le portent
     * toutes. Corrigé en même temps que l'exposition du champ : livrer une date que notre chemin
     * d'écriture laisse vide reviendrait à livrer un champ cassé.
     */
    @Test
    void echeanceCreeeParLApi_porteSaDateDeCreation() throws Exception {
        ComFolder f = dossierDeLaCapture();

        mockMvc.perform(post("/api/dossiers/{id}/echeancier", f.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montant\":50000,\"dateEcheance\":\"2026-06-01T00:00:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creeLe").exists());
    }

    @Test
    void decompteVide_ressortEnNullJamaisEnChaineVide() throws Exception {
        ComFolder f = dossierDeLaCapture();

        ComDocument sansDecompte = new ComDocument();
        sansDecompte.setId(UUID.randomUUID());
        sansDecompte.setComFolder(f);
        sansDecompte.setAmount(new BigDecimal("75000"));
        sansDecompte.setQuittanceCode("");
        documentRepository.saveAndFlush(sansDecompte);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier", f.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].decompte").doesNotExist());
    }
}
