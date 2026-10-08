package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JoinFormula;
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

    /**
     * L'employé <i>concerné</i>, pas l'auteur : ne jamais s'en servir pour attribuer une interaction
     * (souvent vide, parfois une autre personne que {@link #admUser}). Voir FEATURE-kpi-performance-commerciaux.md §3.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgEmployeeId")
    private CfgTier cfgEmployee;

    /**
     * L'auteur (le compte qui a saisi) : la seule attribution fiable, toujours conforme à {@code UserCreate}.
     * Le commercial s'en déduit par {@code AdmUser.cfgEmployee}, parfois absent.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmUserId")
    private AdmUser admUser;

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
    private ComActionMessage messageParent;

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

    /** « Action » : nature de l'interaction (Note, Visite, Téléphone entrant…). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComActionMessageTypeId")
    private ComActionMessageType comActionMessageType;

    /** Toujours dérivée de la catégorie de {@link #comActionMessageSubCategory} quand il y en a une. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComActionMessageCategoryId")
    private ComActionMessageCategory comActionMessageCategory;

    /** « Résultat » de l'interaction (Rendez vous pris, Hors Budget…). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComActionMessageSubCategoryId")
    private ComActionMessageSubCategory comActionMessageSubCategory;

    /** Rendez-vous créé pour les résultats « Rendez vous pris », « RDV reporté », visite du projet. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmEventId")
    private AdmEvent admEvent;

    /**
     * Tranche résolue comme {@code ComActionMessageView} (Prospection → tranche de la prospection, sinon
     * de l'article), en formule de jointure car la vue est trop lente ; groupable car les sous-requêtes
     * sont dans la jointure, pas le {@code GROUP BY}. Voir FEATURE-suivi-actions.md §1.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinFormula("(CASE WHEN ComActionMessageCategoryId = '69F8E7D6-1111-1111-1111-1AA8B1F02008' "
            + "THEN (SELECT p.StkHierarchyId FROM ComProspection p WHERE p.PKey = ComProspectionId) "
            + "ELSE (SELECT i.StkHierarchyId FROM StkItem i WHERE i.PKey = StkItemId) END)")
    private CfgTranche trancheResolue;

}