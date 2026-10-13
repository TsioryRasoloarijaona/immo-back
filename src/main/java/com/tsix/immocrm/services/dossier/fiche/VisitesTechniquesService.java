package com.tsix.immocrm.services.dossier.fiche;

import com.tsix.immocrm.dto.dossier.visites.CalendrierVisitesDto;
import com.tsix.immocrm.dto.dossier.visites.ProgrammerVisiteRequest;
import com.tsix.immocrm.dto.dossier.visites.VisiteProgrammeeDto;
import com.tsix.immocrm.dto.dossier.visites.VisiteTechniqueDto;
import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.mail.EnvoiEmail;
import com.tsix.immocrm.mail.MessageEmail;
import com.tsix.immocrm.repositories.adm.VisiteTechniqueRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Calendrier des visites techniques d'une tranche et programmation d'une visite pour un dossier (aide OMRAN,
 * « Calendrier », action Enregistrer). Voir FEATURE-vente-fiche.md §B4.
 */
@Service
public class VisitesTechniquesService {

    private static final Logger log = LoggerFactory.getLogger(VisitesTechniquesService.class);
    private static final String TEMPLATE = "mail/visite-technique";
    private static final int JOURS_PAR_DEFAUT = 60;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final EcritureDossier ecriture;
    private final VisiteTechniqueRepository visiteRepository;
    private final ComFolderRepository folderRepository;
    private final EnvoiEmail envoiEmail;
    private final TransactionTemplate transactions;

    public VisitesTechniquesService(EcritureDossier ecriture, VisiteTechniqueRepository visiteRepository,
                                    ComFolderRepository folderRepository, EnvoiEmail envoiEmail,
                                    PlatformTransactionManager transactionManager) {
        this.ecriture = ecriture;
        this.visiteRepository = visiteRepository;
        this.folderRepository = folderRepository;
        this.envoiEmail = envoiEmail;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /** Période absente : d'aujourd'hui à soixante jours. */
    @Transactional(readOnly = true)
    public CalendrierVisitesDto calendrier(UUID dossierId, LocalDateTime du, LocalDateTime au) {
        CfgTranche tranche = tranche(ecriture.charger(dossierId));
        LocalDateTime debut = du != null ? du : ecriture.maintenant().toLocalDate().atStartOfDay();
        LocalDateTime fin = au != null ? au : debut.plusDays(JOURS_PAR_DEFAUT);
        List<AdmEvent> visites = visiteRepository
                .findByCfgTrancheIdAndComFolderIdIsNotNullAndStartHourGreaterThanEqualAndStartHourLessThanOrderByStartHourAsc(
                        tranche.getId(), debut, fin);
        Map<UUID, String> codes = folderRepository.findAllById(visites.stream().map(AdmEvent::getComFolderId)
                        .distinct().toList()).stream()
                .collect(Collectors.toMap(ComFolder::getId, f -> Objects.requireNonNullElse(f.getCode(), ""),
                        (a, b) -> a, LinkedHashMap::new));
        return new CalendrierVisitesDto(
                tranche.getId(),
                StringUtils.hasText(tranche.getDescription()) ? tranche.getDescription() : tranche.getCode(),
                TierNomFormatter.nom(tranche.getCfgClaimsManager()),
                tranche.getInterventionMinimumDelai(),
                tranche.getMaxVisitNumber(),
                visites.stream().map(v -> versDto(v, codes::get)).toList());
    }

    /** Écriture bornée par {@link #transactions} ; le mail au S.A.V. part ensuite, sans jamais l'annuler. */
    public VisiteProgrammeeDto programmer(UUID dossierId, ProgrammerVisiteRequest request) {
        verifierCreneau(request);
        PreparationMail p = transactions.execute(statut -> enregistrer(dossierId, request));
        boolean envoye = envoyerMail(Objects.requireNonNull(p));
        return new VisiteProgrammeeDto(p.visite(), envoye);
    }

    private static void verifierCreneau(ProgrammerVisiteRequest request) {
        if (request == null || request.debut() == null || request.fin() == null) {
            throw new InvalidRequestException("debut et fin sont requis");
        }
        if (!request.fin().isAfter(request.debut())) {
            throw new InvalidRequestException("fin doit être postérieure à debut");
        }
    }

    private PreparationMail enregistrer(UUID dossierId, ProgrammerVisiteRequest request) {
        ComFolder dossier = ecriture.charger(dossierId);
        CfgTranche tranche = tranche(dossier);
        CfgTier sav = tranche.getCfgClaimsManager();
        if (sav == null) {
            throw new ConflictException("Veuillez affecter un responsable S.A.V à la tranche " + libelle(tranche));
        }
        verifierDelai(tranche, request.debut());
        verifierQuota(tranche, request.debut());

        AdmEvent visite = new AdmEvent();
        visite.setId(UUID.randomUUID());
        visite.setComFolderId(dossier.getId());
        visite.setCfgTrancheId(tranche.getId());
        visite.setSubject("Visite technique " + Objects.requireNonNullElse(dossier.getCode(), ""));
        visite.setStartHour(request.debut());
        visite.setEndHour(request.fin());
        visite.setNotes(StringUtils.hasText(request.note()) ? request.note().trim() : null);
        visite.setAllDay(false);
        visite.setCfgEmployeeId(sav.getId());
        visite.setUserCreate(ecriture.auteur().nom());
        visite.setUserCreateDateTime(ecriture.maintenant());
        AdmEvent enregistree = visiteRepository.save(visite);

        dossier.setReceptionDate(request.debut());
        ecriture.signer(dossier);
        return new PreparationMail(versDto(enregistree, id -> dossier.getCode()), adresse(sav),
                TierNomFormatter.nom(sav), TierNomFormatter.nom(dossier.getCfgCustomer()), libelle(tranche));
    }

    /** « Vous devez programmer la visite technique XX heures à l'avance » ; délai nul ou absent = libre. */
    private void verifierDelai(CfgTranche tranche, LocalDateTime debut) {
        Integer delai = tranche.getInterventionMinimumDelai();
        if (delai != null && delai > 0 && debut.isBefore(ecriture.maintenant().plusHours(delai))) {
            throw new ConflictException("Vous devez programmer la visite technique " + delai + " heures à l'avance");
        }
    }

    /** « Vous ne devez pas programmer plus que XX visites technique par jour » ; quota nul ou absent = libre. */
    private void verifierQuota(CfgTranche tranche, LocalDateTime debut) {
        Integer max = tranche.getMaxVisitNumber();
        if (max == null || max <= 0) {
            return;
        }
        LocalDateTime jour = debut.toLocalDate().atStartOfDay();
        long deja = visiteRepository.countByCfgTrancheIdAndComFolderIdIsNotNullAndStartHourGreaterThanEqualAndStartHourLessThan(
                tranche.getId(), jour, jour.plusDays(1));
        if (deja >= max) {
            throw new ConflictException("Vous ne devez pas programmer plus que " + max + " visites technique par jour");
        }
    }

    private static CfgTranche tranche(ComFolder dossier) {
        CfgTranche tranche = Champs.de(dossier.getStkItem(), StkItem::getStkHierarchy);
        if (tranche == null) {
            throw new ConflictException("Ce dossier n'a pas d'article rattaché à une tranche.");
        }
        return tranche;
    }

    private static String libelle(CfgTranche tranche) {
        return StringUtils.hasText(tranche.getDescription()) ? tranche.getDescription() : tranche.getCode();
    }

    private static String adresse(CfgTier tier) {
        String email = tier.getEMail();
        return email != null && email.trim().contains("@") ? email.trim() : null;
    }

    private boolean envoyerMail(PreparationMail p) {
        if (p.email() == null) {
            log.info("Visite technique {} : le responsable S.A.V. n'a pas d'email, aucun mail.", p.visite().id());
            return false;
        }
        if (!envoiEmail.estActif()) {
            return false;
        }
        String quand = p.visite().debut().format(FORMAT) + " – " + p.visite().fin().format(FORMAT);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nomSav", p.nomSav());
        variables.put("codeDossier", p.visite().dossierCode());
        variables.put("client", p.client());
        variables.put("tranche", p.tranche());
        variables.put("quand", quand);
        variables.put("note", p.visite().note());
        String texte = "Bonjour,\n\nUne visite technique est programmée sur le dossier %s (%s) : %s.\n"
                .formatted(p.visite().dossierCode(), p.tranche(), quand);
        envoiEmail.envoyer(new MessageEmail(p.email(), "Visite technique — " + p.visite().dossierCode(),
                TEMPLATE, variables, texte));
        return true;
    }

    private static VisiteTechniqueDto versDto(AdmEvent v, Function<UUID, String> codeDossier) {
        return new VisiteTechniqueDto(v.getId(), v.getComFolderId(), codeDossier.apply(v.getComFolderId()),
                v.getSubject(), v.getStartHour(), v.getEndHour(), v.getNotes());
    }

    private record PreparationMail(VisiteTechniqueDto visite, String email, String nomSav, String client,
                                   String tranche) {
    }
}
