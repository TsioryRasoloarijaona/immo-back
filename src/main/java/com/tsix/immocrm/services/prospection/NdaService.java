package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.dto.prospection.NdaData;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Gathers the data behind the "NDA AYAD PRIVATE" document (see {@code NdaPdfRenderer} for the
 * actual PDF rendering) — modeled on the legacy "NDA AYAD PRIVATE" template.
 */
@Service
@Transactional(readOnly = true)
public class NdaService {

    private final ComProspectionRepository prospectionRepository;

    public NdaService(ComProspectionRepository prospectionRepository) {
        this.prospectionRepository = prospectionRepository;
    }

    public NdaData collect(UUID prospectionId) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));

        CfgTier prospect = prospection.getCfgProspect();

        return new NdaData(
                prospect != null ? prospect.getLastName() : null,
                prospect != null ? prospect.getFirstName() : null,
                prospect != null && prospect.getAdmNationality() != null ? prospect.getAdmNationality().getDescription() : null,
                prospect != null && prospect.getAdmProfession() != null ? prospect.getAdmProfession().getDescription() : null,
                prospect != null && prospect.getBirthDate() != null ? prospect.getBirthDate().toLocalDate() : null,
                prospect != null ? prospect.getBirthPlace() : null,
                prospect != null && prospect.getAdmSexType() != null ? prospect.getAdmSexType().getCode() : null,
                prospect != null ? adresse(prospect) : null,
                prospect != null ? prospect.getZipCode() : null,
                prospect != null ? prospect.getCity() : null,
                prospect != null && prospect.getAdmCountry() != null ? prospect.getAdmCountry().getDescription() : null,
                prospect != null ? prospect.getMobile() : null,
                prospect != null ? prospect.getPhone1() : null,
                prospect != null ? prospect.getEMail() : null,
                LocalDate.now()
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
