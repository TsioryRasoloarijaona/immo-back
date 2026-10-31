package com.tsix.immocrm.entities.stl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Comptes de dépôt d'un utilisateur. */
@Getter
@Setter
@Entity
@Table(name = "StlDepositUser")
public class StlDepositUser {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "AdmUserId")
    private UUID admUserId;

    @Column(name = "StlDepositId")
    private UUID stlDepositId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;
}
