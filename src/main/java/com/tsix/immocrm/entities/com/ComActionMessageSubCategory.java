package com.tsix.immocrm.entities.com;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * « Résultat » d'une interaction, rattaché à une catégorie ; la liste hotline/prospection = celles
 * de la catégorie {@code "Prospection"}. {@code isRevivalMandatory} : le légataire attend une date d'alerte.
 */
@Getter
@Setter
@Entity
@Table(name = "ComActionMessageSubCategory")
public class ComActionMessageSubCategory {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComActionMessageCategoryId")
    private ComActionMessageCategory comActionMessageCategory;

    @Column(name = "IsRevivalMandatory")
    private Boolean isRevivalMandatory;

    @Column(name = "UpdateFinancialSchema")
    private Boolean updateFinancialSchema;

    @Column(name = "IsCommercialSetAutomatically")
    private Boolean isCommercialSetAutomatically;

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
