package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.BilanAttachementDto;
import com.tsix.immocrm.dto.patrimoine.BilanAttachementDto.Resultat;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.piecejointe.PieceJointeSupport;
import com.tsix.immocrm.services.piecejointe.PorteurDepot;
import com.tsix.immocrm.services.piecejointe.SaisiePieceJointe;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

/** Un fichier de l'attachement en masse, dans sa propre transaction. */
@Component
public class AttachementMasseFichier {

    private final PieceJointeSupport support;

    public AttachementMasseFichier(PieceJointeSupport support) {
        this.support = support;
    }

    /** Pièce du même type déjà sur l'article : la plus récente est remplacée ; sinon une pièce est créée. */
    @Transactional
    public BilanAttachementDto ajouterOuRemplacer(StkItem article, String nom, MultipartFile fichier,
                                                  SaisiePieceJointe saisie, AuthenticatedUser principal) {
        PorteurDepot porteur = PiecesJointesPatrimoineService.article(article);
        Optional<UUID> existante = support.pieceDuType(article.getId(), saisie.typeId());
        if (existante.isPresent()) {
            UUID id = support.remplacer(existante.get(), porteur, fichier, saisie, principal).id();
            return new BilanAttachementDto(nom, Resultat.REMPLACE, null, id);
        }
        UUID id = support.deposer(porteur, fichier, saisie, principal).id();
        return new BilanAttachementDto(nom, Resultat.ATTACHE, null, id);
    }
}
