package com.tsix.immocrm.entities.str;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Intervenant qui voit les objets d'un statut dans les listes ({@code StrStatus.GetStatusFilterQueryClauseForListing}). */
@Getter
@Setter
@Entity
@Table(name = "StrStatusIntervener")
public class StrStatusIntervener {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "StrIntervenerId")
    private UUID strIntervenerId;

    @Column(name = "StrStatusId")
    private UUID strStatusId;
}
