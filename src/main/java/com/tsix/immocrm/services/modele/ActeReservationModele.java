package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.dto.dossier.ActeReservationData;
import com.tsix.immocrm.services.dossier.ActeReservationService;
import com.tsix.immocrm.services.dossier.ContratApresPointage;
import com.tsix.immocrm.services.dossier.document.ActeReservationDocxRenderer;
import com.tsix.immocrm.services.dossier.document.ActeReservationPdfRenderer;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Seul point de rendu de l'acte de réservation : le modèle {@code DOSSIER_VENTE/ACTE_RESERVATION} s'il est actif,
 * sinon les rendus codés en dur, gardés en repli (FEATURE-modeles-documents.md §8).
 */
@Component
public class ActeReservationModele {

    public static final String CODE = "ACTE_RESERVATION";

    private final GenerationModeleService generation;
    private final ActeReservationService acteReservationService;
    private final ActeReservationPdfRenderer pdfRenderer;
    private final ActeReservationDocxRenderer docxRenderer;
    private final ContratApresPointage contratApresPointage;

    public ActeReservationModele(GenerationModeleService generation, ActeReservationService acteReservationService,
                                 ActeReservationPdfRenderer pdfRenderer, ActeReservationDocxRenderer docxRenderer,
                                 ContratApresPointage contratApresPointage) {
        this.generation = generation;
        this.acteReservationService = acteReservationService;
        this.pdfRenderer = pdfRenderer;
        this.docxRenderer = docxRenderer;
        this.contratApresPointage = contratApresPointage;
    }

    /** Document et code du dossier (pour le nom de fichier {@code acte-reservation-<code>}). */
    public static final class Acte {
        private final byte[] contenu;
        private final String dossierCode;

        Acte(byte[] contenu, String dossierCode) {
            this.contenu = contenu;
            this.dossierCode = dossierCode;
        }

        public byte[] contenu() {
            return contenu;
        }

        public String dossierCode() {
            return dossierCode;
        }
    }

    public Acte rendre(UUID dossierId, FormatDocument format) {
        contratApresPointage.verifier(dossierId);
        return generation.produireParCode(CategorieModele.DOSSIER_VENTE, CODE, dossierId, format)
                .map(d -> new Acte(d.contenu(), d.codeFiche()))
                .orElseGet(() -> repli(dossierId, format));
    }

    private Acte repli(UUID dossierId, FormatDocument format) {
        ActeReservationData data = acteReservationService.collect(dossierId);
        byte[] contenu = format == FormatDocument.DOCX ? docxRenderer.render(data) : pdfRenderer.render(data);
        return new Acte(contenu, data.dossierCode());
    }
}
