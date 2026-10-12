package com.tsix.immocrm.entities.act;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/** Compte du plan comptable (« Plan compt. » de la tranche). Lu seulement : le CRM ne tient pas la comptabilité. */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "ActAccount")
public class ActAccount extends CodeLibelleOmran {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;
}
