package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * dbo_AdmAttachedFile — full mapping, including the {@code AttachedFile} blob. Only used to
 * fetch a single file for download/viewing; use {@link AdmAttachedFileSummary} for lists (see
 * its doc-comment for the confirmed {@code CategoryId} linkage to the parent Dossier).
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

    // DB column is "varbinary(max)" — SQL Server has no true BLOB type, so @Lob (which maps
    // byte[] to JDBC BLOB in Hibernate 6) makes the driver return empty content. Same class of
    // bug as ComFolder.ComDocumentDetail (LONGVARCHAR vs LONGNVARCHAR) — explicit SqlTypes
    // matching the real column type is required.
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "AttachedFile")
    private byte[] attachedFile;
}
