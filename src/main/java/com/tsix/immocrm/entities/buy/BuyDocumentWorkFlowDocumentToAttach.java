package com.tsix.immocrm.entities.buy;

import com.tsix.immocrm.entities.stk.DocumentAJoindreOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "BuyDocumentWorkFlowDocumentToAttach")
public class BuyDocumentWorkFlowDocumentToAttach extends DocumentAJoindreOmran {
    @Column(name = "BuyDocumentWorkFlowId")
    private UUID buyDocumentWorkFlowId;

    @Column(name = "PrjMarketTypeId")
    private UUID prjMarketTypeId;

    @Column(name = "ForFirstWorkingOut")
    private Boolean forFirstWorkingOut;

    @Column(name = "ForIntermediateWorkingOut")
    private Boolean forIntermediateWorkingOut;

    @Column(name = "ForFinalWorkingOut")
    private Boolean forFinalWorkingOut;

    @Override
    public void setPorteurId(UUID porteurId) {
        buyDocumentWorkFlowId = porteurId;
    }
}
