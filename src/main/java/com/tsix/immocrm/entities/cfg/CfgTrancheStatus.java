package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.com.ComFolderStatusPaymentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "CfgTrancheStatus")
public class CfgTrancheStatus {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "CfgTrancheId")
    private CfgTranche cfgTranche;

    @Column(name = "ThresholdOperator")
    private Integer thresholdOperator;

    @Column(name = "ThresholdRatio", precision = 26, scale = 6)
    private BigDecimal thresholdRatio;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComFolderStatusPaymentTypeId")
    private ComFolderStatusPaymentType comFolderStatusPaymentType;

    /** Statut dont c'est le seuil de versement ; {@code CfgTrancheId} nul = seuil global. */
    @Column(name = "StrStatusId")
    private UUID strStatusId;

    @Column(name = "StrFolderEntityId")
    private UUID strFolderEntityId;

}