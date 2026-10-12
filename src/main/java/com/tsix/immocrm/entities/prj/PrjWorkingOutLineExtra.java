package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Travaux supplémentaires d'un décompte ; leur somme TTC est {@code PrjWorkingOut.extraAmount}. */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjWorkingOutLineExtra")
public class PrjWorkingOutLineExtra extends LigneOmran {

    // « Pkey » (k minuscule) sur cette table.
    @Id
    @Column(name = "Pkey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PrjWorkingOutId")
    private PrjWorkingOut prjWorkingOut;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Unity")
    private String unity;
}
