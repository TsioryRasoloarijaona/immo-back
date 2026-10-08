package com.tsix.immocrm.entities.app;

import com.tsix.immocrm.entities.adm.AdmUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clé d'API d'un client machine (table applicative, voir FEATURE-cle-api-n8n.md) : seul le hash est
 * stocké. Le compte de service porte l'auteur et le périmètre ; {@code Portees} limite les routes.
 */
@Getter
@Setter
@Entity
@Table(name = "AppApiClient")
public class AppApiClient {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @NotNull
    @Nationalized
    @Size(max = 100)
    @Column(name = "Nom", nullable = false, length = 100)
    private String nom;

    @NotNull
    @Nationalized
    @Size(max = 64)
    @Column(name = "CleHash", nullable = false, length = 64)
    private String cleHash;

    /** {@code ak_} + 8 premiers caractères : de quoi reconnaître une clé dans une liste. */
    @NotNull
    @Nationalized
    @Size(max = 12)
    @Column(name = "ClePrefixe", nullable = false, length = 12)
    private String clePrefixe;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AdmUserId", nullable = false)
    private AdmUser admUser;

    /** Codes {@link com.tsix.immocrm.domain.PorteeCleApi} séparés par des virgules. */
    @NotNull
    @Nationalized
    @Size(max = 1000)
    @Column(name = "Portees", nullable = false, length = 1000)
    private String portees;

    @NotNull
    @Column(name = "Actif", nullable = false)
    private Boolean actif = true;

    @Column(name = "DernierUsage")
    private LocalDateTime dernierUsage;

    @Nationalized
    @Size(max = 255)
    @Column(name = "UserCreate")
    private String userCreate;

    @NotNull
    @Column(name = "UserCreateDateTime", nullable = false)
    private LocalDateTime userCreateDateTime;

    @Nationalized
    @Size(max = 255)
    @Column(name = "RevoquePar")
    private String revoquePar;

    @Column(name = "RevoqueDateTime")
    private LocalDateTime revoqueDateTime;
}
