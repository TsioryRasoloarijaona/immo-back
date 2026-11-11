package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Lot A2 — dépenses du projet (FEATURE-achat-depenses-projet.md §6). Tout en SQL natif, parents avant enfants (FK de la
 * CI), annulé avec la transaction du test. Jeu :
 * <pre>
 *   projet A : lot A1 ← FA1 1000 (décompte D1, n° 1) réglée RG1 600 IN_WORK + RG2 200 CANCELED
 *                     ← FA2  500 (décompte D2, n° 2) sans règlement
 *              hors lot ← FH1 300 réglée RG3 320 (trop réglée, reste −20)
 *                       ← FH2 TTC 400, à payer 380, réglée par RG4 à cheval : 150 sur FH2 + 250 sur FB1
 *                       ← DTP DOCUMENT_TO_PAY 700 (hors synthèse)
 *   projet B : lot B ← FB1 800 (ligne de 250 de RG4)
 *   toutes les pièces au module PRJ ; « a » habilité société + A ; « ab » société + A et B ; rôle Commercial.
 * </pre>
 */
@Component
public class FixtureDepensesProjet {

    public static final String ROLE = "Commercial";

    private static final String P_ID = "id";

    private final EntityManager em;
    private final FixtureAchat achat;
    private final FixtureProjetLots projets;
    private final FixtureVisibiliteOmran visibilite;

    public FixtureDepensesProjet(EntityManager em, FixtureAchat achat, FixtureProjetLots projets,
                                 FixtureVisibiliteOmran visibilite) {
        this.em = em;
        this.achat = achat;
        this.projets = projets;
        this.visibilite = visibilite;
    }

    /** Ids et codes du jeu ; les codes de pièce et de règlement sont {@code jeton + suffixe}. */
    public record Jeu(String jeton, AdmUser compteA, AdmUser compteAB, UUID societe, UUID fournisseur,
                      String fournisseurNom, String typeCode,
                      UUID projetA, UUID projetB, UUID lotA1, UUID lotB,
                      UUID factureA1, UUID factureA2, UUID factureTropReglee, UUID factureACheval, UUID documentAPayer,
                      UUID factureB, UUID decompte1, UUID decompte2,
                      UUID reglementEnCours, UUID reglementAnnule, UUID reglementTropRegle, UUID reglementACheval,
                      UUID ligneEnCours, UUID ligneAnnulee, UUID ligneAChevalA, UUID ligneAChevalB) {

        public String code(String suffixe) {
            return jeton + suffixe;
        }

        public String projetACode() {
            return code("-PA");
        }

        public String projetBCode() {
            return code("-PB");
        }

        public String lotA1Code() {
            return code("-LA1");
        }

        public String decompte1Code() {
            return code("-DZ1");
        }

        public String decompte2Code() {
            return code("-DA2");
        }
    }

    public Jeu jeu() {
        String jeton = FixtureAchat.jeton("DEP");
        UUID societe = achat.societe("SOC-" + jeton);
        String nom = "Fournisseur " + jeton;
        UUID fournisseur = achat.fournisseur("FOU-" + jeton, nom);
        String typeCode = "VIR-" + jeton;
        UUID type = achat.typeReglement(typeCode);
        UUID projetA = achat.projet(jeton + "-PA", societe);
        UUID projetB = achat.projet(jeton + "-PB", societe);
        UUID lotA1 = achat.lot(jeton + "-LA1", projetA);
        UUID lotB = achat.lot(jeton + "-LB", projetB);

        UUID fa1 = facture(jeton + "-FA1", LocalDate.of(2026, 3, 20), fournisseur, nom, societe, projetA, lotA1, "1000");
        UUID fa2 = facture(jeton + "-FA2", LocalDate.of(2026, 3, 10), fournisseur, nom, societe, projetA, lotA1, "500");
        UUID fh1 = facture(jeton + "-FH1", LocalDate.of(2026, 3, 5), fournisseur, nom, societe, projetA, null, "300");
        UUID fh2 = facture(jeton + "-FH2", LocalDate.of(2026, 3, 6), fournisseur, nom, societe, projetA, null, "400");
        achat.poser(fh2, "AmountToPay", new BigDecimal("380"));
        UUID dtp = achat.piece(Piece.facture(jeton + "-DTP", LocalDate.of(2026, 3, 7), fournisseur, nom, societe,
                projetA, new BigDecimal("700")).nature("DOCUMENT_TO_PAY"));
        visibilite.modulePiece(dtp, visibilite.modulePrj());
        UUID fb1 = facture(jeton + "-FB1", LocalDate.of(2026, 3, 8), fournisseur, nom, societe, projetB, lotB, "800");

        UUID d1 = decompte(lotA1, jeton + "-DZ1", 1, "1000");
        UUID d2 = decompte(lotA1, jeton + "-DA2", 2, "500");
        lier(d1, fa1);
        lier(d2, fa2);

        UUID rg1 = reglement(jeton + "-RG1", FixtureAchat.EN_COURS, LocalDate.of(2026, 4, 10), type, "600");
        UUID l1 = ligne(rg1, fa1, "600");
        UUID rg2 = reglement(jeton + "-RG2", FixtureAchat.ANNULE, LocalDate.of(2026, 4, 10), type, "200");
        UUID l2 = ligne(rg2, fa1, "200");
        UUID rg3 = reglement(jeton + "-RG3", FixtureAchat.EN_COURS, LocalDate.of(2026, 4, 12), type, "320");
        ligne(rg3, fh1, "320");
        UUID rg4 = reglement(jeton + "-RG4", FixtureAchat.EN_COURS, LocalDate.of(2026, 4, 13), type, "400");
        UUID l4a = ligne(rg4, fh2, "150");
        UUID l4b = ligne(rg4, fb1, "250");

        AdmUser compteA = achat.compte("DEPA-" + jeton, ROLE);
        visibilite.habiliterSociete(compteA, societe);
        achat.habiliter(compteA, projetA);
        AdmUser compteAB = achat.compte("DEPAB-" + jeton, ROLE);
        visibilite.habiliterSociete(compteAB, societe);
        achat.habiliter(compteAB, projetA);
        achat.habiliter(compteAB, projetB);

        return new Jeu(jeton, compteA, compteAB, societe, fournisseur, nom, typeCode, projetA, projetB, lotA1, lotB,
                fa1, fa2, fh1, fh2, dtp, fb1, d1, d2, rg1, rg2, rg3, rg4, l1, l2, l4a, l4b);
    }

    /** Facture ({@code INVOICE}) du module PRJ ; {@code lot} nul = hors lot. TTC = à payer. */
    public UUID facture(String code, LocalDate date, UUID fournisseur, String nom, UUID societe, UUID projet, UUID lot,
                        String aPayer) {
        UUID id = achat.piece(Piece.facture(code, date, fournisseur, nom, societe, projet, new BigDecimal(aPayer)).lot(lot));
        visibilite.modulePiece(id, visibilite.modulePrj());
        return id;
    }

    /** Décompte du lot (TTC = à payer = {@code montant}), daté du {@code numero} mars 2026. */
    public UUID decompte(UUID lot, String code, int numero, String montant) {
        BigDecimal m = new BigDecimal(montant);
        return projets.decompte(lot, code, numero, Map.of(FixtureProjetLots.TTC, m, FixtureProjetLots.A_PAYER, m));
    }

    /** Lien décompte → facture par {@code PrjWorkingOut.BuyDocumentId} (le seul sens retenu, §3.5). */
    public void lier(UUID decompte, UUID facture) {
        executer(em.createNativeQuery("UPDATE PrjWorkingOut SET BuyDocumentId = :facture WHERE PKey = :id")
                .setParameter("facture", facture).setParameter(P_ID, decompte));
    }

    /** Pièce qui pointe un décompte par {@code BuyDocument.PrjWorkingOutId} (sens des avances, sans réciproque). */
    public void pointerDecompte(UUID piece, UUID decompte) {
        achat.poser(piece, "PrjWorkingOutId", decompte);
    }

    /** Règlement fournisseur ({@code IsCredit = 0}) sans ligne ; échéance = date + 30 j. */
    public UUID reglement(String code, String statut, LocalDate date, UUID type, String montant) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlement (PKey, Code, DocDate, EcheanceDate, Amount, Status, IsCredit,
                            StlSettlementTypeId)
                        VALUES (:id, :code, :date, :echeance, :montant, :statut, 0, :type)""")
                .setParameter(P_ID, id).setParameter("code", code).setParameter("date", date.atStartOfDay())
                .setParameter("echeance", date.plusDays(30).atStartOfDay())
                .setParameter("montant", new BigDecimal(montant)).setParameter("statut", statut)
                .setParameter("type", type));
        return id;
    }

    /** Affectation {@code StlSettlementLine} du règlement à une facture ; rend l'id de la ligne. */
    public UUID ligne(UUID reglement, UUID facture, String montant) {
        UUID id = UUID.randomUUID();
        executer(em.createNativeQuery("""
                        INSERT INTO StlSettlementLine (PKey, StlSettlementId, BuyDocumentId, Amount)
                        VALUES (:id, :reglement, :facture, :montant)""")
                .setParameter(P_ID, id).setParameter("reglement", reglement).setParameter("facture", facture)
                .setParameter("montant", new BigDecimal(montant)));
        return id;
    }

    private void executer(Query query) {
        em.flush();
        query.executeUpdate();
    }
}
