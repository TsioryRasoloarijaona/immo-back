package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.dto.patrimoine.TrancheEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.services.patrimoine.TrancheService;
import com.tsix.immocrm.services.patrimoine.importation.EcritureParLigne.Libelles;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.champ;
import static com.tsix.immocrm.services.patrimoine.importation.ColonneGenerique.ref;
import static com.tsix.immocrm.services.patrimoine.importation.ImportPatrimoineService.TRANCHES;

/** Tranches, colonnes de {@code omran-src/Yes.Rems/Yes.Cfg/CfgTranche.cs} ; le programme n'est posé qu'en création (E6). */
@Component
class ImportTranches extends ImportGenerique<CfgTranche> {

    private static final String PROJET_ID = "projetId";
    private static final List<ColonneGenerique> COLONNES = List.of(
            ref("Programme", PROJET_ID, Referentiel.PROGRAMME), champ("Code", CODE), champ("Description", "description"),
            champ("Visible dans le module Vente", "estEnVente"), champ("Visible dans le module Projet", "visibleProjet"),
            champ("Visible dans le module Location", "estEnLocation"),
            champ("Visible dans le module Location-Vente", "visibleRom"), champ("Visible dans le module Achat", "visibleAchat"),
            champ("Visible dans le module service après vente", "visibleSav"),
            champ("Visible dans le module gestion immobilière", "visiblePrm"),
            ref("Responsable S.A.V.", "responsableSavId", Referentiel.PERSONNEL),
            ref("Standing", "standingId", Referentiel.STANDING), ref("Nbr. étage", "niveauTerrainId", Referentiel.NIVEAU),
            ref("Rubrique", "rubriqueId", Referentiel.RUBRIQUE), champ("Adresse SAV", "adresseSav"),
            champ("Avec octroi d'un cadeau pour un ancien client", "cadeauAncienClient"),
            champ("Avec octroi d'un cadeau pour un parrain", "cadeauParrain"),
            champ("Avec octroi d'un cadeau pour une source de contact", "cadeauOrigineProspection"),
            champ("Avec possibilité de cumul des cadeaux", "cadeauxCumules"),
            champ("Date lancement des traveaux", "dateDebutTravaux"), champ("Date fin des traveaux", "dateFinTravaux"),
            champ("Date octroi des T.F", "dateOctroiTitresFonciers"), champ("Date permis d'habiter", "datePermisHabiter"),
            champ("Date début de commercialisation", "dateDebutCommercialisation"),
            champ("Date fin des concrétisations", "dateFinConcretisation"),
            champ("Date début des finalisations", "dateDebutFinalisation"),
            champ("Date fin de commercialisation", "dateFinCommercialisation"), champ("Date réception", "dateReception"),
            champ("Date de la 1ère relance", "datePremiereRelance"), champ("Adresse 1", "adresse"),
            champ("Adresse 2", "adresse2"), champ("Adresse 3", "adresse3"), champ("Code postal", "codePostal"),
            ref("Pays", "paysId", Referentiel.PAYS), champ("Email", "email"), champ("Mobile", "mobile"),
            champ("Tél.", "telephone"), champ("Fax", "fax"), champ("Site web", "siteWeb"),
            champ("Pré-réservation", "montantPreReservation"), champ("Avance basée sur le montant", "avanceSurMontant"),
            champ("Concrétisation", "advancePercent"), champ("Frais de dossier", "fraisDossier"),
            champ("Pénalité de désistement", "tauxPenaliteAnnulation"),
            champ("Pénalité de désistement basée sur le prix de vente / encaissement", "penaliteAnnulationSurPrixArticle"),
            champ("Pénalité de transfert produit", "penaliteTransfertArticle"),
            champ("Pénalité de transfert tranche", "penaliteTransfertTranche"),
            champ("Pénalité de transfert client", "penaliteTransfertClient"), champ("Autre frais", "autresFrais"),
            champ("Frais de syndic", "montantSyndic"), champ("Période de syndic", "periodeSyndic"),
            champ("Délai d’attente après première notification T.F. (en jours)", "delaiPremierEngagement"),
            champ("Délai d’attente après deuxième notification T.F. (en jours)", "delaiDeuxiemeEngagement"),
            champ("Nombre maximum de visite par jours", "maxVisitNumber"),
            champ("Pénalité de non finalisation de la vente", "tauxPenalitePremierEngagement"),
            champ("Déduction après clôture du dossier de vente", "tauxPenaliteDeuxiemeEngagement"),
            champ("Délai de mise à disposition du solde des versements (en jour)", "delaiSoldePaiement"),
            champ("Durée du service après-vente (en mois)", "delaiGarantie"),
            champ("Durée du service après-vente supp. (en mois)", "delaiGarantieSupplement"),
            champ("Tribunal de commerce", "tribunalCommerce"),
            champ("Article légal pour la résiliation", "codeDroitRetractation"),
            champ("Délai de rétractation (heures)", "delaiRetractation"),
            champ("Délai max des échéances", "delaiMaxEcheance"),
            champ("Avec vérification du délai max des échéances", "verifierDelaiMaxEcheance"),
            champ("Crédit", "creditPercent"), champ("Avec vérification du crédit", "isWithVerifCredit"),
            champ("Remise sur la pré-réservation", "montantPreReservationRemise"),
            champ("Seuil de versement lors de la concrétisation", "advancePercentRemise"),
            champ("Active", "advancePercentRemiseActive"),
            champ("Remise sur les frais de dossier", "fraisDossierRemise"),
            champ("Remise sur les pénalités de désistement", "tauxPenaliteAnnulationRemise"),
            champ("Remise sur les pénalités de transfert produit", "penaliteTransfertArticleRemise"),
            champ("Remise sur les pénalitsé de transfert tranche", "penaliteTransfertTrancheRemise"),
            champ("Remise sur les pénalités de transfert client", "penaliteTransfertClientRemise"),
            champ("Remise sur les frais de syndic", "montantSyndicRemise"), champ("Remise sur autre frais", "autresFraisRemise"),
            champ("Délai entre la date d'envoi au notaire et la date minute", "delaiNotaireMinutes"),
            champ("Délai entre la date minute et l'encaissement des restes à régler", "delaiToutSoldeMinutes"),
            champ("Description détaillée", "descriptionLongue"),
            champ("Ecart-type concrétisation", "ecartTypeConcretisation"),
            champ("Ecart-type finalisation", "ecartTypeFinalisation"),
            ref("Tier autrui", "tiersAutruiId", Referentiel.FOURNISSEUR),
            ref("Plan compt.", "planComptableId", Referentiel.PLAN_COMPTABLE));

    private final TrancheService service;

    ImportTranches(TrancheService service, Outils outils) {
        super(new Definition<>(TRANCHES, TrancheEcritureDto.class, CfgTranche.class, CfgTranche::getId, COLONNES,
                Set.of(PROJET_ID, CODE, "description"), new Libelles("Création de la tranche : %s",
                "Mise à jour de la tranche : %s", "La tranche : %s n'a pas été modifiée",
                "Problèmes d'import de la tranche : %s")), outils);
        this.service = service;
    }

    @Override
    protected void creer(Map<String, Object> champs) {
        service.creerTranche(dto(champs, TrancheEcritureDto.class));
    }

    @Override
    protected void modifier(UUID id, Map<String, Object> champs) {
        service.modifierTranche(id, dto(champs, TrancheEcritureDto.class));
    }

    /** Décision du manager (26/10) n° 9 : programme différent = avertissement, le reste de la ligne s'écrit. */
    @Override
    protected void preparerMiseAJour(CfgTranche existante, Map<String, Object> champs, LigneGenerique l, Contexte ctx) {
        Object saisi = champs.remove(PROJET_ID);
        if (saisi != null && !Objects.equals(saisi, Champs.de(existante.getCfgProject(), CfgProject::getId))) {
            ctx.avertissements().add("Ligne %d : Le programme d'une tranche existante ne change pas par import."
                    .formatted(l.numero()));
        }
    }
}
