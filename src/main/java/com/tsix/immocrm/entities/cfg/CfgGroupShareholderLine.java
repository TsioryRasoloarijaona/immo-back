package com.tsix.immocrm.entities.cfg;

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

/** Ligne d'un groupe d'actionnaires ; {@code SharingNumber} est ici une fraction (0,4 = 40 %), pas un nombre de parts. */
@Getter
@Setter
@Entity
@Table(name = "CfgGroupShareholderLine")
public class CfgGroupShareholderLine {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgGroupShareholderId")
    private CfgGroupShareholder cfgGroupShareholder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgShareholderId")
    private CfgShareholder cfgShareholder;

    @Column(name = "SharingNumber", precision = 26, scale = 6)
    private BigDecimal sharingNumber;

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
