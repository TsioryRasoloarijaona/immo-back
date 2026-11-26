package com.tsix.immocrm.services.achat.demandes;

import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.dto.projet.BordereauSaisieDto;
import com.tsix.immocrm.dto.projet.GroupeSaisieDto;
import com.tsix.immocrm.dto.projet.LigneSaisieDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentLine;
import com.tsix.immocrm.entities.buy.BuySelection;
import com.tsix.immocrm.entities.buy.BuySelectionLine;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.services.projet.EcritureBordereauLot;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** R20 : clôture d'une DIA à lot non multi = bordereau définitif du lot régénéré (OMRAN {@code GenerateLinesForMarket}). */
@Component
class GenerationBordereauLot {

    private static final String CODE_INTROUVABLE = "Il y a une erreur au niveaux du bordereau de dépouillement !";

    private final EntityManager em;
    private final BordereauDemandeAchat bordereau;
    private final EcritureBordereauLot bordereauLot;

    GenerationBordereauLot(EntityManager em, BordereauDemandeAchat bordereau, EcritureBordereauLot bordereauLot) {
        this.em = em;
        this.bordereau = bordereau;
        this.bordereauLot = bordereauLot;
    }

    /** Tous les refus (409) avant la première écriture ; codes contrôlés dans les deux sens (voir FEATURE-n6-demandes-achat.md R20). */
    void generer(BuySelection d, PrjMarket lot) {
        BuyDocument offre = em.createQuery("""
                        select b from BuyDocument b where b.buySelectionId = :id and b.docType = 'QUOTE' and b.isSelected = true
                        order by b.code""", BuyDocument.class)
                .setParameter("id", d.getId()).setMaxResults(1).getResultStream().findFirst()
                .orElseThrow(() -> new ConflictException("Aucune offre n'est sélectionnée !"));
        List<BuySelectionLine> lignes = bordereau.lignes(d.getId());
        Set<String> codes = uniques(lignes.stream().map(l -> cle(l.getCode())).toList());
        List<BuyDocumentLine> lignesOffre = em.createQuery("select l from BuyDocumentLine l where l.buyDocumentId = :id",
                BuyDocumentLine.class).setParameter("id", offre.getId()).getResultList();
        uniques(lignesOffre.stream().map(l -> cle(l.getCode())).toList());
        Map<String, BigDecimal> prix = lignesOffre.stream()
                .collect(Collectors.toMap(l -> cle(l.getCode()), GenerationBordereauLot::apresRemise));
        Stream.concat(codes.stream().filter(c -> !prix.containsKey(c)), prix.keySet().stream().filter(c -> !codes.contains(c)))
                .sorted().findFirst().ifPresent(c -> {
                    throw new ConflictException(CODE_INTROUVABLE + " " + c);
                });
        bordereauLot.remplacerDefinitif(lot, new BordereauSaisieDto(
                bordereau.groupes(d.getId()).stream().map(g -> new GroupeSaisieDto(g.getId(), g.getParentId(), g.getSorting(),
                        g.getCode(), g.getDescription(), g.getNotes())).toList(),
                lignes.stream().map(l -> new LigneSaisieDto(UUID.randomUUID(), l.getBuySelectionGroupId(), l.getSorting(), l.getCode(),
                        l.getDescription(), l.getUnity(), l.getQuantity(), prix.get(cle(l.getCode())), pourcent(l.getVatRatio()),
                        pourcent(l.getFodecRatio()), l.getStlCategoryId(), null, l.getNotes())).toList()));
        lignes.forEach(l -> {
            l.setUnitPriceHT(prix.get(cle(l.getCode())));
            l.setUnitPriceTTC(MontantsLot.prixTTC(l.getUnitPriceHT(), zero(l.getFodecRatio()), zero(l.getVatRatio())));
        });
        lot.setCfgSupplier(offre.getCfgTierId() == null ? null : em.find(CfgTier.class, offre.getCfgTierId()));
        lot.setStlSettlementTypeId(d.getStlSettlementTypeId());
        lot.setPaymentDelay(d.getPaymentDelay());
        lot.setLaunchOfTenderDate(d.getLaunchOfTenderDate());
        lot.setWithdrawalTenderDocumentsDate(d.getWithdrawalTenderDocumentsDate());
        lot.setTenderMaturityDate(d.getTenderMaturityDate());
        lot.setTendersResortDate(d.getTendersResortDate());
        lot.setBidOpeningDate(d.getBidOpeningDate());
        lot.setTendersReportDate(d.getTendersReportDate());
        lot.setTenderDate(d.getTenderDate());
        lot.setContractSignatureDate(d.getContractSignatureDate());
    }

    /** {@code AmountHTAfterDiscount} : PU HT × (1 − remise). */
    private static BigDecimal apresRemise(BuyDocumentLine l) {
        return zero(l.getUnitPriceHT()).multiply(BigDecimal.ONE.subtract(zero(l.getDiscountRatio())));
    }

    /** R4 ({@code BuySelection.Verif}) : l'appariement par code exige des codes uniques. */
    private static Set<String> uniques(List<String> codes) {
        Set<String> vus = new HashSet<>();
        codes.stream().filter(c -> !vus.add(c)).findFirst().ifPresent(c -> {
            throw new ConflictException("Le code %s doit être unique pour les lignes du bordereaux !".formatted(c));
        });
        return vus;
    }

    private static String cle(String code) {
        return Objects.requireNonNullElse(code, "");
    }

    private static BigDecimal pourcent(BigDecimal fraction) {
        return zero(fraction).movePointRight(2);
    }

    private static BigDecimal zero(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
    }
}
