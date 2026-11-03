package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.domain.FamilleTiers;
import com.tsix.immocrm.services.administration.tiers.TiersService;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import org.springframework.stereotype.Component;

@Component
class ImportPersonnels extends ImportTiers {

    ImportPersonnels(TiersService service, Outils outils) {
        super(FamilleTiers.PERSONNELS, Referentiel.NATURE_PERSONNEL, new Libelles("Création du personnel : %s",
                "Mise à jour du personnel : %s", "Le personnel : %s n'a pas été modifié",
                "Problèmes d'import du personnel : %s"), service, outils);
    }
}
