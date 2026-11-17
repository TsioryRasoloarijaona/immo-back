package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import com.tsix.immocrm.fixtures.FixtureAchat.SequenceNature;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;

/**
 * Lot L3b-2b (FEATURE-lot-pieces-a-payer.md) : cadre de création des pièces à payer du lot — statuts de lot à drapeaux
 * ({@code BoolField008} avance décompte, {@code BoolField010} avance marché), lot complet (fournisseur, devise, délai, TVA,
 * ventilation), configuration A2b-2a (statuts EN COURS / VALIDÉ DT lecture seule, séquences, devise et TVA par défaut), gestions
 * OMRAN des 4 natures à leurs PKeys de prod avec un workflow EN COURS → VALIDÉ DG ; pièces, avoirs, devis, factures, règlements,
 * décomptes. SQL natif, parents avant enfants (FK de la CI) ; annulé avec la transaction du test.
 */
@Component
public class FixturePiecesLot {

    public static final String MARKET_ADVANCE = "MARKET_ADVANCE";
    public static final String WORKINGOUT_ADVANCE = "WORKINGOUT_ADVANCE";
    public static final String MARKET_WARRANTY = "MARKET_WARRANTY";
    public static final String MARKET_PROPORTION = "MARKET_PROPORTION";

    /** {@code YesRessources.Constants.StrEntity} des 4 natures (PKeys de prod). */
    public static final UUID GESTION_AVA = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B601");
    public static final UUID GESTION_WAVA = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B603");
    public static final UUID GESTION_RRG = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B602");
    public static final UUID GESTION_RCP = UUID.fromString("F9091D83-6134-4B70-BE54-B208B25F073D");
    /** Remboursement RG sur bon de commande multi-fournisseur : compté dans n des RRG (R5). */
    public static final UUID GESTION_RRG_MULTI = UUID.fromString("A71B4F91-AEFC-4C70-87F7-82A6C7365CB5");
    public static final UUID GESTION_FACTURE_DECOMPTE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B604");
    public static final UUID GESTION_FACTURE_AVENANT = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B622");

    public static final Map<String, UUID> GESTIONS = Map.of(MARKET_ADVANCE, GESTION_AVA, WORKINGOUT_ADVANCE, GESTION_WAVA,
            MARKET_WARRANTY, GESTION_RRG, MARKET_PROPORTION, GESTION_RCP);
    private static final Map<UUID, String> CODES_GESTION = Map.of(GESTION_AVA, "PrjDocumentDebitMarketAdvance",
            GESTION_WAVA, "PrjDocumentDebitMarketWorkingOutAdvance", GESTION_RRG, "PrjDocumentDebitMarketWarranty",
            GESTION_RCP, "PrjDocumentDebitMarketProrataRefund", GESTION_RRG_MULTI, "PrjDocumentDebitOrderWarrantyMultiSupplier");

    /** Séquence « Document à régler entrant » de prod (absente de yesRemsTest), posée à 900. */
    public static final SequenceNature SEQUENCE_DOCUMENT_A_REGLER = new SequenceNature("Document à régler entrant",
            UUID.fromString("50C4F768-BE0C-49AA-A6FD-F784D28F2F6A"), 900);

    public static final int DELAI_PAIEMENT = 30;
    public static final String TVA_LOT = "0.2";
    public static final LocalDate DATE_PIECE = LocalDate.of(2026, 3, 1);

    private static final String SANS_FOURNISSEUR = "UPDATE PrjMarket SET CfgSupplierId = NULL WHERE PKey = ?";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final FixtureLotsParite parite;
    private final FixtureLotSousFenetres sf;
    private final FixtureLotPrevisions prev;
    private final FixtureBordereauxEcriture bordereaux;
    private final FixtureAchat achat;
    private final FixtureProjetLots projetLots;

    public FixturePiecesLot(EntityManager em, JdbcTemplate jdbc, FixtureLotsParite parite, FixtureLotSousFenetres sf,
                            FixtureLotPrevisions prev, FixtureBordereauxEcriture bordereaux, FixtureAchat achat,
                            FixtureProjetLots projetLots) {
        this.em = em;
        this.jdbc = jdbc;
        this.parite = parite;
        this.sf = sf;
        this.prev = prev;
        this.bordereaux = bordereaux;
        this.achat = achat;
        this.projetLots = projetLots;
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    private static BigDecimal n(String valeur) {
        return valeur == null ? null : new BigDecimal(valeur);
    }

    private static Timestamp jour(LocalDate date) {
        return Timestamp.valueOf(date.atStartOfDay());
    }

    /**
     * Cadre d'un test : {@code enCours} = statut de départ « Nouveau » des 4 gestions, {@code verrouille} = statut lecture seule
     * ({@code IsReadOnly}), {@code deviseLot} ≠ {@code deviseParDefaut} ({@code AdmParamsGuid.DefaultCurrency}).
     */
    public record Cadre(String jeton, UUID societe, UUID projet, String projetDescription, UUID fournisseur, UUID deviseLot,
                        UUID deviseParDefaut, UUID categorie, UUID enCours, UUID verrouille, UUID modulePrj,
                        UUID statutLibre, UUID statutSansDrapeau, UUID statutAvanceDecompte, UUID statutAvanceMarche) {
    }

    public Cadre cadre(String prefixe) {
        String jeton = prefixe + FixtureProjetLots.jeton();
        parite.genresDeLot();
        prev.preparer();
        Saisie saisie = achat.configurationSaisie(jeton);
        achat.sequenceSaisie(SEQUENCE_DOCUMENT_A_REGLER);
        CODES_GESTION.forEach(this::entite);
        entite(GESTION_FACTURE_DECOMPTE, "PrjInvoiceDebitWorkingOut");
        entite(GESTION_FACTURE_AVENANT, "PrjInvoiceDebitMarketClause");
        GESTIONS.values().forEach(g -> achat.transitionDe(achat.gestionVide(CODES_GESTION.get(g), g), 10, saisie.enCours(),
                saisie.valideDg()));
        UUID societe = parite.entiteJuridique("PLE-" + jeton);
        UUID projet = parite.projet("PLP-" + jeton, societe);
        return new Cadre(jeton, societe, projet, "Projet PLP-" + jeton, achat.fournisseur("PLF-" + jeton, "Fournisseur " + jeton),
                sf.devise("PLD-" + jeton), saisie.devise(), saisie.categorie(), saisie.enCours(), saisie.valideDt(),
                saisie.moduleProjet(), statutLot(ENTREPRENEURS, "PLS1-" + jeton, true, true),
                statutLot(ENTREPRENEURS, "PLS0-" + jeton, false, false), statutLot(ENTREPRENEURS, "PLS8-" + jeton, true, false),
                statutLot(ENTREPRENEURS, "PLS10-" + jeton, false, true));
    }

    /** {@code StrEntity} à sa PKey de prod, créée si absente. */
    public void entite(UUID pkey, String code) {
        maj("IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = ?) INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, ?, ?)",
                s(pkey), s(pkey), code, code);
    }

    /** Statut de lot du genre ; {@code avanceDecompte} = {@code BoolField008}, {@code avanceMarche} = {@code BoolField010}. */
    public UUID statutLot(String genre, String code, boolean avanceDecompte, boolean avanceMarche) {
        UUID statut = parite.statut(genre, code);
        maj("UPDATE StrStatus SET BoolField008 = ?, BoolField010 = ? WHERE PKey = ?", avanceDecompte, avanceMarche, s(statut));
        return statut;
    }

    /**
     * Lot mono-fournisseur complet : fournisseur, devise du lot, délai de paiement 30 j, TVA 20 %, avance 0, RG non bloquée,
     * une catégorie de ventilation à 100 %. Description = « Lot » + code.
     */
    public UUID lot(Cadre c, String code, UUID statut) {
        UUID lot = parite.lot(c.projet(), code + "-" + c.jeton(), ENTREPRENEURS, statut);
        completer(c, lot);
        bordereaux.ventilation(lot, c.categorie(), "1");
        return lot;
    }

    /** Lot mono-fournisseur complet sans aucune catégorie de ventilation. */
    public UUID lotSansVentilation(Cadre c, String code, UUID statut) {
        bordereaux.tableVentilation();
        UUID lot = parite.lot(c.projet(), code + "-" + c.jeton(), ENTREPRENEURS, statut);
        completer(c, lot);
        return lot;
    }

    /** Lot multi-fournisseur ({@code IsMultiSupplier} = 1), sans fournisseur, statut du genre Fournisseurs aux deux drapeaux. */
    public UUID lotMultiSansFournisseur(Cadre c, String code) {
        UUID statut = statutLot(FOURNISSEURS, "PLM-" + code + "-" + c.jeton(), true, true);
        UUID lot = parite.lot(c.projet(), code + "-" + c.jeton(), FOURNISSEURS, statut);
        completer(c, lot);
        maj(SANS_FOURNISSEUR, s(lot));
        bordereaux.ventilation(lot, c.categorie(), "1");
        return lot;
    }

    private void completer(Cadre c, UUID lot) {
        maj("""
                UPDATE PrjMarket SET CfgSupplierId = ?, StlCurrencyId = ?, PaymentDelay = ?, DefaultVat = ?, AdvanceRatio = 0,
                    BlokWarrantyRestitution = 0, StlCategoryId = ? WHERE PKey = ?""",
                s(c.fournisseur()), s(c.deviseLot()), DELAI_PAIEMENT, n(TVA_LOT), s(c.categorie()), s(lot));
    }

    /** Lot complet d'un autre projet (autre société), que les comptes du cadre ne voient pas. */
    public UUID lotHorsPerimetre(Cadre c, String code) {
        UUID autreProjet = parite.projet("PLX-" + c.jeton(), parite.entiteJuridique("PLY-" + c.jeton()));
        UUID lot = parite.lot(autreProjet, code + "-" + c.jeton(), ENTREPRENEURS, c.statutLibre());
        completer(c, lot);
        return lot;
    }

    public UUID categorie(String code) {
        return bordereaux.categorie(code);
    }

    /** Catégorie de ventilation du lot au taux donné (fraction 0–1, comme en prod). */
    public void ventilation(UUID lot, UUID categorie, String taux) {
        bordereaux.ventilation(lot, categorie, taux);
    }

    /** Prévisions de paiement calculables (planning, estimation, retenues à 0) et une ligne périmée que le recalcul efface. */
    public void previsionsCalculables(UUID lot) {
        LocalDate debut = LocalDate.of(2026, 1, 1);
        prev.planning(lot, debut, 90, debut, 90);
        prev.estimationBase(lot, "1000", "200", "1200", TVA_LOT);
        prev.retenues(lot, "0", "0", "0");
        prev.lignePerimee(lot);
    }

    /** Libellés des lignes de {@code PrjMarketPaymentPrevision} du lot. */
    public List<Object> libellesPrevisions(UUID lot) {
        return prev.lignesPaiement(lot).stream().map(l -> l.get("Description")).toList();
    }

    public String descriptionLot(Cadre c, String code) {
        return "Lot " + code + "-" + c.jeton();
    }

    public void sansFournisseur(UUID lot) {
        maj(SANS_FOURNISSEUR, s(lot));
    }

    public void rgBloquee(UUID lot) {
        maj("UPDATE PrjMarket SET BlokWarrantyRestitution = 1 WHERE PKey = ?", s(lot));
    }

    public void tauxAvance(UUID lot, String taux) {
        maj("UPDATE PrjMarket SET AdvanceRatio = ? WHERE PKey = ?", n(taux), s(lot));
    }

    public void lotProrataDuProjet(Cadre c, UUID lot) {
        prev.lotProrata(c.projet(), lot);
    }

    // ------------------------------------------------------------------ pièces du lot

    /**
     * Pièce à payer du lot ({@code DOCUMENT_TO_PAY}, payable) de la nature, à la gestion OMRAN de la nature, au statut donné,
     * fournisseur et devise du lot ; HT = TTC = à payer, datée du 01/03/2026.
     */
    public UUID piece(Cadre c, UUID lot, String nature, String code, String ttc, UUID statut) {
        UUID piece = sf.piece(lot, c.projet(), c.societe(), code + "-" + c.jeton(), FixtureLotSousFenetres.DOCUMENT_TO_PAY, nature,
                DATE_PIECE, ttc);
        sf.entete(piece, statut, c.deviseLot(), null);
        maj("UPDATE BuyDocument SET StrEntityId = ?, CfgTierId = ?, AdmModuleId = ? WHERE PKey = ?",
                s(GESTIONS.get(nature)), s(c.fournisseur()), s(c.modulePrj()), s(piece));
        return piece;
    }

    /** {@code BuyDocument.StrEntityId} d'une pièce de fixture. */
    public void gestion(UUID piece, UUID gestion) {
        maj("UPDATE BuyDocument SET StrEntityId = ? WHERE PKey = ?", s(gestion), s(piece));
    }

    /** Avoir du lot ({@code REFUND}, payable), au statut donné. */
    public UUID avoir(Cadre c, UUID lot, String code, UUID statut) {
        UUID avoir = sf.piece(lot, c.projet(), c.societe(), code + "-" + c.jeton(), FixtureLotSousFenetres.REFUND, null,
                DATE_PIECE, "100");
        sf.entete(avoir, statut, c.deviseLot(), null);
        maj("UPDATE BuyDocument SET CfgTierId = ? WHERE PKey = ?", s(c.fournisseur()), s(avoir));
        return avoir;
    }

    /** Devis retenu ({@code QUOTE}, {@code IsSelected} = 1) : le marché du lot. */
    public UUID devisRetenu(Cadre c, UUID lot, String ht, String ttc) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BuyDocument (PKey, Code, DocType, DocDate, CfgCompanyId, PrjProjectId, PrjMarketId, AmountHT, AmountTTC,
                    AmountToPay, IsSelected, CanBePayed)
                VALUES (?, ?, N'QUOTE', ?, ?, ?, ?, ?, ?, ?, 1, 0)""",
                s(id), "PLQ-" + c.jeton(), jour(DATE_PIECE), s(c.societe()), s(c.projet()), s(lot), n(ht), n(ttc), n(ttc));
        return id;
    }

    /** Facture ({@code INVOICE}) du lot à la gestion donnée, avec sa RG et son prorata retenus. */
    public UUID facture(Cadre c, UUID lot, String code, UUID gestion, String rg, String prorata) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BuyDocument (PKey, Code, DocType, DocDate, CfgCompanyId, PrjProjectId, PrjMarketId, StrEntityId, CfgTierId,
                    AmountHT, AmountTTC, AmountToPay, WarrantyAmount, ProportionAmount, CanBePayed)
                VALUES (?, ?, N'INVOICE', ?, ?, ?, ?, ?, ?, 1000, 1000, 1000, ?, ?, 1)""",
                s(id), code + "-" + c.jeton(), jour(DATE_PIECE), s(c.societe()), s(c.projet()), s(lot), s(gestion),
                s(c.fournisseur()), n(rg), n(prorata));
        return id;
    }

    /** Avenant du lot ({@code PrjClause}) à RG stockée, facturé par une facture d'avenant ({@code B622}) de même RG. */
    public UUID avenantFacture(Cadre c, UUID lot, String rg) {
        UUID avenant = UUID.randomUUID();
        maj("""
                INSERT INTO PrjClause (PKey, Sorting, Code, Description, DocDate, AmountHT, AmountVat, AmountTTC, AmountToPay,
                    WarrantyAmount, PrjMarketId)
                VALUES (?, 1, ?, N'Avenant de test', ?, 1000, 200, 1200, 1200, ?, ?)""",
                s(avenant), "PLA-" + c.jeton(), jour(DATE_PIECE), n(rg), s(lot));
        UUID facture = facture(c, lot, "PLFA", GESTION_FACTURE_AVENANT, rg, "0");
        sf.rattacherAvenant(facture, avenant);
        return avenant;
    }

    /** Règlement pointé d'une ligne sur la pièce. */
    public UUID reglement(UUID piece, String code, String montant) {
        return projetLots.reglement(piece, code, n(montant), FixtureAchat.POINTE);
    }

    /**
     * Décompte du lot de rang {@code rang} ({@code Sorting}) ; {@code facture} non nul = facturé ({@code BuyDocumentId}) ;
     * {@code avanceDecompte} = {@code WorkingOutAdvanceAmount} déjà imputé.
     */
    public UUID decompte(Cadre c, UUID lot, int rang, boolean definitif, UUID facture, String avanceDecompte) {
        Map<String, BigDecimal> montants = avanceDecompte == null ? Map.of()
                : Map.of(FixtureProjetLots.AVANCE_DECOMPTE, n(avanceDecompte));
        UUID decompte = projetLots.decompte(lot, "PLW" + rang + "-" + c.jeton(), rang, montants);
        maj("UPDATE PrjWorkingOut SET FinalWorkingOut = ?, BuyDocumentId = ? WHERE PKey = ?", definitif, s(facture), s(decompte));
        return decompte;
    }

    /** Compte non Administrateur, exempté, habilité sur le projet et sa société, portant exactement ces droits. */
    public AdmUser compte(Cadre c, String code, List<String> droits) {
        AdmUser compte = parite.avecDroits(code + "." + c.jeton(), droits, c.projet());
        achat.habiliterSociete(compte, c.societe());
        return compte;
    }

    public AdmUser administrateur(Cadre c, String code) {
        AdmUser compte = parite.administrateur(code + "." + c.jeton(), c.projet());
        achat.habiliterSociete(compte, c.societe());
        return compte;
    }

    // ------------------------------------------------------------------ lecture

    public Map<String, Object> pieceEnBase(UUID piece) {
        return achat.pieceEnBase(piece);
    }

    public List<Map<String, Object>> lignes(UUID piece) {
        em.flush();
        em.clear();
        return jdbc.queryForList("""
                SELECT Quantity, UnitPriceHT, VatRatio, Unity, Description, Sorting, CAST(StlCategoryId AS varchar(36)) AS categorie,
                    Quantity * UnitPriceHT AS ht, Quantity * UnitPriceHT * (1 + VatRatio) AS ttc
                FROM BuyDocumentLine WHERE BuyDocumentId = ? ORDER BY UnitPriceHT DESC""", s(piece));
    }

    /** Pièces du lot de la nature ({@code ReferenceType}). */
    public List<Map<String, Object>> piecesDuLot(UUID lot, String nature) {
        em.flush();
        em.clear();
        return jdbc.queryForList("SELECT * FROM BuyDocument WHERE PrjMarketId = ? AND ReferenceType = ?", s(lot), nature);
    }

    /** Colonnes de rattachement et de reste d'avance d'un décompte. */
    public Map<String, Object> decompteEnBase(UUID decompte) {
        em.flush();
        em.clear();
        return jdbc.queryForMap("""
                SELECT WorkingOutAdvanceAmountRestAtCreation AS reste FROM PrjWorkingOut WHERE PKey = ?""", s(decompte));
    }

    /** 1 si la pièce existe encore, 0 sinon. */
    public int existe(UUID piece) {
        return compter("SELECT COUNT(*) FROM BuyDocument WHERE PKey = ?", s(piece));
    }

    public int nombreDeLignes(UUID piece) {
        return compter("SELECT COUNT(*) FROM BuyDocumentLine WHERE BuyDocumentId = ?", s(piece));
    }

    /** Lignes d'historique de statut de la pièce ({@code BuyDocumentStatusHistory}). */
    public List<Map<String, Object>> historique(UUID piece) {
        em.flush();
        em.clear();
        return jdbc.queryForList("""
                SELECT CAST(BuyDocumentStatusId AS varchar(36)) AS statut FROM BuyDocumentStatusHistory WHERE BuyDocumentId = ?""",
                s(piece));
    }

    public int compter(String sql, Object... parametres) {
        em.flush();
        Integer total = jdbc.queryForObject(sql, Integer.class, parametres);
        return total == null ? 0 : total;
    }

    public static String guid(Object valeur) {
        return valeur == null ? null : valeur.toString().toUpperCase();
    }

    public static String guid(UUID id) {
        return id == null ? null : id.toString().toUpperCase();
    }
}
