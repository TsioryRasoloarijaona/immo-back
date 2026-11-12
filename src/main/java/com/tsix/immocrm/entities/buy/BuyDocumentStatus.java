package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.AuditTrieOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Statut de pièce d'achat ; listes de champs au format OMRAN « ,a,b » (CatalogueStatutAchat). */
@Getter
@Setter
@Entity
@Table(name = "BuyDocumentStatus")
public class BuyDocumentStatus extends AuditTrieOmran {
    @Column(name = "IsOkForPayement")
    private Boolean isOkForPayement;

    @Column(name = "IsValidatedOnAccounting")
    private Boolean isValidatedOnAccounting;

    @Column(name = "IsReadOnly")
    private Boolean isReadOnly;

    @Column(name = "AdmWorkFlowStateTypeId")
    private UUID admWorkFlowStateTypeId;

    @Column(name = "CfgImageId")
    private UUID cfgImageId;

    @Column(name = "Delay")
    private Integer delay;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ColorName")
    private String colorName;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "SupplierMandatoryFields")
    private String supplierMandatoryFields;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "MandatoryFields")
    private String mandatoryFields;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;
}
