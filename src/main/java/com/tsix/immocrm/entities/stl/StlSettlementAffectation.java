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
 * <p>Étroit à dessein — deux tables, une seule colonne agrégée, pas de référentiel joint. C'est la
 * leçon du lot G : la pré-agrégation large coûtait ~1 s, l'étroite 6 ms. Mesuré ici à 9 ms.
 *
 * <h2>Pourquoi partir de {@code StlSettlement} et pas des seules lignes</h2>
 *
 * <p>Une première version n'agrégeait que {@code StlSettlementLine}, donc ne portait une ligne que
 * pour les règlements <em>déjà affectés</em>. C'était un défaut, et il n'était pas théorique : la
 * clé étrangère de l'association ci-contre est le {@code PKey} du règlement, toujours renseigné,
 * donc Hibernate fabrique toujours un proxy — et le déréférencer sur un règlement sans ligne levait
 * {@code ObjectNotFoundException}, soit un <strong>500</strong> sur
 * {@code GET /api/encaissements/{id}}. Mesuré : <strong>130 règlements sur 3 243</strong> (4 %)
 * n'ont aucune ligne, dont les encaissements non encore imputés.
 *
 * <p>Le {@code LEFT JOIN} depuis {@code StlSettlement} rend donc une ligne pour <em>chaque</em>
 * règlement. {@code SUM} d'un ensemble vide vaut {@code NULL}, et c'est voulu : la substitution par
 * zéro se fait en Java, jamais par un littéral en SQL — même règle que dans
 * {@code GroupementService}.
 *
 * <p>{@code @Synchronize} pour qu'Hibernate vide son cache d'écriture sur les deux tables avant de
 * lire ce sous-select : sans lui, une affectation créée dans la même transaction ne serait pas
 * reflétée — ce qui casserait les tests, qui construisent leurs fixtures puis interrogent dans la
 * même transaction. {@code @Immutable}, comme toute projection.
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
     * {@code StlSettlement.PKey}. Le sous-select porte une ligne pour <strong>chaque</strong>
     * règlement, y compris ceux sans aucune affectation — voir l'entête : sans cela le proxy de
     * l'association ne se résolvait pas, et la fiche tombait en 500 sur 4 % des règlements.
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
