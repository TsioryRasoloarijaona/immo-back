package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.dto.projet.ReglementFactureDto;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Facture d'un décompte, lue par {@code PrjWorkingOut.BuyDocumentId} seulement (jamais {@code BuyDocument.PrjWorkingOutId}),
 * et son solde par {@link SoldeDocumentAchat} : une seule requête pour les listes, la fiche et les totaux de lot.
 */
@Component
class FacturesDecomptes {

    /** Facture du décompte {@code decompteId}, du lot {@code lotId}. */
    record Facture(UUID decompteId, UUID lotId, UUID id, String code, String statutCode, String statutDescription,
                   boolean okPourPaiement, SoldeDocumentAchat solde) {
    }

    private static final String FACTURES = """
            SELECT wo.PKey, wo.PrjMarketId, d.PKey, d.Code, st.Code, st.Description, d.AmountToPay,
                   CASE WHEN st.IsOkForPayement = 1 THEN 1 ELSE 0 END,
                   (""" + SoldeDocumentAchat.SQL_REGLE + """
            )
            FROM PrjWorkingOut wo
            JOIN BuyDocument d ON d.PKey = wo.BuyDocumentId
            LEFT JOIN BuyDocumentStatus st ON st.PKey = d.BuyDocumentStatusId
            WHERE\s""";

    private static final String PAR_LOTS = FACTURES + "wo.PrjMarketId IN (:ids)";
    private static final String PAR_DECOMPTES = FACTURES + "wo.PKey IN (:ids)";

    private static final String REGLEMENTS = """
            SELECT s.PKey, s.Code, s.DocDate, s.EcheanceDate, l.Amount, t.Code, t.Description,
                   CASE WHEN """ + StatutReglement.SQL_COMPTE_DANS_LES_TOTAUX + """
             THEN 0 ELSE 1 END
            FROM StlSettlementLine l
            JOIN StlSettlement s ON s.PKey = l.StlSettlementId
            LEFT JOIN StlSettlementType t ON t.PKey = s.StlSettlementTypeId
            WHERE l.BuyDocumentId = :facture
            ORDER BY s.DocDate ASC, s.Code ASC""";

    private final EntityManager em;

    FacturesDecomptes(EntityManager em) {
        this.em = em;
    }

    /** Factures des décomptes donnés, par décompte. */
    Map<UUID, Facture> parDecompte(Collection<UUID> decompteIds) {
        return lire(PAR_DECOMPTES, decompteIds).stream()
                .collect(Collectors.toMap(Facture::decompteId, Function.identity(), (a, b) -> a));
    }

    /** Facturé / réglé / reste par lot : Σ des soldes de ses factures distinctes. */
    Map<UUID, SoldeDocumentAchat> totauxParLot(Collection<UUID> lotIds) {
        Map<UUID, Map<UUID, SoldeDocumentAchat>> parLot = new HashMap<>();
        lire(PAR_LOTS, lotIds).forEach(f -> parLot.computeIfAbsent(f.lotId(), k -> new HashMap<>()).put(f.id(), f.solde()));
        Map<UUID, SoldeDocumentAchat> totaux = new HashMap<>();
        parLot.forEach((lot, factures) -> totaux.put(lot, SoldeDocumentAchat.total(factures.values())));
        return totaux;
    }

    List<ReglementFactureDto> reglements(UUID factureId) {
        List<?> lignes = em.createNativeQuery(REGLEMENTS).setParameter("facture", factureId).getResultList();
        return lignes.stream().map(Object[].class::cast).map(r -> new ReglementFactureDto(
                uuid(r[0]), (String) r[1], date(r[2]), date(r[3]), montant(r[4]), (String) r[5], (String) r[6],
                ((Number) r[7]).intValue() == 1)).toList();
    }

    private List<Facture> lire(String sql, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        List<?> lignes = em.createNativeQuery(sql).setParameter("ids", ids).getResultList();
        return lignes.stream().map(Object[].class::cast).map(r -> new Facture(uuid(r[0]), uuid(r[1]), uuid(r[2]),
                (String) r[3], (String) r[4], (String) r[5], ((Number) r[7]).intValue() == 1,
                SoldeDocumentAchat.de(montant(r[6]), montant(r[8]))))
                .toList();
    }

    static UUID uuid(Object brut) {
        return brut instanceof UUID u ? u : UUID.fromString(String.valueOf(brut));
    }

    static BigDecimal montant(Object brut) {
        return brut instanceof Number n ? new BigDecimal(n.toString()) : null;
    }

    static LocalDate date(Object brut) {
        if (brut instanceof Timestamp t) {
            return t.toLocalDateTime().toLocalDate();
        }
        return brut instanceof LocalDateTime d ? d.toLocalDate() : null;
    }
}
