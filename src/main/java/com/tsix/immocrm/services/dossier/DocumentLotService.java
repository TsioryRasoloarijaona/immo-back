package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.dossier.document.ActeReservationPdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheBancairePdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheDossierVentePdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheReservationPdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheTropPercuPdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheVisiteTechniquePdfRenderer;
import com.tsix.immocrm.services.dossier.document.PdfMerger;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * {@code POST /api/dossiers/documents/{type}}: "génération en lot" for every dossier-scoped
 * document generator except "Reçu de règlement" (scoped to one règlement, not one dossier — see
 * {@link com.tsix.immocrm.dto.dossier.GenererDocumentsLotRequest}'s javadoc). Each method here
 * mirrors one single-dossier endpoint already on {@code DossierController}: same
 * {@code xxxService.collect(id)} + {@code xxxPdfRenderer.render(data)} pipeline, called once per
 * dossier, then concatenated into a single PDF via {@link PdfMerger} — never re-implements the
 * per-dossier rendering logic itself.
 */
@Service
@Transactional(readOnly = true)
public class DocumentLotService {

    private final FicheReservationService ficheReservationService;
    private final FicheReservationPdfRenderer ficheReservationPdfRenderer;
    private final ActeReservationService acteReservationService;
    private final ActeReservationPdfRenderer acteReservationPdfRenderer;
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
                               ActeReservationService acteReservationService,
                               ActeReservationPdfRenderer acteReservationPdfRenderer,
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
        this.acteReservationService = acteReservationService;
        this.acteReservationPdfRenderer = acteReservationPdfRenderer;
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

    public byte[] ficheReservation(List<UUID> dossierIds) {
        return genererLot(dossierIds, id -> ficheReservationPdfRenderer.render(ficheReservationService.collect(id)));
    }

    public byte[] acteReservation(List<UUID> dossierIds) {
        return genererLot(dossierIds, id -> acteReservationPdfRenderer.render(acteReservationService.collect(id)));
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
     * Fails on the first unknown/invalid dossier id (via the underlying {@code xxxService.collect}'s
     * own {@code ResourceNotFoundException}) rather than silently skipping it — a batch print run
     * with a missing dossier should be reported, not quietly short one page group.
     *
     * <p>Périmètre commercial : ces endpoints reçoivent leurs ids dans le <b>corps</b> de la requête,
     * hors de portée de {@code PerimetreInterceptor} (qui ne lit que l'URL) — c'est le seul endroit
     * de l'application où la vérification d'accès par id doit être appelée à la main. Un dossier hors
     * périmètre y est traité comme inexistant (404), exactement comme sur le endpoint unitaire.
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
