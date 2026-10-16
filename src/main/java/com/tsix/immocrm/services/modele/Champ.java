package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.TypeChamp;

import java.util.function.Function;

/** Un champ du catalogue, déclaré une seule fois : liste de l'éditeur, aperçu et génération lisent cette déclaration. */
public record Champ<T>(String cle, String libelle, TypeChamp type, String exemple, Function<T, Object> extracteur) {
}
