package com.tsix.immocrm.entities.stl;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "StlSettlementRelation")
public class StlSettlementRelation {
    @Id
    @NotNull
    @Column(name = "PKey", nullable = false)
    private UUID pKey;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;


}