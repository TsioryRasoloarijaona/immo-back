package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.AuditTrieOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "AdmLiability")
public class AdmLiability extends AuditTrieOmran {
    @Column(name = "IsExonarated")
    private Boolean isExonarated;

    @Column(name = "IsMajorated")
    private Boolean isMajorated;

    @Column(name = "MajorationValue", precision = 26, scale = 6)
    private BigDecimal majorationValue;
}
