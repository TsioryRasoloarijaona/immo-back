package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.config.audit.EtatAudite;
import com.tsix.immocrm.dto.patrimoine.ArticleDetailDto;
import com.tsix.immocrm.dto.patrimoine.ArticleEcritureDto;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto;
import com.tsix.immocrm.dto.patrimoine.importation.LigneBilanDto.ResultatImport;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.patrimoine.ArticleService;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.ChampImport;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.LigneImport;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur.LigneLue;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.web.export.FeuilleExport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.patrimoine.ArticleService.NATURE_BATIMENT;
import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.ARTICLE_PARENT;
import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.NATURE;
import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.PIVOT;
import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.TRANCHE;
import static com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.VOCATION;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.ARTICLES;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.SOUS_ARTICLES;
import static com.tsix.immocrm.services.patrimoine.importation.LectureChamps.texte;

/**
 * Traduction (en-têtes → {@link ArticleEcritureDto}, codes → ids) et écriture d'une ligne d'import d'articles ou de
 * sous-articles par les méthodes de l'écran (FEATURE-patrimoine-import-export.md §1.4, §1.5) : aucune règle métier ici.
 */
@Component
class ImportArticles implements ImportParType<ImportArticles.Contexte> {

    private static final String PAR_CODE = "select i from StkItem i where i.stkHierarchy.id = :tranche and i.code = :code";
    private static final String DE_LA_TRANCHE = "select i from StkItem i where i.stkHierarchy.id = :tranche order by i.code";
    private static final String CODE = "code";
    private static final String VOCATION_ID = "vocationId";
    private static final String PARENT_ID = "parentId";
    private static final Libelles ARTICLE = new Libelles("Création de l'article : %s", "Mise à jour de l'article : %s",
            "l'article : %s n'a pas été modifié", "Problèmes d'import de l'article : %s");
    private static final Libelles SOUS_ARTICLE = new Libelles("Création du sous-article : %s",
            "Mise à jour du sous-article : %s", "le sous-article : %s n'a pas été modifié",
            "Problèmes d'import du sous-article : %s");

    private final ArticleService articles;
    private final StkItemRepository items;
    private final EntityManager em;
    private final EtatAudite etat;
    private final JsonMapper json;

    ImportArticles(ArticleService articles, StkItemRepository items, EntityManager em, EtatAudite etat, JsonMapper json) {
        this.articles = articles;
        this.items = items;
        this.em = em;
        this.etat = etat;
        this.json = json;
    }

    /** Un import : tranche choisie, référentiels chargés une fois, en-têtes présents dans le fichier. */
    record Contexte(boolean sousArticles, CfgTranche tranche, ResolutionCodes codes, Set<String> entetes,
                    List<String> avertissements) {

        List<ChampImport> champs() {
            return ChampsArticle.du(sousArticles);
        }
    }

    /** Ligne traduite ; {@code sous} = colonnes « Sous article n » ayant un code ; {@code erreurs} de traduction. */
    record LigneTraduite(int numero, String pivot, Map<String, Object> champs, List<Map<String, Object>> sous,
                         String parent, boolean natureLue, List<String> erreurs) implements LigneImport {

        @Override
        public String code() {
            Object code = champs.get(CODE);
            return code == null ? pivot : code.toString();
        }
    }

    // ------------------------------------------------------------------ ImportParType

    @Override
    public Set<String> types() {
        return Set.of(ARTICLES, SOUS_ARTICLES);
    }

    @Override
    public boolean trancheExigee() {
        return true;
    }

    @Override
    public Contexte contexte(String type, CfgTranche tranche, Set<String> entetes, List<String> avertissements) {
        return new Contexte(SOUS_ARTICLES.equals(type), tranche, new ResolutionCodes(em, tranche.getId()), entetes,
                avertissements);
    }

    @Override
    public Set<String> entetesConnus(Contexte ctx) {
        return ctx.champs().stream().map(ChampImport::entete).collect(Collectors.toSet());
    }

    @Override
    public List<LigneBilanDto> ecrire(List<LigneLue> lignes, Contexte ctx) {
        return EcritureParLigne.ecrire(em, lignes.stream().map(l -> traduire(l, ctx)).toList(), l -> ecrire(l, ctx),
                ctx.sousArticles() ? SOUS_ARTICLE : ARTICLE);
    }

    @Override
    public Modele modele(String type, CfgTranche tranche) {
        boolean sous = SOUS_ARTICLES.equals(type);
        List<ChampImport> champs = ChampsArticle.du(sous);
        String nom = sous ? "SOUS_ARTICLES" : "ARTICLES";
        List<Map<String, Object>> lignes = em.createQuery(DE_LA_TRANCHE, StkItem.class)
                .setParameter("tranche", tranche.getId()).getResultList().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsUnderItem()) == sous
                        && NATURE_BATIMENT.equals(Champs.de(item.getStkKind(), StkKind::getCode)))
                .map(item -> ModeleImport.ligne(item, sous ? List.of() : sousArticles(item), champs))
                .toList();
        List<FeuilleExport> feuilles = new ArrayList<>();
        feuilles.add(ModeleImport.donnees(nom, champs, lignes));
        feuilles.addAll(ModeleImport.aides(champs, new ResolutionCodes(em, tranche.getId())));
        return new Modele(tranche.getCode() + "_STOCK_" + nom, feuilles);
    }

    private List<StkItem> sousArticles(StkItem article) {
        return items.findByParentId(article.getId()).stream()
                .sorted(Comparator.comparing(StkItem::getCode, Comparator.nullsLast(String::compareTo)))
                .limit(ChampsArticle.SOUS_ARTICLES_PAR_LIGNE).toList();
    }

    // ------------------------------------------------------------------ traduction

    LigneTraduite traduire(LigneLue l, Contexte ctx) {
        Map<String, Object> v = l.valeurs();
        List<String> erreurs = new ArrayList<>();
        Map<Integer, Map<String, Object>> lus = LectureChamps.lire(ctx.champs(), l, ctx.codes(), ctx.avertissements(),
                erreurs);
        Map<String, Object> champs = Objects.requireNonNullElseGet(lus.remove(0), HashMap::new);
        String pivot = texte(v.get(PIVOT));
        if (pivot == null) {
            erreurs.add("Manque le pivot");
        }
        controlerTranche(v, ctx, erreurs);
        boolean natureLue = !ctx.sousArticles()
                && LectureChamps.controlerNature(v.get(NATURE), ctx.codes(), NATURE_BATIMENT, "Bâtiment", erreurs);
        List<Map<String, Object>> sousArticles = lus.values().stream().filter(s -> s.get(CODE) != null).toList();
        return new LigneTraduite(l.numero(), pivot, champs, sousArticles, texte(v.get(ARTICLE_PARENT)), natureLue, erreurs);
    }

    private static void controlerTranche(Map<String, Object> v, Contexte ctx, List<String> erreurs) {
        String tranche = texte(v.get(TRANCHE));
        if (tranche == null) {
            if (ctx.entetes().contains(TRANCHE)) {
                erreurs.add("Manque le code de la tranche");
            }
        } else if (!tranche.equalsIgnoreCase(ctx.tranche().getCode())
                && !tranche.equalsIgnoreCase(ctx.tranche().getDescription())) {
            erreurs.add("Le code tranche saisi est introuvable dans notre référenciel");
        }
    }

    // ------------------------------------------------------------------ écriture (dans la transaction de l'import)

    ResultatImport ecrire(LigneTraduite l, Contexte ctx) {
        List<StkItem> trouves = parCode(ctx, l.pivot());
        if (trouves.size() > 1) {
            throw new InvalidRequestException("Il existe plusieurs article avec le même code");
        }
        StkItem existant = trouves.isEmpty() ? null : trouves.get(0);
        return ctx.sousArticles() ? sousArticle(l, existant, ctx) : article(l, existant, ctx);
    }

    private ResultatImport article(LigneTraduite l, StkItem existant, Contexte ctx) {
        Map<String, Object> champs = new HashMap<>(l.champs());
        if (existant == null) {
            if (!l.natureLue()) {
                throw new InvalidRequestException("Manque la définition de la nature de l'article");
            }
            champs.put("trancheId", ctx.tranche().getId());
            champs.putIfAbsent(CODE, l.pivot());
            sousArticles(l, articles.creerArticle(dto(champs)));
            return ResultatImport.CREATION;
        }
        if (Boolean.TRUE.equals(existant.getIsUnderItem())) {
            throw new InvalidRequestException("Il existe un sous article avec le même pivot");
        }
        Object[] avant = etat.etat(existant);
        boolean sousModifies = sousArticles(l, articles.modifierArticle(existant.getId(), dto(champs)));
        return sousModifies || etat.modifie(existant, avant) ? ResultatImport.MISE_A_JOUR : ResultatImport.INCHANGEE;
    }

    /** OMRAN : sous-article cherché parmi ceux de l'article ; créé sinon, libellé = code, étage = celui de l'article. */
    private boolean sousArticles(LigneTraduite l, ArticleDetailDto article) {
        boolean modifie = false;
        for (Map<String, Object> s : l.sous()) {
            String code = s.get(CODE).toString();
            ArticleListItemDto existant = article.sousLots().stream()
                    .filter(sl -> code.equalsIgnoreCase(sl.code())).findFirst().orElse(null);
            if (existant == null) {
                Map<String, Object> champs = new HashMap<>(s);
                champs.put("description", code);
                champs.put("etageId", article.etageId());
                articles.creerSousArticle(article.id(), dto(champs));
                modifie = true;
            } else {
                StkItem sous = em.find(StkItem.class, existant.id());
                Object[] avant = etat.etat(sous);
                articles.modifierArticle(sous.getId(), dto(s));
                modifie |= etat.modifie(sous, avant);
            }
        }
        return modifie;
    }

    private ResultatImport sousArticle(LigneTraduite l, StkItem existant, Contexte ctx) {
        if (existant != null && !Boolean.TRUE.equals(existant.getIsUnderItem())) {
            throw new InvalidRequestException("Il existe un article avec le même pivot");
        }
        Map<String, Object> champs = new HashMap<>(l.champs());
        StkItem parent = l.parent() == null ? null : parent(l.parent(), ctx);
        if (parent != null) {
            champs.put(PARENT_ID, parent.getId());
            champs.put(VOCATION_ID, Champs.de(parent.getStkVocation(), StkVocation::getId));
        }
        if (existant == null) {
            exigerCodes(champs, parent == null && ctx.entetes().contains(VOCATION));
            champs.put("trancheId", ctx.tranche().getId());
            champs.putIfAbsent(CODE, l.pivot());
            articles.creerSousArticleLibre(dto(champs));
            return ResultatImport.CREATION;
        }
        Object[] avant = etat.etat(existant);
        if (existant.getParent() == null) {
            articles.modifierSousArticle(existant.getId(), dto(champs));
        } else {
            // Rattaché : se modifie depuis la fiche de son article (PATCH /api/articles/{id}), sans vocation ni parent.
            champs.remove(VOCATION_ID);
            champs.remove(PARENT_ID);
            articles.modifierArticle(existant.getId(), dto(champs));
        }
        return etat.modifie(existant, avant) ? ResultatImport.MISE_A_JOUR : ResultatImport.INCHANGEE;
    }

    /** Messages OMRAN d'{@code ImportUnderItems} pour une création. */
    private static void exigerCodes(Map<String, Object> champs, boolean vocationExigee) {
        if (vocationExigee && champs.get(VOCATION_ID) == null) {
            throw new InvalidRequestException("Manque le code de la vocation");
        }
        if (champs.get("categorieId") == null) {
            throw new InvalidRequestException("Manque le code de la catégorie");
        }
        if (champs.get("typeId") == null) {
            throw new InvalidRequestException("Manque le code du type");
        }
    }

    private StkItem parent(String code, Contexte ctx) {
        List<StkItem> trouves = parCode(ctx, code);
        if (trouves.size() != 1 || Boolean.TRUE.equals(trouves.get(0).getIsUnderItem())) {
            throw new InvalidRequestException("Il y a un problème avec le code article parent");
        }
        return trouves.get(0);
    }

    /** Lu avant toute écriture de la ligne (la précédente est déjà flushée) : pas d'auto-flush, coûteux sur un gros import. */
    private List<StkItem> parCode(Contexte ctx, String code) {
        return em.createQuery(PAR_CODE, StkItem.class).setFlushMode(FlushModeType.COMMIT)
                .setParameter("tranche", ctx.tranche().getId()).setParameter(CODE, code).getResultList();
    }

    private ArticleEcritureDto dto(Map<String, Object> champs) {
        return json.convertValue(champs, ArticleEcritureDto.class);
    }
}
