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
 * dbo.ComActionMessageSubCategory — the "Résultat" of an interaction (e.g. "Rendez vous pris",
 * "RDV reporté", "Client injoignable ou Boite vocale", "Hors Budget"...), scoped to a
 * {@link ComActionMessageCategory}. {@code isRevivalMandatory} flags results that the legacy
 * app expects a follow-up alert date for (see {@code ComActionMessage.alertValidityDate}).
 * Confirmed empirically: the "Résultat" dropdown for hotline/prospection interactions is the set
 * of sub-categories under the category coded {@code "Prospection"} (23 rows in real data).
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
