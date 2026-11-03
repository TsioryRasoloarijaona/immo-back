package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "BuyDocumentStatus")
public class BuyDocumentStatus extends CodeLibelleOmran {
}
