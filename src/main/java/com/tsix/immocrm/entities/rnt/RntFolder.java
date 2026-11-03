package com.tsix.immocrm.entities.rnt;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;
import java.util.UUID;

/** Dossier de location OMRAN, lu par l'onglet Dossiers d'un tiers ; jamais écrit par nous. */
@Getter
@Entity
@Immutable
@Table(name = "RntFolder")
public class RntFolder extends CodeLibelleOmran {
    @Column(name = "DocDate")
    private LocalDateTime docDate;
    @Column(name = "CfgCustomerId")
    private UUID cfgCustomerId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrStatusId")
    private StrStatus strStatus;
}
