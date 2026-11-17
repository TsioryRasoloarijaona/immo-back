package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.dto.achat.ActionsPieceAchatDto;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.GenerationPieceAchatDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentStatus;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Bloc {@code actions} de la fiche, tiré des mêmes règles que les écritures (FEATURE-achat-fiche-ecritures.md E-B6). */
@Component
public class ActionsPieceAchat {

    static final String GEREE_AILLEURS = "Cette pièce est gérée ailleurs (décompte, avance, frais de terrain, document "
            + "à régler) : elle ne se modifie pas depuis sa fiche.";
    private static final String GESTION_COMMISSION = "ComInvoiceDebitCommission";

    private final DroitsPieceAchat droits;
    private final TransitionsPieceAchat transitions;
    private final GenerationPieceAchat generation;
    private final EntityManager em;

    ActionsPieceAchat(DroitsPieceAchat droits, TransitionsPieceAchat transitions, GenerationPieceAchat generation,
                      EntityManager em) {
        this.droits = droits;
        this.transitions = transitions;
        this.generation = generation;
        this.em = em;
    }

    /** {@code vue} : la pièce telle que la liste du compte la voit (porte le décompte qui la cite). */
    public ActionsPieceAchatDto de(DocumentAchatListItemDto vue) {
        BuyDocument d = Objects.requireNonNull(em.find(BuyDocument.class, vue.id()));
        boolean libre = !gereeAilleurs(d, vue);
        String nature = d.getDocType();
        boolean aLot = d.getPrjMarketId() != null;
        return new ActionsPieceAchatDto(libre && droits.peutSupprimer(nature, aLot),
                libre && estFacture(d) && droits.peutModifier(nature, false), transitions.permises(d),
                refusModification(d, vue, aLot).isEmpty(), generations(d, aLot));
    }

    /** G7 : cibles que le POST accepterait depuis cette seule pièce (droit de la cible, puis préconditions). */
    private List<GenerationPieceAchatDto> generations(BuyDocument d, boolean aLot) {
        return Stream.of(NatureDocumentAchat.ORDER, NatureDocumentAchat.DELIVRY, NatureDocumentAchat.INVOICE,
                        NatureDocumentAchat.REFUND)
                .filter(c -> droits.peutModifier(c.name(), aLot) && generation.permise(c, d))
                .map(c -> new GenerationPieceAchatDto(c.name(), "Générer " + c.libelle())).toList();
    }

    /**
     * Premier refus d'une modification par la fiche, dans l'ordre du PUT (E-C5.1) : 409 gérée ailleurs, 403 droit (lot
     * stocké et lot du corps), 409 lecture seule ; vide si la pièce est modifiable. Seule règle de {@code peutModifier}.
     */
    Optional<RuntimeException> refusModification(BuyDocument d, DocumentAchatListItemDto vue, boolean lotCorps) {
        String nature = d.getDocType();
        if (!NatureDocumentAchat.saisissable(nature) || gereeAilleurs(d, vue) || commission(d)) {
            return Optional.of(new ConflictException(GEREE_AILLEURS));
        }
        if (!droits.peutModifier(nature, d.getPrjMarketId() != null) || !droits.peutModifier(nature, lotCorps)) {
            return Optional.of(DroitsPieceAchat.refus());
        }
        BuyDocumentStatus statut = d.getBuyDocumentStatusId() == null ? null
                : em.find(BuyDocumentStatus.class, d.getBuyDocumentStatusId());
        if (statut != null && Boolean.TRUE.equals(statut.getIsReadOnly())
                && !NatureDocumentAchat.QUOTE.name().equals(nature)) {
            return Optional.of(new ConflictException("Cette pièce est en lecture seule (statut %s)."
                    .formatted(Objects.requireNonNullElse(statut.getDescription(), statut.getCode()))));
        }
        return Optional.empty();
    }

    private boolean commission(BuyDocument d) {
        StrEntity gestion = d.getStrEntityId() == null ? null : em.find(StrEntity.class, d.getStrEntityId());
        return gestion != null && GESTION_COMMISSION.equals(gestion.getCode());
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
