package com.tsix.immocrm.services.calendrier;

import com.tsix.immocrm.dto.calendrier.EvenementCalendrierDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/** Backs {@code GET /api/calendrier/evenements} — the visite/rendez-vous calendar. */
@Service
@Transactional(readOnly = true)
public class CalendrierService {

    private final ComActionMessageRepository actionMessageRepository;

    public CalendrierService(ComActionMessageRepository actionMessageRepository) {
        this.actionMessageRepository = actionMessageRepository;
    }

    /**
     * {@code GET /api/calendrier/evenements?debut=...&fin=...}: every rendez-vous/visite
     * ({@code AdmEvent}) created via {@code POST /api/prospections/{id}/interactions} whose
     * start falls within [debut, fin], earliest first.
     */
    public List<EvenementCalendrierDto> evenements(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null) {
            throw new InvalidRequestException("debut et fin sont requis");
        }
        if (fin.isBefore(debut)) {
            throw new InvalidRequestException("fin doit être postérieure ou égale à debut");
        }
        return actionMessageRepository
                .findByAdmEventIsNotNullAndAdmEventStartHourBetweenOrderByAdmEventStartHourAsc(debut, fin)
                .stream()
                .map(CalendrierService::toEvenementDto)
                .toList();
    }

    private static EvenementCalendrierDto toEvenementDto(ComActionMessage m) {
        var event = m.getAdmEvent();
        var resultat = m.getComActionMessageSubCategory();
        var prospection = m.getComProspection();
        CfgTier prospect = prospection != null ? prospection.getCfgProspect() : m.getCfgTier();
        CfgTier commercial = m.getCfgEmployee();
        return new EvenementCalendrierDto(
                m.getId(),
                event.getSubject(),
                event.getStartHour(),
                event.getEndHour(),
                event.getLocation(),
                resultat != null ? resultat.getId() : null,
                resultat != null ? resultat.getCode() : null,
                resultat != null ? resultat.getDescription() : null,
                prospection != null ? prospection.getId() : null,
                prospection != null ? prospection.getCode() : null,
                prospect != null ? prospect.getId() : null,
                nomTier(prospect),
                commercial != null ? commercial.getId() : null,
                nomTier(commercial)
        );
    }

    private static String nomTier(CfgTier tier) {
        return com.tsix.immocrm.utils.TierNomFormatter.nom(tier);
    }
}
