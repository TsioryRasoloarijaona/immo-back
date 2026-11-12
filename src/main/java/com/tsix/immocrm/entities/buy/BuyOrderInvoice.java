package com.tsix.immocrm.entities.buy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Lien bon de commande ↔ facture d'OMRAN ; Arpent ne fait que le supprimer avec la facture. */
@Getter
@Setter
@Entity
@Table(name = "BuyOrderInvoice")
public class BuyOrderInvoice {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;
    @Column(name = "BuyOrderId")
    private UUID buyOrderId;
    @Column(name = "BuyInvoiceId")
    private UUID buyInvoiceId;
}
