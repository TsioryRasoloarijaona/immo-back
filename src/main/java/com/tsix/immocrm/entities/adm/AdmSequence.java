package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/**
 * Compteur de numérotation du légataire, une ligne par type de document : source du prochain code,
 * pas {@code MAX(Code)}. La ligne « Dossier de vente » est globale, partagée par tous les préfixes.
 * {@code SeqValueNumber}/{@code SeqYearNumber} = largeurs de remplissage ; {@code BasedOnSystemYear} = année système.
 */
@Getter
@Setter
@Entity
@Table(name = "AdmSequence")
public class AdmSequence {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Column(name = "SeqYear")
    private Integer seqYear;

    @Column(name = "SeqYearNumber")
    private Integer seqYearNumber;

    @Column(name = "SeqValue")
    private Integer seqValue;

    @Column(name = "SeqValueNumber")
    private Integer seqValueNumber;

    @Size(max = 10)
    @Nationalized
    @Column(name = "Separator", length = 10)
    private String separator;

    @Column(name = "BasedOnSystemYear")
    private Boolean basedOnSystemYear;

    @Column(name = "CfgCompanyId")
    private UUID cfgCompanyId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TypeCode")
    private String typeCode;

    @Size(max = 50)
    @Nationalized
    @Column(name = "SeqPrefix", length = 50)
    private String seqPrefix;

    @Size(max = 50)
    @Nationalized
    @Column(name = "SeqSuffix", length = 50)
    private String seqSuffix;
}
