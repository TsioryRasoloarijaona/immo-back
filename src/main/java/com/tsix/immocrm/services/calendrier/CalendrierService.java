package com.tsix.immocrm.services.calendrier;

import com.tsix.immocrm.dto.calendrier.EvenementCalendrierDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CalendrierService {

    private final ComActionMessageRepository actionMessageRepository;
    private final PerimetreCommercialService perimetreService;

    public CalendrierService(ComActionMessageRepository actionMessageRepository,
                             PerimetreCommercialService perimetreService) {
        this.actionMessageRepository = actionMessageRepository;
        this.perimetreService = perimetreService;
    }

    /**
     * Visibilité par entité portée par la requête {@code findEvenementsVisibles} ; un compte agence passe par
     * {@code filtreInteractions} (FEATURE-agences-vente.md R-V8).
     */
    public List<EvenementCalendrierDto> evenements(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null) {
            throw new InvalidRequestException("debut et fin sont requis");
        }
        if (fin.isBefore(debut)) {
            throw new InvalidRequestException("fin doit être postérieure ou égale à debut");
        }
        List<ComActionMessage> evenements = perimetreService.vendeursAgenceCourante().isPresent()
                ? actionMessageRepository.findAll(perimetreService.filtreInteractions().and(entre(debut, fin)),
                        Sort.by(Sort.Direction.ASC, "admEvent.startHour"))
                : actionMessageRepository.findEvenementsVisibles(debut, fin);
        return evenements.stream()
                .map(CalendrierService::toEvenementDto)
                .toList();
    }

    private static Specification<ComActionMessage> entre(LocalDateTime debut, LocalDateTime fin) {
        return (root, query, cb) -> cb.between(root.join("admEvent").get("startHour"), debut, fin);
    }

    private static EvenementCalendrierDto toEvenementDto(ComActionMessage m) {
        var event = m.getAdmEvent();
        ComActionMessageSubCategory resultat = m.getComActionMessageSubCategory();
        ComProspection prospection = m.getComProspection();
        CfgTier prospect = prospection != null ? prospection.getCfgProspect() : m.getCfgTier();
        CfgTier commercial = m.getCfgEmployee();
        return new EvenementCalendrierDto(
                m.getId(),
                event.getSubject(),
                event.getStartHour(),
                event.getEndHour(),
                event.getLocation(),
                Champs.de(resultat, ComActionMessageSubCategory::getId),
                Champs.de(resultat, ComActionMessageSubCategory::getCode),
                Champs.de(resultat, ComActionMessageSubCategory::getDescription),
                Champs.de(prospection, ComProspection::getId),
                Champs.de(prospection, ComProspection::getCode),
                Champs.de(prospect, CfgTier::getId),
                ProspectionService.nomProspect(prospect, prospection),
                Champs.de(commercial, CfgTier::getId),
                TierNomFormatter.nom(commercial)
        );
    }
}
