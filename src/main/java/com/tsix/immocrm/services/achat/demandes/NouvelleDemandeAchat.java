package com.tsix.immocrm.services.achat.demandes;

import com.tsix.immocrm.dto.achat.demandes.SaisieDemandeAchatLot;
import com.tsix.immocrm.entities.buy.BuySelection;
import com.tsix.immocrm.entities.buy.BuySelectionKind;
import com.tsix.immocrm.entities.buy.BuySelectionPaymentTerm;
import com.tsix.immocrm.entities.buy.BuySelectionSupplier;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketLineEstimation;
import com.tsix.immocrm.entities.prj.PrjMarketLineGroupingEstimation;
import com.tsix.immocrm.entities.prj.PrjMarketType;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.achat.ecriture.ConfigurationPieceAchat;
import com.tsix.immocrm.services.projet.LotsProjetService;
import com.tsix.immocrm.services.workflow.MoteurWorkflowStr;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** R18 (DIA depuis un lot) et R19 (renouvellement d'une DIA annulée) ; visibilité et droits contrôlés par l'appelant. */
@Component
class NouvelleDemandeAchat {

    private static final String NON_STOCK = " and (x.isStock is null or x.isStock = false) order by x.sorting, x.code";

    private final EntityManager em;
    private final EcrituresDemandeAchat ecritures;
    private final BordereauDemandeAchat bordereau;
    private final LotsProjetService lots;
    private final ConfigurationPieceAchat configuration;
    private final UtilisateurAffiche utilisateur;
    private final Clock clock;

    NouvelleDemandeAchat(EntityManager em, EcrituresDemandeAchat ecritures, BordereauDemandeAchat bordereau, LotsProjetService lots,
                         ConfigurationPieceAchat configuration, UtilisateurAffiche utilisateur, Clock clock) {
        this.em = em;
        this.ecritures = ecritures;
        this.bordereau = bordereau;
        this.lots = lots;
        this.configuration = configuration;
        this.utilisateur = utilisateur;
        this.clock = clock;
    }

    /** Lot visible de l'appelant, sinon 404. */
    PrjMarket lot(UUID id) {
        return lots.visible(id);
    }

    /** R18 ({@code New(MarketMonitoring)}) : statut du lot « Avec DIA », puis corps, puis aucune autre DIA non annulée (lot non multi). */
    UUID depuisLot(PrjMarket lot, SaisieDemandeAchatLot saisie) {
        if (!lots.peutAjouterDia(lot)) {
            throw new ConflictException("Le statut du lot ne permet pas de créer une demande interne d'achat.");
        }
        if (saisie == null || saisie.description() == null || saisie.description().isBlank()) {
            throw new InvalidRequestException("description : obligatoire");
        }
        if (saisie.description().trim().length() > 255) {
            throw new InvalidRequestException("description : 255 caractères au plus");
        }
        BigDecimal estime = Objects.requireNonNullElse(lot.getEstimatedPriceTTCManual(), BigDecimal.ZERO);
        BuySelectionKind procedure = estime.signum() != 0 ? ecritures.procedureParMontant(estime).orElse(null)
                : Optional.ofNullable(saisie.procedureId()).map(id -> em.find(BuySelectionKind.class, id)).orElse(null);
        if (procedure == null) {
            throw new InvalidRequestException("procedureId : obligatoire et existante");
        }
        if (!Boolean.TRUE.equals(lot.getIsMultiSupplier()) && em.createQuery("""
                        select count(d) from BuySelection d where d.prjMarketId = :lot
                        and (d.strStatus is null or d.strStatus.strStatusTypeId is null or d.strStatus.strStatusTypeId <> :annulation)""",
                        Long.class).setParameter("lot", lot.getId()).setParameter("annulation", MoteurWorkflowStr.TYPE_ANNULATION)
                .getSingleResult() > 0) {
            throw new ConflictException("Pour pouvoir ajouter un nouvel appel d'offre il faut d'abords annuler tous les autres !");
        }
        StrEntity entite = EcrituresDemandeAchat.entiteDe(procedure);
        BuySelection d = nouvelle(procedure, entite, ecritures.depart(entite));
        d.setGroupId(d.getId());
        d.setAdmModuleId(configuration.module("PRJ"));
        d.setCfgCompanyId(Champs.de(lot.getPrjProject(), PrjProject::getCfgCompany, CfgCompany::getId));
        d.setPrjProjectId(Champs.de(lot.getPrjProject(), PrjProject::getId));
        d.setPrjMarketId(lot.getId());
        d.setPrjMarketTypeId(Champs.de(lot.getPrjMarketType(), PrjMarketType::getId));
        d.setStlCategoryId(Champs.de(lot.getStlCategory(), StlCategory::getId));
        d.setStlCurrencyId(lot.getStlCurrencyId());
        d.setDefaultVat(lot.getDefaultVat());
        d.setPriceEstimatedHT(lot.getEstimatedPriceHTManual());
        d.setPriceEstimatedTTC(lot.getEstimatedPriceTTCManual());
        d.setStlSettlementTypeId(lot.getStlSettlementTypeId());
        d.setPaymentDelay(lot.getPaymentDelay());
        d.setDescription(saisie.description().trim());
        d.setDescriptionLong(lot.getDescriptionLong());
        d.setDocDate(LocalDate.now(clock).atStartOfDay());
        d.setCfgTierTriggerId(ecritures.employeCourant());
        d.setLaunchOfTenderDate(lot.getLaunchOfTenderDate());
        d.setWithdrawalTenderDocumentsDate(lot.getWithdrawalTenderDocumentsDate());
        d.setTenderMaturityDate(lot.getTenderMaturityDate());
        d.setTendersResortDate(lot.getTendersResortDate());
        d.setBidOpeningDate(lot.getBidOpeningDate());
        d.setTendersReportDate(lot.getTendersReportDate());
        d.setTenderDate(lot.getTenderDate());
        d.setContractSignatureDate(lot.getContractSignatureDate());
        ecritures.persister(d);
        bordereau.ecrire(d.getId(),
                em.createQuery("select x from PrjMarketLineGroupingEstimation x where x.prjMarketId = :lot" + NON_STOCK,
                                PrjMarketLineGroupingEstimation.class).setParameter("lot", lot.getId()).getResultStream()
                        .map(g -> new BordereauDemandeAchat.GroupeSource(g.getId(), g.getParentId(), g.getSorting(), g.getCode(),
                                g.getDescription(), g.getNotes())).toList(),
                em.createQuery("select x from PrjMarketLineEstimation x where x.prjMarketId = :lot" + NON_STOCK,
                                PrjMarketLineEstimation.class).setParameter("lot", lot.getId()).getResultStream()
                        .map(l -> new BordereauDemandeAchat.LigneSource(l.getPrjMarketLineGroupingId(), l.getSorting(), l.getCode(),
                                l.getDescription(), l.getUnity(), l.getQuantity(), l.getUnitPriceHT(), l.getVatRatio(), l.getFodecRatio(),
                                l.getStlCategoryId(), l.getNotes())).toList());
        return d.getId();
    }

    /** R19 ({@code New(BuySelection)}) : copie d'une DIA annulée, dernière de son groupe. */
    UUID renouveler(BuySelection origine) {
        if (!MoteurWorkflowStr.TYPE_ANNULATION.equals(Champs.de(origine.getStrStatus(), StrStatus::getStrStatusTypeId))) {
            throw new ConflictException("Pour pouvoir générer un nouvel appel d'offre il faut d'abords annuler l'actuel !");
        }
        StrEntity entite = Objects.requireNonNullElseGet(origine.getStrEntity(),
                () -> EcrituresDemandeAchat.entiteDe(origine.getProcedure()));
        BuySelection d = nouvelle(origine.getProcedure(), entite, ecritures.depart(entite));
        d.setGroupId(Objects.requireNonNullElse(origine.getGroupId(), origine.getId()));
        d.setSorting(Objects.requireNonNullElse(origine.getSorting(), 0) + 1);
        d.setAdmModuleId(origine.getAdmModuleId());
        d.setCfgCompanyId(origine.getCfgCompanyId());
        d.setPrjProjectId(origine.getPrjProjectId());
        d.setPrjMarketId(origine.getPrjMarketId());
        d.setPrjMarketTypeId(origine.getPrjMarketTypeId());
        d.setStlCategoryId(origine.getStlCategoryId());
        d.setStlCurrencyId(origine.getStlCurrencyId());
        d.setDefaultVat(origine.getDefaultVat());
        d.setPriceEstimatedHT(origine.getPriceEstimatedHT());
        d.setPriceEstimatedTTC(origine.getPriceEstimatedTTC());
        d.setStlSettlementTypeId(origine.getStlSettlementTypeId());
        d.setPaymentDelay(origine.getPaymentDelay());
        d.setDescription(origine.getDescription());
        d.setDescriptionLong(origine.getDescriptionLong());
        d.setDocDate(LocalDate.now(clock).atStartOfDay());
        d.setCfgTierTriggerId(origine.getCfgTierTriggerId());
        em.createQuery("select x from BuySelection x where x.groupId = :g", BuySelection.class).setParameter("g", d.getGroupId())
                .getResultList().forEach(x -> x.setIsTheLast(false));
        origine.setIsTheLast(false);
        ecritures.persister(d);
        bordereau.ecrire(d.getId(),
                bordereau.groupes(origine.getId()).stream().map(g -> new BordereauDemandeAchat.GroupeSource(g.getId(), g.getParentId(),
                        g.getSorting(), g.getCode(), g.getDescription(), g.getNotes())).toList(),
                bordereau.lignes(origine.getId()).stream().map(l -> new BordereauDemandeAchat.LigneSource(l.getBuySelectionGroupId(),
                        l.getSorting(), l.getCode(), l.getDescription(), l.getUnity(), l.getQuantity(), l.getUnitPriceHT(), l.getVatRatio(),
                        l.getFodecRatio(), l.getStlCategoryId(), l.getNotes())).toList());
        copierFilles(origine.getId(), d.getId());
        return d.getId();
    }

    /** Lignes de stock (sans entité), modalités et fournisseurs non écartés ayant déposé, dates et référence vidées. */
    private void copierFilles(UUID origine, UUID copie) {
        String auteur = utilisateur.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        em.createNativeQuery("""
                        INSERT INTO BuySelectionStockLine (PKey, BuyItemId, BuySelectionId, Sorting, Code, Description, Unity, Quantity,
                            UnitPriceHT, FODECRatio, VatRatio, UnitPriceTTC, Notes, UserCreate, UserCreateDateTime)
                        SELECT NEWID(), BuyItemId, :copie, Sorting, Code, Description, Unity, Quantity, UnitPriceHT, FODECRatio, VatRatio,
                            UnitPriceTTC, Notes, :auteur, :maintenant
                        FROM BuySelectionStockLine WHERE BuySelectionId = :origine""")
                .setParameter("copie", copie).setParameter("origine", origine).setParameter("auteur", auteur)
                .setParameter("maintenant", maintenant).executeUpdate();
        em.createQuery("select m from BuySelectionPaymentTerm m where m.buySelectionId = :id", BuySelectionPaymentTerm.class)
                .setParameter("id", origine).getResultList().forEach(m -> {
                    BuySelectionPaymentTerm n = new BuySelectionPaymentTerm();
                    n.setId(UUID.randomUUID());
                    n.setBuySelectionId(copie);
                    n.setSorting(m.getSorting());
                    n.setCode(m.getCode());
                    n.setRatio(m.getRatio());
                    n.setUserCreate(auteur);
                    n.setUserCreateDateTime(maintenant);
                    em.persist(n);
                });
        em.createQuery("""
                        select s from BuySelectionSupplier s where s.buySelectionId = :id and s.tenderDepositDate is not null
                        and (s.isRejected is null or s.isRejected = false)""", BuySelectionSupplier.class)
                .setParameter("id", origine).getResultList().forEach(s -> {
                    BuySelectionSupplier n = new BuySelectionSupplier();
                    n.setId(UUID.randomUUID());
                    n.setBuySelectionId(copie);
                    n.setCfgTierId(s.getCfgTierId());
                    n.setOfficeOrderRef("");
                    n.setIsRejected(false);
                    n.setNotes(s.getNotes());
                    n.setUserCreate(auteur);
                    n.setUserCreateDateTime(maintenant);
                    em.persist(n);
                });
        em.flush();
    }

    private BuySelection nouvelle(BuySelectionKind procedure, StrEntity entite, StrStatus depart) {
        BuySelection d = new BuySelection();
        d.setId(UUID.randomUUID());
        d.setCode(ecritures.codeLibre());
        d.setIsTheLast(true);
        d.setProcedure(procedure);
        d.setStrEntity(entite);
        d.setStrStatus(depart);
        return d;
    }

}
