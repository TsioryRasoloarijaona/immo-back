package com.tsix.immocrm.entities.str;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
public class StrIntervener extends LigneOmran {
    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "AdmRoleId")
    private UUID admRoleId;

    @Column(name = "AdmUserId")
    private UUID admUserId;

    @Column(name = "StrConfigEntityId")
    private UUID strConfigEntityId;
}
