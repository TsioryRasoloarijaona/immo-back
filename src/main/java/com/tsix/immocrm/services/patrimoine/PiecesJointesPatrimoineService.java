package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.piecejointe.PieceJointeSupport;
import com.tsix.immocrm.services.piecejointe.PorteurDepot;
import com.tsix.immocrm.services.piecejointe.SaisiePieceJointe;
import com.tsix.immocrm.storage.PorteurPieceJointe;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/** Pièces jointes d'un article, d'une tranche ou d'un programme : R4 du porteur, puis {@link PieceJointeSupport}. */
@Service
@Transactional(readOnly = true)
public class PiecesJointesPatrimoineService {

    private final StkItemRepository itemRepository;
    private final TrancheVisibleRepository trancheRepository;
    private final ProjetService projetService;
    private final PieceJointeSupport support;

    public PiecesJointesPatrimoineService(StkItemRepository itemRepository, TrancheVisibleRepository trancheRepository,
                                          ProjetService projetService, PieceJointeSupport support) {
        this.itemRepository = itemRepository;
        this.trancheRepository = trancheRepository;
        this.projetService = projetService;
        this.support = support;
    }

    public List<PieceJointeDto> lister(String segment, UUID id) {
        return support.lister(porteur(segment, id).id());
    }

    public URI urlFichier(String segment, UUID id, UUID pieceId) {
        PorteurDepot porteur = porteur(segment, id);
        return support.urlTelechargement(id, pieceId, porteur.type().libelle());
    }

    @Transactional
    public PieceJointeDto ajouterPieceJointe(String segment, UUID id, MultipartFile fichier, SaisiePieceJointe saisie,
                                             AuthenticatedUser principal) {
        return support.deposer(porteur(segment, id), fichier, saisie, principal);
    }

    @Transactional
    public void supprimerPieceJointe(String segment, UUID id, UUID pieceId, AuthenticatedUser principal) {
        support.supprimer(porteur(segment, id), pieceId, principal);
    }

    public List<String> groupes() {
        return support.groupes();
    }

    /** Porteur visible (R4, 404 sinon), avec la {@code Category} et la {@code CategoryDescription} d'OMRAN. */
    PorteurDepot porteur(String segment, UUID id) {
        return switch (segment) {
            case "articles" -> article(itemRepository.findVisible(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Article introuvable: " + id)));
            case "tranches" -> {
                CfgTranche t = trancheRepository.exigerVisible(id);
                yield depot(id, PorteurPieceJointe.TRANCHE, t.getCode(), t.getDescription());
            }
            default -> {
                CfgProject p = projetService.visible(id);
                yield depot(id, PorteurPieceJointe.PROJET, p.getCode(), p.getCode());
            }
        };
    }

    static PorteurDepot article(StkItem article) {
        return depot(article.getId(), PorteurPieceJointe.ARTICLE, article.getCode(), article.getCode());
    }

    private static PorteurDepot depot(UUID id, PorteurPieceJointe type, String code, String categoryDescription) {
        return new PorteurDepot(id, type, code, type.categoryParDefaut(), categoryDescription);
    }
}
