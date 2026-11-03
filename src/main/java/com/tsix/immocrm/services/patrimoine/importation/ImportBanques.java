package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.domain.FamilleTiers;
import com.tsix.immocrm.services.administration.tiers.TiersService;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import org.springframework.stereotype.Component;

@Component
class ImportBanques extends ImportTiers {

    ImportBanques(TiersService service, Outils outils) {
        super(FamilleTiers.BANQUES, Referentiel.NATURE_BANQUE, new Libelles("Création de la banque : %s",
                "Mise à jour de la banque : %s", "La banque : %s n'a pas été modifiée",
                "Problèmes d'import de la banque : %s"), service, outils);
    }
}
