package com.tsix.immocrm.entities.buy;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Famille d'article d'achat ; la clé de la table s'appelle {@code Pkey}, pas {@code PKey}. */
@Getter
@Setter
@Entity
@Table(name = "BuyItemFamily")
@AttributeOverride(name = "id", column = @Column(name = "Pkey", nullable = false))
public class BuyItemFamily extends NomenclatureAchat {
    @Column(name = "Sorting")
    private Integer sorting;
}
