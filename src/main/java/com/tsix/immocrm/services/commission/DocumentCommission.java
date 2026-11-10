package com.tsix.immocrm.services.commission;

import com.tsix.immocrm.dto.commission.DocumentCommissionDto;
import com.tsix.immocrm.dto.commission.DocumentCommissionDto.LigneDocumentCommissionDto;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComCommissionLine;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComCommissionLineRepository;
import com.tsix.immocrm.services.achat.ecriture.ConfigurationPieceAchat;
import com.tsix.immocrm.services.achat.ecriture.EcriturePieceAchat;
import com.tsix.immocrm.services.achat.ecriture.PieceAchatACreer;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Document à payer d'une personne pour des lignes de commission (R-G, C6) : tout est contrôlé et lu avant la première
 * écriture ; la pièce passe par {@link EcriturePieceAchat}, les lignes de commission dans la même transaction.
 */
@Component
public class DocumentCommission {

    private static final String PROJETS = "SELECT t.id, p.id, soc.id FROM CfgTranche t LEFT JOIN t.prjProject p "
            + "LEFT JOIN t.cfgProject pr LEFT JOIN pr.cfgCompany soc WHERE t.id IN :ids";
    private static final String DOSSIERS = "SELECT f.id, f.code FROM ComFolder f WHERE f.id IN :ids";
    private static final String STATUT = "SELECT Description FROM BuyDocumentStatus WHERE PKey = :id";

    private final CommissionsVisibles visibles;
    private final ComCommissionLineRepository lignes;
    private final SoldesCommission soldes;
    private final EcriturePieceAchat ecriture;
    private final ConfigurationPieceAchat configuration;
    private final EntityManager em;

    public DocumentCommission(CommissionsVisibles visibles, ComCommissionLineRepository lignes, SoldesCommission soldes,
                              EcriturePieceAchat ecriture, ConfigurationPieceAchat configuration, EntityManager em) {
        this.visibles = visibles;
        this.lignes = lignes;
        this.soldes = soldes;
        this.ecriture = ecriture;
        this.configuration = configuration;
        this.em = em;
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
        BuyDocument d = ecriture.creer(piece(choisies, em.find(CfgTier.class, personne), projets,
                societes.get(choisies.get(0).getCfgTrancheId())));
        choisies.forEach(l -> {
            l.setBuyDocumentId(d.getId());
            l.setUserUpdate(d.getUserCreate());
            l.setUserUpdateDateTime(d.getUserCreateDateTime());
        });
        em.flush();
        return dto(d.getId());
    }

    /** Une ligne par commission (« {type} du dossier N° {code} », note = mode), une part par projet de facturation. */
    private PieceAchatACreer piece(List<ComCommissionLine> choisies, CfgTier tiers, Map<UUID, UUID> projets, UUID societe) {
        Map<UUID, String> codes = codesDossiers(choisies);
        Map<UUID, BigDecimal> parProjet = choisies.stream().collect(Collectors.groupingBy(
                l -> projets.get(l.getCfgTrancheId()), LinkedHashMap::new,
                Collectors.reducing(BigDecimal.ZERO, SoldesCommission::montant, BigDecimal::add)));
        return new PieceAchatACreer("COM", "ComInvoiceDebitCommission",
                "Aucun statut de départ dans le workflow des documents de commission.", "INVOICE", "SALE_COMMISSION",
                tiers, societe, configuration.marcheParDefaut(), null, code -> "Commission N° %s".formatted(code), "",
                choisies.stream().map(c -> new PieceAchatACreer.Ligne(null, libelle(c, codes), c.getCommissionAmount(),
                        Objects.toString(c.getCalculationMethod(), ""), null)).toList(),
                List.of(), parProjet.entrySet().stream()
                        .map(e -> new PieceAchatACreer.Part(e.getKey(), e.getValue())).toList());
    }

    private DocumentCommissionDto dto(UUID documentId) {
        BuyDocument d = em.find(BuyDocument.class, documentId);
        // Un document peut couvrir plusieurs tranches : seules les lignes des tranches visibles de l'appelant sortent (C8).
        Map<UUID, Boolean> visibilite = new HashMap<>();
        List<ComCommissionLine> toutes = lignes.findByBuyDocumentIdIn(List.of(documentId));
        List<ComCommissionLine> duDocument = toutes.stream()
                .filter(l -> visibilite.computeIfAbsent(l.getCfgTrancheId(), t -> t != null && visibles.visible(t).isPresent()))
                .toList();
        Map<UUID, String> codes = codesDossiers(duDocument);
        BigDecimal montant;
        BigDecimal regle;
        if (duDocument.size() == toutes.size()) {
            montant = SoldesCommission.nonNul(d.getAmountTTC());
            regle = soldes.reglesParDocument(List.of(documentId)).getOrDefault(documentId, BigDecimal.ZERO);
        } else {
            // Document partiellement visible : part des lignes visibles seulement, réglé au prorata (C8, Q5).
            SoldesCommission.Solde part = SoldesCommission.solde(duDocument, soldes.reglesParLigne(duDocument));
            montant = part.genere();
            regle = part.regle();
        }
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
