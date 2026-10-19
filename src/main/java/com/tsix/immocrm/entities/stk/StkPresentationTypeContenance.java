package com.tsix.immocrm.entities.stk;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Contenance modèle d'un type de présentation, recopiée sur l'article qui prend ce type ({@code Item.SetContenance}). */
@Getter
@Setter
@Entity
@Table(name = "StkPresentationTypeContenance")
public class StkPresentationTypeContenance extends LigneContenanceOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkPresentationTypeId")
    private StkPresentationType stkPresentationType;
}
