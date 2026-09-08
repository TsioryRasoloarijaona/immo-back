package com.tsix.immocrm.controller.auth;

import com.tsix.immocrm.dto.auth.CreerEmployeRequest;
import com.tsix.immocrm.dto.auth.CreerEmployeResponse;
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
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

import static com.tsix.immocrm.web.PaginationDefaults.DEFAULT_PAGE_SIZE;

@Tag(name = "Utilisateurs (admin)", description = "Actions administratives sur les comptes "
        + "AdmUser — réservées au rôle Administrateur : liste des comptes, création d'employé, "
        + "réinitialisation de mot de passe, et affectation des projets qui composent le périmètre "
        + "commercial d'un utilisateur.")
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
            summary = "Réinitialise le mot de passe d'un utilisateur",
            description = "Génère un mot de passe temporaire, **l'envoie par email à "
                    + "l'utilisateur**, le renvoie une seule fois dans la réponse, et force son "
                    + "changement à la prochaine connexion (mustChangePassword=true). Invalide "
                    + "immédiatement tous les tokens déjà émis pour ce compte, sur tous les "
                    + "appareils. Crée l'enregistrement d'identifiants web si ce compte legacy "
                    + "(AdmUser) ne s'était encore jamais connecté à l'application web.\n\n"
                    + "**Le compte doit avoir une adresse email, sinon 400.** L'adresse est "
                    + "cherchée sur `AdmUser.Email` puis, à défaut, sur la fiche employé liée "
                    + "(`CfgTier`). Le refus intervient **avant toute écriture** : aucun mot de "
                    + "passe n'est généré et les sessions en cours ne sont pas invalidées, le "
                    + "compte reste donc strictement intact. Cas fréquent sur les comptes "
                    + "hérités — au 08/09/2026, 5 des 15 comptes actifs sont dans ce cas.\n\n"
                    + "L'envoi est **asynchrone** et postérieur au commit : `emailEnvoye=true` "
                    + "signifie qu'il a été déclenché sans obstacle, pas qu'il a été remis. "
                    + "C'est pourquoi `motDePasseTemporaire` reste dans la réponse et doit "
                    + "continuer d'être affiché à l'administrateur. Réservé au rôle "
                    + "Administrateur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe temporaire généré et envoi déclenché."),
            @ApiResponse(responseCode = "400", description = "Le compte n'a aucune adresse email "
                    + "(ni sur AdmUser, ni sur sa fiche employé) : le mot de passe ne peut être "
                    + "envoyé. Le message serveur nomme le remède — renseigner l'email de "
                    + "l'utilisateur — et peut être affiché tel quel."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "404", description = "Aucun AdmUser avec cet id.")
    })
    public ReinitialiserMotDePasseResponse reinitialiserMotDePasse(
            @Parameter(description = "Id de l'AdmUser à réinitialiser.") @PathVariable UUID admUserId) {
        return utilisateurAdminService.reinitialiserMotDePasse(admUserId);
    }

    @PostMapping
    @PreAuthorize("hasRole('Administrateur')")
    @Operation(
            summary = "Crée un nouvel employé",
            description = "Crée en une seule opération la fiche personne (CfgTier), le compte de "
                    + "connexion (AdmUser) qui lui est lié, les rôles demandés (AdmUserRole), et "
                    + "génère un mot de passe temporaire **envoyé par email à l'employé** et "
                    + "renvoyé une seule fois dans la réponse. Le compte devra changer ce mot de "
                    + "passe et compléter son profil à sa première connexion "
                    + "(mustChangePassword=true, profileCompleted=false — voir "
                    + "GET /api/mon-compte).\n\n"
                    + "**`email` est désormais réellement obligatoire et doit être une adresse "
                    + "valide**, alors qu'il était jusqu'ici accepté vide malgré sa mention "
                    + "REQUIRED : c'est à cette adresse qu'est expédié le mot de passe, et elle "
                    + "est écrite à la fois sur la fiche personne et sur le compte. Le "
                    + "formulaire doit donc la valider avant envoi.\n\n"
                    + "L'envoi est **asynchrone** et postérieur au commit : `emailEnvoye=true` "
                    + "signifie qu'il a été déclenché sans obstacle, pas qu'il a été remis. "
                    + "C'est pourquoi `motDePasseTemporaire` reste dans la réponse et doit "
                    + "continuer d'être affiché à l'administrateur. Réservé au rôle "
                    + "Administrateur."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employé créé et envoi déclenché."),
            @ApiResponse(responseCode = "400", description = "Champ requis manquant, `email` "
                    + "absent ou malformé, ou typeEmployeCode/rôle référençant un code inconnu "
                    + "en base."),
            @ApiResponse(responseCode = "403", description = "L'appelant n'a pas le rôle Administrateur."),
            @ApiResponse(responseCode = "409", description = "L'identifiant demandé est déjà utilisé "
                    + "par un autre compte.")
    })
    public ResponseEntity<CreerEmployeResponse> creerEmploye(@Valid @RequestBody CreerEmployeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(utilisateurAdminService.creerEmploye(request));
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
