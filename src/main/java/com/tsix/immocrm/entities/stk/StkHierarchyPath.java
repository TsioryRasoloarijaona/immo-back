package com.tsix.immocrm.entities.stk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/** Tracé de plan d'un article sur un étage ; l'étage du tracé peut différer de celui de l'article. */
@Getter
@Setter
@Entity
@Table(name = "StkHierarchyPath")
public class StkHierarchyPath {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    /** Colonne {@code text} (non Unicode) : LONGVARCHAR, pas {@code @Nationalized}. */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "SvgPath")
    private String svgPath;

    @Column(name = "StkItemId")
    private UUID stkItemId;

    @Column(name = "StkFloorId")
    private UUID stkFloorId;
}
