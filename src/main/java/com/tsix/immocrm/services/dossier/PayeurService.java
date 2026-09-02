package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.PayeurDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * {@code GET /api/dossiers/{id}/payeurs} — les tiers susceptibles de régler une échéance de ce
 * dossier, pour alimenter le champ « Réglé par » ({@code CreerReglementRequest.payeurId}).
 *
 * <p>Existe parce que {@code payeurId} est devenu obligatoire à la création d'un règlement (voir
 * {@code FEATURE-reglement-statut-preuve.md}) alors qu'aucun endpoint ne permettait de le peupler :
 * {@code DossierDetailDto} n'expose que l'acquéreur principal, et les co-acquéreurs
 * ({@code ComFolderCustomer}) n'étaient lus que par les générateurs de PDF. C'est exactement la
 * même liste que celle assemblée par {@link AutorisationVersementService} pour le bloc
 * « Nom et prénom des clients » du document.
 *
 * <p><b>Ce n'est pas une liste fermée.</b> Sur 654 règlements réels portant un payeur, 651 sont
 * l'acquéreur principal, 1 est un co-acquéreur et <b>2 ne sont ni l'un ni l'autre</b> (tiers
 * payeur). La création n'impose donc <i>pas</i> que {@code payeurId} appartienne à cette liste —
 * elle vérifie seulement que le {@code CfgTier} existe. Cet endpoint alimente une liste déroulante,
 * il ne définit pas une contrainte d'intégrité ; le cas du vrai tiers payeur reste à traiter par une
 * recherche de tiers générale, non implémentée à ce jour.
 */
@Service
@Transactional(readOnly = true)
public class PayeurService {

    public static final String ACQUEREUR_PRINCIPAL = "ACQUEREUR_PRINCIPAL";
    public static final String CO_ACQUEREUR = "CO_ACQUEREUR";

    private final ComFolderRepository folderRepository;
    private final ComFolderCustomerRepository folderCustomerRepository;

    public PayeurService(ComFolderRepository folderRepository,
                         ComFolderCustomerRepository folderCustomerRepository) {
        this.folderRepository = folderRepository;
        this.folderCustomerRepository = folderCustomerRepository;
    }

    public List<PayeurDto> findByDossier(UUID dossierId) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));

        List<PayeurDto> payeurs = new ArrayList<>();
        Set<UUID> vus = new LinkedHashSet<>();

        if (folder.getCfgCustomer() != null) {
            payeurs.add(toDto(folder.getCfgCustomer(), ACQUEREUR_PRINCIPAL, null));
            vus.add(folder.getCfgCustomer().getId());
        }
        for (ComFolderCustomer coAcquereur : folderCustomerRepository.findByComFolderId(dossierId)) {
            CfgTier tier = coAcquereur.getCfgTier();
            // Un même tiers peut être à la fois CfgCustomerId et ligne ComFolderCustomer : on ne
            // le propose qu'une fois, avec son rôle le plus fort (acquéreur principal).
            if (tier != null && vus.add(tier.getId())) {
                payeurs.add(toDto(tier, CO_ACQUEREUR, coAcquereur.getPartPercent()));
            }
        }
        return payeurs;
    }

    private static PayeurDto toDto(CfgTier tier, String role, BigDecimal quotePart) {
        return new PayeurDto(tier.getId(), TierNomFormatter.nom(tier), role, pieceIdentite(tier), quotePart);
    }

    /** Même format que {@code AutorisationVersementService} ("{type}: {numéro}"), pour que la liste
     * déroulante et le document imprimé désignent la personne de la même façon. */
    private static String pieceIdentite(CfgTier tier) {
        String type = tier.getAdmIdentityDocumentType() != null ? tier.getAdmIdentityDocumentType().getCode() : null;
        if (type == null && tier.getIDNumber() == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (type != null) {
            sb.append(type);
        }
        if (tier.getIDNumber() != null) {
            if (!sb.isEmpty()) sb.append(": ");
            sb.append(tier.getIDNumber());
        }
        return sb.toString();
    }
}
