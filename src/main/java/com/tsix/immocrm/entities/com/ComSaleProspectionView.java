package com.tsix.immocrm.entities.com;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Projection <strong>en lecture seule</strong> de la vue SQL Server {@code ComSaleProspectionView}
 * (148 colonnes), dont on ne mappe que le bloc « activité » que le logiciel de bureau affiche sur
 * la fiche prospection et que nos entités ne savent pas produire : nombre d'interactions,
 * inactivité, dernière communication, visites, alerte en cours, et le lien de conversion vers le
 * dossier.
 *
 * <p>Cette classe ne mappe <em>pas</em> les champs déjà servis depuis {@link ComProspection} —
 * {@code ProspectionService} enrichit son DTO, il ne le reconstruit pas depuis la vue.
 *
 * <p><strong>Jamais écrite.</strong> {@code @Immutable} : Hibernate ignore toute modification et
 * n'émet aucun {@code INSERT}/{@code UPDATE}. La classe n'a volontairement pas de {@code @Setter}.
 *
 * <p><strong>Ne pas utiliser en liste.</strong> Charger l'entité complète coûte 26 ms sur une
 * ligne mais 5 942 ms sur un {@code OFFSET 5000}, parce que SQL Server n'élague les sous-requêtes
 * corrélées de la vue que si on ne sélectionne pas les colonnes calculées. Les listes restent sur
 * les {@code Specification} de {@link ComProspection} — voir
 * {@code instructions/FEATURE-vues-legacy-prospection.md} §5.
 *
 * <p><strong>Les pré-prospections en sont absentes</strong> : la vue filtre
 * {@code StrEntityId <> 'B92DF3D8-158C-4E1E-827A-70FEB172A264'} (entité {@code ComPreProspection}),
 * ce qui exclut 3 817 lignes ayant leur propre vue {@code ComSalePreProspectionView}. Une
 * recherche par id sur une pré-prospection rend donc un {@code Optional} vide — c'est le
 * comportement attendu, pas une anomalie.
 */
@Getter
@Entity
@Immutable
@Table(name = "ComSaleProspectionView")
public class ComSaleProspectionView {

    /** {@code ComProspection.PKey} — même identifiant que l'entité {@link ComProspection}. */
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    // ---------------------------------------------------------------------
    // Bloc activité — calculé par la vue, peuplé sur la quasi-totalité des lignes
    // ---------------------------------------------------------------------

    /** Jours écoulés depuis la dernière action. Peuplé sur 100 % des 14 118 lignes réelles. */
    @Column(name = "InactivityTimeout")
    private Integer inactivityTimeout;

    /** Nombre d'interactions journalisées. 97 %. */
    @Column(name = "ActionsMessageNumber")
    private Integer actionsMessageNumber;

    /** Dernière action, tous types confondus. 97 %. */
    @Column(name = "LastActionDate")
    private LocalDateTime lastActionDate;

    /** Dernière communication avec le prospect. 90 %. */
    @Column(name = "LastCommunicationDate")
    private LocalDateTime lastCommunicationDate;

    /**
     * Libellé du résultat de la dernière communication (ex. « Prospect n'est plus intéressé »).
     *
     * <p>La vue rend une <strong>chaîne vide</strong> plutôt que {@code NULL} quand il n'y a rien :
     * non-{@code NULL} sur 100 % des lignes, mais non-vide sur 90 % seulement. Le service
     * normalise le vide en {@code null}.
     */
    @Size(max = 255)
    @Nationalized
    @Column(name = "LastCommunicationResult")
    private String lastCommunicationResult;

    /** Texte de la dernière communication. Même piège de chaîne vide que ci-dessus — 90 %. */
    @Nationalized
    @Column(name = "LastCommunicationMessage")
    private String lastCommunicationMessage;

    // ---------------------------------------------------------------------
    // Relance en cours
    // ---------------------------------------------------------------------

    /**
     * Date de l'alerte active. 25 %.
     *
     * <p>Dépend du paramètre {@code AdmParamsInteger.DefaultAlertDelai} : sans cette ligne, la
     * sous-requête de la vue rend {@code NULL} et cette colonne devient silencieusement vide —
     * d'où son semis dans {@code ci/schema/050_views.sql}.
     */
    @Column(name = "LastActiveAlertDate")
    private LocalDateTime lastActiveAlertDate;

    /** Motif de l'alerte active. 25 % hors chaînes vides. {@code varchar}, non {@code nvarchar}. */
    @Size(max = 284)
    @Column(name = "LastActiveAlertReason")
    private String lastActiveAlertReason;

    // ---------------------------------------------------------------------
    // Visites — 20 %, ce qui correspond aux prospections ayant dépassé l'étape visite
    // ---------------------------------------------------------------------

    @Column(name = "NbrVisit")
    private Integer nbrVisit;

    @Column(name = "FirstVisit")
    private LocalDateTime firstVisit;

    @Column(name = "LastVisitDate")
    private LocalDateTime lastVisitDate;

    // ---------------------------------------------------------------------
    // Contexte projet — 99 %. Nos DTO n'exposaient que la tranche.
    // ---------------------------------------------------------------------

    @Column(name = "CfgProjectId")
    private UUID cfgProjectId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CfgProjectCode")
    private String cfgProjectCode;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CfgProjectDescription")
    private String cfgProjectDescription;

    /**
     * CIN du prospect, portée par la vue. <strong>Rare : 2,7 %</strong> (387 lignes sur 14 118).
     *
     * <p>Une première mesure annonçait 99 % en comptant les non-{@code NULL} d'une colonne qui
     * porte des chaînes vides. La correction avait été passée dans les {@code @Schema} et les
     * specs sans l'être ici — voir {@code INVENTAIRE-colonnes-prospection.md} §4.
     */
    @Size(max = 255)
    @Nationalized
    @Column(name = "CfgTierIDNumber")
    private String cfgTierIdNumber;

    // ---------------------------------------------------------------------
    // Conversion en dossier — 2 %, qui est le taux de conversion réel
    // (358 dossiers pour 14 118 prospections), pas un défaut de peuplement.
    // ---------------------------------------------------------------------

    @Column(name = "ComFolderId")
    private UUID comFolderId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ComFolderCode")
    private String comFolderCode;

    @Column(name = "CfgCustomerId")
    private UUID cfgCustomerId;

    /** Statut du dossier issu de la prospection. Chaîne vide plutôt que {@code NULL}. */
    @Size(max = 255)
    @Nationalized
    @Column(name = "Status")
    private String status;
}
