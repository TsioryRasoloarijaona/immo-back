package com.tsix.immocrm.domain;

import java.util.Set;

/**
 * Les codes de rôle ({@code AdmRole.Code}) qui échappent au périmètre commercial : leurs titulaires
 * voient l'intégralité des prospections, pré-prospections, dossiers, encaissements et agrégats du
 * tableau de bord, même si des projets leur sont affectés.
 *
 * <p>Deux règles à ne pas contourner (mesurées sur {@code yesRems}) :
 * <ul>
 *   <li>La comparaison se fait sur le <b>code exact</b>, jamais par préfixe : les codes de rôle sont
 *       de la saisie libre et un {@code LIKE 'Commercial%'} capturerait {@code Commercial Chef} et
 *       {@code Commercial Aide}.</li>
 *   <li>{@code Commercial Chef} (13 comptes) n'est <b>pas</b> exempté : aucune table d'équipe ou de
 *       hiérarchie n'existe dans le schéma pour résoudre "voit son équipe". Un chef reçoit
 *       simplement plusieurs projets, ce que le modèle couvre déjà.</li>
 * </ul>
 *
 * <p>La source de vérité des rôles est {@code AdmUserRole} (m2m), jamais {@code AdmUser.WebRoleId} :
 * les deux mécanismes coexistent et divergent en base réelle.
 */
public final class RolesExemptesPerimetre {

    /** {@code Administrateur} (20 comptes) et {@code Direction Générale} (2 comptes). */
    public static final Set<String> CODES = Set.of("Administrateur", "Direction Générale");

    private RolesExemptesPerimetre() {
    }

    /** true si l'un des codes de rôle fournis exempte du filtrage de périmètre. */
    public static boolean exempte(Iterable<String> codesRole) {
        for (String code : codesRole) {
            if (code != null && CODES.contains(code)) {
                return true;
            }
        }
        return false;
    }
}
