package com.tsix.immocrm.domain;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolderStatusPaymentType;
import com.tsix.immocrm.entities.prj.PrjMarketType;
import com.tsix.immocrm.entities.stk.SpecificFunction;
import com.tsix.immocrm.entities.stk.StkContenance;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.entities.str.StrStatus;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Table visée par une clé étrangère saisie dans un référentiel administrable, et liste de choix qui l'alimente : une
 * route existante, ou {@code /api/referentiels-admin/choix/{slug}} quand aucune n'existe.
 */
public enum ReferenceAdmin {
    NATURE(StkKind.class, null, "/api/referentiels/natures"),
    CATEGORIE_COMPTABLE(StlCategory.class, "categories-comptables", null),
    TYPE_MARCHE(PrjMarketType.class, "types-marche", null),
    TRANCHE(CfgTranche.class, null, "/api/referentiels/tranches"),
    /** Seuils de versement : statuts des dossiers de vente seulement (FEATURE-patrimoine-referentiels.md §8.2). */
    STATUT_VENTE(StrStatus.class, "statuts-vente", null) {
        @Override
        public Predicate<Object> accepte() {
            return o -> o instanceof StrStatus s && StatutDossier.CONFIG_ENTITY_ID.equals(s.getStrConfigEntityId());
        }
    },
    TYPE_REGLEMENT(ComFolderStatusPaymentType.class, "types-reglement", null),
    CONTENANCE(StkContenance.class, null, "/api/referentiels-patrimoine/contenances"),
    FONCTION_SPECIFIQUE(SpecificFunction.class, "fonctions-specifiques", null),
    /** Toutes natures confondues (FEATURE-patrimoine-faisabilite.md §3). */
    CATEGORIE_ARTICLE(StkItemCategory.class, "categories-article", null);

    private static final String CHOIX = "/api/referentiels-admin/choix/";

    private final Class<?> classe;
    private final String slugChoix;
    private final String routeExistante;

    ReferenceAdmin(Class<?> classe, String slugChoix, String routeExistante) {
        this.classe = classe;
        this.slugChoix = slugChoix;
        this.routeExistante = routeExistante;
    }

    public Class<?> classe() {
        return classe;
    }

    /** Route GET qui rend la liste de choix ({@code ReferentielItemDto[]}). */
    public String choix() {
        return slugChoix != null ? CHOIX + slugChoix : routeExistante;
    }

    /** Lignes acceptées comme valeur ; toute autre PKey est refusée comme inconnue. */
    public Predicate<Object> accepte() {
        return o -> true;
    }

    /** Listes servies par {@code /api/referentiels-admin/choix/{slug}}. */
    public static Optional<ReferenceAdmin> parSlugChoix(String slug) {
        return Arrays.stream(values()).filter(r -> r.slugChoix != null && r.slugChoix.equals(slug)).findFirst();
    }
}
