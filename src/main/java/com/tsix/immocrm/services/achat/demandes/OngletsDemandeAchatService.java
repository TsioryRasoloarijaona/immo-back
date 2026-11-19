package com.tsix.immocrm.services.achat.demandes;

import com.tsix.immocrm.dto.achat.demandes.BonCommandeDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.DemandeAchatFiche;
import com.tsix.immocrm.dto.achat.demandes.FournisseurDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.ModaliteDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.OffreDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.PrixLigneOffre;
import com.tsix.immocrm.dto.achat.demandes.SaisieDemandeAchatLot;
import com.tsix.immocrm.dto.achat.demandes.SaisieFournisseurDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.SaisieOffreDemandeAchat;
import com.tsix.immocrm.dto.projet.BordereauSaisieDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.UUID;

@Service
public class OngletsDemandeAchatService {

    public List<FournisseurDemandeAchat> fournisseurs(UUID id) {
        throw new UnsupportedOperationException();
    }

    public FournisseurDemandeAchat ajouterFournisseur(UUID id, SaisieFournisseurDemandeAchat saisie) {
        throw new UnsupportedOperationException();
    }

    public FournisseurDemandeAchat modifierFournisseur(UUID id, UUID fid, SaisieFournisseurDemandeAchat saisie) {
        throw new UnsupportedOperationException();
    }

    public void supprimerFournisseur(UUID id, UUID fid) {
        throw new UnsupportedOperationException();
    }

    public FournisseurDemandeAchat ecarterFournisseur(UUID id, UUID fid, boolean ecarte) {
        throw new UnsupportedOperationException();
    }

    public List<OffreDemandeAchat> offres(UUID id) {
        throw new UnsupportedOperationException();
    }

    public OffreDemandeAchat creerOffre(UUID id, SaisieOffreDemandeAchat saisie) {
        throw new UnsupportedOperationException();
    }

    public OffreDemandeAchat modifierOffre(UUID id, UUID oid, List<PrixLigneOffre> prix) {
        throw new UnsupportedOperationException();
    }

    public void supprimerOffre(UUID id, UUID oid) {
        throw new UnsupportedOperationException();
    }

    public DemandeAchatFiche selectionnerOffre(UUID id, UUID oid) {
        throw new UnsupportedOperationException();
    }

    public DemandeAchatFiche deselectionnerOffres(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<OffreDemandeAchat> comparaison(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<ModaliteDemandeAchat> modalites(UUID id) {
        throw new UnsupportedOperationException();
    }

    public ModaliteDemandeAchat ajouterModalite(UUID id, ModaliteDemandeAchat saisie) {
        throw new UnsupportedOperationException();
    }

    public void supprimerModalite(UUID id, UUID mid) {
        throw new UnsupportedOperationException();
    }

    public BordereauSaisieDto bordereau(UUID id) {
        throw new UnsupportedOperationException();
    }

    public BordereauSaisieDto enregistrerBordereau(UUID id, BordereauSaisieDto bordereau) {
        throw new UnsupportedOperationException();
    }

    public BordereauSaisieDto importerBordereau(UUID id, MultipartFile fichier) {
        throw new UnsupportedOperationException();
    }

    public ResponseEntity<StreamingResponseBody> exporterBordereau(UUID id) {
        throw new UnsupportedOperationException();
    }

    public List<BonCommandeDemandeAchat> bonsCommande(UUID id) {
        throw new UnsupportedOperationException();
    }

    public DemandeAchatFiche renouveler(UUID id) {
        throw new UnsupportedOperationException();
    }

    public DemandeAchatFiche creerDepuisLot(UUID lot, SaisieDemandeAchatLot saisie) {
        throw new UnsupportedOperationException();
    }
}
