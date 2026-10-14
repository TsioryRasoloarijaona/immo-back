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
 * Vue légataire en lecture seule, réduite au bloc « activité » de la fiche prospection. Jamais en
 * liste : ses sous-requêtes corrélées s'effondrent en pagination (FEATURE-vues-legacy-prospection.md §5).
 * Exclut les pré-prospections : un id de pré-prospection rend un {@code Optional} vide, c'est attendu.
 */
@Getter
@Entity
@Immutable
@Table(name = "ComSaleProspectionView")
public class ComSaleProspectionView {

    /** = {@code ComProspection.PKey}. */
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    /** Jours écoulés depuis la dernière action. */
    @Column(name = "InactivityTimeout")
    private Integer inactivityTimeout;

    @Column(name = "ActionsMessageNumber")
    private Integer actionsMessageNumber;

    @Column(name = "LastActionDate")
    private LocalDateTime lastActionDate;

    @Column(name = "LastCommunicationDate")
    private LocalDateTime lastCommunicationDate;

    /** Chaîne vide plutôt que {@code NULL} quand il n'y a rien : le service la ramène à {@code null}. */
    @Size(max = 255)
    @Nationalized
    @Column(name = "LastCommunicationResult")
    private String lastCommunicationResult;

    /** Même piège de chaîne vide. */
    @Nationalized
    @Column(name = "LastCommunicationMessage")
    private String lastCommunicationMessage;

    /**
     * Vide en silence si {@code AdmParamsInteger.DefaultAlertDelai} manque (la sous-requête rend
     * {@code NULL}) — d'où son semis dans {@code ci/schema/050_views.sql}.
     */
    @Column(name = "LastActiveAlertDate")
    private LocalDateTime lastActiveAlertDate;

    /** {@code varchar}, non {@code nvarchar} ; chaînes vides possibles. */
    @Size(max = 284)
    @Column(name = "LastActiveAlertReason")
    private String lastActiveAlertReason;

    @Column(name = "NbrVisit")
    private Integer nbrVisit;

    @Column(name = "FirstVisit")
    private LocalDateTime firstVisit;

    @Column(name = "LastVisitDate")
    private LocalDateTime lastVisitDate;

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

    /** CIN, rarement renseignée : chaînes vides comptées non-{@code NULL} (INVENTAIRE-colonnes-prospection.md §4). */
    @Size(max = 255)
    @Nationalized
    @Column(name = "CfgTierIDNumber")
    private String cfgTierIdNumber;

    // Conversion en dossier : rare, c'est le taux de conversion réel, pas un défaut de peuplement.

    @Column(name = "ComFolderId")
    private UUID comFolderId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ComFolderCode")
    private String comFolderCode;

    @Column(name = "CfgCustomerId")
    private UUID cfgCustomerId;

    /** Statut du dossier issu de la prospection ; chaîne vide plutôt que {@code NULL}. */
    @Size(max = 255)
    @Nationalized
    @Column(name = "Status")
    private String status;

    /** {@code StrStatus.BoolField002} du dossier : le filtre « Aboutie » d'OMRAN exige 0. */
    @Column(name = "StatusIsCancellation")
    private Boolean statusIsCancellation;

    /** {@code StrStatus.BoolField003} du dossier : le filtre « Aboutie » d'OMRAN exige 0. */
    @Column(name = "StatusIsClosing")
    private Boolean statusIsClosing;
}
