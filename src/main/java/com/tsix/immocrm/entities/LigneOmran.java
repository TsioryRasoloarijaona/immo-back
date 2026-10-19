package com.tsix.immocrm.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Ligne OMRAN sans Code ni Description : {@code PKey} et la trace {@code UserCreate*} / {@code UserUpdate*}. */
@Getter
@Setter
@MappedSuperclass
public abstract class LigneOmran {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserUpdate")
    private String userUpdate;

    @Column(name = "UserUpdateDateTime")
    private LocalDateTime userUpdateDateTime;
}
