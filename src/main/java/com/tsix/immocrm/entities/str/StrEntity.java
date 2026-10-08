package com.tsix.immocrm.entities.str;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/**
 * Type de document du légataire (prospection de vente, pré-prospection, dossier…), jamais écrit par nous.
 * Les vues du bureau font un {@code INNER JOIN} dessus : une ligne sans {@code StrEntityId} disparaît
 * de tous ses écrans. PKey identiques entre bases réelle et de test : les fixtures portent les UUID réels.
 */
@Getter
@Entity
@Immutable
@Table(name = "StrEntity")
public class StrEntity {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    /** Porte le sens ({@code ComProspection}, {@code ComPreProspection}…) : tester ce champ, pas la description. */
    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;
}
