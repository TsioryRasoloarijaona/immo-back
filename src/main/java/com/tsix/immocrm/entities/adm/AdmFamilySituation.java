package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.AuditTrieOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "AdmFamilySituation")
public class AdmFamilySituation extends AuditTrieOmran {
    @Column(name = "IsHouseHolder")
    private Boolean isHouseHolder;
}
