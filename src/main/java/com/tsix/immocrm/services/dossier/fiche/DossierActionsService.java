package com.tsix.immocrm.services.dossier.fiche;

import com.tsix.immocrm.domain.ActionsDossier;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.dto.dossier.actions.ActionDossierDto;
import com.tsix.immocrm.dto.dossier.actions.ActionsDossierDto;
import com.tsix.immocrm.dto.dossier.actions.CompteurTypeActionDto;
import com.tsix.immocrm.dto.dossier.actions.CreerActionDossierRequest;
import com.tsix.immocrm.dto.dossier.actions.CreerInstructionRequest;
import com.tsix.immocrm.dto.dossier.actions.InstructionCreeeDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.mail.EnvoiEmail;
import com.tsix.immocrm.mail.MessageEmail;
import com.tsix.immocrm.repositories.com.ActionDossierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Actions du dossier (GEN0005) et instructions au commercial ({@code Sale.cs:instructionBtn_Click}).
 * Voir FEATURE-vente-fiche.md §B1 et §B2.
 */
@Service
public class DossierActionsService {

    private static final Logger log = LoggerFactory.getLogger(DossierActionsService.class);
    private static final String TEMPLATE_INSTRUCTION = "mail/instruction-dossier";
    private static final String SANS_TYPE = "Sans type";
    private static final String ROLE_ADMINISTRATEUR = "ROLE_Administrateur";
    /** Clé de regroupement des actions sans type, jamais exposée (le DTO porte alors {@code typeId = null}). */
    private static final UUID SANS_TYPE_ID = new UUID(0L, 0L);

    private final EcritureDossier ecriture;
    private final ActionDossierRepository actionRepository;
    private final ComActionMessageTypeRepository typeRepository;
    private final ComActionMessageCategoryRepository categoryRepository;
    private final EnvoiEmail envoiEmail;
    private final TransactionTemplate transactions;

    public DossierActionsService(EcritureDossier ecriture, ActionDossierRepository actionRepository,
                                 ComActionMessageTypeRepository typeRepository,
                                 ComActionMessageCategoryRepository categoryRepository,
                                 EnvoiEmail envoiEmail, PlatformTransactionManager transactionManager) {
        this.ecriture = ecriture;
        this.actionRepository = actionRepository;
        this.typeRepository = typeRepository;
        this.categoryRepository = categoryRepository;
        this.envoiEmail = envoiEmail;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public ActionsDossierDto lister(UUID dossierId) {
        ecriture.charger(dossierId);
        List<ComActionMessage> actions = actionRepository.actionsDuDossier(dossierId);
        List<CompteurTypeActionDto> parType = actions.stream()
                .collect(Collectors.groupingBy(m -> Objects.requireNonNullElse(
                        Champs.de(m.getComActionMessageType(), ComActionMessageType::getId), SANS_TYPE_ID),
                        LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> compteur(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(CompteurTypeActionDto::nombre).reversed())
                .toList();
        return new ActionsDossierDto(actions.stream().map(DossierActionsService::versDto).toList(), parType);
    }

    private static CompteurTypeActionDto compteur(UUID typeId, List<ComActionMessage> actions) {
        ComActionMessageType type = actions.get(0).getComActionMessageType();
        String libelle = type != null ? libelle(type) : SANS_TYPE;
        return new CompteurTypeActionDto(SANS_TYPE_ID.equals(typeId) ? null : typeId, libelle, actions.size());
    }

    @Transactional
    public ActionDossierDto creer(UUID dossierId, CreerActionDossierRequest request) {
        ComFolder dossier = ecriture.charger(dossierId);
        if (request == null || request.typeId() == null) {
            throw new InvalidRequestException("typeId est requis");
        }
        exigerNote(request.note());
        ComActionMessageType type = typeRepository.findById(request.typeId())
                .orElseThrow(() -> new InvalidRequestException("Type d'action introuvable: " + request.typeId()));
        if (ActionsDossier.TYPE_INSTRUCTION.equals(type.getId())) {
            throw new InvalidRequestException("Une instruction se crée par POST /api/dossiers/{id}/instructions");
        }
        UtilisateurAffiche.Auteur auteur = ecriture.auteur();
        AdmUser compte = auteur.compte();
        ComActionMessage action = nouvelleAction(dossier, type, request.note(), request.dateAlerte(),
                request.date() != null ? request.date() : ecriture.maintenant());
        action.setCfgEmployee(compte != null ? compte.getCfgEmployee() : null);
        action.setIsNotRead(false);
        ComActionMessage enregistree = actionRepository.save(action);
        ecriture.signer(dossier);
        return versDto(enregistree);
    }

    /**
     * Pas de {@code @Transactional} : l'écriture est bornée par {@link #transactions}, le mail part ensuite
     * et n'annule jamais l'instruction (EnvoiEmail ne lève pas).
     */
    public InstructionCreeeDto creerInstruction(UUID dossierId, CreerInstructionRequest request) {
        exigerNote(request != null ? request.note() : null);
        PreparationMail preparation = transactions.execute(statut -> enregistrerInstruction(dossierId, request));
        boolean envoye = envoyerMail(Objects.requireNonNull(preparation));
        return new InstructionCreeeDto(preparation.instruction(), envoye);
    }

    private PreparationMail enregistrerInstruction(UUID dossierId, CreerInstructionRequest request) {
        ComFolder dossier = ecriture.charger(dossierId);
        CfgTier commercial = dossier.getCfgCommercial();
        if (commercial == null) {
            throw new ConflictException("Ce dossier n'a pas de commercial : l'instruction n'a pas de destinataire.");
        }
        ComActionMessageType type = typeRepository.findById(ActionsDossier.TYPE_INSTRUCTION)
                .orElseThrow(() -> new ConflictException("Le type d'action « Instruction » est absent du référentiel."));
        ComActionMessage action = nouvelleAction(dossier, type, request.note(), request.dateAlerte(),
                ecriture.maintenant());
        action.setCfgEmployee(commercial);
        action.setIsNotRead(true);
        ComActionMessage enregistree = actionRepository.save(action);
        ecriture.signer(dossier);
        return new PreparationMail(versDto(enregistree), adresse(commercial), TierNomFormatter.nom(commercial),
                dossier.getCode(), TierNomFormatter.nom(dossier.getCfgCustomer()), action.getUserName());
    }

    /** Marquage explicite par le destinataire (le front l'appelle à l'ouverture de l'onglet Actions). */
    @Transactional
    public ActionDossierDto marquerInstructionLue(UUID dossierId, UUID instructionId) {
        ecriture.charger(dossierId);
        ComActionMessage instruction = actionRepository.findByIdAndComFolderId(instructionId, dossierId)
                .filter(m -> m.getComActionMessageType() != null
                        && ActionsDossier.TYPE_INSTRUCTION.equals(m.getComActionMessageType().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Instruction introuvable: " + instructionId));
        UUID destinataire = Champs.de(instruction.getCfgEmployee(), CfgTier::getId);
        if (!estAdministrateur() && (destinataire == null || !destinataire.equals(employeCourant()))) {
            throw new ForbiddenException("Seul le commercial destinataire peut marquer cette instruction comme lue.");
        }
        instruction.setIsNotRead(false);
        instruction.setUserUpdate(ecriture.auteur().nom());
        instruction.setUserUpdateDateTime(ecriture.maintenant());
        return versDto(actionRepository.save(instruction));
    }

    private ComActionMessage nouvelleAction(ComFolder dossier, ComActionMessageType type, String note,
                                            LocalDateTime dateAlerte, LocalDateTime date) {
        StrStatus statut = dossier.getStrStatus();
        StatutDossier statutDossier = statutConnu(statut);
        UtilisateurAffiche.Auteur auteur = ecriture.auteur();
        LocalDateTime maintenant = ecriture.maintenant();

        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setComFolder(dossier);
        m.setComProspection(dossier.getComProspection());
        m.setCfgTier(dossier.getCfgCustomer());
        m.setStkItem(dossier.getStkItem());
        m.setStrFolderStatusId(statut.getId());
        m.setComActionMessageType(type);
        m.setComActionMessageCategory(categorie(statutDossier));
        m.setDocDate(date);
        m.setAlertMessage(note.trim());
        m.setDescription(type.getDescription());
        m.setAlertValidityDate(dateAlerte);
        m.setAdmUser(auteur.compte());
        m.setUserName(auteur.nom());
        m.setUserCreate(auteur.nom());
        m.setUserCreateDateTime(maintenant);
        m.setIsIn(false);
        m.setIsSystem(false);
        m.setIsUnpaid(false);
        m.setWithoutRevival(dateAlerte == null);
        return m;
    }

    /** OMRAN n'ouvre les actions que sur un dossier qui a un statut ({@code Sale.cs:AttachNotes}). */
    private static StatutDossier statutConnu(StrStatus statut) {
        if (statut == null || statut.getCode() == null) {
            throw new ConflictException("Ce dossier n'a pas de statut : la catégorie de l'action est indéterminée.");
        }
        try {
            return StatutDossier.fromCode(statut.getCode());
        } catch (IllegalArgumentException e) {
            throw new ConflictException("Statut de dossier inconnu: " + statut.getCode());
        }
    }

    /** Catégorie absente du référentiel (base de test) : l'action reste enregistrable sans elle. */
    private ComActionMessageCategory categorie(StatutDossier statut) {
        return categoryRepository.findById(ActionsDossier.categoriePourStatut(statut)).orElse(null);
    }

    private static void exigerNote(String note) {
        if (!StringUtils.hasText(note)) {
            throw new InvalidRequestException("note est requise");
        }
    }

    private boolean envoyerMail(PreparationMail p) {
        if (p.email() == null) {
            log.info("Instruction {} : le commercial n'a pas d'adresse email, aucun mail.", p.instruction().id());
            return false;
        }
        if (!envoiEmail.estActif()) {
            return false;
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nomCommercial", p.nomCommercial());
        variables.put("codeDossier", p.codeDossier());
        variables.put("client", p.client());
        variables.put("instruction", p.instruction().note());
        variables.put("auteur", p.auteur());
        String sujet = "Instruction sur le dossier " + Objects.requireNonNullElse(p.codeDossier(), "");
        String texte = "Bonjour,\n\nUne instruction vous a été adressée sur le dossier %s :\n\n%s\n"
                .formatted(Objects.requireNonNullElse(p.codeDossier(), ""), p.instruction().note());
        envoiEmail.envoyer(new MessageEmail(p.email(), sujet.trim(), TEMPLATE_INSTRUCTION, variables, texte));
        return true;
    }

    private static String adresse(CfgTier commercial) {
        String email = commercial.getEMail();
        return email != null && email.trim().contains("@") ? email.trim() : null;
    }

    private static boolean estAdministrateur() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_ADMINISTRATEUR::equals);
    }

    private static UUID employeCourant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AuthenticatedUser user ? user.employeeId() : null;
    }

    static ActionDossierDto versDto(ComActionMessage m) {
        ComActionMessageType type = m.getComActionMessageType();
        boolean instruction = type != null && ActionsDossier.TYPE_INSTRUCTION.equals(type.getId());
        return new ActionDossierDto(
                m.getId(),
                m.getDocDate(),
                Champs.de(type, ComActionMessageType::getId),
                type != null ? libelle(type) : null,
                Champs.de(m.getComActionMessageCategory(), ComActionMessageCategory::getDescription),
                m.getAlertMessage(),
                StringUtils.hasText(m.getUserName()) ? m.getUserName() : m.getUserCreate(),
                m.getAlertValidityDate(),
                instruction,
                instruction ? Champs.de(m.getCfgEmployee(), CfgTier::getId) : null,
                instruction ? TierNomFormatter.nom(m.getCfgEmployee()) : null,
                instruction && Boolean.TRUE.equals(m.getIsNotRead()));
    }

    private static String libelle(ComActionMessageType type) {
        return StringUtils.hasText(type.getDescription()) ? type.getDescription() : type.getCode();
    }

    private record PreparationMail(ActionDossierDto instruction, String email, String nomCommercial,
                                   String codeDossier, String client, String auteur) {
    }
}
