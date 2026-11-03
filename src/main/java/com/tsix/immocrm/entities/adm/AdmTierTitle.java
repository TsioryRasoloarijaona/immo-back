package com.tsix.immocrm.entities.adm;

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
@Table(name = "AdmTierTitle")
public class AdmTierTitle extends AuditTrieOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmSexTypeId")
    private AdmSexType admSexType;
}
