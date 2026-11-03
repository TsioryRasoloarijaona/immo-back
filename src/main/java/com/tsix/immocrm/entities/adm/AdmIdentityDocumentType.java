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
@Table(name = "AdmIdentityDocumentType")
public class AdmIdentityDocumentType extends AuditTrieOmran {
    @Column(name = "IsPerson")
    private Boolean isPerson;
}
