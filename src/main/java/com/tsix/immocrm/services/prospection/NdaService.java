package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.dto.prospection.NdaData;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.adm.AdmNationality;
import com.tsix.immocrm.entities.adm.AdmProfession;
import com.tsix.immocrm.entities.adm.AdmSexType;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Gathers the data behind the "NDA AYAD PRIVATE" document (see {@code NdaPdfRenderer} for the
 * actual PDF rendering) — modeled on the legacy "NDA AYAD PRIVATE" template.
 */
@Service
@Transactional(readOnly = true)
public class NdaService {

    private final ComProspectionRepository prospectionRepository;
    private final Clock clock;

    public NdaService(ComProspectionRepository prospectionRepository, Clock clock) {
        this.prospectionRepository = prospectionRepository;
        this.clock = clock;
    }

    public NdaData collect(UUID prospectionId) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));

        CfgTier prospect = prospection.getCfgProspect();

        return new NdaData(
                Champs.de(prospect, CfgTier::getLastName),
                Champs.de(prospect, CfgTier::getFirstName),
                Champs.de(prospect, CfgTier::getAdmNationality, AdmNationality::getDescription),
                Champs.de(prospect, CfgTier::getAdmProfession, AdmProfession::getDescription),
                Champs.de(prospect, CfgTier::getBirthDate, LocalDateTime::toLocalDate),
                Champs.de(prospect, CfgTier::getBirthPlace),
                Champs.de(prospect, CfgTier::getAdmSexType, AdmSexType::getCode),
                prospect != null ? adresse(prospect) : null,
                Champs.de(prospect, CfgTier::getZipCode),
                Champs.de(prospect, CfgTier::getCity),
                Champs.de(prospect, CfgTier::getAdmCountry, AdmCountry::getDescription),
                Champs.de(prospect, CfgTier::getMobile),
                Champs.de(prospect, CfgTier::getPhone1),
                Champs.de(prospect, CfgTier::getEMail),
                LocalDate.now(clock)
        );
    }

    private static String adresse(CfgTier tier) {
        StringBuilder sb = new StringBuilder();
        for (String part : new String[]{tier.getAddress(), tier.getAddress2(), tier.getAddress3()}) {
            if (part != null && !part.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append(part.trim());
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
