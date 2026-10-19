package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.ResponsablesOmran;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "CfgProject")
public class CfgProject extends ResponsablesOmran {
    @Column(name = "WorkStartDate")
    private LocalDateTime workStartDate;

    @Column(name = "WorkEndDate")
    private LocalDateTime workEndDate;

    @Column(name = "MarketingStartDate")
    private LocalDateTime marketingStartDate;

    @Column(name = "MarketingEndDate")
    private LocalDateTime marketingEndDate;

    @Size(max = 500)
    @Nationalized
    @Column(name = "DescriptionLong", length = 500)
    private String descriptionLong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCompanyId")
    private CfgCompany cfgCompany;
}
