package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.ModelePieceAchatDto;
import com.tsix.immocrm.dto.achat.SaisiePieceAchatRequest;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentLine;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.achat.FicheAchatService;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Ligne;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Création et modification d'une pièce d'achat par sa fiche (FEATURE-achat-fiche-creation.md) ; tous les contrôles
 * avant la première écriture, par {@link EcriturePieceAchat}.
 */
@Service
public class SaisiePieceAchatService {

    private final FicheAchatService fiche;
    private final DroitsPieceAchat droits;
    private final ActionsPieceAchat actions;
    private final LectureSaisieAchat lecture;
    private final EcriturePieceAchat ecriture;
    private final QuantitesBonCommande quantites;
    private final EntityManager em;

    SaisiePieceAchatService(FicheAchatService fiche, DroitsPieceAchat droits, ActionsPieceAchat actions,
                            LectureSaisieAchat lecture, EcriturePieceAchat ecriture, QuantitesBonCommande quantites,
                            EntityManager em) {
        this.fiche = fiche;
        this.droits = droits;
        this.actions = actions;
        this.lecture = lecture;
        this.ecriture = ecriture;
        this.quantites = quantites;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public ModelePieceAchatDto modele(String nature, List<UUID> sources) {
        NatureDocumentAchat n = LectureSaisieAchat.nature(nature);
        droits.exigerModification(n.name(), false);
        return lecture.modele(n, ecriture.employeDuCompte());
    }

    /** Ordre : 400 nature, 404 société / projet, 403 droit, 400 contenu ; la séquence n'est prise qu'à l'écriture. */
    @Transactional
    public DocumentAchatDetailDto creer(SaisiePieceAchatRequest corps) {
        NatureDocumentAchat nature = LectureSaisieAchat.nature(corps.nature());
        UUID declencheur = corps.declencheurId() != null ? corps.declencheurId() : ecriture.employeDuCompte();
        PieceAchatACreer piece = lecture.piece(corps, nature, declencheur,
                () -> droits.exigerModification(nature.name(), corps.lotId() != null));
        verifierIdsDeLignes(Set.of(), piece.lignes());
        BuyDocument d = ecriture.creer(piece);
        em.flush();
        return fiche.fiche(d.getId());
    }

    /** Ordre E-C5.1 : 404 pièce, 409 gérée ailleurs, 403, 409 lecture seule, 400 corps, 409 quantités du BC. */
    @Transactional
    public DocumentAchatDetailDto modifier(UUID id, SaisiePieceAchatRequest corps) {
        DocumentAchatListItemDto vue = fiche.visible(id)
                .orElseThrow(() -> new ResourceNotFoundException(FicheAchatService.INTROUVABLE));
        BuyDocument d = Objects.requireNonNull(em.find(BuyDocument.class, id));
        actions.refusModification(d, vue, corps.lotId() != null).ifPresent(refus -> {
            throw refus;
        });
        PieceAchatACreer piece = lecture.piece(corps, LectureSaisieAchat.nature(d.getDocType()), corps.declencheurId(),
                () -> { });
        verifierIdsDeLignes(Set.copyOf(em.createQuery("SELECT l.id FROM BuyDocumentLine l WHERE l.buyDocumentId = :piece",
                UUID.class).setParameter("piece", d.getId()).getResultList()), piece.lignes());
        boolean livraison = NatureDocumentAchat.DELIVRY.name().equals(d.getDocType());
        if ((ActionsPieceAchat.estFacture(d) || livraison) && d.getBuyOrderId() != null) {
            quantites.verifier(d, Objects.requireNonNull(em.find(BuyDocument.class, d.getBuyOrderId())),
                    piece.lignes().stream().map(l -> EcriturePieceAchat.remplir(new BuyDocumentLine(), l, 0, d)).toList());
        }
        ecriture.modifier(d, piece);
        em.flush();
        return fiche.fiche(id);
    }

    /** {@code siennes} : lignes de la pièce modifiée, vide à la création (tout id y est inconnu). */
    private static void verifierIdsDeLignes(Set<UUID> siennes, List<Ligne> lignes) {
        Set<UUID> vues = new HashSet<>();
        for (int i = 0; i < lignes.size(); i++) {
            UUID ligne = lignes.get(i).id();
            if (ligne != null && (!siennes.contains(ligne) || !vues.add(ligne))) {
                throw new InvalidRequestException("lignes[%d].id : ligne inconnue de cette pièce.".formatted(i));
            }
        }
    }
}
