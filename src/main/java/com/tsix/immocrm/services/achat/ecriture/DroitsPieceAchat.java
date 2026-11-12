package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Seule table des droits OMRAN d'écriture des pièces d'achat (FEATURE-achat-fiche-ecritures.md E-B1) : un des codes
 * applicables suffit ; le code Projet › Lot ne compte que pour une pièce à lot, jamais pour la facture (Q-D).
 */
@Component
public class DroitsPieceAchat {

    private static final String SUPPRIMER = ".toolDelete";
    private static final String MODIFIER = ".toolNew";
    private static final Map<String, Ecrans> ECRANS = Map.of(
            NatureDocumentAchat.QUOTE.name(), new Ecrans("Yes.Buy.Front.Quote.QuoteList", "Yes.Prj.Front.Market.QuoteList"),
            NatureDocumentAchat.ORDER.name(), new Ecrans("Yes.Buy.Front.Order.OrderList", "Yes.Prj.Front.Market.OrderList"),
            NatureDocumentAchat.DELIVRY.name(),
            new Ecrans("Yes.Buy.Front.Delivery.DeliveryList", "Yes.Prj.Front.Market.DeliveryList"),
            NatureDocumentAchat.INVOICE.name(), new Ecrans("Yes.Stl.Front.Debit.InvoiceList", null),
            NatureDocumentAchat.REFUND.name(), new Ecrans("Yes.Stl.Front.Debit.RefundList", "Yes.Prj.Front.Market.RefundList"));

    private record Ecrans(String achat, String lot) {
    }

    private final DroitsOmran droits;

    public DroitsPieceAchat(DroitsOmran droits) {
        this.droits = droits;
    }

    /** Nature écrite par Arpent (DOCUMENT_TO_PAY n'a aucun code de droit : gérée ailleurs). */
    public static boolean supportee(String nature) {
        return ECRANS.containsKey(nature);
    }

    public boolean peutSupprimer(String nature, boolean aLot) {
        return porte(nature, aLot, SUPPRIMER);
    }

    public boolean peutModifier(String nature, boolean aLot) {
        return porte(nature, aLot, MODIFIER);
    }

    public void exigerSuppression(String nature, boolean aLot) {
        exiger(peutSupprimer(nature, aLot));
    }

    public void exigerModification(String nature, boolean aLot) {
        exiger(peutModifier(nature, aLot));
    }

    private static void exiger(boolean autorise) {
        if (!autorise) {
            throw new ForbiddenException("Vous n'avez pas le droit d'effectuer cette action.");
        }
    }

    private boolean porte(String nature, boolean aLot, String action) {
        Ecrans ecrans = nature == null ? null : ECRANS.get(nature);
        if (ecrans == null) {
            return false;
        }
        String[] codes = Stream.of(ecrans.achat(), aLot ? ecrans.lot() : null)
                .filter(Objects::nonNull).map(e -> e + action).toArray(String[]::new);
        return !droits.parmi(codes).isEmpty();
    }
}
