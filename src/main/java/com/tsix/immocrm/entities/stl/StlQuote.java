package com.tsix.immocrm.entities.stl;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
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
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * dbo_StlQuote (+ StlQuoteItem/StlQuoteDocument, not yet mapped) — the "Offres" tab (GAP-07). Now
 * carries the full HT/TTC "Montants" breakdown the legacy "Offre" screen shows (prix du m²,
 * prix article, prix sous-articles, total, remise, prix de vente), not just the final
 * {@code priceAfterDiscount}; still doesn't map the ~40 remaining rent/caution/commission
 * columns the real table carries, which don't apply to a sale offer.
 */
@Getter
@Setter
@Entity
@Table(name = "StlQuote")
public class StlQuote {
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

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTrancheId")
    private CfgTranche cfgTranche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgProspectId")
    private CfgTier cfgProspect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComProspectionId")
    private ComProspection comProspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCommercialId")
    private CfgTier cfgCommercial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComPaymentMethodId")
    private ComPaymentMethod comPaymentMethod;

    // -- Montants (onglet "Montants" du formulaire Offre legacy) --------------------------------

    @Column(name = "Area", precision = 26, scale = 6)
    private BigDecimal area;

    /** Taux de TVA (ex. 0.20 = 20%), repris de l'article au moment de la création de l'offre. */
    @Column(name = "VATRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;

    /** "Prix du m²" HT/TTC. */
    @Column(name = "UnitCostHT", precision = 26, scale = 6)
    private BigDecimal unitCostHT;

    @Column(name = "UnitCost", precision = 26, scale = 6)
    private BigDecimal unitCost;

    /** "Prix Article" HT/TTC. */
    @Column(name = "ItemPriceHT", precision = 26, scale = 6)
    private BigDecimal itemPriceHT;

    @Column(name = "ItemPrice", precision = 26, scale = 6)
    private BigDecimal itemPrice;

    /** "Prix sous articles" HT/TTC (somme des sous-articles inclus dans l'offre). */
    @Column(name = "UnderItemsPriceHT", precision = 26, scale = 6)
    private BigDecimal underItemsPriceHT;

    @Column(name = "UnderItemsPrice", precision = 26, scale = 6)
    private BigDecimal underItemsPrice;

    /** "Total" HT/TTC = prix article + prix sous-articles, avant remise. */
    @Column(name = "PriceBeforeDiscountHT", precision = 26, scale = 6)
    private BigDecimal priceBeforeDiscountHT;

    @Column(name = "PriceBeforeDiscount", precision = 26, scale = 6)
    private BigDecimal priceBeforeDiscount;

    /** "Remise" en %. */
    @Column(name = "Discount", precision = 26, scale = 6)
    private BigDecimal discount;

    @Column(name = "DiscountAmountHT", precision = 26, scale = 6)
    private BigDecimal discountAmountHT;

    @Column(name = "DiscountAmount", precision = 26, scale = 6)
    private BigDecimal discountAmount;

    /** "Prix du m²" HT/TTC après remise. */
    @Column(name = "UnitCostAfterDiscountHT", precision = 26, scale = 6)
    private BigDecimal unitCostAfterDiscountHT;

    @Column(name = "UnitCostAfterDiscount", precision = 26, scale = 6)
    private BigDecimal unitCostAfterDiscount;

    /** "Prix Article" HT/TTC après remise. */
    @Column(name = "ItemPriceAfterDiscountHT", precision = 26, scale = 6)
    private BigDecimal itemPriceAfterDiscountHT;

    @Column(name = "ItemPriceAfterDiscount", precision = 26, scale = 6)
    private BigDecimal itemPriceAfterDiscount;

    /** "Prix sous articles" HT/TTC après remise. */
    @Column(name = "UnderItemsPriceAfterDiscountHT", precision = 26, scale = 6)
    private BigDecimal underItemsPriceAfterDiscountHT;

    @Column(name = "UnderItemsPriceAfterDiscount", precision = 26, scale = 6)
    private BigDecimal underItemsPriceAfterDiscount;

    /** "Prix de vente" HT/TTC = total − remise. Ligne finale du formulaire Offre legacy. */
    @Column(name = "PriceAfterDiscountHT", precision = 26, scale = 6)
    private BigDecimal priceAfterDiscountHT;

    @Column(name = "PriceAfterDiscount", precision = 26, scale = 6)
    private BigDecimal priceAfterDiscount;

    /** Taux de commission (ex. 0.03 = 3%) — champ "Commission" du formulaire Offre legacy. */
    @Column(name = "CommissionRate", precision = 26, scale = 6)
    private BigDecimal commissionRate;

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
