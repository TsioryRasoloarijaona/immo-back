package com.tsix.immocrm.services.dashboard;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.dto.dashboard.DashboardDto;
import com.tsix.immocrm.dto.dashboard.EtapeCountDto;
import com.tsix.immocrm.dto.dashboard.RelanceDto;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.dossier.DossierService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospection.ProspectionService;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ComFolderRepository folderRepository;
    private final ComProspectionRepository prospectionRepository;
    private final PerimetreCommercialService perimetreService;
    private final DossierService dossierService;
    private final ProspectionService prospectionService;

    public DashboardService(ComFolderRepository folderRepository, ComProspectionRepository prospectionRepository,
                             PerimetreCommercialService perimetreService, DossierService dossierService,
                             ProspectionService prospectionService) {
        this.folderRepository = folderRepository;
        this.prospectionRepository = prospectionRepository;
        this.perimetreService = perimetreService;
        this.dossierService = dossierService;
        this.prospectionService = prospectionService;
    }

    /**
     * Pas de {@code Specification} ici : le périmètre s'applique <b>à chaque agrégat</b>, à la main
     * ({@code FEATURE-perimetre-commercial.md} §6.6). La tranche restreint les mêmes lignes aux ids de
     * {@code /api/dossiers?trancheId=}, sans calcul parallèle ({@code FEATURE-dashboard-filtre-tranche.md} R2/R3).
     */
    public DashboardDto get(@Nullable UUID trancheId) {
        Optional<List<UUID>> perimetre = perimetreService.perimetreCourant();
        List<Object[]> rows = perimetre
                .map(folderRepository::findDashboardProjectionDansPerimetre)
                .orElseGet(folderRepository::findDashboardProjection);
        if (trancheId != null) {
            Set<UUID> dossiersDeLaTranche = dossierService.idsDossiersDeTranche(trancheId);
            rows = rows.stream().filter(row -> dossiersDeLaTranche.contains((UUID) row[0])).toList();
        }
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
                // Jours au calendrier mural, comme avant : un Duration zoné perdrait un jour à la fin du Ramadan.
                long joursRetard = ChronoUnit.DAYS.between(dateEcheance, now);
                relancesEnRetard.add(new RelanceDto(id, nomClient(socialReason, firstName, lastName), dateEcheance, joursRetard));
            }
        }

        BigDecimal panierMoyen = nombreActifs > 0
                ? valeurPipeline.divide(BigDecimal.valueOf(nombreActifs), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<EtapeCountDto> dossiersParEtape = parEtape.entrySet().stream()
                .map(e -> new EtapeCountDto(e.getKey(), e.getValue()))
                .toList();

        long prospectionsOuvertes;
        long prospectionsTotal;
        if (trancheId != null) {
            prospectionsOuvertes = prospectionService.compterProspections(trancheId, Boolean.TRUE);
            prospectionsTotal = prospectionService.compterProspections(trancheId, null);
        } else {
            prospectionsOuvertes = perimetre
                    .map(prospectionRepository::countOpenExcludingPreProspectionsDansPerimetre)
                    .orElseGet(prospectionRepository::countOpenExcludingPreProspections);
            prospectionsTotal = perimetre
                    .map(prospectionRepository::countExcludingPreProspectionsDansPerimetre)
                    .orElseGet(prospectionRepository::countExcludingPreProspections);
        }

        return new DashboardDto(valeurPipeline, panierMoyen, dossiersParEtape, relancesEnRetard,
                prospectionsOuvertes, prospectionsTotal);
    }

    /** Une relance sans {@code ValidityDate} n'est jamais en retard ; si les deux le sont, la plus ancienne. */
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
