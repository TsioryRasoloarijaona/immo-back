package com.tsix.immocrm.entities.bul;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Contact d'un intervenant de réunion. */
@Getter
@Setter
@Entity
@Table(name = "BulDelegateContact")
public class BulDelegateContact extends ContactReunionOmran {
    @Column(name = "BulDelegateId")
    private UUID bulDelegateId;
}
