package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dossier.ChangerStatutRequest;
import com.tsix.immocrm.dto.dossier.CreerDossierRequest;
import com.tsix.immocrm.dto.dossier.CreerEcheanceRequest;
import com.tsix.immocrm.dto.dossier.CreerReglementRequest;
import com.tsix.immocrm.dto.dossier.DossierDetailDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.dossier.EcheanceDto;
import com.tsix.immocrm.dto.dossier.ModifierEcheanceRequest;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.dto.dossier.ReglementDto;
import com.tsix.immocrm.dto.dossier.ActeReservationData;
import com.tsix.immocrm.dto.dossier.FicheReservationData;
import com.tsix.immocrm.dto.dossier.HistoriqueEvenementDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.services.dossier.ActeReservationService;
import com.tsix.immocrm.services.dossier.DossierHistoriqueService;
import com.tsix.immocrm.services.dossier.DossierService;
import com.tsix.immocrm.services.dossier.EcheancierService;
import com.tsix.immocrm.services.dossier.FicheReservationService;
import com.tsix.immocrm.services.dossier.PieceJointeService;
import com.tsix.immocrm.services.dossier.document.ActeReservationDocxRenderer;
import com.tsix.immocrm.services.dossier.document.ActeReservationPdfRenderer;
import com.tsix.immocrm.services.dossier.document.FicheReservationDocxRenderer;
import com.tsix.immocrm.services.dossier.document.FicheReservationPdfRenderer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/api/dossiers")
public class DossierController {

    private static final Map<String, MediaType> CONTENT_TYPES_PAR_EXTENSION = Map.ofEntries(
            Map.entry(".pdf", MediaType.APPLICATION_PDF),
            Map.entry(".jpg", MediaType.IMAGE_JPEG),
            Map.entry(".jpeg", MediaType.IMAGE_JPEG),
            Map.entry(".png", MediaType.IMAGE_PNG),
            Map.entry(".gif", MediaType.IMAGE_GIF),
            Map.entry(".doc", MediaType.valueOf("application/msword")),
            Map.entry(".docx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry(".xls", MediaType.valueOf("application/vnd.ms-excel")),
            Map.entry(".xlsx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry(".txt", MediaType.TEXT_PLAIN)
    );

    private final DossierService dossierService;
    private final EcheancierService echeancierService;
    private final PieceJointeService pieceJointeService;
    private final FicheReservationService ficheReservationService;
    private final FicheReservationPdfRenderer ficheReservationPdfRenderer;
    private final FicheReservationDocxRenderer ficheReservationDocxRenderer;
    private final ActeReservationService acteReservationService;
    private final ActeReservationPdfRenderer acteReservationPdfRenderer;
    private final ActeReservationDocxRenderer acteReservationDocxRenderer;
    private final DossierHistoriqueService historiqueService;

    public DossierController(DossierService dossierService, EcheancierService echeancierService,
                              PieceJointeService pieceJointeService, FicheReservationService ficheReservationService,
                              FicheReservationPdfRenderer ficheReservationPdfRenderer,
                              FicheReservationDocxRenderer ficheReservationDocxRenderer,
                              ActeReservationService acteReservationService,
                              ActeReservationPdfRenderer acteReservationPdfRenderer,
                              ActeReservationDocxRenderer acteReservationDocxRenderer,
                              DossierHistoriqueService historiqueService) {
        this.dossierService = dossierService;
        this.echeancierService = echeancierService;
        this.pieceJointeService = pieceJointeService;
        this.ficheReservationService = ficheReservationService;
        this.ficheReservationPdfRenderer = ficheReservationPdfRenderer;
        this.ficheReservationDocxRenderer = ficheReservationDocxRenderer;
        this.acteReservationService = acteReservationService;
        this.acteReservationPdfRenderer = acteReservationPdfRenderer;
        this.acteReservationDocxRenderer = acteReservationDocxRenderer;
        this.historiqueService = historiqueService;
    }

    @GetMapping
    public PageDto<DossierListItemDto> findAll(@RequestParam(required = false) String statut,
            @RequestParam(required = false) UUID commercial,
            @RequestParam(required = false) UUID projet,
            @RequestParam(required = false) String search,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(dossierService.findAll(statut, commercial, projet, search, pageable));
    }

    @GetMapping("/{id}")
    public DossierDetailDto findById(@PathVariable UUID id) {
        return dossierService.findById(id);
    }

    @PostMapping
    public ResponseEntity<DossierDetailDto> create(@RequestBody CreerDossierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dossierService.create(request));
    }

    @PatchMapping("/{id}/statut")
    public DossierDetailDto changerStatut(@PathVariable UUID id, @RequestBody ChangerStatutRequest request) {
        return dossierService.changerStatut(id, request);
    }

    @GetMapping("/{id}/statut/etapes-suivantes")
    public List<com.tsix.immocrm.dto.dossier.EtapeSuivanteDto> getEtapesSuivantes(@PathVariable UUID id) {
        return dossierService.etapesSuivantes(id);
    }

    /**
     * Unified, chronological, traceable history of the Dossier: status changes (with note),
     * échéances created, règlements posted, pièces jointes added. See
     * {@link DossierHistoriqueService}.
     */
    @GetMapping("/{id}/historique")
    public PageDto<HistoriqueEvenementDto> getHistorique(@PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(historiqueService.findHistorique(id, pageable));
    }

    @GetMapping("/{id}/echeancier")
    public PageDto<EcheanceDto> findEcheances(@PathVariable UUID id,
                                                @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(echeancierService.findEcheances(id, pageable));
    }

    @PostMapping("/{id}/echeancier")
    public ResponseEntity<EcheanceDto> addEcheance(@PathVariable UUID id, @RequestBody CreerEcheanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(echeancierService.addEcheance(id, request));
    }

    @PatchMapping("/{id}/echeancier/{echeanceId}")
    public EcheanceDto updateEcheance(@PathVariable UUID id, @PathVariable UUID echeanceId,
                                       @RequestBody ModifierEcheanceRequest request) {
        return echeancierService.updateEcheance(id, echeanceId, request);
    }

    @PostMapping("/{id}/echeancier/{echeanceId}/reglements")
    public ResponseEntity<ReglementDto> addReglement(@PathVariable UUID id, @PathVariable UUID echeanceId,
                                                       @RequestBody CreerReglementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(echeancierService.addReglement(id, echeanceId, request));
    }

    @GetMapping("/{id}/montants")
    public MontantsDossierDto montants(@PathVariable UUID id) {
        return dossierService.montants(id);
    }

    // -- Pièces jointes (AdmAttachedFile — see PieceJointeService for the confirmed linkage) --

    @GetMapping("/{id}/pieces-jointes")
    public List<PieceJointeDto> getPiecesJointes(@PathVariable UUID id) {
        return pieceJointeService.findByDossier(id);
    }

    @GetMapping("/{id}/pieces-jointes/{pieceId}/fichier")
    public ResponseEntity<byte[]> getFichier(@PathVariable UUID id, @PathVariable UUID pieceId) {
        AdmAttachedFile fichier = pieceJointeService.findFichier(id, pieceId);
        String extension = fichier.getExtension() != null ? fichier.getExtension().toLowerCase(Locale.ROOT) : "";
        MediaType contentType = CONTENT_TYPES_PAR_EXTENSION.getOrDefault(extension, MediaType.APPLICATION_OCTET_STREAM);
        String nomFichier = (fichier.getDescription() != null ? fichier.getDescription() : "piece-jointe") + extension;

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier).build().toString())
                .body(fichier.getAttachedFile());
    }

    // -- Génération de documents ("Fiche de réservation" pour l'instant — voir instructions/templates) --

    @GetMapping("/{id}/documents/fiche-reservation")
    public ResponseEntity<byte[]> getFicheReservation(@PathVariable UUID id,
            @RequestParam(defaultValue = "pdf") String format) {
        FicheReservationData data = ficheReservationService.collect(id);
        String nomFichier = "fiche-reservation-" + (data.dossierCode() != null ? data.dossierCode() : id);
        return documentResponse(format, nomFichier,
                () -> ficheReservationPdfRenderer.render(data),
                () -> ficheReservationDocxRenderer.render(data));
    }

    @GetMapping("/{id}/documents/acte-reservation")
    public ResponseEntity<byte[]> getActeReservation(@PathVariable UUID id,
            @RequestParam(defaultValue = "pdf") String format) {
        ActeReservationData data = acteReservationService.collect(id);
        String nomFichier = "acte-reservation-" + (data.dossierCode() != null ? data.dossierCode() : id);
        return documentResponse(format, nomFichier,
                () -> acteReservationPdfRenderer.render(data),
                () -> acteReservationDocxRenderer.render(data));
    }

    /** Shared PDF/Word response builder for the "Génération de documents" endpoints above. */
    private static ResponseEntity<byte[]> documentResponse(String format, String nomFichier,
            java.util.function.Supplier<byte[]> pdfSupplier, java.util.function.Supplier<byte[]> docxSupplier) {
        if ("docx".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename(nomFichier + ".docx").build().toString())
                    .body(docxSupplier.get());
        }
        if (!"pdf".equalsIgnoreCase(format)) {
            throw new com.tsix.immocrm.exceptions.InvalidRequestException(
                    "format doit être 'pdf' ou 'docx' (reçu: " + format + ")");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(nomFichier + ".pdf").build().toString())
                .body(pdfSupplier.get());
    }
}
