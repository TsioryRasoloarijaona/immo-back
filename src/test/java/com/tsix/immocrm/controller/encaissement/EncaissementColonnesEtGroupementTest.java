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
import jakarta.persistence.EntityManager;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    @Autowired
    private EntityManager entityManager;

    /**
     * Vide le contexte de persistance avant l'appel HTTP.
     *
     * <p>Indispensable dès qu'un test lit une valeur portée par la base plutôt que par le code :
     * une {@code @Formula} n'est peuplée qu'au <strong>chargement</strong>, et une association vers
     * un {@code @Subselect} n'est résolue qu'à ce moment-là. Sans ce vidage, la requête rend
     * l'instance déjà gérée — celle que le test vient de construire, dont ces champs sont nuls.
     *
     * <p>Ce n'est pas un artefact gênant en production : chaque requête HTTP y ouvre sa propre
     * transaction et charge donc toujours des entités fraîches. C'est le test, transactionnel de
     * bout en bout, qui est le cas particulier. Même remède qu'au lot des délais de cycle.
     */
    private void relireDepuisLaBase() {
        entityManager.flush();
        entityManager.clear();
    }

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

    /** D1 : un règlement annulé reste compté dans son groupe, mais sort de toutes les sommes de montants. */
    @Test
    void reglementAnnule_compteDansLeGroupeMaisHorsDesSommes() throws Exception {
        StlSettlementType type = newType("TYPE-CGE-ANN", "ANNULE CGE");
        StlSettlement valide = newEncaissement("CGE-ANN-1", new BigDecimal("300.00"));
        valide.setStlSettlementType(type);
        settlementRepository.save(valide);
        StlSettlement annule = newEncaissement("CGE-ANN-2", new BigDecimal("700.00"));
        annule.setStlSettlementType(type);
        annule.setStatus("CANCELED");
        settlementRepository.save(annule);
        relireDepuisLaBase();

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrut,montantNet,montantBrutRestant,montantBrut:AVG,"
                                + "montantBrut:COUNT")
                        .param("reference", "CGE-ANN-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value(2))
                .andExpect(jsonPath("$.content[0].agregats.montantBrut").value(300.00))
                .andExpect(jsonPath("$.content[0].agregats.montantNet").value(300.00))
                .andExpect(jsonPath("$.content[0].agregats.montantBrutRestant").value(0))
                .andExpect(jsonPath("$.content[0].agregats['montantBrut:AVG']").value(300.00))
                .andExpect(jsonPath("$.content[0].agregats['montantBrut:COUNT']").value(2))
                .andExpect(jsonPath("$.totaux.montantBrut").value(300.00))
                .andExpect(jsonPath("$.nombreTotal").value(2));
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

    /**
     * {@code montantBrutRestant} est sommable mais <strong>pas groupable</strong> : sa valeur est
     * une différence entre le montant et une pré-agrégation jointe, et le moteur agrège les deux
     * termes séparément — ce qui donne un total juste et ne donne aucune clé de groupe.
     */
    @Test
    void groupBy_surMontantBrutRestant_rend400() throws Exception {
        mockMvc.perform(get("/api/encaissements/groupes").param("groupBy", "montantBrutRestant"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Et ni {@code MIN} ni {@code MAX} dessus : {@code MIN(a - b)} ne se déduit pas de
     * {@code MIN(a)} et {@code MIN(b)}. Le total et la moyenne, eux, restent exacts — la
     * soustraction est linéaire.
     */
    @Test
    void extremumSurUneDifference_rend400() throws Exception {
        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrutRestant:MIN"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrutRestant:MAX"))
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
    // La bande « Total : » de l'écran légataire
    // ------------------------------------------------------------------

    /**
     * Reproduit littéralement la capture de l'écran <em>Encaissements Vente</em> : la grille y est
     * groupée par <strong>Montant brut</strong>, chaque en-tête annonce
     * « Montant brut : 8 400.00 DH (14 items) », et la bande jaune juste en dessous totalise deux
     * colonnes — « Montant brut rest » à 0 DH et « Montant net » à 117 600 DH.
     *
     * <p>Ce sont exactement les deux colonnes que le lot précédent avait écartées, faute de les
     * avoir en base. Si ce test casse, c'est que la bande « Total : » a cessé d'être calculable.
     */
    @Test
    void totauxDeGroupe_reproduisentLaBandeTotalDuLegataire() throws Exception {
        BigDecimal montant = new BigDecimal("8400.00");
        for (int i = 1; i <= 3; i++) {
            newEncaissement("CGE-IMG-" + i, montant);
        }

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "montantBrut")
                        .param("agregats", "montantBrutRestant,montantNet")
                        .param("reference", "CGE-IMG-"))
                .andExpect(status().isOk())
                // Un seul groupe : toutes les pièces portent le même montant brut.
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value(3))
                // « Montant net » : 3 x 8 400, les deux retenues étant nulles.
                .andExpect(jsonPath("$.content[0].agregats.montantNet").value(25200.00))
                // « Montant brut rest » : les pièces sont intégralement affectées, donc zéro —
                // ce que la capture montre aussi, ligne à ligne.
                .andExpect(jsonPath("$.content[0].agregats.montantBrutRestant").value(0))
                // Et le total général, que le légataire ne sait obtenir qu'en chargeant tout.
                .andExpect(jsonPath("$.totaux.montantNet").value(25200.00))
                .andExpect(jsonPath("$.nombreTotal").value(3));
    }

    /**
     * La ligne et son total viennent de la même expression.
     *
     * <p>{@code montantBrutRestant} est calculé ligne à ligne depuis l'association pré-agrégée et
     * totalisé par {@code SUM(amount) - SUM(affecte)}. Un total qui ne retombe pas sur la somme de
     * ses propres lignes est le défaut d'écran le plus difficile à voir — d'où ce test qui compare
     * les deux bouts sur une pièce partiellement affectée.
     */
    @Test
    void ligneEtTotal_saccordentSurLeMontantRestant() throws Exception {
        StlSettlement partiel = newEncaissement("CGE-ACC-1", new BigDecimal("1000.00"));
        StlSettlementLine ligne = settlementLineRepository.findAll().stream()
                .filter(l -> l.getStlSettlement() != null
                        && l.getStlSettlement().getId().equals(partiel.getId()))
                .findFirst().orElseThrow();
        ligne.setAmount(new BigDecimal("400.00"));
        settlementLineRepository.save(ligne);
        relireDepuisLaBase();

        mockMvc.perform(get("/api/encaissements").param("reference", "CGE-ACC-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].montantBrutRestant").value(600.00));

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "montantBrut")
                        .param("agregats", "montantBrutRestant")
                        .param("reference", "CGE-ACC-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].agregats.montantBrutRestant").value(600.00));
    }

    // ------------------------------------------------------------------
    // Le menu Σ
    // ------------------------------------------------------------------

    /**
     * Un nom nu vaut toujours une somme et revient sous ce nom nu.
     *
     * <p>C'est la preuve de non-rupture de l'ouverture du menu Σ : une intégration écrite avant ce
     * lot envoie des noms nus et doit retrouver exactement les mêmes clés.
     */
    @Test
    void nomNu_resteUneSommeSousLaMemeCle() throws Exception {
        StlSettlementType type = newType("TYPE-CGE-NU", "NU CGE");
        for (Object[] couple : new Object[][]{{"CGE-NU-1", new BigDecimal("100.00")},
                                              {"CGE-NU-2", new BigDecimal("300.00")}}) {
            StlSettlement s = newEncaissement((String) couple[0], (BigDecimal) couple[1]);
            s.setStlSettlementType(type);
            settlementRepository.save(s);
        }

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrut")
                        .param("reference", "CGE-NU-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].agregats.montantBrut").value(400.00))
                .andExpect(jsonPath("$.content[0].agregats['montantBrut:SUM']").doesNotExist());
    }

    /** Les cinq fonctions sur une colonne qui les autorise, dans le même appel. */
    @Test
    void menuSigma_appliqueChaqueFonctionSousSonPropreJeton() throws Exception {
        StlSettlementType type = newType("TYPE-CGE-SIG", "SIGMA CGE");
        for (Object[] couple : new Object[][]{{"CGE-SIG-1", new BigDecimal("100.00")},
                                              {"CGE-SIG-2", new BigDecimal("200.00")},
                                              {"CGE-SIG-3", new BigDecimal("600.00")}}) {
            StlSettlement s = newEncaissement((String) couple[0], (BigDecimal) couple[1]);
            s.setStlSettlementType(type);
            settlementRepository.save(s);
        }

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrut,montantBrut:AVG,montantBrut:MIN,"
                                + "montantBrut:MAX,montantBrut:COUNT")
                        .param("reference", "CGE-SIG-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].agregats.montantBrut").value(900.00))
                .andExpect(jsonPath("$.content[0].agregats['montantBrut:AVG']").value(300.00))
                .andExpect(jsonPath("$.content[0].agregats['montantBrut:MIN']").value(100.00))
                .andExpect(jsonPath("$.content[0].agregats['montantBrut:MAX']").value(600.00))
                .andExpect(jsonPath("$.content[0].agregats['montantBrut:COUNT']").value(3));
    }

    /**
     * La moyenne ne passe jamais par un {@code double} — le pendant de
     * {@link #sommeDesMontants_conserveLesCentimes} pour la division.
     *
     * <p>0,615 n'est pas représentable en binaire : il vaut 0,614999… en {@code double}. Trois
     * pièces à ce montant font 1,845 exactement en décimal, donc une moyenne de 0,615, qui
     * s'arrondit au centime supérieur à <strong>0,62</strong>. Un {@code cb.avg()} aurait rendu
     * 0,614999… et donc 0,61. C'est un centime, et c'est le genre d'écart qui ne se remarque que
     * sur un rapprochement comptable.
     */
    @Test
    void moyenne_neDerivePasEnVirguleFlottante() throws Exception {
        StlSettlementType type = newType("TYPE-CGE-AVG", "AVG CGE");
        for (int i = 1; i <= 3; i++) {
            StlSettlement s = newEncaissement("CGE-AVG-" + i, new BigDecimal("0.615"));
            s.setStlSettlementType(type);
            settlementRepository.save(s);
        }

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrut,montantBrut:AVG")
                        .param("reference", "CGE-AVG-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].agregats.montantBrut").value(1.845))
                .andExpect(jsonPath("$.content[0].agregats['montantBrut:AVG']").value(0.62));
    }

    /** Une date offre un extremum, jamais une somme : additionner des dates n'a pas de sens. */
    @Test
    void dates_offrentMinEtMaxMaisPasLaSomme() throws Exception {
        StlSettlementType type = newType("TYPE-CGE-DT", "DATES CGE");
        StlSettlement ancien = newEncaissement("CGE-DT-1", new BigDecimal("100.00"));
        ancien.setStlSettlementType(type);
        ancien.setDocDate(LocalDateTime.of(2025, 1, 5, 10, 0));
        settlementRepository.save(ancien);
        StlSettlement recent = newEncaissement("CGE-DT-2", new BigDecimal("200.00"));
        recent.setStlSettlementType(type);
        recent.setDocDate(LocalDateTime.of(2025, 3, 20, 16, 30));
        settlementRepository.save(recent);

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "dateDocument:MIN,dateDocument:MAX")
                        .param("reference", "CGE-DT-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].agregats['dateDocument:MIN']")
                        .value(Matchers.startsWith("2025-01-05")))
                .andExpect(jsonPath("$.content[0].agregats['dateDocument:MAX']")
                        .value(Matchers.startsWith("2025-03-20")));

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "dateDocument"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fonctionInconnue_rend400AvecLesFonctionsAcceptees() throws Exception {
        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrut:MEDIANE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fonctionsValides",
                        Matchers.hasItems("SUM", "AVG", "MIN", "MAX", "COUNT")));
    }

    /** Une colonne sans chemin n'est agrégeable par aucune fonction. */
    @Test
    void agregatSurColonneSansChemin_rend400() throws Exception {
        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "dossierCode:COUNT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fonctionsValides").isEmpty());
    }

    // ------------------------------------------------------------------
    // Total général
    // ------------------------------------------------------------------

    /**
     * Le total général porte sur tout le périmètre filtré, pas sur la page.
     *
     * <p>Vérifié en ne demandant qu'un seul groupe par page alors qu'il y en a trois : les totaux
     * doivent quand même couvrir les trois, sans quoi un front qui affiche un pied de grille
     * annoncerait un chiffre plausible et faux.
     */
    @Test
    void totalGeneral_couvreToutLePerimetreEtPasLaPage() throws Exception {
        StlSettlementType a = newType("TYPE-CGE-TGA", "TG-A CGE");
        StlSettlementType b = newType("TYPE-CGE-TGB", "TG-B CGE");

        StlSettlement s1 = newEncaissement("CGE-TG-1", new BigDecimal("100.00"));
        s1.setStlSettlementType(a);
        settlementRepository.save(s1);
        StlSettlement s2 = newEncaissement("CGE-TG-2", new BigDecimal("200.00"));
        s2.setStlSettlementType(b);
        settlementRepository.save(s2);
        // Sans type : le groupe à clé nulle, qu'il faut inclure pour que les deux comptes
        // désignent la même population.
        newEncaissement("CGE-TG-3", new BigDecimal("400.00"));

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrut")
                        .param("inclureSansValeur", "true")
                        .param("reference", "CGE-TG-")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                // Une seule ligne servie, mais les totaux portent sur les trois groupes.
                .andExpect(jsonPath("$.totaux.montantBrut").value(700.00))
                .andExpect(jsonPath("$.nombreTotal").value(3));
    }

    /**
     * Et il suit {@code inclureSansValeur} : le groupe sans valeur entre ou sort des deux comptes
     * à la fois, jamais d'un seul.
     */
    @Test
    void totalGeneral_suitInclureSansValeur() throws Exception {
        StlSettlementType type = newType("TYPE-CGE-ISV", "ISV CGE");
        StlSettlement avecType = newEncaissement("CGE-ISV-1", new BigDecimal("100.00"));
        avecType.setStlSettlementType(type);
        settlementRepository.save(avecType);
        newEncaissement("CGE-ISV-2", new BigDecimal("900.00"));

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrut")
                        .param("reference", "CGE-ISV-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaux.montantBrut").value(100.00))
                .andExpect(jsonPath("$.nombreTotal").value(1));

        mockMvc.perform(get("/api/encaissements/groupes")
                        .param("groupBy", "typeLibelle")
                        .param("agregats", "montantBrut")
                        .param("inclureSansValeur", "true")
                        .param("reference", "CGE-ISV-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaux.montantBrut").value(1000.00))
                .andExpect(jsonPath("$.nombreTotal").value(2));
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
                // Portée par une @Formula depuis ce lot : groupable ET agrégeable.
                .andExpect(jsonPath("$[?(@.nom=='montantNet')].groupable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='montantNet')].agregeable").value(true))
                // Sommable par jointure, mais sans clé de groupe exprimable.
                .andExpect(jsonPath("$[?(@.nom=='montantBrutRestant')].groupable").value(false))
                .andExpect(jsonPath("$[?(@.nom=='montantBrutRestant')].agregeable").value(true))
                .andExpect(jsonPath("$[?(@.nom=='notes')].groupable").value(false))
                // Mesurées à 0/643 ou à une seule valeur distincte : jamais au catalogue.
                .andExpect(jsonPath("$[?(@.nom=='group1Label')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.nom=='categorie')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.nom=='itemNumber')]").doesNotExist());
    }

    /**
     * Le menu Σ se pilote sur {@code fonctions}, jamais sur une liste en dur — et cette liste n'est
     * pas la même d'une colonne à l'autre.
     *
     * <p>Assertions en Java plutôt qu'en JSONPath : un filtre JSONPath rend une liste de listes,
     * que les matchers Hamcrest traversent de façon ambiguë. Ici la lecture est directe.
     */
    @Test
    void catalogue_publieLesFonctionsAutoriseesParColonne() throws Exception {
        String corps = mockMvc.perform(get("/api/referentiels/colonnes/encaissements"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode catalogue = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(corps);

        Map<String, List<String>> fonctionsParColonne = new HashMap<>();
        for (JsonNode colonne : catalogue) {
            List<String> fonctions = new ArrayList<>();
            colonne.get("fonctions").forEach(f -> fonctions.add(f.asText()));
            fonctionsParColonne.put(colonne.get("nom").asText(), fonctions);
        }

        // Un montant : les cinq.
        assertThat(fonctionsParColonne.get("montantBrut"))
                .containsExactly("SUM", "AVG", "MIN", "MAX", "COUNT");
        // Une date : extremums et comptage, mais sommer des dates n'a pas de sens.
        assertThat(fonctionsParColonne.get("dateDocument"))
                .containsExactly("MIN", "MAX", "COUNT");
        // Une différence : ni minimum ni maximum, le moteur agrège ses deux termes séparément.
        assertThat(fonctionsParColonne.get("montantBrutRestant"))
                .containsExactly("SUM", "AVG", "COUNT");
        // Une colonne sans chemin n'existe pas en base : rien à agréger.
        assertThat(fonctionsParColonne.get("dossierCode")).isEmpty();
        // Et le booléen historique reste vrai exactement quand SUM est offert.
        assertThat(fonctionsParColonne.get("etat")).containsExactly("COUNT");
    }

    // ------------------------------------------------------------------
    // Lot 2 : totaux de la liste plate, pointage prévu
    // ------------------------------------------------------------------

    /** B1 : les totaux du filtre égalent la somme de toutes les pages, CANCELED compté mais hors des sommes. */
    @Test
    void totauxListePlate_egalentLaSommeDeToutesLesPagesHorsAnnules() throws Exception {
        newEncaissement("CGE-TOT-1", new BigDecimal("1000.50"));
        newEncaissement("CGE-TOT-2", new BigDecimal("2000.25"));
        StlSettlement annule = newEncaissement("CGE-TOT-3", new BigDecimal("4000.00"));
        annule.setStatus("CANCELED");
        settlementRepository.save(annule);
        relireDepuisLaBase();

        BigDecimal sommePages = BigDecimal.ZERO;
        for (int page = 0; page < 3; page++) {
            String corps = mockMvc.perform(get("/api/encaissements").param("reference", "CGE-TOT-")
                            .param("size", "1").param("page", String.valueOf(page))
                            .param("colonnes", "montantBrut,etat"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode ligne = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                    .readTree(corps).get("content").get(0);
            if (!"CANCELED".equals(ligne.get("etat").asText())) {
                sommePages = sommePages.add(ligne.get("montantBrut").decimalValue());
            }
        }

        mockMvc.perform(get("/api/encaissements/totaux").param("reference", "CGE-TOT-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(3))
                .andExpect(jsonPath("$.montantBrut").value(sommePages.doubleValue()))
                .andExpect(jsonPath("$.montantBrut").value(3000.75))
                .andExpect(jsonPath("$.montantNet").value(3000.75))
                .andExpect(jsonPath("$.montantBrutRestant").value(0));
    }

    /**
     * B2 : {@code MotionDate} de {@code StlSettlementView} — un IN_WORK échu passe à aujourd'hui, un pointé garde sa
     * date de règlement, une échéance à venir reste telle quelle.
     */
    @Test
    void pointagePrevu_reporteAAujourdhuiUnReglementEchuNonPointe() throws Exception {
        // Dates dans la plage smalldatetime de la base de CI (1900-01-01 .. 2079-06-06).
        // « Aujourd'hui » de SQL Server (la colonne lit GETDATE()) : en CI la JVM peut avoir déjà changé de jour.
        java.time.LocalDate jour = aujourdhuiSelonLaBase();
        StlSettlement echu = newEncaissement("CGE-PTG-1", new BigDecimal("100.00"));
        echu.setEcheanceDate(jour.minusDays(10).atStartOfDay());
        settlementRepository.save(echu);
        StlSettlement pointe = newEncaissement("CGE-PTG-2", new BigDecimal("200.00"));
        pointe.setStatus("POINTED");
        pointe.setEcheanceDate(LocalDateTime.of(2024, 3, 1, 0, 0));
        pointe.setSettlementDate(LocalDateTime.of(2024, 3, 5, 0, 0));
        settlementRepository.save(pointe);
        StlSettlement aVenir = newEncaissement("CGE-PTG-3", new BigDecimal("300.00"));
        java.time.LocalDate futur = jour.plusYears(5);
        aVenir.setEcheanceDate(futur.atStartOfDay());
        settlementRepository.save(aVenir);
        relireDepuisLaBase();
        String aujourdhui = jour.toString();

        mockMvc.perform(get("/api/encaissements").param("reference", "CGE-PTG-")
                        .param("colonnes", "reference,pointagePrevu").param("sort", "pointagePrevu,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reference").value("CGE-PTG-2"))
                .andExpect(jsonPath("$.content[0].pointagePrevu").value(Matchers.startsWith("2024-03-05")))
                .andExpect(jsonPath("$.content[1].reference").value("CGE-PTG-1"))
                .andExpect(jsonPath("$.content[1].pointagePrevu").value(Matchers.startsWith(aujourdhui)))
                .andExpect(jsonPath("$.content[2].pointagePrevu").value(Matchers.startsWith(futur.toString())));

        mockMvc.perform(get("/api/encaissements").param("reference", "CGE-PTG-")
                        .param("champDate", "POINTAGE_PREVU")
                        .param("dateDebut", futur + "T00:00:00").param("dateFin", futur + "T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].reference").value("CGE-PTG-3"));
    }

    private java.time.LocalDate aujourdhuiSelonLaBase() {
        Object jour = entityManager.createNativeQuery("SELECT CONVERT(char(10), GETDATE(), 23)").getSingleResult();
        return java.time.LocalDate.parse(jour.toString().trim());
    }
}
