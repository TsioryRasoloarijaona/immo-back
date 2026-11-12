package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "BuyDocumentWorkFlowAdmRole")
public class BuyDocumentWorkFlowAdmRole extends LigneOmran {
    @Column(name = "AdmRoleId")
    private UUID role;

    @Column(name = "WorkFlowId")
    private UUID transition;
}
