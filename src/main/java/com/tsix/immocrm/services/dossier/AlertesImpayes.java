package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.ActionsDossier;
import com.tsix.immocrm.entities.adm.AdmAlertData;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.repositories.adm.AdmAlertDataRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.dossier.workflow.AlertesStatutDossier;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Alerte d'impayé d'une échéance ({@code ActionMessage.cs:ManageAlertForComDocument}, aide GEN0005 §4.3), après chaque
 * écriture d'échéance ou de règlement : action système créée ou remise à jour tant qu'il reste à payer, alerte éteinte
 * sinon. Toute échéance, sans filtre de catégorie ni de date. Voir FEATURE-dossier-durcissement.md §3.
 */
@Component
public class AlertesImpayes {

    private final ComActionMessageRepository messageRepository;
    private final ComActionMessageTypeRepository typeRepository;
    private final ComActionMessageCategoryRepository categoryRepository;
    private final AdmAlertDataRepository alerteRepository;
    private final StlSettlementLineRepository settlementLineRepository;
    private final EntityManager entityManager;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    /** Dépendances regroupées (S107). */
    @Component
    public record Depots(ComActionMessageRepository messages, ComActionMessageTypeRepository types,
                         ComActionMessageCategoryRepository categories, AdmAlertDataRepository alertes,
                         StlSettlementLineRepository reglements) {
    }

    public AlertesImpayes(Depots depots, EntityManager entityManager, UtilisateurAffiche utilisateurAffiche,
                          Clock clock) {
        this.messageRepository = depots.messages();
        this.typeRepository = depots.types();
        this.categoryRepository = depots.categories();
        this.alerteRepository = depots.alertes();
        this.settlementLineRepository = depots.reglements();
        this.entityManager = entityManager;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    public void synchroniser(Collection<ComDocument> echeances) {
        echeances.forEach(this::synchroniser);
    }

    public void synchroniser(ComDocument echeance) {
        if (echeance == null || echeance.getComFolder() == null) {
            return;
        }
        BigDecimal regle = settlementLineRepository.sumAmountByComDocumentId(echeance.getId());
        BigDecimal reste = nonNul(echeance.getAmount()).subtract(nonNul(regle));
        List<ComActionMessage> actions = entityManager.createQuery(
                        "SELECT m FROM ComActionMessage m WHERE m.comDocument.id = :id ORDER BY m.docDate ASC",
                        ComActionMessage.class)
                .setParameter("id", echeance.getId())
                .getResultList();
        if (reste.signum() > 0) {
            ComActionMessage action = actions.isEmpty() ? nouvelle(echeance) : actions.get(0);
            action.setUserName(AlertesStatutDossier.SYSTEME);
            action.setDocDate(LocalDateTime.now(clock));
            action.setAlertValidityDate(echeance.getDocDate());
            action.setAlertMessage(message(echeance.getComSettlementCategory(), reste));
            alerter(messageRepository.save(action));
        } else if (!actions.isEmpty()) {
            alertesDe(actions.get(0).getId()).forEach(a -> a.setActive(false));
        }
    }

    private ComActionMessage nouvelle(ComDocument echeance) {
        ComFolder dossier = echeance.getComFolder();
        ComActionMessage action = new ComActionMessage();
        action.setId(UUID.randomUUID());
        action.setComDocument(echeance);
        action.setComFolder(dossier);
        action.setComProspection(dossier.getComProspection());
        action.setCfgTier(dossier.getCfgCustomer());
        action.setStkItem(dossier.getStkItem());
        action.setStrFolderStatusId(dossier.getStrStatus() != null ? dossier.getStrStatus().getId() : null);
        action.setComActionMessageType(typeRepository.findById(AlertesStatutDossier.TYPE_NOTE).orElse(null));
        action.setComActionMessageCategory(
                categoryRepository.findById(ActionsDossier.CATEGORIE_RESERVATION).orElse(null));
        action.setIsUnpaid(true);
        action.setWithoutRevival(false);
        action.setIsSystem(true);
        action.setUserCreate(utilisateurAffiche.courant().nom());
        action.setUserCreateDateTime(LocalDateTime.now(clock));
        return action;
    }

    /** {@code YesSchedulerClient/Manager.cs:ManageAlert} : alerte créée ou réactivée, éteinte sans date d'alerte. */
    private void alerter(ComActionMessage action) {
        List<AdmAlertData> existantes = alertesDe(action.getId());
        if (action.getAlertValidityDate() == null || Boolean.TRUE.equals(action.getWithoutRevival())) {
            existantes.forEach(a -> a.setActive(false));
            return;
        }
        if (!existantes.isEmpty()) {
            existantes.forEach(a -> {
                a.setAlertDate(action.getAlertValidityDate());
                a.setMessage(action.getAlertMessage());
                a.setActive(true);
            });
            return;
        }
        alerteRepository.save(nouvelleAlerte(action));
    }

    /** Ligne {@code AdmAlertData} d'une action, comme {@code Manager.ManageAlert} la crée : active, groupée par catégorie. */
    public static AdmAlertData nouvelleAlerte(ComActionMessage action) {
        ComActionMessageCategory categorie = action.getComActionMessageCategory();
        AdmAlertData alerte = new AdmAlertData();
        UUID id = UUID.randomUUID();
        alerte.setId(id);
        alerte.setCode(id.toString().toLowerCase(Locale.ROOT));
        alerte.setDescription(AdmAlertData.DESCRIPTION_ALERTE);
        alerte.setMessage(action.getAlertMessage());
        alerte.setGroupping(categorie != null ? categorie.getCode() : null);
        alerte.setAlertDate(action.getAlertValidityDate());
        alerte.setRelatedKey(action.getId());
        alerte.setRightKey(categorie != null ? categorie.getId().toString().toLowerCase(Locale.ROOT) : null);
        alerte.setActive(true);
        return alerte;
    }

    /** Plusieurs alertes peuvent viser la même action (voir {@code LiensEcheance}). */
    private List<AdmAlertData> alertesDe(UUID actionId) {
        return entityManager.createQuery("SELECT a FROM AdmAlertData a WHERE a.relatedKey = :id", AdmAlertData.class)
                .setParameter("id", actionId)
                .getResultList();
    }

    /** « AVANCE (273 000.00 DH arrivé(e) à échéance) » : format monétaire relevé en production. */
    static String message(ComSettlementCategory categorie, BigDecimal reste) {
        String libelle = "";
        if (categorie != null) {
            libelle = StringUtils.hasText(categorie.getDescription()) ? categorie.getDescription() : categorie.getCode();
        }
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.ROOT);
        symboles.setGroupingSeparator(' ');
        symboles.setDecimalSeparator('.');
        String montant = new DecimalFormat("#,##0.00", symboles).format(reste.setScale(2, RoundingMode.HALF_EVEN));
        return libelle + " (" + montant + " DH arrivé(e) à échéance)";
    }

    private static BigDecimal nonNul(BigDecimal valeur) {
        return valeur != null ? valeur : BigDecimal.ZERO;
    }
}
