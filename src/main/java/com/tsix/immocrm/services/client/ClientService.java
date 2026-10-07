package com.tsix.immocrm.services.client;

import com.tsix.immocrm.dto.client.ClientListItemDto;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.services.perimetre.VisibiliteEntites;
import com.tsix.immocrm.services.tranche.AppartenanceTranche;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client = {@code CfgTier} référencé par au moins un {@code ComFolder.CfgCustomerId} : aucun drapeau ni
 * type « client » n'existe. Le détail réutilise {@code ProspectService} ; ici, la liste seulement.
 */
@Service
@Transactional(readOnly = true)
public class ClientService {

    private final CfgTierRepository tierRepository;
    private final ComFolderRepository folderRepository;
    private final ComDocumentRepository documentRepository;
    private final StlSettlementLineRepository settlementLineRepository;

    public ClientService(CfgTierRepository tierRepository, ComFolderRepository folderRepository,
                          ComDocumentRepository documentRepository,
                          StlSettlementLineRepository settlementLineRepository) {
        this.tierRepository = tierRepository;
        this.folderRepository = folderRepository;
        this.documentRepository = documentRepository;
        this.settlementLineRepository = settlementLineRepository;
    }

    /**
     * @param trancheId clients ayant au moins un dossier visible de la tranche ; leurs agrégats restent
     *                  calculés sur tous leurs dossiers visibles ({@code FEATURE-dashboard-filtre-tranche.md}).
     */
    public Page<ClientListItemDto> findAll(String search, @Nullable UUID trancheId, Pageable pageable) {
        Specification<CfgTier> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Visibilité par entité dans la sous-requête : un client dont tous les dossiers sont sur
            // une entité inactive disparaît (FEATURE-visibilite-transversale-entites.md R5).
            Subquery<UUID> clientsAvecDossier = query.subquery(UUID.class);
            Root<ComFolder> folderRoot = clientsAvecDossier.from(ComFolder.class);
            Join<ComFolder, StkItem> article = folderRoot.join("stkItem", JoinType.LEFT);
            Join<StkItem, CfgTranche> tranche = article.join("stkHierarchy", JoinType.LEFT);
            Join<CfgTranche, CfgProject> projet = tranche.join("cfgProject", JoinType.LEFT);
            List<Predicate> conditionsDossier = new ArrayList<>();
            conditionsDossier.add(cb.isNotNull(folderRoot.get("cfgCustomer")));
            conditionsDossier.add(VisibiliteEntites.depuisProjet(projet, cb));
            if (trancheId != null) {
                conditionsDossier.add(AppartenanceTranche.dossier(folderRoot, clientsAvecDossier, cb,
                        List.of(trancheId)));
            }
            clientsAvecDossier.select(folderRoot.get("cfgCustomer").get("id"))
                    .where(cb.and(conditionsDossier.toArray(new Predicate[0])));
            predicates.add(root.get("id").in(clientsAvecDossier));

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("socialReason")), pattern),
                        cb.like(cb.lower(root.get("eMail")), pattern)
                ));
            }
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CfgTier> page = tierRepository.findAll(spec, pageable);
        // Page vide : pas de requête d'agrégat sur un « IN () ».
        return page.isEmpty() ? page.map(t -> toDto(t, 0, BigDecimal.ZERO, BigDecimal.ZERO)) : enrichirAvecAgregats(page);
    }

    private Page<ClientListItemDto> enrichirAvecAgregats(Page<CfgTier> page) {
        List<UUID> clientIds = page.getContent().stream().map(CfgTier::getId).toList();

        Map<UUID, Integer> nbDossiersParClient = new HashMap<>();
        for (Object[] row : folderRepository.countGroupedByClientId(clientIds)) {
            nbDossiersParClient.put((UUID) row[0], ((Long) row[1]).intValue());
        }
        Map<UUID, BigDecimal> aPayerParClient = new HashMap<>();
        for (Object[] row : documentRepository.sumAmountGroupedByClientId(clientIds)) {
            aPayerParClient.put((UUID) row[0], (BigDecimal) row[1]);
        }
        Map<UUID, BigDecimal> regleParClient = new HashMap<>();
        for (Object[] row : settlementLineRepository.sumAmountGroupedByClientId(clientIds)) {
            regleParClient.put((UUID) row[0], (BigDecimal) row[1]);
        }

        return page.map(t -> toDto(t,
                nbDossiersParClient.getOrDefault(t.getId(), 0),
                aPayerParClient.getOrDefault(t.getId(), BigDecimal.ZERO),
                regleParClient.getOrDefault(t.getId(), BigDecimal.ZERO)));
    }

    private static ClientListItemDto toDto(CfgTier t, int nbDossiers, BigDecimal totalAPayer, BigDecimal totalRegle) {
        return new ClientListItemDto(
                t.getId(),
                t.getCode(),
                nomAffiche(t),
                t.getMobile(),
                t.getPhone(),
                t.getEMail(),
                t.getCity(),
                nbDossiers,
                totalAPayer,
                totalRegle,
                totalAPayer.subtract(totalRegle)
        );
    }

    private static String nomAffiche(CfgTier t) {
        if (StringUtils.hasText(t.getSocialReason())) {
            return t.getSocialReason();
        }
        String prenom = t.getFirstName() != null ? t.getFirstName() : "";
        String nom = t.getLastName() != null ? t.getLastName() : "";
        String full = (prenom + " " + nom).trim();
        return full.isEmpty() ? t.getCode() : full;
    }
}
