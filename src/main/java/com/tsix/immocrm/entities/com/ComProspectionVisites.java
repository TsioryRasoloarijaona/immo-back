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
 * Visites d'une prospection pré-agrégées, clé de groupe de {@code derniereVisiteDate}/{@code nbVisites} ;
 * la colonne affichée vient de la vue, trop lente à grouper. Même formule que la vue, mais ses jointures
 * internes perdent quelques lignes — voir FEATURE-groupement-toutes-colonnes.md §5. {@code @Synchronize} : écritures en cours.
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
     * Seules les prospections avec au moins une visite : jointes en {@code LEFT}, les autres ont une
     * clé nulle, le groupe que {@code inclureSansValeur=false} écarte.
     */
    @Id
    @Column(name = "ComProspectionId")
    private UUID id;

    @Column(name = "LastVisitDate")
    private LocalDateTime lastVisitDate;

    /** Jamais nul ici : une ligne existe = au moins une visite. */
    @Column(name = "NbrVisit")
    private Integer nbrVisit;
}
