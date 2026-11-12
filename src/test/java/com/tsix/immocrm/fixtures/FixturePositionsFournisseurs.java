package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lot A3 « positions fournisseurs » (FEATURE-achat-impayes-soldes.md §3.1) : mouvements de {@code BuySupplierPositionView}
 * recréés sur tables (vue absente de yesRemsTest et de la CI). Parents écrits avant les FK de la CI ; tout est annulé
 * avec la transaction du test. S'appuie sur {@link FixtureAchat} pour sociétés, projets, comptes et habilitations.
 */
@Component
public class FixturePositionsFournisseurs {

    /** PKey de production de {@code CfgTierType} FOU ({@code CfgTierType.Supplier} d'OMRAN) et CLI. */
    public static final UUID TYPE_FOURNISSEUR = UUID.fromString("00685105-DFD9-497B-8830-DBF0FBE91668");
    public static final UUID TYPE_CLIENT = UUID.fromString("07830036-EF72-492A-AC39-0A407392DA62");

    private static final String P_ID = "id";
    private static final String P_CODE = "code";

    private final EntityManager em;
    private final FixtureAchat achat;

    public FixturePositionsFournisseurs(EntityManager em, FixtureAchat achat) {
        this.em = em;
        this.achat = achat;
    }

    // ------------------------------------------------------------------ tiers

    /** Tiers de type FOU (seul type compté par la position, §2 n° 4) ; même nom en Description et raison sociale. */
    public UUID fournisseur(String code, String nom) {
        return tiers(code, nom, TYPE_FOURNISSEUR, "FOU", "FOURNISSEUR");
    }

    /** Tiers d'un autre type (CLI) : jamais une ligne de position. */
    public UUID client(String code, String nom) {
        return tiers(code, nom, TYPE_CLIENT, "CLI", "CLIENT");
    }

    private UUID tiers(String code, String nom, UUID type, String codeType, String libelleType) {
        executer(em.createNativeQuery("""
                        IF NOT EXISTS (SELECT 1 FROM CfgTierType WHERE PKey = :id)
                            INSERT INTO CfgTierType (PKey, Code, Description) VALUES (:id, :code, :libelle)""")
                .setParameter(P_ID, type).setParameter(P_CODE, codeType).setParameter("libelle", libelleType));
        UUID id = achat.fournisseur(code, nom);
        achat.poserSur("CfgTier", id, "CfgTierTypeId", type);
        return id;
    }

    // ------------------------------------------------------------------ pièces

    /** Pièce payable ({@code CanBePayed = 1}) : TTC = à payer = {@code aPayer}, échéance = date + 30 j ; projet nul possible. */
    public UUID piece(String code, String nature, LocalDate date, UUID fournisseur, String nom, UUID societe,
                      UUID projet, String aPayer) {
        UUID id = achat.piece(FixtureAchat.Piece.facture(code, date, fournisseur, nom, societe, projet,
                new BigDecimal(aPayer)).nature(nature));
        achat.poser(id, "CanBePayed", true);
        return id;
    }

    /** Avoir payable ; {@code financier} = {@code IsFinancialRefund} ; {@code source} = facture d'origine, nulle possible. */
    public UUID avoir(String code, LocalDate date, UUID fournisseur, String nom, UUID societe, UUID projet,
                      String aPayer, boolean financier, UUID source) {
        UUID id = piece(code, FixtureAchat.AVOIR, date, fournisseur, nom, societe, projet, aPayer);
        achat.poser(id, "IsFinancialRefund", financier);
        if (source != null) {
            achat.poser(id, "InvoiceSourceId", source);
        }
        return id;
    }

    // ------------------------------------------------------------------ règlements

    /** Entité OMRAN ({@code StrEntity}) d'un règlement : la vue exige un {@code StrEntityId} existant. */
    public UUID entiteReglement(String jeton) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("INSERT INTO StrEntity (PKey, Code, Description) VALUES (:id, :code, :code)")
                .setParameter(P_ID, id).setParameter(P_CODE, "BuySettlement-" + jeton));
        return id;
    }

    /** Règlement d'un fournisseur ; {@code estCredit} nul = {@code IsCredit} NULL ; {@code Amount} = base − RAS. */
    public record Reglement(String code, LocalDate date, UUID societe, UUID fournisseur, UUID entite,
                            String base, String ras, Boolean estCredit, String statut) {

        public static Reglement debit(String code, LocalDate date, UUID societe, UUID fournisseur, UUID entite,
                                      String base) {
            return new Reglement(code, date, societe, fournisseur, entite, base, "0", false, FixtureAchat.EN_COURS);
        }

        public Reglement credit(Boolean c) {
            return new Reglement(code, date, societe, fournisseur, entite, base, ras, c, statut);
        }

        public Reglement statut(String s) {
            return new Reglement(code, date, societe, fournisseur, entite, base, ras, estCredit, s);
        }

        public Reglement ras(String r) {
            return new Reglement(code, date, societe, fournisseur, entite, base, r, estCredit, statut);
        }

        public Reglement entite(UUID e) {
            return new Reglement(code, date, societe, fournisseur, e, base, ras, estCredit, statut);
        }
    }

    /** Règlement sans ligne ; lignes ajoutées par {@link #ligne}. */
    public UUID reglement(Reglement r) {
        UUID id = UUID.randomUUID();
        BigDecimal base = new BigDecimal(r.base());
        BigDecimal ras = new BigDecimal(r.ras());
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlement (PKey, Code, Description, DocDate, Amount, AmountBase, AmountRS, Status,
                            IsCredit, CfgCompanyId, TierId)
                        VALUES (:id, :code, :code, :date, :montant, :base, :ras, :statut, 0, :societe, :tiers)""")
                .setParameter(P_ID, id).setParameter(P_CODE, r.code()).setParameter("date", r.date().atStartOfDay())
                .setParameter("montant", base.subtract(ras)).setParameter("base", base).setParameter("ras", ras)
                .setParameter("statut", r.statut()).setParameter("societe", r.societe())
                .setParameter("tiers", r.fournisseur()));
        if (r.estCredit() == null) {
            executer(em.createNativeQuery("UPDATE StlSettlement SET IsCredit = NULL WHERE PKey = :id").setParameter(P_ID, id));
        } else if (Boolean.TRUE.equals(r.estCredit())) {
            achat.poserSur("StlSettlement", id, "IsCredit", true);
        }
        if (r.entite() != null) {
            achat.poserSur("StlSettlement", id, "StrEntityId", r.entite());
        }
        return id;
    }

    /** Ligne de règlement ({@code StlSettlementLine}) sur la pièce. */
    public void ligne(UUID reglement, UUID piece, String montant) {
        achat.ligneReglement(reglement, piece, new BigDecimal(montant));
    }

    // ------------------------------------------------------------------ outils

    /** Ventilation par projet ({@code BuyVentilationExpense}) : taux de projet de la pièce. */
    public void ventilation(UUID piece, UUID projet, String taux) {
        achat.ventilationProjet(piece, projet, new BigDecimal(taux));
    }

    private void executer(Query query) {
        em.flush();
        query.executeUpdate();
    }
}
