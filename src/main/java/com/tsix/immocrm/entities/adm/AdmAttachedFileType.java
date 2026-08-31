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
 * dbo_AdmAttachedFileType — reference table for the "type" of a pièce jointe (ex. "PIECE
 * D'IDENTITE", "CONTRAT", "FICHE PERMISSION REMISE DES CLES"...). Referenced by
 * {@code AdmAttachedFile.AdmAttachedFileTypeId} (see {@link AdmAttachedFileSummary}).
 * {@code Code} and {@code Description} are identical for every row in real data (confirmed via
 * {@code SELECT COUNT(*) WHERE Code <> Description} = 0), so only {@code Description} is
 * exposed at the API boundary.
 */
@Getter
@Setter
@Entity
@Table(name = "AdmAttachedFileType")
public class AdmAttachedFileType {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;
}
