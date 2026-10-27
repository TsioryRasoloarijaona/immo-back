package com.tsix.immocrm.services.patrimoine.impressions;

import com.tsix.immocrm.dto.dossier.MontantsDossierDto;
import com.tsix.immocrm.dto.dossier.SyntheseMontantDto;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.services.dossier.MontantsDossier;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.OrdreNaturel;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/** Seule sélection de la Relance 2 par statut : aperçu et lettres (FEATURE-patrimoine-impressions-tranche.md §1.1). */
@Component
public class SelectionRelance2 {

    /** Liste codée en dur du modèle OMRAN {@code CfgTrancheFicheRelance2}, dans son ordre. */
    public static final List<String> STATUTS =
            List.of("CONCRETISATION", "MEILLEURTAUX", "ENVOI AU NOTAIRE", "CREDIT EN COURS", "CREDIT ACCORDE");
    private static final int LOT = 2_000;
    // Tranche de l'article (sous-article compris), jamais de la prospection ; jointures internes : sans statut ou
    // sans client principal = exclu.
    private static final String DOSSIERS = """
            SELECT f FROM ComFolder f JOIN FETCH f.strStatus s JOIN FETCH f.stkItem i JOIN FETCH f.cfgCustomer
            WHERE i.stkHierarchy.id = :tranche AND s.code IN :statuts""";
    private static final Comparator<ComFolder> ORDRE = Comparator
            .comparing((ComFolder f) -> Champs.de(f.getStkItem(), StkItem::getCode),
                    Comparator.nullsLast(OrdreNaturel.CODES))
            .thenComparing(ComFolder::getCode, Comparator.nullsLast(OrdreNaturel.CODES));
    private static final Comparator<CfgTier> PAR_NOM =
            Comparator.comparing(TierNomFormatter::nom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));

    private final TrancheVisibleRepository tranches;
    private final ComFolderCustomerRepository coAcquereurs;
    private final MontantsDossier montants;
    private final EntityManager em;

    public SelectionRelance2(TrancheVisibleRepository tranches, ComFolderCustomerRepository coAcquereurs,
                             MontantsDossier montants, EntityManager em) {
        this.tranches = tranches;
        this.coAcquereurs = coAcquereurs;
        this.montants = montants;
        this.em = em;
    }

    /** Dossier retenu par son statut ; {@code coAcquereurs} triés par nom. */
    public record Retenu(ComFolder dossier, MontantsDossierDto montants, List<CfgTier> coAcquereurs) {

        public SyntheseMontantDto prix() {
            return montants.prixVente();
        }

        /** Reste du prix ≤ 0 : aucune lettre (décision Q3). */
        public boolean ecarte() {
            return prix().reste().signum() <= 0;
        }

        public Stream<Lettre> lettres() {
            if (ecarte()) {
                return Stream.empty();
            }
            return Stream.concat(Stream.of(new Lettre(this, dossier.getCfgCustomer(), false)),
                    coAcquereurs.stream().map(c -> new Lettre(this, c, true)));
        }
    }

    public record Lettre(Retenu retenu, CfgTier acquereur, boolean coAcquereur) {
    }

    /** {@code retenus} dans l'ordre des pages : code d'article (tri naturel), puis dossier. */
    public record Selection(CfgTranche tranche, List<String> statuts, List<Retenu> retenus) {

        public List<Lettre> lettres() {
            return retenus.stream().flatMap(Retenu::lettres).toList();
        }
    }

    /** 404 tranche invisible ; 400 statut hors des {@link #STATUTS}. */
    public Selection selectionner(UUID trancheId, List<String> statut) {
        CfgTranche tranche = tranches.exigerVisible(trancheId);
        List<String> statuts = statuts(statut);
        List<ComFolder> dossiers = em.createQuery(DOSSIERS, ComFolder.class)
                .setParameter("tranche", trancheId).setParameter("statuts", statuts)
                .getResultList().stream().sorted(ORDRE).toList();
        Map<UUID, MontantsDossierDto> parDossier = montants.calculer(dossiers);
        Map<UUID, List<CfgTier>> co = coAcquereurs(dossiers);
        List<Retenu> retenus = dossiers.stream()
                .map(d -> new Retenu(d, parDossier.get(d.getId()),
                        co.getOrDefault(d.getId(), List.of()).stream().sorted(PAR_NOM).toList()))
                .toList();
        return new Selection(tranche, statuts, retenus);
    }

    private static List<String> statuts(List<String> demandes) {
        if (demandes == null || demandes.isEmpty()) {
            return STATUTS;
        }
        demandes.stream().filter(s -> !STATUTS.contains(s)).findFirst().ifPresent(s -> {
            throw new InvalidRequestException("Statut « %s » hors de la Relance 2 (%s).".formatted(s, String.join(", ", STATUTS)));
        });
        return STATUTS.stream().filter(demandes::contains).toList();
    }

    private Map<UUID, List<CfgTier>> coAcquereurs(List<ComFolder> dossiers) {
        List<UUID> ids = dossiers.stream().map(ComFolder::getId).toList();
        Map<UUID, List<CfgTier>> parDossier = new HashMap<>();
        for (int i = 0; i < ids.size(); i += LOT) {
            for (ComFolderCustomer c : coAcquereurs.findByComFolderIdIn(ids.subList(i, Math.min(i + LOT, ids.size())))) {
                parDossier.computeIfAbsent(c.getComFolder().getId(), k -> new ArrayList<>()).add(c.getCfgTier());
            }
        }
        return parDossier;
    }
}
