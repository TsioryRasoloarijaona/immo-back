package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * dbo_AdmAttachedFile — full mapping, including the {@code AttachedFile} blob. Used both to
 * fetch a single file for download/viewing (use {@link AdmAttachedFileSummary} for lists, see
 * its doc-comment for the confirmed {@code CategoryId} linkage to the parent Dossier) and to
 * insert a new pièce jointe ({@code PieceJointeService#create}).
 */
@Getter
@Setter
@Entity
@Table(name = "AdmAttachedFile")
public class AdmAttachedFile {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Category")
    private String category;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Extention")
    private String extension;

    @Column(name = "CategoryId")
    private UUID categoryId;

    /** FK to {@code AdmAttachedFileType.PKey} — see {@link AdmAttachedFileSummary#getType()}. */
    @Column(name = "AdmAttachedFileTypeId")
    private UUID admAttachedFileTypeId;

    // La colonne AttachedFile n'est plus mappée : les binaires vivent sur S3/MinIO et
    // AppFileObject porte le pointeur. Elle est même physiquement supprimée par
    // 004_drop_attachedfile_filestream.sql — c'était une colonne FILESTREAM, que SQL Server
    // pour Linux refuse au RESTORE. La mapper à nouveau casserait le déploiement sur le VPS.

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;
}
