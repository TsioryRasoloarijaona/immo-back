package com.tsix.immocrm.entities.cfg;

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
 * dbo.CfgCompany — the legal entity a {@link CfgProject} (or a {@code StlSettlement}, see its
 * {@code cfgCompany} — "l'entité juridique sur laquelle le règlement a été saisi") is sold/settled
 * under (e.g. "ADMOD", "MAYLINE", "GHRIZA" — AYAD Developpement operates several). Referenced by
 * name in generated documents (Acte de réservation, Fiche de réservation, Reçu de règlement...) —
 * never hardcode a company name in a renderer, always source it via {@code cfgCompany}.
 * {@code Address}/{@code Address2}/{@code Address3}/{@code City}/{@code LegalForm}/
 * {@code SignatureCity} were added for the Reçu de règlement (modeled on
 * {@code instructions/templates/RECEIP/*.yes}'s {@code CfgCompanyAddress}/
 * {@code CfgComPanyViewLegalForm}/{@code CfgComPanySignatureCity}) — confirmed present on the real
 * table (39 columns total; only the subset actually consumed so far is mapped here).
 */
@Getter
@Setter
@Entity
@Table(name = "CfgCompany")
public class CfgCompany {
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
    @Column(name = "Address")
    private String address;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Address2")
    private String address2;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Address3")
    private String address3;

    @Size(max = 255)
    @Nationalized
    @Column(name = "City")
    private String city;

    /** Forme juridique (ex. "SARL", "SA") — {@code StlSettlementView.CfgComPanyViewLegalForm}. */
    @Size(max = 255)
    @Nationalized
    @Column(name = "LegalForm")
    private String legalForm;

    /** Ville utilisée pour "{Ville} le, {date}" en pied de document. */
    @Size(max = 255)
    @Nationalized
    @Column(name = "SignatureCity")
    private String signatureCity;

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
