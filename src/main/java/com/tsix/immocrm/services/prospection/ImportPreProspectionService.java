package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.domain.StatutLigneImport;
import com.tsix.immocrm.dto.prospection.CreerPreProspectionRequest;
import com.tsix.immocrm.dto.prospection.LigneImportDto;
import com.tsix.immocrm.dto.prospection.RapportImportPreProspectionsDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.services.equipe.RepartitionLeadsService;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.prospection.LecteurCsvLeads.LigneLead;
import com.tsix.immocrm.utils.PhoneNumberUtils;
import com.tsix.immocrm.utils.Requis;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * Import de l'export CSV des leads en pré-prospections. <b>Un fichier = une tranche</b>, choisie
 * par l'utilisateur. Voir {@code instructions/FEATURE-creation-pre-prospection.md} §5.
 */
@Service
public class ImportPreProspectionService {

    // Défauts = ceux des leads injectés par n8n depuis les mêmes formulaires.
    static final String CODE_ETAPE_DEFAUT = "1 A TRAITER";
    static final String CODE_ORIGINE_DEFAUT = "FACEBOOK";
    static final String CODE_TYPE_DEFAUT = "WEB";

    /** Valeur de « Propriétaire » quand le lead n'est attribué à personne — non recopiée. */
    private static final String PROPRIETAIRE_VIDE = "unassigned";

    private static final int NOTES_MAX = 1000;

    static final long TAILLE_MAX_OCTETS = 5L * 1024 * 1024;

    /** Statuts importables, dans l'ordre de {@code statutsImportes} ; défaut quand {@code statuts} est absent. */
    static final List<StatutLigneImport> STATUTS_IMPORTABLES =
            List.of(StatutLigneImport.NOUVELLE, StatutLigneImport.DOUBLON);

    private final ProspectionService prospectionService;
    private final CfgTrancheRepository trancheRepository;
    private final ComProspectionKindRepository kindRepository;
    private final ComProspectionOriginRepository originRepository;
    private final ComActionMessageTypeRepository typeRepository;
    private final RepartitionLeadsService repartition;

    public ImportPreProspectionService(ProspectionService prospectionService,
                                       CfgTrancheRepository trancheRepository,
                                       ComProspectionKindRepository kindRepository,
                                       ComProspectionOriginRepository originRepository,
                                       ComActionMessageTypeRepository typeRepository,
                                       RepartitionLeadsService repartition) {
        this.prospectionService = prospectionService;
        this.repartition = repartition;
        this.trancheRepository = trancheRepository;
        this.kindRepository = kindRepository;
        this.originRepository = originRepository;
        this.typeRepository = typeRepository;
    }

    /** Référentiels posés sur chaque ligne créée. */
    public record Rattachement(UUID etapeId, UUID origineId, UUID origineSupportId, UUID typeContactId,
                               UUID actionCommercialeId) {
    }

    /** Lit et classe chaque ligne, sans rien écrire. */
    @Transactional(readOnly = true)
    public RapportImportPreProspectionsDto apercu(MultipartFile fichier) {
        return traiter(LecteurCsvLeads.lire(octets(fichier)), null, null, List.of());
    }

    /**
     * Crée une pré-prospection par ligne dont le statut est dans {@code statuts} (défaut :
     * {@code NOUVELLE} et {@code DOUBLON}), toutes sur {@code trancheId}. Tout ou rien : une
     * tranche hors périmètre (403) ou une sélection invalide (400) n'écrit aucune ligne.
     */
    @Transactional
    public RapportImportPreProspectionsDto importer(MultipartFile fichier, UUID trancheId, Rattachement demande,
                                                    List<StatutLigneImport> statuts) {
        List<StatutLigneImport> selection = selection(statuts);
        Requis.champ(trancheId, "trancheId est requis : un fichier est importé sur une seule tranche");
        CfgTranche tranche = trancheRepository.findById(trancheId)
                .orElseThrow(() -> new InvalidRequestException("CfgTranche introuvable: " + trancheId));
        List<LigneLead> lignes = LecteurCsvLeads.lire(octets(fichier));
        Rattachement rattachement = new Rattachement(
                demande.etapeId() != null ? demande.etapeId()
                        : kindRepository.findFirstByCode(CODE_ETAPE_DEFAUT).map(k -> k.getId()).orElse(null),
                demande.origineId() != null ? demande.origineId()
                        : originRepository.findFirstByCode(CODE_ORIGINE_DEFAUT).map(o -> o.getId()).orElse(null),
                demande.origineSupportId(),
                demande.typeContactId() != null ? demande.typeContactId()
                        : typeRepository.findFirstByCode(CODE_TYPE_DEFAUT).map(t -> t.getId()).orElse(null),
                demande.actionCommercialeId());
        return traiter(lignes, tranche, rattachement, selection);
    }

    /** {@code null} = défaut ; vide, {@code DEJA_IMPORTEE} ou {@code ERREUR} → 400. Ordre de {@link #STATUTS_IMPORTABLES}. */
    static List<StatutLigneImport> selection(List<StatutLigneImport> statuts) {
        if (statuts == null) {
            return STATUTS_IMPORTABLES;
        }
        if (statuts.isEmpty() || statuts.contains(null)) {
            throw new InvalidRequestException("statuts vide : choisir NOUVELLE, DOUBLON ou les deux");
        }
        for (StatutLigneImport s : statuts) {
            if (!STATUTS_IMPORTABLES.contains(s)) {
                throw new InvalidRequestException("statuts : " + s + " n'est pas importable "
                        + "(valeurs admises : NOUVELLE, DOUBLON)");
            }
        }
        return STATUTS_IMPORTABLES.stream().filter(statuts::contains).toList();
    }

    /**
     * @param tranche   {@code null} = aperçu, rien n'est écrit
     * @param selection statuts à écrire ; sans effet sur la classification (FEATURE-import-selectif.md R2)
     */
    private RapportImportPreProspectionsDto traiter(List<LigneLead> lignes, CfgTranche tranche,
                                                    Rattachement rattachement, List<StatutLigneImport> selection) {
        // Doublons internes au fichier : l'aperçu n'écrit rien, la base ne peut donc pas les voir.
        // Les lignes écrites par cet import ne comptent pas pour classer les suivantes : sinon le
        // statut dépendrait de la sélection et divergerait de l'aperçu.
        EtatImport etat = new EtatImport(tranche, rattachement, selection,
                new HashMap<>(), new HashMap<>(), new HashSet<>(),
                tranche != null ? repartition.nouveauLot() : null, new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>());
        List<LigneImportDto> resultat = new ArrayList<>();
        for (LigneLead l : lignes) {
            resultat.add(traiterLigne(l, etat));
        }
        if (tranche != null) {
            repartition.publier(PerimetreCommercialService.admUserIdCourant().orElse(null),
                    etat.attribuees(), etat.nonAttribuees());
        }
        return rapport(lignes, tranche, resultat, selection, etat);
    }

    /**
     * État partagé entre les lignes d'un même import (doublons internes au fichier, lignes créées, répartition).
     * {@code lot} nul = aperçu.
     */
    private record EtatImport(CfgTranche tranche, Rattachement rattachement, List<StatutLigneImport> selection,
                              Map<String, Integer> contactsVus, Map<String, Integer> soumissionsVues,
                              Set<UUID> creeesIci, RepartitionLeadsService.Lot lot,
                              List<RepartitionLeadsService.Decision> decisions,
                              List<ComProspection> attribuees, List<ComProspection> nonAttribuees) {
    }

    /** Répartition obligatoire de chaque ligne créée (FEATURE-repartition-notifications.md §3.2). */
    private void repartir(ComProspection creee, EtatImport etat) {
        RepartitionLeadsService.Decision decision = repartition.decider(creee, etat.lot());
        repartition.appliquer(creee, decision);
        etat.decisions().add(decision);
        if (decision.estAttribuee()) {
            etat.attribuees().add(creee);
        } else {
            etat.nonAttribuees().add(creee);
        }
    }

    private LigneImportDto traiterLigne(LigneLead l, EtatImport etat) {
        if (l.erreur() != null) {
            return ligne(l, null, StatutLigneImport.ERREUR, l.erreur(), null, List.of());
        }
        String telephone = telephone(l);
        String contact = cleContact(l.nom(), telephone, l.email());
        LocalDateTime minute = l.dateCreation().truncatedTo(ChronoUnit.MINUTES);
        String soumission = contact + "|" + minute;

        Integer memeLeadPlusHaut = etat.soumissionsVues().get(soumission);
        if (memeLeadPlusHaut != null) {
            return ligne(l, telephone, StatutLigneImport.DEJA_IMPORTEE,
                    "Même lead que la ligne " + memeLeadPlusHaut + " du fichier", null, List.of());
        }
        // Réimport jugé sur toute la table (sinon un commercial restreint recréerait un lead
        // importé ailleurs), mais seuls les doublons visibles sont nommés ou font un DOUBLON :
        // un contact hors périmètre n'est jamais révélé.
        Set<UUID> creeesIci = etat.creeesIci();
        List<ComProspection> tous = prospectionService.doublonsContact(null, l.nom(), l.email(), telephone);
        List<ComProspection> avant = tous.stream().filter(p -> !creeesIci.contains(p.getId())).toList();
        // Requête dédiée : doublonsContact est plafonné, ses lignes récentes peuvent évincer la soumission.
        List<ComProspection> memeSoumission = prospectionService
                .memeSoumission(l.nom(), l.email(), telephone, minute).stream()
                .filter(p -> !creeesIci.contains(p.getId()))
                .toList();
        if (!memeSoumission.isEmpty()) {
            List<ComProspection> nommables = prospectionService.visibles(memeSoumission);
            return ligne(l, telephone, StatutLigneImport.DEJA_IMPORTEE,
                    nommables.isEmpty() ? "Lead déjà importé" : "Déjà en base : " + codes(nommables),
                    null, ids(nommables));
        }
        Integer memeContactPlusHaut = etat.contactsVus().get(contact);
        StatutLigneImport statut = !prospectionService.visibles(avant).isEmpty() || memeContactPlusHaut != null
                ? StatutLigneImport.DOUBLON : StatutLigneImport.NOUVELLE;
        // Nommés : y compris les lignes créées plus haut par cet import.
        List<ComProspection> existants = statut == StatutLigneImport.DOUBLON
                ? prospectionService.visibles(tous) : List.of();
        String message = messageDoublon(statut, existants, memeContactPlusHaut);
        etat.contactsVus().putIfAbsent(contact, l.numeroLigne());
        etat.soumissionsVues().put(soumission, l.numeroLigne());

        ComProspection creee = null;
        if (etat.tranche() != null && !etat.selection().contains(statut)) {
            message = message == null ? "Non importée (statut non sélectionné)"
                    : message + " — non importée (statut non sélectionné)";
        } else if (etat.tranche() != null) {
            creee = creerPreProspection(l, telephone, etat);
            creeesIci.add(creee.getId());
            repartir(creee, etat);
        }
        return ligne(l, telephone, statut, message, creee, ids(existants));
    }

    private static String messageDoublon(StatutLigneImport statut, List<ComProspection> existants,
                                         Integer memeContactPlusHaut) {
        if (!existants.isEmpty()) {
            return "Même contact que " + codes(existants) + " (autre soumission)";
        }
        if (statut == StatutLigneImport.DOUBLON) {
            return "Même contact que la ligne " + memeContactPlusHaut + " du fichier (autre soumission)";
        }
        return null;
    }

    private ComProspection creerPreProspection(LigneLead l, String telephone, EtatImport etat) {
        Rattachement rattachement = etat.rattachement();
        return prospectionService.ecrirePreProspection(new CreerPreProspectionRequest(
                l.nom(), telephone, l.email(), notes(l), etat.tranche().getId(), null,
                rattachement.etapeId(), rattachement.origineId(), rattachement.origineSupportId(),
                rattachement.typeContactId(), rattachement.actionCommercialeId(), null),
                l.dateCreation());
    }

    private RapportImportPreProspectionsDto rapport(List<LigneLead> lignes, CfgTranche tranche,
                                                    List<LigneImportDto> resultat, List<StatutLigneImport> selection,
                                                    EtatImport etat) {
        Set<String> formulaires = new LinkedHashSet<>();
        lignes.stream().map(LigneLead::formulaire).filter(f -> f != null).forEach(formulaires::add);

        // Tranche proposée : le premier mot de chaque formulaire, s'il est un code de tranche et
        // que tous les formulaires désignent la même.
        Map<UUID, CfgTranche> designees = new LinkedHashMap<>();
        boolean toutesReconnues = !formulaires.isEmpty();
        for (String f : formulaires) {
            Optional<CfgTranche> t = trancheDuFormulaire(f);
            t.ifPresent(tr -> designees.putIfAbsent(tr.getId(), tr));
            toutesReconnues &= t.isPresent();
        }
        CfgTranche suggeree = toutesReconnues && designees.size() == 1
                ? designees.values().iterator().next() : null;

        List<String> avertissements = new ArrayList<>();
        if (designees.size() > 1) {
            StringJoiner codes = new StringJoiner(", ");
            designees.values().forEach(t -> codes.add(t.getCode()));
            avertissements.add("Les formulaires de ce fichier désignent plusieurs tranches (" + codes
                    + ") : un import rattache toutes les lignes à une seule — découper le fichier par tranche.");
        }
        if (tranche != null && suggeree != null && !suggeree.getId().equals(tranche.getId())) {
            avertissements.add("Tranche choisie (" + tranche.getCode() + ") différente de celle des "
                    + "formulaires (" + suggeree.getCode() + ").");
        }

        return new RapportImportPreProspectionsDto(
                tranche != null,
                tranche != null ? tranche.getId() : null,
                tranche != null ? tranche.getCode() : null,
                suggeree != null ? suggeree.getId() : null,
                suggeree != null ? suggeree.getCode() : null,
                List.copyOf(formulaires),
                avertissements,
                resultat.size(),
                compter(resultat, StatutLigneImport.NOUVELLE),
                compter(resultat, StatutLigneImport.DOUBLON),
                compter(resultat, StatutLigneImport.DEJA_IMPORTEE),
                compter(resultat, StatutLigneImport.ERREUR),
                resultat,
                tranche != null ? selection : null,
                (int) resultat.stream().filter(l -> l.preProspectionId() != null).count(),
                RepartitionLeadsService.parCommercial(etat.decisions()),
                etat.nonAttribuees().size());
    }

    /** {@code "APF FORM 02*2026 - GISK"} → la tranche de code {@code APF}, si elle existe. */
    private Optional<CfgTranche> trancheDuFormulaire(String formulaire) {
        String[] mots = formulaire.trim().split("[\\s*_\\-]+");
        if (mots.length == 0 || mots[0].isEmpty()) {
            return Optional.empty();
        }
        return trancheRepository.findFirstByCode(mots[0].toUpperCase(Locale.ROOT));
    }

    /** « Téléphone », à défaut WhatsApp, à défaut le secondaire — tel qu'écrit dans le fichier. */
    private static String telephone(LigneLead l) {
        if (l.telephone() != null) return l.telephone();
        if (l.whatsapp() != null) return l.whatsapp();
        return l.telephoneSecondaire();
    }

    /**
     * Ce que l'export sait du lead et qui n'a pas de colonne dédiée — même usage des notes que
     * les leads n8n, qui y déposent les réponses au formulaire.
     */
    static String notes(LigneLead l) {
        List<String> lignes = new ArrayList<>();
        lignes.add("Lead importé (export CSV)" + (l.formulaire() != null ? " — formulaire : " + l.formulaire() : ""));
        StringJoiner contexte = new StringJoiner(" · ");
        if (l.source() != null) contexte.add("Source : " + l.source());
        if (l.canal() != null) contexte.add("Canal : " + l.canal());
        if (l.etape() != null) contexte.add("Étape : " + l.etape());
        if (contexte.length() > 0) lignes.add(contexte.toString());
        if (l.proprietaire() != null && !PROPRIETAIRE_VIDE.equalsIgnoreCase(l.proprietaire())) {
            lignes.add("Propriétaire : " + l.proprietaire());
        }
        if (l.etiquettes() != null) lignes.add("Étiquettes : " + l.etiquettes());
        String retenu = telephone(l);
        if (l.whatsapp() != null && !l.whatsapp().equals(retenu)) lignes.add("WhatsApp : " + l.whatsapp());
        if (l.telephoneSecondaire() != null && !l.telephoneSecondaire().equals(retenu)) {
            lignes.add("Téléphone secondaire : " + l.telephoneSecondaire());
        }
        String notes = String.join("\n", lignes);
        return notes.length() > NOTES_MAX ? notes.substring(0, NOTES_MAX) : notes;
    }

    private static String cleContact(String nom, String telephone, String email) {
        return (nom == null ? "" : nom.trim().toLowerCase(Locale.ROOT)) + "|"
                + PhoneNumberUtils.normalize(telephone) + "|"
                + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private static LigneImportDto ligne(LigneLead l, String telephone, StatutLigneImport statut, String message,
                                        ComProspection creee, List<UUID> doublonIds) {
        return new LigneImportDto(l.numeroLigne(), l.dateCreation(), l.nom(),
                telephone != null ? telephone : telephone(l), l.email(), l.formulaire(), statut, message,
                creee != null ? creee.getId() : null, creee != null ? creee.getCode() : null, doublonIds);
    }

    private static String codes(List<ComProspection> prospections) {
        StringJoiner j = new StringJoiner(", ");
        prospections.stream().limit(5).forEach(p -> j.add(p.getCode() != null ? p.getCode() : p.getId().toString()));
        return j + (prospections.size() > 5 ? "…" : "");
    }

    private static List<UUID> ids(List<ComProspection> prospections) {
        return prospections.stream().map(ComProspection::getId).toList();
    }

    private static int compter(List<LigneImportDto> lignes, StatutLigneImport statut) {
        return (int) lignes.stream().filter(l -> l.statut() == statut).count();
    }

    private static byte[] octets(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new InvalidRequestException("Fichier requis (champ multipart « fichier »)");
        }
        // Avant tout décodage : 5 000 lignes de leads pèsent bien moins ; la limite globale (50 Mo) vise les pièces jointes.
        if (fichier.getSize() > TAILLE_MAX_OCTETS) {
            throw new InvalidRequestException("Fichier trop volumineux : 5 Mo au plus — le découper");
        }
        try {
            return fichier.getBytes();
        } catch (IOException e) {
            throw new InvalidRequestException("Fichier illisible : " + e.getMessage());
        }
    }
}
