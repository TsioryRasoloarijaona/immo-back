package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/** Statut d'une pièce d'achat ; c'est le seul statut d'un décompte (porté par sa facture). */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "BuyDocumentStatus")
public class BuyDocumentStatus extends CodeLibelleOmran {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "Sorting")
    private Integer sorting;

    // Orthographe de la base : « Payement ».
    @Column(name = "IsOkForPayement")
    private Boolean isOkForPayement;
}
