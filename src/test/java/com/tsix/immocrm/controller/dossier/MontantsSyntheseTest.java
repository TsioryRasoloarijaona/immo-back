package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Les deux lignes de synthèse de l'onglet « Montants » — voir
 * {@code instructions/FEATURE-fiche-dossier-alignement-ecran.md} §5.
 *
 * <p><b>Le scénario de référence est un dossier réel</b>, `ARS 2025/00357`, dont la capture de
 * l'écran légataire a servi à spécifier la feature : prix 931 500, frais syndic 16 800 exclus du
 * prix, deux avances encaissées pour 160 000. Le logiciel affiche « Prix de vente
 * 931 500 / 160 000 / 771 500 » et « Net à payer 948 300 / 160 000 / 788 300 ». Les tests
 * reproduisent ces chiffres : s'ils changent, c'est que l'API a cessé de dire ce que l'écran dit.
 *
 * <p><b>La règle des dossiers sortis est la plus facile à casser par mégarde.</b> Un dossier
 * annulé ne reste rien devoir : son net à payer se réduit à l'encaissé et son reste vaut zéro.
 * Un refactor « simplificateur » qui retirerait le cas particulier ferait apparaître un reste dû
 * sur trente dossiers annulés.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class MontantsSyntheseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private ComSettlementCategoryRepository settlementCategoryRepository;
    @Autowired
    private StkVocationRepository stkVocationRepository;
    @Autowired
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;
    @Autowired
    private StrStatusRepository strStatusRepository;

    private static String suffixe() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /** Dossier au prix stocké donné, éventuellement porteur d'un statut. */
    private ComFolder dossier(BigDecimal prix, String codeStatut) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-SYNT-" + suffixe());
        stkVocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-SYNT-" + suffixe());
        folder.setStkVocation(vocation);
        folder.setPrice(prix);
        if (codeStatut != null) {
            StrStatus statut = new StrStatus();
            statut.setId(UUID.randomUUID());
            statut.setCode(codeStatut);
            statut.setStrConfigEntityId(com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID);
            statut.setAnnulation("ANNULATION".equals(codeStatut));
            statut.setResiliation("DESISTEMENT".equals(codeStatut));
            folder.setStrStatus(strStatusRepository.save(statut));
        }
        return folderRepository.save(folder);
    }

    private ComSettlementCategory categorie(String code) {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode(code);
        return settlementCategoryRepository.save(c);
    }

    /** Échéance, et son règlement quand {@code regle} est non nul. */
    private void echeance(ComFolder folder, ComSettlementCategory cat, BigDecimal montant,
                          boolean exclureDuPrix, BigDecimal regle) {
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setComSettlementCategory(cat);
        doc.setAmount(montant);
        doc.setExcludeFromPrice(exclureDuPrix);
        documentRepository.save(doc);

        if (regle == null) {
            return;
        }
        StlSettlement s = new StlSettlement();
        s.setId(UUID.randomUUID());
        s.setAmount(regle);
        settlementRepository.save(s);

        StlSettlementLine l = new StlSettlementLine();
        l.setId(UUID.randomUUID());
        l.setStlSettlement(s);
        l.setComDocument(doc);
        l.setAmount(regle);
        settlementLineRepository.save(l);
    }

    /** Le dossier de la capture : prix 931 500, frais 16 800 exclus, 160 000 encaissés. */
    private ComFolder dossierDeLaCapture(String codeStatut) {
        ComFolder folder = dossier(new BigDecimal("931500"), codeStatut);
        ComSettlementCategory avance = categorie("AVANCE-SYNT-" + suffixe());
        ComSettlementCategory autoFin = categorie("AUTOFIN-SYNT-" + suffixe());
        ComSettlementCategory frais = categorie("FRAIS-SYNT-" + suffixe());

        echeance(folder, avance, new BigDecimal("75000"), false, new BigDecimal("75000"));
        echeance(folder, avance, new BigDecimal("85000"), false, new BigDecimal("85000"));
        echeance(folder, autoFin, new BigDecimal("771500"), false, null);
        echeance(folder, frais, new BigDecimal("16800"), true, null);
        return folder;
    }

    @Test
    void prixVente_reproduitLaLigneDeLEcranLegataire() throws Exception {
        ComFolder folder = dossierDeLaCapture("RESERVATION");

        mockMvc.perform(get("/api/dossiers/{id}/montants", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixVente.montant").value(931500))
                .andExpect(jsonPath("$.prixVente.regle").value(160000))
                .andExpect(jsonPath("$.prixVente.reste").value(771500));
    }

    @Test
    void netAPayer_ajouteLesFraisExclusDuPrix() throws Exception {
        ComFolder folder = dossierDeLaCapture("RESERVATION");

        mockMvc.perform(get("/api/dossiers/{id}/montants", folder.getId()))
                .andExpect(status().isOk())
                // 931 500 + 16 800 de frais syndic exclus du prix
                .andExpect(jsonPath("$.netAPayer.montant").value(948300))
                .andExpect(jsonPath("$.netAPayer.regle").value(160000))
                .andExpect(jsonPath("$.netAPayer.reste").value(788300))
                .andExpect(jsonPath("$.dossierSorti").value(false));
    }

    /**
     * Les frais sont exclus du <em>prix</em> mais comptent dans le <em>net à payer</em> : c'est
     * toute la différence entre les deux lignes, et l'erreur la plus probable d'un futur refactor.
     */
    @Test
    void lesFraisComptentDansLeNetAPayer_maisPasDansLePrixDeVente() throws Exception {
        ComFolder folder = dossierDeLaCapture("RESERVATION");

        mockMvc.perform(get("/api/dossiers/{id}/montants", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netAPayer.montant").value(948300))
                .andExpect(jsonPath("$.prixVente.montant").value(931500));
    }

    /**
     * Un règlement posé sur une échéance exclue du prix (frais) ne doit pas gonfler le réglé du
     * prix de vente, mais doit bien compter dans celui du net à payer.
     */
    @Test
    void unReglementDeFrais_neCompteQueDansLeNetAPayer() throws Exception {
        ComFolder folder = dossier(new BigDecimal("100000"), "RESERVATION");
        echeance(folder, categorie("PRIX-SYNT-" + suffixe()), new BigDecimal("100000"), false,
                new BigDecimal("30000"));
        echeance(folder, categorie("FRAIS-SYNT-" + suffixe()), new BigDecimal("5000"), true,
                new BigDecimal("5000"));

        mockMvc.perform(get("/api/dossiers/{id}/montants", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixVente.regle").value(30000))
                .andExpect(jsonPath("$.prixVente.reste").value(70000))
                .andExpect(jsonPath("$.netAPayer.montant").value(105000))
                .andExpect(jsonPath("$.netAPayer.regle").value(35000))
                .andExpect(jsonPath("$.netAPayer.reste").value(70000));
    }

    /**
     * <b>La règle métier à ne pas perdre.</b> Sur les 30 dossiers sortis réels, le net à payer
     * vaut exactement l'encaissé et le reste est nul — un dossier annulé ne reste rien devoir.
     * Sans ce cas particulier, l'API annoncerait 788 300 DH encore dus sur un dossier annulé.
     */
    @Test
    void dossierAnnule_netAPayerSeReduitALEncaisseEtLeResteEstNul() throws Exception {
        ComFolder folder = dossierDeLaCapture("ANNULATION");

        mockMvc.perform(get("/api/dossiers/{id}/montants", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dossierSorti").value(true))
                .andExpect(jsonPath("$.netAPayer.montant").value(160000))
                .andExpect(jsonPath("$.netAPayer.regle").value(160000))
                .andExpect(jsonPath("$.netAPayer.reste").value(0));
    }

    /** `DESISTEMENT` est l'autre sortie, et suit la même règle que `ANNULATION`. */
    @Test
    void dossierDesiste_suitLaMemeRegleQuUneAnnulation() throws Exception {
        ComFolder folder = dossierDeLaCapture("DESISTEMENT");

        mockMvc.perform(get("/api/dossiers/{id}/montants", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dossierSorti").value(true))
                .andExpect(jsonPath("$.netAPayer.reste").value(0));
    }

    /**
     * Le prix de vente, lui, n'est pas concerné par la sortie : il reste ce qu'il était. Seul le
     * net à payer porte la règle.
     */
    @Test
    void dossierAnnule_lePrixDeVenteResteInchange() throws Exception {
        ComFolder folder = dossierDeLaCapture("ANNULATION");

        mockMvc.perform(get("/api/dossiers/{id}/montants", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixVente.montant").value(931500))
                .andExpect(jsonPath("$.prixVente.reste").value(771500));
    }

    /**
     * Un statut hors référentiel ne doit pas faire tomber le calcul en 500 : `StatutDossier.fromCode`
     * lève, le service replie sur « non sorti ».
     */
    @Test
    void statutHorsReferentiel_neCassePasLeCalcul() throws Exception {
        ComFolder folder = dossierDeLaCapture("STATUT-INCONNU-" + suffixe());

        mockMvc.perform(get("/api/dossiers/{id}/montants", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dossierSorti").value(false))
                .andExpect(jsonPath("$.netAPayer.montant").value(948300));
    }

    /** Sans échéancier, les lignes de synthèse restent servies — le prix stocké fait foi. */
    @Test
    void dossierSansEcheancier_sertQuandMemeLesLignesDeSynthese() throws Exception {
        ComFolder folder = dossier(new BigDecimal("500000"), "RESERVATION");

        mockMvc.perform(get("/api/dossiers/{id}/montants", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixVente.montant").value(500000))
                .andExpect(jsonPath("$.prixVente.regle").value(0))
                .andExpect(jsonPath("$.netAPayer.montant").value(500000))
                .andExpect(jsonPath("$.netAPayer.reste").value(500000));
    }
}
