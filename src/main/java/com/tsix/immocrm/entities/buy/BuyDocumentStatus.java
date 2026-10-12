package com.tsix.immocrm.entities.buy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Statut d'une pièce d'achat ; c'est le seul statut d'un décompte (porté par sa facture). */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "BuyDocumentStatus")
public class BuyDocumentStatus {

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

    @Column(name = "Sorting")
    private Integer sorting;

    // Orthographe de la base : « Payement ».
    @Column(name = "IsOkForPayement")
    private Boolean isOkForPayement;
}
