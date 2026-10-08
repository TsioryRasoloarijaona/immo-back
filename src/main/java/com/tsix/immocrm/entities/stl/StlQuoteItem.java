package com.tsix.immocrm.entities.stl;

import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Ligne du tableau « SOUS ARTICLES » d'une offre {@link StlQuote}. */
@Getter
@Setter
@Entity
@Table(name = "StlQuoteItem")
public class StlQuoteItem {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StlQuoteId", nullable = false)
    private StlQuote stlQuote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemTypeId")
    private StkItemType stkItemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemCategoryId")
    private StkItemCategory stkItemCategory;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "Area", precision = 26, scale = 6)
    private BigDecimal area;

    @Column(name = "UnitCostHT", precision = 26, scale = 6)
    private BigDecimal unitCostHT;

    @Column(name = "VATRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;

    @Column(name = "UnitCost", precision = 26, scale = 6)
    private BigDecimal unitCost;

    /** "Prix TTC avant remise" HT/TTC. */
    @Column(name = "PriceBeforeDiscountHT", precision = 26, scale = 6)
    private BigDecimal priceBeforeDiscountHT;

    @Column(name = "PriceBeforeDiscount", precision = 26, scale = 6)
    private BigDecimal priceBeforeDiscount;

    /** "Remise" en %. */
    @Column(name = "Discount", precision = 26, scale = 6)
    private BigDecimal discount;

    /** "Montant remise" HT/TTC. */
    @Column(name = "DiscountAmountHT", precision = 26, scale = 6)
    private BigDecimal discountAmountHT;

    @Column(name = "DiscountAmount", precision = 26, scale = 6)
    private BigDecimal discountAmount;

    @Column(name = "UnitCostAfterDiscountHT", precision = 26, scale = 6)
    private BigDecimal unitCostAfterDiscountHT;

    @Column(name = "UnitCostAfterDiscount", precision = 26, scale = 6)
    private BigDecimal unitCostAfterDiscount;

    /** "Prix TTC après remise" HT/TTC. */
    @Column(name = "PriceAfterDiscountHT", precision = 26, scale = 6)
    private BigDecimal priceAfterDiscountHT;

    @Column(name = "PriceAfterDiscount", precision = 26, scale = 6)
    private BigDecimal priceAfterDiscount;

    /** "Tjs affecté" — sous-article intrinsèquement toujours rattaché à l'article principal. */
    @Column(name = "AlwaysAffected")
    private Boolean alwaysAffected;

    @Column(name = "IsUnderItem")
    private Boolean isUnderItem;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserUpdate")
    private String userUpdate;

    @Column(name = "UserUpdateDateTime")
    private LocalDateTime userUpdateDateTime;
}
