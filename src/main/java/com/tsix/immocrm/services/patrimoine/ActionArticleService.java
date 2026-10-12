package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.prospection.CreerInteractionRequest;
import com.tsix.immocrm.dto.suivi.SuiviActionDto;
import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmEventRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageSubCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.prospection.AlerteRelanceService;
import com.tsix.immocrm.services.prospection.ContexteInteraction;
import com.tsix.immocrm.services.prospection.ReglesInteraction;
import com.tsix.immocrm.services.suivi.SuiviActionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Panneau « Actions » (GEN0005) d'une fiche article : le même {@code ComActionMessage} que les interactions de
 * prospection, rattaché par {@code StkItemId} seul (FEATURE-patrimoine-audit-actions.md §3).
 */
@Service
@Transactional(readOnly = true)
public class ActionArticleService {

    private static final String ARTICLE_INTROUVABLE = "Article introuvable: ";
    private static final DateTimeFormatter FORMAT_LIBELLE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StkItemRepository articleRepository;
    private final ComActionMessageRepository messageRepository;
    private final ComActionMessageTypeRepository typeRepository;
    private final ComActionMessageSubCategoryRepository resultatRepository;
    private final ComActionMessageCategoryRepository categorieRepository;
    private final AdmEventRepository evenementRepository;
    private final AlerteRelanceService alerteRelanceService;
    private final SuiviActionService suiviActionService;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    @SuppressWarnings("java:S107") // un dépôt par référentiel lu, comme ProspectionService
    public ActionArticleService(StkItemRepository articleRepository, ComActionMessageRepository messageRepository,
                                ComActionMessageTypeRepository typeRepository,
                                ComActionMessageSubCategoryRepository resultatRepository,
                                ComActionMessageCategoryRepository categorieRepository,
                                AdmEventRepository evenementRepository, AlerteRelanceService alerteRelanceService,
                                SuiviActionService suiviActionService, UtilisateurAffiche utilisateurAffiche,
                                Clock clock) {
        this.articleRepository = articleRepository;
        this.messageRepository = messageRepository;
        this.typeRepository = typeRepository;
        this.resultatRepository = resultatRepository;
        this.categorieRepository = categorieRepository;
        this.evenementRepository = evenementRepository;
        this.alerteRelanceService = alerteRelanceService;
        this.suiviActionService = suiviActionService;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    public Page<SuiviActionDto> lister(UUID articleId, Pageable pageable) {
        if (!articleRepository.existsById(articleId)) {
            throw new ResourceNotFoundException(ARTICLE_INTROUVABLE + articleId);
        }
        return suiviActionService.parArticle(articleId, pageable);
    }

    @Transactional
    public SuiviActionDto creer(UUID articleId, CreerInteractionRequest demande) {
        StkItem article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException(ARTICLE_INTROUVABLE + articleId));
        ComActionMessageType type = demande.actionTypeId() == null ? null
                : typeRepository.findById(demande.actionTypeId()).orElseThrow(() -> new InvalidRequestException(
                        "ComActionMessageType introuvable: " + demande.actionTypeId()));
        ComActionMessageSubCategory resultat = demande.resultatId() == null ? null
                : resultatRepository.findById(demande.resultatId()).orElseThrow(() -> new InvalidRequestException(
                        "ComActionMessageSubCategory introuvable: " + demande.resultatId()));
        ReglesInteraction.verifier(type, resultat, demande);

        ComActionMessage action = nouvelleAction(article, demande, type, resultat);
        if (ReglesInteraction.exigeUneDateDeRdv(resultat)) {
            AdmEvent evenement = new AdmEvent();
            evenement.setId(UUID.randomUUID());
            evenement.setSubject(demande.description());
            evenement.setStartHour(demande.dateRdv());
            evenement.setComActionMessageSubCategory(resultat);
            action.setAdmEvent(evenementRepository.save(evenement));
        }
        ComActionMessage enregistree = messageRepository.save(action);
        if (enregistree.getAlertValidityDate() != null) {
            alerteRelanceService.poserAlerteArticle(enregistree);
        }
        return suiviActionService.parId(enregistree.getId());
    }

    /** Colonnes de la ligne modèle de production (§3) ; seul {@code CfgTierId} reste vide : une fiche article n'a pas de tiers. */
    private ComActionMessage nouvelleAction(StkItem article, CreerInteractionRequest demande,
                                            ComActionMessageType type, ComActionMessageSubCategory resultat) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        UtilisateurAffiche.Auteur auteur = utilisateurAffiche.courant();
        AdmUser compte = auteur.compte();

        ComActionMessage action = new ComActionMessage();
        action.setId(UUID.randomUUID());
        action.setStkItem(article);
        action.setDocDate(maintenant);
        action.setDescription(StringUtils.hasText(auteur.nom())
                ? maintenant.format(FORMAT_LIBELLE) + " " + auteur.nom() : maintenant.format(FORMAT_LIBELLE));
        action.setAlertMessage(demande.description());
        action.setAlertValidityDate(demande.dateAlerte());
        action.setAdmUser(compte);
        action.setCfgEmployee(compte != null ? compte.getCfgEmployee() : null);
        action.setUserName(auteur.nom());
        action.setUserCreate(auteur.nom());
        action.setUserCreateDateTime(maintenant);
        action.setUserUpdate(auteur.nom());
        action.setUserUpdateDateTime(maintenant);
        action.setIsIn(demande.entrante());
        action.setIsSystem(false);
        action.setIsNotRead(false);
        action.setIsUnpaid(false);
        action.setWithoutRevival(false);
        action.setComActionMessageType(type);
        action.setComActionMessageSubCategory(resultat);
        // Celle du résultat, sinon celle de l'écran parent : Prospection, la seule des lignes article de production.
        action.setComActionMessageCategory(resultat != null && resultat.getComActionMessageCategory() != null
                ? resultat.getComActionMessageCategory()
                : categorieRepository.findById(ContexteInteraction.CATEGORIE_PROSPECTION).orElse(null));
        return action;
    }
}
