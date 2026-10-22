package com.tsix.immocrm.web.export;

import com.tsix.immocrm.domain.Colonne;

import java.util.List;
import java.util.Map;

/**
 * Une feuille d'un classeur : lignes {@code record} ou {@code Map} (clé = {@link Colonne#nom()}) ; {@code totaux}
 * non nul ⇒ dernière ligne « Total » en gras.
 */
public record FeuilleExport(String nom, List<? extends Colonne> colonnes, List<?> lignes, Map<String, ?> totaux) {
}
