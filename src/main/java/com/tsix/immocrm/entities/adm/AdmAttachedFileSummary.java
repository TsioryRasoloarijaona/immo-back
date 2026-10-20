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
 * {@code AdmAttachedFile} en lecture seule, sans le binaire, pour les listes de pièces jointes.
 * Aucune FK : {@code CategoryId} = PKey du porteur (polymorphe). Ne pas filtrer sur {@code Category} :
 * des pièces de dossier sont classées "Yes.Com.Sales.Sale" et non "Dossier de vente…".
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmAttachedFileTypeId")
    private AdmAttachedFileType type;

    @Size(max = 255)
    @Nationalized
    @Column(name = "FoderGroup")
    private String groupe;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    /**
     * Faux pour les cases de checklist {@code IsCheckedButNotAttached = 1} (requis, jamais déposé).
     * Ne jamais y nommer {@code AttachedFile} : colonne supprimée par 004_drop_attachedfile_filestream.sql,
     * la formule étant évaluée à chaque chargement, toutes les listes échoueraient.
     */
    @Formula("(CASE WHEN EXISTS (SELECT 1 FROM AppFileObject afo "
            + "WHERE afo.AdmAttachedFileId = PKey AND afo.DeletedAt IS NULL) THEN 1 ELSE 0 END)")
    private Boolean hasFile;
}
