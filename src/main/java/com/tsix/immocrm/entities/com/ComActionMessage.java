package com.tsix.immocrm.entities.com;

import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.adm.AdmUser;
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

    /**
     * <b>L'employé <i>concerné</i>, pas l'auteur</b> — ne pas s'en servir pour attribuer une
     * interaction à quelqu'un. Renseignée sur 37 186/48 118 lignes seulement, et là où les deux
     * sont présentes elle désigne une personne <b>différente</b> de {@link #admUser} sur 844
     * lignes. Le départage vient de {@code UserCreate}, la trace laissée par l'application
     * Windows : elle vaut le compte 37 186/37 186 et cet employé-ci 30 894/37 186.
     * Voir {@code FEATURE-kpi-performance-commerciaux.md} §3.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgEmployeeId")
    private CfgTier cfgEmployee;

    /**
     * <b>L'auteur de l'interaction</b> : le compte qui l'a saisie. Renseigné sur les 48 118 lignes
     * réelles, et concordant avec {@code UserCreate} sans une seule exception — c'est donc la
     * seule attribution fiable, et celle qu'utilisent les KPI de performance commerciale. Le
     * commercial s'en déduit par {@code AdmUser.cfgEmployee} (N→1, toujours résoluble sauf pour
     * 80 lignes dont le compte n'a pas d'employé).
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

    // -- "Action"/"Résultat"/"Date du rendez-vous" fields (hotline & prospection interactions) --

    /** "Action" — nature of the interaction (Note, Visite, Téléphone entrant, Mail Sortant...). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComActionMessageTypeId")
    private ComActionMessageType comActionMessageType;

    /**
     * Business context of the interaction (Prospection, Dossier de vente, Réclamation...).
     * Always derived from {@link #comActionMessageSubCategory}'s own category when one is set —
     * never set independently by client code.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComActionMessageCategoryId")
    private ComActionMessageCategory comActionMessageCategory;

    /** "Résultat" — outcome of the interaction (Rendez vous pris, Hors Budget, RDV reporté...). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComActionMessageSubCategoryId")
    private ComActionMessageSubCategory comActionMessageSubCategory;

    /**
     * The appointment created when {@link #comActionMessageSubCategory} is "Rendez vous pris",
     * "RDV reporté" or "Le prospect veut faire une visite du projet" — {@code admEvent.startHour}
     * is the "Date du rendez-vous"/visite, listed by {@code GET /api/calendrier/evenements}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmEventId")
    private AdmEvent admEvent;

}