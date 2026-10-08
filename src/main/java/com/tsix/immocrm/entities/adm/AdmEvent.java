package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
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

import java.time.LocalDateTime;
import java.util.UUID;

/** Rendez-vous du calendrier, lié par {@code ComActionMessage.AdmEventId}. */
@Getter
@Setter
@Entity
@Table(name = "AdmEvent")
public class AdmEvent {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Subject")
    private String subject;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Location")
    private String location;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Column(name = "StartHour")
    private LocalDateTime startHour;

    @Column(name = "EndHour")
    private LocalDateTime endHour;

    @Column(name = "AllDay")
    private Boolean allDay;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Color")
    private String color;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Status")
    private String status;

    @Size(max = 255)
    @Nationalized
    @Column(name = "EventStatus")
    private String eventStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ComActionMessageSubCategoryId")
    private ComActionMessageSubCategory comActionMessageSubCategory;

    // Faute de frappe du schéma réel (« Emloyee »).
    @Column(name = "CfgEmloyeeId")
    private UUID cfgEmployeeId;

    @Column(name = "StrEntityId")
    private UUID strEntityId;

    @Column(name = "AdmEventTypeId")
    private UUID admEventTypeId;

    @Column(name = "ComFolderId")
    private UUID comFolderId;

    @Column(name = "CfgTrancheId")
    private UUID cfgTrancheId;

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
