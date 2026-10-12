package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Type de lot (« 20 TRAVAUX »…) ; la table n'a pas de colonne d'ordre. */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjMarketType")
public class PrjMarketType {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;
}
