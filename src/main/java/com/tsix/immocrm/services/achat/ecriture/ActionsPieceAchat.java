package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.dto.achat.ActionsPieceAchatDto;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.Objects;

/** Bloc {@code actions} de la fiche, tiré des mêmes règles que les écritures (FEATURE-achat-fiche-ecritures.md E-B6). */
@Component
public class ActionsPieceAchat {

    private final DroitsPieceAchat droits;
    private final TransitionsPieceAchat transitions;
    private final EntityManager em;

    public ActionsPieceAchat(DroitsPieceAchat droits, TransitionsPieceAchat transitions, EntityManager em) {
        this.droits = droits;
        this.transitions = transitions;
        this.em = em;
    }

    /** {@code vue} : la pièce telle que la liste du compte la voit (porte le décompte qui la cite). */
    public ActionsPieceAchatDto de(DocumentAchatListItemDto vue) {
        BuyDocument d = Objects.requireNonNull(em.find(BuyDocument.class, vue.id()));
        boolean libre = !gereeAilleurs(d, vue);
        String nature = d.getDocType();
        return new ActionsPieceAchatDto(libre && droits.peutSupprimer(nature, d.getPrjMarketId() != null),
                libre && estFacture(d) && droits.peutModifier(nature, false), transitions.permises(d));
    }

    /** Pièce qu'Arpent n'écrit pas : à régler, à référence (avance, frais de terrain…) ou facture d'un décompte (Q-C). */
    static boolean gereeAilleurs(BuyDocument d, DocumentAchatListItemDto vue) {
        String reference = d.getReferenceType();
        return NatureDocumentAchat.DOCUMENT_TO_PAY.name().equals(d.getDocType())
                || (reference != null && !reference.isBlank()) || vue.decompteId() != null;
    }

    static boolean estFacture(BuyDocument d) {
        return NatureDocumentAchat.INVOICE.name().equals(d.getDocType());
    }
}
