package com.tsix.immocrm.entities.bul;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Réunion de chantier ; {@code Code} = Référence P.V., {@code Sorting} = O.D.T. dans (projet, type). */
@Getter
@Setter
@Entity
@Table(name = "BulMeeting")
public class BulMeeting extends LigneOmran {
    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "MeetingDate")
    private LocalDateTime meetingDate;

    @Column(name = "NextDate")
    private LocalDateTime nextDate;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Note")
    private String note;

    @Column(name = "PrjProjectId")
    private UUID prjProjectId;

    @Column(name = "BulMeetingTypeId")
    private UUID bulMeetingTypeId;

    @Column(name = "CfgEmployeeId")
    private UUID cfgEmployeeId;
}
