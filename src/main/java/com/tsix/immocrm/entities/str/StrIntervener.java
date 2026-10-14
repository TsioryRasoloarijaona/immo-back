package com.tsix.immocrm.entities.str;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/**
 * Intervenant d'un workflow : un rôle ({@code AdmRoleId}) ou un utilisateur ({@code AdmUserId}). La colonne
 * {@code QueryText} (requête SQL libre d'OMRAN) n'est pas mappée : voir FEATURE-vente-workflow.md, règle 2.
 */
@Getter
@Setter
@Entity
@Table(name = "StrIntervener")
public class StrIntervener {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Column(name = "AdmRoleId")
    private UUID admRoleId;

    @Column(name = "AdmUserId")
    private UUID admUserId;

    @Column(name = "StrConfigEntityId")
    private UUID strConfigEntityId;
}
