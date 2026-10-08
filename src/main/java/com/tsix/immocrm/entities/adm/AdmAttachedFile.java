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

/** Pièce jointe en écriture et pour un fichier seul ; les listes passent par {@link AdmAttachedFileSummary}. */
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

    @Column(name = "AdmAttachedFileTypeId")
    private UUID admAttachedFileTypeId;

    // Ne pas mapper AttachedFile : binaires sur S3 (pointeur dans AppFileObject), colonne FILESTREAM
    // supprimée par 004_drop_attachedfile_filestream.sql car SQL Server Linux la refuse au RESTORE.

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;
}
