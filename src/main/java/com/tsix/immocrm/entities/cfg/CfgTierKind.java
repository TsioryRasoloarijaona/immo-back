package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.AuditTrieOmran;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "CfgTierKind")
public class CfgTierKind extends AuditTrieOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierTypeId")
    private CfgTierType cfgTierType;
}
