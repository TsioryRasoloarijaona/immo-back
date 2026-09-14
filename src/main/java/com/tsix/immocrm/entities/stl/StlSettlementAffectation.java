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
 * Part d'un règlement déjà affectée à des échéances, pré-agrégée — pour que
 * {@code montantBrutRestant} devienne sommable.
 *
 * <h2>Pourquoi une pré-agrégation et pas une {@code @Formula}</h2>
 *
 * <p>Le calcul est « montant du règlement moins la somme de ses lignes ». La voie évidente est une
 * {@code @Formula} portant une sous-requête corrélée, exactement comme
 * {@code ComFolder.delaiConcretisation}. Elle donne bien la valeur ligne à ligne — mais elle
 * <strong>ne peut pas être agrégée</strong> : SQL Server refuse {@code SUM()} sur une expression
 * contenant une sous-requête (erreur 130). Mesuré en tentant la requête, pas déduit.
 *
 * <p>Pré-agréger rend le terme <em>joignable</em>, donc sommable : la différence devient
 * {@code SUM(StlSettlement.Amount) - SUM(Affecte)} sur un {@code LEFT JOIN}. Mesuré à <b>7 ms</b>
 * sur les 643 règlements du périmètre Vente, trois passes.
 *
 * <h2>Pourquoi ne pas mapper {@code StlSettlementAmountAffectedView}</h2>
 *
 * <p>Le légataire a déjà sa vue pour ça, et {@code StlSettlementView} s'en sert. Mais cette vue
 * <strong>n'existe pas dans {@code yesRemsTest}</strong> — comme {@code StlSettlementView}
 * elle-même, qui y compte zéro colonne. La mapper ferait tomber toute la suite sur une dérive de
 * base, le mode de panne le plus fréquent de ce dépôt. Un {@code @Subselect} est défini en Java :
 * il n'a besoin que de {@code StlSettlementLine}, qui existe partout.
 *
 * <p>Étroit à dessein — une seule table, une seule colonne agrégée, pas de référentiel joint.
 * C'est la leçon du lot G : la pré-agrégation large coûtait ~1 s, l'étroite 6 ms.
 *
 * <p>{@code @Synchronize} pour qu'Hibernate vide son cache d'écriture sur
 * {@code StlSettlementLine} avant de lire ce sous-select : sans lui, une affectation créée dans la
 * même transaction ne serait pas reflétée — ce qui casserait les tests, qui construisent leurs
 * fixtures puis interrogent dans la même transaction. {@code @Immutable}, comme toute projection.
 */
@Entity
@Immutable
@Synchronize("StlSettlementLine")
@Subselect("""
        SELECT l.StlSettlementId AS StlSettlementId,
               SUM(l.Amount)     AS Affecte
        FROM StlSettlementLine l
        WHERE l.StlSettlementId IS NOT NULL
        GROUP BY l.StlSettlementId
        """)
@Getter
@Setter
public class StlSettlementAffectation {

    /**
     * {@code StlSettlement.PKey}. Le sous-select ne porte <strong>que</strong> les règlements ayant
     * au moins une ligne : les autres ressortent avec une association nulle par le {@code LEFT
     * JOIN}, et leur part affectée vaut alors zéro — substitué en Java, jamais par un
     * {@code COALESCE} SQL.
     */
    @Id
    @Column(name = "StlSettlementId")
    private UUID id;

    /**
     * {@code SUM(StlSettlementLine.Amount)}, toutes échéances confondues.
     *
     * <p>Volontairement <strong>sans filtre sur {@code ComDocumentId}</strong> : c'est la part
     * affectée du règlement, pas sa part affectée à des ventes. Restreindre ici ferait apparaître
     * comme « restant » de l'argent déjà imputé à un achat ou une location.
     */
    @Column(name = "Affecte", precision = 26, scale = 6)
    private BigDecimal montantAffecte;
}
