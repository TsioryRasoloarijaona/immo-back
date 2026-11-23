package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lot N5b-a — 4 rapports Projet (FEATURE-n5b-rapports-projet.md R1-R5) : lignes {@code AdmReportData} du type
 * {@code Yes.Prj.Front.Tdb.Reports}, montants de lot posés en SQL natif (ratios, blocage, décomptes, factures, restitutions).
 * Parents avant enfants (FK de la CI) ; tout s'annule avec la transaction du test.
 */
@Component
public class FixtureRapportsProjet {

    public static final String TYPE_PROJET = "Yes.Prj.Front.Tdb.Reports";
    public static final String RETENUES = "PRJWarrantyStatus";
    public static final String DEC = "PRJInsuranceDECStatus";
    public static final String TRC = "PRJInsuranceTRCStatus";
    public static final String PRORATA = "PrjProrataState";
    public static final List<String> CODES = List.of(RETENUES, DEC, TRC, PRORATA);

    /** Genre des pièces de restitution de RG que lit {@code CalculLot} (PKey de prod). */
    public static final UUID ENTITE_RESTITUTION = UUID.fromString("A71B4F91-AEFC-4C70-87F7-82A6C7365CB5");

    // Colonnes lues par CalculLot ; présentes en prod et en CI, ajoutées si yesRemsTest les ignore (annulé au rollback).
    private static final String COLONNES = """
            IF COL_LENGTH('dbo.PrjMarket', 'InsuranceDECRatio') IS NULL ALTER TABLE dbo.PrjMarket ADD InsuranceDECRatio decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.PrjMarket', 'InsuranceTRCRatio') IS NULL ALTER TABLE dbo.PrjMarket ADD InsuranceTRCRatio decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.PrjMarket', 'ProportionRatio') IS NULL ALTER TABLE dbo.PrjMarket ADD ProportionRatio decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.PrjMarket', 'BlokWarrantyRestitution') IS NULL
                ALTER TABLE dbo.PrjMarket ADD BlokWarrantyRestitution bit NULL;
            IF COL_LENGTH('dbo.PrjWorkingOut', 'ProportionAmount') IS NULL
                ALTER TABLE dbo.PrjWorkingOut ADD ProportionAmount decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.PrjWorkingOut', 'Description') IS NULL ALTER TABLE dbo.PrjWorkingOut ADD Description nvarchar(255) NULL;
            IF COL_LENGTH('dbo.BuyDocument', 'InsuranceDECAmount') IS NULL
                ALTER TABLE dbo.BuyDocument ADD InsuranceDECAmount decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.BuyDocument', 'InsuranceTRCAmount') IS NULL
                ALTER TABLE dbo.BuyDocument ADD InsuranceTRCAmount decimal(26, 6) NULL;
            IF COL_LENGTH('dbo.BuyDocument', 'AmountRegul') IS NULL ALTER TABLE dbo.BuyDocument ADD AmountRegul decimal(26, 6) NULL;""";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final FixtureRapports rapports;
    private final FixtureAchat achat;
    private final FixtureLotFiche fiche;
    private final FixtureProjetLots lots;

    public FixtureRapportsProjet(EntityManager em, JdbcTemplate jdbc, FixtureRapports rapports, FixtureAchat achat,
                                 FixtureLotFiche fiche, FixtureProjetLots lots) {
        this.em = em;
        this.jdbc = jdbc;
        this.rapports = rapports;
        this.achat = achat;
        this.fiche = fiche;
        this.lots = lots;
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    /** Colonnes, genre des restitutions et les 4 lignes {@code AdmReportData} du groupe « PROJET » ; rend code → id. */
    public Map<String, UUID> rapports() {
        rapports.tables();
        maj(COLONNES);
        maj("""
                IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = ?)
                    INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, N'PrjDocumentWarrantyRestitution', N'Restitution RG')""",
                s(ENTITE_RESTITUTION), s(ENTITE_RESTITUTION));
        Map<String, UUID> ids = new LinkedHashMap<>();
        int ordre = 1;
        for (String code : CODES) {
            ids.put(code, rapports.rapport(TYPE_PROJET, code, "RAPPORT " + code, "PROJET", ordre++));
        }
        return ids;
    }

    /** Colonne de {@code PrjMarket} (ratio en fraction, blocage, fournisseur). */
    public void marche(UUID lot, String colonne, Object valeur) {
        maj("UPDATE PrjMarket SET " + colonne + " = ? WHERE PKey = ?", valeur instanceof UUID u ? s(u) : valeur, s(lot));
    }

    public void fournisseur(UUID lot, UUID fournisseur) {
        marche(lot, "CfgSupplierId", fournisseur);
    }

    /** Décompte du lot : RG {@code rg}, prorata décompté {@code prorata}, remboursement {@code rembourse}, date nulle possible. */
    public UUID decompte(UUID lot, String code, int numero, String rg, String prorata, String rembourse, LocalDate date) {
        UUID id = lots.decompte(lot, code, numero, Map.of(FixtureProjetLots.TTC, new BigDecimal("1200"),
                FixtureProjetLots.RG, new BigDecimal(rg), FixtureProjetLots.PRORATA, new BigDecimal(rembourse),
                FixtureProjetLots.A_PAYER, new BigDecimal("1000")));
        maj("UPDATE PrjWorkingOut SET ProportionAmount = ?, Description = ?, DocDate = ? WHERE PKey = ?",
                new BigDecimal(prorata), "Decompte " + code, FixtureLotFiche.jour(date), s(id));
        return id;
    }

    /** Facture de décompte du lot (INVOICE, entité B604) portant ces RG et assurances. */
    public UUID facture(UUID lot, String code, String rg, String dec, String trc) {
        UUID id = fiche.factureLot(lot, code, "800", "960");
        achat.poser(id, "WarrantyAmount", new BigDecimal(rg));
        achat.poser(id, "InsuranceDECAmount", new BigDecimal(dec));
        achat.poser(id, "InsuranceTRCAmount", new BigDecimal(trc));
        return id;
    }

    /** Pièce de restitution de RG (entité A71B4F91) : {@code AmountRegul} donné, réglée de {@code regle} (ligne de règlement). */
    public UUID restitution(UUID lot, String code, String amountRegul, String regle) {
        UUID id = fiche.pieceLot(lot, code, FixtureAchat.DOCUMENT_A_REGLER, null, ENTITE_RESTITUTION, new BigDecimal(regle),
                new BigDecimal(regle));
        achat.poser(id, "AmountRegul", new BigDecimal(amountRegul));
        achat.poser(id, "WarrantyAmount", BigDecimal.ZERO);
        lots.reglement(id, code + "-R", new BigDecimal(regle), null);
        return id;
    }
}
