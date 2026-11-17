package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "PrjMarketReceptionReserve")
public class PrjMarketReceptionReserve extends LigneOmran {
    @Column(name = "PrjMarketReceptionId")
    private UUID prjMarketReceptionId;

    @Column(name = "sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "IsFixed")
    private Boolean isFixed;
}
