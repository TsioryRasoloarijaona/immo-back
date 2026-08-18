package com.tsix.immocrm.entities.stl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "StlCategory")
public class StlCategory {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "CodeCountable")
    private String codeCountable;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "IsCredit")
    private Boolean isCredit;

    @Column(name = "ParentId")
    private UUID parentId;

    @Column(name = "IsWithoutPrevision")
    private Boolean isWithoutPrevision;

    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "IsCharge")
    private Boolean isCharge;

    @Column(name = "ActAccountForPaymentId")
    private UUID actAccountForPaymentId;

    @Column(name = "IsProduct")
    private Boolean isProduct;


}