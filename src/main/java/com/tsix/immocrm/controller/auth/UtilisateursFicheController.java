package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.dto.auth.ComptesDepotEcritureDto;
import com.tsix.immocrm.dto.auth.FiltresUtilisateurs;
import com.tsix.immocrm.dto.auth.HabilitationsEcritureDto;
import com.tsix.immocrm.dto.auth.NoeudHabilitationDto;
import com.tsix.immocrm.dto.auth.RolesUtilisateurEcritureDto;
import com.tsix.immocrm.dto.auth.UtilisateurEcritureDto;
import com.tsix.immocrm.dto.auth.UtilisateurFicheDto;
import com.tsix.immocrm.dto.auth.UtilisateurReferencesDto;
import com.tsix.immocrm.services.auth.HabilitationsUtilisateurService;
import com.tsix.immocrm.services.auth.RolesUtilisateurService;
import com.tsix.immocrm.services.auth.UtilisateurAdminService;
import com.tsix.immocrm.services.auth.UtilisateursService;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.FeuilleExport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Utilisateurs (admin)")
@ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur.", content = @Content)
@RestController
@RequestMapping("/api/utilisateurs")
@PreAuthorize("hasRole('Administrateur')")
public class UtilisateursFicheController {

    private static final String FICHE = "/{id}";
    private static final String ID = "PKey du compte AdmUser.";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String INTROUVABLE = "Compte inconnu.";
    private static final String GARDE = "Soi-même, ou dernier Administrateur actif : rien n'est écrit, message à afficher tel quel.";
    private static final String CHEMIN_HABILITATIONS = FICHE + "/habilitations";

    private static final List<ColonneFixe> COLONNES = List.of(ColonneFixe.texte("username", "Code"),
            ColonneFixe.texte("description", "Description"), ColonneFixe.texte("email", "Email"),
            ColonneFixe.texte("mobile", "Mobile"), ColonneFixe.de("disabled", "Inactif", TypeExport.BOOLEEN),
            ColonneFixe.texte("creePar", "Créé par"), ColonneFixe.de("dateCreation", "Créé le", TypeExport.DATE),
            ColonneFixe.texte("modifiePar", "Modifié par"), ColonneFixe.de("modifieLe", "Modifié le", TypeExport.DATE));

    private final UtilisateursService service;
    private final RolesUtilisateurService roles;
    private final HabilitationsUtilisateurService habilitations;
    private final UtilisateurAdminService liste;
    private final ExportTableur export;

    public UtilisateursFicheController(UtilisateursService service, RolesUtilisateurService roles,
                                       HabilitationsUtilisateurService habilitations, UtilisateurAdminService liste,
                                       ExportTableur export) {
        this.service = service;
        this.roles = roles;
        this.habilitations = habilitations;
        this.liste = liste;
        this.export = export;
    }

    @GetMapping("/export")
    @Operation(summary = "Export Excel des comptes, mêmes filtres que la liste")
    @ApiResponse(responseCode = "200", description = "Classeur .xlsx.", content = @Content(mediaType = XLSX,
            schema = @Schema(type = "string", format = "binary")))
    public ResponseEntity<byte[]> exporter(@ParameterObject @ModelAttribute FiltresUtilisateurs filtres) {
        return export.reponseEnMemoire("utilisateurs", "Utilisateurs", List.of(new FeuilleExport(ExportTableur.FEUILLE,
                COLONNES, liste.lignesExport(filtres), null)));
    }

    @GetMapping("/references")
    @Operation(summary = "Rôles, cultures web, personnels et comptes de dépôt pour la fiche")
    @ApiResponse(responseCode = "200", description = "Listes de choix.")
    public UtilisateurReferencesDto references() {
        return service.references();
    }

    @GetMapping(FICHE)
    @Operation(summary = "Fiche d'un compte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Le compte."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public UtilisateurFicheDto detail(@Parameter(description = ID) @PathVariable UUID id) {
        return service.detail(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crée un compte, inactif sauf inactif:false, sans mot de passe")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé."),
            @ApiResponse(responseCode = "400", description = "Code ou description invalide, personnel, rôle ou culture inconnu.",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Code déjà pris, casse ignorée.", content = @Content)
    })
    public UtilisateurFicheDto creer(@RequestBody UtilisateurEcritureDto corps) {
        return service.creer(corps);
    }

    @PatchMapping(FICHE)
    @Operation(summary = "Modifie un compte (null = inchangé, \"\" = vidé pour les textes, code ignoré)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche relue. Sans effet : rien n'est écrit."),
            @ApiResponse(responseCode = "400", description = "Description vide ou trop longue, ou référence inconnue.",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = GARDE, content = @Content)
    })
    public UtilisateurFicheDto modifier(@Parameter(description = ID) @PathVariable UUID id,
                                        @RequestBody UtilisateurEcritureDto corps) {
        return service.modifier(id, corps);
    }

    @PutMapping(FICHE + "/roles")
    @Operation(summary = "Remplace les rôles : écrit la différence, un changement coupe la session du compte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche relue."),
            @ApiResponse(responseCode = "400", description = "roleIds absent, ou rôle inconnu.", content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content),
            @ApiResponse(responseCode = "409", description = GARDE, content = @Content)
    })
    public UtilisateurFicheDto remplacerRoles(@Parameter(description = ID) @PathVariable UUID id,
                                              @RequestBody RolesUtilisateurEcritureDto corps) {
        return roles.remplacerRoles(id, corps);
    }

    @PutMapping(FICHE + "/comptes-depot")
    @Operation(summary = "Remplace les comptes de dépôt : écrit la différence")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche relue."),
            @ApiResponse(responseCode = "400", description = "compteIds absent, ou compte inconnu.", content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public UtilisateurFicheDto remplacerComptesDepot(@Parameter(description = ID) @PathVariable UUID id,
                                                     @RequestBody ComptesDepotEcritureDto corps) {
        return roles.remplacerComptesDepot(id, corps);
    }

    @GetMapping(CHEMIN_HABILITATIONS)
    @Operation(summary = "Arbre des habilitations du compte")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Les sociétés et leurs descendants."),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public List<NoeudHabilitationDto> lireHabilitations(@Parameter(description = ID) @PathVariable UUID id) {
        return habilitations.arbre(id);
    }

    @PutMapping(CHEMIN_HABILITATIONS)
    @Operation(summary = "Remplace les habilitations à une cible ; les lignes à plusieurs cibles restent")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Arbre relu."),
            @ApiResponse(responseCode = "400", description = "noeuds absent, ou nœud inconnu.", content = @Content),
            @ApiResponse(responseCode = "404", description = INTROUVABLE, content = @Content)
    })
    public List<NoeudHabilitationDto> remplacerHabilitations(@Parameter(description = ID) @PathVariable UUID id,
                                                             @RequestBody HabilitationsEcritureDto corps) {
        return habilitations.remplacer(id, corps);
    }
}
