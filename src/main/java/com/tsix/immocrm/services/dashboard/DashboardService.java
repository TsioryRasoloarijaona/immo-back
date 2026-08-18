package com.tsix.immocrm.services.dashboard;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.dto.dashboard.DashboardDto;
import com.tsix.immocrm.dto.dashboard.EtapeCountDto;
import com.tsix.immocrm.dto.dashboard.RelanceDto;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ComFolderRepository folderRepository;
    private final ComProspectionRepository prospectionRepository;

    public DashboardService(ComFolderRepository folderRepository, ComProspectionRepository prospectionRepository) {
        this.folderRepository = folderRepository;
        this.prospectionRepository = prospectionRepository;
    }

    public DashboardDto get() {
        List<Object[]> rows = folderRepository.findDashboardProjection();
        LocalDateTime now = LocalDateTime.now();

        Map<String, Long> parEtape = new LinkedHashMap<>();
        BigDecimal valeurPipeline = BigDecimal.ZERO;
        long nombreActifs = 0;
        List<RelanceDto> relancesEnRetard = new ArrayList<>();

        for (Object[] row : rows) {
            UUID id = (UUID) row[0];
            String statutCode = (String) row[1];
            BigDecimal totalToPay = (BigDecimal) row[2];
            LocalDateTime revival1 = (LocalDateTime) row[3];
            LocalDateTime revival1ValidityDate = (LocalDateTime) row[4];
            LocalDateTime revival2 = (LocalDateTime) row[5];
            LocalDateTime revival2ValidityDate = (LocalDateTime) row[6];
            String socialReason = (String) row[7];
            String firstName = (String) row[8];
            String lastName = (String) row[9];

            String etapeLabel = statutCode != null ? statutCode : "SANS_STATUT";
            parEtape.merge(etapeLabel, 1L, Long::sum);

            boolean estSortie = isSortie(statutCode);
            if (!estSortie) {
                valeurPipeline = valeurPipeline.add(totalToPay != null ? totalToPay : BigDecimal.ZERO);
                nombreActifs++;
            }

            LocalDateTime dateEcheance = earliestOverdue(revival1, revival1ValidityDate, revival2, revival2ValidityDate, now);
            if (dateEcheance != null) {
                long joursRetard = Duration.between(dateEcheance, now).toDays();
                relancesEnRetard.add(new RelanceDto(id, nomClient(socialReason, firstName, lastName), dateEcheance, joursRetard));
            }
        }

        BigDecimal panierMoyen = nombreActifs > 0
                ? valeurPipeline.divide(BigDecimal.valueOf(nombreActifs), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<EtapeCountDto> dossiersParEtape = parEtape.entrySet().stream()
                .map(e -> new EtapeCountDto(e.getKey(), e.getValue()))
                .toList();

        long prospectionsOuvertes = prospectionRepository.countOpenExcludingPreProspections();
        long prospectionsTotal = prospectionRepository.countExcludingPreProspections();

        return new DashboardDto(valeurPipeline, panierMoyen, dossiersParEtape, relancesEnRetard,
                prospectionsOuvertes, prospectionsTotal);
    }

    /**
     * Revival1 and Revival2 are each checked independently against their own ValidityDate;
     * when both are overdue the earliest ValidityDate (the longest-overdue one) is returned.
     * A revival with no ValidityDate set is never considered overdue.
     */
    private static LocalDateTime earliestOverdue(LocalDateTime revival1, LocalDateTime revival1ValidityDate,
                                                  LocalDateTime revival2, LocalDateTime revival2ValidityDate,
                                                  LocalDateTime now) {
        LocalDateTime overdue1 = (revival1 != null && revival1ValidityDate != null && revival1ValidityDate.isBefore(now))
                ? revival1ValidityDate : null;
        LocalDateTime overdue2 = (revival2 != null && revival2ValidityDate != null && revival2ValidityDate.isBefore(now))
                ? revival2ValidityDate : null;

        if (overdue1 != null && overdue2 != null) {
            return overdue1.isBefore(overdue2) ? overdue1 : overdue2;
        }
        return overdue1 != null ? overdue1 : overdue2;
    }

    private static String nomClient(String socialReason, String firstName, String lastName) {
        if (StringUtils.hasText(socialReason)) {
            return socialReason;
        }
        String prenom = firstName != null ? firstName : "";
        String nom = lastName != null ? lastName : "";
        String full = (prenom + " " + nom).trim();
        return full.isEmpty() ? null : full;
    }

    private static boolean isSortie(String statutCode) {
        if (statutCode == null) {
            return false;
        }
        try {
            return StatutDossier.fromCode(statutCode).isSortie();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
