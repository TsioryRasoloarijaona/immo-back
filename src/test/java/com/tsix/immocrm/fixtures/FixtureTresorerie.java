package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lot N3 — trésorerie (FEATURE-n3-recettes-depenses.md). Règlements {@code StlSettlement} et lignes {@code StlSettlementLine}
 * sur tables (jamais la vue), pièces {@code BuyDocument} au module donné. SQL natif, parents avant enfants (FK de la CI),
 * annulé avec la transaction du test.
 */
@Component
public class FixtureTresorerie {

    public static final String ROLE = "Commercial";

    public static final String DROIT_DECAISSEMENTS = "Yes.Stl.Front.Debit.SettlementList";
    public static final String DROIT_AFFECTATIONS = "Yes.Stl.Front.Debit.SettlementAffectationList";
    public static final String DROIT_PROFORMA = "Yes.Stl.Front.Credit.InvoiceProformaList";
    public static final String DROIT_FACTURES = "Yes.Stl.Front.Credit.InvoiceList";
    public static final String DROIT_AVOIRS = "Yes.Stl.Front.Credit.RefundList";
    public static final String DROIT_ENCAISSEMENTS = "Yes.Stl.Front.Credit.SettlementList";
    public static final String DROIT_AFFECTATIONS_RECETTE = "Yes.Stl.Front.Credit.SettlementAffectationList";

    public static final String EN_COURS = FixtureAchat.EN_COURS;
    public static final String POINTE = FixtureAchat.POINTE;
    public static final String ANNULE = FixtureAchat.ANNULE;

    /** Échéance par défaut, loin dans le futur (smalldatetime s'arrête en 2079) : jamais « en retard ». */
    public static final LocalDate ECHEANCE_LOINTAINE = LocalDate.of(2078, 12, 31);

    private static final String P_ID = "id";

    private final EntityManager em;
    private final FixtureAchat achat;

    public FixtureTresorerie(EntityManager em, FixtureAchat achat) {
        this.em = em;
        this.achat = achat;
    }

    /** Société habilitée, son projet habilité, un projet non habilité, un fournisseur, un type de règlement, modules. */
    public record Cadre(String jeton, UUID societe, UUID projet, UUID projetNonHabilite, UUID fournisseur,
                        String fournisseurNom, UUID type, UUID modulePrj, UUID moduleBuy) {

        public String code(String suffixe) {
            return jeton + suffixe;
        }
    }

    public Cadre cadre(String prefixe) {
        String jeton = FixtureAchat.jeton(prefixe);
        UUID societe = achat.societe("SOC-" + jeton);
        String nom = "Fournisseur " + jeton;
        return new Cadre(jeton, societe, achat.projet(jeton + "-PA", societe), achat.projet(jeton + "-PB", societe),
                achat.fournisseur("FOU-" + jeton, nom), nom, achat.typeReglement("VIR-" + jeton),
                achat.module("PRJ"), achat.module("BUY"));
    }

    /** Compte (rôle {@link #ROLE}) habilité à la société et au projet du cadre, droits OMRAN donnés. */
    public AdmUser compte(Cadre c, String code, String... droits) {
        AdmUser compte = achat.compte(code + "-" + c.jeton(), ROLE);
        achat.habiliterSociete(compte, c.societe());
        achat.habiliter(compte, c.projet());
        for (String droit : droits) {
            achat.droitOmran(compte, droit);
        }
        return compte;
    }

    /** Facture d'achat de la société et du projet donnés ({@code projet} nul = sans projet), au module donné. */
    public UUID piece(String code, UUID societe, UUID projet, UUID fournisseur, String nom, UUID module, String montant) {
        UUID id = achat.piece(Piece.facture(code, LocalDate.of(2026, 3, 2), fournisseur, nom, societe, projet,
                new BigDecimal(montant)));
        achat.poser(id, "AdmModuleId", module);
        return id;
    }

    /**
     * Règlement à saisir ; {@code statut} nul = colonne NULL ; {@code pointage} nul = non pointé. Conversion
     * {@code ConvertValue / ConvertUnit} (1 / 1 par défaut).
     */
    public record Reglement(String code, UUID societe, UUID module, UUID tiers, UUID type, String statut, boolean credit,
                            String montant, String base, LocalDate piece, LocalDate echeance, LocalDate pointage,
                            String valeurConversion, int uniteConversion) {

        public static Reglement debit(String code, Cadre c, String montant) {
            return new Reglement(code, c.societe(), c.modulePrj(), c.fournisseur(), c.type(), EN_COURS, false, montant,
                    montant, LocalDate.of(2026, 4, 1), ECHEANCE_LOINTAINE, null, "1", 1);
        }

        public Reglement statut(String s) {
            return new Reglement(code, societe, module, tiers, type, s, credit, montant, base, piece, echeance, pointage,
                    valeurConversion, uniteConversion);
        }

        public Reglement societe(UUID s) {
            return new Reglement(code, s, module, tiers, type, statut, credit, montant, base, piece, echeance, pointage,
                    valeurConversion, uniteConversion);
        }

        public Reglement module(UUID m) {
            return new Reglement(code, societe, m, tiers, type, statut, credit, montant, base, piece, echeance, pointage,
                    valeurConversion, uniteConversion);
        }

        public Reglement base(String b) {
            return new Reglement(code, societe, module, tiers, type, statut, credit, montant, b, piece, echeance, pointage,
                    valeurConversion, uniteConversion);
        }

        public Reglement dates(LocalDate p, LocalDate e, LocalDate pt) {
            return new Reglement(code, societe, module, tiers, type, statut, credit, montant, base, p, e, pt,
                    valeurConversion, uniteConversion);
        }

        public Reglement conversion(String valeur, int unite) {
            return new Reglement(code, societe, module, tiers, type, statut, credit, montant, base, piece, echeance,
                    pointage, valeur, unite);
        }
    }

    public UUID reglement(Reglement r) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlement (PKey, Code, Description, DocDate, EcheanceDate, Amount, AmountBase, Status,
                            IsCredit, StlSettlementTypeId, TierId, CfgCompanyId, AdmModuleId, ConvertValue, ConvertUnit,
                            IsAccounted)
                        VALUES (:id, :code, :description, :piece, :echeance, :montant, :base, :statut, :credit, :type,
                            :tiers, :societe, :module, :valeur, :unite, 0)""")
                .setParameter(P_ID, id).setParameter("code", r.code()).setParameter("description", "Règlement " + r.code())
                .setParameter("piece", r.piece().atStartOfDay()).setParameter("echeance", r.echeance().atStartOfDay())
                .setParameter("montant", new BigDecimal(r.montant())).setParameter("base", new BigDecimal(r.base()))
                .setParameter("statut", r.statut() == null ? EN_COURS : r.statut()).setParameter("credit", r.credit())
                .setParameter("type", r.type()).setParameter("tiers", r.tiers()).setParameter("societe", r.societe())
                .setParameter("module", r.module()).setParameter("valeur", new BigDecimal(r.valeurConversion()))
                .setParameter("unite", r.uniteConversion()));
        if (r.statut() == null) {
            executer(em.createNativeQuery("UPDATE StlSettlement SET Status = NULL WHERE PKey = :id").setParameter(P_ID, id));
        }
        if (r.pointage() != null) {
            executer(em.createNativeQuery("UPDATE StlSettlement SET SettlementDate = :date WHERE PKey = :id")
                    .setParameter(P_ID, id).setParameter("date", r.pointage().atStartOfDay()));
        }
        return id;
    }

    /** Affectation {@code StlSettlementLine} du règlement à une pièce d'achat ; rend l'id de la ligne. */
    public UUID ligne(UUID reglement, UUID piece, String montant) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlementLine (PKey, StlSettlementId, BuyDocumentId, Amount)
                        VALUES (:id, :reglement, :piece, :montant)""")
                .setParameter(P_ID, id).setParameter("reglement", reglement).setParameter("piece", piece)
                .setParameter("montant", new BigDecimal(montant)));
        return id;
    }

    private void executer(Query query) {
        em.flush();
        query.executeUpdate();
    }
}
