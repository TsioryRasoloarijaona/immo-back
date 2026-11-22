package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;

/**
 * N5a Bilans (FEATURE-n5-tableaux-de-bord-projet.md « Bilans — formules sur tables ») : catégories en arbre, lots ventilés par
 * catégorie, pièces d'achat, étude de faisabilité ventilée par tranche. SQL natif, parents avant enfants (FK de la CI).
 */
@Component
public class FixtureBilansProjet {

    public static final String BILANS = "/api/projet/bilans";
    public static final String EXPORT = BILANS + "/export";
    public static final String DROIT_BILANS = "Yes.Prj.Front.Tdb.ProjectReview";

    public static final String FAISABILITE = "etudeFaisabilite";
    public static final String ESTIMATION_BASE = "estimationBase";
    public static final String ESTIMATION_CONFIDENTIELLE = "estimationConfidentielle";
    public static final String MARCHE = "marche";
    public static final String MARCHE_AJUSTE = "marcheAjuste";
    public static final String PREVISIONS = "previsions";
    public static final String COUT_PREVISIONNEL = "coutPrevisionnel";
    public static final String COUT_REEL = "coutReel";
    public static final List<String> COLONNES = List.of(FAISABILITE, ESTIMATION_BASE, ESTIMATION_CONFIDENTIELLE, MARCHE,
            MARCHE_AJUSTE, PREVISIONS, COUT_PREVISIONNEL, COUT_REEL);

    private static final BigDecimal TVA = new BigDecimal("1.2");
    private static final String TEST = "Test";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final FixtureLotsParite parite;
    private final FixtureAchat achat;
    private final FixtureBordereauxEcriture bordereaux;
    private final FixtureProjetConfig config;

    private String jeton;
    private UUID societe;
    private UUID fournisseur;

    public FixtureBilansProjet(EntityManager em, JdbcTemplate jdbc, FixtureLotsParite parite, FixtureAchat achat,
                               FixtureBordereauxEcriture bordereaux, FixtureProjetConfig config) {
        this.em = em;
        this.jdbc = jdbc;
        this.parite = parite;
        this.achat = achat;
        this.bordereaux = bordereaux;
        this.config = config;
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    /** Genres de lot, table de ventilation, société et fournisseur du test ; renvoie le jeton. */
    public String preparer() {
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        bordereaux.tableVentilation();
        societe = parite.entiteJuridique("BIL-" + jeton);
        fournisseur = achat.fournisseur("BILF-" + jeton, "Fournisseur " + jeton);
        return jeton;
    }

    public UUID societe() {
        return societe;
    }

    /** Projet non commun ({@code IsACommon = 0}) ou commun. */
    public UUID projet(String code, boolean commun) {
        UUID projet = parite.projet(code + "-" + jeton, societe);
        maj("UPDATE PrjProject SET IsACommon = ? WHERE PKey = ?", commun, s(projet));
        return projet;
    }

    /** Catégorie {@code StlCategory} de libellé donné ; {@code parent} nul = racine. */
    public UUID categorie(String description, UUID parent, int ordre) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO StlCategory (PKey, Code, Description, IsCredit, ParentId, Sorting) VALUES (?, ?, ?, 0, ?, ?)",
                s(id), "BC-" + id.toString().substring(0, 8), description, s(parent), ordre);
        return id;
    }

    /** Lot entrepreneur (non multi) ou multi-fournisseur, estimations manuelles à 0. */
    public UUID lot(String code, UUID projet, boolean multi) {
        String genre = multi ? FOURNISSEURS : ENTREPRENEURS;
        UUID statut = parite.statut(genre, "BILS-" + code + "-" + jeton);
        return parite.lot(projet, "BIL-" + code + "-" + jeton, genre, statut);
    }

    /**
     * Lot entrepreneur terminé (statut de type « fin succès », {@code IsTerminated} sans annulation, {@code CancelRatio} = 1)
     * ou annulé ({@code StrStatus.BoolField002} = 1 : terminé et {@code CancelRatio} = avancement décompté).
     */
    public UUID lotTermine(String code, UUID projet, boolean annule) {
        UUID statut = parite.statut(ENTREPRENEURS, "BILS-" + code + "-" + jeton);
        maj("""
                IF NOT EXISTS (SELECT 1 FROM StrStatusType WHERE PKey = ?)
                    INSERT INTO StrStatusType (PKey, Code, Description, Sorting)
                    VALUES (?, N'Terminé avec succès', N'Terminé avec succès', 3)""",
                s(FixtureAchat.ETAT_TERMINE_SUCCES), s(FixtureAchat.ETAT_TERMINE_SUCCES));
        maj("UPDATE StrStatus SET StrStatusTypeId = ?, BoolField002 = ? WHERE PKey = ?",
                s(annule ? null : FixtureAchat.ETAT_TERMINE_SUCCES), annule, s(statut));
        return parite.lot(projet, "BIL-" + code + "-" + jeton, ENTREPRENEURS, statut);
    }

    /** Décompte {@code PrjWorkingOut} du lot, montant HT (TTC = HT × 1,2), sans ligne (aucun dépassement). */
    public void decompte(UUID lot, String code, String ht) {
        BigDecimal h = new BigDecimal(ht);
        maj("""
                INSERT INTO PrjWorkingOut (PKey, Code, Sorting, DocDate, PrjMarketId, AmountHT, AmountTTC)
                VALUES (?, ?, 1, ?, ?, ?, ?)""", s(UUID.randomUUID()), code + "-" + jeton,
                java.sql.Timestamp.valueOf(LocalDate.of(2026, 3, 1).atStartOfDay()), s(lot), h, h.multiply(TVA));
    }

    public void ventiler(UUID lot, UUID categorie, String taux) {
        bordereaux.ventilation(lot, categorie, taux);
    }

    /** Colonne 2 : {@code EstimatedPriceHTManual} / {@code TTCManual} (TTC = HT × 1,2). */
    public void estimationBase(UUID lot, String ht) {
        BigDecimal h = new BigDecimal(ht);
        BigDecimal ttc = h.multiply(TVA);
        maj("""
                UPDATE PrjMarket SET EstimatedPriceHTManual = ?, EstimatedVatAmountManual = ?, EstimatedPriceTTCManual = ?
                WHERE PKey = ?""", h, ttc.subtract(h), ttc, s(lot));
    }

    /** Colonne 3 : ligne {@code PrjMarketLineEstimation} (PU TTC = PU HT × 1,2). */
    public void ligneEstimation(UUID lot, String code, String quantite, String puHT) {
        BigDecimal pu = new BigDecimal(puHT);
        maj("""
                INSERT INTO PrjMarketLineEstimation (PKey, Code, Description, Sorting, Unity, Quantity, UnitPriceHT, VatRatio,
                    UnitPriceTTC, FODECRatio, IsStock, PrjMarketId)
                VALUES (?, ?, ?, 1, N'U', ?, ?, 0.2, ?, 0, 0, ?)""",
                s(UUID.randomUUID()), code, "Ligne " + code, new BigDecimal(quantite), pu, pu.multiply(TVA), s(lot));
    }

    /** Colonne 5 : ligne {@code PrjMarketLine} (PU TTC = PU HT × 1,2). */
    public void ligneDefinitive(UUID lot, String code, String quantite, String puHT) {
        BigDecimal pu = new BigDecimal(puHT);
        maj("""
                INSERT INTO PrjMarketLine (PKey, Code, Description, PrjMarketId, Sorting, Unity, Quantity, UnitPriceHT,
                    VatRatio, UnitPriceTTC, FODECRatio, IsStock) VALUES (?, ?, ?, ?, 1, N'U', ?, ?, 0.2, ?, 0, 0)""",
                s(UUID.randomUUID()), code, "Ligne " + code, s(lot), new BigDecimal(quantite), pu, pu.multiply(TVA));
    }

    /** Pièce d'achat du lot : HT = TTC / 1,2 ; {@code statut} nul possible. */
    public UUID piece(UUID lot, UUID projet, String code, String nature, String ttc, LocalDate date, UUID statut) {
        return achat.piece(FixtureAchat.Piece.facture(code + "-" + jeton, date, fournisseur, "Fournisseur " + jeton, societe,
                projet, new BigDecimal(ttc)).nature(nature).lot(lot).statut(statut));
    }

    /** Devis ({@code QUOTE}) du lot, retenu ou non ({@code IsSelected}). */
    public UUID devis(UUID lot, UUID projet, String code, String ttc, LocalDate date, boolean retenu) {
        UUID id = piece(lot, projet, code, FixtureAchat.DEVIS, ttc, date, null);
        achat.poser(id, "IsSelected", retenu);
        return id;
    }

    /** Bon de commande ({@code ORDER}) du lot, une ligne qui porte le même HT que l'en-tête. */
    public UUID commande(UUID lot, UUID projet, String code, String ttc, boolean okPaiement) {
        UUID statut = achat.statut("BILST-" + code + "-" + jeton, okPaiement, false, 1);
        UUID id = piece(lot, projet, code, FixtureAchat.BON_COMMANDE, ttc, LocalDate.of(2026, 3, 1), statut);
        BigDecimal ht = new BigDecimal(ttc).divide(TVA, 6, java.math.RoundingMode.HALF_UP);
        achat.ligne(id, 1, "Ligne " + code, "U", BigDecimal.ONE, ht, new BigDecimal("0.2"));
        return id;
    }

    // ------------------------------------------------------------------ faisabilité (branche A)

    public UUID tranche(String code, UUID projet) {
        return config.trancheDuProjet("BILT-" + code + "-" + jeton, projet);
    }

    /** {@code PrjProjectCfgTrancheVentilation} : taux du projet sur la tranche. */
    public void ventilationTranche(UUID projet, UUID tranche, String taux) {
        config.ventilation(projet, tranche, new BigDecimal(taux));
    }

    /** Étude ({@code StkFeasibilityStudy}) de la société du test. */
    public UUID etude(String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO StkFeasibilityStudy (PKey, Code, CfgCompanyId) VALUES (?, ?, ?)", s(id), "BILE-" + code + "-" + jeton,
                s(societe));
        return id;
    }

    /** {@code StkFeasibilityStudyCfgTranche.Weight} de la tranche dans l'étude. */
    public void poids(UUID etude, UUID tranche, String poids) {
        maj("INSERT INTO StkFeasibilityStudyCfgTranche (PKey, Code, FeasibilityStudyId, CfgTrancheId, Weight) VALUES (?, ?, ?, ?, ?)",
                s(UUID.randomUUID()), TEST, s(etude), s(tranche), new BigDecimal(poids));
    }

    /** Ligne {@code FSPrevision} : HT ({@code AmountHT}) et TTC ({@code Amount}). */
    public void prevision(UUID etude, UUID categorie, String ht, String ttc, boolean pourProjet) {
        maj("""
                INSERT INTO FSPrevision (PKey, FeasibilityStudyId, StlCategoryId, Ratio, AmountHT, Amount, VatRatio, GlobalRatio,
                    Note, Undivided, IsForProject) VALUES (?, ?, ?, 0, ?, ?, 0.2, 0, N'', 0, ?)""",
                s(UUID.randomUUID()), s(etude), s(categorie), new BigDecimal(ht), new BigDecimal(ttc), pourProjet);
    }

    // ------------------------------------------------------------------ comptes

    /** Projet non commun d'une autre société, que les comptes du test ne voient pas. */
    public UUID projetHorsPerimetre(String code) {
        UUID autre = parite.entiteJuridique("BILH-" + code + "-" + jeton);
        UUID projet = parite.projet(code + "-" + jeton, autre);
        maj("UPDATE PrjProject SET IsACommon = 0 WHERE PKey = ?", s(projet));
        return projet;
    }

    /** Compte à rôle propre portant le droit des Bilans (ou aucun droit), habilité sur la société du test et ces projets. */
    public AdmUser compte(String code, boolean droit, UUID... projets) {
        AdmUser compte = parite.avecDroits("bil." + code + "." + jeton, droit ? List.of(DROIT_BILANS) : List.of(), projets);
        achat.habiliterSociete(compte, societe);
        return compte;
    }
}
