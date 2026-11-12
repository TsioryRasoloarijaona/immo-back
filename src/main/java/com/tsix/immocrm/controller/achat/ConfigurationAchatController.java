package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.dto.achat.config.CatalogueChampsStatutDto;
import com.tsix.immocrm.dto.achat.config.ImageStatutDto;
import com.tsix.immocrm.dto.achat.config.StatutAchatConfigDto;
import com.tsix.immocrm.dto.achat.config.TransitionAchatDto;
import com.tsix.immocrm.dto.achat.config.TypeAchatDto;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.achat.config.ConfigurationAchatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Achat — configuration")
@RestController
@RequestMapping("/api/achat/config")
public class ConfigurationAchatController {

    private final ConfigurationAchatService service;

    public ConfigurationAchatController(ConfigurationAchatService service) {
        this.service = service;
    }

    @GetMapping("/statuts")
    @Operation(summary = "Statuts des pièces d'achat, par ordre puis code (lecture seule, tout compte connecté)")
    public List<StatutAchatConfigDto> statuts() {
        return service.statuts();
    }

    @GetMapping("/statuts/types")
    @Operation(summary = "Types d'état (AdmWorkFlowStateType) proposés par la fiche statut, par ordre")
    public List<TypeAchatDto> typesStatut() {
        return service.types();
    }

    @GetMapping("/statuts/types-documents")
    @Operation(summary = "Types de document (AdmAttachedFileType) proposés pour les documents à joindre, par libellé")
    public List<TypeAchatDto> typesDocuments() {
        return service.typesDocuments();
    }

    @GetMapping("/statuts/champs-obligatoires")
    @Operation(summary = "Catalogue des champs cochables comme obligatoires (pièce et fournisseur), dans l'ordre OMRAN")
    public CatalogueChampsStatutDto champsObligatoires() {
        return service.champsObligatoires();
    }

    @GetMapping("/images")
    @Operation(summary = "Bibliothèque d'images OMRAN (CfgImage), par code, sans les octets")
    public List<ImageStatutDto> images() {
        return service.images();
    }

    @GetMapping("/images/{id}/contenu")
    @Operation(summary = "Octets d'une image de la bibliothèque, type déduit de leur signature")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Les octets de l'image (PNG, JPEG, GIF, BMP ou binaire)."),
            @ApiResponse(responseCode = "404", description = "Image inconnue ou sans octets.", content = @Content)})
    public ResponseEntity<byte[]> contenuImage(@Parameter(description = "CfgImage.PKey.") @PathVariable UUID id) {
        byte[] octets = service.contenuImage(id)
                .orElseThrow(() -> new ResourceNotFoundException("Image introuvable : " + id));
        return ResponseEntity.ok().contentType(typeImage(octets)).body(octets);
    }

    @GetMapping("/transitions")
    @Operation(summary = "Transitions des pièces d'achat par gestion, avec rôles et usage réel (lecture seule)")
    public List<TransitionAchatDto> transitions(
            @Parameter(description = "true = seulement les transitions déjà empruntées (nombrePassages > 0).")
            @RequestParam(defaultValue = "false") boolean utilisees) {
        return service.transitions(utilisees);
    }

    private static MediaType typeImage(byte[] o) {
        if (commencePar(o, 0x89, 'P', 'N', 'G')) {
            return MediaType.IMAGE_PNG;
        }
        if (commencePar(o, 0xFF, 0xD8)) {
            return MediaType.IMAGE_JPEG;
        }
        if (commencePar(o, 'G', 'I', 'F')) {
            return MediaType.IMAGE_GIF;
        }
        return commencePar(o, 'B', 'M') ? MediaType.parseMediaType("image/bmp") : MediaType.APPLICATION_OCTET_STREAM;
    }

    private static boolean commencePar(byte[] o, int... signature) {
        if (o.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((o[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
