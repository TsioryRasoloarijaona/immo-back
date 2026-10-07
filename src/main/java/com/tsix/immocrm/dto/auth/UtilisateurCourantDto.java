package com.tsix.immocrm.dto.auth;

import com.tsix.immocrm.dto.perimetre.ProjetPerimetreDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Le compte authentifié — renvoyé par POST /api/auth/connexion et "
        + "GET /api/mon-compte.")
public record UtilisateurCourantDto(

        UUID id,

        @Schema(description = "Nom d'utilisateur (AdmUser.Code).")
        String username,

        String description,

        String email,

        @Schema(description = "Id de la fiche employé/commercial (CfgTier) liée, si connue.")
        UUID employeeId,

        @Schema(description = "Codes des rôles assignés (AdmRole.Code), ex: [\"Commercial\", \"Administrateur\"].")
        List<String> roles,

        @Schema(description = "Drapeau hérité (AppUserCredential.MustChangePassword), conservé "
                + "pour le contrat. **Ne bloque plus rien** et le front l'ignore : il n'y a plus "
                + "de changement de mot de passe imposé à la connexion. Une réinitialisation par "
                + "un administrateur le remet à false.")
        boolean mustChangePassword,

        @Schema(description = "Drapeau hérité (AppUserCredential.ProfileCompleted), conservé "
                + "pour le contrat. **Ne bloque plus rien** et le front l'ignore : les informations "
                + "du compte viennent d'Omran. Une réinitialisation par un administrateur le met "
                + "à true.")
        boolean profileCompleted,

        @Schema(description = "true si le périmètre commercial s'applique réellement à ce compte, "
                + "c'est-à-dire si au moins un projet lui a été affecté par un administrateur ET "
                + "qu'il ne porte aucun rôle d'exemption (Administrateur, Direction Générale).\n\n"
                + "Quand il vaut true, sont filtrés côté serveur sur ces projets : les listes ET "
                + "les détails de prospections, pré-prospections, dossiers et encaissements (une "
                + "ressource hors périmètre renvoie 404, y compris sur toutes leurs routes "
                + "imbriquées — échéancier, règlements, pièces jointes, documents PDF) ; tous les "
                + "agrégats du tableau de bord ; la liste des offres GET /api/devis ; et les "
                + "sous-ressources des fiches tiers — GET /api/clients/{id}/dossiers, "
                + "GET /api/prospects/{id}/{dossiers,prospections,reglements} et "
                + "GET /api/hotline/recherche. Dans ces dernières, ce sont les **lignes** qui sont "
                + "masquées : le client, le prospect et la fiche hotline eux-mêmes restent "
                + "accessibles, seul leur contenu hors périmètre disparaît.\n\n"
                + "Ne sont volontairement PAS filtrés : le patrimoine (projets/tranches/articles), "
                + "le calendrier, et les listes de tiers elles-mêmes (GET /api/clients, "
                + "GET /api/prospects).\n\n"
                + "Le front ne doit surtout pas refiltrer lui-même : il affiche ce que l'API "
                + "renvoie. Ce champ existe pour qu'un commercial restreint ne confonde jamais "
                + "restriction et perte de données (bandeau \"Périmètre limité à : …\").")
        boolean perimetreRestreint,

        @Schema(description = "Les projets du périmètre, quand perimetreRestreint vaut true. "
                + "Toujours vide si perimetreRestreint vaut false — un périmètre vide signifie "
                + "\"aucune restriction\", jamais \"ne voit rien\".")
        List<ProjetPerimetreDto> projetsAutorises
) {
}
