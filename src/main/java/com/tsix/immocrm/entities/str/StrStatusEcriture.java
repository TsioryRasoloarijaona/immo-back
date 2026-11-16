package com.tsix.immocrm.entities.str;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Fiche complète d'un statut, pour l'éditeur des statuts de lot (FEATURE-restes-projet-achat.md §6) ; {@link StrStatus}
 * reste la lecture des autres modules. Les cases {@code BoolField001…020} se lisent et s'écrivent par numéro.
 */
@Getter
@Setter
@Entity
@Table(name = "StrStatus")
public class StrStatusEcriture extends AuditOmran {

    public static final int NOMBRE_CASES = 20;
    private static final VarHandle[] CASES = IntStream.rangeClosed(1, NOMBRE_CASES)
            .mapToObj(StrStatusEcriture::poignee).toArray(VarHandle[]::new);

    @Column(name = "StrStatusTypeId")
    private UUID strStatusTypeId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Column(name = "CfgImageId")
    private UUID cfgImageId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ColorName")
    private String colorName;

    @Column(name = "Delay")
    private Integer delay;

    @Column(name = "StrConfigEntityId")
    private UUID strConfigEntityId;

    @Column(name = "BoolField001") private Boolean boolField001;
    @Column(name = "BoolField002") private Boolean boolField002;
    @Column(name = "BoolField003") private Boolean boolField003;
    @Column(name = "BoolField004") private Boolean boolField004;
    @Column(name = "BoolField005") private Boolean boolField005;
    @Column(name = "BoolField006") private Boolean boolField006;
    @Column(name = "BoolField007") private Boolean boolField007;
    @Column(name = "BoolField008") private Boolean boolField008;
    @Column(name = "BoolField009") private Boolean boolField009;
    @Column(name = "BoolField010") private Boolean boolField010;
    @Column(name = "BoolField011") private Boolean boolField011;
    @Column(name = "BoolField012") private Boolean boolField012;
    @Column(name = "BoolField013") private Boolean boolField013;
    @Column(name = "BoolField014") private Boolean boolField014;
    @Column(name = "BoolField015") private Boolean boolField015;
    @Column(name = "BoolField016") private Boolean boolField016;
    @Column(name = "BoolField017") private Boolean boolField017;
    @Column(name = "BoolField018") private Boolean boolField018;
    @Column(name = "BoolField019") private Boolean boolField019;
    @Column(name = "BoolField020") private Boolean boolField020;

    private static VarHandle poignee(int numero) {
        try {
            return MethodHandles.lookup().findVarHandle(StrStatusEcriture.class, "boolField%03d".formatted(numero),
                    Boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Case {@code numero} (1-20) cochée ; NULL vaut faux. */
    public boolean coche(int numero) {
        return Boolean.TRUE.equals(CASES[numero - 1].get(this));
    }

    public void cocher(int numero, boolean valeur) {
        CASES[numero - 1].set(this, Boolean.valueOf(valeur));
    }
}
