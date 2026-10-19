package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Groupe d'actionnaires : modèle de répartition en pourcentages ({@link CfgGroupShareholderLine}). */
@Getter
@Setter
@Entity
@Table(name = "CfgGroupShareholder")
public class CfgGroupShareholder extends AuditOmran {
}
