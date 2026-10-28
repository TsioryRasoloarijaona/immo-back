package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.dto.dossier.ActeReservationData;
import com.tsix.immocrm.dto.dossier.ActeReservationData.Destinataire;
import com.tsix.immocrm.dto.dossier.ActeReservationData.LignePaiement;
import com.tsix.immocrm.dto.dossier.ActeReservationData.SousArticle;
import com.tsix.immocrm.services.dossier.ActeReservationService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.tsix.immocrm.domain.TypeChamp.BOOLEEN;
import static com.tsix.immocrm.domain.TypeChamp.DATE;
import static com.tsix.immocrm.domain.TypeChamp.MONTANT;
import static com.tsix.immocrm.domain.TypeChamp.NOMBRE;
import static com.tsix.immocrm.domain.TypeChamp.POURCENTAGE;
import static com.tsix.immocrm.domain.TypeChamp.TEXTE;

/** Catalogue {@code DOSSIER_VENTE} (FEATURE-modeles-documents.md §4), lu sur {@link ActeReservationData}. */
@Component
public class CatalogueDossierVente implements CatalogueFiche {

    /** Une page du formulaire Barid par élément (FEATURE-vente-formulaire-barid.md §3), d'un dossier ou d'un lot. */
    private static final ListeChamps<ActeReservationData, Destinataire> DESTINATAIRES = liste("destinataires",
            "Destinataires (acquéreur et co-acquéreurs)", ActeReservationData::destinataires, List.of(
                    new Champ<>("destinataire.titre", "Titre", TEXTE, "Mr", Destinataire::titre),
                    new Champ<>("destinataire.nom", "Nom", TEXTE, "BENNANI KARIM", Destinataire::nom),
                    new Champ<>("destinataire.adresse", "Adresse avec code postal", TEXTE,
                            "12 RUE A 20250 CASA MAROC", Destinataire::adresse),
                    new Champ<>("destinataire.adresseSansCodePostal", "Adresse sans code postal", TEXTE,
                            "12 RUE A CASA MAROC", Destinataire::adresseSansCodePostal),
                    new Champ<>("destinataire.codePostal", "Code postal", TEXTE, "2  0  2  5  0",
                            Destinataire::codePostal),
                    new Champ<>("destinataire.societe", "Société", TEXTE, "GHRIZA", Destinataire::societe),
                    new Champ<>("destinataire.societeAdresseSansCodePostal", "Adresse de la société", TEXTE,
                            "10 BD SOCIETE casablanca MAROC", Destinataire::societeAdresseSansCodePostal),
                    new Champ<>("destinataire.societeCodePostal", "Code postal de la société", TEXTE, "2  0  0  0  0",
                            Destinataire::societeCodePostal),
                    new Champ<>("destinataire.dossier", "Dossier", TEXTE, "APF 2022/0012", Destinataire::dossier),
                    new Champ<>("destinataire.tranche", "Tranche", TEXTE, "AYAD PRIVATE T1", Destinataire::tranche),
                    new Champ<>("destinataire.statut", "Statut", TEXTE, "RESERVATION", Destinataire::statut),
                    new Champ<>("destinataire.role", "Rôle", TEXTE, "Acquéreur", Destinataire::role)));

    static final Catalogue<ActeReservationData> CATALOGUE = new Catalogue<>(
            List.of(
                    new GroupeChamps<>("Dossier", List.of(
                            new Champ<>("dossier.code", "Code du dossier", TEXTE, "APF 2022/0012",
                                    ActeReservationData::dossierCode),
                            new Champ<>("dossier.dateEdition", "Date d'édition", DATE, "16/10/2026",
                                    ActeReservationData::dateEdition),
                            new Champ<>("dossier.prixVente", "Prix de vente TTC", MONTANT, "1 200 000.00 DH",
                                    ActeReservationData::prixVente),
                            new Champ<>("dossier.avanceMontant", "Montant de l'avance", MONTANT, "120 000.00 DH",
                                    ActeReservationData::avanceMontant),
                            new Champ<>("dossier.avancePct", "Avance en % du prix", POURCENTAGE, "10.00 %",
                                    ActeReservationData::avancePct))),
                    new GroupeChamps<>("Client", List.of(
                            new Champ<>("client.titre", "Civilité", TEXTE, "Monsieur", ActeReservationData::clientTitre),
                            new Champ<>("client.nom", "Nom du client", TEXTE, "Nor CHEMAOU", ActeReservationData::clientNom),
                            new Champ<>("client.cin", "CIN", TEXTE, "A742122", ActeReservationData::clientCin),
                            new Champ<>("client.adresse", "Adresse", TEXTE, "20 BIS CADI BEN HAMMADI Casablanca",
                                    ActeReservationData::clientAdresse),
                            new Champ<>("client.telephone", "Téléphone", TEXTE, "0612345678",
                                    ActeReservationData::clientTelephone))),
                    new GroupeChamps<>("Projet / société", List.of(
                            new Champ<>("projet.nom", "Nom du projet", TEXTE, "AYAD PRIVATE", ActeReservationData::projetNom),
                            new Champ<>("projet.adresse", "Adresse du projet", TEXTE, "Route d'Azemmour Casablanca",
                                    ActeReservationData::projetAdresse),
                            new Champ<>("societe.nom", "Société", TEXTE, "MAYLINE", ActeReservationData::entrepriseNom),
                            new Champ<>("societe.signataire", "Signataire", TEXTE, "Karim ALAOUI",
                                    ActeReservationData::signataireNom),
                            new Champ<>("societe.tribunal", "Tribunal compétent", TEXTE, "Casablanca",
                                    ActeReservationData::tribunalCompetent))),
                    new GroupeChamps<>("Article", List.of(
                            new Champ<>("article.categorie", "Catégorie", TEXTE, "APPARTEMENT",
                                    ActeReservationData::articleCategorie),
                            new Champ<>("article.code", "Numéro", TEXTE, "APF-0-12", ActeReservationData::articleCode),
                            new Champ<>("article.immeuble", "Immeuble", TEXTE, "IMM01", ActeReservationData::immeubleCode),
                            new Champ<>("article.etage", "Étage", TEXTE, "Rez de chaussée", ActeReservationData::etage),
                            new Champ<>("article.superficie", "Superficie (m²)", NOMBRE, "60.00",
                                    ActeReservationData::superficie),
                            new Champ<>("article.estTerrain", "Terrain", BOOLEEN, "Non", ActeReservationData::estTerrain))),
                    new GroupeChamps<>("Pénalités / syndic", List.of(
                            new Champ<>("penalite.resiliationPct", "Pénalité de résiliation", POURCENTAGE, "15.00 %",
                                    ActeReservationData::penaliteResiliationPct),
                            new Champ<>("penalite.transfertProduit", "Transfert de produit", MONTANT, "10 000.00 DH",
                                    ActeReservationData::transfertProduitMontant),
                            new Champ<>("penalite.transfertTranche", "Transfert de tranche / projet", MONTANT,
                                    "20 000.00 DH", ActeReservationData::transfertTrancheMontant),
                            new Champ<>("penalite.transfertClientPct", "Transfert de client", POURCENTAGE, "2.50 %",
                                    ActeReservationData::transfertClientPct),
                            new Champ<>("sav.dureeMois", "Durée du SAV (mois)", NOMBRE, "12",
                                    ActeReservationData::dureeSavMois),
                            new Champ<>("syndic.montantMensuel", "Syndic par mois", MONTANT, "700.00 DH",
                                    ActeReservationData::syndicMontantMensuel),
                            new Champ<>("syndic.dureeMois", "Durée du syndic (mois)", NOMBRE, "24",
                                    ActeReservationData::syndicDureeMois),
                            new Champ<>("syndic.montantTotal", "Syndic total", MONTANT, "16 800.00 DH",
                                    CatalogueDossierVente::syndicTotal)))),
            List.of(
                    liste("sousArticles", "Sous-articles", ActeReservationData::sousArticles, List.of(
                            new Champ<>("sousArticle.type", "Type", TEXTE, "TERRASSE", SousArticle::typeLabel),
                            new Champ<>("sousArticle.superficie", "Superficie (m²)", NOMBRE, "16.00",
                                    SousArticle::superficie))),
                    liste("echeancesSolde", "Échéances du solde", ActeReservationData::lignesSolde, List.of(
                            new Champ<>("echeance.montant", "Montant", MONTANT, "540 000.00 DH", LignePaiement::montant),
                            new Champ<>("echeance.pct", "% du prix", POURCENTAGE, "45.00 %", LignePaiement::pct),
                            new Champ<>("echeance.date", "Date", DATE, "30/06/2027", LignePaiement::date))),
                    DESTINATAIRES));

    private final ActeReservationService acteReservationService;
    private final PerimetreCommercialService perimetreService;

    public CatalogueDossierVente(ActeReservationService acteReservationService,
                                 PerimetreCommercialService perimetreService) {
        this.acteReservationService = acteReservationService;
        this.perimetreService = perimetreService;
    }

    @Override
    public CategorieModele categorie() {
        return CategorieModele.DOSSIER_VENTE;
    }

    @Override
    public Catalogue<ActeReservationData> catalogue() {
        return CATALOGUE;
    }

    @Override
    public FicheLue lire(UUID ficheId) {
        perimetreService.verifierAccesDossier(ficheId);
        ActeReservationData data = acteReservationService.collect(ficheId);
        return new FicheLue(data.dossierCode(), CATALOGUE.valeurs(data));
    }

    /** Valeurs d'un lot de formulaires : la seule liste {@code destinataires}, tous dossiers confondus. */
    static ValeursFiche valeursDestinataires(List<Destinataire> destinataires) {
        return new ValeursFiche(Map.of(), Map.of(DESTINATAIRES.cle(), DESTINATAIRES.valeursDe(destinataires)));
    }

    /** Même règle que l'acte codé en dur : rien si le montant mensuel ou la durée manque. */
    private static BigDecimal syndicTotal(ActeReservationData data) {
        if (data.syndicMontantMensuel() == null || data.syndicDureeMois() == null) {
            return null;
        }
        return data.syndicMontantMensuel().multiply(BigDecimal.valueOf(data.syndicDureeMois()));
    }

    private static <E> ListeChamps<ActeReservationData, E> liste(String cle, String libelle,
            Function<ActeReservationData, List<E>> elements, List<Champ<E>> champs) {
        return new ListeChamps<>(cle, libelle, elements, champs);
    }
}
