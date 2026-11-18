package com.tsix.immocrm.entities.buy;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Sous-famille d'article d'achat ; clé {@code Pkey} comme la famille. */
@Getter
@Setter
@Entity
@Table(name = "BuyItemUnderFamily")
@AttributeOverride(name = "id", column = @Column(name = "Pkey", nullable = false))
public class BuyItemUnderFamily extends NomenclatureAchat {
    @Column(name = "Sorting")
    private Integer sorting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BuyItemFamilyId")
    private BuyItemFamily buyItemFamily;
}
