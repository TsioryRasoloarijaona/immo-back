package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.services.dossier.ActeReservationService;
import com.tsix.immocrm.services.dossier.MontantsDossier;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.domain.TypeChamp.DATE;
import static com.tsix.immocrm.domain.TypeChamp.MONTANT;
import static com.tsix.immocrm.domain.TypeChamp.NOMBRE;
import static com.tsix.immocrm.domain.TypeChamp.POURCENTAGE;
import static com.tsix.immocrm.domain.TypeChamp.TEXTE;

/** Catalogue {@code RELANCE_TITRE_FONCIER} : lettre de 1re relance d'un dossier (gabarit OMRAN CfgTrancheFicheRelance1). */
@Component
public class CatalogueRelanceTitreFoncier implements CatalogueFiche {

    /** Variables de la lettre ; restes dus = {@link MontantsDossier} (prix, total), frais = total − prix. */
    public record DonneesRelance(String dossierCode, LocalDate dateEdition, LocalDate dateRelance, LocalDate echeance,
                                 String clientTitre, String clientNom, String clientAdresse, String clientVille,
                                 String clientPays, String articleCode, String articleCategorie, String projetNom,
                                 String projetAdresse, String societeNom, String societeAdresse, String signataireNom,
                                 String signataireQualite, String villeSignature, Integer premierDelai,
                                 Integer secondDelai, BigDecimal penalitePct, String droitAnnulation, Integer delaiSolde,
                                 BigDecimal restePrix, BigDecimal resteFrais, BigDecimal resteTotal) {
    }

    static final Catalogue<DonneesRelance> CATALOGUE = new Catalogue<>(List.of(
            new GroupeChamps<>("Dossier", List.of(
                    new Champ<>("dossier.code", "Code du dossier", TEXTE, "APF 2022/0012", DonneesRelance::dossierCode),
                    new Champ<>("dossier.dateEdition", "Date d'édition", DATE, "16/10/2026", DonneesRelance::dateEdition),
                    new Champ<>("relance.date", "Date de 1re relance", DATE, "01/03/2026", DonneesRelance::dateRelance),
                    new Champ<>("relance.echeance", "Échéance de la relance", DATE, "30/04/2026",
                            DonneesRelance::echeance))),
            new GroupeChamps<>("Client", List.of(
                    new Champ<>("client.titre", "Civilité", TEXTE, "Monsieur", DonneesRelance::clientTitre),
                    new Champ<>("client.nom", "Nom du client", TEXTE, "Nor CHEMAOU", DonneesRelance::clientNom),
                    new Champ<>("client.adresse", "Adresse", TEXTE, "20 BIS CADI BEN HAMMADI", DonneesRelance::clientAdresse),
                    new Champ<>("client.ville", "Ville", TEXTE, "Casablanca", DonneesRelance::clientVille),
                    new Champ<>("client.pays", "Pays", TEXTE, "Maroc", DonneesRelance::clientPays))),
            new GroupeChamps<>("Article / programme", List.of(
                    new Champ<>("article.code", "Numéro de l'article", TEXTE, "APF-0-12", DonneesRelance::articleCode),
                    new Champ<>("article.categorie", "Catégorie", TEXTE, "APPARTEMENT", DonneesRelance::articleCategorie),
                    new Champ<>("projet.nom", "Programme", TEXTE, "AYAD PRIVATE", DonneesRelance::projetNom),
                    new Champ<>("projet.adresse", "Adresse du programme", TEXTE, "Route d'Azemmour Casablanca",
                            DonneesRelance::projetAdresse))),
            new GroupeChamps<>("Société", List.of(
                    new Champ<>("societe.nom", "Société", TEXTE, "MAYLINE", DonneesRelance::societeNom),
                    new Champ<>("societe.adresse", "Adresse de la société", TEXTE, "Bd Zerktouni Casablanca",
                            DonneesRelance::societeAdresse),
                    new Champ<>("societe.signataire", "Signataire", TEXTE, "Karim ALAOUI", DonneesRelance::signataireNom),
                    new Champ<>("societe.qualiteSignataire", "Qualité du signataire", TEXTE, "Gérant",
                            DonneesRelance::signataireQualite),
                    new Champ<>("societe.villeSignature", "Ville de signature", TEXTE, "Casablanca",
                            DonneesRelance::villeSignature))),
            new GroupeChamps<>("Délais de la tranche", List.of(
                    new Champ<>("tranche.premierDelai", "1er délai (jours)", NOMBRE, "60", DonneesRelance::premierDelai),
                    new Champ<>("tranche.secondDelai", "2e délai (jours)", NOMBRE, "30", DonneesRelance::secondDelai),
                    new Champ<>("tranche.penalite", "Pénalité du 1er délai", POURCENTAGE, "5.00 %",
                            DonneesRelance::penalitePct),
                    new Champ<>("tranche.droitAnnulation", "Droit d'annulation", TEXTE, "Article 12",
                            DonneesRelance::droitAnnulation),
                    new Champ<>("tranche.delaiSolde", "Délai de solde (jours)", NOMBRE, "90", DonneesRelance::delaiSolde))),
            new GroupeChamps<>("Reste dû", List.of(
                    new Champ<>("reste.prix", "Reste dû sur le prix", MONTANT, "982 700.00 DH", DonneesRelance::restePrix),
                    new Champ<>("reste.frais", "Reste dû sur les frais", MONTANT, "21 250.00 DH", DonneesRelance::resteFrais),
                    new Champ<>("reste.total", "Reste dû total", MONTANT, "1 003 950.00 DH",
                            DonneesRelance::resteTotal)))),
            List.of());

    private final ComFolderRepository dossiers;
    private final MontantsDossier montants;
    private final PerimetreCommercialService perimetre;
    private final Clock clock;

    public CatalogueRelanceTitreFoncier(ComFolderRepository dossiers, MontantsDossier montants,
                                        PerimetreCommercialService perimetre, Clock clock) {
        this.dossiers = dossiers;
        this.montants = montants;
        this.perimetre = perimetre;
        this.clock = clock;
    }

    @Override
    public CategorieModele categorie() {
        return CategorieModele.RELANCE_TITRE_FONCIER;
    }

    @Override
    public Catalogue<DonneesRelance> catalogue() {
        return CATALOGUE;
    }

    @Override
    public FicheLue lire(UUID ficheId) {
        perimetre.verifierAccesDossier(ficheId);
        ComFolder dossier = dossiers.findById(ficheId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + ficheId));
        return new FicheLue(dossier.getCode(), CATALOGUE.valeurs(donnees(dossier)));
    }

    public DonneesRelance donnees(ComFolder d) {
        MontantsDossierDto m = montants.calculer(d);
        BigDecimal prix = m.prixVente().reste();
        BigDecimal total = m.netAPayer().reste();
        CfgTier client = d.getCfgCustomer();
        StkItem article = d.getStkItem();
        CfgTranche tranche = Champs.de(article, StkItem::getStkHierarchy);
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);
        CfgCompany societe = Champs.de(projet, CfgProject::getCfgCompany);
        CfgTier signataire = Champs.de(societe, CfgCompany::getAdmSignatory);
        return new DonneesRelance(d.getCode(), LocalDate.now(clock), jour(d.getRevival1()),
                jour(d.getRevival1ValidityDate()), ActeReservationService.titre(client),
                Champs.de(client, TierNomFormatter::nom),
                Champs.de(client, c -> ActeReservationService.adresse(c.getAddress(), c.getAddress2(), c.getAddress3(), null)),
                Champs.de(client, CfgTier::getCity), Champs.de(client, CfgTier::getAdmCountry, AdmCountry::getDescription),
                Champs.de(article, StkItem::getCode),
                Champs.de(article, StkItem::getStkItemCategory, StkItemCategory::getDescription),
                Champs.de(projet, CfgProject::getDescription),
                Champs.de(projet, p -> ActeReservationService.adresse(p.getAddress(), p.getAddress2(), p.getAddress3(),
                        p.getCity())),
                Champs.de(societe, CfgCompany::getDescription),
                Champs.de(societe, s -> ActeReservationService.adresse(s.getAddress(), s.getAddress2(), s.getAddress3(),
                        s.getCity())),
                Champs.de(signataire, TierNomFormatter::nom), Champs.de(signataire, CfgTier::getQuality),
                Champs.de(societe, CfgCompany::getSignatureCity),
                Champs.de(tranche, CfgTranche::getAgreementFirstDelay), Champs.de(tranche, CfgTranche::getAgreementSecondDelay),
                Champs.de(tranche, CfgTranche::getAgreementFirstPenaltyPercent),
                Champs.de(tranche, CfgTranche::getCancellationRightCode),
                Champs.de(tranche, CfgTranche::getPaymentBalanceDelay),
                prix, total.subtract(prix), total);
    }

    private static LocalDate jour(LocalDateTime date) {
        return date == null ? null : date.toLocalDate();
    }
}
