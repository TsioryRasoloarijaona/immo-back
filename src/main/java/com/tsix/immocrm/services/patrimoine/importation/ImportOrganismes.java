package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.domain.FamilleTiers;
import com.tsix.immocrm.services.administration.tiers.TiersService;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import org.springframework.stereotype.Component;

@Component
class ImportOrganismes extends ImportTiers {

    ImportOrganismes(TiersService service, Outils outils) {
        super(FamilleTiers.ORGANISMES, Referentiel.NATURE_ORGANISME, new Libelles("Création de l'organisme : %s",
                "Mise à jour de l'organisme : %s", "L'organisme : %s n'a pas été modifié",
                "Problèmes d'import de l'organisme : %s"), service, outils);
    }
}
