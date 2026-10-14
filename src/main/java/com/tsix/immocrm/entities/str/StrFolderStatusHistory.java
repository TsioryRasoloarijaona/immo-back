package com.tsix.immocrm.entities.str;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Journal des changements de statut d'un dossier (source de {@link com.tsix.immocrm.domain.TransitionsValidees}).
 * {@code Description} reçoit la {@code note} de {@code PATCH /api/dossiers/{id}/statut}.
 */
@Getter
@Setter
@Entity
@Table(name = "StrFolderStatusHistory")
public class StrFolderStatusHistory {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComFolderId")
    private ComFolder comFolder;

    @Column(name = "AdmUserId")
    private UUID admUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierId")
    private CfgTier cfgTier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrFolderStatusId")
    private StrStatus strFolderStatus;

    /** Lot principal, ou le sous-article d'une ligne « AFFECTATION » / « ANNULATION AFFECTATION ». */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Description")
    private String description;
}
