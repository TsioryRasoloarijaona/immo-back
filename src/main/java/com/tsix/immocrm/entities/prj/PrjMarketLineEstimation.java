package com.tsix.immocrm.entities.prj;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/** Ligne du bordereau estimatif : seule sa somme par lot est lue (« estimation confidentielle »). */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjMarketLineEstimation")
public class PrjMarketLineEstimation extends LigneOmran {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PrjMarketId")
    private PrjMarket prjMarket;

    @Column(name = "IsStock")
    private Boolean isStock;
}
