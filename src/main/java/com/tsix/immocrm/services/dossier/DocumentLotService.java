package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.dossier.document.FicheBancairePdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheDossierVentePdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheReservationPdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheTropPercuPdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheVisiteTechniquePdfRenderer;
import com.tsix.immocrm.services.dossier.document.PdfMerger;
import com.tsix.immocrm.services.modele.ActeReservationModele;
import com.tsix.immocrm.services.modele.FormatDocument;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** Génération en lot : le pipeline {@code collect} + {@code render} de chaque document unitaire, fusionné. */
@Service
@Transactional(readOnly = true)
public class DocumentLotService {

    private final FicheReservationService ficheReservationService;
    private final FicheReservationPdfRenderer ficheReservationPdfRenderer;
    private final ActeReservationModele acteReservationModele;
    private final FicheBancaireService ficheBancaireService;
    private final FicheBancairePdfRenderer ficheBancairePdfRenderer;
    private final FicheVisiteTechniqueService ficheVisiteTechniqueService;
    private final FicheVisiteTechniquePdfRenderer ficheVisiteTechniquePdfRenderer;
    private final FicheDossierVenteService ficheDossierVenteService;
    private final FicheDossierVentePdfRenderer ficheDossierVentePdfRenderer;
    private final FicheTropPercuService ficheTropPercuService;
    private final FicheTropPercuPdfRenderer ficheTropPercuPdfRenderer;
    private final PdfMerger pdfMerger;
    private final PerimetreCommercialService perimetreService;

    public DocumentLotService(FicheReservationService ficheReservationService,
                               FicheReservationPdfRenderer ficheReservationPdfRenderer,
                               ActeReservationModele acteReservationModele,
                               FicheBancaireService ficheBancaireService,
                               FicheBancairePdfRenderer ficheBancairePdfRenderer,
                               FicheVisiteTechniqueService ficheVisiteTechniqueService,
                               FicheVisiteTechniquePdfRenderer ficheVisiteTechniquePdfRenderer,
                               FicheDossierVenteService ficheDossierVenteService,
                               FicheDossierVentePdfRenderer ficheDossierVentePdfRenderer,
                               FicheTropPercuService ficheTropPercuService,
                               FicheTropPercuPdfRenderer ficheTropPercuPdfRenderer,
                               PdfMerger pdfMerger,
                               PerimetreCommercialService perimetreService) {
        this.perimetreService = perimetreService;
        this.ficheReservationService = ficheReservationService;
        this.ficheReservationPdfRenderer = ficheReservationPdfRenderer;
        this.acteReservationModele = acteReservationModele;
        this.ficheBancaireService = ficheBancaireService;
        this.ficheBancairePdfRenderer = ficheBancairePdfRenderer;
        this.ficheVisiteTechniqueService = ficheVisiteTechniqueService;
        this.ficheVisiteTechniquePdfRenderer = ficheVisiteTechniquePdfRenderer;
        this.ficheDossierVenteService = ficheDossierVenteService;
        this.ficheDossierVentePdfRenderer = ficheDossierVentePdfRenderer;
        this.ficheTropPercuService = ficheTropPercuService;
        this.ficheTropPercuPdfRenderer = ficheTropPercuPdfRenderer;
        this.pdfMerger = pdfMerger;
    }

    /** Écrit (première impression : {@code ContractPrintDate}) : hors lecture seule, donc sur la source. */
    @Transactional
    public byte[] ficheReservation(List<UUID> dossierIds) {
        return genererLot(dossierIds, id -> ficheReservationPdfRenderer.render(ficheReservationService.collect(id)));
    }

    public byte[] acteReservation(List<UUID> dossierIds) {
        return genererLot(dossierIds, id -> acteReservationModele.rendre(id, FormatDocument.PDF).contenu());
    }

    public byte[] ficheBancaire(List<UUID> dossierIds) {
        return genererLot(dossierIds, id -> ficheBancairePdfRenderer.render(ficheBancaireService.collect(id)));
    }

    public byte[] ficheVisiteTechnique(List<UUID> dossierIds) {
        return genererLot(dossierIds, id -> ficheVisiteTechniquePdfRenderer.render(ficheVisiteTechniqueService.collect(id)));
    }

    public byte[] ficheDossierVente(List<UUID> dossierIds) {
        return genererLot(dossierIds, id -> ficheDossierVentePdfRenderer.render(ficheDossierVenteService.collect(id)));
    }

    public byte[] ficheTropPercu(List<UUID> dossierIds) {
        return genererLot(dossierIds, id -> ficheTropPercuPdfRenderer.render(ficheTropPercuService.collect(id)));
    }

    /**
     * Échoue au premier id inconnu plutôt que de l'ignorer. Ids dans le <b>corps</b>, hors de portée
     * de {@code PerimetreInterceptor} (URL seulement) : le périmètre est vérifié à la main, 404 hors périmètre.
     */
    private byte[] genererLot(List<UUID> dossierIds, Function<UUID, byte[]> genererUnDocument) {
        if (dossierIds == null || dossierIds.isEmpty()) {
            throw new InvalidRequestException("dossierIds est requis et ne peut pas être vide");
        }
        List<byte[]> documents = new ArrayList<>(dossierIds.size());
        for (UUID dossierId : dossierIds) {
            perimetreService.verifierAccesDossier(dossierId);
            documents.add(genererUnDocument.apply(dossierId));
        }
        return pdfMerger.merge(documents);
    }
}
