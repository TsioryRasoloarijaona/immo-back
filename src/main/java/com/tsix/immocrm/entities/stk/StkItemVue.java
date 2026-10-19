package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.LigneOmran;
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
@Table(name = "StkItemVue")
public class StkItemVue extends LigneOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkVueId")
    private StkVue stkVue;
}
