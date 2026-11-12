package com.tsix.immocrm.domain.achat;

import com.tsix.immocrm.dto.achat.AgregatsBonCommandeDto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Agrégats d'un bon de commande sur ses pièces liées ({@code BuyOrderId}), formules de {@code BuyDocumentOrderView} :
 * seul calcul (règle 6), voir FEATURE-achat-bc-agregats.md. Arrondi HALF_UP à 2 décimales sur chaque agrégat final.
 */
public final class AgregatsBonCommande {

    static final String AVANCE = "ORDER_ADVANCE";
    static final String GARANTIE = "ORDER_WARRANTY";
    private static final int ECHELLE_TAUX = 6;

    /** Montants du bon de commande ; {@code devisReference} / {@code devisCode} = DocRef / Code du devis lié. */
    public record Commande(BigDecimal ht, BigDecimal ttc, BigDecimal aPayer, BigDecimal tauxAvance,
                           String devisReference, String devisCode) {
    }

    /** Pièce liée ; {@code regle} = {@code SoldeDocumentAchat.SQL_REGLE}, {@code factureDuBl} = code de BuyInvoiceId. */
    public record PieceLiee(String nature, String referenceType, String code, BigDecimal ht, BigDecimal ttc,
                            BigDecimal aPayer, BigDecimal avanceAmortie, BigDecimal garantie, BigDecimal regle,
                            String factureDuBl) {
    }

    private AgregatsBonCommande() {
    }

    public static AgregatsBonCommandeDto de(Commande bc, List<PieceLiee> pieces) {
        List<PieceLiee> factures = parmi(pieces, p -> NatureDocumentAchat.INVOICE.name().equals(p.nature()));
        List<PieceLiee> livraisons = parmi(pieces, p -> NatureDocumentAchat.DELIVRY.name().equals(p.nature()));
        List<PieceLiee> avances = parmi(pieces, p -> AVANCE.equals(p.referenceType()));
        BigDecimal factureHT = somme(factures, PieceLiee::ht);
        BigDecimal factureTTC = somme(factures, PieceLiee::ttc);
        BigDecimal factureNet = somme(factures, PieceLiee::aPayer);
        BigDecimal livreHT = somme(livraisons, PieceLiee::ht);
        BigDecimal livreTTC = somme(livraisons, PieceLiee::ttc);
        BigDecimal livreNet = somme(livraisons, PieceLiee::aPayer);
        BigDecimal avance = somme(avances, PieceLiee::aPayer);
        BigDecimal avanceRegle = somme(avances, PieceLiee::regle);
        BigDecimal amortie = somme(factures, PieceLiee::avanceAmortie);
        BigDecimal retenue = somme(factures, PieceLiee::garantie);
        BigDecimal restituee = somme(parmi(pieces, p -> GARANTIE.equals(p.referenceType())), PieceLiee::ht);
        BigDecimal regle = somme(factures, PieceLiee::regle).add(avanceRegle);
        BigDecimal aPayer = zero(bc.aPayer());
        BigDecimal tauxEffectif = aPayer.signum() == 0 ? BigDecimal.ZERO
                : avance.divide(aPayer, ECHELLE_TAUX, RoundingMode.HALF_UP);
        return new AgregatsBonCommandeDto(arrondi(factureHT), arrondi(factureTTC.subtract(factureHT)),
                arrondi(factureTTC), arrondi(factureNet), arrondi(livreHT), arrondi(livreTTC), arrondi(livreNet),
                arrondi(zero(bc.ht()).subtract(livreHT)), arrondi(zero(bc.ttc()).subtract(livreTTC)),
                arrondi(aPayer.subtract(livreNet)), arrondi(avance), zero(bc.tauxAvance()), tauxEffectif,
                arrondi(amortie), arrondi(avanceRegle), arrondi(avance.subtract(avanceRegle)),
                arrondi(avance.subtract(amortie)), arrondi(retenue), arrondi(restituee),
                arrondi(retenue.subtract(restituee)), arrondi(regle), arrondi(factureNet.subtract(regle)),
                arrondi(aPayer.subtract(regle)), codes(livraisons.stream().map(PieceLiee::code)),
                codes(Stream.concat(factures.stream().map(PieceLiee::code),
                        livraisons.stream().map(PieceLiee::factureDuBl))),
                bc.devisReference() != null ? bc.devisReference() : bc.devisCode());
    }

    /** Σ champ à champ des montants (lignes déjà arrondies : total = Σ affichée) ; ratios et codes null ; null si vide. */
    public static AgregatsBonCommandeDto total(Collection<AgregatsBonCommandeDto> bons) {
        if (bons.isEmpty()) {
            return null;
        }
        Function<Function<AgregatsBonCommandeDto, BigDecimal>, BigDecimal> s = champ -> somme(bons, champ);
        return new AgregatsBonCommandeDto(s.apply(AgregatsBonCommandeDto::factureHT),
                s.apply(AgregatsBonCommandeDto::factureTVA), s.apply(AgregatsBonCommandeDto::factureTTC),
                s.apply(AgregatsBonCommandeDto::factureNetAPayer), s.apply(AgregatsBonCommandeDto::livreHT),
                s.apply(AgregatsBonCommandeDto::livreTTC), s.apply(AgregatsBonCommandeDto::livreNetAPayer),
                s.apply(AgregatsBonCommandeDto::resteALivrerHT), s.apply(AgregatsBonCommandeDto::resteALivrerTTC),
                s.apply(AgregatsBonCommandeDto::resteALivrerNetAPayer), s.apply(AgregatsBonCommandeDto::avance),
                null, null, s.apply(AgregatsBonCommandeDto::avanceAmortie),
                s.apply(AgregatsBonCommandeDto::avanceRegle), s.apply(AgregatsBonCommandeDto::avanceReste),
                s.apply(AgregatsBonCommandeDto::avanceResteAAmortir), s.apply(AgregatsBonCommandeDto::garantieRetenue),
                s.apply(AgregatsBonCommandeDto::garantieRestituee), s.apply(AgregatsBonCommandeDto::garantieReste),
                s.apply(AgregatsBonCommandeDto::regle), s.apply(AgregatsBonCommandeDto::resteAPayerSurFacture),
                s.apply(AgregatsBonCommandeDto::resteAPayerTotal), null, null, null);
    }

    private static List<PieceLiee> parmi(List<PieceLiee> pieces, Predicate<PieceLiee> filtre) {
        return pieces.stream().filter(filtre).toList();
    }

    private static <T> BigDecimal somme(Collection<T> elements, Function<T, BigDecimal> champ) {
        return elements.stream().map(champ).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String codes(Stream<String> codes) {
        String liste = codes.filter(Objects::nonNull).distinct().sorted().collect(Collectors.joining(", "));
        return liste.isEmpty() ? null : liste;
    }

    private static BigDecimal zero(BigDecimal montant) {
        return montant == null ? BigDecimal.ZERO : montant;
    }

    private static BigDecimal arrondi(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP);
    }
}
