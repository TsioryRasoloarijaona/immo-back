package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Transition de statut d'achat propre à une gestion ({@code StrEntityId} NULL = « Sans gestion »). */
@Getter
@Setter
@Entity
@Table(name = "BuyDocumentWorkFlow")
public class BuyDocumentWorkFlow extends LigneOmran {
    @Column(name = "StrEntityId")
    private UUID strEntityId;

    @Column(name = "StartStatusId")
    private UUID startStatusId;

    @Column(name = "EndStatusId")
    private UUID endStatusId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "IsValidatedByCfgTierTrigger")
    private Boolean isValidatedByCfgTierTrigger;

    @Column(name = "NotesMandatory")
    private Boolean notesMandatory;

    // OMRAN fait .Split(',') sur ces deux listes en changeant le statut d'une pièce : jamais NULL.
    @Size(max = 255)
    @Nationalized
    @Column(name = "MandatoryFields")
    private String mandatoryFields;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "SupplierMandatoryFields")
    private String supplierMandatoryFields;
}
