package com.tsix.immocrm.services.client;

import com.tsix.immocrm.dto.client.ClientListItemDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
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
 * {@code GET /api/clients}: distinct {@code CfgTier} referenced by at least one
 * {@code ComFolder.CfgCustomerId} — "client" here means "has bought", not a {@code CfgTierType}/
 * {@code CfgTierKind} filter (there's no dedicated "client" flag on {@code CfgTier}, unlike
 * {@code IsProspect}). {@code /api/clients/{id}} and {@code /api/clients/{id}/dossiers} reuse
 * {@code ProspectService}/{@code ProspectSubResourcesService} internally (same {@code CfgTier}
 * table — see {@code ClientController}), so this service only owns the list + its aggregates.
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

    public Page<ClientListItemDto> findAll(String search, Pageable pageable) {
        Specification<CfgTier> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Subquery<UUID> clientsAvecDossier = query.subquery(UUID.class);
            var folderRoot = clientsAvecDossier.from(ComFolder.class);
            clientsAvecDossier.select(folderRoot.get("cfgCustomer").get("id"))
                    .where(cb.isNotNull(folderRoot.get("cfgCustomer")));
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
            // Distinct: a client can have several dossiers, the IN-subquery above must not
            // duplicate rows if the JPA provider doesn't already distinct the subquery result.
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CfgTier> page = tierRepository.findAll(spec, pageable);
        // Avoid an "IN ()" query when the page is empty (invalid/unnecessary on some drivers).
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
