package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.NaturePieceLot;
import com.tsix.immocrm.dto.projet.PieceLotDetailDto;
import com.tsix.immocrm.dto.projet.PieceLotModeleDto;
import com.tsix.immocrm.dto.projet.PieceLotSaisieDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.achat.ecriture.EcrituresPieceAchatService;
import com.tsix.immocrm.services.projet.DroitsPieceLot.EtatPiece;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Pièces à payer créées depuis la fiche lot (FEATURE-lot-pieces-a-payer.md) ; ordre des refus : 404, 403, 400, 409. */
@Service
public class EcriturePieceLot {

    private static final String FOURNISSEUR = "Veuillez d'abord selectionnez un fournisseur";
    private static final String RG_BLOQUEE = "La restitution de la retenue de garantie a été bloquée !";
    private static final String ENREGISTREE = "Pièce à payer enregistrée";
    private static final String SUPPRIMEE = "Pièce à payer supprimée";

    private final LotsProjetService lots;
    private final DroitsPieceLot pieces;
    private final ContenuPieceLot contenu;
    private final EcrituresPieceAchatService suppression;
    private final GrillesLot grilles;
    private final PrevisionsPaiementLot previsions;
    private final EntityManager em;

    EcriturePieceLot(LotsProjetService lots, DroitsPieceLot pieces, ContenuPieceLot contenu,
                     EcrituresPieceAchatService suppression, GrillesLot grilles, PrevisionsPaiementLot previsions,
                     EntityManager em) {
        this.lots = lots;
        this.pieces = pieces;
        this.contenu = contenu;
        this.suppression = suppression;
        this.grilles = grilles;
        this.previsions = previsions;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public PieceLotModeleDto modele(UUID lotId, NaturePieceLot nature) {
        PrjMarket lot = lots.visible(lotId);
        pieces.exigerCreation(nature);
        refuserEtat(lot, nature);
        return contenu.modele(lot, nature);
    }

    @Transactional
    public PieceLotDetailDto creer(UUID lotId, PieceLotSaisieDto corps) {
        PrjMarket lot = lots.visible(lotId);
        NaturePieceLot nature = corps.nature();
        if (nature == null) {
            throw new InvalidRequestException("nature est obligatoire.");
        }
        pieces.exigerCreation(nature);
        valider(corps);
        refuserEtat(lot, nature);
        BuyDocument d = contenu.creer(lot, nature, corps);
        grilles.tracer(lot, ENREGISTREE);
        return detail(lot, d.getId());
    }

    @Transactional(readOnly = true)
    public PieceLotDetailDto lire(UUID lotId, UUID piece) {
        return detail(lots.visible(lotId), piece);
    }

    @Transactional
    public PieceLotDetailDto modifier(UUID lotId, UUID piece, PieceLotSaisieDto corps) {
        PrjMarket lot = lots.visible(lotId);
        EtatPiece etat = piece(lotId, piece, false);
        valider(corps);
        NaturePieceLot nature = NaturePieceLot.valueOf(etat.reference());
        lever(DroitsPieceLot.refusModification(nature, pieces.etatLot(lotId), etat));
        contenu.modifier(em.find(BuyDocument.class, piece), lot, nature, corps);
        grilles.tracer(lot, ENREGISTREE);
        return detail(lot, piece);
    }

    @Transactional
    public void supprimer(UUID lotId, UUID piece) {
        PrjMarket lot = lots.visible(lotId);
        EtatPiece etat = piece(lotId, piece, false);
        lever(DroitsPieceLot.refusSuppression(NaturePieceLot.valueOf(etat.reference()), pieces.droits(), etat));
        BuyDocument d = em.find(BuyDocument.class, piece);
        UUID decompte = d.getPrjWorkingOutId();
        suppression.supprimerContenu(d);
        contenu.recalculerReste(decompte, lotId);
        apresSuppression(lot);
    }

    @Transactional
    public void supprimerAvoir(UUID lotId, UUID avoir) {
        PrjMarket lot = lots.visible(lotId);
        lever(DroitsPieceLot.refusSuppressionAvoir(pieces.droits(), piece(lotId, avoir, true)));
        BuyDocument d = em.find(BuyDocument.class, avoir);
        UUID facture = d.getInvoiceSourceId();
        suppression.supprimerContenu(d);
        contenu.apresSuppressionAvoir(facture);
        apresSuppression(lot);
    }

    private void apresSuppression(PrjMarket lot) {
        grilles.tracer(lot, SUPPRIMEE);
        previsions.recalculer(lot.getId());
    }

    private EtatPiece piece(UUID lot, UUID id, boolean avoir) {
        return pieces.piece(lot, id, avoir)
                .orElseThrow(() -> new ResourceNotFoundException(avoir ? "Avoir introuvable" : "Pièce introuvable"));
    }

    private static void lever(RuntimeException refus) {
        if (refus != null) {
            throw refus;
        }
    }

    /** R3 puis R4 (409). */
    private void refuserEtat(PrjMarket lot, NaturePieceLot nature) {
        if (!Boolean.TRUE.equals(lot.getIsMultiSupplier()) && lot.getCfgSupplier() == null) {
            throw new ConflictException(FOURNISSEUR);
        }
        if (!pieces.etatLot(lot.getId()).permet(nature)) {
            throw new ConflictException(nature.refusEtat());
        }
        if (nature == NaturePieceLot.MARKET_WARRANTY && Boolean.TRUE.equals(lot.getBlokWarrantyRestitution())) {
            throw new ConflictException(RG_BLOQUEE);
        }
    }

    /** R9 (400). */
    private static void valider(PieceLotSaisieDto corps) {
        if (corps.date() == null) {
            throw new InvalidRequestException("date est obligatoire.");
        }
        if (negatif(corps.montantTTC()) || negatif(corps.penaliteRg())) {
            throw new InvalidRequestException("montantTTC et penaliteRg doivent être positifs ou nuls.");
        }
        if (corps.echeance() != null && corps.echeance().isBefore(corps.date())) {
            throw new InvalidRequestException("echeance doit être postérieure ou égale à date.");
        }
    }

    private static boolean negatif(BigDecimal v) {
        return v != null && v.signum() < 0;
    }

    private PieceLotDetailDto detail(PrjMarket lot, UUID id) {
        EtatPiece etat = piece(lot.getId(), id, false);
        NaturePieceLot nature = NaturePieceLot.valueOf(etat.reference());
        BuyDocument d = Objects.requireNonNull(em.find(BuyDocument.class, id));
        // Montants relus tels que stockés (6 décimales) : PU HT × (1 + TVA) donne 999.9999996 en mémoire.
        em.flush();
        em.refresh(d);
        CfgTier fournisseur = d.getCfgTierId() == null ? null : em.find(CfgTier.class, d.getCfgTierId());
        BigDecimal penalite = nature == NaturePieceLot.MARKET_WARRANTY
                ? Objects.requireNonNullElse(d.getAmountRegul(), BigDecimal.ZERO).negate() : null;
        return new PieceLotDetailDto(id, nature, d.getCode(), d.getDocRef(), DecomptesProjetService.jour(d.getDocDate()),
                DecomptesProjetService.jour(d.getMaturityDate()),
                d.getAmountHT(), d.getAmountVat(), d.getAmountTTC(), penalite, d.getNotes(), TierNomFormatter.nom(fournisseur),
                etat.statut(), DroitsPieceLot.refusModification(nature, pieces.etatLot(lot.getId()), etat) == null,
                DroitsPieceLot.refusSuppression(nature, pieces.droits(), etat) == null);
    }
}
