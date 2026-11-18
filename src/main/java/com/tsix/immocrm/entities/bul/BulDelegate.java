package com.tsix.immocrm.entities.bul;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Intervenant d'une réunion : un fournisseur sur un lot. */
@Getter
@Setter
@Entity
@Table(name = "BulDelegate")
public class BulDelegate extends LigneOmran {
    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    @Column(name = "CfgSupplierId")
    private UUID cfgSupplierId;

    @Column(name = "BulMeetingId")
    private UUID bulMeetingId;

    @Column(name = "Effectif")
    private Integer effectif;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Notes")
    private String notes;
}
