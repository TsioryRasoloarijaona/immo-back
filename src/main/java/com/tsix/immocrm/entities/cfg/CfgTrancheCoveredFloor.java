package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.stk.StkItemCategory;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Onglet « Plancher couvert » de la tranche : une superficie par catégorie d'article. */
@Getter
@Setter
@Entity
@Table(name = "CfgTrancheCoveredFloor")
public class CfgTrancheCoveredFloor {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTrancheId")
    private CfgTranche cfgTranche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemCategoryId")
    private StkItemCategory stkItemCategory;

    @Column(name = "Area", precision = 26, scale = 16)
    private BigDecimal area;

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
