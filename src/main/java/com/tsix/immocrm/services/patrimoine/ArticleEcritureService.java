package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.CalculPrixArticle;
import com.tsix.immocrm.dto.patrimoine.AffectationSousArticleDto;
import com.tsix.immocrm.dto.patrimoine.ArticleDetailDto;
import com.tsix.immocrm.dto.patrimoine.ArticleEcritureDto;
import com.tsix.immocrm.dto.patrimoine.SousArticleEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.exceptions.ChampEnConflitException;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Écritures des articles et sous-articles (FEATURE-patrimoine-ecriture.md §5). Les colonnes d'affectation
 * ({@code ComFolderId}, {@code RntFolderId}, {@code SynFolderId}) ne sont que lues : elles appartiennent à Vente/Location.
 */
@Service
@Transactional(readOnly = true)
public class ArticleEcritureService {

    private static final String ARTICLE_INTROUVABLE = "Article introuvable: ";
    private static final String CHAMP_CODE = "code";
    private static final String CHAMP_DESCRIPTION = "description";
    private static final String CHAMP_IMMEUBLE = "immeubleId";
    private static final String LIBELLE_IMMEUBLE = "Immeuble";
    private static final String SOUS_ARTICLE_PAS_LIBRE = "Ce sous article n'est pas libre";

    private final StkItemRepository itemRepository;
    private final ComFolderRepository folderRepository;
    private final ReferencesArticle references;
    private final ArticleService articleService;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public ArticleEcritureService(StkItemRepository itemRepository, ComFolderRepository folderRepository,
                                  ReferencesArticle references, ArticleService articleService,
                                  UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.itemRepository = itemRepository;
        this.folderRepository = folderRepository;
        this.references = references;
        this.articleService = articleService;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    @Transactional
    public ArticleDetailDto create(ArticleEcritureDto demande) {
        StkItem item = nouvelArticle();
        CfgTranche tranche = references.tranche(ReglesSaisie.requis(demande.trancheId(), "trancheId", "Tranche"));
        item.setStkHierarchy(tranche);
        item.setCode(codeLibre(tranche, code(demande.code()), item.getId()));
        item.setDescription(description(demande.description()));
        item.setStkVocation(demande.vocationId() == null
                ? references.vocationParDefaut() : references.vocation(demande.vocationId()));
        appliquerClassementRequis(item, demande.categorieId(), demande.typeId(), demande.etageId(), false);
        item.setStkBuilding(references.immeuble(ReglesSaisie.requis(demande.immeubleId(), CHAMP_IMMEUBLE, LIBELLE_IMMEUBLE)));
        item.setStkKind(references.natureBatiment());
        saisirSurfacesEtPrix(item, demande);
        CalculPrixArticle.appliquerPrixDeVente(item, List.of());
        return enregistrerNouveau(item);
    }

    @Transactional
    public ArticleDetailDto createSousArticle(UUID parentId, SousArticleEcritureDto demande) {
        StkItem parent = article(parentId);
        verifierArticlePrincipal(parent);
        verifierLibreDeDossier(parent);
        StkItem sous = nouvelArticle();
        sous.setIsUnderItem(true);
        sous.setIsUnderItemOrigin(true);
        sous.setIsTitled(false);
        sous.setParent(parent);
        sous.setAlwaysAffected(Boolean.TRUE.equals(demande.toujoursAffecte()));
        // OMRAN : « même tranche, même vocation et même nature » que l'article.
        sous.setStkHierarchy(parent.getStkHierarchy());
        sous.setStkVocation(parent.getStkVocation());
        sous.setStkKind(parent.getStkKind() != null ? parent.getStkKind() : references.natureBatiment());
        sous.setCode(codeLibre(parent.getStkHierarchy(), code(demande.code()), sous.getId()));
        sous.setDescription(description(demande.description()));
        appliquerClassementRequis(sous, demande.categorieId(), demande.typeId(), demande.etageId(), true);
        sous.setStkBuilding(demande.immeubleId() != null ? references.immeuble(demande.immeubleId())
                : ReglesSaisie.requis(parent.getStkBuilding(), CHAMP_IMMEUBLE, LIBELLE_IMMEUBLE));
        saisirSurfacesEtPrix(sous, versArticle(demande));
        CalculPrixArticle.appliquerPrixDeVente(sous, List.of());
        ArticleDetailDto cree = enregistrerNouveau(sous);
        recalculerPrixDeVente(parent);
        return cree;
    }

    @Transactional
    public ArticleDetailDto update(UUID id, ArticleEcritureDto demande) {
        StkItem item = article(id);
        UUID trancheActuelle = Champs.de(item.getStkHierarchy(), CfgTranche::getId);
        if (demande.trancheId() != null && !demande.trancheId().equals(trancheActuelle)) {
            throw new ChampInvalideException("trancheId", "La tranche d'un article ne se modifie pas.");
        }
        if (ArticleService.estOccupe(item)) {
            SaisieArticle.champProtegeModifie(item, demande).ifPresent(champ -> {
                throw new ChampEnConflitException(champ, messageOccupe(item));
            });
        }
        appliquerClassement(item, demande);
        SaisieArticle.EntreesPrix avant = SaisieArticle.EntreesPrix.de(item);
        SaisieArticle.appliquer(item, demande);
        SaisieArticle.verifierSurfaces(item);
        tracerModification(item);
        itemRepository.save(item);
        // Prix stockés réécrits seulement si leurs entrées changent : un article affecté garde ses prix tels quels.
        if (avant.differeDe(SaisieArticle.EntreesPrix.de(item))) {
            CalculPrixArticle.appliquerPrixArticle(item);
            recalculerPrixDeVente(item);
            if (item.getParent() != null) {
                recalculerPrixDeVente(item.getParent());
            }
        }
        return articleService.findById(id);
    }

    @Transactional
    public ArticleDetailDto affecter(UUID parentId, UUID sousArticleId, AffectationSousArticleDto demande) {
        StkItem parent = article(parentId);
        StkItem sous = article(sousArticleId);
        verifierArticlePrincipal(parent);
        verifierLibreDeDossier(parent);
        if (!memeTranche(parent, sous)) {
            throw new ConflictException(SOUS_ARTICLE_PAS_LIBRE + " : il n'est pas dans la tranche de l'article.");
        }
        boolean libre = Boolean.TRUE.equals(sous.getIsUnderItem()) && sous.getParent() == null
                && !Boolean.TRUE.equals(sous.getAlwaysAffected()) && !ArticleService.estOccupe(sous);
        if (!libre) {
            throw new ConflictException(SOUS_ARTICLE_PAS_LIBRE + ".");
        }
        sous.setParent(parent);
        sous.setAlwaysAffected(demande != null && Boolean.TRUE.equals(demande.toujoursAffecte()));
        tracerModification(sous);
        itemRepository.save(sous);
        recalculerPrixDeVente(parent);
        return articleService.findById(parentId);
    }

    @Transactional
    public ArticleDetailDto desaffecter(UUID parentId, UUID sousArticleId) {
        StkItem parent = article(parentId);
        StkItem sous = article(sousArticleId);
        if (sous.getParent() == null || !sous.getParent().getId().equals(parentId)) {
            throw new ConflictException("Ce sous article n'est pas rattaché à l'article %s.".formatted(parent.getCode()));
        }
        // Messages d'OMRAN (BuildItem.CancelAffectUnderItem).
        if (sous.getComFolderId() != null) {
            throw new ConflictException("Vous ne pouvez pas désaffecter un sous article ayant un dossier de vente !");
        }
        if (sous.getRntFolderId() != null) {
            throw new ConflictException("Vous ne pouvez pas désaffecter un sous article ayant un dossier de location !");
        }
        verifierLibreDeDossier(parent);
        sous.setAlwaysAffected(false);
        sous.setParent(null);
        tracerModification(sous);
        itemRepository.save(sous);
        recalculerPrixDeVente(parent);
        return articleService.findById(parentId);
    }

    private StkItem article(UUID id) {
        return itemRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ARTICLE_INTROUVABLE + id));
    }

    private StkItem nouvelArticle() {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        SaisieArticle.initialiserDefauts(item);
        item.setStrEntity(references.typeDocumentArticle());
        return item;
    }

    /** Surface définitive absente = surface provisoire (OMRAN : les deux valent 1 par défaut). */
    private static void saisirSurfacesEtPrix(StkItem item, ArticleEcritureDto demande) {
        SaisieArticle.appliquer(item, demande);
        if (demande.surfaceDefinitive() == null) {
            item.setArea(item.getAreaFloor());
        }
        SaisieArticle.verifierSurfaces(item);
        CalculPrixArticle.appliquerPrixArticle(item);
    }

    private ArticleDetailDto enregistrerNouveau(StkItem item) {
        UtilisateurAffiche.Auteur auteur = utilisateurAffiche.courant();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        item.setUserCreate(auteur.nom());
        item.setUserCreateDateTime(maintenant);
        item.setUserUpdate(auteur.nom());
        item.setUserUpdateDateTime(maintenant);
        itemRepository.save(item);
        return articleService.findById(item.getId());
    }

    private void tracerModification(StkItem item) {
        item.setUserUpdate(utilisateurAffiche.courant().nom());
        item.setUserUpdateDateTime(LocalDateTime.now(clock));
    }

    private void appliquerClassement(StkItem item, ArticleEcritureDto demande) {
        boolean sousArticle = Boolean.TRUE.equals(item.getIsUnderItem());
        if (demande.code() != null) {
            item.setCode(codeLibre(item.getStkHierarchy(), code(demande.code()), item.getId()));
        }
        if (demande.description() != null) {
            item.setDescription(description(demande.description()));
        }
        if (demande.vocationId() != null) {
            UUID actuelle = Champs.de(item.getStkVocation(), StkVocation::getId);
            if (sousArticle && !demande.vocationId().equals(actuelle)) {
                throw new ChampInvalideException("vocationId", "La vocation d'un sous-article est celle de son article.");
            }
            item.setStkVocation(references.vocation(demande.vocationId()));
        }
        if (demande.categorieId() != null) {
            item.setStkItemCategory(references.categorie(demande.categorieId(), sousArticle));
        }
        if (demande.typeId() != null) {
            item.setStkItemType(references.type(demande.typeId(), sousArticle));
        }
        if (demande.immeubleId() != null) {
            item.setStkBuilding(references.immeuble(demande.immeubleId()));
        }
        if (demande.etageId() != null) {
            item.setStkFloor(references.etage(demande.etageId()));
        }
    }

    private static String code(String saisi) {
        return ReglesSaisie.requis(saisi, CHAMP_CODE, "Code");
    }

    private static String description(String saisie) {
        return ReglesSaisie.requis(saisie, CHAMP_DESCRIPTION, "Description");
    }

    /** Catégorie, type et étage obligatoires à la création ; listes « Pour les sous articles » ou non. */
    private void appliquerClassementRequis(StkItem item, UUID categorieId, UUID typeId, UUID etageId, boolean sousArticle) {
        item.setStkItemCategory(references.categorie(ReglesSaisie.requis(categorieId, "categorieId", "Catégorie"), sousArticle));
        item.setStkItemType(references.type(ReglesSaisie.requis(typeId, "typeId", "Type"), sousArticle));
        item.setStkFloor(references.etage(ReglesSaisie.requis(etageId, "etageId", "Étage")));
    }

    /** OMRAN réenregistre le parent : son prix de vente reprend ses sous-articles rattachés (lus après vidage). */
    private void recalculerPrixDeVente(StkItem article) {
        CalculPrixArticle.appliquerPrixDeVente(article, itemRepository.findByParentId(article.getId()));
        tracerModification(article);
        itemRepository.save(article);
    }

    private String codeLibre(CfgTranche tranche, String code, UUID id) {
        itemRepository.findFirstByStkHierarchyIdAndCodeAndIdNot(tranche.getId(), code, id).ifPresent(existant -> {
            throw new ChampEnConflitException(CHAMP_CODE, "Le code « %s » est déjà utilisé dans la tranche par l'article « %s »."
                    .formatted(existant.getCode(), existant.getDescription()));
        });
        return code;
    }

    private static void verifierArticlePrincipal(StkItem parent) {
        if (Boolean.TRUE.equals(parent.getIsUnderItem()) || parent.getParent() != null) {
            throw new InvalidRequestException("Un sous-article se rattache à un article principal, pas à un sous-article.");
        }
    }

    private void verifierLibreDeDossier(StkItem article) {
        if (ArticleService.estOccupe(article)) {
            throw new ConflictException(messageOccupe(article));
        }
    }

    private String messageOccupe(StkItem item) {
        StkItem porteur = item.getComFolderId() == null && item.getRntFolderId() == null && item.getParent() != null
                ? item.getParent() : item;
        String dossier = porteur.getComFolderId() != null
                ? folderRepository.findById(porteur.getComFolderId()).map(ComFolder::getCode).orElse("de vente")
                : "de location";
        return "L'article %s est affecté au dossier %s : ses prix, surfaces et classification ne se modifient plus."
                .formatted(item.getCode(), dossier);
    }

    private static boolean memeTranche(StkItem a, StkItem b) {
        UUID ta = Champs.de(a.getStkHierarchy(), CfgTranche::getId);
        return ta != null && ta.equals(Champs.de(b.getStkHierarchy(), CfgTranche::getId));
    }

    private static ArticleEcritureDto versArticle(SousArticleEcritureDto s) {
        return new ArticleEcritureDto(null, s.code(), s.description(), null, s.categorieId(), s.typeId(),
                s.immeubleId(), s.etageId(), s.surfaceProvisoire(), s.surfaceDefinitive(), s.prixM2ProvisoireHT(),
                s.prixM2DefinitifHT(), s.tauxTva(), s.titreFoncier(), s.estTitre(), s.remiseMax1(), s.remiseMax2(),
                s.adresse(), s.notes(), s.surfaceInterieure(), s.surfaceMurs(), s.surfacePartiesCommunes());
    }
}
