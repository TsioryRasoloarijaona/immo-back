package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.ArticleDossierDto;
import com.tsix.immocrm.dto.patrimoine.ArticleHistoriqueDto;
import com.tsix.immocrm.dto.patrimoine.OrigineDossier;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrFolderStatusHistory;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.str.StrFolderStatusHistoryRepository;
import com.tsix.immocrm.services.dossier.AuteurStatutDossier;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospect.ProspectSubResourcesService;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Onglets de lecture de la fiche article ; chacun commence par la R4 de l'article (404). */
@Service
@Transactional(readOnly = true)
public class ArticleOngletsService {

    private static final String SYSTEME = "Système";

    // Équivalent table de ComTierTransfertOperationView, qui n'expose que les transferts légalisés.
    private static final String TRANSFERTS = """
            SELECT CONVERT(varchar(36), p.PKey), a.Code, n.Code
            FROM ComSaleWithdrawalProduct p
            LEFT JOIN StkItem a ON a.PKey = p.StkOldItemId
            LEFT JOIN StkItem n ON n.PKey = p.StkItemId
            WHERE (p.StkItemId = :id OR p.StkOldItemId = :id) AND p.IsLegalized = 1""";

    private final StkItemRepository itemRepository;
    private final ProspectionService prospectionService;
    private final ArticleDossiersService dossiersService;
    private final StrFolderStatusHistoryRepository historyRepository;
    private final PerimetreCommercialService perimetreService;
    private final AuteurStatutDossier auteurStatut;
    private final EntityManager entityManager;

    public ArticleOngletsService(StkItemRepository itemRepository, ProspectionService prospectionService,
                                 ArticleDossiersService dossiersService, StrFolderStatusHistoryRepository historyRepository,
                                 PerimetreCommercialService perimetreService, AuteurStatutDossier auteurStatut,
                                 EntityManager entityManager) {
        this.itemRepository = itemRepository;
        this.prospectionService = prospectionService;
        this.dossiersService = dossiersService;
        this.historyRepository = historyRepository;
        this.perimetreService = perimetreService;
        this.auteurStatut = auteurStatut;
        this.entityManager = entityManager;
    }

    public List<ProspectionListItemDto> prospections(UUID articleId) {
        return prospectionService.deLArticle(article(articleId).getId());
    }

    public List<ArticleDossierDto> dossiers(UUID articleId) {
        return dossiersService.dossiers(article(articleId));
    }

    /** Lignes de l'article et des transferts produit légalisés qui le visent ; dossier hors périmètre → masquée. */
    public List<ArticleHistoriqueDto> historique(UUID articleId) {
        UUID id = article(articleId).getId();
        Map<UUID, String> transferts = transferts(id);
        Specification<StrFolderStatusHistory> spec = Specification
                .<StrFolderStatusHistory>where((root, query, cb) -> {
                    Predicate deLArticle = cb.equal(root.get("stkItem").get("id"), id);
                    return transferts.isEmpty() ? deLArticle
                            : cb.or(deLArticle, root.get("comSaleWithdrawalProductId").in(transferts.keySet()));
                })
                .and(perimetreService.filtreHistoriqueStatuts());
        List<StrFolderStatusHistory> lignes = historyRepository.findAll(spec,
                Sort.by(Sort.Direction.DESC, "docDate").and(Sort.by("id")));
        Function<StrFolderStatusHistory, String> auteur = auteurStatut.resoudre(lignes);
        return lignes.stream()
                .map(h -> versDto(h, auteur.apply(h), transferts.get(h.getComSaleWithdrawalProductId())))
                .toList();
    }

    private StkItem article(UUID id) {
        return itemRepository.findVisible(id).orElseThrow(() -> new ResourceNotFoundException("Article introuvable: " + id));
    }

    /** Transfert légalisé → « TRANSFERT PRODUIT : ancien => nouveau », le préfixe de détail d'OMRAN. */
    @SuppressWarnings("unchecked")
    private Map<UUID, String> transferts(UUID articleId) {
        List<Object[]> lignes = entityManager.createNativeQuery(TRANSFERTS).setParameter("id", articleId).getResultList();
        return lignes.stream().collect(Collectors.toMap(l -> UUID.fromString(l[0].toString()),
                l -> "TRANSFERT PRODUIT : %s => %s".formatted(Objects.toString(l[1], ""), Objects.toString(l[2], "")),
                (a, b) -> a));
    }

    private static ArticleHistoriqueDto versDto(StrFolderStatusHistory h, String auteur, String prefixe) {
        ComFolder dossier = h.getComFolder();
        String utilisateur = auteur;
        if (utilisateur == null && dossier == null) {
            utilisateur = SYSTEME;
        }
        String detail = prefixe == null ? h.getDescription() : prefixe + Objects.toString(h.getDescription(), "");
        return new ArticleHistoriqueDto(h.getId(), h.getDocDate(), detail, utilisateur,
                ProspectSubResourcesService.libelleStatut(h.getStrFolderStatus()),
                Champs.de(dossier, ComFolder::getId), Champs.de(dossier, ComFolder::getCode),
                Champs.de(h.getStkItem(), StkItem::getCode), origine(h), TierNomFormatter.nom(h.getCfgTier()));
    }

    private static OrigineDossier origine(StrFolderStatusHistory h) {
        if (h.getComFolder() != null) {
            return OrigineDossier.VENTE;
        }
        if (h.getRntFolderId() != null) {
            return OrigineDossier.LOCATION;
        }
        return h.getSynFolderId() != null ? OrigineDossier.SYNDIC : null;
    }
}
