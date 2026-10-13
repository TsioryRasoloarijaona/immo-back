package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.TypeTiersProspect;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Type client / prospect des tiers d'un dossier, recalculé après chaque création ou changement de statut
 * comme {@code Sale.cs:ManageTier} : client tant qu'il a un dossier actif à {@code BoolField011} (comme
 * client principal ou acquéreur), prospect sinon ; {@code CfgTierKindId} remis à NULL dans les deux sens.
 */
@Component
public class TransformationProspectClient {

    public static final String MENTION_HISTORIQUE = "Prospect transformé en client";
    public static final String MENTION_RETOUR_PROSPECT = "Retour prospect";

    private final CfgTierTypeRepository tierTypeRepository;
    private final CfgTierRepository tierRepository;
    private final ComFolderRepository folderRepository;
    private final ComFolderCustomerRepository folderCustomerRepository;

    public TransformationProspectClient(CfgTierTypeRepository tierTypeRepository, CfgTierRepository tierRepository,
                                        ComFolderRepository folderRepository,
                                        ComFolderCustomerRepository folderCustomerRepository) {
        this.tierTypeRepository = tierTypeRepository;
        this.tierRepository = tierRepository;
        this.folderRepository = folderRepository;
        this.folderCustomerRepository = folderCustomerRepository;
    }

    /** Mentions d'historique à écrire sur le dossier, une par tiers dont le type a changé. */
    public List<String> synchroniser(ComFolder folder) {
        Map<UUID, CfgTier> tiers = new LinkedHashMap<>();
        if (folder.getCfgCustomer() != null) {
            tiers.put(folder.getCfgCustomer().getId(), folder.getCfgCustomer());
        }
        folderCustomerRepository.findByComFolderId(folder.getId()).stream()
                .map(ComFolderCustomer::getCfgTier)
                .filter(Objects::nonNull)
                .forEach(acquereur -> tiers.putIfAbsent(acquereur.getId(), acquereur));

        UUID principalId = folder.getCfgCustomer() != null ? folder.getCfgCustomer().getId() : null;
        List<String> mentions = new ArrayList<>();
        for (CfgTier tier : tiers.values()) {
            boolean principal = tier.getId().equals(principalId);
            synchroniser(tier).map(mention -> principal ? mention : mention + " : " + TierNomFormatter.nom(tier))
                    .ifPresent(mentions::add);
        }
        return mentions;
    }

    private Optional<String> synchroniser(CfgTier tier) {
        if (tier.getCfgTierType() == null) {
            return Optional.empty();
        }
        String code = tier.getCfgTierType().getCode();
        boolean actif = folderRepository.aUnDossierActifClient(tier.getId())
                || folderRepository.compterLocationsActives(tier.getId()) > 0;
        Function<String, Optional<String>> cible = actif
                ? TypeTiersProspect::codeClientCorrespondant
                : TypeTiersProspect::codeProspectCorrespondant;
        Optional<CfgTierType> nouveauType = cible.apply(code).flatMap(tierTypeRepository::findByCode);
        if (nouveauType.isEmpty()) {
            return Optional.empty();
        }
        tier.setCfgTierType(nouveauType.get());
        tier.setCfgTierKind(null);
        tierRepository.save(tier);
        return Optional.of(actif ? MENTION_HISTORIQUE : MENTION_RETOUR_PROSPECT);
    }
}
