package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Connexions OMRAN, jamais créées par nous. {@code WebSession} et {@code AdmSessionOriginId} : non mappés exprès. */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "AdmWebSessions")
public class AdmWebSessions {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "UserId")
    private UUID userId;

    @Nationalized
    @Column(name = "UserDesc")
    private String userDesc;

    @Column(name = "StartDate", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "EndDate")
    private LocalDateTime endDate;

    @Nationalized
    @Column(name = "IPAddress")
    private String ipAddress;

    @Column(name = "State", nullable = false)
    private int state;

    @Size(max = 255)
    @Column(name = "SystemVersion")
    private String systemVersion;
}
