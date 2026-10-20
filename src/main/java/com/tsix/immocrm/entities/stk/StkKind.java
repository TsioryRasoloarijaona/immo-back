package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Nature physique d'un article ("TER" = terrain), distincte de la catégorie/typologie commerciales.
 * Pour "TER", l'acte de réservation omet étage/immeuble et l'achèvement des travaux : voir {@code ActeReservationService}.
 */
@Entity
@Table(name = "StkKind")
public class StkKind extends AuditOmran {
}
