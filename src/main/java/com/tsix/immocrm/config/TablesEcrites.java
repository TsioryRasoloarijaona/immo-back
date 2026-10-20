package com.tsix.immocrm.config;

import java.util.List;

/**
 * Bloc de tables qu'un domaine écrit, parent avant enfant, pour {@link CollecteurLignesEcrites}. Un lot qui écrit une
 * nouvelle table ajoute sa classe dans {@code config.ecritures} ; {@link #rang()} (unique) place le bloc dans l'ordre global.
 */
public interface TablesEcrites {

    int rang();

    List<String> tables();
}
