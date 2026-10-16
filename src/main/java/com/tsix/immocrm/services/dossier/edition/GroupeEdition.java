package com.tsix.immocrm.services.dossier.edition;

import java.util.Collection;
import java.util.Set;

/**
 * Groupes de champs modifiables de la fiche et rôles autorisés : seule règle d'autorisation de
 * {@code PATCH /api/dossiers/{id}}. Rôles de la spec, droits OMRAN mesurés : voir FEATURE-vente-edition.md.
 */
public enum GroupeEdition {

    DATES(Roles.CHEF, Roles.DIRECTION, Roles.ADMIN),
    ANNULATION(Roles.CHEF, Roles.DIRECTION, Roles.ADMIN),
    CLIENT(Roles.ADMIN),
    COMMERCIAL(Roles.ADMIN),
    GESTIONNAIRES(Roles.CHEF, Roles.ADMIN),
    NOTAIRE(Roles.CHEF, Roles.ADMIN),
    CREDIT(Roles.CHEF, Roles.ADMIN),
    OPTIONS(Roles.CHEF, Roles.ADMIN),
    PENALITES(Roles.ADMIN),
    PRIX(Roles.ADMIN),
    /** Tout utilisateur qui voit le dossier ; la visibilité est contrôlée par PerimetreInterceptor. */
    NOTE;

    private final Set<String> roles;

    GroupeEdition(String... roles) {
        this.roles = Set.of(roles);
    }

    public Set<String> roles() {
        return roles;
    }

    /** {@code rolesAppelant} : codes de rôle sans le préfixe {@code ROLE_}. */
    public boolean autorise(Collection<String> rolesAppelant) {
        return roles.isEmpty() || rolesAppelant.stream().anyMatch(roles::contains);
    }

    static final class Roles {
        static final String ADMIN = "Administrateur";
        static final String CHEF = "Commercial Chef";
        static final String DIRECTION = "Direction Générale";

        private Roles() {
        }
    }
}
