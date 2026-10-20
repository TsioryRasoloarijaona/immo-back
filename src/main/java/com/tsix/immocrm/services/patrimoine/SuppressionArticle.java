package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.services.patrimoine.ArticleService.estOccupe;

/**
 * Suppression d'un article et de ses sous-articles rattachés (FEATURE-patrimoine-article-fiche.md §3, §11.4), patron
 * {@link SuppressionProjet} : 409 si l'un d'eux est occupé ou référencé, sinon cascades par entités (audit, copie).
 */
@Component
public record SuppressionArticle(EntityManager em, StkItemRepository items, SuppressionProjet suppression,
                                 EcritureArticle ecriture) {

    private static final String IDS = "ids";
    /** Références NO_ACTION vers StkItem mappées ; les autres tombent au flush (409 nommant la contrainte). */
    private static final Map<String, String> BLOQUANTS = Map.of(
            "SELECT COUNT(p) FROM ComProspection p WHERE p.stkItem.id IN :ids", "prospection(s)",
            "SELECT COUNT(p) FROM ComProspectionProject p WHERE p.stkItem.id IN :ids", "projet(s) de prospection",
            "SELECT COUNT(f) FROM ComFolder f WHERE f.stkItem.id IN :ids", "dossier(s) de vente",
            "SELECT COUNT(l) FROM ComFolderItem l WHERE l.stkItem.id IN :ids", "ligne(s) de dossier",
            "SELECT COUNT(q) FROM StlQuote q WHERE q.stkItem.id IN :ids", "offre(s)",
            "SELECT COUNT(l) FROM StlQuoteItem l WHERE l.stkItem.id IN :ids", "ligne(s) d'offre",
            "SELECT COUNT(h) FROM StrFolderStatusHistory h WHERE h.stkItem.id IN :ids", "historique(s) de dossier",
            "SELECT COUNT(t) FROM CfgTrancheStkItem t WHERE t.stkItem.id IN :ids", "terrain(s) de tranche");
    // CASCADE en production : bloquant quand même, une suppression d'article n'efface pas des dépenses (argent).
    private static final String DEPENSES = "SELECT COUNT(*) FROM StkExpense WHERE StkItemId IN (:ids)";
    private static final List<String> CASCADES = List.of(
            "SELECT v FROM StkItemVue v WHERE v.stkItem.id IN :ids",
            "SELECT c FROM StkItemContenance c WHERE c.stkItem.id IN :ids",
            "SELECT p FROM StkHierarchyPath p WHERE p.stkItemId IN :ids",
            "SELECT a FROM AdmAttachedFile a WHERE a.categoryId IN :ids",
            "SELECT m FROM AdmMessage m WHERE m.categoryId IN :ids");

    public void supprimer(StkItem article) {
        // BuildItem.cs:DeleteUnderItem ; la suppression du parent, elle, emporte ses sous-articles affectés par défaut.
        if (article.getParent() != null && Boolean.TRUE.equals(article.getAlwaysAffected())) {
            throw new ConflictException(
                    "Vous ne pouvez pas supprimer un sous article affecté par défaut à l'article en cours !");
        }
        List<StkItem> objets = new ArrayList<>(items.findByParentId(article.getId()));
        objets.add(article);
        objets.stream().filter(i -> estOccupe(i) || i.getSynFolderId() != null).findFirst().ifPresent(i -> {
            throw new ConflictException(("Suppression impossible : l'article %s est affecté à un dossier de vente, de "
                    + "location ou de syndic.").formatted(i.getCode()));
        });
        List<UUID> ids = objets.stream().map(StkItem::getId).toList();
        long depenses = ((Number) em.createNativeQuery(DEPENSES).setParameter(IDS, ids).getSingleResult()).longValue();
        List<String> presents = Stream.concat(BLOQUANTS.entrySet().stream()
                        .map(b -> Map.entry(em.createQuery(b.getKey(), Long.class).setParameter(IDS, ids)
                                .getSingleResult(), b.getValue())),
                        Stream.of(Map.entry(depenses, "dépense(s)")))
                .filter(b -> b.getKey() > 0)
                .map(b -> b.getKey() + " " + b.getValue())
                .sorted()
                .toList();
        if (!presents.isEmpty()) {
            throw new ConflictException("Suppression impossible : l'article %s ou ses sous-articles portent encore %s."
                    .formatted(article.getCode(), String.join(", ", presents)));
        }
        CASCADES.forEach(jpql -> em.createQuery(jpql, Object.class).setParameter(IDS, ids).getResultList()
                .forEach(em::remove));
        objets.forEach(em::remove);
        suppression.flushOuConflit("l'article");
        if (article.getParent() != null) {
            ecriture.reecrireParent(article.getParent());
        }
    }
}
