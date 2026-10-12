package com.tsix.immocrm.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/** Colonnes d'audit qu'OMRAN pose sur presque toutes ses tables (auteur et date de création, de modification). */
@Getter
@Setter
@MappedSuperclass
public abstract class AuditOmran {

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserUpdate")
    private String userUpdate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;

    @Column(name = "UserUpdateDateTime")
    private LocalDateTime userUpdateDateTime;
}
