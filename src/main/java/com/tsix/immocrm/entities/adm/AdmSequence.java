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
 * dbo_AdmSequence — the legacy document-numbering counter (e.g. {@code Code = "Dossier de
 * vente"}). Confirmed against real data: its {@code SeqValue} (379) exactly matches the highest
 * existing {@code ComFolder.Code} sequence number ("... 2026/00379") — this is the real,
 * authoritative source for the next Dossier number, not something to recompute via
 * {@code MAX(ComFolder.Code)}. One row per document type; the "Dossier de vente" row is global
 * (not scoped per project/tranche — every project/prefix shares the same counter, confirmed: real
 * codes count down 379→378→377 across different prefixes APF/ART/ARS with no gaps).
 *
 * <p>{@code SeqValueNumber} is the zero-padding width for the number (5 → "00380"),
 * {@code SeqYearNumber} the width for the year (4 → "2026"). {@code BasedOnSystemYear = true}
 * means the year in the formatted code is the current system year, not a stored one.
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
