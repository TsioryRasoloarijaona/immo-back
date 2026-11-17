package com.tsix.immocrm.services.achat.ecriture;

import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.dto.achat.SaisiePieceAchatRequest;
import com.tsix.immocrm.dto.achat.SaisiePieceAchatRequest.LigneSaisieAchat;
import com.tsix.immocrm.dto.achat.SaisiePieceAchatRequest.PartVentilationAchat;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentLine;
import com.tsix.immocrm.entities.buy.BuyVentilationExpense;
import com.tsix.immocrm.entities.cfg.CfgTier;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Modèle d'une pièce générée (FEATURE-achat-generation.md G4, G5 ; OMRAN {@code DocumentBase.GenateDocFromDocs},
 * {@code OrderList.GenerateDelivry}) : rien n'est écrit ici.
 */
@Component
class PreremplissagePieceAchat {

    private static final String LIGNES = "SELECT l FROM BuyDocumentLine l WHERE l.buyDocumentId = :piece "
            + "ORDER BY l.sorting ASC, l.id ASC";
    private static final String LIVRE = """
            SELECT l FROM BuyDocumentLine l, BuyDocument d WHERE l.buyDocumentId = d.id AND d.buyOrderId = :bc
            AND d.docType = 'DELIVRY'""";

    /** Clé de fusion G4 ; null = null (décision Q-C). */
    private record Cle(UUID bordereau, UUID categorie, BigDecimal prix, BigDecimal remise, BigDecimal fodec,
                       BigDecimal tva, String code, String description) {
        static Cle de(BuyDocumentLine l) {
            return new Cle(l.getPrjMarketLineId(), l.getStlCategoryId(), norme(l.getUnitPriceHT()),
                    norme(l.getDiscountRatio()), norme(l.getFodecRatio()), norme(l.getVatRatio()), l.getCode(),
                    l.getDescription());
        }

        private static BigDecimal norme(BigDecimal v) {
            return v == null ? null : v.stripTrailingZeros();
        }
    }

    private final EntityManager em;
    private final EcriturePieceAchat ecriture;
    private final Clock clock;

    PreremplissagePieceAchat(EntityManager em, EcriturePieceAchat ecriture, Clock clock) {
        this.em = em;
        this.ecriture = ecriture;
        this.clock = clock;
    }

    /**
     * En-tête de la 1re source, sommes, date du jour, échéance recalculée d'une facture, ventilation projet de la 1re.
     * Avoir (OMRAN {@code Refund.cs:Create}) : toutes les lignes clonées, ni régul ni timbre ; facture sans ligne : HT,
     * TVA et 1re catégorie ventilée recopiés. {@code avoirFinancier} : case par défaut, null hors avoir.
     */
    SaisiePieceAchatRequest saisie(NatureDocumentAchat cible, List<BuyDocument> sources, Boolean avoirFinancier) {
        BuyDocument p = sources.getFirst();
        LocalDate jour = LocalDate.now(clock);
        LocalDate echeance = cible == NatureDocumentAchat.INVOICE ? ecriture.echeance(jour,
                p.getCfgTierId() == null ? null : em.find(CfgTier.class, p.getCfgTierId()), p.getPrjMarketId()) : null;
        List<PartVentilationAchat> projets = em.createQuery(
                        "SELECT v FROM BuyVentilationExpense v WHERE v.buyDocumentId = :piece", BuyVentilationExpense.class)
                .setParameter("piece", p.getId()).getResultStream()
                .map(v -> new PartVentilationAchat(v.getPrjProjectId(), v.getVentilationRate())).toList();
        boolean avoir = cible == NatureDocumentAchat.REFUND;
        List<LigneSaisieAchat> lignes = avoir ? lignesDe(p).map(l -> saisie(l, quantite(l.getQuantity()))).toList()
                : lignes(cible, sources);
        boolean sansLigne = avoir && lignes.isEmpty();
        UUID categorie = sansLigne ? em.createQuery("SELECT v.stlCategoryId FROM BuyVentilationCategory v "
                        + "WHERE v.buyDocumentId = :piece ORDER BY v.id ASC", UUID.class)
                .setParameter("piece", p.getId()).setMaxResults(1).getResultStream().findFirst().orElse(null) : null;
        return new SaisiePieceAchatRequest(cible.name(), jour, echeance, jour, p.getCfgCompanyId(), p.getAdmModuleId(),
                p.getCfgTierId(), p.getPrjProjectId(), p.getPrjMarketId(), p.getPrjMarketTypeId(), p.getCfgTierTriggerId(),
                texte(p.getPersonToContact()), texte(p.getDocRef()), texte(p.getOfficeOrderRef()), texte(p.getNotes()),
                texte(p.getNotes2()), avoir ? BigDecimal.ZERO : somme(sources, BuyDocument::getAmountRegul),
                avoir ? BigDecimal.ZERO : somme(sources, BuyDocument::getFiscalStamp), categorie, lignes, projets,
                sources.stream().map(BuyDocument::getId).toList(), sansLigne ? quantite(p.getAmountHT()) : null,
                sansLigne ? quantite(p.getAmountVat()) : null, avoirFinancier);
    }

    private Stream<BuyDocumentLine> lignesDe(BuyDocument piece) {
        return em.createQuery(LIGNES, BuyDocumentLine.class).setParameter("piece", piece.getId()).getResultStream();
    }

    /** Lignes de quantité non nulle fusionnées ; BL : restant à livrer du BC, lignes soldées omises (G5, Q-B). */
    List<LigneSaisieAchat> lignes(NatureDocumentAchat cible, List<BuyDocument> sources) {
        Map<Cle, LigneSaisieAchat> fusion = new LinkedHashMap<>();
        sources.forEach(s -> lignesDe(s).filter(l -> quantite(l.getQuantity()).signum() != 0)
                .forEach(l -> fusion.merge(Cle.de(l), saisie(l, quantite(l.getQuantity())),
                        (a, b) -> avec(a, a.quantite().add(b.quantite())))));
        List<LigneSaisieAchat> lignes = List.copyOf(fusion.values());
        return cible == NatureDocumentAchat.DELIVRY ? restantes(lignes, sources.getFirst().getId()) : lignes;
    }

    /** Commandé − déjà livré par code sur tous les BL du BC, quel que soit leur statut ; le livré s'impute ligne à ligne. */
    private List<LigneSaisieAchat> restantes(List<LigneSaisieAchat> commandees, UUID bc) {
        Map<String, BigDecimal> livre = new HashMap<>();
        em.createQuery(LIVRE, BuyDocumentLine.class).setParameter("bc", bc).getResultStream()
                .forEach(l -> livre.merge(texte(l.getCode()), quantite(l.getQuantity()), BigDecimal::add));
        return commandees.stream().map(l -> {
            BigDecimal deja = livre.getOrDefault(texte(l.code()), BigDecimal.ZERO).max(BigDecimal.ZERO);
            BigDecimal impute = deja.min(l.quantite().max(BigDecimal.ZERO));
            livre.put(texte(l.code()), deja.subtract(impute));
            return avec(l, l.quantite().subtract(impute));
        }).filter(l -> l.quantite().signum() > 0).toList();
    }

    private static LigneSaisieAchat saisie(BuyDocumentLine l, BigDecimal quantite) {
        return new LigneSaisieAchat(null, l.getCode(), l.getDescription(), l.getUnity(), quantite, l.getUnitPriceHT(),
                l.getDiscountRatio(), l.getFodecRatio(), l.getVatRatio(), l.getStlCategoryId(), l.getNotes(),
                l.getPrjMarketLineId());
    }

    private static LigneSaisieAchat avec(LigneSaisieAchat l, BigDecimal quantite) {
        return new LigneSaisieAchat(null, l.code(), l.description(), l.unite(), quantite, l.prixUnitaireHT(), l.remise(),
                l.fodec(), l.tva(), l.categorieId(), l.notes(), l.ligneBordereauId());
    }

    private static BigDecimal somme(List<BuyDocument> sources, Function<BuyDocument, BigDecimal> montant) {
        return sources.stream().map(montant).map(PreremplissagePieceAchat::quantite).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal quantite(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
    }

    private static String texte(String v) {
        return Objects.requireNonNullElse(v, "");
    }
}
