package com.tsix.immocrm.controller.prospect;

import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.dossier.DossierListItemDto;
import com.tsix.immocrm.dto.prospect.ContactProspectDto;
import com.tsix.immocrm.dto.prospect.CreerContactProspectRequest;
import com.tsix.immocrm.dto.prospect.CreerProspectRequest;
import com.tsix.immocrm.dto.prospect.HistoriqueDto;
import com.tsix.immocrm.dto.prospect.ModifierProspectRequest;
import com.tsix.immocrm.domain.ColonneProspect;
import com.tsix.immocrm.dto.prospect.ProspectDetailDto;
import com.tsix.immocrm.dto.prospect.ProspectListItemDto;
import com.tsix.immocrm.dto.prospect.ReglementProspectDto;
import com.tsix.immocrm.dto.prospection.ProspectionListItemDto;
import com.tsix.immocrm.services.prospect.ProspectService;
import com.tsix.immocrm.services.prospect.ProspectSubResourcesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@RestController
@RequestMapping("/api/prospects")
public class ProspectController {

    private final ProspectService prospectService;
    private final ProspectSubResourcesService subResourcesService;

    public ProspectController(ProspectService prospectService, ProspectSubResourcesService subResourcesService) {
        this.prospectService = prospectService;
        this.subResourcesService = subResourcesService;
    }

    @GetMapping
    public PageDto<ProspectListItemDto> findAll(@RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> type,
            @Parameter(description = "Colonnes a serialiser, repetable ou separees par des "
                    + "virgules. Absent : les 10 colonnes historiques. Catalogue et liste blanche : "
                    + "`GET /api/referentiels/colonnes/prospects`. Un nom hors catalogue rend 400 "
                    + "avec la liste des noms valides. `id` est toujours servi.",
                    example = "code,nom,prenom,cin,creePar")
            @RequestParam(required = false) List<String> colonnes,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "userCreateDateTime", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(prospectService.findAll(search, type, pageable, ColonneProspect.resoudre(colonnes)));
    }

    @GetMapping("/{id}")
    public ProspectDetailDto findById(@PathVariable UUID id) {
        return prospectService.findById(id);
    }

    @PostMapping
    @Operation(
            summary = "Crée un prospect",
            description = "`nom` ou `raisonSociale` est requis ; tout le reste est optionnel.\n\n"
                    + "**Un numéro de téléphone ne s'enregistre jamais seul.** Fournir `mobile` "
                    + "impose `indicatifMobile`, fournir `telephone` impose "
                    + "`indicatifTelephone` — sinon **400**. L'indicatif est l'indicatif pays "
                    + "sans le `+` (`\"212\"`, le `+` étant toléré et retiré), et il doit exister "
                    + "dans le référentiel : voir GET /api/referentiels/pays, champ "
                    + "`indicatifTelephone`. C'est ce contrôle qui rend l'envoi WhatsApp fiable "
                    + "en aval — un numéro sans indicatif est inenvoyable, un indicatif fautif "
                    + "expédie le message à un inconnu.\n\n"
                    + "**Le numéro est normalisé avant écriture**, au format que porte déjà la "
                    + "base : 9 chiffres deviennent `6 98 29 80 95` (espacé, zéro initial retiré) "
                    + "— la forme de 98,8 % des fiches existantes ; toute autre longueur est "
                    + "stockée en chiffres nus, comme le sont déjà les mobiles nord-américains à "
                    + "10 chiffres. La valeur relue peut donc différer de celle envoyée."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prospect créé."),
            @ApiResponse(responseCode = "400", description = "`nom`/`raisonSociale` absents ; "
                    + "numéro fourni sans son indicatif, ou indicatif seul sans numéro ; "
                    + "indicatif absent du référentiel ; numéro hors de 6–14 chiffres ; "
                    + "référence (type, pays, profession...) inconnue.")
    })
    public ResponseEntity<ProspectDetailDto> create(@RequestBody CreerProspectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prospectService.create(request));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Modifie un prospect",
            description = "Mise à jour partielle : un champ absent ou `null` est laissé "
                    + "inchangé.\n\n"
                    + "**Les mêmes règles de téléphone qu'à la création s'appliquent** (voir "
                    + "POST /api/prospects), avec une conséquence propre à la modification : "
                    + "l'indicatif **n'est jamais repris implicitement** de la fiche existante. "
                    + "Modifier `mobile` sans envoyer `indicatifMobile` renvoie 400 même si la "
                    + "fiche en portait déjà un — un numéro qui change peut changer de pays, et "
                    + "hériter de l'ancien indicatif fabriquerait un numéro qui n'existe pas.\n\n"
                    + "En revanche, une modification qui **ne touche pas** au téléphone ne "
                    + "demande aucun indicatif : les fiches historiques dont l'indicatif manque "
                    + "restent modifiables sur leurs autres champs."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospect mis à jour."),
            @ApiResponse(responseCode = "400", description = "Mêmes causes qu'à la création."),
            @ApiResponse(responseCode = "404", description = "Aucun prospect avec cet id.")
    })
    public ProspectDetailDto update(@PathVariable UUID id, @RequestBody ModifierProspectRequest request) {
        return prospectService.update(id, request);
    }

    // -- GAP-06: prospect fiche sub-resources --
    //
    // Les trois listes ci-dessous renvoient exactement les mêmes DTO que GET /api/prospections,
    // GET /api/dossiers et GET /api/encaissements : elles sont donc filtrées par le périmètre
    // commercial au même titre, bien qu'elles vivent sous /api/prospects. Le prospect lui-même
    // n'est pas filtré (voir §9bis.2 de instructions/FEATURE-perimetre-commercial.md).

    @GetMapping("/{id}/prospections")
    @Operation(
            summary = "Prospections d'un prospect",
            description = "Mêmes `ProspectionListItemDto` que GET /api/prospections.\n\n"
                    + "**Périmètre commercial** : si l'appelant est restreint, seules les "
                    + "prospections de ses projets affectés sont renvoyées. Le prospect, lui, "
                    + "n'est pas filtré — sa fiche reste accessible et la liste peut simplement "
                    + "revenir vide ou partielle. `totalElements` reflète le nombre filtré."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prospections du prospect (éventuellement vide)."),
            @ApiResponse(responseCode = "404", description = "Aucun prospect avec cet id.")
    })
    public PageDto<ProspectionListItemDto> getProspections(
            @Parameter(description = "Identifiant du prospect (CfgTier.PKey).")
            @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(subResourcesService.prospections(id, pageable));
    }

    @GetMapping("/{id}/dossiers")
    @Operation(
            summary = "Dossiers de vente d'un prospect",
            description = "Mêmes `DossierListItemDto` que GET /api/dossiers.\n\n"
                    + "**Périmètre commercial** : si l'appelant est restreint, seuls les dossiers "
                    + "de ses projets affectés sont renvoyés ; le prospect lui-même n'est pas "
                    + "filtré. `totalElements` reflète le nombre filtré."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dossiers du prospect (éventuellement vide)."),
            @ApiResponse(responseCode = "404", description = "Aucun prospect avec cet id.")
    })
    public PageDto<DossierListItemDto> getDossiers(
            @Parameter(description = "Identifiant du prospect (CfgTier.PKey).")
            @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "docDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageDto.of(subResourcesService.dossiers(id, pageable));
    }

    @GetMapping("/{id}/reglements")
    @Operation(
            summary = "Règlements d'un prospect, tous dossiers confondus",
            description = "Triés par date de pièce décroissante par défaut.\n\n"
                    + "**Périmètre commercial** : si l'appelant est restreint, seuls les "
                    + "règlements rattachés à un dossier de ses projets affectés sont renvoyés "
                    + "(plus ceux dont aucune ligne ne résout de projet) ; le prospect lui-même "
                    + "n'est pas filtré. `totalElements` reflète le nombre filtré."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Règlements du prospect (éventuellement vide)."),
            @ApiResponse(responseCode = "404", description = "Aucun prospect avec cet id.")
    })
    public PageDto<ReglementProspectDto> getReglements(
            @Parameter(description = "Identifiant du prospect (CfgTier.PKey).")
            @PathVariable UUID id,
            @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(subResourcesService.reglements(id, pageable));
    }

    @GetMapping("/{id}/historique")
    public PageDto<HistoriqueDto> getHistorique(@PathVariable UUID id,
                                                 @PageableDefault(size = DEFAULT_PAGE_SIZE) Pageable pageable) {
        return PageDto.of(subResourcesService.historique(id, pageable));
    }

    @GetMapping("/{id}/contacts")
    public List<ContactProspectDto> getContacts(@PathVariable UUID id) {
        return subResourcesService.contacts(id);
    }

    @PostMapping("/{id}/contacts")
    public ContactProspectDto ajouterContact(@PathVariable UUID id, @RequestBody CreerContactProspectRequest request) {
        return subResourcesService.ajouterContact(id, request);
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    public ResponseEntity<Void> retirerContact(@PathVariable UUID id, @PathVariable UUID contactId) {
        subResourcesService.retirerContact(id, contactId);
        return ResponseEntity.noContent().build();
    }
}
