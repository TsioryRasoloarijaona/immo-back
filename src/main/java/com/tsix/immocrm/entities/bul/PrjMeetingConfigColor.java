package com.tsix.immocrm.entities.bul;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

/** Plage de couleur de l'impression du P.V. (avancement de {@code StartFrom} à {@code EndTo}, en fraction). */
@Getter
@Setter
@Entity
@Table(name = "PrjMeetingConfigColor")
public class PrjMeetingConfigColor extends LigneOmran {
    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ColorName")
    private String colorName;

    @Column(name = "StartFrom", precision = 26, scale = 6)
    private BigDecimal startFrom;

    @Column(name = "EndTo", precision = 26, scale = 6)
    private BigDecimal endTo;
}
