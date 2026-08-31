package com.tsix.immocrm.entities.stk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * dbo.StkKind — the physical nature of a {@link StkItem} (e.g. "TER" for terrain/land, vs a
 * built lot). Distinct from {@link StkItemCategory}/{@link StkItemType} (marketing
 * category/type). Drives conditional wording in generated documents (the legacy "Acte de
 * réservation" template omits floor/building and the "achèvement des travaux" mention, and
 * renumbers a whole section, when {@code StkKind.Code == "TER"} — land has no construction to
 * complete and no keys/syndic to manage) — see {@code ActeReservationService}.
 */
@Getter
@Setter
@Entity
@Table(name = "StkKind")
public class StkKind {
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

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserUpdate")
    private String userUpdate;

    @Column(name = "UserUpdateDateTime")
    private LocalDateTime userUpdateDateTime;
}
