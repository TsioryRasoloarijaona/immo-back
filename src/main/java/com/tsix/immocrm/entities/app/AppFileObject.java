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
 * Pointeur vers le fichier stocké sur S3/MinIO — table applicative, hors schéma legacy
 * (créée par {@code src/main/resources/db/migration/002_AppFileObject.sql}, même convention
 * que {@link AppUserCredential}).
 *
 * <p>{@code AdmAttachedFile} reste la ligne de métadonnées et de rattachement
 * ({@code Category}, {@code CategoryId}, {@code Description}, type, auteur) ; seule la
 * localisation du binaire vit ici. La relation est 1:1 et partage la clé :
 * {@code AdmAttachedFileId = AdmAttachedFile.PKey}.
 *
 * <p>{@code storageKey} est stockée <b>en entier</b> et n'est jamais recalculée à la relecture.
 * C'est délibéré : les clés produites par le script de reprise
 * ({@code migration/migrate_blobs_to_s3.py}) et celles des nouveaux dépôts peuvent différer de
 * forme sans qu'aucun fichier ne devienne introuvable.
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

    /**
     * Suppression logique. L'objet S3 n'est jamais supprimé dans la transaction de la requête :
     * on marque ici, un balayage hors ligne nettoie ensuite le bucket.
     */
    @Column(name = "DeletedAt")
    private LocalDateTime deletedAt;

    /**
     * Nom affiché de l'auteur de la suppression, symétrique de {@link #uploadedBy}. Ajoutée par
     * {@code 006_AppFileObject_DeletedBy.sql} en même temps que les endpoints {@code DELETE} :
     * jusque-là {@code DeletedAt} n'était écrite par personne, donc la question ne se posait pas.
     * {@code null} tant que la pièce n'est pas supprimée, et pour toute suppression faite hors
     * API.
     */
    @Size(max = 255)
    @Nationalized
    @Column(name = "DeletedBy")
    private String deletedBy;
}
