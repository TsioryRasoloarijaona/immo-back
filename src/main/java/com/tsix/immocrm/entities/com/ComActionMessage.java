package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.cfg.CfgTier;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "ComActionMessage")
public class ComActionMessage {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "IsIn")
    private Boolean isIn;

    @Column(name = "WithoutRevival")
    private Boolean withoutRevival;

    @Column(name = "AlertValidityDate")
    private LocalDateTime alertValidityDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserName")
    private String userName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgEmployeeId")
    private CfgTier cfgEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "CfgTierId")
    private CfgTier cfgTier;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "ComProspectionId")
    private ComProspection comProspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "ComFolderId")
    private ComFolder comFolder;

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

    @Column(name = "IsUnpaid")
    private Boolean isUnpaid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComActionMessageId")
    private ComActionMessage comActionMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "ComDocumentId")
    private ComDocument comDocument;

    @Nationalized
    @Lob
    @Column(name = "AlertMessage")
    private String alertMessage;

    @Column(name = "IsSystem")
    private Boolean isSystem;

    @Column(name = "IsNotRead")
    private Boolean isNotRead;

    @Column(name = "IsSent")
    private Boolean isSent;


}