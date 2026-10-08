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
 * Propositions pour « Réglé par » : acquéreur principal et co-acquéreurs, comme
 * {@link AutorisationVersementService}. <b>Pas une liste fermée</b> : un payeur réel peut être un
 * tiers extérieur, la création ne vérifie que l'existence du {@code CfgTier}.
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
            // Un tiers à la fois CfgCustomerId et ComFolderCustomer n'apparaît qu'une fois, en principal.
            if (tier != null && vus.add(tier.getId())) {
                payeurs.add(toDto(tier, CO_ACQUEREUR, coAcquereur.getPartPercent()));
            }
        }
        return payeurs;
    }

    private static PayeurDto toDto(CfgTier tier, String role, BigDecimal quotePart) {
        return new PayeurDto(tier.getId(), TierNomFormatter.nom(tier), role, pieceIdentite(tier), quotePart);
    }

    /** Même format que {@code AutorisationVersementService}, pour que liste et document concordent. */
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
