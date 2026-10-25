package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** Définition d'un champ spécifique ; les colonnes de défaut typées d'OMRAN ne sont pas mappées (jamais lues). */
@Getter
@Setter
@Entity
@Table(name = "AdmSpecificField")
public class AdmSpecificField extends LigneOmran {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmSpecificFieldEntityId")
    private AdmSpecificFieldEntity entite;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Category")
    private String category;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Name")
    private String name;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Caption")
    private String caption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmSpecificFieldDataTypeId")
    private AdmSpecificFieldDataType dataType;

    @Size(max = 255)
    @Nationalized
    @Column(name = "DefaultValue")
    private String defaultValue;

    @Column(name = "Required")
    private Boolean required;

    @Column(name = "CategorySorting")
    private Integer categorySorting;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ComboValue")
    private String comboValue;
}
