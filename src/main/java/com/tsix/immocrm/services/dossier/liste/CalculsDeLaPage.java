package com.tsix.immocrm.services.dossier.liste;

import org.springframework.stereotype.Component;

/** Les deux lectures par page de la liste des dossiers, regroupées pour le constructeur du service (S107). */
@Component
public record CalculsDeLaPage(ColonnesCalculeesDossier colonnes, MontantsParNatureDossier montants) {
}
