package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.services.modele.FormulairesBaridLot;
import com.tsix.immocrm.services.modele.GenerationModeleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Rapports")
@RestController
public class FormulairesBaridController {

    private static final String TRANCHES = "Tranches (répétable) ; absent = toutes les tranches visibles.";
    private static final String STATUTS = "Statuts de dossier (répétable, options du descripteur) ; absent = tous.";

    private final FormulairesBaridLot lot;

    public FormulairesBaridController(FormulairesBaridLot lot) {
        this.lot = lot;
    }

    @GetMapping("/api/rapports/{module}/{code}/formulaires")
    @Operation(summary = "Formulaires Barid Al-Maghrib du rapport n° 35 : un PDF, une page par destinataire")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF de toutes les pages, dans l'ordre de la section du hub.",
                    content = @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "400", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rapport non visible par l'appelant, ou clé d'API.",
                    content = @Content),
            @ApiResponse(responseCode = "404", content = @Content)
    })
    @Parameters({
            @Parameter(name = "trancheId", in = ParameterIn.QUERY, description = TRANCHES,
                    array = @ArraySchema(schema = @Schema(type = "string", format = "uuid"))),
            @Parameter(name = "statutId", in = ParameterIn.QUERY, description = STATUTS,
                    array = @ArraySchema(schema = @Schema(type = "string", format = "uuid")))
    })
    public ResponseEntity<byte[]> formulaires(
            @Parameter(description = "Module : seul `vente` porte ce rapport.", example = "vente") @PathVariable String module,
            @Parameter(description = "Code du rapport : seul `ComSaleFormulaireBaridMaroc`.",
                    example = "ComSaleFormulaireBaridMaroc") @PathVariable String code,
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, String> parametres) {
        GenerationModeleService.DocumentProduit pdf = lot.imprimer(module, code, parametres);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore().cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(pdf.nomFichier() + ".pdf").build().toString())
                .body(pdf.contenu());
    }
}
