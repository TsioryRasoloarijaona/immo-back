package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Valeur d'un champ spécifique pour un objet ({@code NameSpaceId} = PKey du porteur, sans clé étrangère). */
@Getter
@Setter
@Entity
@Table(name = "AdmSpecificFieldValue")
public class AdmSpecificFieldValue extends LigneOmran {

    @Column(name = "AdmSpecificFieldId")
    private UUID champId;

    @Column(name = "NameSpaceId")
    private UUID nameSpaceId;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Value", length = 1000)
    private String value;

    @Column(name = "DateValue")
    private LocalDateTime dateValue;

    @Column(name = "BoolValue")
    private Boolean boolValue;

    @Column(name = "DecimalValue", precision = 26, scale = 6)
    private BigDecimal decimalValue;

    @Column(name = "IntValue")
    private Integer intValue;
}
