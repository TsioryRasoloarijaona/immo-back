package com.tsix.immocrm.entities.stl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/**
 * "Dépôt" — the broad category a règlement is deposited under (see
 * {@code StlSettlement.stlDeposit}). Only 3 rows in real data: FOURNISSEUR/CLIENT/BANQUE
 * ({@code IsSupplier}/{@code IsCustomer}/{@code IsBank}) — the "Encaissements Vente" screen only
 * ever shows CLIENT rows in practice, but the filter itself is generic.
 */
@Getter
@Setter
@Entity
@Table(name = "StlDeposit")
public class StlDeposit {
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

    @Column(name = "IsBank")
    private Boolean isBank;

    @Column(name = "IsSupplier")
    private Boolean isSupplier;

    @Column(name = "IsCustomer")
    private Boolean isCustomer;
}
