package com.tsix.immocrm.entities;

import com.tsix.immocrm.entities.adm.AdmCountry;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** {@link AuditOmran} plus les coordonnées communes à l'entité, au programme et à la tranche. */
@Getter
@Setter
@MappedSuperclass
public abstract class CoordonneesOmran extends AuditOmran {
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
    @Column(name = "ZipCode")
    private String zipCode;

    @Size(max = 255)
    @Nationalized
    @Column(name = "City")
    private String city;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Phone")
    private String phone;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Mobile")
    private String mobile;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Fax")
    private String fax;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Email")
    private String email;

    @Size(max = 255)
    @Nationalized
    @Column(name = "WebSite")
    private String webSite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmCountryId")
    private AdmCountry admCountry;
}
