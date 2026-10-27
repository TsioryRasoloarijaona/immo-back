package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.entities.com.ComCommissionLine;
import com.tsix.immocrm.repositories.com.ComCommissionLineRepository;
import com.tsix.immocrm.services.rapport.LotsTranches;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Seule agrégation des commissions (C7) : généré, réglé, reste. Le réglé d'un document (règlements non annulés) est
 * réparti sur ses lignes au prorata de leur montant (Q5) ; écran, en-tête, lignes et rapport n° 34 passent ici.
 */
@Component
public class SoldesCommission {

    /** Décaissement d'un document à payer : positif ; un encaissement le diminue. */
    private static final String REGLES = "SELECT l.BuyDocumentId AS Document, SUM(-(" + StatutReglement.SQL_MONTANT_SIGNE
            + ")) AS Regle FROM StlSettlementLine l JOIN StlSettlement s ON s.PKey = l.StlSettlementId "
            + "WHERE l.BuyDocumentId IN (:documents) AND " + StatutReglement.SQL_COMPTE_DANS_LES_TOTAUX
            + " GROUP BY l.BuyDocumentId";

    public record Solde(BigDecimal genere, BigDecimal regle) {

        public BigDecimal reste() {
            return genere.subtract(regle);
        }
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final ComCommissionLineRepository lignes;

    public SoldesCommission(NamedParameterJdbcTemplate jdbc, ComCommissionLineRepository lignes) {
        this.jdbc = jdbc;
        this.lignes = lignes;
    }

    /** Réglé de chaque document, règlements annulés exclus ; un document sans règlement vaut 0. */
    public Map<UUID, BigDecimal> reglesParDocument(Collection<UUID> documents) {
        List<UUID> ids = documents.stream().filter(Objects::nonNull).distinct().toList();
        Map<UUID, BigDecimal> regles = new HashMap<>();
        ids.forEach(id -> regles.put(id, BigDecimal.ZERO));
        LotsTranches.lire(ids, lot -> jdbc.query(REGLES, Map.of("documents", LotsTranches.textes(lot)),
                        (rs, n) -> Map.entry(UUID.fromString(rs.getString("Document")), rs.getBigDecimal("Regle"))))
                .forEach(e -> regles.put(e.getKey(), nonNul(e.getValue())));
        return regles;
    }

    /** Réglé de chaque ligne : réglé du document × montant de la ligne / Σ des lignes du document. */
    public Map<UUID, BigDecimal> reglesParLigne(Collection<ComCommissionLine> lignesVues) {
        List<UUID> documents = lignesVues.stream().map(ComCommissionLine::getBuyDocumentId).filter(Objects::nonNull)
                .distinct().toList();
        Map<UUID, BigDecimal> regleDocument = reglesParDocument(documents);
        Map<UUID, BigDecimal> totalDocument = new HashMap<>();
        LotsTranches.lire(documents, lignes::findByBuyDocumentIdIn)
                .forEach(l -> totalDocument.merge(l.getBuyDocumentId(), montant(l), BigDecimal::add));
        Map<UUID, BigDecimal> regles = new HashMap<>();
        for (ComCommissionLine l : lignesVues) {
            BigDecimal total = totalDocument.get(l.getBuyDocumentId());
            BigDecimal regle = l.getBuyDocumentId() == null || total == null || total.signum() == 0 ? BigDecimal.ZERO
                    : regleDocument.getOrDefault(l.getBuyDocumentId(), BigDecimal.ZERO).multiply(montant(l)).divide(total, 16, RoundingMode.HALF_UP);
            regles.put(l.getId(), regle);
        }
        return regles;
    }

    /** Σ des montants et des réglés de {@code lignesVues}, réglés lus par {@link #reglesParLigne}. */
    public static Solde solde(Collection<ComCommissionLine> lignesVues, Map<UUID, BigDecimal> regles) {
        BigDecimal genere = lignesVues.stream().map(SoldesCommission::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal regle = lignesVues.stream().map(l -> regles.getOrDefault(l.getId(), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Solde(genere, regle);
    }

    static BigDecimal montant(ComCommissionLine l) {
        return nonNul(l.getCommissionAmount());
    }

    static BigDecimal nonNul(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
