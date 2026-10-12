package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmUserHabilitation;
import com.tsix.immocrm.entities.adm.AdmUserRole;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentStatus;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketLine;
import com.tsix.immocrm.entities.prj.PrjMarketLineGrouping;
import com.tsix.immocrm.entities.prj.PrjMarketType;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLine;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLineExtra;
import com.tsix.immocrm.entities.prj.PrjWorkingOutRetainedLine;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Chaîne Chantier complète et valide pour les FK du schéma CI : projet → lots → bordereau → décomptes →
 * factures → règlements (FEATURE-projet-chantier-lecture.md §3.3). Montants stockés selon la sémantique
 * mesurée en production ; chaque appel crée des lignes neuves (codes suffixés), annulées par le test.
 */
@Component
public class FixtureChantier {

    /** PKey réelles des entités de configuration (StrEntity), identiques en production. */
    public static final UUID ENTITE_PROJET = UUID.fromString("C8D5550A-936C-414A-814D-0D6B57074613");
    public static final UUID ENTITE_LOT = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B613");

    private final EntityManager em;
    private final JdbcTemplate jdbc;

    public FixtureChantier(EntityManager em, JdbcTemplate jdbc) {
        this.em = em;
        this.jdbc = jdbc;
    }

    /** Ce que les tests lisent ; tous les montants attendus sont recalculables depuis ces lignes. */
    public record Chaine(PrjProject projetA, PrjProject projetB, PrjProject centreDeCouts,
                         PrjMarket lotBordereau, PrjMarket lotForfait, PrjMarket lotVide, PrjMarket lotB,
                         PrjWorkingOut decompte1, PrjWorkingOut decompte2, PrjWorkingOut decompteExtras,
                         PrjWorkingOut decompteB, BuyDocument facture1, BuyDocument factureExtras,
                         UUID statutLot, UUID typeLot, UUID statutFactureValide, CfgTier fournisseur,
                         CfgTier fournisseurB, StlCategory categorie, StlCategory categorieB) {
    }

    public Chaine creer() {
        String s = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        entite(ENTITE_PROJET, "PrjProject", "Projet");
        entite(ENTITE_LOT, "PrjMarket", "Lot Entrepreneurs");
        UUID typeStatut = UUID.randomUUID();
        jdbc.update("INSERT INTO dbo.StrStatusType (PKey, Code, Description, Sorting) VALUES (?, ?, ?, 20)",
                typeStatut.toString(), "Intermédiaire", "Intermédiaire");
        UUID statutProjet = statut(ENTITE_PROJET, "NOUVEAU-" + s, 10, null);
        UUID statutLot = statut(ENTITE_LOT, "ADJUDICATION-" + s, 40, typeStatut);

        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("STE-CH-" + s);
        societe.setDescription("Société chantier " + s);
        societe.setIsActive(true);
        em.persist(societe);
        CfgTier fournisseur = tiers("FRN-CH-" + s, "WASSILA TEST " + s);
        CfgTier fournisseurB = tiers("FRN-CHB-" + s, "TOREE TEST " + s);
        StlCategory categorie = categorie("CAT-CH-" + s);
        StlCategory categorieB = categorie("CAT-CHB-" + s);

        PrjMarketType type = new PrjMarketType();
        type.setId(UUID.randomUUID());
        type.setCode("20" + s);
        type.setDescription("TRAVAUX");
        em.persist(type);

        PrjProject projetA = projet("PRJ-A-" + s, societe, statutProjet);
        PrjProject projetB = projet("PRJ-B-" + s, societe, statutProjet);
        PrjProject centre = projet("2099-CDC-" + s, societe, statutProjet);

        PrjMarket lotBordereau = lot("L1-" + s, projetA, type, fournisseur, categorie, statutLot,
                new BigDecimal("9999"), new BigDecimal("1999.8"), new BigDecimal("11998.8"), new BigDecimal("0.1"));
        PrjMarket lotForfait = lot("L2-" + s, projetA, type, fournisseur, categorie, statutLot,
                new BigDecimal("5000"), new BigDecimal("1000"), new BigDecimal("6000"), BigDecimal.ZERO);
        PrjMarket lotVide = lot("L3-" + s, projetA, type, null, categorie, statutLot,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        PrjMarket lotB = lot("LB-" + s, projetB, type, fournisseurB, categorieB, statutLot,
                new BigDecimal("100"), new BigDecimal("20"), new BigDecimal("120"), BigDecimal.ZERO);

        PrjMarketLineGrouping groupe = new PrjMarketLineGrouping();
        groupe.setId(UUID.randomUUID());
        groupe.setPrjMarketId(lotBordereau.getId());
        groupe.setCode("G1");
        groupe.setDescription("Gros oeuvre");
        groupe.setSorting(1);
        em.persist(groupe);
        PrjMarketLine l1 = ligneMarche(lotBordereau, 1, "10", "100", "120", groupe.getId());
        PrjMarketLine l2 = ligneMarche(lotBordereau, 2, "5", "200", "240", null);
        PrjMarketLine l3 = ligneMarche(lotBordereau, 3, "2", "333.3333333333333333", "400", null);

        BuyDocumentStatus valide = statutFacture("30-" + s, "VALIDÉ DG", 30, true);
        BuyDocumentStatus enCours = statutFacture("10-" + s, "EN COURS", 10, false);
        BuyDocument facture1 = facture("F1-" + s, fournisseur, lotBordereau, projetA, valide, "216", "0");
        BuyDocument factureExtras = facture("F3-" + s, fournisseur, lotBordereau, projetA, enCours, "130", "10");

        // n° 1 : HT 200, TTC 240, RG 10 % = 24, net 216, cumul net de RG 216.
        PrjWorkingOut d1 = decompte("D1-" + s, 1, LocalDateTime.of(2026, 1, 10, 0, 0), lotBordereau, fournisseur,
                facture1, new String[]{"200", "40", "240", "24", "216", "0", "216", "216", "0"}, false);
        ligneDecompte(d1, l1, 1, "2", "0", "100", "120");
        ligneDecompte(d1, l2, 2, "0", "0", "200", "240");
        ligneDecompte(d1, l3, 3, "0", "0", "333.333333", "400");

        // n° 2 : HT 500, TTC 600, RG 60, extra 120, retenue 50 → net 610 ; cumul net 756 ; non facturé.
        PrjWorkingOut d2 = decompte("D2-" + s, 2, LocalDateTime.of(2026, 2, 10, 0, 0), lotBordereau, fournisseur,
                null, new String[]{"500", "100", "600", "60", "610", "120", "756", "600", "50"}, false);
        ligneDecompte(d2, l1, 1, "3", "2", "100", "120");
        ligneDecompte(d2, l2, 2, "1", "0", "200", "240");
        ligneDecompte(d2, l3, 3, "0", "0", "333.333333", "400");
        extra(d2, "1", "100", "120");
        PrjWorkingOutRetainedLine retenue = new PrjWorkingOutRetainedLine();
        retenue.setId(UUID.randomUUID());
        retenue.setPrjWorkingOut(d2);
        retenue.setSorting(1);
        retenue.setDescription("FOURNITURE FAUX CADRES");
        retenue.setAmount(new BigDecimal("50"));
        em.persist(retenue);

        // n° 3 « extras seuls » : aucune ligne, HT 0, extra 120, net 120 ; facture à 130 (régularisation 10).
        PrjWorkingOut d3 = decompte("D3-" + s, 3, LocalDateTime.of(2026, 3, 10, 0, 0), lotBordereau, fournisseur,
                factureExtras, new String[]{"0", "0", "0", "0", "120", "120", "756", "120", "0"}, true);
        extra(d3, "2", "50", "60");

        PrjWorkingOut dB = decompte("DB-" + s, 1, LocalDateTime.of(2026, 1, 15, 0, 0), lotB, fournisseurB, null,
                new String[]{"100", "20", "120", "0", "120", "0", "120", "120", "0"}, false);

        StlSettlementType virement = new StlSettlementType();
        virement.setId(UUID.randomUUID());
        virement.setCode("VIR-" + s);
        virement.setDescription("VIREMENT");
        em.persist(virement);
        reglement("R1-" + s, facture1, "216", "IN_WORK", virement);
        // Règlement annulé sur la même facture : listé, jamais compté.
        reglement("R2-" + s, facture1, "100", "CANCELED", virement);

        em.flush();
        // Relire depuis la base (échelle des colonnes, comme en production), pas le cache de la session.
        em.clear();
        return new Chaine(projetA, projetB, centre, lotBordereau, lotForfait, lotVide, lotB, d1, d2, d3, dB,
                facture1, factureExtras, statutLot, type.getId(), valide.getId(), fournisseur, fournisseurB,
                categorie, categorieB);
    }

    // ------------------------------------------------------------------ utilisateurs et périmètre

    public AdmUser utilisateur(String prefixe) {
        AdmUser user = new AdmUser();
        user.setId(UUID.randomUUID());
        user.setCode(prefixe + "." + UUID.randomUUID());
        user.setDisabled(false);
        em.persist(user);
        return user;
    }

    /** Habilitation légataire sur un projet de travaux ({@code AdmUserHabilitation.PrjProjectId}). */
    public void habiliter(AdmUser user, PrjProject projet) {
        AdmUserHabilitation h = new AdmUserHabilitation();
        h.setId(UUID.randomUUID());
        h.setAdmUser(user);
        h.setPrjProjectId(projet.getId());
        em.persist(h);
    }

    public void donnerRole(AdmUser user, String code) {
        AdmRole role = new AdmRole();
        role.setId(UUID.randomUUID());
        role.setCode(code);
        em.persist(role);
        AdmUserRole lien = new AdmUserRole();
        lien.setId(UUID.randomUUID());
        lien.setAdmUser(user);
        lien.setAdmRole(role);
        em.persist(lien);
        em.flush();
    }

    /** Vrai principal : le périmètre se lit dans {@code AuthenticatedUser.admUserId()}. */
    public static RequestPostProcessor connecte(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    /** Administrateur (rôle exempté) : voit tout le module. */
    public AdmUser administrateur() {
        AdmUser admin = utilisateur("chantier.admin");
        donnerRole(admin, "Administrateur");
        return admin;
    }

    // ------------------------------------------------------------------ référentiels

    private void entite(UUID id, String code, String description) {
        jdbc.update("IF NOT EXISTS (SELECT 1 FROM dbo.StrEntity WHERE PKey = ?) "
                + "INSERT INTO dbo.StrEntity (PKey, Code, Description) VALUES (?, ?, ?)",
                id.toString(), id.toString(), code, description);
    }

    // StrStatusTypeId n'est pas mappé sur l'entité StrStatus : insertion SQL directe.
    private UUID statut(UUID entite, String code, int ordre, UUID type) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO dbo.StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, StrStatusTypeId) "
                + "VALUES (?, ?, ?, ?, ?, ?)", id.toString(), code, code, ordre, entite.toString(),
                type == null ? null : type.toString());
        return id;
    }

    private CfgTier tiers(String code, String nom) {
        CfgTier tiers = new CfgTier();
        tiers.setId(UUID.randomUUID());
        tiers.setCode(code);
        tiers.setDescription(nom);
        em.persist(tiers);
        return tiers;
    }

    private StlCategory categorie(String code) {
        StlCategory categorie = new StlCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(code);
        categorie.setDescription("Gros oeuvres " + code);
        em.persist(categorie);
        return categorie;
    }

    private BuyDocumentStatus statutFacture(String code, String libelle, int ordre, boolean okPaiement) {
        BuyDocumentStatus statut = new BuyDocumentStatus();
        statut.setId(UUID.randomUUID());
        statut.setCode(code);
        statut.setDescription(libelle);
        statut.setSorting(ordre);
        statut.setIsOkForPayement(okPaiement);
        em.persist(statut);
        return statut;
    }

    // ------------------------------------------------------------------ projet, lots, bordereau

    private PrjProject projet(String code, CfgCompany societe, UUID statut) {
        PrjProject projet = new PrjProject();
        projet.setId(UUID.randomUUID());
        projet.setCode(code);
        projet.setDescription("Projet " + code);
        projet.setNotes("");
        projet.setDescriptionLong("");
        projet.setAddress("");
        projet.setCfgCompany(societe);
        projet.setStrStatus(em.getReference(StrStatus.class, statut));
        projet.setStrEntity(em.getReference(StrEntity.class, ENTITE_PROJET));
        projet.setUserCreate("fixture");
        projet.setUserCreateDateTime(LocalDateTime.of(2026, 1, 2, 9, 0));
        em.persist(projet);
        return projet;
    }

    @SuppressWarnings("java:S107") // un lot réel porte tous ces champs ; un builder n'apporterait rien ici
    private PrjMarket lot(String code, PrjProject projet, PrjMarketType type, CfgTier fournisseur,
                          StlCategory categorie, UUID statut, BigDecimal ht, BigDecimal tva, BigDecimal ttc,
                          BigDecimal tauxRg) {
        PrjMarket lot = new PrjMarket();
        lot.setId(UUID.randomUUID());
        lot.setCode(code);
        lot.setDescription("Lot " + code);
        lot.setNotes("");
        lot.setDescriptionLong("");
        lot.setCancellationCause("");
        lot.setPrjProject(projet);
        lot.setPrjMarketType(type);
        lot.setCfgSupplier(fournisseur);
        lot.setStlCategory(categorie);
        lot.setStrStatus(em.getReference(StrStatus.class, statut));
        lot.setStrEntity(em.getReference(StrEntity.class, ENTITE_LOT));
        lot.setEstimatedPriceHTManual(ht);
        lot.setEstimatedVatAmountManual(tva);
        lot.setEstimatedPriceTTCManual(ttc);
        lot.setWarrantyRatio(tauxRg);
        lot.setAdvanceRatio(BigDecimal.ZERO);
        lot.setDefaultVat(new BigDecimal("0.2"));
        lot.setStartDateManual(LocalDateTime.of(2024, 1, 2, 0, 0));
        lot.setDelayManual(366);
        lot.setEndDateManual(LocalDateTime.of(2025, 1, 2, 0, 0));
        lot.setStartDate(LocalDateTime.of(2023, 6, 23, 0, 0));
        lot.setDelay(0);
        lot.setEndDate(LocalDateTime.of(2023, 6, 23, 0, 0));
        lot.setIsMultiSupplier(false);
        lot.setIsClosed(false);
        lot.setUserCreate("fixture");
        lot.setUserCreateDateTime(LocalDateTime.of(2024, 1, 2, 9, 0));
        em.persist(lot);
        return lot;
    }

    private PrjMarketLine ligneMarche(PrjMarket lot, int ordre, String quantite, String puHT, String puTTC,
                                      UUID groupe) {
        PrjMarketLine ligne = new PrjMarketLine();
        ligne.setId(UUID.randomUUID());
        ligne.setPrjMarket(lot);
        ligne.setSorting(ordre);
        ligne.setCode("A" + ordre);
        ligne.setDescription("Article " + ordre);
        ligne.setUnity("M3");
        ligne.setQuantity(new BigDecimal(quantite));
        ligne.setUnitPriceHT(new BigDecimal(puHT));
        ligne.setVatRatio(new BigDecimal("0.2"));
        ligne.setUnitPriceTTC(new BigDecimal(puTTC));
        ligne.setIsStock(false);
        ligne.setPrjMarketLineGroupingId(groupe);
        em.persist(ligne);
        return ligne;
    }

    // ------------------------------------------------------------------ décomptes, factures, règlements

    private BuyDocument facture(String code, CfgTier fournisseur, PrjMarket lot, PrjProject projet,
                                BuyDocumentStatus statut, String aPayer, String regul) {
        BuyDocument facture = new BuyDocument();
        facture.setId(UUID.randomUUID());
        facture.setCode(code);
        facture.setDocDate(LocalDateTime.of(2026, 1, 10, 0, 0));
        facture.setMaturityDate(LocalDateTime.of(2026, 2, 10, 0, 0));
        facture.setDocType("INVOICE");
        facture.setAmountHT(new BigDecimal(aPayer));
        facture.setAmountTTC(new BigDecimal(aPayer));
        facture.setAmountToPay(new BigDecimal(aPayer));
        facture.setAmountRegul(new BigDecimal(regul));
        facture.setBuyDocumentStatus(statut);
        facture.setCfgTier(fournisseur);
        facture.setPrjMarketId(lot.getId());
        facture.setPrjProjectId(projet.getId());
        em.persist(facture);
        return facture;
    }

    /**
     * {@code m} = [AmountHT, AmountVat, AmountTTC, WarrantyAmount, AmountToPay, extraAmount,
     * AmountTTCAfterDeductions, amountToBePaid, nonRefundableRetainer].
     */
    @SuppressWarnings("java:S107")
    private PrjWorkingOut decompte(String code, int numero, LocalDateTime date, PrjMarket lot, CfgTier fournisseur,
                                   BuyDocument facture, String[] m, boolean dernier) {
        PrjWorkingOut d = new PrjWorkingOut();
        d.setId(UUID.randomUUID());
        d.setCode(code);
        d.setSorting(numero);
        d.setDocDate(date);
        d.setNotes("DECOMPTE N° " + numero);
        d.setPrjMarket(lot);
        d.setCfgTier(fournisseur);
        d.setBuyDocument(facture);
        d.setAmountHT(new BigDecimal(m[0]));
        d.setAmountVat(new BigDecimal(m[1]));
        d.setAmountTTC(new BigDecimal(m[2]));
        d.setWarrantyAmount(new BigDecimal(m[3]));
        d.setRefundableRetainer(new BigDecimal(m[3]));
        d.setWarrantyRatio(lot.getWarrantyRatio());
        d.setWorkingOutAmountTTC(new BigDecimal(m[2]).subtract(new BigDecimal(m[3])));
        d.setAmountToPay(new BigDecimal(m[4]));
        d.setExtraAmount(new BigDecimal(m[5]));
        d.setAmountTTCAfterDeductions(new BigDecimal(m[6]));
        d.setAmountToBePaid(new BigDecimal(m[7]));
        d.setNonRefundableRetainer(new BigDecimal(m[8]));
        d.setFluctuations(BigDecimal.ZERO);
        d.setFluctuationsToPay(BigDecimal.ZERO);
        d.setAdvanceAmount(BigDecimal.ZERO);
        d.setWorkingOutAdvanceAmount(BigDecimal.ZERO);
        d.setProportionAmount(BigDecimal.ZERO);
        d.setInsuranceDECAmount(BigDecimal.ZERO);
        d.setInsuranceTRCAmount(BigDecimal.ZERO);
        d.setFinalWorkingOut(dernier);
        d.setUserCreate("fixture");
        d.setUserCreateDateTime(date);
        em.persist(d);
        return d;
    }

    @SuppressWarnings("java:S107")
    private void ligneDecompte(PrjWorkingOut d, PrjMarketLine ligneMarche, int ordre, String quantite,
                               String anterieure, String puHT, String puTTC) {
        PrjWorkingOutLine l = new PrjWorkingOutLine();
        l.setId(UUID.randomUUID());
        l.setPrjWorkingOut(d);
        l.setPrjMarketLine(ligneMarche);
        l.setSorting(ordre);
        l.setCode(ligneMarche.getCode());
        l.setDescription(ligneMarche.getDescription());
        l.setUnity(ligneMarche.getUnity());
        l.setQuantityOrigin(new BigDecimal(quantite));
        l.setCoefficient(BigDecimal.ONE);
        l.setQuantity(new BigDecimal(quantite));
        l.setQuantityOld(new BigDecimal(anterieure));
        l.setUnitPriceHT(new BigDecimal(puHT));
        l.setVatRatio(new BigDecimal("0.2"));
        l.setUnitPriceTTC(new BigDecimal(puTTC));
        em.persist(l);
    }

    private void extra(PrjWorkingOut d, String quantite, String puHT, String puTTC) {
        PrjWorkingOutLineExtra x = new PrjWorkingOutLineExtra();
        x.setId(UUID.randomUUID());
        x.setPrjWorkingOut(d);
        x.setSorting(1);
        x.setCode("ART1");
        x.setDescription("Revêtement en marbre");
        x.setUnity("M2");
        x.setQuantity(new BigDecimal(quantite));
        x.setUnitPriceHT(new BigDecimal(puHT));
        x.setVatRatio(new BigDecimal("0.2"));
        x.setUnitPriceTTC(new BigDecimal(puTTC));
        em.persist(x);
    }

    private void reglement(String code, BuyDocument facture, String montant, String statut, StlSettlementType type) {
        StlSettlement r = new StlSettlement();
        r.setId(UUID.randomUUID());
        r.setCode(code);
        r.setDocDate(LocalDateTime.of(2026, 1, 20, 0, 0));
        r.setEcheanceDate(LocalDateTime.of(2026, 1, 20, 0, 0));
        r.setAmount(new BigDecimal(montant));
        r.setIsCredit(false);
        r.setStatus(statut);
        r.setStlSettlementType(type);
        em.persist(r);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(r);
        ligne.setBuyDocumentId(facture.getId());
        ligne.setAmount(new BigDecimal(montant));
        em.persist(ligne);
    }
}
