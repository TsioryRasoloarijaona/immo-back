package com.tsix.immocrm.services.achat.config;

import com.tsix.immocrm.dto.achat.config.ArticleAchatDto;
import com.tsix.immocrm.dto.achat.config.ArticleAchatEcritureDto;
import com.tsix.immocrm.dto.achat.config.ArticlesAchatDto;
import com.tsix.immocrm.dto.achat.config.SousFamilleAchatDto;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.buy.BuyItem;
import com.tsix.immocrm.entities.buy.BuyItemFamily;
import com.tsix.immocrm.entities.buy.BuyItemUnderFamily;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.patrimoine.ReferentielAdminService;
import com.tsix.immocrm.services.patrimoine.SaisiePatrimoine;
import com.tsix.immocrm.services.patrimoine.SuppressionProjet;
import com.tsix.immocrm.services.patrimoine.TracePatrimoine;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** Écran Articles d'achat (FEATURE-n8-nomenclature.md R1–R4, R7, D3, D6) : droit OMRAN vérifié avant toute lecture. */
@Service
@Transactional(readOnly = true)
public class ArticlesAchatService {

    private static final String LISTE = "Yes.Buy.Front.Config.BuyItemList";
    private static final String NOUVEAU = LISTE + ".toolNew";
    private static final String SUPPRIMER = LISTE + ".toolDelete";
    private static final String EXPORTER = LISTE + ".toolExport";
    private static final String R3 = "La sous famille sélectionnée n'appartient pas à la famille sélectionnée !";
    private static final String ARTICLES = """
            SELECT a FROM BuyItem a LEFT JOIN FETCH a.stlCategory
            LEFT JOIN FETCH a.buyItemUnderFamily s LEFT JOIN FETCH s.buyItemFamily f
            WHERE :motif IS NULL OR LOWER(a.code) LIKE :motif OR LOWER(a.description) LIKE :motif
               OR LOWER(a.unity) LIKE :motif OR LOWER(s.code) LIKE :motif OR LOWER(f.code) LIKE :motif
            ORDER BY a.code ASC""";
    private static final String SOUS_FAMILLES = """
            SELECT s FROM BuyItemUnderFamily s LEFT JOIN FETCH s.buyItemFamily f ORDER BY f.code ASC, s.code ASC""";
    private static final String DOUBLON = "SELECT COUNT(a) FROM BuyItem a WHERE a.code = :code AND a.id <> :id";
    /** D3 : tables qui citent un article, SQL natif (aucune n'a d'entité ici). */
    private static final Map<String, String> CITANTES = Map.of(
            "SELECT COUNT(*) FROM BuyDocumentLine WHERE BuyItemId = :id", "ligne(s) de pièce d'achat",
            "SELECT COUNT(*) FROM PrjWorkingOutLine WHERE BuyItemId = :id", "ligne(s) de décompte",
            "SELECT COUNT(*) FROM BuySelectionLine WHERE BuyItemId = :id", "ligne(s) de sélection",
            "SELECT COUNT(*) FROM BuySelectionStockLine WHERE BuyItemId = :id", "ligne(s) de sélection de stock",
            "SELECT COUNT(*) FROM GrhWorkedDay WHERE BuyItemId = :id", "journée(s) travaillée(s)",
            "SELECT COUNT(*) FROM PrjMarketLine WHERE BuyItemId = :id", "ligne(s) de marché",
            "SELECT COUNT(*) FROM PrjMarketLineEstimation WHERE BuyItemId = :id", "ligne(s) d'estimation de marché");

    private final EntityManager em;
    private final DroitsOmran droits;
    private final TracePatrimoine trace;
    private final SuppressionProjet suppression;

    public ArticlesAchatService(EntityManager em, DroitsOmran droits, TracePatrimoine trace,
                                SuppressionProjet suppression) {
        this.em = em;
        this.droits = droits;
        this.trace = trace;
        this.suppression = suppression;
    }

    public ArticlesAchatDto lister(String search) {
        droits.exiger(LISTE);
        String motif = search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        List<ArticleAchatDto> articles = em.createQuery(ARTICLES, BuyItem.class).setParameter("motif", motif)
                .getResultList().stream().map(ArticlesAchatService::dto).toList();
        Set<String> permis = droits.parmi(NOUVEAU, SUPPRIMER, EXPORTER);
        return new ArticlesAchatDto(articles, permis.contains(NOUVEAU), permis.contains(SUPPRIMER),
                permis.contains(EXPORTER));
    }

    public List<SousFamilleAchatDto> sousFamilles() {
        droits.exiger(LISTE);
        return em.createQuery(SOUS_FAMILLES, BuyItemUnderFamily.class).getResultList().stream()
                .map(s -> {
                    BuyItemFamily f = s.getBuyItemFamily();
                    return new SousFamilleAchatDto(s.getId(), s.getCode(), s.getDescription(), id(f),
                            valeur(f, CodeLibelleOmran::getCode), valeur(f, CodeLibelleOmran::getDescription));
                }).toList();
    }

    public ArticleAchatDto lire(UUID id) {
        droits.exiger(LISTE);
        return dto(trouver(id));
    }

    @Transactional
    public ArticleAchatDto creer(ArticleAchatEcritureDto corps) {
        droits.exiger(LISTE, NOUVEAU);
        BuyItem article = new BuyItem();
        article.setId(UUID.randomUUID());
        appliquer(article, corps);
        em.persist(article);
        em.flush();
        return dto(article);
    }

    @Transactional
    public ArticleAchatDto modifier(UUID id, ArticleAchatEcritureDto corps) {
        droits.exiger(LISTE, NOUVEAU);
        BuyItem article = trouver(id);
        appliquer(article, corps);
        em.flush();
        return dto(article);
    }

    @Transactional
    public void supprimer(UUID id) {
        droits.exiger(LISTE, SUPPRIMER);
        BuyItem article = trouver(id);
        List<String> presents = new ArrayList<>();
        CITANTES.forEach((sql, libelle) -> suppression.ajouterSiPresent(presents, sql, true, id, libelle));
        if (!presents.isEmpty()) {
            throw SuppressionProjet.utilise("l'article " + article.getCode(), presents.stream().sorted().toList());
        }
        em.remove(article);
        suppression.flushOuConflit("l'article");
    }

    private BuyItem trouver(UUID id) {
        BuyItem article = em.find(BuyItem.class, id);
        if (article == null) {
            throw new ResourceNotFoundException("Article d'achat introuvable (%s).".formatted(id));
        }
        return article;
    }

    /** Tout est validé avant le premier setter : un refus ne laisse pas l'entité modifiée dans la session. */
    private void appliquer(BuyItem a, ArticleAchatEcritureDto c) {
        String code = SaisiePatrimoine.obligatoire(c.code(), a.getCode(), "code");
        String description = SaisiePatrimoine.obligatoire(c.description(), a.getDescription(), "description");
        StlCategory categorie = reference(StlCategory.class, c.categorieComptableId(), a.getStlCategory(),
                "categorieComptableId");
        BuyItemUnderFamily sousFamille = reference(BuyItemUnderFamily.class, c.sousFamilleId(),
                a.getBuyItemUnderFamily(), "sousFamilleId");
        if (c.familleId() != null && !c.familleId().equals(id(sousFamille.getBuyItemFamily()))) {
            throw new InvalidRequestException(R3);
        }
        String unite = SaisiePatrimoine.texte(c.unite(), a.getUnity(), 255, "unite");
        String notes = SaisiePatrimoine.texte(c.notes(), a.getNotes(), 500, "notes");
        ReferentielAdminService.verrouiller(em, BuyItem.class, code, "Articles");
        if (em.createQuery(DOUBLON, Long.class).setParameter("code", code).setParameter("id", a.getId())
                .getSingleResult() > 0) {
            throw new ConflictException(ReferentielAdminService.CODE_EXISTANT);
        }
        a.setCode(code);
        a.setDescription(description);
        a.setStlCategory(categorie);
        a.setBuyItemUnderFamily(sousFamille);
        a.setUnity(unite);
        a.setNotes(notes);
        a.setUnitPriceHT(montant(c.prixUnitaireHT(), a.getUnitPriceHT()));
        a.setFodecRatio(montant(c.fodec(), a.getFodecRatio()));
        a.setCostLogisticHT(montant(c.coutLogistiqueHT(), a.getCostLogisticHT()));
        a.setCostLogisticVatRatio(montant(c.coutLogistiqueTva(), a.getCostLogisticVatRatio()));
        a.setCostIndirectHT(montant(c.coutIndirectHT(), a.getCostIndirectHT()));
        a.setCostIndirectVatRatio(montant(c.coutIndirectTva(), a.getCostIndirectVatRatio()));
        a.setCostFinacialHT(montant(c.coutFinancierHT(), a.getCostFinacialHT()));
        a.setCostFinacialVatRatio(montant(c.coutFinancierTva(), a.getCostFinacialVatRatio()));
        a.setSaleUnitPriceHT(montant(c.prixVenteHT(), a.getSaleUnitPriceHT()));
        a.setSaleVatRatio(montant(c.venteTva(), a.getSaleVatRatio()));
        trace.horodater(a);
    }

    /** Saisie, sinon valeur actuelle ; obligatoire à la création (400), PKey inconnue → 400 nommant le champ. */
    private <T> T reference(Class<T> classe, UUID saisie, T actuelle, String champ) {
        if (saisie == null) {
            if (actuelle == null) {
                throw new InvalidRequestException(champ + " est obligatoire.");
            }
            return actuelle;
        }
        T cible = em.find(classe, saisie);
        if (cible == null) {
            throw new InvalidRequestException("%s : référence inconnue (%s).".formatted(champ, saisie));
        }
        return cible;
    }

    private static BigDecimal montant(BigDecimal saisie, BigDecimal actuel) {
        return Objects.requireNonNullElse(SaisiePatrimoine.ou(saisie, actuel), BigDecimal.ZERO);
    }

    private static UUID id(CodeLibelleOmran ligne) {
        return ligne == null ? null : ligne.getId();
    }

    private static String valeur(CodeLibelleOmran ligne, Function<CodeLibelleOmran, String> lecture) {
        return ligne == null ? null : lecture.apply(ligne);
    }

    private static ArticleAchatDto dto(BuyItem a) {
        BuyItemUnderFamily s = a.getBuyItemUnderFamily();
        BuyItemFamily f = s == null ? null : s.getBuyItemFamily();
        StlCategory c = a.getStlCategory();
        return new ArticleAchatDto(a.getId(), a.getCode(), a.getDescription(), c == null ? null : c.getId(),
                c == null ? null : c.getCode(), id(f), valeur(f, CodeLibelleOmran::getCode), id(s),
                valeur(s, CodeLibelleOmran::getCode), a.getUnity(), a.getUnitPriceHT(), a.getFodecRatio(),
                a.getCostLogisticHT(), a.getCostLogisticVatRatio(), a.getCostIndirectHT(), a.getCostIndirectVatRatio(),
                a.getCostFinacialHT(), a.getCostFinacialVatRatio(), a.getSaleUnitPriceHT(), a.getSaleVatRatio(),
                a.getNotes());
    }
}
