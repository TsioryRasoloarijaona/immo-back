package com.tsix.immocrm.config.ecritures;

import com.tsix.immocrm.config.TablesEcrites;

import java.util.List;

/** Champs spécifiques (GEN0007) : entité, type, définition, valeur ; aucune clé vers les autres blocs. */
public class EcrituresChampsSpecifiques implements TablesEcrites {

    @Override
    public int rang() {
        return 1700;
    }

    @Override
    public List<String> tables() {
        return List.of("AdmSpecificFieldEntity", "AdmSpecificFieldDataType", "AdmSpecificField", "AdmSpecificFieldValue");
    }
}
