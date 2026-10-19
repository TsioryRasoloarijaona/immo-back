package com.tsix.immocrm.entities.lnd;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Niveau (étage) d'une tranche, libellé OMRAN « Nbr. étage ». */
@Entity
@Table(name = "LndLevel")
public class LndLevel extends CodeLibelleOmran {
}
