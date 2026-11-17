package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.dto.achat.DocumentAchatDetailDto;
import com.tsix.immocrm.dto.achat.DocumentAchatListItemDto;
import com.tsix.immocrm.dto.achat.ModelePieceAchatDto;
import com.tsix.immocrm.dto.achat.SaisiePieceAchatRequest;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.achat.FicheAchatService;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Generation;
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

    private static final String FACTURE_INTROUVABLE = "La facture sélectionnée n'est pas présente dans notre référentiel !";

    private final FicheAchatService fiche;
    private final DroitsPieceAchat droits;
    private final ActionsPieceAchat actions;
    private final LectureSaisieAchat lecture;
    private final EcriturePieceAchat ecriture;
    private final GenerationPieceAchat generation;
    private final EntityManager em;

    SaisiePieceAchatService(FicheAchatService fiche, DroitsPieceAchat droits, ActionsPieceAchat actions,
                            LectureSaisieAchat lecture, EcriturePieceAchat ecriture, GenerationPieceAchat generation,
                            EntityManager em) {
        this.fiche = fiche;
        this.droits = droits;
        this.actions = actions;
        this.lecture = lecture;
        this.ecriture = ecriture;
        this.generation = generation;
        this.em = em;
    }

    /** Génération (FEATURE-achat-generation.md G1) : 400 nature, 404 sources, 403, 400 / 409 préconditions. */
    @Transactional(readOnly = true)
    public ModelePieceAchatDto modele(String nature, List<UUID> sources) {
        NatureDocumentAchat n = LectureSaisieAchat.nature(nature, sources);
        List<BuyDocument> docs = sources(n, sources);
        droits.exigerModification(n.name(), !docs.isEmpty() && docs.getFirst().getPrjMarketId() != null);
        if (docs.isEmpty()) {
            return lecture.modele(n, ecriture.employeDuCompte());
        }
        Generation g = generation.verifier(n, docs);
        return lecture.modele(generation.saisie(n, docs), g.gestion(), g.devise());
    }

    /** Ordre : 400 nature, 404 société / projet / sources, 403 droit, 400 contenu, 409 génération ; séquence à l'écriture. */
    @Transactional
    public DocumentAchatDetailDto creer(SaisiePieceAchatRequest corps) {
        NatureDocumentAchat nature = LectureSaisieAchat.nature(corps.nature(), corps.sources());
        UUID declencheur = corps.declencheurId() != null ? corps.declencheurId() : ecriture.employeDuCompte();
        List<BuyDocument> docs = sources(nature, corps.sources());
        PieceAchatACreer piece = lecture.piece(corps, nature, declencheur,
                () -> droits.exigerModification(nature.name(), corps.lotId() != null), generation.bordereaux(docs));
        verifierIdsDeLignes(Set.of(), piece.lignes());
        if (!docs.isEmpty()) {
            Generation g = generation.verifier(nature, docs);
            piece = piece.generee(g, LectureSaisieAchat.sansDepart(g.gestion()));
            BuyDocument transitoire = new BuyDocument();
            transitoire.setId(UUID.randomUUID());
            transitoire.setDocType(nature.name());
            transitoire.setBuyOrderId(g.commande());
            generation.verifierQuantites(transitoire, piece.lignes());
        }
        BuyDocument d = ecriture.creer(piece);
        em.flush();
        return fiche.fiche(d.getId());
    }

    /** Ordre E-C5.1 : 404 pièce, 409 gérée ailleurs, 403, 409 lecture seule, 400 corps, 409 quantités du BC. */
    @Transactional
    public DocumentAchatDetailDto modifier(UUID id, SaisiePieceAchatRequest saisie) {
        DocumentAchatListItemDto vue = fiche.visible(id)
                .orElseThrow(() -> new ResourceNotFoundException(FicheAchatService.INTROUVABLE));
        BuyDocument d = Objects.requireNonNull(em.find(BuyDocument.class, id));
        // le lot d'un avoir est celui de sa facture : jamais changé (FEATURE-achat-avoirs.md R5)
        SaisiePieceAchatRequest corps = NatureDocumentAchat.REFUND.name().equals(d.getDocType())
                ? saisie.avecLot(d.getPrjMarketId()) : saisie;
        actions.refusModification(d, vue, corps.lotId() != null).ifPresent(refus -> {
            throw refus;
        });
        PieceAchatACreer piece = lecture.piece(corps, LectureSaisieAchat.nature(d.getDocType()), corps.declencheurId(),
                () -> { }, null);
        verifierIdsDeLignes(Set.copyOf(em.createQuery("SELECT l.id FROM BuyDocumentLine l WHERE l.buyDocumentId = :piece",
                UUID.class).setParameter("piece", d.getId()).getResultList()), piece.lignes());
        generation.verifierQuantites(d, piece.lignes());
        ecriture.modifier(d, piece);
        em.flush();
        return fiche.fiche(id);
    }

    /** Sources visibles du compte (même règle que la fiche), dans l'ordre ; une invisible = 404 (texte OMRAN si avoir). */
    private List<BuyDocument> sources(NatureDocumentAchat cible, List<UUID> ids) {
        String introuvable = cible == NatureDocumentAchat.REFUND ? FACTURE_INTROUVABLE : FicheAchatService.INTROUVABLE;
        return ids == null ? List.of() : ids.stream().distinct().map(id -> {
            fiche.visible(id).orElseThrow(() -> new ResourceNotFoundException(introuvable));
            BuyDocument source = Objects.requireNonNull(em.find(BuyDocument.class, id));
            em.refresh(source); // préconditions relues en base, pas dans le contexte d'une écriture native antérieure
            return source;
        }).toList();
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
