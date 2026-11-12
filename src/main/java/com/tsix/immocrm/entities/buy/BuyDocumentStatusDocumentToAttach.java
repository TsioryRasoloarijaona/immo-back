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
@Table(name = "BuyDocumentStatusDocumentToAttach")
public class BuyDocumentStatusDocumentToAttach extends DocumentAJoindreOmran {
    @Column(name = "BuyDocumentStatusId")
    private UUID buyDocumentStatusId;

    @Override
    public void setPorteurId(UUID porteurId) {
        buyDocumentStatusId = porteurId;
    }
}
