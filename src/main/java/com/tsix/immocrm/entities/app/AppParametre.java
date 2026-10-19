package com.tsix.immocrm.entities.app;

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

/** Paramètre d'application réglable par l'Administrateur (table applicative, FEATURE-suivi-commerciaux.md §7.6). */
@Getter
@Setter
@Entity
@Table(name = "AppParametre")
public class AppParametre {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @NotNull
    @Nationalized
    @Size(max = 100)
    @Column(name = "Cle", nullable = false, length = 100)
    private String cle;

    @Nationalized
    @Size(max = 500)
    @Column(name = "Valeur", length = 500)
    private String valeur;
}
