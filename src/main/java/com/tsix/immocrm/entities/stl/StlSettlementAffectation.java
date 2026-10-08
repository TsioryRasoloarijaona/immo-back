package com.tsix.immocrm.entities.stl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Part affectée de chaque règlement, pré-agrégée pour rendre {@code montantBrutRestant} sommable, en
 * {@code @Subselect} étroit et non la vue légataire absente de yesRemsTest (FEATURE-colonnes-groupement-encaissements.md §7.2).
 * {@code @Synchronize} : sans lui, une affectation de la même transaction n'est pas vue.
 */
@Entity
@Immutable
@Synchronize({"StlSettlement", "StlSettlementLine"})
@Subselect("""
        SELECT s.PKey        AS StlSettlementId,
               SUM(l.Amount) AS Affecte
        FROM StlSettlement s
        LEFT JOIN StlSettlementLine l ON l.StlSettlementId = s.PKey
        GROUP BY s.PKey
        """)
@Getter
@Setter
public class StlSettlementAffectation {

    /**
     * Une ligne par règlement, même sans affectation (LEFT JOIN depuis {@code StlSettlement}) : sinon
     * le proxy de l'association lève {@code ObjectNotFoundException}, soit un 500 sur la fiche.
     */
    @Id
    @Column(name = "StlSettlementId")
    private UUID id;

    /**
     * {@code NULL} sans affectation : le zéro se substitue en Java, jamais par un littéral SQL. Sans
     * filtre sur {@code ComDocumentId} : sinon l'argent imputé à un achat ou une location paraîtrait restant.
     */
    @Column(name = "Affecte", precision = 26, scale = 6)
    private BigDecimal montantAffecte;
}
