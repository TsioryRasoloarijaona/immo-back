package com.tsix.immocrm.entities.buy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Historique de statut d'une facture fournisseur ; {@code buyDocumentWorkFlowId} reste NULL comme en production. */
@Getter
@Setter
@Entity
@Table(name = "BuyDocumentStatusHistory")
public class BuyDocumentStatusHistory {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;
    @Column(name = "DocDate")
    private LocalDateTime docDate;
    @Column(name = "BuyDocumentStatusId")
    private UUID buyDocumentStatusId;
    @Column(name = "BuyDocumentId")
    private UUID buyDocumentId;
    @Column(name = "AdmUserId")
    private UUID admUserId;
    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;
}
