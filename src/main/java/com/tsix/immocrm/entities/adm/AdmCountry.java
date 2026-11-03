package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

@Getter
@Setter
@Entity
@Table(name = "AdmCountry")
public class AdmCountry extends AuditOmran {
    @Size(max = 255)
    @Nationalized
    @Column(name = "ISOAlpha3")
    private String isoAlpha3;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description_an")
    private String descriptionAn;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ISOName")
    private String isoName;

    @Size(max = 255)
    @Nationalized
    @Column(name = "NationCode")
    private String nationCode;

    // Indicatif numérique stocké en texte ("261").
    @Size(max = 255)
    @Nationalized
    @Column(name = "PhoneCode")
    private String phoneCode;

    @Size(max = 255)
    @Nationalized
    @Column(name = "PhoneFormat")
    private String phoneFormat;

    @Size(max = 255)
    @Nationalized
    @Column(name = "WebPhoneFormat")
    private String webPhoneFormat;
}
