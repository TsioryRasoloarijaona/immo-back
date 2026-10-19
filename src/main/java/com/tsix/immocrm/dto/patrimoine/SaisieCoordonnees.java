package com.tsix.immocrm.dto.patrimoine;

import java.util.UUID;

/** Champs de saisie communs au programme et à l'entité juridique (colonnes de {@code ResponsablesOmran}). */
public interface SaisieCoordonnees {

    String adresse();

    String adresse2();

    String adresse3();

    String codePostal();

    String ville();

    String villeSignature();

    String telephone();

    String mobile();

    String fax();

    String email();

    String siteWeb();

    UUID paysId();

    UUID signataireId();

    UUID responsableTechniqueId();

    UUID responsableCommercialId();
}
