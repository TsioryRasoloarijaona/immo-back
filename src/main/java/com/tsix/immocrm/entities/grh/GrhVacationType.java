package com.tsix.immocrm.entities.grh;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "GrhVacationType")
public class GrhVacationType extends CodeLibelleOmran {
}
