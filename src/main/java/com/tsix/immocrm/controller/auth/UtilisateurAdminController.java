package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.dto.auth.ReinitialiserMotDePasseRequest;
import com.tsix.immocrm.dto.auth.ReinitialiserMotDePasseResponse;
import com.tsix.immocrm.dto.auth.UtilisateurListItemDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.dto.perimetre.AffecterProjetsRequest;
import com.tsix.immocrm.dto.perimetre.ProjetPerimetreDto;
import com.tsix.immocrm.services.auth.UtilisateurAdminService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Utilisateurs (admin)", description = "Actions administratives sur les comptes "
        + "AdmUser — réservées au rôle Administrateur : liste des comptes, réinitialisation du mot "
        + "de passe Arpent, et affectation des projets qui composent le périmètre commercial d'un "
        + "utilisateur. Les comptes eux-mêmes se créent dans Omran, jamais ici.")
@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurAdminController {

    private final UtilisateurAdminService utilisateurAdminService;
    private final PerimetreCommercialService perimetreService;

    public UtilisateurAdminController(UtilisateurAdminService utilisateurAdminService,
                                       PerimetreCommercialService perimetreService) {
        this.utilisateurAdminService = utilisateurAdminService;
        this.perimetreService = perimetreService;
    }

    @GetMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(
            summary = "Liste les comptes utilisateurs",
            description = "Tous les comptes (AdmUser) avec leurs informations (identifiant, "
                    + "coordonnées, statut activé/désactivé), la fiche employé (CfgTier) liée "
                    + "et les rôles (AdmRole) assignés — écran d'administration des comptes. "
                    + "`search` filtre (insensible à la casse, sous-chaîne) sur "
                    + "identifiant/description/email. `disabled` filtre sur le statut activé/"
                    + "désactivé ; absent = tous les comptes. Réservé au rôle Administrateur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste (éventuellement vide) des comptes."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur.")
    })
    public PageDto<UtilisateurListItemDto> findAll(
            @Parameter(description = "Recherche sous-chaîne sur identifiant/description/email.")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filtre sur le statut désactivé. Absent = tous les comptes.")
            @RequestParam(required = false) Boolean disabled,
            @PageableDefault(size = DEFAULT_PAGE_SIZE, sort = "description", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return PageDto.of(utilisateurAdminService.findAll(search, disabled, pageable));
    }

    @PostMapping("/{admUserId}/reinitialiser-mot-de-passe")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(
            summary = "Réinitialise le mot de passe Arpent d'un compte Omran",
            description = "Les comptes se créent dans Omran (AdmUser, fiche employé, rôles) ; ce "
                    + "geste est le seul qui ouvre ou rouvre l'accès web. Génère **toujours** un "
                    + "nouveau mot de passe Arpent pour le login Omran (`AdmUser.Code`), le "
                    + "renvoie une seule fois dans la réponse, et rend le compte **utilisable "
                    + "immédiatement** : aucun changement de mot de passe ni formulaire de profil "
                    + "n'est demandé à la connexion (mustChangePassword=false, "
                    + "profileCompleted=true). Invalide tous les tokens déjà émis pour ce compte, "
                    + "sur tous les appareils (TokenVersion incrémenté). Crée l'enregistrement "
                    + "d'identifiants web si le compte ne s'était jamais connecté.\n\n"
                    + "**Destinataire** : `email` du corps s'il est fourni (pour cet envoi "
                    + "seulement, jamais enregistré), sinon `AdmUser.Email`, à défaut l'email de "
                    + "la fiche employé (`CfgTier`). **Un compte sans aucune adresse n'est plus "
                    + "refusé** : la réinitialisation a lieu, aucun email n'est envoyé "
                    + "(`emailEnvoye=false`, `destinataire=null`) et l'administrateur transmet "
                    + "lui-même les identifiants affichés. Le corps est facultatif (absent = `{}`).\n\n"
                    + "L'email contient l'identifiant, le mot de passe et le lien vers Arpent "
                    + "(sans identifiant ni mot de passe dans le lien). L'envoi est "
                    + "**asynchrone** et postérieur au commit : `emailEnvoye=true` signifie qu'il "
                    + "a été déclenché, pas qu'il a été remis — `motDePasseTemporaire` doit donc "
                    + "toujours être affiché. Réservé au rôle Administrateur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe généré ; email déclenché "
                    + "si un destinataire existe et que le service d'envoi est actif."),
            @ApiResponse(responseCode = "400", description = "`email` fourni dans le corps mais "
                    + "invalide (pas de forme local@domaine.tld). Rien n'est modifié : pas de "
                    + "nouveau mot de passe, sessions conservées. Le message serveur peut être "
                    + "affiché tel quel sous le champ."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = "Aucun AdmUser avec cet id.")
    })
    public ReinitialiserMotDePasseResponse reinitialiserMotDePasse(
            @Parameter(description = "Id de l'AdmUser à réinitialiser.") @PathVariable UUID admUserId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false,
                    description = "Facultatif. `email` : adresse d'envoi pour cette fois seulement.")
            @RequestBody(required = false) ReinitialiserMotDePasseRequest request) {
        return utilisateurAdminService.reinitialiserMotDePasse(admUserId,
                request == null ? null : request.email());
    }

    // -- Périmètre commercial : affectation de projets (voir instructions/FEATURE-perimetre-commercial.md) --

    @GetMapping("/{admUserId}/projets")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(
            summary = "Projets affectés à un utilisateur (périmètre commercial)",
            description = "Les projets (CfgProject) qui composent le périmètre commercial de ce "
                    + "compte. Un utilisateur restreint ne voit que les prospections, "
                    + "pré-prospections, dossiers et encaissements rattachés à ces projets, et le "
                    + "tableau de bord n'agrège que ceux-ci — le filtrage est intégralement côté "
                    + "serveur. Liste vide = aucune restriction : ce compte voit tout (un périmètre "
                    + "vide ne signifie jamais \"ne voit rien\"). La liste des projets "
                    + "sélectionnables s'obtient par GET /api/projets (8 projets en tout, ni "
                    + "recherche ni pagination nécessaires).\n\n"
                    + "Renvoie les affectations **brutes**, pas le périmètre effectivement "
                    + "appliqué : un compte portant un rôle d'exemption (Administrateur, Direction "
                    + "Générale) continue de tout voir même si cette liste est non vide. Une liste "
                    + "non vide ne prouve donc pas qu'un compte est restreint — pour l'état "
                    + "réellement appliqué, voir `perimetreRestreint` sur GET /api/mon-compte. "
                    + "Réservé au rôle Administrateur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projets affectés (éventuellement vide)."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = "Aucun AdmUser avec cet id.")
    })
    public List<ProjetPerimetreDto> getProjets(
            @Parameter(description = "Id de l'AdmUser dont on consulte le périmètre.")
            @PathVariable UUID admUserId) {
        return perimetreService.projetsAffectes(admUserId);
    }

    @PutMapping("/{admUserId}/projets")
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(
            summary = "Remplace les projets affectés à un utilisateur (périmètre commercial)",
            description = "**Remplacement intégral** du périmètre projet : le corps porte l'état "
                    + "final des cases à cocher, pas un delta. `projetIds` est obligatoire ; "
                    + "envoyer une liste **vide** retire toute restriction à ce compte (il voit "
                    + "alors tout) et n'est pas une erreur, alors qu'un corps sans `projetIds` "
                    + "(`{}` ou `null`) est refusé en 400 — une levée de restriction doit être "
                    + "explicite, jamais la conséquence d'un corps tronqué. Prise en compte "
                    + "immédiate, sans reconnexion : le périmètre est relu en base à chaque "
                    + "requête et n'est jamais embarqué dans le jeton. Les identifiants répétés "
                    + "sont dédoublonnés.\n\n"
                    + "Seule la portée **projet** est remplacée. `AdmUserHabilitation` est "
                    + "polymorphe et une même ligne peut porter plusieurs portées à la fois : une "
                    + "ligne qui ne porte qu'un projet est supprimée, tandis qu'une ligne mixte "
                    + "(portant aussi une société, une tranche ou un projet-travaux) est "
                    + "**conservée**, seul son projet étant retiré. Aucune habilitation société / "
                    + "tranche / projet-travaux n'est donc jamais perdue — elles appartiennent à "
                    + "l'application de bureau légataire, toujours en service.\n\n"
                    + "Deux comportements à connaître côté écran d'affectation : les comptes "
                    + "portant un rôle d'exemption (Administrateur, Direction Générale) restent "
                    + "non filtrés même après affectation ; et le sélecteur d'utilisateurs devrait "
                    + "filtrer sur `disabled=false` (la grande majorité des comptes legacy sont "
                    + "désactivés). Réservé au rôle Administrateur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Périmètre remplacé — renvoie le nouvel état."),
            @ApiResponse(responseCode = "400", description = "`projetIds` absent ou null (envoyez "
                    + "`[]` pour retirer la restriction), ou au moins un `projetId` ne correspond "
                    + "à aucun CfgProject."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = "Aucun AdmUser avec cet id.")
    })
    public List<ProjetPerimetreDto> remplacerProjets(
            @Parameter(description = "Id de l'AdmUser dont on redéfinit le périmètre.")
            @PathVariable UUID admUserId,
            @RequestBody AffecterProjetsRequest request) {
        return perimetreService.remplacerProjets(admUserId, request.projetIds());
    }
}
