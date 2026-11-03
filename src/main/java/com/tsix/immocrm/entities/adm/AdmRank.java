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

@Getter
@Setter
@Entity
@Table(name = "AdmRank")
public class AdmRank {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "\"Rank\"")
    private Integer rang;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;
}
