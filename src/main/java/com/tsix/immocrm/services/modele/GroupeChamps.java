package com.tsix.immocrm.services.modele;

import java.util.List;

public record GroupeChamps<T>(String libelle, List<Champ<T>> champs) {
}
