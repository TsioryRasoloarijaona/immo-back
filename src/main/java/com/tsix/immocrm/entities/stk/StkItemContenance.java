package com.tsix.immocrm.entities.stk;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "StkItemContenance")
public class StkItemContenance extends LigneContenanceOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;
}
