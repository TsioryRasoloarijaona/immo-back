package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Cultures web d'OMRAN, lues pour la fiche utilisateur : aucun setter, la table n'est jamais écrite ici. */
@Getter
@Entity
@Table(name = "AdmWebCultures")
public class AdmWebCultures {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;
}
