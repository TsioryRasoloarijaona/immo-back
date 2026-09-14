package com.tsix.immocrm.controller.encaissement;

import com.tsix.immocrm.domain.ColonneEncaissement;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sélecteur de colonnes et groupement de {@code GET /api/encaissements} — voir
 * {@code instructions/FEATURE-colonnes-groupement-encaissements.md}.
 *
 * <p>Les fixtures portent toutes un préfixe de référence distinct par test, et chaque requête
 * filtre dessus via {@code ?reference=} : c'est indispensable ici, la base de test étant partagée
 * avec les autres classes qui créent elles aussi des règlements.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EncaissementColonnesEtGroupementTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private StlSettlementTypeRepository settlementTypeRepository;
    @Autowired
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private ComDocument newEcheance(String suffix) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-CGE-" + suffix);
        vocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-CGE-" + suffix);
        folder.setStkVocation(vocation);
        folderRepository.save(folder);

        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setAmount(new BigDecimal("1000"));
        return documentRepository.save(doc);
    }

    /** Règlement rattaché à une échéance de vente — donc visible de la liste. */
    private StlSettlement newEncaissement(String reference, BigDecimal montant) {
        return newEncaissement(reference, montant, true);
    }

    private StlSettlement newEncaissement(String reference, BigDecimal montant, boolean credit) {
        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setCode(reference);
        settlement.setDocDate(LocalDateTime.now());
        settlement.setAmount(montant);
        settlement.setStatus("IN_WORK");
        settlement.setIsCredit(credit);
        settlement = settlementRepository.save(settlement);

        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(newEcheance(reference));
        line.setAmount(montant);
        settlementLineRepository.save(line);
        return settlement;
    }

    private StlSettlementType newType(String code, String libelle) {
        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode(code);
        type.setDescription(libelle);
        return settlementTypeRepository.save(type);
    }

    // ------------------------------------------------------------------
    // Sélecteur de colonnes
    // ------------------------------------------------------------------

    /**
     * Le contrat d'avant le lot, à la clé près.
     *
     * <p>C'est <strong>le</strong> test qui protège les intégrations existantes : sans
     * {@code ?colonnes=}, la réponse doit porter exactement les 27 champs historiques — ni un de
     * plus (les treize ajouts de ce lot doivent rester invisibles), ni un de moins.
     */
    @Test
    void sansParametreColonnes_laReponsePorteExactementLesColonnesHistoriques() throws Exception {
        StlSettlement settlement = newEncaissement("CGE-DEFAUT-1", new BigDecimal("1000"));
        settlement.setDescription("Libellé qui ne doit pas sortir par défaut");
        settlement.setUserCreate("agent.test");
        settlementRepository.save(settlement);

        var resultat = mockMvc.perform(get("/api/encaissements").param("reference", "CGE-DEFAUT-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reference").value("CGE-DEFAUT-1"))
                // Les ajouts du lot sont absents tant qu'on ne les demande pas.
                .andExpect(jsonPath("$.content[0].description").doesNotExist())
                .andExpect(jsonPath("$.content[0].creePar").doesNotExist())
                .andExpect(jsonPath("$.content[0].montantBase").doesNotExist())
                .andReturn();

        // Et le compte y est : exactement les colonnes déclarées parDefaut au catalogue.
        var json = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(resultat.getResponse().getContentAsString());
        var cles = new java.util.HashSet<String>();
        json.get("content").get(0).fieldNames().forEachRemaining(cles::add);
        assertThat(cles).isEqualTo(ColonneEncaissement.nomsParDefaut());
    }

    @Test
    void colonnes_neSerialiseQueCeQuiEstDemandeEtToujoursId() throws Exception {
        StlSettlement settlement = newEncaissement("CGE-SEL-1", new BigDecimal("1000"));
        settlement.setDescription("Chèque BMCE");
        settlementRepository.save(settlement);

        mockMvc.perform(get("/api/encaissements")
                        .param("reference", "CGE-SEL-")
                        .param("colonnes", "description,montantBrut"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(settlement.getId().toString()))
                .andExpect(jsonPath("$.content[0].description").value("Chèque BMCE"))
                .andExpect(jsonPath("$.content[0].montantBrut").value(1000))
                // Historique mais non demandée : absente.
                .andExpect(jsonPath("$.content[0].reference").doesNotExist())
                .andExpect(jsonPath("$.content[0].etat").doesNotExist());
    }

    /**
     * Les colonnes mesurées à 643/643 que le DTO ne servait pas — le cœur de l'apport du lot.
     */
    @Test
    void colonnes_serventLesChampsAjoutesParLeLot() throws Exception {
        CfgTier payeur = new CfgTier();
        payeur.setId(UUID.randomUUID());
        payeur.setCode("PAYEUR-CGE-1");
        payeur.setDescription("Société Payeuse SARL");
        payeur = tierRepository.save(payeur);

        StlSettlement settlement = newEncaissement("CGE-AJOUT-1", new BigDecimal("1000"));
        settlement.setDescription("Versement acompte");
        settlement.setAmountBase(new BigDecimal("1200.50"));
        settlement.setTierPayer(payeur);
        settlement.setUserCreate("s.amrani");
        settlement.setIsDcoumentDeliveredToCenter(true);
        settlement.setReceiptCode("RCU-42");
        settlementRepository.save(settlement);

        mockMvc.perform(get("/api/encaissements")
                        .param("reference", "CGE-AJOUT-")
                        .param("colonnes", "description,montantBase,tierPayeurNom,creePar,"
                                + "remisAuCentre,recuCode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Versement acompte"))
                .andExpect(jsonPath("$.content[0].montantBase").value(1200.50))
                .andExpect(jsonPath("$.content[0].tierPayeurNom").value("Société Payeuse SARL"))
                .andExpect(jsonPath("$.content[0].creePar").value("s.amrani"))
                .andExpect(jsonPath("$.content[0].remisAuCentre").value(true))
                .andExpect(jsonPath("$.content[0].recuCode").value("RCU-42"));
    }

    @Test
    void colonnes_surNomInconnu_rend400() throws Exception {
        mockMvc.perform(get("/api/encaissements").param("colonnes", "colonneQuiNExistePas"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Groupement
    // ------------------------------------------------------------------

    @Test
    void groupeParTypeLibelle_rendUnGroupeParTypeAvecEffectifEtSomme() throws Exception {
        StlSettlementType cheque = newType("TYPE-CGE-CHQ", "CHEQUE CGE");
        StlSettlementType virement = newType("TYPE-CGE-VIR", "VIREMENT CGE");

        for (var couple : new Object[][]{
                {"CGE-GRP-1", new BigDecimal("1000.00"), cheque},
                {"CGE-GRP-2", new BigDecimal("350.00"), cheque},
                {"CGE-GRP-3", new BigDecimal("950.00"), virement}}) {
            StlSettlement s = newEncaissement((String) couple[0], (BigDecimal) couple[1]);
            s.setStlSettlementType((StlSettlementType) couple[2]);
            settlementRepository.save(s);
        }

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrut")
                        .param("reference", "CGE-GRP-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='CHEQUE CGE')].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='CHEQUE CGE')].agregats.montantBrut").value(1350.00))
                .andExpect(jsonPath("$.content[?(@.cle=='VIREMENT CGE')].nombre").value(1))
                .andExpect(jsonPath("$.content[?(@.cle=='VIREMENT CGE')].agregats.montantBrut").value(950.00));
    }

    /**
     * Le scénario du bug n°2 du journal de session, rejoué sur ce catalogue : un
     * {@code COALESCE(SUM(...), 0)} en SQL tirerait le type du résultat vers l'échelle du littéral
     * entier et arrondirait la somme à l'unité, en silence. 190,75 rend l'arrondi visible dans les
     * deux sens.
     */
    @Test
    void sommeDesMontants_conserveLesCentimes() throws Exception {
        StlSettlementType type = newType("TYPE-CGE-CTS", "CENTIMES CGE");
        for (var couple : new Object[][]{
                {"CGE-CTS-1", new BigDecimal("100.50")},
                {"CGE-CTS-2", new BigDecimal("90.25")}}) {
            StlSettlement s = newEncaissement((String) couple[0], (BigDecimal) couple[1]);
            s.setStlSettlementType(type);
            settlementRepository.save(s);
        }

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrut")
                        .param("reference", "CGE-CTS-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].agregats.montantBrut").value(190.75));
    }

    /**
     * L'invariant du §5 : si ce test casse, c'est que {@code specEncaissements} et le groupement
     * ont divergé — un filtre appliqué d'un seul côté, ou un {@code INNER JOIN} implicite qui
     * écarte les lignes à association nulle.
     */
    @Test
    void sommeDesEffectifs_egaleLeTotalDeLaListe() throws Exception {
        StlSettlementType a = newType("TYPE-CGE-INVA", "INV-A CGE");
        StlSettlementType b = newType("TYPE-CGE-INVB", "INV-B CGE");

        StlSettlement s1 = newEncaissement("CGE-INV-1", new BigDecimal("100.00"));
        s1.setStlSettlementType(a);
        settlementRepository.save(s1);
        StlSettlement s2 = newEncaissement("CGE-INV-2", new BigDecimal("200.00"));
        s2.setStlSettlementType(a);
        settlementRepository.save(s2);
        StlSettlement s3 = newEncaissement("CGE-INV-3", new BigDecimal("300.00"));
        s3.setStlSettlementType(b);
        settlementRepository.save(s3);
        // Sans type : le groupe à clé nulle, celui que COUNT(DISTINCT) ne compte pas.
        newEncaissement("CGE-INV-4", new BigDecimal("400.00"));

        mockMvc.perform(get("/api/encaissements").param("reference", "CGE-INV-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrut")
                        .param("inclureSansValeur", "true")
                        .param("reference", "CGE-INV-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[?(@.cle=='INV-A CGE')].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle=='INV-B CGE')].nombre").value(1))
                .andExpect(jsonPath("$.content[?(@.cle == null)].nombre").value(1))
                .andExpect(jsonPath("$.content[?(@.cle == null)].agregats.montantBrut").value(400.00));
    }

    /**
     * La restriction {@code IsCredit = 1} doit valoir des deux côtés. Si elle ne s'appliquait qu'à
     * la liste, un décaissement gonflerait un total de groupe sans jamais apparaître dans les
     * lignes qu'on peut ouvrir — le genre d'écart qui ne se voit pas.
     */
    @Test
    void decaissement_resteExcluDuGroupementAussi() throws Exception {
        StlSettlementType type = newType("TYPE-CGE-DEC", "DECAISS CGE");

        StlSettlement encaissement = newEncaissement("CGE-DEC-1", new BigDecimal("500.00"));
        encaissement.setStlSettlementType(type);
        settlementRepository.save(encaissement);

        StlSettlement decaissement = newEncaissement("CGE-DEC-2", new BigDecimal("-470.00"), false);
        decaissement.setStlSettlementType(type);
        settlementRepository.save(decaissement);

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrut")
                        .param("reference", "CGE-DEC-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.cle=='DECAISS CGE')].nombre").value(1))
                .andExpect(jsonPath("$.content[?(@.cle=='DECAISS CGE')].agregats.montantBrut").value(500.00));
    }

    @Test
    void depliage_rendExactementLesLignesComptees() throws Exception {
        StlSettlementType vise = newType("TYPE-CGE-VISE", "VISE CGE");
        StlSettlementType autre = newType("TYPE-CGE-AUTRE", "AUTRE CGE");

        StlSettlement dedans = newEncaissement("CGE-DEP-1", new BigDecimal("100.00"));
        dedans.setStlSettlementType(vise);
        settlementRepository.save(dedans);
        StlSettlement dehors = newEncaissement("CGE-DEP-2", new BigDecimal("200.00"));
        dehors.setStlSettlementType(autre);
        settlementRepository.save(dehors);

        mockMvc.perform(get("/api/encaissements")
                        .param("reference", "CGE-DEP-")
                        .param("groupeColonne", "typeLibelle")
                        .param("groupeValeur", "VISE CGE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(dedans.getId().toString()));
    }

    @Test
    void depliage_sansValeur_ouvreLeGroupeDesLignesSansValeur() throws Exception {
        StlSettlement sansType = newEncaissement("CGE-DEPV-1", new BigDecimal("100.00"));
        StlSettlement avecType = newEncaissement("CGE-DEPV-2", new BigDecimal("200.00"));
        avecType.setStlSettlementType(newType("TYPE-CGE-DEPV", "DEPV CGE"));
        settlementRepository.save(avecType);

        mockMvc.perform(get("/api/encaissements")
                        .param("reference", "CGE-DEPV-")
                        .param("groupeColonne", "typeLibelle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(sansType.getId().toString()));
    }

    @Test
    void groupeParDate_regroupeAuMoisParDefaut() throws Exception {
        StlSettlement janvierA = newEncaissement("CGE-DATE-1", new BigDecimal("100.00"));
        janvierA.setDocDate(LocalDateTime.of(2025, 1, 5, 10, 0));
        settlementRepository.save(janvierA);
        StlSettlement janvierB = newEncaissement("CGE-DATE-2", new BigDecimal("200.00"));
        janvierB.setDocDate(LocalDateTime.of(2025, 1, 22, 16, 30));
        settlementRepository.save(janvierB);
        StlSettlement fevrier = newEncaissement("CGE-DATE-3", new BigDecimal("300.00"));
        fevrier.setDocDate(LocalDateTime.of(2025, 2, 3, 9, 0));
        settlementRepository.save(fevrier);

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "dateDocument")
                        .param("reference", "CGE-DATE-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.cle==202501)].nombre").value(2))
                .andExpect(jsonPath("$.content[?(@.cle==202501)].libelle").value("janvier 2025"))
                .andExpect(jsonPath("$.content[?(@.cle==202502)].nombre").value(1));
    }

    // ------------------------------------------------------------------
    // Refus
    // ------------------------------------------------------------------

    /**
     * Le refus structurel du §5.2 : le dossier se résout par les lignes du règlement, une
     * association to-many. Grouper dessus compterait plusieurs fois les règlements qui portent sur
     * plusieurs dossiers (8 sur 643 en données réelles) et la somme des effectifs ne retomberait
     * plus sur le total de la liste. Un 400 vaut mieux qu'un total faux.
     */
    @Test
    void groupBy_surDossier_rend400() throws Exception {
        mockMvc.perform(get("/api/encaissements/groupes").param("groupBy", "dossierCode"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/encaissements/groupes").param("groupBy", "dossierId"))
                .andExpect(status().isBadRequest());
    }

    /** Calculés en Java, absents de la base : ni groupables ni sommables. */
    @Test
    void groupBy_surMontantCalcule_rend400() throws Exception {
        mockMvc.perform(get("/api/encaissements/groupes").param("groupBy", "montantBrutRestant"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantNet"))
                .andExpect(status().isBadRequest());
    }

    /** {@code nvarchar(max)} : le {@code COUNT(DISTINCT)} du comptage de groupes le refuse. */
    @Test
    void groupBy_surNotes_rend400() throws Exception {
        mockMvc.perform(get("/api/encaissements/groupes").param("groupBy", "notes"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void groupBy_surColonneInconnue_rend400() throws Exception {
        mockMvc.perform(get("/api/encaissements/groupes").param("groupBy", "colonneQuiNExistePas"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void groupBy_absent_rend400() throws Exception {
        mockMvc.perform(get("/api/encaissements/groupes"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void groupeValeur_sansGroupeColonne_rend400() throws Exception {
        mockMvc.perform(get("/api/encaissements").param("groupeValeur", "CHEQUE"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Catalogue
    // ------------------------------------------------------------------

    @Test
    void catalogue_publieLesBooleensDeGroupementEtEcarteLesColonnesMesureesVides() throws Exception {
        mockMvc.perform(get("/api/referentiels/colonnes/encaissements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nom=='description')].groupable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='description')].parDefaut").value(false))
                .andExpect(jsonPath("$[?(@.nom=='montantBrut')].agregeable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='dossierCode')].groupable").value(false))
                .andExpect(jsonPath("$[?(@.nom=='montantNet')].groupable").value(false))
                .andExpect(jsonPath("$[?(@.nom=='notes')].groupable").value(false))
                // Mesurées à 0/643 ou à une seule valeur distincte : jamais au catalogue.
                .andExpect(jsonPath("$[?(@.nom=='group1Label')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.nom=='categorie')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.nom=='itemNumber')]").doesNotExist());
    }
}
