package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lot L1 (FEATURE-lots-liste-parite.md) : genres de lot ({@code StrEntity} aux PKeys de prod, absents de yesRemsTest), statuts,
 * entités juridiques, lots typés, droits OMRAN ({@code AdmRight}), séquence 'Market', transitions et pièces d'achat du lot.
 * Tout en SQL natif, parents avant enfants (FK de la CI) ; annulé avec la transaction du test.
 */
@Component
public class FixtureLotsParite {

    public static final String ENTREPRENEURS = "ENTREPRENEURS";
    public static final String FOURNISSEURS = "FOURNISSEURS";
    public static final String ADMINISTRATION = "ADMINISTRATION";
    public static final String MAITRES_OEUVRE = "MAITRES_OEUVRE";
    public static final String CONCESSIONNAIRES = "CONCESSIONNAIRES";

    /** Genre du fil → {@code StrEntity} de prod (PKey, Code). */
    public static final Map<String, UUID> GENRES = genres();
    private static final Map<String, String> CODES_GENRE = Map.of(
            ENTREPRENEURS, "PrjMarket", FOURNISSEURS, "PrjMarketMultiSupplier", ADMINISTRATION, "PrjMarketAdministrative",
            MAITRES_OEUVRE, "PrjMarketStudies", CONCESSIONNAIRES, "PrjMarketDealers");

    public static final UUID TYPE_STATUT_NOUVEAU = UUID.fromString("1004DFA9-D4C9-4C61-A3AF-44F174A76D53");

    public static final String DROIT_NOUVEAU = "Yes.Prj.Front.Market.MarketMonitoringList.toolNew";
    public static final String DROIT_NOUVEAU_MULTI = "Yes.Prj.Front.Market.MarketMonitoringList.toolNewMultiSupplier";
    public static final String DROIT_SUPPRIMER = "Yes.Prj.Front.Market.MarketMonitoringList.toolDelete";
    public static final String DROIT_EXPORTER = "Yes.Prj.Front.Market.MarketMonitoringList.toolExport";

    public static final String SEQUENCE_MARCHE = "Market";

    private static final String ADMINISTRATEUR = "Administrateur";

    private final EntityManager em;
    private final JdbcTemplate jdbc;
    private final FixtureDossierVente vente;
    private final FixtureProjetLots lots;

    public FixtureLotsParite(EntityManager em, JdbcTemplate jdbc, FixtureDossierVente vente, FixtureProjetLots lots) {
        this.em = em;
        this.jdbc = jdbc;
        this.vente = vente;
        this.lots = lots;
    }

    private static Map<String, UUID> genres() {
        Map<String, UUID> m = new LinkedHashMap<>();
        m.put(ENTREPRENEURS, UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B613"));
        m.put(FOURNISSEURS, UUID.fromString("E7999F70-7507-40D9-AA78-A2825C42B614"));
        m.put(ADMINISTRATION, UUID.fromString("E7999F71-7507-40D9-AA78-A2825C42B614"));
        m.put(MAITRES_OEUVRE, UUID.fromString("E7999F72-7507-40D9-AA78-A2825C42B614"));
        m.put(CONCESSIONNAIRES, UUID.fromString("E7999F74-7507-40D9-AA78-A2825C42B614"));
        return Map.copyOf(m);
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    // ------------------------------------------------------------------ genres et statuts

    /** Les 5 {@code StrEntity} de genre et le type de statut « Nouveau », créés s'ils manquent. */
    public void genresDeLot() {
        GENRES.forEach((genre, id) -> maj("""
                IF NOT EXISTS (SELECT 1 FROM StrEntity WHERE PKey = ?)
                    INSERT INTO StrEntity (PKey, Code, Description) VALUES (?, ?, ?)""",
                s(id), s(id), CODES_GENRE.get(genre), "Lot " + genre));
        maj("""
                IF NOT EXISTS (SELECT 1 FROM StrStatusType WHERE PKey = ?)
                    INSERT INTO StrStatusType (PKey, Code, Description, Sorting) VALUES (?, N'Nouveau', N'Nouveau', 0)""",
                s(TYPE_STATUT_NOUVEAU), s(TYPE_STATUT_NOUVEAU));
    }

    /** Statut du genre, couleur .NET {@code LimeGreen}, sans type. Code = libellé. */
    public UUID statut(String genre, String code) {
        return statut(genre, code, null);
    }

    /** Statut « de départ » du genre ({@code StrStatusTypeId} = Nouveau, comme {@code StrStatus.GetStartStatus}). */
    public UUID statutDepart(String genre, String code) {
        return statut(genre, code, TYPE_STATUT_NOUVEAU, 1);
    }

    /** Statut « de départ » d'ordre ({@code Sorting}) donné. */
    public UUID statutDepart(String genre, String code, int ordre) {
        return statut(genre, code, TYPE_STATUT_NOUVEAU, ordre);
    }

    private UUID statut(String genre, String code, UUID type) {
        return statut(genre, code, type, 1);
    }

    private UUID statut(String genre, String code, UUID type, int ordre) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO StrStatus (PKey, Code, Description, Sorting, StrConfigEntityId, StrStatusTypeId, ColorName)
                VALUES (?, ?, ?, ?, ?, ?, N'LimeGreen')""", s(id), code, code, ordre, s(GENRES.get(genre)), s(type));
        return id;
    }

    /** Un statut de départ pour chacun des 5 genres. */
    public void statutsDepart(String jeton) {
        GENRES.keySet().forEach(g -> statutDepart(g, "DEP-" + g + "-" + jeton));
    }

    // ------------------------------------------------------------------ entités, projets, lots

    public UUID entiteJuridique(String code) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO CfgCompany (PKey, Code, Description, IsActive) VALUES (?, ?, ?, 1)", s(id), code, "Entite " + code);
        return id;
    }

    public UUID projet(String code, UUID entiteJuridique) {
        UUID id = UUID.randomUUID();
        maj("INSERT INTO PrjProject (PKey, Code, Description, CfgCompanyId) VALUES (?, ?, ?, ?)",
                s(id), code, "Projet " + code, s(entiteJuridique));
        return id;
    }

    /** Lot du genre sur ce statut ; estimations manuelles à 0 comme les lots vides de prod. {@code projet} nul = sans projet. */
    public UUID lot(UUID projet, String code, String genre, UUID statut) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO PrjMarket (PKey, Code, Description, PrjProjectId, StrEntityId, StrStatusId, WarrantyRatio,
                    EstimatedPriceHTManual, EstimatedVatAmountManual, EstimatedPriceTTCManual, IsMultiSupplier)
                VALUES (?, ?, ?, ?, ?, ?, 0.1, 0, 0, 0, ?)""",
                s(id), code, "Lot " + code, s(projet), s(GENRES.get(genre)), s(statut), !ENTREPRENEURS.equals(genre));
        return id;
    }

    public void notes(UUID lot, String notes) {
        maj("UPDATE PrjMarket SET Notes = ? WHERE PKey = ?", notes, s(lot));
    }

    public void statutDuLot(UUID lot, UUID statut) {
        maj("UPDATE PrjMarket SET StrStatusId = ? WHERE PKey = ?", s(statut), s(lot));
    }

    // ------------------------------------------------------------------ comptes et droits OMRAN

    /** Rôle « Administrateur » (tous les droits OMRAN), exempté de la clause de statut, habilité sur ces projets. */
    public AdmUser administrateur(String code, UUID... projets) {
        AdmUser compte = vente.compte(code, ADMINISTRATEUR);
        maj("""
                UPDATE AdmRole SET WithSequenceFreeRestriction = 0
                WHERE PKey IN (SELECT AdmRoleId FROM AdmUserRole WHERE AdmUserId = ?)""", s(compte.getId()));
        for (UUID projet : projets) {
            lots.habiliter(compte, projet);
        }
        return compte;
    }

    /** Compte à rôle propre exempté (jamais Administrateur), portant exactement ces droits, habilité sur ces projets. */
    public AdmUser avecDroits(String code, List<String> droits, UUID... projets) {
        AdmUser compte = lots.compte(code);
        String role = s(roleDe(compte));
        for (String droit : droits) {
            maj("INSERT INTO AdmRight (PKey, Code, Description, AdmRoleId) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID().toString(), droit, droit, role);
        }
        for (UUID projet : projets) {
            lots.habiliter(compte, projet);
        }
        return compte;
    }

    /** Compte à rôle restreint ({@code WithSequenceFreeRestriction = 1}), habilité sur ces projets. */
    public AdmUser restreint(String code, UUID... projets) {
        AdmUser compte = vente.compte(code, "RESTREINT-" + code);
        maj("""
                UPDATE AdmRole SET WithSequenceFreeRestriction = 1
                WHERE PKey IN (SELECT AdmRoleId FROM AdmUserRole WHERE AdmUserId = ?)""", s(compte.getId()));
        for (UUID projet : projets) {
            lots.habiliter(compte, projet);
        }
        return compte;
    }

    public UUID roleDe(AdmUser compte) {
        em.flush();
        return UUID.fromString(jdbc.queryForObject(
                "SELECT TOP 1 CAST(AdmRoleId AS varchar(36)) FROM AdmUserRole WHERE AdmUserId = ? ORDER BY AdmRoleId ASC",
                String.class, s(compte.getId())));
    }

    // ------------------------------------------------------------------ séquence 'Market'

    /** Ligne 'Market' au format de prod ({@code yy/nnnnn}, année système) posée sur {@code valeur}. */
    public void sequenceMarche(int valeur) {
        maj("""
                IF EXISTS (SELECT 1 FROM AdmSequence WHERE Code = ?)
                    UPDATE AdmSequence SET SeqValue = ?, SeqYearNumber = 2, SeqValueNumber = 5, Separator = N'/',
                        BasedOnSystemYear = 1, SeqPrefix = NULL, SeqSuffix = NULL WHERE Code = ?
                ELSE
                    INSERT INTO AdmSequence (PKey, Code, SeqValue, SeqYearNumber, SeqValueNumber, Separator, BasedOnSystemYear)
                    VALUES (?, ?, ?, 2, 5, N'/', 1)""",
                SEQUENCE_MARCHE, valeur, SEQUENCE_MARCHE, UUID.randomUUID().toString(), SEQUENCE_MARCHE, valeur);
    }

    // ------------------------------------------------------------------ transitions

    /** {@code StrWorkFlow} du genre ; code = libellé. */
    public UUID transition(String genre, UUID depuis, UUID vers, String libelle) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO StrWorkFlow (PKey, StatusFromId, StatusToId, Sorting, Code, Description, WithComment,
                    WithPassword, WithUserDate, WithAutomaticTransition, StrConfigEntityId)
                VALUES (?, ?, ?, 10, ?, ?, 0, 0, 0, 0, ?)""",
                s(id), s(depuis), s(vers), libelle, libelle, s(GENRES.get(genre)));
        return id;
    }

    /**
     * Intervenant « rôle » : voit le statut ({@code StrStatusIntervener}) et, si {@code transition} non nulle, peut la passer
     * ({@code StrWorkFlowIntervener}, {@code WithPassword = 0}).
     */
    public void intervenant(String genre, UUID role, UUID statut, UUID transition) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO StrIntervener (PKey, Code, Description, AdmRoleId, StrConfigEntityId) VALUES (?, ?, ?, ?, ?)""",
                s(id), "INT-" + id.toString().substring(0, 8), "Intervenant de test", s(role), s(GENRES.get(genre)));
        maj("INSERT INTO StrStatusIntervener (PKey, StrIntervenerId, StrStatusId) VALUES (?, ?, ?)",
                UUID.randomUUID().toString(), s(id), s(statut));
        if (transition != null) {
            maj("INSERT INTO StrWorkFlowIntervener (PKey, StrIntervenerId, StrWorkFlowId, WithPassword) VALUES (?, ?, ?, 0)",
                    UUID.randomUUID().toString(), s(id), s(transition));
        }
    }

    // ------------------------------------------------------------------ pièces, règlements, références

    /** Pièce d'achat rattachée au lot par {@code BuyDocument.PrjMarketId} (le lien que lit {@code AmountSettled}). */
    public UUID pieceDuLot(UUID lot, String code, BigDecimal montant) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BuyDocument (PKey, Code, DocType, DocDate, AmountHT, AmountTTC, AmountToPay, PrjMarketId)
                VALUES (?, ?, N'INVOICE', ?, ?, ?, ?, ?)""",
                s(id), code, Timestamp.valueOf(LocalDateTime.of(2026, 3, 20, 0, 0)), montant, montant, montant, s(lot));
        return id;
    }

    /** Ligne de bordereau ({@code PrjMarketLine}, enfant {@code ON DELETE CASCADE} du lot). */
    public UUID ligneBordereau(UUID lot, String code) {
        cleEtrangere(LIGNE_BORDEREAU);
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO PrjMarketLine (PKey, Code, Description, Quantity, UnitPriceHT, VatRatio, UnitPriceTTC, PrjMarketId)
                VALUES (?, ?, ?, 1, 100, 0.2, 120, ?)""", s(id), code, code, s(lot));
        return id;
    }

    /** Décompte du lot ({@code PrjWorkingOut}, FK {@code NO_ACTION} vers le lot). */
    public UUID decompteDuLot(UUID lot, String code) {
        cleEtrangere(DECOMPTE);
        return lots.decompte(lot, code, 1, Map.of(FixtureProjetLots.TTC, new BigDecimal("1200.00"),
                FixtureProjetLots.A_PAYER, new BigDecimal("1200.00")));
    }

    /** Pièce d'achat qui cite le lot ({@code BuyDocument.PrjMarketId}, FK {@code NO_ACTION}). */
    public UUID pieceQuiReferenceLeLot(UUID lot, String code) {
        cleEtrangere(PIECE);
        return pieceDuLot(lot, code, new BigDecimal("10.00"));
    }

    private record Cle(String nom, String table, String colonne, boolean cascade) {
    }

    private static final Cle LIGNE_BORDEREAU = new Cle("FK_L1_PrjMarketLine_PrjMarket", "PrjMarketLine", "PrjMarketId", true);
    private static final Cle DECOMPTE = new Cle("FK_L1_PrjWorkingOut_PrjMarket", "PrjWorkingOut", "PrjMarketId", false);
    private static final Cle PIECE = new Cle("FK_L1_BuyDocument_PrjMarket", "BuyDocument", "PrjMarketId", false);

    // yesRemsTest n'a aucune FK (la CI les a) : posée WITH NOCHECK si absente, retirée par le rollback. Noms écrits en dur ici.
    private void cleEtrangere(Cle cle) {
        maj("IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo." + cle.table()
                + "') AND referenced_object_id = OBJECT_ID(N'dbo.PrjMarket')) ALTER TABLE dbo." + cle.table()
                + " WITH NOCHECK ADD CONSTRAINT " + cle.nom() + " FOREIGN KEY (" + cle.colonne()
                + ") REFERENCES dbo.PrjMarket (PKey)" + (cle.cascade() ? " ON DELETE CASCADE" : ""));
    }

    // ------------------------------------------------------------------ lecture

    /** Lignes natives, paramètres liés ; la session JPA est vidée avant pour voir les écritures de l'appel. */
    public List<Map<String, Object>> lignes(String sql, Object... parametres) {
        em.flush();
        return jdbc.queryForList(sql, parametres);
    }

    public int compter(String sql, Object... parametres) {
        em.flush();
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }
}
