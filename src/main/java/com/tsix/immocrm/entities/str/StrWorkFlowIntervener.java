package com.tsix.immocrm.entities.str;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Intervenant autorisé à exécuter une transition ; {@code WithPassword} = second valideur (jamais posé pour la vente). */
@Getter
@Setter
@Entity
@Table(name = "StrWorkFlowIntervener")
public class StrWorkFlowIntervener {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "StrIntervenerId")
    private UUID strIntervenerId;

    @Column(name = "StrWorkFlowId")
    private UUID strWorkFlowId;

    @Column(name = "WithPassword")
    private Boolean withPassword;
}
