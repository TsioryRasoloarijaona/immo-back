package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.SimulationPaiementsLot.Ligne;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjProject;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Recalcul de {@code PrjMarketPaymentPrevision} après une écriture (FEATURE-lot-previsions.md §6, OMRAN
 * {@code Market.cs:UpdatePaymentPrevision}) : lignes du lot effacées puis réécrites, dans la transaction de l'appelant.
 */
@Component
public class PrevisionsPaiementLot {

    private static final Set<String> PIECES_DU_LOT = Set.of("ORDER", "DOCUMENT_TO_PAY");
    private static final String EFFACER = "DELETE FROM PrjMarketPaymentPrevision WHERE PrjMarketId = :lot";
    // Code StrEntity absent de la base → NULL (P12).
    private static final String INSERER = """
            INSERT INTO PrjMarketPaymentPrevision (PKey, PaymentDate, Amount, Description, PrjMarketId, Achievement, AmountHT,
                AmountTTC, StrEntityId)
            VALUES (:id, :date, :montant, :libelle, :lot, :avancement, :ht, :ttc, (SELECT TOP 1 PKey FROM StrEntity WHERE Code = :code))""";

    private final LectureSimulationLot lecture;
    private final EntityManager em;

    PrevisionsPaiementLot(LectureSimulationLot lecture, EntityManager em) {
        this.lecture = lecture;
        this.em = em;
    }

    /** Le lot, puis le lot gestionnaire du compte prorata de son projet quand le lot n'est pas multi-fournisseur. */
    public void recalculer(UUID lotId) {
        PrjMarket lot = lotId == null ? null : em.find(PrjMarket.class, lotId);
        if (lot == null) {
            return;
        }
        reecrire(lot);
        if (!Boolean.TRUE.equals(lot.getIsMultiSupplier())) {
            recalculerProrata(lot.getPrjProject(), lotId);
        }
    }

    /** Bon de commande ou pièce à payer rattaché à un lot : le lot est recalculé. */
    public void apresPiece(BuyDocument piece) {
        if (piece.getPrjMarketId() != null && PIECES_DU_LOT.contains(piece.getDocType())) {
            recalculer(piece.getPrjMarketId());
        }
    }

    /** Lot gestionnaire du compte prorata du projet, sauf {@code sauf} (lot déjà recalculé ou supprimé). */
    public void recalculerProrata(PrjProject projet, UUID sauf) {
        UUID prorata = projet == null ? null : projet.getManageProrataPrjMarketId();
        PrjMarket gestionnaire = prorata == null || prorata.equals(sauf) ? null : em.find(PrjMarket.class, prorata);
        if (gestionnaire != null) {
            reecrire(gestionnaire);
        }
    }

    /** Lignes du lot effacées, avant la suppression du lot. */
    public void effacer(UUID lot) {
        em.createNativeQuery(EFFACER).setParameter("lot", lot).executeUpdate();
    }

    private void reecrire(PrjMarket lot) {
        em.flush();
        effacer(lot.getId());
        for (Ligne l : lecture.paiements(lot)) {
            em.createNativeQuery(INSERER).setParameter("id", UUID.randomUUID()).setParameter("date", l.date().atStartOfDay())
                    .setParameter("montant", echelle(l.montant())).setParameter("libelle", Objects.toString(l.libelle(), ""))
                    .setParameter("lot", lot.getId()).setParameter("avancement", echelle(l.avancement()))
                    .setParameter("ht", echelle(l.ht())).setParameter("ttc", echelle(l.ttc()))
                    .setParameter("code", l.genre().code()).executeUpdate();
        }
    }

    private static BigDecimal echelle(BigDecimal valeur) {
        return valeur.setScale(6, RoundingMode.HALF_UP);
    }
}
