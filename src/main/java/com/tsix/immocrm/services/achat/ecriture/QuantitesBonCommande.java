package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentLine;
import com.tsix.immocrm.exceptions.ConflictException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Quantités d'une facture affectée à un BC, si {@code LimitDeliveryAndInvoiceQuantityToOrder} (ligne absente = faux) :
 * OMRAN {@code Invoice.cs:Verif} l.640-657, comparé au BC choisi (FEATURE-achat-fiche-ecritures.md E-B4, Q-G).
 */
@Component
class QuantitesBonCommande {

    private static final String PARAMETRE =
            "SELECT COUNT(*) FROM AdmParamsBoolean WHERE Code = 'LimitDeliveryAndInvoiceQuantityToOrder' AND CodeValue = 1";
    private static final String LIGNES = "SELECT l FROM BuyDocumentLine l WHERE l.buyDocumentId = :piece";
    private static final String DEJA_FACTURE = """
            SELECT l FROM BuyDocumentLine l, BuyDocument d WHERE l.buyDocumentId = d.id AND d.buyOrderId = :bc
            AND d.docType = :facture AND d.id <> :piece""";

    private record Article(UUID categorie, BigDecimal prix, BigDecimal remise, BigDecimal fodec, BigDecimal tva, String code,
                           String description) {
        static Article de(BuyDocumentLine l) {
            return new Article(l.getStlCategoryId(), l.getUnitPriceHT(), l.getDiscountRatio(), l.getFodecRatio(),
                    l.getVatRatio(), l.getCode(), l.getDescription());
        }
    }

    private record Commande(String code, BigDecimal prix) {
        /** Article du BC : même code, PU nul côté BC ou égal. */
        boolean couvre(String code, BigDecimal prix) {
            return Objects.equals(this.code, code) && (estNul(this.prix) || prix != null && this.prix.compareTo(prix) == 0);
        }

        private static boolean estNul(BigDecimal prix) {
            return prix == null || prix.signum() == 0;
        }
    }

    private final EntityManager em;

    QuantitesBonCommande(EntityManager em) {
        this.em = em;
    }

    void verifier(BuyDocument facture, BuyDocument bc) {
        if (((Number) em.createNativeQuery(PARAMETRE).getSingleResult()).intValue() == 0) {
            return;
        }
        Map<Commande, BigDecimal> commandees = sommes(lignes(bc.getId()), l -> new Commande(l.getCode(), l.getUnitPriceHT()));
        List<BuyDocumentLine> dejaFacturees = em.createQuery(DEJA_FACTURE, BuyDocumentLine.class)
                .setParameter("bc", bc.getId()).setParameter("facture", NatureDocumentAchat.INVOICE.name())
                .setParameter("piece", facture.getId()).getResultList();
        sommes(lignes(facture.getId()), Article::de).forEach((article, quantite) -> {
            Map.Entry<Commande, BigDecimal> commande = commandees.entrySet().stream()
                    .filter(e -> e.getKey().couvre(article.code(), article.prix())).findFirst()
                    .orElseThrow(() -> new ConflictException(
                            "L'article '" + article.code() + "' n'existe pas dans le bon de commande !"));
            BigDecimal livree = somme(dejaFacturees.stream()
                    .filter(l -> commande.getKey().couvre(l.getCode(), l.getUnitPriceHT())).toList());
            if (commande.getValue().compareTo(livree.add(quantite)) < 0) {
                throw new ConflictException(
                        "La quantité de l'article '" + article.code() + "' dépasse celle définie dans le bon de commande !");
            }
        });
    }

    private List<BuyDocumentLine> lignes(UUID piece) {
        return em.createQuery(LIGNES, BuyDocumentLine.class).setParameter("piece", piece).getResultList();
    }

    private static <K> Map<K, BigDecimal> sommes(List<BuyDocumentLine> lignes,
                                                 Function<BuyDocumentLine, K> cle) {
        Map<K, BigDecimal> sommes = new LinkedHashMap<>();
        lignes.forEach(l -> sommes.merge(cle.apply(l), quantite(l), BigDecimal::add));
        return sommes;
    }

    private static BigDecimal somme(List<BuyDocumentLine> lignes) {
        return lignes.stream().map(QuantitesBonCommande::quantite).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal quantite(BuyDocumentLine l) {
        return Objects.requireNonNullElse(l.getQuantity(), BigDecimal.ZERO);
    }
}
