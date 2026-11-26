package com.tsix.immocrm.services.achat.demandes;

import com.tsix.immocrm.domain.achat.MontantsPieceAchat;
import com.tsix.immocrm.domain.achat.NatureDocumentAchat;
import com.tsix.immocrm.dto.achat.demandes.BonCommandeDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.ElementDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.LigneOffreDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.OffreDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.PrixLigneOffre;
import com.tsix.immocrm.dto.achat.demandes.SaisieOffreDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.StatutDemandeAchat;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentLine;
import com.tsix.immocrm.entities.buy.BuyDocumentStatus;
import com.tsix.immocrm.entities.buy.BuySelection;
import com.tsix.immocrm.entities.buy.BuySelectionSupplier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.achat.ecriture.EcriturePieceAchat;
import com.tsix.immocrm.services.achat.ecriture.EcrituresPieceAchatService;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Offres (devis QUOTE) d'une DIA, R16-R17, sélection et bons de commande ; droits, R7 et phase contrôlés par l'appelant. */
@Component
class OffresDemandeAchat {

    private static final String DEVIS = "QUOTE";
    private static final String OFFRES = "select b from BuyDocument b where b.buySelectionId = :id and b.docType = 'QUOTE' order by b.code";
    private static final String MARCHE = "Vous ne pouvez pas sélectionnez/désélectionnez une offre pour un marché ";

    private final EntityManager em;
    private final PreselectionDemandeAchat preselection;
    private final BordereauDemandeAchat bordereau;
    private final EcriturePieceAchat ecriture;
    private final EcrituresPieceAchatService pieces;

    OffresDemandeAchat(EntityManager em, PreselectionDemandeAchat preselection, BordereauDemandeAchat bordereau,
                       EcriturePieceAchat ecriture, EcrituresPieceAchatService pieces) {
        this.em = em;
        this.preselection = preselection;
        this.bordereau = bordereau;
        this.ecriture = ecriture;
        this.pieces = pieces;
    }

    List<OffreDemandeAchat> offres(UUID dia, boolean avecEcartees) {
        List<BuyDocument> devis = em.createQuery(OFFRES, BuyDocument.class).setParameter("id", dia).getResultList().stream()
                .filter(o -> avecEcartees || !Boolean.TRUE.equals(o.getIsRejected())).toList();
        if (devis.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<BuyDocumentLine>> lignes = em.createQuery(
                        "select l from BuyDocumentLine l where l.buyDocumentId in :ids order by l.sorting", BuyDocumentLine.class)
                .setParameter("ids", devis.stream().map(BuyDocument::getId).toList()).getResultStream()
                .collect(Collectors.groupingBy(BuyDocumentLine::getBuyDocumentId));
        return devis.stream().map(o -> offre(o, lignes.getOrDefault(o.getId(), List.of()))).toList();
    }

    /** R16 / R17 ({@code BuySelection.AddQuote}) : devis sans statut, une ligne par ligne du bordereau, prix à 0. */
    OffreDemandeAchat creer(BuySelection d, SaisieOffreDemandeAchat saisie) {
        if (saisie == null || saisie.fournisseurId() == null) {
            throw new InvalidRequestException("fournisseurId : obligatoire");
        }
        BuySelectionSupplier s = preselection.duTiers(d.getId(), saisie.fournisseurId())
                .orElseThrow(() -> new InvalidRequestException("fournisseurId : fournisseur non présélectionné"));
        if (Boolean.TRUE.equals(s.getIsRejected())) {
            throw new ConflictException("Vous ne pouvez pas ajouter une offre pour un fournisseur écarté.");
        }
        if (s.getTenderDepositDate() == null) {
            throw new ConflictException("Vous ne pouvez pas ajouter une offre pour un fournisseur dont la date dépôt n'est pas renseigné.");
        }
        if (d.getPrjProjectId() == null) {
            throw new ConflictException("Une offre exige une demande rattachée à un projet.");
        }
        PrjMarket lot = d.getPrjMarketId() == null ? null : em.find(PrjMarket.class, d.getPrjMarketId());
        String gestion = gestion(lot);
        var depot = s.getTenderDepositDate().toLocalDate();
        List<PieceAchatACreer.Ligne> lignes = bordereau.lignes(d.getId()).stream()
                .map(l -> new PieceAchatACreer.Ligne(null, l.getCode(), l.getDescription(), l.getUnity(), l.getQuantity(),
                        BigDecimal.ZERO, BigDecimal.ZERO, l.getFodecRatio(), l.getVatRatio(), l.getStlCategoryId(), l.getNotes(),
                        l.getId())).toList();
        PieceAchatACreer.EnTete entete = new PieceAchatACreer.EnTete(d.getAdmModuleId(), d.getPrjProjectId(), d.getPrjMarketId(),
                d.getCfgTierTriggerId(), depot, depot, null, "", "", "", "", BigDecimal.ZERO, BigDecimal.ZERO);
        BuyDocument o = ecriture.creer(new PieceAchatACreer(null, gestion,
                "La gestion %s n'a aucun statut de départ « Nouveau ».".formatted(gestion), DEVIS, "",
                em.find(CfgTier.class, saisie.fournisseurId()), d.getCfgCompanyId(), d.getPrjMarketTypeId(), null,
                code -> "%s N° %s".formatted(NatureDocumentAchat.QUOTE.libelle(), code), "", lignes, List.of(), List.of(), entete));
        o.setBuySelectionId(d.getId());
        o.setIsRejected(false);
        o.setBuyDocumentStatusId(null);
        if (d.getStlCurrencyId() != null) {
            o.setStlCurrencyId(d.getStlCurrencyId());
        }
        em.flush();
        return offres(d.getId(), true).stream().filter(x -> x.id().equals(o.getId())).findFirst().orElseThrow();
    }

    /** Prix et remises saisis ligne à ligne ; totaux du devis recalculés. */
    OffreDemandeAchat modifier(UUID dia, UUID oid, List<PrixLigneOffre> prix) {
        BuyDocument o = offre(dia, oid);
        if (prix == null || prix.stream().anyMatch(Objects::isNull)) {
            throw new InvalidRequestException("lignes : liste obligatoire, sans élément nul");
        }
        Map<UUID, BuyDocumentLine> lignes = lignes(oid).stream()
                .collect(Collectors.toMap(BuyDocumentLine::getId, Function.identity()));
        for (PrixLigneOffre p : prix) {
            if (p.ligneId() == null || !lignes.containsKey(p.ligneId())) {
                throw new InvalidRequestException("ligneId : ligne inconnue de ce devis");
            }
            verifierPrix(p.prixUnitaireHT(), Objects.requireNonNullElse(p.remise(), BigDecimal.ZERO));
        }
        verifierNonEcartee(o);
        prix.forEach(p -> {
            BuyDocumentLine l = lignes.get(p.ligneId());
            l.setUnitPriceHT(p.prixUnitaireHT());
            l.setDiscountRatio(Objects.requireNonNullElse(p.remise(), BigDecimal.ZERO));
        });
        return recalculer(dia, o, lignes.values());
    }

    List<BuyDocumentLine> lignes(UUID oid) {
        return em.createQuery("select l from BuyDocumentLine l where l.buyDocumentId = :id", BuyDocumentLine.class)
                .setParameter("id", oid).getResultList();
    }

    static void verifierPrix(BigDecimal prixUnitaireHT, BigDecimal remise) {
        if (prixUnitaireHT == null || prixUnitaireHT.signum() < 0 || remise.signum() < 0 || remise.compareTo(BigDecimal.ONE) > 0) {
            throw new InvalidRequestException("prixUnitaireHT positif ou nul, remise entre 0 et 1");
        }
    }

    static void verifierNonEcartee(BuyDocument o) {
        if (Boolean.TRUE.equals(o.getIsRejected())) {
            throw new ConflictException("Vous ne pouvez pas modifier une offre écartée.");
        }
    }

    /** P.U. TTC de chaque ligne, totaux HT / TVA / TTC et net à payer du devis (comparé par R6), puis offre relue. */
    OffreDemandeAchat recalculer(UUID dia, BuyDocument o, Collection<BuyDocumentLine> lignes) {
        lignes.forEach(l -> l.setUnitPriceTTC(montants(l).prixTTC()));
        MontantsPieceAchat.Totaux t = MontantsPieceAchat.totaux(lignes.stream().map(OffresDemandeAchat::montants).toList());
        o.setAmountHT(t.ht());
        o.setAmountVat(t.tva());
        o.setAmountTTC(t.ttc());
        ecriture.recalculerNetAPayer(o.getId());
        em.flush();
        return offres(dia, true).stream().filter(x -> x.id().equals(o.getId())).findFirst().orElseThrow();
    }

    void supprimer(UUID dia, UUID oid) {
        BuyDocument o = offre(dia, oid);
        if (Boolean.TRUE.equals(o.getIsSelected())) {
            throw new ConflictException("Vous ne pouvez pas supprimer une offre sélectionnée.");
        }
        pieces.supprimerContenu(o);
    }

    /** {@code SelectQuote} : seule cette offre sélectionnée ; fournisseur et, à défaut, délai de paiement de la DIA. */
    void selectionner(BuySelection d, UUID oid, boolean confirme) {
        BuyDocument o = offre(d.getId(), oid);
        verifierMarche(d);
        boolean ecarte = Boolean.TRUE.equals(o.getIsRejected()) || preselection.duTiers(d.getId(), o.getCfgTierId())
                .map(s -> Boolean.TRUE.equals(s.getIsRejected())).orElse(false);
        if (ecarte) {
            throw new ConflictException("Vous ne pouvez pas selectionnez l'offre d'un fournisseur écarté.");
        }
        List<BuyDocument> toutes = em.createQuery(OFFRES, BuyDocument.class).setParameter("id", d.getId()).getResultList();
        BigDecimal montant = Objects.requireNonNullElse(o.getAmountToPay(), BigDecimal.ZERO);
        boolean plusDisante = toutes.size() >= 2 && toutes.stream()
                .allMatch(x -> Objects.requireNonNullElse(x.getAmountToPay(), BigDecimal.ZERO).compareTo(montant) <= 0);
        if (plusDisante && !confirme) {
            throw new ConflictException("Voulez vous continuer avec l'offre la plus disante ?", "OFFRE_LA_PLUS_DISANTE");
        }
        toutes.forEach(x -> x.setIsSelected(x.getId().equals(oid)));
        CfgTier tiers = o.getCfgTierId() == null ? null : em.find(CfgTier.class, o.getCfgTierId());
        d.setCfgTier(tiers);
        if ((d.getPaymentDelay() == null || d.getPaymentDelay() <= 0) && tiers != null) {
            d.setPaymentDelay(tiers.getPaymentDelay());
        }
        em.flush();
    }

    /** {@code DeSelectQuote} : toutes désélectionnées, bordereau de la DIA vidé, fournisseur retiré ; le lot est intact. */
    void deselectionner(BuySelection d) {
        verifierMarche(d);
        List<BuyDocument> toutes = em.createQuery(OFFRES, BuyDocument.class).setParameter("id", d.getId()).getResultList();
        if (toutes.stream().noneMatch(x -> Boolean.TRUE.equals(x.getIsSelected()))) {
            throw new ConflictException("Vous ne pouvez pas désélectionner une offre non sélectionnée.");
        }
        toutes.forEach(x -> x.setIsSelected(false));
        d.setCfgTier(null);
        em.flush();
        bordereau.vider(d.getId());
    }

    List<BonCommandeDemandeAchat> bonsCommande(UUID dia) {
        return em.createQuery("""
                        select b, t, s from BuyDocument b left join CfgTier t on t.id = b.cfgTierId
                        left join BuyDocumentStatus s on s.id = b.buyDocumentStatusId
                        where b.buySelectionId = :id and (b.docType is null or b.docType <> 'QUOTE') order by b.docDate, b.code""",
                        Object[].class).setParameter("id", dia).getResultStream()
                .map(r -> bonCommande((BuyDocument) r[0], (CfgTier) r[1], (BuyDocumentStatus) r[2])).toList();
    }

    /** {@code CanSelectDeSelectQuote} : lot exigé (OMRAN {@code toolSelectQuote}), ni décompte, ni affectation de décomposition (Q-b3). */
    private void verifierMarche(BuySelection d) {
        UUID lot = d.getPrjMarketId();
        if (lot == null) {
            throw new ConflictException("Vous ne pouvez pas sélectionnez/désélectionnez une offre d'une demande sans lot.");
        }
        PrjMarket marche = em.find(PrjMarket.class, lot);
        if (marche != null && Boolean.TRUE.equals(marche.getIsMultiSupplier())) {
            if (compter("SELECT COUNT(*) FROM BuyDocument WHERE BuySelectionId = :lot AND DocType <> 'QUOTE'",
                    d.getId()) > 0) {
                throw new ConflictException("Vous ne pouvez pas sélectionnez/désélectionnez une offre pour un lot multi-fournisseur "
                        + "contenant des documents affectés à cet appel d'offre !");
            }
            return;
        }
        if (compter("SELECT COUNT(*) FROM PrjWorkingOut WHERE PrjMarketId = :lot", lot) > 0) {
            throw new ConflictException(MARCHE + "contenant des décomptes !");
        }
        boolean decomposition = em.createNativeQuery("SELECT OBJECT_ID('dbo.PrjMarketViewLine', 'U')").getSingleResult() != null
                && compter("""
                SELECT COUNT(*) FROM PrjMarketViewLine l JOIN PrjMarketView v ON v.PKey = l.PrjMarketViewId
                WHERE v.PrjMarketId = :lot""", lot) > 0;
        if (decomposition) {
            throw new ConflictException(MARCHE + "avec des affectations de décomposition !");
        }
    }

    private long compter(String sql, UUID lot) {
        return ((Number) em.createNativeQuery(sql).setParameter("lot", lot).getSingleResult()).longValue();
    }

    BuyDocument offre(UUID dia, UUID oid) {
        return Optional.ofNullable(em.find(BuyDocument.class, oid))
                .filter(o -> dia.equals(o.getBuySelectionId()) && DEVIS.equals(o.getDocType()))
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable : " + oid));
    }

    private static String gestion(PrjMarket lot) {
        if (lot == null) {
            return NatureDocumentAchat.QUOTE.gestion();
        }
        return Boolean.TRUE.equals(lot.getIsMultiSupplier()) ? "PrjMarketMultiSupplierQuote" : "PrjMarketQuote";
    }

    private OffreDemandeAchat offre(BuyDocument o, List<BuyDocumentLine> lignes) {
        MontantsPieceAchat.Totaux t = MontantsPieceAchat.totaux(lignes.stream().map(OffresDemandeAchat::montants).toList());
        return new OffreDemandeAchat(o.getId(), o.getCode(), tiers(o.getCfgTierId()), PreselectionDemandeAchat.jour(o.getDocDate()),
                t.ht(), t.ttc(), Boolean.TRUE.equals(o.getIsSelected()), Boolean.TRUE.equals(o.getIsRejected()),
                lignes.stream().map(OffresDemandeAchat::ligne).toList());
    }

    private static LigneOffreDemandeAchat ligne(BuyDocumentLine l) {
        BigDecimal pu = Objects.requireNonNullElse(l.getUnitPriceHT(), BigDecimal.ZERO);
        BigDecimal apres = pu.multiply(BigDecimal.ONE.subtract(Objects.requireNonNullElse(l.getDiscountRatio(), BigDecimal.ZERO)));
        return new LigneOffreDemandeAchat(l.getId(), l.getSorting(), l.getCode(), l.getDescription(), l.getUnity(), l.getQuantity(), pu,
                l.getDiscountRatio(), apres, l.getVatRatio(),
                apres.multiply(Objects.requireNonNullElse(l.getQuantity(), BigDecimal.ZERO)));
    }

    private static MontantsPieceAchat.Ligne montants(BuyDocumentLine l) {
        return new MontantsPieceAchat.Ligne(l.getQuantity(), Objects.requireNonNullElse(l.getUnitPriceHT(), BigDecimal.ZERO),
                l.getDiscountRatio(), l.getFodecRatio(), l.getVatRatio());
    }

    private ElementDemandeAchat tiers(UUID id) {
        CfgTier t = id == null ? null : em.find(CfgTier.class, id);
        return t == null ? null : new ElementDemandeAchat(t.getId(), t.getCode(), t.getDescription());
    }

    private static BonCommandeDemandeAchat bonCommande(BuyDocument b, CfgTier t, BuyDocumentStatus s) {
        return new BonCommandeDemandeAchat(b.getId(), b.getCode(), b.getDocType(), PreselectionDemandeAchat.jour(b.getDocDate()),
                t == null ? null : new ElementDemandeAchat(t.getId(), t.getCode(), t.getDescription()),
                s == null ? null : new StatutDemandeAchat(s.getId(), s.getDescription(), s.getColorName()), b.getAmountTTC());
    }
}
