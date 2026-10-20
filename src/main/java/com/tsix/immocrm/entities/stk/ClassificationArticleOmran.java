package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.stl.StlCategory;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

/** Colonnes communes à la catégorie et au type d'article ({@code StkItemCategory}, {@code StkItemType}). */
@Getter
@Setter
@MappedSuperclass
public abstract class ClassificationArticleOmran extends AuditOmran {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StkKindId", nullable = false)
    private StkKind stkKind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StlCategoryId")
    private StlCategory stlCategory;

    @Column(name = "ForUnderItem")
    private Boolean forUnderItem;

    @Column(name = "WithAreaSale")
    private Boolean withAreaSale;

    @Size(max = 500)
    @Nationalized
    @Column(name = "ReportDescription", length = 500)
    private String reportDescription;

    @Column(name = "StockVariationCoef", precision = 26, scale = 6)
    private BigDecimal stockVariationCoef;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Notes", length = 500)
    private String notes;
}
