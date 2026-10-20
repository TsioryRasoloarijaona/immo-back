package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.dossier.SyntheseMontantDto;
import com.tsix.immocrm.dto.patrimoine.ArticleDossierDto;
import com.tsix.immocrm.dto.patrimoine.OrigineDossier;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.services.dossier.MontantsDossier;
import com.tsix.immocrm.services.dossier.liste.FiltresListeDossiers;
import com.tsix.immocrm.services.dossier.liste.SpecificationsListeDossiers;
import com.tsix.immocrm.services.dossier.liste.StatutsVisiblesListe;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospect.ProspectSubResourcesService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Onglet Dossiers d'un article : ventes (liste des dossiers, montants {@link MontantsDossier}), locations, syndics. */
@Service
@Transactional(readOnly = true)
public class ArticleDossiersService {

    /** OMRAN n'exclut aucun statut ({@code ComFolderView} sans filtre) : sortis compris. */
    private static final FiltresListeDossiers TOUS_STATUTS = new FiltresListeDossiers(null, null, null, null, null,
            null, null, null, null, true, null);

    // Sans entité RntFolder / SynFolder : lecture native, comme ComFolderRepository.
    private static final String HORS_VENTE = """
            SELECT 'LOCATION', CONVERT(varchar(36), f.PKey), f.Code, s.Description, s.Code,
                   CONVERT(varchar(36), f.CfgCustomerId)
            FROM RntFolder f LEFT JOIN StrStatus s ON s.PKey = f.StrStatusId WHERE f.StkItemId = :id
            UNION ALL
            SELECT 'SYNDIC', CONVERT(varchar(36), f.PKey), f.Code, s.Description, s.Code,
                   CONVERT(varchar(36), f.CfgCustomerId)
            FROM SynFolder f LEFT JOIN StrStatus s ON s.PKey = f.StrStatusId WHERE f.StkItemId = :id
            ORDER BY 1 ASC, 3 ASC""";

    private final ComFolderRepository folderRepository;
    private final StatutsVisiblesListe statutsVisibles;
    private final PerimetreCommercialService perimetreService;
    private final MontantsDossier montantsDossier;
    private final CfgTierRepository tierRepository;
    private final EntityManager entityManager;

    public ArticleDossiersService(ComFolderRepository folderRepository, StatutsVisiblesListe statutsVisibles,
                                  PerimetreCommercialService perimetreService, MontantsDossier montantsDossier,
                                  CfgTierRepository tierRepository, EntityManager entityManager) {
        this.folderRepository = folderRepository;
        this.statutsVisibles = statutsVisibles;
        this.perimetreService = perimetreService;
        this.montantsDossier = montantsDossier;
        this.tierRepository = tierRepository;
        this.entityManager = entityManager;
    }

    /** L'appelant a vérifié l'article (R4). */
    public List<ArticleDossierDto> dossiers(StkItem article) {
        List<ArticleDossierDto> lignes = new ArrayList<>(ventes(article.getId()));
        if (dansLePerimetre(article)) {
            lignes.addAll(locationsEtSyndics(article));
        }
        return lignes;
    }

    /** Article principal du dossier, ou ligne {@code ComFolderItem} (sous-article) ; périmètre et statuts de la liste. */
    private List<ArticleDossierDto> ventes(UUID articleId) {
        Specification<ComFolder> deLArticle = (root, query, cb) -> {
            Subquery<UUID> ligne = query.subquery(UUID.class);
            Root<ComFolderItem> item = ligne.from(ComFolderItem.class);
            ligne.select(item.get("id")).where(cb.equal(item.get("comFolder"), root),
                    cb.equal(item.get("stkItem").get("id"), articleId));
            return cb.or(cb.equal(root.get("stkItem").get("id"), articleId), cb.exists(ligne));
        };
        Specification<ComFolder> spec = SpecificationsListeDossiers.de(TOUS_STATUTS, statutsVisibles.pourUtilisateurCourant())
                .and(deLArticle)
                .and(perimetreService.filtreDossiers());
        return folderRepository.findAll(spec, Sort.by("code")).stream()
                .map(f -> ligne(OrigineDossier.VENTE, f.getId(), f.getCode(),
                        ProspectSubResourcesService.libelleStatut(f.getStrStatus()),
                        TierNomFormatter.nom(f.getCfgCustomer()), f.getStkItem(), montantsDossier.calculer(f).netAPayer()))
                .toList();
    }

    /**
     * Visibilité des dossiers de location et de syndic (sans entité) : par la tranche de l'article, un article sans
     * programme est visible de tous. Seule règle, partagée avec l'onglet Historique.
     */
    boolean dansLePerimetre(StkItem article) {
        UUID projet = Champs.de(article.getStkHierarchy(), CfgTranche::getCfgProject, CfgProject::getId);
        return perimetreService.perimetreCourant().map(ids -> projet == null || ids.contains(projet)).orElse(true);
    }

    @SuppressWarnings("unchecked")
    private List<ArticleDossierDto> locationsEtSyndics(StkItem article) {
        List<Object[]> lignes = entityManager.createNativeQuery(HORS_VENTE)
                .setParameter("id", article.getId())
                .getResultList();
        List<UUID> clientIds = lignes.stream().map(l -> uuid(l[5])).filter(Objects::nonNull).distinct().toList();
        Map<UUID, String> clients = tierRepository.findAllById(clientIds).stream()
                .collect(Collectors.toMap(CfgTier::getId, t -> Objects.toString(TierNomFormatter.nom(t), ""),
                        (a, b) -> a));
        Function<Object[], String> statut = l -> StringUtils.hasText((String) l[3]) ? (String) l[3] : (String) l[4];
        return lignes.stream()
                .map(l -> ligne(OrigineDossier.valueOf((String) l[0]), uuid(l[1]), (String) l[2], statut.apply(l),
                        clients.get(uuid(l[5])), article, null))
                .toList();
    }

    private static UUID uuid(Object valeur) {
        return valeur == null ? null : UUID.fromString(valeur.toString());
    }

    private static ArticleDossierDto ligne(OrigineDossier origine, UUID id, String code, String statut, String client,
                                           StkItem article, SyntheseMontantDto montants) {
        CfgTranche tranche = Champs.de(article, StkItem::getStkHierarchy);
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);
        return new ArticleDossierDto(origine, id, code, statut, client,
                Champs.de(article, StkItem::getCode),
                Champs.de(projet, CfgProject::getCfgCompany, CfgCompany::getDescription),
                Champs.de(tranche, CfgTranche::getCode),
                Champs.de(projet, CfgProject::getCode),
                Champs.de(article, StkItem::getStkItemCategory, StkItemCategory::getDescription),
                Champs.de(article, StkItem::getStkItemType, StkItemType::getDescription),
                Champs.de(article, StkItem::getStkPresentationType, StkPresentationType::getDescription),
                Champs.de(montants, SyntheseMontantDto::montant),
                Champs.de(montants, SyntheseMontantDto::regle),
                Champs.de(montants, SyntheseMontantDto::reste));
    }
}
