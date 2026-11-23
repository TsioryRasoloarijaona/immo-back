package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.controller.rapport.RapportsProjetSupport.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureBilansProjet;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.fixtures.FixtureRapportsProjet;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;

/**
 * Jeu de données des rapports N5b-b (FEATURE-n5b-b-rapports-projet.md) au-dessus du cadre N5b-a : lignes
 * {@code AdmReportData}, catégories à libellé unique (jeton), lots ventilés et pièces d'achat. Les colonnes stockées
 * ({@code EstimatedPriceTTC}, {@code OfferedPriceTTC}) sont posées égales à leurs sources (lignes d'estimation, devis
 * retenu) : le test ne tranche pas entre les deux lectures (réponse du manager).
 */
final class RapportsProjetBilanSupport {

    static final String BILAN = "PrjTdbBilan";
    static final String SUIVI = "PrjMarketTresorieByMonth";
    static final String CATEGORIE = "categorie";
    static final String NIVEAU = "niveau";
    static final String ESTIMATION_BASE = "estimationBase";
    static final String ESTIMATION_CONFIDENTIELLE = "estimationConfidentielle";
    static final String MARCHE = "marche";
    static final String CUMUL_TRAVAUX = "cumulTravaux";
    static final String A_PAYER = "aPayer";
    static final String REGLE = "regle";
    static final String RESTE = "reste";
    static final String FAISABILITE = "etudeFaisabilite";
    static final List<String> MONTANTS = List.of(ESTIMATION_BASE, ESTIMATION_CONFIDENTIELLE, MARCHE, CUMUL_TRAVAUX, A_PAYER,
            REGLE, RESTE, FAISABILITE);
    static final String MOIS = "mois";
    static final String REEL = "reel";
    static final String ANNEE = "annee";
    static final String MESSAGE_UN_PROJET = "Veuillez sélectionner un projet !";

    private static final BigDecimal TVA = new BigDecimal("1.2");
    private static final String CAN_BE_PAYED = "CanBePayed";

    private final Cadre c;
    private final FixtureAchat achat;
    private final FixtureRapportsProjet fixture;
    private final FixtureLotsParite parite;
    private final FixtureBilansProjet bilans;
    private final FixtureProjetLots lots;
    private final JdbcTemplate jdbc;
    private final EntityManager em;

    RapportsProjetBilanSupport(Cadre c, FixtureAchat achat, FixtureRapportsProjet fixture, FixtureLotsParite parite,
                               FixtureBilansProjet bilans, FixtureProjetLots lots, JdbcTemplate jdbc, EntityManager em) {
        this.c = c;
        this.achat = achat;
        this.fixture = fixture;
        this.parite = parite;
        this.bilans = bilans;
        this.lots = lots;
        this.jdbc = jdbc;
        this.em = em;
    }

    /** Les deux lignes {@code AdmReportData} du lot (après {@code cadre()}, qui crée les tables) ; rend code → id. */
    static Map<String, UUID> rapports(FixtureRapports rapports) {
        UUID bilan = rapports.rapport(FixtureRapportsProjet.TYPE_PROJET, BILAN, "BILAN", "PROJET", 5);
        UUID suivi = rapports.rapport(FixtureRapportsProjet.TYPE_PROJET, SUIVI, "SUIVI MENSUEL DES DÉCAISSEMENTS", "PROJET", 6);
        rapports.relire();
        return Map.of(BILAN, bilan, SUIVI, suivi);
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    /** Libellé unique au test d'une catégorie. */
    String libelle(String nom) {
        return nom + " " + c.jeton();
    }

    UUID categorie(String nom, UUID parent, int ordre) {
        return bilans.categorie(libelle(nom), parent, ordre);
    }

    /** Lot entrepreneur (mono-fournisseur), statut « Nouveau » du cadre. */
    UUID lot(UUID projet, String code) {
        return parite.lot(projet, c.code(code), ENTREPRENEURS, c.statutMono());
    }

    UUID lotMulti(UUID projet, String code) {
        return parite.lot(projet, c.code(code), FixtureLotsParite.FOURNISSEURS, c.statutMulti());
    }

    /** Lot résilié (D5) : statut de type {@code StrStatusTypeId} = « Terminé avec échec ». */
    UUID lotResilie(UUID projet, String code) {
        UUID statut = parite.statut(ENTREPRENEURS, "N5BR-" + code + "-" + c.jeton());
        maj("""
                IF NOT EXISTS (SELECT 1 FROM StrStatusType WHERE PKey = ?)
                    INSERT INTO StrStatusType (PKey, Code, Description, Sorting)
                    VALUES (?, N'Terminé avec échec', N'Terminé avec échec', 4)""",
                FixtureAchat.ETAT_TERMINE_ECHEC.toString(), FixtureAchat.ETAT_TERMINE_ECHEC.toString());
        maj("UPDATE StrStatus SET StrStatusTypeId = ? WHERE PKey = ?", FixtureAchat.ETAT_TERMINE_ECHEC.toString(),
                statut.toString());
        return parite.lot(projet, c.code(code), ENTREPRENEURS, statut);
    }

    /** {@code PrjMarketVentilationCategory} du lot (fraction). */
    void ventiler(UUID lot, UUID categorie, String taux) {
        bilans.ventiler(lot, categorie, taux);
    }

    /** {@code EstimatedPriceTTCManual} = HT × 1,2. */
    void estimationBase(UUID lot, String ht) {
        bilans.estimationBase(lot, ht);
    }

    /** Une ligne d'estimation (1 × PU HT, TTC = × 1,2) et {@code EstimatedPriceTTC} égal à son TTC. */
    void estimationConfidentielle(UUID lot, String code, String ht) {
        bilans.ligneEstimation(lot, c.code(code), "1", ht);
        fixture.marche(lot, "EstimatedPriceTTC", new BigDecimal(ht).multiply(TVA));
    }

    /** Devis retenu de TTC donné (jamais payable) ; {@code PrjMarket.OfferedPriceTTC} n'existe ni en CI ni en yesRemsTest. */
    void marche(UUID lot, UUID projet, String code, String ttc) {
        UUID devis = achat.piece(FixtureAchat.Piece.facture(c.code(code), LocalDate.of(2026, 2, 10), c.fournisseur(), c.nom(),
                c.societe(), projet, new BigDecimal(ttc)).nature(FixtureAchat.DEVIS).lot(lot));
        achat.poser(devis, "IsSelected", true);
        achat.poser(devis, CAN_BE_PAYED, false);
    }

    /** Facture du lot (ou sans lot si {@code lot} nul) : AmountTTC {@code ttc}, AmountToPay {@code aPayer}, CanBePayed. */
    UUID piece(UUID lot, UUID projet, String code, String ttc, String aPayer, boolean payable) {
        UUID id = achat.piece(FixtureAchat.Piece.facture(c.code(code), LocalDate.of(2026, 4, 1), c.fournisseur(), c.nom(),
                c.societe(), projet, new BigDecimal(ttc)).lot(lot));
        BigDecimal ht = new BigDecimal(ttc).divide(TVA, 6, RoundingMode.HALF_UP);
        achat.poser(id, "AmountToPay", new BigDecimal(aPayer));
        achat.poser(id, "AmountHT", ht);
        achat.poser(id, CAN_BE_PAYED, payable);
        return id;
    }

    /** {@code BuyVentilationCategory} de la pièce (fraction). */
    void ventilerPiece(UUID piece, UUID categorie, String taux) {
        achat.ventilation(piece, categorie, new BigDecimal(taux));
    }

    void regler(UUID piece, String code, String montant) {
        lots.reglement(piece, c.code(code), new BigDecimal(montant), null);
    }

    /** Décompte du lot ({@code AmountTTC} donné), {@code DocDate} nulle possible. */
    UUID decompte(UUID lot, String code, int numero, String ttc, LocalDate date) {
        UUID id = fixture.decompte(lot, c.code(code), numero, "0", "0", "0", date);
        maj("UPDATE PrjWorkingOut SET AmountTTC = ? WHERE PKey = ?", new BigDecimal(ttc), id.toString());
        return id;
    }
}
