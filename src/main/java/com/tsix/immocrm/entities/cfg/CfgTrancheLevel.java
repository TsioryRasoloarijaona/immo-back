package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.lnd.LndLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "CfgTrancheLevel")
public class CfgTrancheLevel {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "CfgTrancheId")
    private CfgTranche cfgTranche;

    /** Sans lien avec {@code StkItem.StkFloorId} : niveau de construction du référentiel foncier. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LndLevelId")
    private LndLevel lndLevel;

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