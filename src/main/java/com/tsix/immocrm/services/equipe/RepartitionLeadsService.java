package com.tsix.immocrm.services.equipe;

import com.tsix.immocrm.domain.MotifAttribution;
import com.tsix.immocrm.domain.RaisonNonAttribution;
import com.tsix.immocrm.domain.RolesExemptesPerimetre;
import com.tsix.immocrm.domain.TypeActionRecente;
import com.tsix.immocrm.dto.equipe.RepartitionCommercialDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.adm.AdmUserHabilitationRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRoleRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.notification.LeadsAttribues;
import com.tsix.immocrm.services.parcours.ChargeCommerciale;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospect.ProspectService;
import com.tsix.immocrm.services.prospection.HistoriqueProspection;
import com.tsix.immocrm.services.prospection.ProspectionService;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Seule fonction de répartition automatique des leads, par projet : création, import et « Répartir » de Mon équipe
 * passent par {@link #decider} (FEATURE-repartition-notifications.md §3.1).
 */
@Service
@Transactional
public class RepartitionLeadsService {

    private static final int CONTACTS_MAX = 20;
    private static final String SUITE_REPARTITION = " — répartition automatique";

    private final EquipeCommercialeService equipeService;
    private final ChargeCommerciale chargeCommerciale;
    private final ComProspectionRepository prospectionRepository;
    private final CfgTierRepository tierRepository;
    private final AdmUserRepository userRepository;
    private final AdmUserRoleRepository userRoleRepository;
    private final AdmUserHabilitationRepository habilitationRepository;
    private final HistoriqueProspection historique;
    private final ApplicationEventPublisher evenements;

    public RepartitionLeadsService(EquipeCommercialeService equipeService, ChargeCommerciale chargeCommerciale,
                                   ComProspectionRepository prospectionRepository, CfgTierRepository tierRepository,
                                   AdmUserRepository userRepository, AdmUserRoleRepository userRoleRepository,
                                   AdmUserHabilitationRepository habilitationRepository,
                                   HistoriqueProspection historique, ApplicationEventPublisher evenements) {
        this.equipeService = equipeService;
        this.chargeCommerciale = chargeCommerciale;
        this.prospectionRepository = prospectionRepository;
        this.tierRepository = tierRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.habilitationRepository = habilitationRepository;
        this.historique = historique;
        this.evenements = evenements;
    }

    /** Choix pour une ligne : {@code commercialId} nul ⇔ {@code raison} renseignée. */
    public record Decision(@Nullable UUID commercialId, @Nullable String commercialNom,
                           @Nullable MotifAttribution motif, @Nullable RaisonNonAttribution raison) {

        static Decision attribuee(Eligible e, MotifAttribution motif) {
            return new Decision(e.employeId(), e.nom(), motif, null);
        }

        static Decision non(RaisonNonAttribution raison) {
            return new Decision(null, null, null, raison);
        }

        public boolean estAttribuee() {
            return commercialId != null;
        }
    }

    /** Un employé éligible ; plusieurs comptes peuvent le porter. */
    record Eligible(UUID employeId, String nom) {
    }

    /**
     * État d'un lot : membres lus une fois, charge et date de dernière attribution mises à jour au fil des
     * attributions, pour que deux leads du même lot n'aillent pas au même commercial « le moins chargé ».
     */
    public final class Lot {
        private final List<MembreEquipe> membres = equipeService.membresActifs();
        private final Map<UUID, Long> charges = new HashMap<>();
        private final Map<UUID, LocalDateTime> dernieres = new HashMap<>();
        private final Map<UUID, String> noms = new HashMap<>();
        // Contacts attribués dans ce lot : en simulation rien n'est écrit, la base ne les voit pas (aperçu = écriture).
        private final Map<String, UUID> contacts = new HashMap<>();

        private Lot() {
        }

        void retenirContact(Contact contact, UUID employeId) {
            contact.cles().forEach(cle -> contacts.putIfAbsent(cle, employeId));
        }

        Stream<UUID> commerciauxDuContact(Contact contact) {
            return contact.cles().stream().map(contacts::get).filter(Objects::nonNull);
        }

        List<Eligible> eligibles(UUID projetId) {
            List<UUID> employes = membres.stream()
                    .filter(m -> m.employeId() != null && m.projetIds().contains(projetId))
                    .map(MembreEquipe::employeId)
                    .distinct()
                    .toList();
            chargerDernieres(employes);
            return employes.stream().map(e -> new Eligible(e, nom(e))).toList();
        }

        private void chargerDernieres(List<UUID> employes) {
            List<UUID> inconnus = employes.stream().filter(e -> !charges.containsKey(e)).toList();
            if (inconnus.isEmpty()) {
                return;
            }
            for (Object[] ligne : prospectionRepository.dernierePreProspectionParCommercial(inconnus)) {
                dernieres.put((UUID) ligne[0], (LocalDateTime) ligne[1]);
            }
            inconnus.forEach(e -> charges.put(e, chargeCommerciale.leadsATrier(e)));
        }

        private String nom(UUID employeId) {
            return noms.computeIfAbsent(employeId,
                    id -> tierRepository.findById(id).map(TierNomFormatter::nom).orElse(id.toString()));
        }

        long charge(UUID employeId) {
            return charges.getOrDefault(employeId, 0L);
        }

        void compter(UUID employeId, @Nullable LocalDateTime docDate) {
            charges.merge(employeId, 1L, Long::sum);
            LocalDateTime attribution = docDate != null ? docDate : LocalDateTime.MIN;
            dernieres.merge(employeId, attribution, (a, b) -> a.isAfter(b) ? a : b);
        }

        /** Moins de leads à trier, puis dernière attribution la plus ancienne (jamais = d'abord), puis nom. */
        Comparator<Eligible> ordre() {
            return Comparator.<Eligible>comparingLong(e -> charge(e.employeId()))
                    .thenComparing(e -> dernieres.get(e.employeId()), Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(Eligible::nom, Comparator.nullsLast(Comparator.naturalOrder()));
        }
    }

    public Lot nouveauLot() {
        return new Lot();
    }

    /** Le commercial que la répartition choisirait pour ce lead, seul (spec §3.1). */
    public Optional<CfgTier> destinataire(ComProspection lead) {
        Decision decision = decider(lead, nouveauLot());
        return decision.estAttribuee() ? tierRepository.findById(decision.commercialId()) : Optional.empty();
    }

    /** Décide sans rien écrire ; compte l'attribution dans le lot. Une ligne déjà attribuée n'est jamais reprise. */
    public Decision decider(ComProspection lead, Lot lot) {
        if (lead.getCfgCommercial() != null) {
            return Decision.non(RaisonNonAttribution.DEJA_ATTRIBUEE);
        }
        UUID projetId = projetDe(lead.getStkHierarchy());
        if (projetId == null) {
            return Decision.non(RaisonNonAttribution.SANS_PROJET);
        }
        List<Eligible> eligibles = lot.eligibles(projetId);
        if (eligibles.isEmpty()) {
            return Decision.non(RaisonNonAttribution.AUCUN_ELIGIBLE);
        }
        Contact contact = Contact.de(lead);
        Decision decision = connu(lead, contact, eligibles, lot)
                .map(e -> Decision.attribuee(e, MotifAttribution.CONNU))
                .orElseGet(() -> Decision.attribuee(eligibles.stream().min(lot.ordre()).orElseThrow(),
                        MotifAttribution.MOINS_CHARGE));
        lot.compter(decision.commercialId(), lead.getDocDate());
        lot.retenirContact(contact, decision.commercialId());
        return decision;
    }

    /** Critères de « lead déjà connu », normalisés ; {@code ""} = absent. */
    record Contact(String telephone, String email) {

        static Contact de(ComProspection lead) {
            return new Contact(
                    ProspectService.critereTelephone(ProspectionService.telephoneProspect(lead.getCfgProspect(), lead)),
                    ProspectService.critereEmail(ProspectionService.emailProspect(lead.getCfgProspect(), lead)));
        }

        boolean estVide() {
            return telephone.isEmpty() && email.isEmpty();
        }

        List<String> cles() {
            List<String> cles = new ArrayList<>();
            if (!telephone.isEmpty()) {
                cles.add("T:" + telephone);
            }
            if (!email.isEmpty()) {
                cles.add("E:" + email);
            }
            return cles;
        }
    }

    /**
     * Commercial d'un lead du même lot, puis d'une autre prospection ouverte, puis d'un prospect, au même numéro ou
     * e-mail — s'il est éligible. Le lot d'abord : l'aperçu (rien d'écrit) et l'écriture décident pareil.
     */
    private Optional<Eligible> connu(ComProspection lead, Contact contact, List<Eligible> eligibles, Lot lot) {
        if (contact.estVide()) {
            return Optional.empty();
        }
        Map<UUID, Eligible> parEmploye = eligibles.stream()
                .collect(Collectors.toMap(Eligible::employeId, e -> e, (a, b) -> a, LinkedHashMap::new));
        Stream<UUID> duLot = lot.commerciauxDuContact(contact);
        Stream<UUID> desProspections = Stream.of(contact).flatMap(c -> prospectionRepository
                .commerciauxDuMemeContact(lead.getId(), c.telephone(), c.email(), PageRequest.of(0, CONTACTS_MAX))
                .stream());
        Stream<UUID> desProspects = Stream.of(contact).flatMap(c -> tierRepository
                .trouverDoublons("", c.telephone(), "", c.email(), PageRequest.of(0, CONTACTS_MAX)).stream())
                .map(CfgTier::getCfgCommercial)
                .filter(Objects::nonNull)
                .map(CfgTier::getId);
        return Stream.of(duLot, desProspections, desProspects)
                .flatMap(s -> s)
                .map(parEmploye::get)
                .filter(Objects::nonNull)
                .findFirst();
    }

    /** Pose le commercial décidé et trace « Commercial assigné : B — répartition automatique ». */
    public void appliquer(ComProspection lead, Decision decision) {
        if (!decision.estAttribuee()) {
            return;
        }
        poser(lead, decision.commercialId(), SUITE_REPARTITION);
    }

    private void poser(ComProspection lead, UUID commercialId, String suite) {
        CfgTier commercial = tierRepository.getReferenceById(commercialId);
        lead.setCfgCommercial(commercial);
        prospectionRepository.save(lead);
        historique.enregistrerHistorique(lead.getId(), TypeActionRecente.COMMERCIAL_ASSIGNE,
                TierNomFormatter.nom(commercial) + suite);
    }

    // ---------------------------------------------------------------- création manuelle

    /**
     * Le créateur s'il est commercial (ou chef) avec un employé et habilité sur le projet du lead, sinon la
     * répartition (spec §3.2). Un exempté passe par la répartition.
     */
    public void attribuerALaCreation(ComProspection lead) {
        UUID acteur = PerimetreCommercialService.admUserIdCourant().orElse(null);
        Optional<UUID> createur = createurDestinataire(acteur, projetDe(lead.getStkHierarchy()));
        if (createur.isPresent()) {
            poser(lead, createur.get(), "");
        } else {
            appliquer(lead, decider(lead, nouveauLot()));
        }
        if (lead.getCfgCommercial() != null) {
            publier(acteur, List.of(lead), List.of());
        }
    }

    private Optional<UUID> createurDestinataire(@Nullable UUID acteur, @Nullable UUID projetId) {
        if (acteur == null || projetId == null) {
            return Optional.empty();
        }
        AdmUser compte = userRepository.findById(acteur).orElse(null);
        if (compte == null || compte.getCfgEmployee() == null || Boolean.TRUE.equals(compte.getDisabled())) {
            return Optional.empty();
        }
        Set<String> roles = userRoleRepository.findByAdmUserId(acteur).stream()
                .map(ur -> ur.getAdmRole() != null ? ur.getAdmRole().getCode() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (RolesExemptesPerimetre.exempte(roles) || !RolesEquipe.estCommercialOuChef(roles)
                || !habilitationRepository.findProjetIdsByAdmUserId(acteur).contains(projetId)) {
            return Optional.empty();
        }
        return Optional.of(compte.getCfgEmployee().getId());
    }

    // ---------------------------------------------------------------- notifications

    /** Publie le lot pour les notifications après commit ; rien si le lot est vide. */
    public void publier(@Nullable UUID acteur, List<ComProspection> attribues, List<ComProspection> aDistribuer) {
        LeadsAttribues evenement = new LeadsAttribues(acteur,
                attribues.stream().map(LeadsAttribues.Lead::de).toList(),
                aDistribuer.stream().map(LeadsAttribues.Lead::de).toList());
        if (!evenement.estVide()) {
            evenements.publishEvent(evenement);
        }
    }

    /** Total par commercial d'une liste de décisions, trié par nom. */
    public static List<RepartitionCommercialDto> parCommercial(List<Decision> decisions) {
        Map<UUID, String> noms = new LinkedHashMap<>();
        Map<UUID, Integer> nombres = new HashMap<>();
        for (Decision d : decisions) {
            if (d.estAttribuee()) {
                noms.putIfAbsent(d.commercialId(), d.commercialNom());
                nombres.merge(d.commercialId(), 1, Integer::sum);
            }
        }
        List<RepartitionCommercialDto> lignes = new ArrayList<>();
        noms.forEach((id, nom) -> lignes.add(new RepartitionCommercialDto(id, nom, nombres.get(id))));
        lignes.sort(Comparator.comparing(RepartitionCommercialDto::commercialNom,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return lignes;
    }

    static @Nullable UUID projetDe(@Nullable CfgTranche tranche) {
        return tranche != null && tranche.getCfgProject() != null ? tranche.getCfgProject().getId() : null;
    }
}
