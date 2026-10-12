package com.tsix.immocrm.entities.lnd;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/** Niveau de construction (« Nbr. étage » de la tranche) — sans lien avec l'étage d'article {@code StkFloor}. */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "LndLevel")
public class LndLevel extends CodeLibelleOmran {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;
}
