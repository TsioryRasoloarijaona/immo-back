package com.tsix.immocrm.entities.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Version d'un {@link AppModeleDocument} : jamais modifiée après insertion. */
@Getter
@Setter
@Entity
@Table(name = "AppModeleDocumentVersion")
public class AppModeleDocumentVersion {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "AppModeleDocumentId", nullable = false)
    private UUID modeleId;

    @NotNull
    @Column(name = "Numero", nullable = false)
    private Integer numero;

    /** HTML restreint (FEATURE-modeles-documents.md §5). */
    @NotNull
    @Nationalized
    @Lob
    @Column(name = "Contenu", nullable = false)
    private String contenu;

    /** JSON {@link com.tsix.immocrm.dto.modele.MiseEnPageDto}. */
    @NotNull
    @Nationalized
    @Size(max = 400)
    @Column(name = "MiseEnPage", nullable = false, length = 400)
    private String miseEnPage;

    @NotNull
    @Column(name = "CreeLe", nullable = false)
    private LocalDateTime creeLe;

    @NotNull
    @Nationalized
    @Size(max = 255)
    @Column(name = "CreePar", nullable = false)
    private String creePar;
}
