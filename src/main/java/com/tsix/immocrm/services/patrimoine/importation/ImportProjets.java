package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.dto.patrimoine.ProjetEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.services.patrimoine.ProjetService;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.champ;
import static com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.ref;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.PROJETS;

/** Programmes, colonnes de {@code omran-src/Yes.Rems/Yes.Cfg/CfgProject.cs} ; un programme invisible = 404 du service. */
@Component
class ImportProjets extends ImportGenerique<CfgProject> {

    private static final String ENTITE_ID = "entiteId";
    private static final List<ColonneGenerique> COLONNES = List.of(
            ref("Entité", ENTITE_ID, Referentiel.ENTITE), champ("Code", CODE), champ("Description", "description"),
            champ("Description détaillée", "descriptionLongue"), champ("Date lancement des traveaux", "dateDebutTravaux"),
            champ("Date fin des traveaux", "dateFinTravaux"),
            champ("Date début de commercialisation", "dateDebutCommercialisation"),
            champ("Date fin de commercialisation", "dateFinCommercialisation"), champ("Adresse 1", "adresse"),
            champ("Adresse 2", "adresse2"), champ("Adresse 3", "adresse3"), champ("Code postal", "codePostal"),
            champ("Ville", "ville"), ref("Pays", "paysId", Referentiel.PAYS), champ("Email", "email"),
            champ("Mobile", "mobile"), champ("Tél.", "telephone"), champ("Fax", "fax"), champ("Site web", "siteWeb"),
            champ("Courrier fait à", "villeSignature"), ref("Courrier signataire", "signataireId", Referentiel.PERSONNEL),
            ref("Responsable technique", "responsableTechniqueId", Referentiel.PERSONNEL),
            ref("Responsable commercial", "responsableCommercialId", Referentiel.PERSONNEL));

    private final ProjetService service;

    ImportProjets(ProjetService service, Outils outils) {
        super(new Definition<>(PROJETS, ProjetEcritureDto.class, CfgProject.class, CfgProject::getId, COLONNES,
                Set.of(ENTITE_ID, CODE, "description"), new Libelles("Création du programme : %s",
                "Mise à jour du programme : %s", "Le programme : %s n'a pas été modifié",
                "Problèmes d'import du programme : %s")), outils);
        this.service = service;
    }

    @Override
    protected void creer(Map<String, Object> champs) {
        service.creerProjet(dto(champs, ProjetEcritureDto.class));
    }

    @Override
    protected void modifier(UUID id, Map<String, Object> champs) {
        service.modifierProjet(id, dto(champs, ProjetEcritureDto.class));
    }
}
