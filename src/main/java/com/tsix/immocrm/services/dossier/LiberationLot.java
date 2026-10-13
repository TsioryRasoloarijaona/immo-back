package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seul endroit qui libère ou réoccupe les articles d'un dossier quand il entre en sortie
 * (ANNULATION, DESISTEMENT) ou en ressort. Voir {@code FEATURE-liberer-lot.md} R1, R4, R5.
 */
@Component
public class LiberationLot {

    private final StkItemRepository stkItemRepository;
    private final PerimetreCommercialService perimetreService;

    public LiberationLot(StkItemRepository stkItemRepository, PerimetreCommercialService perimetreService) {
        this.stkItemRepository = stkItemRepository;
        this.perimetreService = perimetreService;
    }

    /**
     * R1 : {@code ComFolderId} = NULL sur chaque article lié ; un sous-article sans
     * {@code AlwaysAffected} perd aussi son {@code ParentId}. {@code ComFolder.StkItemId} n'est pas touché (R2).
     */
    public void liberer(ComFolder folder) {
        List<StkItem> articles = stkItemRepository.findByComFolderId(folder.getId());
        // Tous contrôlés avant la première écriture : un 403 ne laisse aucun article à moitié libéré.
        articles.forEach(article -> verifierEcriture(article, "Libération de l'article "));
        for (StkItem article : articles) {
            article.setComFolderId(null);
            if (Boolean.TRUE.equals(article.getIsUnderItem()) && !Boolean.TRUE.equals(article.getAlwaysAffected())) {
                article.setParent(null);
            }
            stkItemRepository.save(article);
        }
    }

    /**
     * Contrôle de R4 seul, sans écriture : à appeler avant tout changement du dossier.
     * 409 si le lot principal est lié à un autre dossier.
     */
    public void verifierReoccupable(ComFolder folder) {
        StkItem principal = folder.getStkItem();
        if (principal != null && principal.getComFolderId() != null
                && !principal.getComFolderId().equals(folder.getId())) {
            throw new ConflictException("Le lot " + principal.getCode()
                    + " a été revendu entre-temps : créer un nouveau dossier");
        }
    }

    /**
     * R4 : le lot principal et ses sous-articles {@code AlwaysAffected} encore libres reprennent le
     * dossier ; les annexes libérées sans {@code AlwaysAffected} ne reviennent pas (comme OMRAN).
     */
    public void reoccuper(ComFolder folder) {
        verifierReoccupable(folder);
        StkItem principal = folder.getStkItem();
        if (principal == null) {
            return;
        }
        List<StkItem> sousArticles = stkItemRepository.findByParentId(principal.getId()).stream()
                .filter(sous -> Boolean.TRUE.equals(sous.getAlwaysAffected()) && sous.getComFolderId() == null)
                .toList();
        verifierEcriture(principal, "Réoccupation de l'article ");
        sousArticles.forEach(sous -> verifierEcriture(sous, "Réoccupation de l'article "));

        principal.setComFolderId(folder.getId());
        stkItemRepository.save(principal);
        for (StkItem sous : sousArticles) {
            sous.setComFolderId(folder.getId());
            stkItemRepository.save(sous);
        }
    }

    private void verifierEcriture(StkItem article, String action) {
        perimetreService.verifierEcritureSurTranche(article.getStkHierarchy(), action + article.getCode());
    }
}
