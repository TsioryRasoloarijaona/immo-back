package com.tsix.immocrm.entities.app;

import com.tsix.immocrm.domain.StatutDemandeRemise;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Table applicative (script 207) : demande de remise d'une agence sur un dossier ; taux en pourcentage 0-100. */
@Getter
@Setter
@Entity
@Table(name = "AppDemandeRemise")
public class AppDemandeRemise {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "ComFolderId", nullable = false)
    private UUID dossierId;

    @NotNull
    @Column(name = "TauxDemande", nullable = false, precision = 26, scale = 6)
    private BigDecimal tauxDemande;

    @Column(name = "TauxAccorde", precision = 26, scale = 6)
    private BigDecimal tauxAccorde;

    @NotNull
    @Nationalized
    @Size(max = 1000)
    @Column(name = "Motif", nullable = false, length = 1000)
    private String motif;

    @Nationalized
    @Size(max = 1000)
    @Column(name = "MotifRefus", length = 1000)
    private String motifRefus;

    @NotNull
    @Nationalized
    @Enumerated(EnumType.STRING)
    @Column(name = "Statut", nullable = false, length = 20)
    private StatutDemandeRemise statut;

    @NotNull
    @Column(name = "DemandeParUserId", nullable = false)
    private UUID demandeParUserId;

    @NotNull
    @Column(name = "DemandeLe", nullable = false)
    private LocalDateTime demandeLe;

    @Column(name = "TraiteParUserId")
    private UUID traiteParUserId;

    @Column(name = "TraiteLe")
    private LocalDateTime traiteLe;
}
