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

/** Transition paramétrée d'un statut à un autre ({@code Yes.Str/StrWorkFlow.cs}), par entité de configuration. */
@Getter
@Setter
@Entity
@Table(name = "StrWorkFlow")
public class StrWorkFlow {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "StatusFromId")
    private UUID statusFromId;

    @Column(name = "StatusToId")
    private UUID statusToId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "WithComment")
    private Boolean withComment;

    @Column(name = "WithPassword")
    private Boolean withPassword;

    @Column(name = "WithUserDate")
    private Boolean withUserDate;

    @Column(name = "WithAutomaticTransition")
    private Boolean withAutomaticTransition;

    @Column(name = "StrConfigEntityId")
    private UUID strConfigEntityId;
}
