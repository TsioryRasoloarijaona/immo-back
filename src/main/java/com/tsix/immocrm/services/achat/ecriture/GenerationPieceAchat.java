package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.dto.achat.SaisiePieceAchatRequest;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentLine;
import com.tsix.immocrm.entities.buy.BuyDocumentStatus;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Generation;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer.Ligne;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.tsix.immocrm.domain.achat.NatureDocumentAchat.DELIVRY;
import static com.tsix.immocrm.domain.achat.NatureDocumentAchat.INVOICE;
import static com.tsix.immocrm.domain.achat.NatureDocumentAchat.ORDER;
import static com.tsix.immocrm.domain.achat.NatureDocumentAchat.QUOTE;
import static com.tsix.immocrm.domain.achat.NatureDocumentAchat.REFUND;

/**
 * Préconditions d'une génération (FEATURE-achat-generation.md G2, G3, G5 ; OMRAN {@code DocumentEdition.cs},
 * {@code Invoice.cs:Create}) : 400 cible non générable, puis 409 ; seule règle du modèle, du POST et des actions.
 */
@Component
class GenerationPieceAchat {

    // Paires (gestion source › nature cible) → gestion d'arrivée mesurées en prod (G3)
    private static final Map<String, String> PAIRES = Map.of(
            "BuyQuote>ORDER", "BuyOrder",
            "PrjMarketMultiSupplierQuote>ORDER", "PrjMarketMultiSupplierOrder",
            "BuyOrder>DELIVRY", "BuyDelivery",
            "BuyOrder>INVOICE", "BuyInvoiceDebit",
            "BuyDelivery>INVOICE", "BuyInvoiceDebit",
            "PrjMarketMultiSupplierOrder>DELIVRY", "PrjMarketMultiSupplierDelivery",
            "PrjMarketMultiSupplierOrder>INVOICE", "PrjInvoiceDebitMultiSupplier",
            "PrjMarketMultiSupplierDelivery>INVOICE", "PrjInvoiceDebitMultiSupplier",
            "PrjInvoiceDebit>REFUND", "BuyRefundDebit");
    private static final String PARAMETRE = "SELECT COUNT(*) FROM AdmParamsBoolean WHERE Code = :code AND CodeValue = 1";

    private final EntityManager em;
    private final PreremplissagePieceAchat preremplissage;
    private final QuantitesBonCommande quantites;

    GenerationPieceAchat(EntityManager em, PreremplissagePieceAchat preremplissage, QuantitesBonCommande quantites) {
        this.em = em;
        this.preremplissage = preremplissage;
        this.quantites = quantites;
    }

    /** Génération de {@code cible} depuis {@code sources} (non vides, visibles) ; 400 / 409 au premier refus. */
    Generation verifier(NatureDocumentAchat cible, List<BuyDocument> sources) {
        if (cible == QUOTE) {
            throw new InvalidRequestException("sources : un devis ne se génère pas depuis une autre pièce.");
        }
        BuyDocument premiere = sources.getFirst();
        NatureDocumentAchat source = NatureDocumentAchat.saisie(premiere.getDocType()).orElse(null);
        boolean memeNature = sources.stream().allMatch(s -> premiere.getDocType().equals(s.getDocType()));
        boolean unique = sources.size() == 1;
        boolean permise = memeNature && switch (cible) {
            case ORDER -> unique && source == QUOTE;
            case DELIVRY -> unique && source == ORDER;
            case REFUND -> unique && source == INVOICE;
            default -> source == DELIVRY || unique && source == ORDER;
        };
        if (!permise) {
            throw new ConflictException("Impossible de générer %s depuis ces pièces.".formatted(cible.libelle()));
        }
        String gestion = gestionDArrivee(premiere, cible);
        if (source == DELIVRY) {
            verifierLivraisons(sources);
        } else if (source == ORDER) {
            verifierCommande(cible, premiere);
        }
        return liens(cible, gestion, sources);
    }

    boolean permise(NatureDocumentAchat cible, BuyDocument source) {
        try {
            verifier(cible, List.of(source));
            return true;
        } catch (ConflictException | InvalidRequestException refus) {
            return false;
        }
    }

    SaisiePieceAchatRequest saisie(NatureDocumentAchat cible, List<BuyDocument> sources) {
        return preremplissage.saisie(cible, sources, avoirFinancier(cible));
    }

    /** Case « Avoir financier » par défaut : {@code DefaultRefundIsFinancial} (faux si absent) ; null hors avoir (D3). */
    private Boolean avoirFinancier(NatureDocumentAchat cible) {
        return cible == REFUND ? parametre("DefaultRefundIsFinancial") : null;
    }

    /** Lignes de bordereau des sources : seules recopiables au POST (§3). */
    Set<UUID> bordereaux(List<BuyDocument> sources) {
        return sources.isEmpty() ? Set.of() : em.createQuery("SELECT l.prjMarketLineId FROM BuyDocumentLine l WHERE l.buyDocumentId IN :pieces "
                        + "AND l.prjMarketLineId IS NOT NULL", UUID.class)
                .setParameter("pieces", sources.stream().map(BuyDocument::getId).toList()).getResultStream()
                .collect(Collectors.toSet());
    }

    /** BL ou facture rattachés à un BC : quantités du BC si {@code LimitDeliveryAndInvoiceQuantityToOrder} (E-B4, G6). */
    void verifierQuantites(BuyDocument d, List<Ligne> lignes) {
        boolean controlee = INVOICE.name().equals(d.getDocType()) || DELIVRY.name().equals(d.getDocType());
        if (controlee && d.getBuyOrderId() != null) {
            quantites.verifier(d, Objects.requireNonNull(em.find(BuyDocument.class, d.getBuyOrderId())),
                    lignes.stream().map(l -> EcriturePieceAchat.remplir(new BuyDocumentLine(), l, 0, d)).toList());
        }
    }

    private String gestionDArrivee(BuyDocument source, NatureDocumentAchat cible) {
        StrEntity gestion = source.getStrEntityId() == null ? null : em.find(StrEntity.class, source.getStrEntityId());
        String code = gestion == null ? null : gestion.getCode();
        String arrivee = PAIRES.get(code + ">" + cible.name());
        if (arrivee == null) {
            throw new ConflictException("Génération non configurée pour la gestion %s"
                    .formatted(Objects.requireNonNullElse(code, "(aucune)")));
        }
        return arrivee;
    }

    /** OMRAN {@code Invoice.cs:Create(List<Delivery>)} l.909-924, dans cet ordre. */
    private void verifierLivraisons(List<BuyDocument> livraisons) {
        if (distincts(livraisons, BuyDocument::getCfgTierId)) {
            throw new ConflictException("Veuillez choisir des bons de livraison générer pour le même fournisseur");
        }
        if (distincts(livraisons, BuyDocument::getBuyOrderId) && !parametre("GenerateInvoiceFromManyOrders")) {
            throw new ConflictException("Veuillez choisir des bons de livraison générer du même bon de commande");
        }
        if (livraisons.stream().anyMatch(l -> l.getBuyInvoiceId() != null)) {
            throw new ConflictException("Vous ne pouvez pas générer une facture sur un bon de livraison déja facturé.");
        }
        if (!livraisons.stream().allMatch(this::validee)) {
            throw new ConflictException(
                    "Vous ne pouvez pas ajouter une facture pour un bon de livraison qui n'est pas validé !");
        }
    }

    private void verifierCommande(NatureDocumentAchat cible, BuyDocument bc) {
        if (cible == INVOICE) {
            if (!parametre("GenerateInvoiceFromOrder")) {
                throw new ConflictException(
                        "La génération d'une facture depuis un bon de commande n'est pas activée (GenerateInvoiceFromOrder).");
            }
            if (!validee(bc)) {
                throw new ConflictException(
                        "Vous ne pouvez pas ajouter une facture pour un bon de commande qui n'est pas validé !");
            }
            return;
        }
        if (!validee(bc)) {
            throw new ConflictException(
                    "Vous ne pouvez pas ajouter un bon de livraison pour un bon de commande qui n'est pas validé !");
        }
        if (preremplissage.lignes(DELIVRY, List.of(bc)).isEmpty()) {
            throw new ConflictException("Toutes les quantités de ce bon de commande sont déjà livrées.");
        }
    }

    /**
     * {@code BuyOrderId} = l'unique BC des sources, sinon l'unique {@code BuyOrderId} commun (l.2413-2431) ; facture : BL
     * facturés et BC distincts de {@code BuyOrderInvoice}.
     */
    private Generation liens(NatureDocumentAchat cible, String gestion, List<BuyDocument> sources) {
        BuyDocument premiere = sources.getFirst();
        boolean depuisBc = ORDER.name().equals(premiere.getDocType());
        List<UUID> commandesSources = sources.stream()
                .map(s -> depuisBc ? s.getId() : s.getBuyOrderId()).distinct().toList();
        UUID commande = commandesSources.size() == 1 ? commandesSources.getFirst() : null;
        boolean facture = cible == INVOICE;
        UUID devis = QUOTE.name().equals(premiere.getDocType()) ? premiere.getId() : null;
        List<UUID> livraisons = facture && !depuisBc ? sources.stream().map(BuyDocument::getId).toList() : List.of();
        List<UUID> commandes = facture ? commandesSources.stream().filter(Objects::nonNull).toList() : List.of();
        UUID factureSource = cible == REFUND ? premiere.getId() : null;
        return new Generation(gestion, premiere.getStlCurrencyId(), devis, commande, livraisons, commandes, factureSource,
                avoirFinancier(cible));
    }

    private boolean validee(BuyDocument d) {
        BuyDocumentStatus statut = d.getBuyDocumentStatusId() == null ? null
                : em.find(BuyDocumentStatus.class, d.getBuyDocumentStatusId());
        return statut != null && Boolean.TRUE.equals(statut.getIsOkForPayement());
    }

    private boolean parametre(String code) {
        return ((Number) em.createNativeQuery(PARAMETRE).setParameter("code", code).getSingleResult()).intValue() > 0;
    }

    private static boolean distincts(List<BuyDocument> sources, Function<BuyDocument, UUID> cle) {
        return sources.stream().map(cle).distinct().count() > 1;
    }
}
