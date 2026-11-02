package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Verrous posés par OMRAN, jamais créés par nous. {@code DataObjectId} : non mappé exprès. */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "AdmWebLockEntities")
public class AdmWebLockEntity {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Nationalized
    @Column(name = "Code")
    private String code;

    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "LockDate", nullable = false)
    private LocalDateTime lockDate;

    @Column(name = "SessionId", nullable = false)
    private UUID sessionId;

    @Column(name = "State", nullable = false)
    private int state;
}
