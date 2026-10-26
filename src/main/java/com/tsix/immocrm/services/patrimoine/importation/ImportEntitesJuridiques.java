package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.dto.entite.EntiteJuridiqueEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.patrimoine.EntiteJuridiqueService;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.champ;
import static com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.ref;
import static com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.refusee;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.ENTITES_JURIDIQUES;

/** Entités juridiques, colonnes de {@code omran-src/Yes.Rems/Yes.Cfg/CfgCompany.cs} ; « Active » refusée (§1.2). */
@Component
class ImportEntitesJuridiques extends ImportGenerique<CfgCompany> {

    private static final List<ColonneGenerique> COLONNES = List.of(
            champ("Code", CODE), champ("Description", "description"), champ("Forme juridique", "formeJuridique"),
            champ("Matricule fiscal", "identifiantFiscal"), champ("Registre du commerce", "registreCommerce"),
            champ("N° Sécurité sociale", "cnss"), champ("Patente", "patente"),
            champ("Identifiant Commun de l'Entreprise", "ice"), champ("Date de création", "dateCreation"),
            champ("Capital", "capital"), ref("Devise", "deviseId", Referentiel.DEVISE),
            ref("Groupe d'actionnaires", "groupeActionnairesId", Referentiel.GROUPE_ACTIONNAIRES),
            champ("Nombre actionnaires", "nombreActionnaires"), champ("Nombre de parts (actions)", "nombreParts"),
            champ("Part maison", "partMaison"), champ("Date liquidation", "dateLiquidation"), refusee("Active", "isActive"),
            champ("Adresse 1", "adresse"), champ("Adresse 2", "adresse2"), champ("Adresse 3", "adresse3"),
            champ("Code postal", "codePostal"), champ("Ville", "ville"), ref("Pays", "paysId", Referentiel.PAYS),
            champ("Téléphone", "telephone"), champ("Fax", "fax"), champ("Mobile", "mobile"), champ("E-mail", "email"),
            champ("Site web", "siteWeb"), champ("Courrier fait à", "villeSignature"),
            ref("Représentant légal", "representantLegalId", Referentiel.PERSONNEL),
            ref("Courrier signataire", "signataireId", Referentiel.PERSONNEL),
            ref("Responsable technique", "responsableTechniqueId", Referentiel.PERSONNEL),
            ref("Responsable commercial", "responsableCommercialId", Referentiel.PERSONNEL),
            champ("Description détaillée", "descriptionLongue"));

    private final EntiteJuridiqueService service;

    ImportEntitesJuridiques(EntiteJuridiqueService service, Outils outils) {
        super(new Definition<>(ENTITES_JURIDIQUES, EntiteJuridiqueEcritureDto.class, CfgCompany.class, CfgCompany::getId,
                COLONNES, Set.of(CODE, "description"), new Libelles("Création de l'entité juridique : %s",
                "Mise à jour de l'entité juridique : %s", "L'entité juridique : %s n'a pas été modifiée",
                "Problèmes d'import de l'entité juridique : %s")), outils);
        this.service = service;
    }

    @Override
    protected void creer(Map<String, Object> champs) {
        service.creer(dto(champs, EntiteJuridiqueEcritureDto.class));
    }

    @Override
    protected void modifier(UUID id, Map<String, Object> champs) {
        service.modifier(id, dto(champs, EntiteJuridiqueEcritureDto.class));
    }

    /** Décision du manager (26/10) n° 6 : une entité inactive ne se modifie pas par import. */
    @Override
    protected void preparerMiseAJour(CfgCompany existante, Map<String, Object> champs, LigneGenerique l, Contexte ctx) {
        if (!Boolean.TRUE.equals(existante.getIsActive())) {
            throw new InvalidRequestException("L'entité " + existante.getCode() + " est inactive : import impossible.");
        }
    }
}
