package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.dto.commission.DocumentCommissionDto;
import com.tsix.immocrm.dto.commission.DocumentCommissionDto.LigneDocumentCommissionDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuyDocumentLine;
import com.tsix.immocrm.entities.buy.BuyDocumentStatusHistory;
import com.tsix.immocrm.entities.buy.BuyVentilationExpense;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComCommissionLine;
import com.tsix.immocrm.exceptions.ConfigurationManquanteException;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComCommissionLineRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Document à payer d'une personne pour des lignes de commission (R-G, C6) : tout est contrôlé et lu avant la première
 * écriture ; document, lignes, ventilation par projet, historique, séquence et lignes de commission dans une transaction.
 */
@Component
public class DocumentCommission {

    private static final String SEQUENCE = "Facture fournisseur";
    private static final String CONFIGURATION = """
            SELECT (SELECT TOP 1 PKey FROM AdmModule WHERE Code = N'COM' ORDER BY PKey ASC) AS Module,
                   (SELECT TOP 1 PKey FROM StrEntity WHERE Code = N'ComInvoiceDebitCommission' ORDER BY PKey ASC) AS Entite,
                   (SELECT TOP 1 CodeValue FROM AdmParamsGuid WHERE Code = N'DefaultCurrency') AS Devise,
                   (SELECT TOP 1 CodeValue FROM AdmParamsGuid WHERE Code = N'DefaultMarketType') AS Marche""";
    // Premier état du workflow de l'entité : début d'une transition qui n'est la fin d'aucune autre.
    private static final String STATUT_DE_DEPART = """
            SELECT TOP 1 w.StartStatusId FROM BuyDocumentWorkFlow w WHERE w.StrEntityId = :entite AND w.StartStatusId IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM BuyDocumentWorkFlow x WHERE x.StrEntityId = :entite AND x.EndStatusId = w.StartStatusId)
            ORDER BY w.Sorting ASC, w.PKey ASC""";
    private static final String PROJETS = "SELECT t.id, p.id, soc.id FROM CfgTranche t LEFT JOIN t.prjProject p "
            + "LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc WHERE t.id IN :ids";
    private static final String DOSSIERS = "SELECT f.id, f.code FROM ComFolder f WHERE f.id IN :ids";
    private static final String STATUT = "SELECT Description FROM BuyDocumentStatus WHERE PKey = :id";

    private record Configuration(UUID module, UUID entite, UUID devise, UUID marche, UUID statut) {
    }

    private final CommissionsVisibles visibles;
    private final ComCommissionLineRepository lignes;
    private final SoldesCommission soldes;
    private final GenerateurCodeSequence sequences;
    private final UtilisateurAffiche utilisateur;
    private final EntityManager em;
    private final Clock clock;

    public DocumentCommission(CommissionsVisibles visibles, ComCommissionLineRepository lignes, SoldesCommission soldes,
                              GenerateurCodeSequence sequences, UtilisateurAffiche utilisateur, EntityManager em,
                              Clock clock) {
        this.visibles = visibles;
        this.lignes = lignes;
        this.soldes = soldes;
        this.sequences = sequences;
        this.utilisateur = utilisateur;
        this.em = em;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DocumentCommissionDto> deLaTranche(UUID trancheId) {
        visibles.tranche(trancheId);
        return lignes.documentsDeLaTranche(trancheId).stream().map(this::dto)
                .sorted(Comparator.comparing(d -> Objects.toString(d.code(), ""))).toList();
    }

    @Transactional(readOnly = true)
    public DocumentCommissionDto deLaLigne(UUID ligneId) {
        UUID document = visibles.ligne(ligneId).getBuyDocumentId();
        if (document == null) {
            throw new ResourceNotFoundException("Cette ligne n'a pas encore un document à payer!");
        }
        return dto(document);
    }

    @Transactional
    public DocumentCommissionDto generer(List<UUID> ligneIds) {
        if (ligneIds.isEmpty()) {
            throw new InvalidRequestException("Aucune ligne n'est sélectionnée !");
        }
        List<ComCommissionLine> choisies = visibles.lignes(ligneIds);
        if (choisies.stream().map(ComCommissionLine::getCfgEmployeeId).distinct().count() > 1) {
            throw new ConflictException("Vous devez générer un document par commercial!");
        }
        if (choisies.stream().anyMatch(l -> l.getBuyDocumentId() != null)) {
            throw new ConflictException("Vous avez des lignes affectées à un document à payé !");
        }
        Map<UUID, UUID> projets = new HashMap<>();
        Map<UUID, UUID> societes = new HashMap<>();
        em.createQuery(PROJETS, Object[].class).setParameter("ids",
                choisies.stream().map(ComCommissionLine::getCfgTrancheId).distinct().toList()).getResultList()
                .forEach(r -> {
                    projets.put((UUID) r[0], (UUID) r[1]);
                    societes.put((UUID) r[0], (UUID) r[2]);
                });
        if (choisies.stream().anyMatch(l -> projets.get(l.getCfgTrancheId()) == null)) {
            throw new InvalidRequestException("Il faut obligatoirement renseigner un projet de facturation pour les dépenses "
                    + "des tranches concernées !");
        }
        BigDecimal total = choisies.stream().map(SoldesCommission::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() == 0) {
            throw new InvalidRequestException("Vous ne pouvez pas générer un document, La somme des commissions "
                    + "sélectionnées égale 0 !");
        }
        UUID personne = choisies.get(0).getCfgEmployeeId();
        if (personne == null) {
            throw new InvalidRequestException("Ligne de commission sans commercial.");
        }
        CfgTier tiers = em.find(CfgTier.class, personne);
        Configuration conf = configuration();
        BuyDocument d = document(tiers, societes.get(choisies.get(0).getCfgTrancheId()), total, conf,
                sequences.suivantSelonLigne(SEQUENCE));
        ecrireLignes(d, choisies);
        ventiler(d, choisies, projets, total);
        historique(d);
        choisies.forEach(l -> {
            l.setBuyDocumentId(d.getId());
            l.setUserUpdate(d.getUserCreate());
            l.setUserUpdateDateTime(d.getUserCreateDateTime());
        });
        em.flush();
        return dto(d.getId());
    }

    private Configuration configuration() {
        Object[] c = (Object[]) em.createNativeQuery(CONFIGURATION).getSingleResult();
        UUID module = guid(c[0], "Module COM absent d'AdmModule.");
        UUID entite = guid(c[1], "StrEntity ComInvoiceDebitCommission absente.");
        UUID devise = guid(c[2], "AdmParamsGuid DefaultCurrency absent.");
        List<?> depart = em.createNativeQuery(STATUT_DE_DEPART).setParameter("entite", entite).getResultList();
        if (depart.isEmpty()) {
            throw new ConfigurationManquanteException("Aucun statut de départ dans le workflow des documents de commission.");
        }
        UUID marche = c[3] == null ? null : UUID.fromString(c[3].toString());
        return new Configuration(module, entite, devise, marche, UUID.fromString(depart.get(0).toString()));
    }

    private static UUID guid(Object valeur, String absent) {
        if (valeur == null) {
            throw new ConfigurationManquanteException(absent);
        }
        return UUID.fromString(valeur.toString());
    }

    private BuyDocument document(CfgTier tiers, UUID societe, BigDecimal total, Configuration conf, String code) {
        LocalDateTime maintenant = LocalDateTime.now(clock);
        LocalDateTime jour = LocalDate.now(clock).atStartOfDay();
        BuyDocument d = new BuyDocument();
        d.setId(UUID.randomUUID());
        d.setCode(code);
        d.setDescription("Commission N° %s".formatted(code));
        d.setDocDate(jour);
        d.setPrevisionDeliveryDate(jour);
        Integer delai = tiers.getPaymentDelay();
        d.setMaturityDate(delai != null && delai > 0 ? jour.plusDays(delai) : jour);
        d.setDocType("INVOICE");
        d.setReferenceType("SALE_COMMISSION");
        d.setCfgTierId(tiers.getId());
        d.setSupplierDescription(tiers.getDescription());
        d.setCfgCompanyId(societe);
        d.setPrjMarketTypeId(conf.marche());
        d.setAdmModuleId(conf.module());
        d.setStlCurrencyId(conf.devise());
        d.setStrEntityId(conf.entite());
        d.setBuyDocumentStatusId(conf.statut());
        d.setAmountHT(total);
        d.setAmountTTC(total);
        d.setAmountToPay(total);
        BigDecimal z = BigDecimal.ZERO;
        List.<Consumer<BigDecimal>>of(d::setAmountVat, d::setFiscalStamp, d::setAmountRegul,
                d::setStockAmountToPay, d::setStockAmountToPayHT, d::setWorkingOutAdvanceAmount, d::setRetainedAmount,
                d::setProrataRefundAmount, d::setFluctuationsToPay, d::setFluctuationsToPayHT, d::setAdvanceRatio,
                d::setAdvanceAmount, d::setAdvanceRest, d::setWarrantyRatio, d::setWarrantyAmount, d::setProportionRatio,
                d::setProportionAmount, d::setInsuranceDECRatio, d::setInsuranceDECAmount, d::setInsuranceDECRest,
                d::setInsuranceTRCRatio, d::setInsuranceTRCAmount, d::setInsuranceTRCRest).forEach(s -> s.accept(z));
        d.setIsSelected(false);
        d.setIsClosed(false);
        d.setCanBePayed(true);
        d.setIsPropotionCharged(false);
        d.setIsAccounted(false);
        d.setDocRef("");
        d.setPersonToContact("");
        d.setNotes("");
        d.setNotes2("");
        d.setOfficeOrderRef("");
        String auteur = utilisateur.courant().nom();
        d.setUserCreate(auteur);
        d.setUserCreateDateTime(maintenant);
        d.setUserUpdate(auteur);
        d.setUserUpdateDateTime(maintenant);
        em.persist(d);
        return d;
    }

    /** Une ligne de document par ligne de commission : « {type} du dossier N° {code} », U × 1, TVA 0, note = mode. */
    private void ecrireLignes(BuyDocument d, List<ComCommissionLine> choisies) {
        Map<UUID, String> codes = codesDossiers(choisies);
        d.setLinesCount(choisies.size());
        int rang = 1;
        for (ComCommissionLine c : choisies) {
            BuyDocumentLine l = new BuyDocumentLine();
            l.setId(UUID.randomUUID());
            l.setBuyDocumentId(d.getId());
            l.setDescription(libelle(c, codes));
            l.setUnity("U");
            l.setQuantity(BigDecimal.ONE);
            l.setSorting(rang++);
            l.setUnitPriceHT(c.getCommissionAmount());
            l.setUnitPriceTTC(c.getCommissionAmount());
            l.setVatRatio(BigDecimal.ZERO);
            l.setDiscountRatio(BigDecimal.ZERO);
            l.setFodecRatio(BigDecimal.ZERO);
            l.setCoefficient(BigDecimal.ONE);
            l.setQuantityOld(BigDecimal.ZERO);
            l.setQuantityOrigin(BigDecimal.ZERO);
            l.setNotes(Objects.toString(c.getCalculationMethod(), ""));
            l.setUserCreate(d.getUserCreate());
            l.setUserCreateDateTime(d.getUserCreateDateTime());
            l.setUserUpdate(d.getUserCreate());
            l.setUserUpdateDateTime(d.getUserCreateDateTime());
            em.persist(l);
        }
    }

    /** Une ventilation par projet de facturation, taux = part du projet ; le dernier taux complète Σ = 1. */
    private void ventiler(BuyDocument d, List<ComCommissionLine> choisies, Map<UUID, UUID> projets, BigDecimal total) {
        Map<UUID, BigDecimal> parProjet = choisies.stream().collect(Collectors.groupingBy(
                l -> projets.get(l.getCfgTrancheId()), LinkedHashMap::new,
                Collectors.reducing(BigDecimal.ZERO, SoldesCommission::montant, BigDecimal::add)));
        BigDecimal reste = BigDecimal.ONE;
        int n = 0;
        for (Map.Entry<UUID, BigDecimal> e : parProjet.entrySet()) {
            BigDecimal taux = ++n == parProjet.size() ? reste : e.getValue().divide(total, 6, RoundingMode.HALF_UP);
            reste = reste.subtract(taux);
            BuyVentilationExpense v = new BuyVentilationExpense();
            v.setId(UUID.randomUUID());
            v.setBuyDocumentId(d.getId());
            v.setPrjProjectId(e.getKey());
            v.setVentilationRate(taux);
            em.persist(v);
        }
    }

    private void historique(BuyDocument d) {
        BuyDocumentStatusHistory h = new BuyDocumentStatusHistory();
        h.setId(UUID.randomUUID());
        h.setBuyDocumentId(d.getId());
        h.setBuyDocumentStatusId(d.getBuyDocumentStatusId());
        h.setDocDate(d.getUserCreateDateTime());
        h.setAdmUserId(Champs.de(utilisateur.courant().compte(), AdmUser::getId));
        h.setNotes("");
        em.persist(h);
    }

    private DocumentCommissionDto dto(UUID documentId) {
        BuyDocument d = em.find(BuyDocument.class, documentId);
        // Un document peut couvrir plusieurs tranches : seules les lignes des tranches visibles de l'appelant sortent (C8).
        Map<UUID, Boolean> visibilite = new HashMap<>();
        List<ComCommissionLine> duDocument = lignes.findByBuyDocumentIdIn(List.of(documentId)).stream()
                .filter(l -> visibilite.computeIfAbsent(l.getCfgTrancheId(), t -> t != null && visibles.visible(t).isPresent()))
                .toList();
        Map<UUID, String> codes = codesDossiers(duDocument);
        BigDecimal montant = SoldesCommission.nonNul(d.getAmountTTC());
        BigDecimal regle = soldes.reglesParDocument(List.of(documentId)).getOrDefault(documentId, BigDecimal.ZERO);
        CfgTier tiers = d.getCfgTierId() == null ? null : em.find(CfgTier.class, d.getCfgTierId());
        List<?> statuts = d.getBuyDocumentStatusId() == null ? List.of()
                : em.createNativeQuery(STATUT).setParameter("id", d.getBuyDocumentStatusId()).getResultList();
        String statut = statuts.isEmpty() ? null : Objects.toString(statuts.get(0), null);
        return new DocumentCommissionDto(d.getId(), d.getCode(), CalculCommission.jour(d.getDocDate()), d.getCfgTierId(),
                tiers == null ? null : TierNomFormatter.nom(tiers), montant, regle, montant.subtract(regle), statut,
                duDocument.stream().map(l -> new LigneDocumentCommissionDto(l.getId(), libelle(l, codes),
                        SoldesCommission.montant(l))).toList());
    }

    private Map<UUID, String> codesDossiers(List<ComCommissionLine> duDocument) {
        Map<UUID, String> codes = new HashMap<>();
        List<UUID> ids = duDocument.stream().map(ComCommissionLine::getComFolderId).filter(Objects::nonNull).distinct()
                .toList();
        if (!ids.isEmpty()) {
            em.createQuery(DOSSIERS, Object[].class).setParameter("ids", ids).getResultList()
                    .forEach(r -> codes.put((UUID) r[0], (String) r[1]));
        }
        return codes;
    }

    private static String libelle(ComCommissionLine l, Map<UUID, String> codes) {
        return "%s du dossier N° %s".formatted(l.getCommissionType(), codes.get(l.getComFolderId()));
    }
}
