package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.AuditTrieOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Classe OMRAN porteuse de champs spécifiques : {@code Code} = {@code GetType().FullName}. */
@Getter
@Setter
@Entity
@Table(name = "AdmSpecificFieldEntity")
public class AdmSpecificFieldEntity extends AuditTrieOmran {
}
