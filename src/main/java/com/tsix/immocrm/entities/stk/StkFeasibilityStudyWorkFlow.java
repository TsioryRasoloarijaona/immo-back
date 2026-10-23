package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Transition d'étude ; note, notification et e-mail sont stockés sans effet (FEATURE-patrimoine-faisabilite-workflow.md §0.4). */
@Getter
@Setter
@Entity
@Table(name = "StkFeasibilityStudyWorkFlow")
public class StkFeasibilityStudyWorkFlow extends LigneOmran {
    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "StartStatusId")
    private UUID startStatusId;

    @Column(name = "EndStatusId")
    private UUID endStatusId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "MandatoryFields")
    private String mandatoryFields;

    @Column(name = "NotesMandatory")
    private Boolean notesMandatory;

    @Column(name = "StrEntityId")
    private UUID strEntityId;

    @Column(name = "WithNotification")
    private Boolean withNotification;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "SendEmailToUsers")
    private Boolean sendEmailToUsers;
}
