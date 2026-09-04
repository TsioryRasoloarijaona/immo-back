package com.tsix.immocrm.entities.adm;

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
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * dbo_AdmAttachedFile — read-only, metadata-only mapping (no {@code AttachedFile} blob column)
 * for listing a Dossier's pièces jointes without pulling megabytes of binary data into memory.
 * See {@link AdmAttachedFile} for the full mapping (with the blob) used when actually
 * downloading one file.
 *
 * <p><b>Linkage confirmed against real data</b>: there is no declared foreign key from this
 * table to anything (verified via {@code sp_help} — "Aucune clé étrangère ne fait référence à
 * la table 'AdmAttachedFile'"). The legacy app instead reuses {@code CategoryId} as a generic
 * polymorphic parent reference: for Dossier attachments, {@code CategoryId = ComFolder.PKey}.
 * This table also stores attachments for other object types, so {@code CategoryId} is NOT
 * scoped to {@code ComFolder} alone in the abstract — but real data disproved the original
 * assumption that Dossier attachments all carry a {@code Category} starting with "Dossier de
 * vente": a real dossier was found with 5 of its 6 attachments categorized
 * "Yes.Com.Sales.Sale" (chèques, contrats...) instead, so
 * {@link com.tsix.immocrm.repositories.adm.AdmAttachedFileSummaryRepository#findByDossierId}
 * now matches on {@code CategoryId} alone, with no {@code Category} prefix filter — acceptable
 * since a UUID collision with a differently-typed object's PKey is astronomically unlikely,
 * consistent with how {@code StrHistory}/{@code AdmWebHistorics}' own {@code ObjectId} columns
 * are used elsewhere in this codebase.
 */
@Getter
@Setter
@Entity
@Table(name = "AdmAttachedFile")
public class AdmAttachedFileSummary {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Category")
    private String category;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Extention")
    private String extension;

    @Column(name = "CategoryId")
    private UUID categoryId;

    /** Type de document (ex. "PIECE D'IDENTITE", "CONTRAT"...) — voir {@link AdmAttachedFileType}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmAttachedFileTypeId")
    private AdmAttachedFileType type;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    /**
     * Calculé côté SQL Server ({@code @Formula}, lecture seule) — vrai quand la ligne porte un
     * fichier réellement déposé. Les lignes sans fichier sont les cases de checklist
     * {@code IsCheckedButNotAttached = 1} ("document requis coché mais jamais téléversé") :
     * {@code CategoryId} correspond bien, mais rien n'a jamais été déposé. Elles sont exclues
     * par {@link com.tsix.immocrm.repositories.adm.AdmAttachedFileSummaryRepository#findByCategoryId}.
     *
     * <p><b>La formule interroge désormais {@code AppFileObject}, plus {@code AttachedFile}.</b>
     * C'était le point de rupture silencieuse du basculement vers S3 : la colonne
     * {@code AttachedFile} est supprimée par
     * {@code 004_drop_attachedfile_filestream.sql}, et la formule précédente la nommait. Elle
     * aurait fait échouer non pas les téléchargements, mais <b>toutes les requêtes de liste</b>
     * ("Nom de colonne non valide"), puisqu'elle est évaluée à chaque chargement de cette entité.
     */
    @Formula("(CASE WHEN EXISTS (SELECT 1 FROM AppFileObject afo "
            + "WHERE afo.AdmAttachedFileId = PKey AND afo.DeletedAt IS NULL) THEN 1 ELSE 0 END)")
    private Boolean hasFile;
}
