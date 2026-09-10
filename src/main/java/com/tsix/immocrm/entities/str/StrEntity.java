package com.tsix.immocrm.entities.str;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/**
 * Type de document du légataire — le discriminant qui sépare une prospection de vente
 * ({@code Code = 'ComProspection'}) d'une pré-prospection ({@code 'ComPreProspection'}), d'une
 * prospection de location ({@code 'RntProspection'}), d'un dossier ({@code 'ComFolder'}), etc.
 *
 * <p>Table de référentiel : <strong>162 lignes</strong> sur la base réelle, dont cinq concernent la
 * prospection. Les vues du logiciel de bureau font un {@code INNER JOIN} dessus, ce qui rend une
 * ligne sans {@code StrEntityId} invisible de tous ses écrans.
 *
 * <p><strong>Seules {@code PKey}, {@code Code} et {@code Description} sont mappées.</strong> La
 * table en porte 42 : une vingtaine de {@code BoolField0xxLabel}, deux colonnes {@code text} de
 * configuration d'écran, un bloc comptable. Même discipline que {@code ComSaleProspectionView} —
 * on ne mappe que ce qu'on sert.
 *
 * <p>{@code @Immutable} : référentiel de configuration du légataire, que nous ne créons ni ne
 * modifions jamais. Pas de {@code @Setter}, en conséquence.
 *
 * <p>Les {@code PKey} sont <strong>identiques entre {@code yesRemsProd} et {@code yesRemsCiCheck}</strong>
 * (vérifié le 10/09/2026) : les fixtures de test peuvent porter les UUID réels.
 *
 * @see com.tsix.immocrm.entities.com.ComProspection#getStrEntityId()
 */
@Getter
@Entity
@Immutable
@Table(name = "StrEntity")
public class StrEntity {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    /**
     * Identifiant métier stable — {@code ComProspection}, {@code ComPreProspection},
     * {@code RntProspection}… <strong>C'est lui qui porte le sens</strong>, pas la description :
     * un appelant qui teste le type doit tester ce champ.
     */
    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    /** Libellé d'affichage, traduit — « Prospection vente », « Pré-Prospection ». */
    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;
}
