package com.tsix.immocrm.entities.grh;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

/** Type de travaux (journée travaillée) : ses unités, prix et TVA sont recopiés dans le jour à son choix. */
@Getter
@Entity
@Immutable
@Table(name = "GrhWorkedDayType")
public class GrhWorkedDayType extends CodeLibelleOmran {
    @Size(max = 255)
    @Nationalized
    @Column(name = "Unity")
    private String unity;

    @Column(name = "UnitPriceHT", precision = 26, scale = 6)
    private BigDecimal unitPriceHT;

    @Column(name = "VatRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Unity1")
    private String unity1;

    @Column(name = "UnitPriceHT1", precision = 26, scale = 6)
    private BigDecimal unitPriceHT1;

    @Column(name = "VatRatio1", precision = 26, scale = 6)
    private BigDecimal vatRatio1;

    @Size(max = 255)
    @Nationalized
    @Column(name = "GroupDescription")
    private String groupDescription;
}
