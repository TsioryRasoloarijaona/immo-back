package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Document à joindre d'un statut ou d'une transition d'étude : {@code IsMandatory} n'est pas lu par OMRAN. */
@Getter
@Setter
@MappedSuperclass
public abstract class DocumentAJoindreOmran extends LigneOmran {
    @Column(name = "IsMandatory")
    private Boolean isMandatory;

    @Column(name = "WithTitle")
    private Boolean withTitle;

    @Column(name = "AdmAttachedFileTypeId")
    private UUID admAttachedFileTypeId;

    /** Statut ou transition porteur. */
    public abstract void setPorteurId(UUID porteurId);
}
