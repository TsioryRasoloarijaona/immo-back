package com.tsix.immocrm.entities.app;

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
 * Table applicative (002_AppFileObject.sql) : pointeur S3 d'une pièce, 1:1 avec {@code AdmAttachedFile}
 * par la clé. {@code storageKey} est stockée en entier, jamais recalculée : les clés de la reprise et
 * des nouveaux dépôts peuvent différer de forme.
 */
@Getter
@Setter
@Entity
@Table(name = "AppFileObject")
public class AppFileObject {

    @Id
    @Column(name = "AdmAttachedFileId", nullable = false)
    private UUID admAttachedFileId;

    @Size(max = 512)
    @Nationalized
    @Column(name = "StorageKey", nullable = false)
    private String storageKey;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ContentType")
    private String contentType;

    @Column(name = "SizeBytes", nullable = false)
    private Long sizeBytes;

    /** SHA-256 hexadécimal minuscule, vérifié par le serveur de stockage à l'écriture. */
    @Size(max = 64)
    @Column(name = "Sha256")
    private String sha256;

    @Column(name = "UploadedAt", nullable = false)
    private LocalDateTime uploadedAt;

    /** Nom affiché du déposant, ou {@code "migration"} pour une ligne issue de la reprise. */
    @Size(max = 255)
    @Nationalized
    @Column(name = "UploadedBy")
    private String uploadedBy;

    /** Suppression logique : l'objet S3 n'est jamais supprimé dans la requête, un balayage hors ligne s'en charge. */
    @Column(name = "DeletedAt")
    private LocalDateTime deletedAt;

    /** Nom affiché ; {@code null} aussi pour une suppression faite hors API. */
    @Size(max = 255)
    @Nationalized
    @Column(name = "DeletedBy")
    private String deletedBy;
}
