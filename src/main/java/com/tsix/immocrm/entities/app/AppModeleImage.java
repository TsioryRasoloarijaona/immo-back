package com.tsix.immocrm.entities.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Image d'un modèle de document : octets dans S3, immuable, jamais supprimée (FEATURE-modeles-documents.md §13). */
@Getter
@Setter
@Entity
@Table(name = "AppModeleImage")
public class AppModeleImage {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @NotNull
    @Nationalized
    @Size(max = 512)
    @Column(name = "StorageKey", nullable = false, length = 512)
    private String storageKey;

    @NotNull
    @Nationalized
    @Size(max = 100)
    @Column(name = "ContentType", nullable = false, length = 100)
    private String contentType;

    @NotNull
    @Column(name = "SizeBytes", nullable = false)
    private Long sizeBytes;

    @NotNull
    @Size(max = 64)
    @Column(name = "Sha256", nullable = false, length = 64, columnDefinition = "char(64)")
    private String sha256;

    @NotNull
    @Column(name = "CreeLe", nullable = false)
    private LocalDateTime creeLe;

    @NotNull
    @Nationalized
    @Size(max = 255)
    @Column(name = "CreePar", nullable = false)
    private String creePar;
}
