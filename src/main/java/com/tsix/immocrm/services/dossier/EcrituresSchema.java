package com.tsix.immocrm.services.dossier;

import org.springframework.stereotype.Component;

/**
 * Ce que chaque écriture de l'échéancier déclenche : défauts, report de l'écart, totaux du dossier, pénalité de
 * retard au règlement (S107).
 */
@Component
public record EcrituresSchema(DefautsEcheance defauts, ReequilibrageSchema reequilibrage,
                              TotauxDossierEcheancier totaux, PenaliteRetardService penalites) {
}
