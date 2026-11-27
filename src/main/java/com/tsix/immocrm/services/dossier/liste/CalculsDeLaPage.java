package com.tsix.immocrm.services.dossier.liste;

import com.tsix.immocrm.services.perimetre.AgencesDesVendeurs;
import org.springframework.stereotype.Component;

/** Les lectures par page de la liste des dossiers, regroupées pour le constructeur du service (S107). */
@Component
public record CalculsDeLaPage(ColonnesCalculeesDossier colonnes, MontantsParNatureDossier montants,
                              AgencesDesVendeurs agences) {
}
