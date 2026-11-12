package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.dto.achat.AffecterBonCommandeRequest;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.TransitionPieceAchatRequest;
import com.tsix.immocrm.dto.achat.config.TransitionAchatDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentLine;
import com.tsix.immocrm.entities.buy.BuyDocumentStatus;
import com.tsix.immocrm.entities.buy.BuyDocumentStatusHistory;
import com.tsix.immocrm.entities.buy.BuyOrderInvoice;
import com.tsix.immocrm.entities.buy.BuyVentilationCategory;
import com.tsix.immocrm.entities.buy.BuyVentilationExpense;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.exceptions.TransitionAchatRefuseeException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.achat.FicheAchatService;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Écritures de la fiche d'une pièce d'achat (FEATURE-achat-fiche-ecritures.md E-B2 à E-B4) : 404 hors liste du compte,
 * puis 403 / 409 / 400 ; tous les contrôles avant le premier setter.
 */
@Service
public class EcrituresPieceAchatService {

    private static final String PIECE = "buyDocumentId";
    private static final int NOTE_MAX = 1000;
    private static final String GEREE_AILLEURS = "Cette pièce est gérée ailleurs (décompte, avance, frais de terrain, document "
            + "à régler) : elle ne se modifie pas depuis sa fiche.";

    private final FicheAchatService fiche;
    private final DroitsPieceAchat droits;
    private final TransitionsPieceAchat transitions;
    private final QuantitesBonCommande quantites;
    private final UtilisateurAffiche utilisateur;
    private final EntityManager em;
    private final Clock clock;

    public EcrituresPieceAchatService(FicheAchatService fiche, DroitsPieceAchat droits, TransitionsPieceAchat transitions,
                                      QuantitesBonCommande quantites, UtilisateurAffiche utilisateur, EntityManager em,
                                      Clock clock) {
        this.fiche = fiche;
        this.droits = droits;
        this.transitions = transitions;
        this.quantites = quantites;
        this.utilisateur = utilisateur;
        this.em = em;
        this.clock = clock;
    }

    private record Piece(DocumentAchatListItemDto vue, BuyDocument entite) {
        boolean gereeAilleurs() {
            return ActionsPieceAchat.gereeAilleurs(entite, vue);
        }
    }

    private Piece piece(UUID id) {
        DocumentAchatListItemDto vue = fiche.visible(id)
                .orElseThrow(() -> new ResourceNotFoundException(FicheAchatService.INTROUVABLE));
        return new Piece(vue, Objects.requireNonNull(em.find(BuyDocument.class, id)));
    }

    @Transactional
    public DocumentAchatDetailDto modifierStatut(UUID id, TransitionPieceAchatRequest corps) {
        BuyDocument d = piece(id).entite();
        UUID cible = corps.statutId();
        if (cible == null) {
            throw new InvalidRequestException("statutId est obligatoire.");
        }
        List<TransitionAchatDto> candidates = transitions.candidates(d);
        List<TransitionAchatDto> autorisees = transitions.autorisees(d, candidates);
        if (candidates.stream().noneMatch(t -> cible.equals(t.statutArriveeId()))) {
            throw new TransitionAchatRefuseeException("Aucune transition de la gestion ne mène du statut courant au statut "
                    + "demandé.", transitions.permises(autorisees));
        }
        TransitionAchatDto retenue = autorisees.stream().filter(t -> cible.equals(t.statutArriveeId())).findFirst()
                .orElseThrow(() -> new ForbiddenException("Cette transition est réservée à d'autres rôles."));
        String note = corps.note();
        if (retenue.noteObligatoire() && (note == null || note.isBlank())) {
            throw new InvalidRequestException("Une note est obligatoire pour cette transition.");
        }
        if (note != null && note.length() > NOTE_MAX) {
            throw new InvalidRequestException("La note ne doit pas dépasser " + NOTE_MAX + " caractères.");
        }
        d.setBuyDocumentStatusId(cible);
        LocalDateTime maintenant = modifiee(d);
        BuyDocumentStatusHistory h = new BuyDocumentStatusHistory();
        h.setId(UUID.randomUUID());
        h.setBuyDocumentId(id);
        h.setBuyDocumentStatusId(cible);
        h.setDocDate(maintenant);
        h.setAdmUserId(Champs.de(utilisateur.courant().compte(), AdmUser::getId));
        h.setNotes(note == null ? "" : note);
        h.setBuyDocumentWorkFlowId(retenue.id());
        em.persist(h);
        em.flush();
        return fiche.fiche(id);
    }

    @Transactional
    public void supprimer(UUID id) {
        Piece p = piece(id);
        BuyDocument d = p.entite();
        if (!DroitsPieceAchat.supportee(d.getDocType())) {
            throw new ConflictException(GEREE_AILLEURS);
        }
        droits.exigerSuppression(d.getDocType(), d.getPrjMarketId() != null);
        verifierSuppression(p);
        lire(BuyDocument.class, "buyQuoteId", id).forEach(c -> c.setBuyQuoteId(null));
        lire(BuyDocument.class, "buyInvoiceId", id).forEach(c -> c.setBuyInvoiceId(null));
        List.of(BuyDocumentLine.class, BuyVentilationCategory.class, BuyVentilationExpense.class,
                BuyDocumentStatusHistory.class).forEach(enfant -> lire(enfant, PIECE, id).forEach(em::remove));
        lire(BuyOrderInvoice.class, "buyInvoiceId", id).forEach(em::remove);
        em.remove(d);
        em.flush();
    }

    private void verifierSuppression(Piece p) {
        BuyDocument d = p.entite();
        UUID id = d.getId();
        if (p.gereeAilleurs()) {
            throw new ConflictException(GEREE_AILLEURS);
        }
        String lectureSeule = switch (NatureDocumentAchat.valueOf(d.getDocType())) {
            case ORDER -> "Vous n'avez pas le droit de supprimer un Bon de Commande avec un statut ";
            case INVOICE -> "Vous n'avez pas le droit de supprimer une Facture de status ";
            default -> null;
        };
        BuyDocumentStatus statut = d.getBuyDocumentStatusId() == null ? null
                : em.find(BuyDocumentStatus.class, d.getBuyDocumentStatusId());
        if (lectureSeule != null && statut != null && Boolean.TRUE.equals(statut.getIsReadOnly())) {
            throw new ConflictException(lectureSeule + Objects.requireNonNullElse(statut.getDescription(), statut.getCode()));
        }
        if (!lire(StlSettlementLine.class, PIECE, id).isEmpty()) {
            throw new ConflictException("Des règlements sont liés à cette pièce : supprimez-les d'abord.");
        }
        if (!lire(BuyDocument.class, "buyOrderId", id).isEmpty() || !lire(BuyDocument.class, "invoiceSourceId", id).isEmpty()
                || !lire(BuyOrderInvoice.class, "buyOrderId", id).isEmpty()) {
            throw new ConflictException("D'autres pièces sont rattachées à celle-ci (livraisons, factures ou avoirs).");
        }
        Number ecritures = (Number) em.createNativeQuery("""
                        SELECT (SELECT COUNT(*) FROM ActEntry WHERE BuyDocumentId = :id)
                             + (SELECT COUNT(*) FROM StlAccountingEntry WHERE BuyDocumentId = :id)""")
                .setParameter("id", id).getSingleResult();
        if (ecritures.intValue() > 0) {
            throw new ConflictException("Cette pièce a des écritures comptables.");
        }
    }

    @Transactional
    public DocumentAchatDetailDto affecterBonCommande(UUID id, AffecterBonCommandeRequest corps) {
        Piece p = piece(id);
        BuyDocument facture = p.entite();
        if (!ActionsPieceAchat.estFacture(facture)) {
            throw new ConflictException("Seule une facture s'affecte à un bon de commande");
        }
        droits.exigerModification(facture.getDocType(), false);
        UUID bcId = corps.bonCommandeId();
        if (bcId == null) {
            throw new InvalidRequestException("bonCommandeId est obligatoire.");
        }
        BuyDocument bc = fiche.visible(bcId).filter(v -> NatureDocumentAchat.ORDER.name().equals(v.nature()))
                .map(v -> em.find(BuyDocument.class, v.id()))
                .orElseThrow(() -> new ResourceNotFoundException("Bon de commande introuvable"));
        if (p.gereeAilleurs() || Objects.requireNonNullElse(facture.getAdvanceAmount(), BigDecimal.ZERO).signum() != 0) {
            throw new ConflictException(GEREE_AILLEURS);
        }
        if (!Objects.equals(facture.getCfgTierId(), bc.getCfgTierId())
                || !Objects.equals(facture.getCfgCompanyId(), bc.getCfgCompanyId())
                || facture.getPrjMarketId() != null && !facture.getPrjMarketId().equals(bc.getPrjMarketId())) {
            throw new ConflictException("Le bon de commande doit être du même fournisseur, de la même société et du même "
                    + "lot que la facture.");
        }
        quantites.verifier(facture, bc);
        facture.setBuyOrderId(bc.getId());
        modifiee(facture);
        em.flush();
        return fiche.fiche(id);
    }

    private LocalDateTime modifiee(BuyDocument d) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        d.setUserUpdate(utilisateur.courant().nom());
        d.setUserUpdateDateTime(maintenant);
        return maintenant;
    }

    private <T> List<T> lire(Class<T> entite, String champ, UUID id) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> q = cb.createQuery(entite);
        Root<T> r = q.from(entite);
        return em.createQuery(q.where(cb.equal(r.get(champ), id))).getResultList();
    }
}
