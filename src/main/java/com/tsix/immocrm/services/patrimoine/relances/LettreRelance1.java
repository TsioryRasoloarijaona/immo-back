package com.tsix.immocrm.services.patrimoine.relances;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.services.modele.CatalogueRelanceTitreFoncier;
import com.tsix.immocrm.services.modele.GenerationModeleService;
import com.tsix.immocrm.services.modele.InitialiseurModeles;
import org.springframework.stereotype.Component;

import java.util.List;

/** Lettres de 1re relance : modèle {@code RELANCE_TITRE_FONCIER/PREMIERE_RELANCE}, texte livré en repli. */
@Component
public class LettreRelance1 {

    private final GenerationModeleService generation;
    private final CatalogueRelanceTitreFoncier catalogue;

    public LettreRelance1(GenerationModeleService generation, CatalogueRelanceTitreFoncier catalogue) {
        this.generation = generation;
        this.catalogue = catalogue;
    }

    public byte[] pdf(List<ComFolder> dossiers) {
        return generation.pdfParPage(CategorieModele.RELANCE_TITRE_FONCIER, InitialiseurModeles.CODE_PREMIERE_RELANCE,
                InitialiseurModeles.contenu(InitialiseurModeles.RESSOURCE_RELANCE),
                dossiers.stream().map(d -> catalogue.catalogue().valeurs(catalogue.donnees(d))).toList());
    }
}
