package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Seul endroit qui libère ou réoccupe les articles d'un dossier quand il entre en sortie
 * (ANNULATION, DESISTEMENT) ou en ressort. Voir {@code FEATURE-liberer-lot.md} R1, R5 et
 * {@code FEATURE-vente-socle.md} B4 (réactivation alignée sur {@code Sale.cs:InsertAfterCancellationOrClosing}).
 */
@Component
public class LiberationLot {

    private static final String REOCCUPATION = "Réoccupation de l'article ";

    private final StkItemRepository stkItemRepository;
    private final ComFolderItemRepository folderItemRepository;
    private final PerimetreCommercialService perimetreService;
    private final HistoriqueDossier historique;

    public LiberationLot(StkItemRepository stkItemRepository, ComFolderItemRepository folderItemRepository,
                         PerimetreCommercialService perimetreService, HistoriqueDossier historique) {
        this.stkItemRepository = stkItemRepository;
        this.folderItemRepository = folderItemRepository;
        this.perimetreService = perimetreService;
        this.historique = historique;
    }

    /**
     * R1 : {@code ComFolderId} = NULL sur chaque article lié ; un sous-article sans
     * {@code AlwaysAffected} perd aussi son {@code ParentId}. {@code ComFolder.StkItemId} n'est pas touché (R2).
     * Chaque sous-article libéré reçoit sa ligne « ANNULATION AFFECTATION » au statut de sortie.
     */
    public void liberer(ComFolder folder, StrStatus statutSortie) {
        List<StkItem> articles = stkItemRepository.findByComFolderId(folder.getId());
        // Tous contrôlés avant la première écriture : un 403 ne laisse aucun article à moitié libéré.
        articles.forEach(article -> verifierEcriture(article, "Libération de l'article "));
        UUID principalId = folder.getStkItem() != null ? folder.getStkItem().getId() : null;
        for (StkItem article : articles) {
            article.setComFolderId(null);
            if (Boolean.TRUE.equals(article.getIsUnderItem()) && !Boolean.TRUE.equals(article.getAlwaysAffected())) {
                article.setParent(null);
            }
            stkItemRepository.save(article);
            if (!article.getId().equals(principalId)) {
                historique.ligneStatut(folder, statutSortie, article, HistoriqueDossier.ANNULATION_AFFECTATION);
            }
        }
    }

    /**
     * Contrôles de réactivation, sans écriture : à appeler avant tout changement du dossier.
     * 409 si le lot principal est lié à un autre dossier, ou si un sous-article ({@code ComFolderItem}) n'est pas libre.
     */
    public void verifierReoccupable(ComFolder folder) {
        StkItem principal = folder.getStkItem();
        if (principal != null && principal.getComFolderId() != null
                && !principal.getComFolderId().equals(folder.getId())) {
            throw new ConflictException("Le lot " + principal.getCode()
                    + " a été revendu entre-temps : créer un nouveau dossier");
        }
        for (StkItem sous : sousArticles(folder)) {
            if (sous.getComFolderId() != null && !sous.getComFolderId().equals(folder.getId())) {
                throw new ConflictException("Le sous article " + sous.getCode() + " n'est pas libre, veuillez "
                        + "rafraichir les données de votre masque de suivi des vente !");
            }
        }
    }

    /**
     * Réactivation : le lot principal et <b>tous</b> les sous-articles du dossier ({@code ComFolderItem})
     * reprennent le dossier ({@code ComFolderId}, {@code ParentId} = lot, {@code IsUnderItem}), chacun avec
     * sa ligne « AFFECTATION » au statut repris.
     */
    public void reoccuper(ComFolder folder, StrStatus statutRepris) {
        verifierReoccupable(folder);
        StkItem principal = folder.getStkItem();
        List<StkItem> sousArticles = sousArticles(folder);
        if (principal != null) {
            verifierEcriture(principal, REOCCUPATION);
        }
        sousArticles.forEach(sous -> verifierEcriture(sous, REOCCUPATION));

        if (principal != null) {
            principal.setComFolderId(folder.getId());
            stkItemRepository.save(principal);
        }
        for (StkItem sous : sousArticles) {
            sous.setComFolderId(folder.getId());
            sous.setParent(principal);
            sous.setIsUnderItem(true);
            stkItemRepository.save(sous);
            historique.ligneStatut(folder, statutRepris, sous, HistoriqueDossier.AFFECTATION);
        }
    }

    /** Articles des lignes {@code ComFolderItem} du dossier, lot principal exclu, sans doublon. */
    private List<StkItem> sousArticles(ComFolder folder) {
        UUID principalId = folder.getStkItem() != null ? folder.getStkItem().getId() : null;
        Map<UUID, StkItem> parId = new LinkedHashMap<>();
        folderItemRepository.findByComFolderId(folder.getId()).stream()
                .map(ComFolderItem::getStkItem)
                .filter(Objects::nonNull)
                .filter(item -> !item.getId().equals(principalId))
                .forEach(item -> parId.putIfAbsent(item.getId(), item));
        return List.copyOf(parId.values());
    }

    private void verifierEcriture(StkItem article, String action) {
        perimetreService.verifierEcritureSurTranche(article.getStkHierarchy(), action + article.getCode());
    }
}
