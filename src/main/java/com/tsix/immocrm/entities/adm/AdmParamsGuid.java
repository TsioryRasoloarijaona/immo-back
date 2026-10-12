package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Paramètre GUID du légataire ({@code DefaultCurrency}…) ; clé primaire {@code Code}, {@code PKey} est nullable. */
@Getter
@Entity
@Immutable
@Table(name = "AdmParamsGuid")
public class AdmParamsGuid {

    @Id
    @Size(max = 255)
    @Nationalized
    @Column(name = "Code", nullable = false)
    private String code;

    @Column(name = "CodeValue")
    private UUID codeValue;
}
