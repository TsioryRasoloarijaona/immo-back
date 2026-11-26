package com.tsix.immocrm.services.achat;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.services.achat.document.PaiementApprouvePdfRenderer;
import com.tsix.immocrm.services.droits.DroitsOmran;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/** Impression « Paiement approuvé » d'une facture d'achat (FEATURE-lc8b-paiement-approuve.md). */
@Service
@Transactional(readOnly = true)
public class PaiementApprouveService {

    static final String DROIT_IMPRESSION = "Yes.Stl.Front.Debit.InvoiceList.toolPrint";
    private static final String NON_FACTURE = "L'impression Paiement approuvé est réservée aux factures d'achat";

    private final FicheAchatService fiches;
    private final DroitsOmran droits;
    private final RequetesDocumentsAchat requetes;
    private final PaiementApprouvePdfRenderer renderer;

    public PaiementApprouveService(FicheAchatService fiches, DroitsOmran droits, RequetesDocumentsAchat requetes,
                                   PaiementApprouvePdfRenderer renderer) {
        this.fiches = fiches;
        this.droits = droits;
        this.requetes = requetes;
        this.renderer = renderer;
    }

    /** PDF rendu et code de la pièce (nom du fichier). */
    public record Impression(String code, byte[] pdf) {
        @Override
        public boolean equals(Object autre) {
            return autre instanceof Impression i && Objects.equals(code, i.code) && Arrays.equals(pdf, i.pdf);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hashCode(code) + Arrays.hashCode(pdf);
        }

        @Override
        public String toString() {
            return "Impression[code=" + code + ", pdf=" + (pdf == null ? 0 : pdf.length) + " octets]";
        }
    }

    /** Ordre des refus : 404 (fiche invisible) → 403 (droit) → 409 (nature). */
    public Impression imprimer(UUID id) {
        DocumentAchatDetailDto fiche = fiches.fiche(id);
        droits.exiger(DROIT_IMPRESSION);
        if (!NatureDocumentAchat.INVOICE.name().equals(fiche.document().nature())) {
            throw new ConflictException(NON_FACTURE);
        }
        return new Impression(fiche.document().code(),
                renderer.rendre(fiche, requetes.reglementsPaiementApprouve(id)));
    }
}
