package com.tsix.immocrm.services.patrimoine.importation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Une ligne d'un catalogue déclaratif de l'import générique (FEATURE-patrimoine-import-generique.md §1.3) : en-tête OMRAN
 * ({@code DisplayName}) → champ du DTO d'écriture ; le type se déduit du composant du DTO.
 */
record ColonneGenerique(String entete, String propriete, Referentiel referentiel, Nature nature) {

    /** IGNOREE : exportée par OMRAN, sans champ chez nous ; REFUSEE : champ existant que l'import n'écrit jamais. */
    enum Nature { CHAMP, IGNOREE, REFUSEE }

    /** Types du fil ({@code ColonneImportDto.type}) et lecture de la cellule. */
    enum TypeColonne {
        TEXTE(TypeCellule.TEXTE), ENTIER(TypeCellule.ENTIER), DECIMAL(TypeCellule.NOMBRE), DATE(TypeCellule.DATE),
        BOOLEEN(TypeCellule.BOOLEEN), REFERENCE(TypeCellule.REFERENCE);

        private static final Map<Class<?>, TypeColonne> PAR_CLASSE = Map.of(String.class, TEXTE, Integer.class, ENTIER,
                BigDecimal.class, DECIMAL, LocalDateTime.class, DATE, LocalDate.class, DATE, Boolean.class, BOOLEEN,
                UUID.class, REFERENCE);

        final TypeCellule cellule;

        TypeColonne(TypeCellule cellule) {
            this.cellule = cellule;
        }

        static TypeColonne de(Class<?> classe) {
            TypeColonne type = PAR_CLASSE.get(classe);
            if (type == null) {
                throw new IllegalStateException("Type de champ non importable : " + classe);
            }
            return type;
        }
    }

    static ColonneGenerique champ(String entete, String propriete) {
        return new ColonneGenerique(entete, propriete, null, Nature.CHAMP);
    }

    static ColonneGenerique ref(String entete, String propriete, Referentiel referentiel) {
        return new ColonneGenerique(entete, propriete, referentiel, Nature.CHAMP);
    }

    static ColonneGenerique ignoree(String entete) {
        return new ColonneGenerique(entete, null, null, Nature.IGNOREE);
    }

    static ColonneGenerique refusee(String entete, String propriete) {
        return new ColonneGenerique(entete, propriete, null, Nature.REFUSEE);
    }

    boolean estIgnoree() {
        return nature == Nature.IGNOREE;
    }
}
