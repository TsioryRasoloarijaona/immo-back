package com.tsix.immocrm.entities.com;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Activité de visite d'une prospection, pré-agrégée — pour que {@code derniereVisiteDate} et
 * {@code nbVisites} deviennent groupables.
 *
 * <h2>Pourquoi ne pas passer par {@code ComSaleProspectionView}</h2>
 *
 * <p>La vue légataire calcule déjà ces deux valeurs, et elle est déjà mappée
 * ({@link ComSaleProspectionView}). Mais elle les calcule au milieu de ~15 sous-requêtes corrélées
 * et ~30 jointures de référentiel : elle est faite pour servir <em>une page de vingt lignes</em>
 * par lot d'identifiants, pas pour être agrégée en entier.
 *
 * <p>Mesuré sur {@code yesRemsProd}, groupement par jour de dernière visite :
 *
 * <table>
 *   <tr><td>{@code GROUP BY} sur la vue</td><td><b>5 496 ms</b></td></tr>
 *   <tr><td>{@code GROUP BY} sur ce sous-select</td><td><b>6 ms</b> à chaud</td></tr>
 * </table>
 *
 * <p>La vue reste la source de la <em>colonne affichée</em> ; ce sous-select est la source de la
 * <em>clé de groupe</em>. Les deux appliquent la même formule — celle de la vue, recopiée
 * ci-dessous — donc elles s'accordent, à une exception documentée près (voir plus bas).
 *
 * <h2>Pourquoi étroit, et pas une table d'activité complète</h2>
 *
 * <p>Une première version agrégeait toute la catégorie « Prospection » (46 667 lignes) pour servir
 * d'un coup les six colonnes d'activité. Mesurée à <b>~1 s à chaud</b> : cent fois plus cher que
 * celle-ci, qui ne lit que les 4 292 lignes de type « visite ». Les quatre autres colonnes
 * ({@code derniereActionDate}, {@code nbInteractions}, {@code derniereCommunicationDate},
 * {@code joursInactivite}) restent donc non groupables — voir
 * {@code instructions/FEATURE-groupement-toutes-colonnes.md} §4.
 *
 * <h2>L'écart assumé avec la vue</h2>
 *
 * <p>2 957 prospections ont une visite d'après {@code ComActionMessage}, 2 932 d'après la vue :
 * 25 lignes que les jointures internes de la vue perdent, dont <b>4 sont visibles par
 * {@code GET /api/prospections}</b>. Pour ces quatre, le groupe portera une date alors que la
 * colonne affichée est nulle. C'est la vue qui perd la donnée, pas nous — anomalie légataire
 * préexistante, signalée plutôt que recopiée.
 *
 * <p>{@code @Synchronize} pour que Hibernate vide son cache d'écriture sur {@code ComActionMessage}
 * avant de lire ce sous-select : sans lui, une interaction créée dans la même transaction ne
 * serait pas reflétée. {@code @Immutable}, comme toute projection : jamais écrite.
 */
@Entity
@Immutable
@Synchronize("ComActionMessage")
@Subselect("""
        SELECT m.ComProspectionId  AS ComProspectionId,
               MAX(m.DocDate)      AS LastVisitDate,
               COUNT(*)            AS NbrVisit
        FROM ComActionMessage m
        WHERE m.ComActionMessageTypeId     = 'fea94cec-b244-4091-9cec-a9b2cffb917c'
          AND m.ComActionMessageCategoryId = '69f8e7d6-1111-1111-1111-1aa8b1f02008'
          AND m.ComProspectionId IS NOT NULL
        GROUP BY m.ComProspectionId
        """)
@Getter
@Setter
public class ComProspectionVisites {

    /**
     * {@code ComProspection.PKey}. Le sous-select ne porte <strong>que</strong> les prospections
     * ayant au moins une visite : l'association est jointe en {@code LEFT}, donc les autres
     * ressortent avec une clé nulle — exactement le groupe que {@code inclureSansValeur=false}
     * écarte.
     */
    @Id
    @Column(name = "ComProspectionId")
    private UUID id;

    /** {@code MAX(ComActionMessage.DocDate)} sur les interactions de type « visite ». */
    @Column(name = "LastVisitDate")
    private LocalDateTime lastVisitDate;

    /** Nombre d'interactions de type « visite ». Jamais nul ici : une ligne existe = au moins une. */
    @Column(name = "NbrVisit")
    private Integer nbrVisit;
}
