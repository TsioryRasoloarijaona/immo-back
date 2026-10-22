package com.tsix.immocrm.entities.lnd;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

/** Colonnes de surfaces communes à {@code LndLand} et {@code LndLandZone} (mêmes noms, mêmes types). */
@Getter
@Setter
@MappedSuperclass
public abstract class SurfacesFoncieresOmran extends LigneOmran {
    @Column(name = "Area", precision = 26, scale = 6)
    private BigDecimal area;

    @Column(name = "Cos", precision = 26, scale = 6)
    private BigDecimal cos;

    @Column(name = "Cuf", precision = 26, scale = 6)
    private BigDecimal cuf;

    @Column(name = "AreaCos", precision = 26, scale = 6)
    private BigDecimal areaCos;

    @Column(name = "AreaCuf", precision = 26, scale = 6)
    private BigDecimal areaCuf;

    @Column(name = "SalableArea", precision = 26, scale = 6)
    private BigDecimal salableArea;

    @Column(name = "CoefSaleableArea", precision = 26, scale = 6)
    private BigDecimal coefSaleableArea;

    @Column(name = "CommerceCoef", precision = 26, scale = 6)
    private BigDecimal commerceCoef;

    @Column(name = "CommerceArea", precision = 26, scale = 6)
    private BigDecimal commerceArea;

    @Column(name = "OfficeCoef", precision = 26, scale = 6)
    private BigDecimal officeCoef;

    @Column(name = "OfficeArea", precision = 26, scale = 6)
    private BigDecimal officeArea;

    @Column(name = "HabitatArea", precision = 26, scale = 6)
    private BigDecimal habitatArea;

    @Column(name = "UndergroundParkingArea", precision = 26, scale = 6)
    private BigDecimal undergroundParkingArea;

    @Column(name = "UndergroundParkingCoef", precision = 26, scale = 6)
    private BigDecimal undergroundParkingCoef;

    @Column(name = "AlinmentStop")
    private Integer alinmentStop;

    @Column(name = "MaxHeight")
    private Integer maxHeight;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;
}
