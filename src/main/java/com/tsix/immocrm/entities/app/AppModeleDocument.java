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

/** Modèle de document modifiable (table applicative, FEATURE-modeles-documents.md §3). */
@Getter
@Setter
@Entity
@Table(name = "AppModeleDocument")
public class AppModeleDocument {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    /** Code {@link com.tsix.immocrm.domain.CategorieModele}. */
    @NotNull
    @Nationalized
    @Size(max = 40)
    @Column(name = "Categorie", nullable = false, length = 40)
    private String categorie;

    @NotNull
    @Nationalized
    @Size(max = 60)
    @Column(name = "Code", nullable = false, length = 60)
    private String code;

    @NotNull
    @Nationalized
    @Size(max = 200)
    @Column(name = "Libelle", nullable = false, length = 200)
    private String libelle;

    @NotNull
    @Column(name = "Ordre", nullable = false)
    private Integer ordre;

    @NotNull
    @Column(name = "Actif", nullable = false)
    private Boolean actif;

    /** Sans clé étrangère en base : voir 017_AppModeleDocument.sql. */
    @Column(name = "VersionCouranteId")
    private UUID versionCouranteId;

    @NotNull
    @Column(name = "CreeLe", nullable = false)
    private LocalDateTime creeLe;

    @NotNull
    @Nationalized
    @Size(max = 255)
    @Column(name = "CreePar", nullable = false)
    private String creePar;

    @NotNull
    @Column(name = "ModifieLe", nullable = false)
    private LocalDateTime modifieLe;

    @NotNull
    @Nationalized
    @Size(max = 255)
    @Column(name = "ModifiePar", nullable = false)
    private String modifiePar;
}
