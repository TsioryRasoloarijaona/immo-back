package com.tsix.immocrm.services.calendrier;

import com.tsix.immocrm.dto.calendrier.EvenementCalendrierDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
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
     *
     * <p>Seuls les événements rattachés à une entité juridique en exploitation sont renvoyés
     * ({@code CfgCompany.IsActive}) ; ceux qu'aucune prospection ni aucun dossier ne rattache à une
     * entité restent affichés. Le prédicat est porté par la requête, pas par ce service — voir
     * {@code ComActionMessageRepository.findEvenementsVisibles}.
     */
    public List<EvenementCalendrierDto> evenements(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null) {
            throw new InvalidRequestException("debut et fin sont requis");
        }
        if (fin.isBefore(debut)) {
            throw new InvalidRequestException("fin doit être postérieure ou égale à debut");
        }
        return actionMessageRepository
                .findEvenementsVisibles(debut, fin)
                .stream()
                .map(CalendrierService::toEvenementDto)
                .toList();
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
                TierNomFormatter.nom(prospect),
                Champs.de(commercial, CfgTier::getId),
                TierNomFormatter.nom(commercial)
        );
    }
}
