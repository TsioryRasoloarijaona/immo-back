package com.tsix.immocrm.services.administration.tiers;

import com.tsix.immocrm.domain.FamilleTiers;
import com.tsix.immocrm.dto.administration.tiers.LigneCongeTiersDto;
import com.tsix.immocrm.dto.administration.tiers.LigneDepenseTiersDto;
import com.tsix.immocrm.dto.administration.tiers.LigneDossierTiersDto;
import com.tsix.immocrm.dto.administration.tiers.LigneHistoriqueStatutTiersDto;
import com.tsix.immocrm.dto.administration.tiers.LigneOngletTiers;
import com.tsix.immocrm.dto.administration.tiers.LigneProspectionTiersDto;
import com.tsix.immocrm.dto.administration.tiers.LigneRecetteTiersDto;
import com.tsix.immocrm.dto.administration.tiers.LigneReglementTiersDto;
import com.tsix.immocrm.dto.common.PageDto;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/** Onglets de lecture de la fiche tiers (FEATURE-admin-onglets-tiers.md) : GET seuls, tri par date décroissante. */
@Service
public class OngletsTiersService {

    private static final String TIERS = "tiers";
    private static final String SELECT = "select new com.tsix.immocrm.dto.administration.tiers.";
    private static final String COMPTE = "select count(*) ";
    private static final String DETAIL_ARTICLE = " left join x.stkItem i left join i.stkHierarchy t"
            + " left join i.stkItemCategory c left join i.stkItemType y";

    private static final String DE_PROSPECTIONS = " from ComProspection x where x.cfgProspect.id = :tiers";
    private static final String FILTRE_DEPENSES = " where x.cfgTierId = :tiers and x.docType = 'INVOICE' and x.canBePayed = true";
    private static final String DE_DEPENSES = " from BuyDocument x" + FILTRE_DEPENSES;
    private static final String DE_RECETTES = " from StlDocument x where x.cfgTierId = :tiers";
    private static final String DE_REGLEMENTS = " from StlSettlement x where x.tier.id = :tiers or x.tierPayer.id = :tiers";
    private static final String DE_CONGES = " from GrhVacation x where x.grhEmployeeId = :tiers";
    private static final String DE_HISTORIQUE = " from StrFolderStatusHistory x where x.cfgTier.id = :tiers";

    /** Onglets d'OMRAN ({@code TierControl}) : Congés pour les personnels seuls. */
    private enum Onglet {
        PROSPECTIONS("prospections", LigneProspectionTiersDto.class, SELECT + """
                LigneProspectionTiersDto(x.id, x.code, x.docDate, v.description, t.description, k.description, i.code,
                    a.description, s.description,
                    (select max(f.code) from ComFolder f where f.comProspection = x), x.isClosed, x.notes)
                from ComProspection x left join x.stkVocation v left join x.stkHierarchy t
                    left join x.comProspectionKind k left join x.stkItem i left join x.comAction a
                    left join x.comProspectionOriginSupport s
                where x.cfgProspect.id = :tiers order by x.docDate desc, x.id""", COMPTE + DE_PROSPECTIONS),
        DOSSIERS("dossiers", LigneDossierTiersDto.class, null, null),
        RECETTES("recettes", LigneRecetteTiersDto.class, SELECT + """
                LigneRecetteTiersDto(x.id, x.code, x.description, x.docDate, x.maturityDate, x.docRef, x.amountToPay,
                    x.notes)""" + DE_RECETTES + " order by x.docDate desc, x.id", COMPTE + DE_RECETTES),
        DEPENSES("depenses", LigneDepenseTiersDto.class, SELECT + """
                LigneDepenseTiersDto(x.id, x.code, x.description, x.docDate, x.maturityDate, c.description,
                    s.description, x.amountHT, x.amountVat, x.amountTTC, x.amountToPay, x.notes)
                from BuyDocument x left join CfgCompany c on c.id = x.cfgCompanyId
                    left join BuyDocumentStatus s on s.id = x.buyDocumentStatusId
                """ + FILTRE_DEPENSES + " order by x.docDate desc, x.id", COMPTE + DE_DEPENSES),
        REGLEMENTS("reglements", LigneReglementTiersDto.class, SELECT + """
                LigneReglementTiersDto(x.id, x.docDate, x.code, x.description, x.amount, m.description, x.status,
                    x.isCredit)
                from StlSettlement x left join x.stlSettlementType m
                where x.tier.id = :tiers or x.tierPayer.id = :tiers order by x.docDate desc, x.id""",
                COMPTE + DE_REGLEMENTS),
        CONGES("conges", LigneCongeTiersDto.class, SELECT + """
                LigneCongeTiersDto(x.id, x.code, k.description, x.dateFrom, x.dateTo, x.daysNumber, s.description,
                    x.notes)
                from GrhVacation x left join x.grhVacationType k left join x.strStatus s
                where x.grhEmployeeId = :tiers order by x.dateFrom desc, x.id""", COMPTE + DE_CONGES),
        HISTORIQUE("historique", LigneHistoriqueStatutTiersDto.class, SELECT + """
                LigneHistoriqueStatutTiersDto(x.id, x.docDate, s.description, i.code, x.description, f.code,
                    u.description)
                from StrFolderStatusHistory x left join x.strFolderStatus s left join x.stkItem i
                    left join x.comFolder f left join AdmUser u on u.id = x.admUserId
                where x.cfgTier.id = :tiers order by x.docDate desc, x.id""", COMPTE + DE_HISTORIQUE);

        private final String segment;
        private final Class<? extends LigneOngletTiers> type;
        private final String lignes;
        private final String compte;

        Onglet(String segment, Class<? extends LigneOngletTiers> type, String lignes, String compte) {
            this.segment = segment;
            this.type = type;
            this.lignes = lignes;
            this.compte = compte;
        }
    }

    // Vente (client ou co-acquéreur) et location : peu de lignes par tiers, fusionnées puis paginées en mémoire.
    private static final String DOSSIERS_VENTE = SELECT + """
            LigneDossierTiersDto(x.id, case when x.cfgCustomer.id = :tiers then 'Vente' else 'Co-acquéreur' end,
                x.code, x.description, x.docDate, st.description, t.description, i.code, c.description, y.description,
                x.totalToPay)
            from ComFolder x left join x.strStatus st""" + DETAIL_ARTICLE + " where x.cfgCustomer.id = :tiers"
            + " or exists (select 1 from ComFolderCustomer fc where fc.comFolder = x and fc.cfgTier.id = :tiers)";
    private static final String DOSSIERS_LOCATION = SELECT + """
            LigneDossierTiersDto(x.id, 'Location', x.code, x.description, x.docDate, st.description, t.description,
                i.code, c.description, y.description, cast(null as BigDecimal))
            from RntFolder x left join x.strStatus st""" + DETAIL_ARTICLE + " where x.cfgCustomerId = :tiers";

    private final EntityManager em;
    private final TiersService tiers;

    public OngletsTiersService(EntityManager em, TiersService tiers) {
        this.em = em;
        this.tiers = tiers;
    }

    @Transactional(readOnly = true)
    public PageDto<LigneOngletTiers> lister(String famille, UUID id, String onglet, int page, int size) {
        FamilleTiers f = tiers.verifier(famille, id);
        Onglet o = Arrays.stream(Onglet.values()).filter(x -> x.segment.equals(onglet)).findFirst()
                .filter(x -> prevu(f, x))
                .orElseThrow(() -> new ResourceNotFoundException("Onglet inconnu pour cette famille : " + onglet));
        return o == Onglet.DOSSIERS ? dossiers(id, page, size) : page(o, id, page, size);
    }

    private static boolean prevu(FamilleTiers famille, Onglet onglet) {
        return switch (famille) {
            case FOURNISSEURS, CONTACTS -> onglet != Onglet.CONGES;
            case PERSONNELS -> true;
            case BANQUES, ORGANISMES -> false;
        };
    }

    private PageDto<LigneOngletTiers> page(Onglet o, UUID id, int page, int size) {
        List<LigneOngletTiers> lignes = List.copyOf(em.createQuery(o.lignes, o.type).setParameter(TIERS, id)
                .setFirstResult(page * size).setMaxResults(size).getResultList());
        long total = em.createQuery(o.compte, Long.class).setParameter(TIERS, id).getSingleResult();
        return enveloppe(lignes, page, size, total);
    }

    private PageDto<LigneOngletTiers> dossiers(UUID id, int page, int size) {
        List<LigneDossierTiersDto> tous = Stream.of(DOSSIERS_VENTE, DOSSIERS_LOCATION)
                .flatMap(q -> em.createQuery(q, LigneDossierTiersDto.class).setParameter(TIERS, id).getResultStream())
                .sorted(Comparator.comparing(LigneDossierTiersDto::date, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(LigneDossierTiersDto::id))
                .toList();
        List<LigneOngletTiers> lignes = tous.stream().skip((long) page * size).limit(size)
                .map(LigneOngletTiers.class::cast).toList();
        return enveloppe(lignes, page, size, tous.size());
    }

    private static PageDto<LigneOngletTiers> enveloppe(List<LigneOngletTiers> lignes, int page, int size, long total) {
        return new PageDto<>(lignes, page, size, total, (int) ((total + size - 1) / size));
    }
}
