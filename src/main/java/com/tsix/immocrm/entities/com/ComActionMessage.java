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

    /**
     * Tranche de l'action <b>telle que la résout la vue OMRAN {@code ComActionMessageView}</b> :
     * catégorie Prospection → tranche de la prospection, sinon tranche de l'article. C'est la
     * colonne sur laquelle l'écran « Suivi des actions » filtre ({@code CfgTrancheId IN (…)}).
     *
     * <p>Lecture seule, portée par une formule de jointure plutôt que par la vue elle-même : la vue
     * coûte 6,6 s pour une page de 50 (son {@code DISTINCT} et son rang corrélé sont calculés sur
     * toutes les lignes), cette jointure quelques dizaines de millisecondes. Les sous-requêtes sont
     * dans la condition de jointure, pas dans un {@code GROUP BY} : grouper sur
     * {@code trancheResolue.code} reste légal pour SQL Server. Résolue sur 46 559 des 48 213
     * actions réelles. Voir {@code instructions/FEATURE-suivi-actions.md} §1.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinFormula("(CASE WHEN ComActionMessageCategoryId = '69F8E7D6-1111-1111-1111-1AA8B1F02008' "
            + "THEN (SELECT p.StkHierarchyId FROM ComProspection p WHERE p.PKey = ComProspectionId) "
            + "ELSE (SELECT i.StkHierarchyId FROM StkItem i WHERE i.PKey = StkItemId) END)")
    private CfgTranche trancheResolue;

}