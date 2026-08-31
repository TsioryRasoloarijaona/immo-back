package com.tsix.immocrm.entities.str;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
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
 * dbo_StrFolderStatusHistory — the Dossier status-change audit log (confirmed: this is the
 * source {@link com.tsix.immocrm.domain.TransitionsValidees} was mined from). {@code Description}
 * is free text attached to a specific transition (confirmed real usage, e.g. "ANNULATION
 * AFFECTATION") — {@code PATCH /api/dossiers/{id}/statut}'s {@code note} is written here.
 *
 * <p>{@code AdmUserId} kept as a plain UUID (no {@code AdmUser} entity in this codebase, and no
 * auth/session context exists yet to populate it from our API) — same "unmapped FK" pattern used
 * for {@code AdmAttachedFile.CategoryId} elsewhere.
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

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Description")
    private String description;
}
