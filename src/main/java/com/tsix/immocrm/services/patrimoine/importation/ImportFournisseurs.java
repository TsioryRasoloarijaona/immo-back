package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.domain.FamilleTiers;
import com.tsix.immocrm.services.administration.tiers.TiersService;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import org.springframework.stereotype.Component;

@Component
class ImportFournisseurs extends ImportTiers {

    ImportFournisseurs(TiersService service, Outils outils) {
        super(FamilleTiers.FOURNISSEURS, Referentiel.NATURE_FOURNISSEUR, new Libelles("Création du fournisseur : %s",
                "Mise à jour du fournisseur : %s", "Le fournisseur : %s n'a pas été modifié",
                "Problèmes d'import du fournisseur : %s"), service, outils);
    }
}
