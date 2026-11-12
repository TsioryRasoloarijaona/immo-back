package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/** Procédure d'achat d'une demande interne d'achat (référentiel OMRAN, lu seulement). */
@Entity
@Immutable
@Table(name = "BuySelectionKind")
public class BuySelectionKind extends CodeLibelleOmran {
}
