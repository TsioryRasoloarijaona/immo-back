package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.NaturePieceLot;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.services.achat.ecriture.EcrituresPieceAchatService;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Pièces à payer et avoirs du lot (FEATURE-lot-pieces-a-payer.md R2, R10-R13, R17) : les gardes des écritures et les drapeaux
 * {@code modifiable} / {@code supprimable} / {@code droitPieces} des lectures passent par les mêmes fonctions.
 */
@Component
class DroitsPieceLot {

    static final String AVANCE = "Yes.Prj.Front.Market.MarketMonitoringList.toolAdvance";
    static final String SUPPRIMER_AVOIR = "Yes.Prj.Front.Market.RefundList.toolDelete";

    // Pièces payables du lot, des seules sociétés habilitées (FEATURE-lot-sous-fenetres.md §3 règle 2).
    static final String PIECES_DU_LOT = """
             FROM BuyDocument d
            LEFT JOIN BuyDocumentStatus st ON st.PKey = d.BuyDocumentStatusId
            LEFT JOIN StlCurrency c ON c.PKey = d.StlCurrencyId
            WHERE d.PrjMarketId = :lot AND d.CanBePayed = 1 AND d.CfgCompanyId IN (:societes) AND\s""";
    /** Statut en lecture seule, puis règlement lié : deux colonnes lues par {@link #etat(Object[], int)}. */
    static final String ETAT_PIECE = """
            ISNULL(st.IsReadOnly, 0),
            CASE WHEN EXISTS (SELECT 1 FROM StlSettlementLine sl WHERE sl.BuyDocumentId = d.PKey) THEN 1 ELSE 0 END""";
    static final String DES_NATURES = "d.DocType = 'DOCUMENT_TO_PAY' AND d.ReferenceType IN (:references)";
    static final String AVOIR = "d.DocType = 'REFUND'";

    private static final String PIECE = "SELECT d.ReferenceType, st.Description, " + ETAT_PIECE + PIECES_DU_LOT
            + "d.PKey = :piece AND " + DES_NATURES;
    private static final String AVOIR_DU_LOT = "SELECT d.ReferenceType, st.Description, " + ETAT_PIECE + PIECES_DU_LOT
            + "d.PKey = :piece AND " + AVOIR;
    private static final String DRAPEAUX = """
            SELECT ISNULL(s.BoolField010, 0), ISNULL(s.BoolField008, 0) FROM PrjMarket m
            LEFT JOIN StrStatus s ON s.PKey = m.StrStatusId WHERE m.PKey = :lot""";
    private static final String MODIFICATION = "Vous ne pouvez pas modifier cette pièce dans l'état actuel du lot !";
    private static final String STATUT_PIECE = "Vous n'avez pas le droit de supprimer un Documents à régler avec un statut ";
    private static final String STATUT_AVOIR = "Vous n'avez pas le droit de supprimer un avoir avec un statut ";

    private final DroitsOmran omran;
    private final PerimetreProjetService perimetre;
    private final EntityManager em;

    DroitsPieceLot(DroitsOmran omran, PerimetreProjetService perimetre, EntityManager em) {
        this.omran = omran;
        this.perimetre = perimetre;
        this.em = em;
    }

    /** Drapeaux du statut du lot : {@code BoolField010} avance marché, {@code BoolField008} avance décompte. */
    record EtatLot(boolean avanceMarche, boolean avanceDecompte) {
        boolean permet(NaturePieceLot nature) {
            return nature.etatPermis(avanceMarche, avanceDecompte);
        }
    }

    record EtatPiece(String reference, String statut, boolean lectureSeule, boolean reglee) {
    }

    record Droits(boolean avance, boolean supprimerAvoir) {
    }

    Droits droits() {
        Set<String> d = omran.parmi(AVANCE, SUPPRIMER_AVOIR);
        return new Droits(d.contains(AVANCE), d.contains(SUPPRIMER_AVOIR));
    }

    /** R2 : AVA, W.AVA et RRG exigent {@code toolAdvance} (modèle et création). */
    void exigerCreation(NaturePieceLot nature) {
        if (nature.exigeDroit()) {
            omran.exiger(AVANCE);
        }
    }

    EtatLot etatLot(UUID lot) {
        Object[] r = (Object[]) em.createNativeQuery(DRAPEAUX).setParameter("lot", lot).getSingleResult();
        return new EtatLot(vrai(r[0]), vrai(r[1]));
    }

    /** Pièce des 4 natures (ou avoir) payable du lot, d'une société habilitée ; vide sinon (404). */
    Optional<EtatPiece> piece(UUID lot, UUID id, boolean avoir) {
        Query q = duLot(avoir ? AVOIR_DU_LOT : PIECE, lot).setParameter("piece", id);
        if (!avoir) {
            q.setParameter("references", references());
        }
        List<?> lignes = q.getResultList();
        return lignes.stream().map(Object[].class::cast)
                .map(r -> new EtatPiece((String) r[0], (String) r[1], vrai(r[2]), vrai(r[3]))).findFirst();
    }

    /** Requête native liée au lot et aux sociétés habilitées ({@link #PIECES_DU_LOT}). */
    Query duLot(String sql, UUID lot) {
        Collection<UUID> societes = perimetre.achat(List.of()).societes();
        // SQL Server refuse IN () : un ensemble vide devient un identifiant qui n'existe pas.
        return em.createNativeQuery(sql).setParameter("lot", lot)
                .setParameter("societes", societes.isEmpty() ? List.of(new UUID(0, 0)) : societes);
    }

    static List<String> references() {
        return Arrays.stream(NaturePieceLot.values()).map(Enum::name).toList();
    }

    /** {@link #ETAT_PIECE} lu à partir de la colonne {@code debut}. */
    static EtatPiece etat(Object[] r, int debut) {
        return new EtatPiece(null, null, vrai(r[debut]), vrai(r[debut + 1]));
    }

    // ------------------------------------------------------------------ règles (R2, R10-R13)

    /** R10 ; null si la modification passe. */
    static RuntimeException refusModification(NaturePieceLot nature, EtatLot lot, EtatPiece piece) {
        return lot.permet(nature) && !piece.lectureSeule() ? null : new ConflictException(MODIFICATION);
    }

    /** R2, R11 ; null si la suppression passe. */
    static RuntimeException refusSuppression(NaturePieceLot nature, Droits droits, EtatPiece piece) {
        return refus(!nature.exigeDroit() || droits.avance(), STATUT_PIECE, piece);
    }

    /** R12, R13 ; null si la suppression passe. */
    static RuntimeException refusSuppressionAvoir(Droits droits, EtatPiece avoir) {
        return refus(droits.supprimerAvoir(), STATUT_AVOIR, avoir);
    }

    private static RuntimeException refus(boolean droit, String statutVerrouille, EtatPiece p) {
        if (!droit) {
            return new ForbiddenException("Vous n'avez pas le droit d'effectuer cette action.");
        }
        if (p.lectureSeule()) {
            return new ConflictException(statutVerrouille + p.statut());
        }
        return p.reglee() ? new ConflictException(EcrituresPieceAchatService.REGLEMENTS_LIES) : null;
    }

    private static boolean vrai(Object brut) {
        return brut instanceof Boolean b ? b : brut instanceof Number n && n.intValue() != 0;
    }
}
