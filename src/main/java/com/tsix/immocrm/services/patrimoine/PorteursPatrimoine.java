package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.stk.StkFeasibilityStudyRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.patrimoine.prospectionterrain.ProspectionTerrainService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

/**
 * Objet patrimoine porteur d'audit, de notes ou d'actions. Visibilité = celle de la lecture par id de son type :
 * la recherche des services est réutilisée telle quelle, une règle qui s'y ajoute (R4…) s'applique d'office ici.
 */
@Component
public class PorteursPatrimoine {

    public enum Porteur {
        PROJET, TRANCHE, ARTICLE, PROSPECTION_TERRAIN, ETUDE;

        /** Valeur du fil, en minuscules ({@code projet}, …, {@code prospection-terrain}, {@code etude}) ; autre → 400. */
        public static Porteur depuis(String valeur) {
            return Arrays.stream(values())
                    .filter(p -> p.name().replace('_', '-').equalsIgnoreCase(valeur == null ? "" : valeur.trim()))
                    .findFirst()
                    .orElseThrow(() -> new InvalidRequestException(
                            "porteur doit valoir projet, tranche, article, prospection-terrain ou etude : '" + valeur + "'"));
        }
    }

    private final ProjetService projetService;
    private final TrancheService trancheService;
    private final ArticleService articleService;
    private final ProspectionTerrainService prospectionTerrainService;
    private final CfgProjectRepository projectRepository;
    private final CfgTrancheRepository trancheRepository;
    private final StkItemRepository itemRepository;
    private final StkFeasibilityStudyRepository etudes;

    public PorteursPatrimoine(ProjetService projetService, TrancheService trancheService, ArticleService articleService,
                              ProspectionTerrainService prospectionTerrainService, ReferentielsPorteurs referentiels) {
        this.projetService = projetService;
        this.trancheService = trancheService;
        this.articleService = articleService;
        this.prospectionTerrainService = prospectionTerrainService;
        this.projectRepository = referentiels.projets();
        this.trancheRepository = referentiels.tranches();
        this.itemRepository = referentiels.articles();
        this.etudes = referentiels.etudes();
    }

    /** Dépôts lus pour reconnaître le type d'un id (record {@code @Component} : constructeur sous S107). */
    @Component
    record ReferentielsPorteurs(CfgProjectRepository projets, CfgTrancheRepository tranches, StkItemRepository articles,
                                StkFeasibilityStudyRepository etudes) {
    }

    /** 404 si {@code id} n'est pas un objet de ce type visible par l'appelant. */
    public void exigerVisible(Porteur porteur, UUID id) {
        switch (porteur) {
            case PROJET -> projetService.findById(id);
            case TRANCHE -> trancheService.findById(id);
            case ARTICLE -> articleService.findById(id);
            case PROSPECTION_TERRAIN -> prospectionTerrainService.exiger(id);
            case ETUDE -> etudes.exigerVisible(id);
        }
    }

    /** Type de l'objet {@code id}, après la même vérification ; 404 s'il n'est pas du patrimoine. */
    public Porteur exigerVisible(UUID id) {
        Porteur porteur;
        if (projectRepository.existsById(id)) {
            porteur = Porteur.PROJET;
        } else if (trancheRepository.existsById(id)) {
            porteur = Porteur.TRANCHE;
        } else if (itemRepository.existsById(id)) {
            porteur = Porteur.ARTICLE;
        } else if (etudes.existsById(id)) {
            porteur = Porteur.ETUDE;
        } else {
            porteur = Porteur.PROSPECTION_TERRAIN;
        }
        exigerVisible(porteur, id);
        return porteur;
    }
}
