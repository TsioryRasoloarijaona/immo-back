package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.dto.patrimoine.prospectionterrain.ProspectionTerrainEcritureDto;
import com.tsix.immocrm.entities.lnd.LndLand;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import com.tsix.immocrm.services.patrimoine.prospectionterrain.ProspectionTerrainService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.champ;
import static com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.ignoree;
import static com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.ref;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.PROSPECTIONS_TERRAIN;

/**
 * Prospections terrain, colonnes de {@code omran-src/Yes.Rems/Yes.Lnd/Prospection.cs} ; statut et archivage jamais
 * importés (workflow, §1.5), surfaces calculées par OMRAN ignorées.
 */
@Component
class ImportProspectionsTerrain extends ImportGenerique<LndLand> {

    private static final List<ColonneGenerique> COLONNES = List.of(
            champ("Code", CODE), champ("Date", "date"), champ("Description", "description"),
            ref("Catégorie", "categorieId", Referentiel.CATEGORIE), ref("Type", "typeId", Referentiel.TYPE_TERRAIN),
            ref("Niveau", "niveauId", Referentiel.NIVEAU), ref("Vocation", "vocationId", Referentiel.VOCATION),
            ref("Pays", "paysId", Referentiel.PAYS), ref("Ville", "villeId", Referentiel.VILLE),
            ignoree("Validité du statut"), ref("Le courtier", "courtierId", Referentiel.TIERS),
            ref("Notaire / avocat", "notaireId", Referentiel.TIERS), champ("Lien G.Map", "lienGoogle"),
            champ("Aménagé/Brut", "estAmenage"), ref("Orientation", "orientationId", Referentiel.ORIENTATION),
            champ("Adresse", "adresse"), champ("Propriétaire", "proprietaires"), champ("Notes", "notes"),
            champ("Superficie(m²)", "superficie"), champ("Superficie Voie d'aménagement (m²)", "superficieEquipements"),
            ignoree("Coef équipements et voies d'aménagement"),
            champ("Superficie équipement à céder (m²)", "superficieEquipementACeder"),
            champ("Superficie à régler (m²)", "superficieARegler"), champ("Superficie à céder (m²)", "superficieACeder"),
            ignoree("Superficie Nette (m²)"), champ("Emprise au sol Batiments (COS)", "cos"),
            champ("Superficie Emprise au sol bâtiment(m²)", "superficieCos"), champ("Coef plancher couvert (cuf)", "cuf"),
            champ("Superficie plancher couvert (cuf) (m²)", "superficieCuf"), champ("Coef Totale projetée", "coefVendable"),
            champ("Superficie Totale projetée", "superficieVendable"), champ("Coef Commerce", "coefCommerce"),
            champ("Superficie Commerce", "superficieCommerce"), champ("Coef Bureau", "coefBureau"),
            champ("Superficie Bureau", "superficieBureau"), ignoree("Superficie Habitat"),
            champ("Coef Parking sous-sol", "coefParking"), champ("Superficie Parking sous-sol", "superficieParking"),
            champ("Arrêté d'alignement(m)", "arreteAlignement"), champ("Hauteur max autorisée(m)", "hauteurMax"),
            champ("Prix du m² du propriétaire", "prixM2"), champ("Prix total du propriétaire", "prix"),
            champ("Prix proposé", "prixPropose"), champ("Date enregistrement", "dateEnregistrement"),
            champ("N° enregistrement", "numeroEnregistrement"), champ("Lieu enregistrement", "lieuEnregistrement"),
            champ("C.P.F. Immatriculation", "cpfImmatriculation"), champ("Date Immatriculation", "dateImmatriculation"),
            ignoree("Archivée"), ignoree("Acquisition"), champ("Anomalies", "anomalies"),
            champ("Identification de la zone", "identificationZone"),
            champ("Conditions de raccordement concessionnaire", "conditionsRaccordement"),
            champ("Assiette foncière", "assietteFonciere"), champ("Levée topographique", "leveeTopographique"));

    private final ProspectionTerrainService service;

    ImportProspectionsTerrain(ProspectionTerrainService service, Outils outils) {
        super(new Definition<>(PROSPECTIONS_TERRAIN, ProspectionTerrainEcritureDto.class, LndLand.class, LndLand::getId,
                COLONNES, Set.of(CODE, "description", "categorieId", "typeId", "paysId", "villeId"),
                new Libelles("Création de la prospection terrain : %s", "Mise à jour de la prospection terrain : %s",
                        "La prospection terrain : %s n'a pas été modifiée", "Problèmes d'import de la prospection terrain : %s")),
                outils);
        this.service = service;
    }

    @Override
    protected void creer(Map<String, Object> champs) {
        service.creer(dto(champs, ProspectionTerrainEcritureDto.class));
    }

    @Override
    protected void modifier(UUID id, Map<String, Object> champs) {
        service.modifier(id, dto(champs, ProspectionTerrainEcritureDto.class));
    }
}
