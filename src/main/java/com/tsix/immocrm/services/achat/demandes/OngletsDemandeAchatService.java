package com.tsix.immocrm.services.achat.demandes;

import com.tsix.immocrm.dto.achat.demandes.BonCommandeDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.DemandeAchatFiche;
import com.tsix.immocrm.dto.achat.demandes.FournisseurDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.ImportOffreDemandeAchatDto;
import com.tsix.immocrm.dto.achat.demandes.ModaliteDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.OffreDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.PhasesDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.PrixLigneOffre;
import com.tsix.immocrm.dto.achat.demandes.SaisieDemandeAchatLot;
import com.tsix.immocrm.dto.achat.demandes.SaisieFournisseurDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.SaisieOffreDemandeAchat;
import com.tsix.immocrm.dto.projet.BordereauSaisieDto;
import com.tsix.immocrm.entities.buy.BuySelection;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Onglets d'une DIA et créations N6b (FEATURE-n6-demandes-achat.md R15-R19). Refus dans l'ordre : 404 visibilité, 403 droit,
 * 409 R7 puis phase, puis 400 et 409 métier dans les composants.
 */
@Service
public class OngletsDemandeAchatService {

    private static final String LECTURE = "Yes.Buy.Front.CallOfTender.BuySelectionList";
    private static final String NOUVEAU = LECTURE + ".toolNew";
    private static final String CONFIDENTIEL = LECTURE + ".IsConfidential";
    private static final String AJOUT_FOURNISSEUR = LECTURE + ".toolAddSupplier";
    private static final String SUPPRESSION_FOURNISSEUR = LECTURE + ".toolDeleteSupplier";
    private static final String ECART_FOURNISSEUR = LECTURE + ".toolSelectDeselectSupplier";
    private static final String AJOUT_OFFRE = LECTURE + ".toolAddQuote";
    private static final String SUPPRESSION_OFFRE = LECTURE + ".toolDeleteQuote";
    private static final String SELECTION_OFFRE = LECTURE + ".toolSelectDeselectQuote";
    private static final Predicate<PhasesDemandeAchat> SANS_PHASE = p -> true;
    private static final String HORS_PHASE = "Le statut de la demande ne permet pas cette action.";

    private final DemandesAchatService demandes;
    private final EcrituresDemandeAchat ecritures;
    private final DroitsOmran droits;
    private final PreselectionDemandeAchat preselection;
    private final OffresDemandeAchat offres;
    private final BordereauDemandeAchat bordereau;
    private final NouvelleDemandeAchat nouvelle;

    public OngletsDemandeAchatService(DemandesAchatService demandes, EcrituresDemandeAchat ecritures, DroitsOmran droits,
                                      PreselectionDemandeAchat preselection, OffresDemandeAchat offres, BordereauDemandeAchat bordereau,
                                      NouvelleDemandeAchat nouvelle) {
        this.demandes = demandes;
        this.ecritures = ecritures;
        this.droits = droits;
        this.preselection = preselection;
        this.offres = offres;
        this.bordereau = bordereau;
        this.nouvelle = nouvelle;
    }

    @Transactional(readOnly = true)
    public List<FournisseurDemandeAchat> fournisseurs(UUID id) {
        lecture(id);
        return preselection.fournisseurs(id);
    }

    @Transactional
    public FournisseurDemandeAchat ajouterFournisseur(UUID id, SaisieFournisseurDemandeAchat saisie) {
        ecriture(id, AJOUT_FOURNISSEUR, PhasesDemandeAchat::preselection);
        return preselection.ajouter(id, saisie);
    }

    @Transactional
    public FournisseurDemandeAchat modifierFournisseur(UUID id, UUID fid, SaisieFournisseurDemandeAchat saisie) {
        ecriture(id, AJOUT_FOURNISSEUR, PhasesDemandeAchat::preselection);
        return preselection.modifier(id, fid, saisie);
    }

    @Transactional
    public void supprimerFournisseur(UUID id, UUID fid) {
        ecriture(id, SUPPRESSION_FOURNISSEUR, PhasesDemandeAchat::preselection);
        preselection.supprimer(id, fid);
    }

    @Transactional
    public FournisseurDemandeAchat ecarterFournisseur(UUID id, UUID fid, boolean ecarte) {
        ecriture(id, ECART_FOURNISSEUR, PhasesDemandeAchat::preselection);
        return preselection.ecarter(id, fid, ecarte);
    }

    @Transactional(readOnly = true)
    public List<OffreDemandeAchat> offres(UUID id) {
        lecture(id);
        return offres.offres(id, true);
    }

    @Transactional
    public OffreDemandeAchat creerOffre(UUID id, SaisieOffreDemandeAchat saisie) {
        return offres.creer(ecriture(id, AJOUT_OFFRE, PhasesDemandeAchat::offres), saisie);
    }

    @Transactional
    public OffreDemandeAchat modifierOffre(UUID id, UUID oid, List<PrixLigneOffre> prix) {
        ecriture(id, AJOUT_OFFRE, PhasesDemandeAchat::offres);
        return offres.modifier(id, oid, prix);
    }

    @Transactional
    public void supprimerOffre(UUID id, UUID oid) {
        ecriture(id, SUPPRESSION_OFFRE, PhasesDemandeAchat::offres);
        offres.supprimer(id, oid);
    }

    @Transactional
    public ImportOffreDemandeAchatDto importerOffre(UUID id, UUID oid, MultipartFile fichier) {
        ecriture(id, AJOUT_OFFRE, PhasesDemandeAchat::offres);
        return ImportOffreDemandeAchat.importer(offres, id, oid, fichier);
    }

    @Transactional
    public DemandeAchatFiche selectionnerOffre(UUID id, UUID oid, boolean confirme) {
        offres.selectionner(ecriture(id, SELECTION_OFFRE, PhasesDemandeAchat::selection), oid, confirme);
        return demandes.fiche(id);
    }

    @Transactional
    public DemandeAchatFiche deselectionnerOffres(UUID id) {
        offres.deselectionner(ecriture(id, SELECTION_OFFRE, PhasesDemandeAchat::selection));
        return demandes.fiche(id);
    }

    @Transactional(readOnly = true)
    public List<OffreDemandeAchat> comparaison(UUID id) {
        if (!lecture(id).phases().comparaison()) {
            throw new ConflictException(HORS_PHASE);
        }
        return offres.offres(id, false);
    }

    @Transactional(readOnly = true)
    public List<ModaliteDemandeAchat> modalites(UUID id) {
        lecture(id);
        return preselection.modalites(id);
    }

    @Transactional
    public ModaliteDemandeAchat ajouterModalite(UUID id, ModaliteDemandeAchat saisie) {
        ecriture(id, NOUVEAU, SANS_PHASE);
        return preselection.ajouterModalite(id, saisie);
    }

    @Transactional
    public void supprimerModalite(UUID id, UUID mid) {
        ecriture(id, NOUVEAU, SANS_PHASE);
        preselection.supprimerModalite(id, mid);
    }

    @Transactional(readOnly = true)
    public BordereauSaisieDto bordereau(UUID id) {
        lecture(id);
        return bordereau.lire(id);
    }

    @Transactional
    public BordereauSaisieDto enregistrerBordereau(UUID id, BordereauSaisieDto corps) {
        ecriture(id, NOUVEAU, PhasesDemandeAchat::bordereau);
        bordereau.enregistrer(id, corps);
        return bordereau.lire(id);
    }

    @Transactional
    public BordereauSaisieDto importerBordereau(UUID id, MultipartFile fichier) {
        ecriture(id, NOUVEAU, PhasesDemandeAchat::bordereau);
        bordereau.importer(id, fichier);
        return bordereau.lire(id);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> exporterBordereau(UUID id) {
        lecture(id);
        return bordereau.exporter(id);
    }

    @Transactional(readOnly = true)
    public List<BonCommandeDemandeAchat> bonsCommande(UUID id) {
        lecture(id);
        return offres.bonsCommande(id);
    }

    @Transactional
    public DemandeAchatFiche renouveler(UUID id) {
        return demandes.relire(nouvelle.renouveler(ecriture(id, NOUVEAU, SANS_PHASE)));
    }

    @Transactional
    public DemandeAchatFiche creerDepuisLot(UUID lot, SaisieDemandeAchatLot saisie) {
        PrjMarket marche = nouvelle.lot(lot);
        droits.exiger(LECTURE, NOUVEAU);
        return demandes.relire(nouvelle.depuisLot(marche, saisie));
    }

    /** 404 puis lecture, et le droit confidentiel si la procédure l'est. */
    private DemandeAchatFiche lecture(UUID id) {
        DemandeAchatFiche f = demandes.fiche(id);
        if (f.confidentielle()) {
            droits.exiger(CONFIDENTIEL);
        }
        return f;
    }

    /** 404, 403, R7, phase ouverte par le statut : tous avant la première écriture. */
    private BuySelection ecriture(UUID id, String droit, Predicate<PhasesDemandeAchat> phase) {
        DemandeAchatFiche f = demandes.visible(id);
        droits.exiger(LECTURE, droit);
        if (f.confidentielle()) {
            droits.exiger(CONFIDENTIEL);
        }
        BuySelection d = ecritures.derniere(id);
        if (f.phases() == null || !phase.test(f.phases())) {
            throw new ConflictException(HORS_PHASE);
        }
        return d;
    }
}
