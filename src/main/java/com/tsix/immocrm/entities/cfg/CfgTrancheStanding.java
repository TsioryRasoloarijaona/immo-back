package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/** Standing d'une tranche (« Standing » de la fiche tranche OMRAN), référentiel en lecture seule. */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "CfgTrancheStanding")
public class CfgTrancheStanding extends CodeLibelleOmran {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;
}
