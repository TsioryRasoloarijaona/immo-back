package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lot N7a — états Achat (FEATURE-n7-etats-achat.md) : pièces datées d'une année vide (2093), montants d'avance / de retenue
 * posés en colonne, natures {@code StrEntity} aux PKeys de production. Tout est annulé avec la transaction du test.
 */
@Component
public class FixtureEtatsAchat {

    public static final int ANNEE = 2093;
    public static final String DU = "2093-01-01";
    public static final String AU = "2093-12-31";

    public static final String DROIT_TVA = "Yes.Buy.Front.Tdb.VatStatement";
    public static final String DROIT_AVANCES = "Yes.Buy.Front.Tdb.BuyMonitoringAdvanceView";
    public static final String DROIT_AVANCES_EXPORT = DROIT_AVANCES + ".toolExport";
    public static final String DROIT_RETENUES = "Yes.Buy.Front.Tdb.BuyMonitoringWarrantyView";
    public static final String DROIT_RETENUES_EXPORT = DROIT_RETENUES + ".toolExport";
    public static final String[] TOUS_LES_DROITS = {DROIT_TVA, DROIT_AVANCES, DROIT_AVANCES_EXPORT, DROIT_RETENUES,
            DROIT_RETENUES_EXPORT};

    /** Natures « avance » de {@code BuyMonitoringAdvanceView} (R5 b). */
    public static final UUID AVANCE_MARCHE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B601");
    public static final UUID AVANCE_DECOMPTE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B603");
    public static final UUID AVANCE_COMMANDE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42C004");
    public static final UUID AVANCE_COMMANDE_MULTI = UUID.fromString("D35918ED-0B9F-42AE-BB6B-EBAA35321EDD");
    /** Natures « restitution » de {@code BuyMonitoringWarrantyView} (R6 b). */
    public static final UUID RESTITUTION_MARCHE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B602");
    public static final UUID RESTITUTION_MULTI = UUID.fromString("A71B4F91-AEFC-4C70-87F7-82A6C7365CB5");
    public static final UUID RESTITUTION_COMMANDE = UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42C866");

    private static final String COMMERCIAL = "Commercial";
    private static final String ZERO = "0";

    private final EntityManager em;
    private final FixtureAchat achat;

    public FixtureEtatsAchat(EntityManager em, FixtureAchat achat) {
        this.em = em;
        this.achat = achat;
    }

    public static LocalDate date(int mois, int jour) {
        return LocalDate.of(ANNEE, mois, jour);
    }

    /** Compte « Commercial » habilité à la société (et au projet si non nul), portant les droits OMRAN donnés. */
    public AdmUser compte(String code, UUID societe, UUID projet, String... droits) {
        AdmUser compte = achat.compte(code, COMMERCIAL);
        achat.habiliterSociete(compte, societe);
        if (projet != null) {
            achat.habiliter(compte, projet);
        }
        for (String droit : droits) {
            achat.droitOmran(compte, droit);
        }
        return compte;
    }

    public static String role() {
        return COMMERCIAL;
    }

    /**
     * Pièce de {@code nature} ; en-tête : TTC = à payer = {@code aPayer}, HT = à payer / 1,2, TVA = le reste ; montants d'avance,
     * de retenue et de régularisation posés à 0 (jamais NULL, la vue OMRAN les additionne).
     */
    public UUID piece(String code, String nature, LocalDate date, UUID fournisseur, String nom, UUID societe, UUID projet,
                      UUID statut, String aPayer) {
        UUID id = achat.piece(FixtureAchat.Piece.facture(code, date, fournisseur, nom, societe, projet, new BigDecimal(aPayer))
                .nature(nature).statut(statut));
        avance(id, ZERO, ZERO);
        retenue(id, ZERO);
        regularisation(id, ZERO);
        return id;
    }

    public void avance(UUID piece, String avance, String avanceDecompte) {
        achat.poser(piece, "AdvanceAmount", new BigDecimal(avance));
        achat.poser(piece, "WorkingOutAdvanceAmount", new BigDecimal(avanceDecompte));
    }

    public void retenue(UUID piece, String montant) {
        achat.poser(piece, "WarrantyAmount", new BigDecimal(montant));
    }

    public void regularisation(UUID piece, String montant) {
        achat.poser(piece, "AmountRegul", new BigDecimal(montant));
    }

    /** {@code BuyDocument.StrEntityId} = nature à sa PKey de prod (créée si absente) ; rend son libellé ({@code Description}). */
    public String nature(UUID piece, UUID entite) {
        em.flush();
        em.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = :id)
                            INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, :code, :code)""")
                .setParameter("id", entite).setParameter("code", "N7-" + entite.toString().substring(28))
                .executeUpdate();
        achat.poser(piece, "StrEntityId", entite);
        Object libelle = em.createNativeQuery("SELECT Description FROM StrEntity WHERE PKey = :id")
                .setParameter("id", entite).getSingleResult();
        return libelle == null ? null : libelle.toString();
    }

    public void dater(UUID piece, LocalDateTime date) {
        achat.poser(piece, "DocDate", date);
    }

    /** En-tête HT / TVA d'une facture (lu par R2 quand la facture n'a pas de ligne). */
    public void enTete(UUID piece, String ht, String tva) {
        achat.poser(piece, "AmountHT", new BigDecimal(ht));
        achat.poser(piece, "AmountVat", new BigDecimal(tva));
    }

    /** Ligne de facture ; remise et TVA en fractions comme OMRAN, FODEC 0. */
    public void ligne(UUID piece, int ordre, String quantite, String prixUnitaireHT, String remise, String tauxTva) {
        BigDecimal pu = new BigDecimal(prixUnitaireHT);
        BigDecimal tva = new BigDecimal(tauxTva);
        BigDecimal r = new BigDecimal(remise);
        BigDecimal puTtc = pu.multiply(BigDecimal.ONE.subtract(r)).multiply(BigDecimal.ONE.add(tva));
        achat.ligne(piece, new FixtureAchat.LigneAchat(ordre, "L" + ordre, "Ligne " + ordre, "U", new BigDecimal(quantite),
                pu, tva, r, BigDecimal.ZERO, puTtc));
    }

    /** Statut de type « Annulation » ({@code AdmWorkFlowStateType} de prod), D4. */
    public UUID statutAnnule(String code) {
        return achat.statutDEtat(code, FixtureAchat.ETAT_ANNULATION, null);
    }
}
