package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Journal des e-mails d'OMRAN, jamais écrit par nous. {@code SmtpHost}, {@code SmtpPort}, {@code EnableSsl} : non mappés exprès. */
@Getter
@Entity
@Immutable
@Table(name = "AdmEMailHistory")
public class AdmEMailHistory {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "MailDate")
    private LocalDateTime mailDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "AdmUserDescription")
    private String admUserDescription;

    @Size(max = 255)
    @Nationalized
    @Column(name = "DisplayFromName")
    private String displayFromName;

    @Size(max = 255)
    @Nationalized
    @Column(name = "EmailFrom")
    private String emailFrom;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "EmailsTo", length = 1000)
    private String emailsTo;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "EmailsCc", length = 1000)
    private String emailsCc;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "EmailsBcc", length = 1000)
    private String emailsBcc;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Subject", length = 1000)
    private String subject;

    @Column(name = "IsBodyHtml")
    private Boolean bodyHtml;

    @Nationalized
    @Column(name = "Body")
    private String body;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "ObjectLabel", length = 1000)
    private String objectLabel;

    @Column(name = "IsAutoMatic")
    private Boolean autoMatic;

    @Column(name = "EmailsSent")
    private Boolean emailsSent;

    @Nationalized
    @Column(name = "ErrorDescription")
    private String errorDescription;
}
