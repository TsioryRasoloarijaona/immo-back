package com.tsix.immocrm.services.chantier;

import com.tsix.immocrm.domain.chantier.CumulsDecomptes;
import com.tsix.immocrm.domain.chantier.MontantsLot;
import com.tsix.immocrm.domain.chantier.SoldeFacture;
import com.tsix.immocrm.domain.chantier.Totaux;
import com.tsix.immocrm.dto.chantier.DecompteListItemDto;
import com.tsix.immocrm.dto.chantier.LotDetailDto;
import com.tsix.immocrm.dto.chantier.LotListItemDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentStatus;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketType;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static com.tsix.immocrm.domain.chantier.Montants.enPourcentage;

/** Entités + chiffres → DTO du module Chantier ; aucun calcul d'argent ici. */
final class ChantierMapper {

    private ChantierMapper() {
    }

    static LocalDate jour(LocalDateTime dateHeure) {
        return dateHeure == null ? null : dateHeure.toLocalDate();
    }

    /** Plusieurs colonnes texte du légataire sont des chaînes vides sur 100 % des lignes : exposées null. */
    static String videEnNull(String texte) {
        return texte == null || texte.isBlank() ? null : texte;
    }

    static boolean vrai(Boolean drapeau) {
        return Boolean.TRUE.equals(drapeau);
    }

    static <T, R> R lire(T objet, Function<T, R> champ) {
        return Optional.ofNullable(objet).map(champ).orElse(null);
    }

    static LotListItemDto lotListItem(PrjMarket lot, LotChiffre chiffre) {
        MontantsLot m = chiffre.montants();
        CumulsDecomptes c = chiffre.cumuls();
        SoldeFacture s = chiffre.solde();
        PrjMarketType type = lot.getPrjMarketType();
        StrStatus statut = lot.getStrStatus();
        CfgTier fournisseur = lot.getCfgSupplier();
        StlCategory categorie = lot.getStlCategory();
        return new LotListItemDto(lot.getId(), lot.getCode(), lot.getDescription(),
                lire(type, PrjMarketType::getCode), lire(type, PrjMarketType::getDescription),
                lire(statut, StrStatus::getCode), lire(statut, StrStatus::getDescription),
                lire(statut, StrStatus::getSorting),
                lire(fournisseur, CfgTier::getId), lire(fournisseur, CfgTier::getDescription),
                lire(categorie, StlCategory::getCode), lire(categorie, StlCategory::getDescription),
                jour(lot.getStartDateManual()), jour(lot.getEndDateManual()), lot.getDelayManual(),
                vrai(lot.getIsMultiSupplier()),
                m.montantHT(), m.montantTVA(), m.montantTTC(), m.montantsDepuisBordereau(),
                enPourcentage(lot.getWarrantyRatio()),
                c.nombre(), c.ht(), c.ttc(), c.travauxSupplementaires(), c.retenueGarantie(),
                m.avancementPct(), s.totalFacture(), s.totalRegle(), s.resteARegler());
    }

    static LotDetailDto lotDetail(PrjMarket lot, LotChiffre chiffre) {
        LotListItemDto l = lotListItem(lot, chiffre);
        MontantsLot m = chiffre.montants();
        Totaux forfait = chiffre.forfait();
        PrjProject projet = lot.getPrjProject();
        StrEntity entite = lot.getStrEntity();
        return new LotDetailDto(l.id(), l.code(), l.description(), l.typeCode(), l.typeDescription(),
                l.statutCode(), l.statutDescription(), l.statutOrdre(), l.fournisseurId(), l.fournisseurNom(),
                l.categorieCode(), l.categorieDescription(), l.dateDebut(), l.dateFin(), l.delaiJours(),
                l.multiFournisseur(), l.montantHT(), l.montantTVA(), l.montantTTC(), l.montantsDepuisBordereau(),
                l.tauxRetenueGarantie(), l.nbDecomptes(), l.cumulDecomptesHT(), l.cumulDecomptesTTC(),
                l.cumulTravauxSupplementaires(), l.cumulRetenueGarantie(), l.avancementPct(), l.totalFacture(),
                l.totalRegle(), l.resteARegler(),
                lire(projet, PrjProject::getId), lire(projet, PrjProject::getCode),
                lire(projet, PrjProject::getDescription),
                lire(lire(projet, PrjProject::getCfgCompany), CfgCompany::getCode),
                lire(entite, StrEntity::getCode), lire(entite, StrEntity::getDescription),
                videEnNull(lot.getDescriptionLong()), videEnNull(lot.getNotes()),
                m.estimationHT(), m.estimationTVA(), m.estimationTTC(), m.estimationDepuisBordereau(),
                forfait.ht(), forfait.tva(), forfait.ttc(),
                m.retenueGarantieTheorique(), m.avanceTheorique(),
                enPourcentage(lot.getAdvanceRatio()), enPourcentage(lot.getProportionRatio()),
                enPourcentage(lot.getInsuranceDECRatio()), enPourcentage(lot.getInsuranceTRCRatio()),
                enPourcentage(lot.getDefaultVat()), lot.getPaymentDelay(), lot.getWarrantyInMonths(),
                vrai(lot.getBlokWarrantyRestitution()), vrai(lot.getWithMarketView()), vrai(lot.getIsClosed()),
                chiffre.cumuls().netAPayer(),
                jour(lot.getStartDate()), lot.getDelay(), jour(lot.getEndDate()),
                jour(lot.getLaunchOfTenderDate()), jour(lot.getWithdrawalTenderDocumentsDate()),
                jour(lot.getTenderMaturityDate()), jour(lot.getTendersReportDate()), jour(lot.getBidOpeningDate()),
                jour(lot.getTendersResortDate()), jour(lot.getTenderDate()), jour(lot.getContractSignatureDate()),
                jour(lot.getServiceOrderDate()), jour(lot.getWorkLaunchDate()), jour(lot.getWorkCompletionDate()),
                jour(lot.getReattachmentDate()), jour(lot.getCancellationDate()),
                videEnNull(lot.getCancellationCause()),
                lot.getUserCreateDateTime(), lot.getUserCreate(), lot.getUserUpdateDateTime(), lot.getUserUpdate());
    }

    static DecompteListItemDto decompteListItem(PrjWorkingOut d, SoldeFacture solde) {
        PrjMarket lot = d.getPrjMarket();
        PrjProject projet = lire(lot, PrjMarket::getPrjProject);
        CfgTier fournisseur = d.getCfgTier();
        BuyDocument facture = d.getBuyDocument();
        BuyDocumentStatus statut = lire(facture, BuyDocument::getBuyDocumentStatus);
        return new DecompteListItemDto(d.getId(), d.getCode(), d.getSorting(), jour(d.getDocDate()),
                lire(lot, PrjMarket::getId), lire(lot, PrjMarket::getCode), lire(lot, PrjMarket::getDescription),
                lire(projet, PrjProject::getId), lire(projet, PrjProject::getCode),
                lire(fournisseur, CfgTier::getId), lire(fournisseur, CfgTier::getDescription),
                d.getAmountHT(), d.getAmountTTC(), d.getExtraAmount(), d.getWarrantyAmount(), d.getAmountToPay(),
                d.getAmountTTCAfterDeductions(), vrai(d.getFinalWorkingOut()),
                lire(facture, BuyDocument::getId), lire(facture, BuyDocument::getCode),
                lire(statut, BuyDocumentStatus::getCode), lire(statut, BuyDocumentStatus::getDescription),
                solde.totalRegle(), solde.resteARegler());
    }

    static UUID projetId(PrjMarket lot) {
        return lire(lot.getPrjProject(), PrjProject::getId);
    }
}
