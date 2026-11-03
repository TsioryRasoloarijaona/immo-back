package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.domain.FamilleTiers;
import com.tsix.immocrm.services.administration.tiers.TiersService;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import org.springframework.stereotype.Component;

@Component
class ImportContacts extends ImportTiers {

    ImportContacts(TiersService service, Outils outils) {
        super(FamilleTiers.CONTACTS, Referentiel.NATURE_CONTACT, new Libelles("Création du contact : %s",
                "Mise à jour du contact : %s", "Le contact : %s n'a pas été modifié",
                "Problèmes d'import du contact : %s"), service, outils);
    }
}
