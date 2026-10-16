package com.tsix.immocrm.controller.modele;

import com.tsix.immocrm.dto.modele.ApercuModeleRequest;
import com.tsix.immocrm.dto.modele.CatalogueChampsDto;
import com.tsix.immocrm.dto.modele.CategorieModeleDto;
import com.tsix.immocrm.dto.modele.CreerModeleDocumentRequest;
import com.tsix.immocrm.dto.modele.ModeleDocumentAdminDto;
import com.tsix.immocrm.dto.modele.ModeleDocumentDetailDto;
import com.tsix.immocrm.dto.modele.ModeleDocumentResumeDto;
import com.tsix.immocrm.dto.modele.ModifierModeleDocumentRequest;
import com.tsix.immocrm.dto.modele.VersionModeleDto;
import com.tsix.immocrm.services.modele.FormatDocument;
import com.tsix.immocrm.services.modele.GenerationModeleService;
import com.tsix.immocrm.services.modele.ModeleDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Modèles de documents")
@RestController
@RequestMapping("/api/modeles-documents")
public class ModeleDocumentController {

    private static final String ADMIN = "hasRole('Administrateur')";
    private static final String NON_ADMIN = "L'appelant n'a pas le rôle Administrateur.";
    private static final String INTROUVABLE = "Aucun modèle avec cet id.";
    private static final String ID_MODELE = "Identifiant du modèle (AppModeleDocument.PKey).";
    private static final String CODE_CATEGORIE = "Code de la catégorie.";
    private static final String CATEGORIE_EXEMPLE = "DOSSIER_VENTE";
    private static final String CATEGORIE_INCONNUE = "Catégorie inconnue.";

    private final ModeleDocumentService service;
    private final GenerationModeleService generation;

    public ModeleDocumentController(ModeleDocumentService service, GenerationModeleService generation) {
        this.service = service;
        this.generation = generation;
    }

    @GetMapping("/categories")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Catégories de modèles disponibles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catégories (lot 1 : `DOSSIER_VENTE`)."),
            @ApiResponse(responseCode = "403", description = NON_ADMIN)
    })
    public List<CategorieModeleDto> categories() {
        return service.categories();
    }

    @GetMapping("/categories/{categorie}/champs")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Catalogue des champs d'une catégorie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Groupes de champs et listes répétables."),
            @ApiResponse(responseCode = "403", description = NON_ADMIN),
            @ApiResponse(responseCode = "404", description = CATEGORIE_INCONNUE)
    })
    public CatalogueChampsDto champs(
            @Parameter(description = CODE_CATEGORIE, example = CATEGORIE_EXEMPLE) @PathVariable String categorie) {
        return service.champs(categorie);
    }

    @GetMapping
    @Operation(summary = "Modèles actifs d'une catégorie, pour « Imprimer »")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modèles actifs triés par `ordre`, éventuellement vide."),
            @ApiResponse(responseCode = "400", description = CATEGORIE_INCONNUE)
    })
    public List<ModeleDocumentResumeDto> listerActifs(
            @Parameter(description = CODE_CATEGORIE, example = CATEGORIE_EXEMPLE) @RequestParam String categorie) {
        return service.listerActifs(categorie);
    }

    @GetMapping("/admin")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Tous les modèles, actifs et inactifs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modèles avec leur version courante."),
            @ApiResponse(responseCode = "400", description = CATEGORIE_INCONNUE),
            @ApiResponse(responseCode = "403", description = NON_ADMIN)
    })
    public List<ModeleDocumentAdminDto> listerTous(
            @Parameter(description = "Code de la catégorie ; absent = toutes.", example = CATEGORIE_EXEMPLE)
            @RequestParam(required = false) String categorie) {
        return service.listerTous(categorie);
    }

    @GetMapping("/{id}")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Modèle et contenu de sa version courante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modèle trouvé."),
            @ApiResponse(responseCode = "403", description = NON_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public ModeleDocumentDetailDto detail(
            @Parameter(description = ID_MODELE) @PathVariable UUID id) {
        return service.detail(id);
    }

    @PostMapping
    @PreAuthorize(ADMIN)
    @Operation(summary = "Crée un modèle et sa version 1")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Modèle créé."),
            @ApiResponse(responseCode = "400", description = "Champ requis manquant, code invalide, HTML hors liste blanche."),
            @ApiResponse(responseCode = "403", description = NON_ADMIN),
            @ApiResponse(responseCode = "409", description = "Un modèle de ce code existe déjà dans la catégorie.")
    })
    public ResponseEntity<ModeleDocumentDetailDto> creer(@RequestBody CreerModeleDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Modifie un modèle ; un nouveau contenu crée une nouvelle version")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Modèle modifié."),
            @ApiResponse(responseCode = "400", description = "Libellé invalide ou HTML hors liste blanche."),
            @ApiResponse(responseCode = "403", description = NON_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public ModeleDocumentDetailDto modifier(
            @Parameter(description = ID_MODELE) @PathVariable UUID id,
            @RequestBody ModifierModeleDocumentRequest request) {
        return service.modifier(id, request);
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Historique des versions d'un modèle, la plus récente d'abord")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versions du modèle."),
            @ApiResponse(responseCode = "403", description = NON_ADMIN),
            @ApiResponse(responseCode = "404", description = INTROUVABLE)
    })
    public List<VersionModeleDto> versions(
            @Parameter(description = ID_MODELE) @PathVariable UUID id) {
        return service.versions(id);
    }

    @PostMapping("/{id}/versions/{numero}/restaurer")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Recopie une ancienne version en nouvelle version courante")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nouvelle version créée, identique à la version restaurée."),
            @ApiResponse(responseCode = "403", description = NON_ADMIN),
            @ApiResponse(responseCode = "404", description = "Modèle ou version introuvable.")
    })
    public ModeleDocumentDetailDto restaurer(
            @Parameter(description = ID_MODELE) @PathVariable UUID id,
            @Parameter(description = "Numéro de la version à restaurer.", example = "1") @PathVariable int numero) {
        return service.restaurer(id, numero);
    }

    @PostMapping("/{id}/apercu")
    @PreAuthorize(ADMIN)
    @Operation(summary = "Aperçu PDF d'un contenu non enregistré, rempli avec une fiche")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF de l'aperçu ; rien n'est enregistré."),
            @ApiResponse(responseCode = "400", description = "`ficheId` manquant ou HTML hors liste blanche."),
            @ApiResponse(responseCode = "403", description = NON_ADMIN),
            @ApiResponse(responseCode = "404", description = "Modèle introuvable, ou fiche introuvable ou hors périmètre.")
    })
    public ResponseEntity<byte[]> apercu(
            @Parameter(description = ID_MODELE) @PathVariable UUID id,
            @RequestBody ApercuModeleRequest request) {
        return document(generation.apercu(id, request), FormatDocument.PDF, "apercu");
    }

    @GetMapping("/{id}/generer")
    @Operation(summary = "Génère le document d'une fiche depuis un modèle actif")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document généré."),
            @ApiResponse(responseCode = "400", description = "`format` autre que `pdf` ou `docx`."),
            @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inactif, ou fiche hors périmètre.")
    })
    public ResponseEntity<byte[]> generer(
            @Parameter(description = ID_MODELE) @PathVariable UUID id,
            @Parameter(description = "Fiche source (pour `DOSSIER_VENTE` : ComFolder.PKey).") @RequestParam UUID ficheId,
            @Parameter(description = "`pdf` (défaut) ou `docx`.") @RequestParam(defaultValue = "pdf") String format) {
        FormatDocument sortie = FormatDocument.depuis(format);
        GenerationModeleService.DocumentProduit produit = generation.produire(id, ficheId, sortie);
        return document(produit.contenu(), sortie, produit.nomFichier());
    }

    private static ResponseEntity<byte[]> document(byte[] contenu, FormatDocument format, String nom) {
        ContentDisposition disposition = format == FormatDocument.PDF
                ? ContentDisposition.inline().filename(nom + ".pdf").build()
                : ContentDisposition.attachment().filename(nom + "." + format.extension()).build();
        MediaType type = format.type();
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(contenu);
    }
}
