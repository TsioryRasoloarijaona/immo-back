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
 * Lien d'une entité vers une autre pour les champs obligatoires : pour le dossier, « Client »
 * ({@code CfgCustomerId}, LINKED_IS_PARENT) et « Acquéreurs » ({@code Purchasers.CfgTierId}, LINKED_IS_UNDERCHILD).
 */
@Getter
@Setter
@Entity
@Table(name = "StrEntityLinkedEntity")
public class StrEntityLinkedEntity {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Column(name = "StrLinkedEntityId")
    private UUID strLinkedEntityId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "LinkPropertyName")
    private String linkPropertyName;

    @Size(max = 255)
    @Nationalized
    @Column(name = "LinkType")
    private String linkType;

    @Column(name = "StrConfigEntityId")
    private UUID strConfigEntityId;
}
