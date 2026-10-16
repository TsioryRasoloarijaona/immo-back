package com.tsix.immocrm.services.dossier.edition;

import com.tsix.immocrm.dto.dossier.edition.DossierModifieDto;
import com.tsix.immocrm.dto.dossier.edition.DroitsEditionDto;
import com.tsix.immocrm.dto.dossier.edition.ModifierDossierRequest;
import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.dossier.DossierSorti;
import com.tsix.immocrm.services.dossier.HistoriqueDossier;
import com.tsix.immocrm.services.dossier.fiche.DossierFicheService;
import com.tsix.immocrm.services.dossier.fiche.EcritureDossier;
import com.tsix.immocrm.services.dossier.workflow.WorkflowDossier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@code PATCH /api/dossiers/{id}} : fiche modifiable par groupe de droits (FEATURE-vente-edition.md). Ordre des
 * refus : 400 (corps), 403 (groupe non autorisé), 409 (dossier sorti), puis 400 métier (plafond, dates).
 */
@Service
public class DossierEditionService {

    private static final String PREFIXE_ROLE = "ROLE_";
    private static final String CLIENT = "clientId";
    private static final String NOTE = "note";
    private static final int LONGUEUR_HISTORIQUE = 255;
    private static final int LONGUEUR_TEXTE = 255;
    private static final int LONGUEUR_LONGUE = 1000;
    /** Jalons contrôlés par {@link OrdreDatesDossier}. */
    private static final Set<String> DATES_ORDONNEES = Set.of("dateDossier", "concretisation",
            "envoiNotairePrevisionnel", "envoiNotaire", "minute", "remiseClesEffective");
    /** Volet résiliation obligatoire d'un dossier sorti ({@code Sale.cs:BeforeUpdate}). */
    private static final Set<String> ANNULATION_OBLIGATOIRE = Set.of("annulationDate", "annulationCauses");
    private static final String MESSAGE_ANNULATION = """
            Dans le cas de la résilation / annulation d'un dossier, il faut s'assurer des points suivant dans le \
            volet de résiliation :
                - Il faut saisir la date de résiliation.
                - Il faut saisir le montant de résiliation à restituer.
                - Il faut saisir les causes de la résiliation.
                - Il faut reparamtrer les sous articles.

            Attention : La résiliation / Annulation du dossier va automatiquement engendrer la libération de \
            l’article concerné pour sa remise en vente !!!""";

    private final EcritureDossier ecriture;
    private final ChampsEdition champs;
    private final PrixEdition prix;
    private final ClientEdition client;
    private final NoteEdition notes;
    private final HistoriqueDossier historique;
    private final DossierFicheService ficheService;
    private final WorkflowDossier workflow;

    public DossierEditionService(EcritureDossier ecriture, ChampsEdition champs, PrixEdition prix,
                                 ClientEdition client, NoteEdition notes, HistoriqueDossier historique,
                                 DossierFicheService ficheService, WorkflowDossier workflow) {
        this.workflow = workflow;
        this.ecriture = ecriture;
        this.champs = champs;
        this.prix = prix;
        this.client = client;
        this.notes = notes;
        this.historique = historique;
        this.ficheService = ficheService;
    }

    @Transactional(readOnly = true)
    public DroitsEditionDto droits(UUID dossierId) {
        ComFolder dossier = ecriture.charger(dossierId);
        Set<String> roles = rolesAppelant();
        boolean sorti = DossierSorti.estSorti(dossier);
        List<String> groupes = Arrays.stream(GroupeEdition.values())
                .filter(g -> g.autorise(roles))
                .filter(g -> !sorti || g == GroupeEdition.ANNULATION)
                .map(Enum::name)
                .toList();
        return new DroitsEditionDto(groupes, sorti, VerrousEdition.verrouilles(dossier, roles));
    }

    @Transactional(readOnly = true)
    public List<ReferentielItemDto> decisionsMeilleurTaux() {
        return champs.decisions();
    }

    @Transactional
    public DossierModifieDto modifier(UUID dossierId, ModifierDossierRequest r) {
        ComFolder dossier = ecriture.charger(dossierId);
        if (r == null) {
            throw new InvalidRequestException("Corps de requête requis.");
        }
        Set<String> aVider = aVider(r);
        Set<GroupeEdition> groupes = groupesTouches(r, aVider);
        valider(r);
        Set<String> roles = rolesAppelant();
        autoriser(groupes, roles);
        VerrousEdition.verifier(dossier, r, aVider, roles);
        prix.verifierVerrou(dossier, r, PlafondRemise.niveauDe(roles));
        boolean sorti = DossierSorti.estSorti(dossier);
        if (sorti && !EnumSet.of(GroupeEdition.ANNULATION).containsAll(groupes)) {
            throw new ConflictException("Dossier annulé ou désisté : seul l'onglet Annulation est modifiable.");
        }
        if (sorti && aVider.stream().anyMatch(ANNULATION_OBLIGATOIRE::contains)) {
            throw new InvalidRequestException(MESSAGE_ANNULATION);
        }

        List<String> modifies = new ArrayList<>();
        appliquerClient(dossier, r, modifies);
        List<String> archivees = modifies.contains(CLIENT) ? client.archiverProspectionsDuClient(dossier) : List.of();
        for (Champ<?> c : champs.tous()) {
            boolean vider = aVider.contains(c.nom());
            if ((vider || c.present(r)) && c.appliquer(r, dossier, vider)) {
                modifies.add(c.nom());
            }
        }
        BigDecimal ecart = appliquerPrix(dossier, r, modifies);
        appliquerNote(dossier, r, aVider, modifies);
        if (modifies.stream().anyMatch(DATES_ORDONNEES::contains)) {
            OrdreDatesDossier.verifier(dossier);
        }
        if (!modifies.isEmpty()) {
            workflow.verifierEnregistrement(dossier);
            ecriture.signer(dossier);
            historique.evenement(dossier, tronquer("Modification : " + String.join(", ", modifies)));
        }
        return new DossierModifieDto(ficheService.fiche(dossierId), List.copyOf(modifies), ecart, archivees);
    }

    private void appliquerClient(ComFolder dossier, ModifierDossierRequest r, List<String> modifies) {
        if (r.clientId() != null && client.changer(dossier, champs.tier(CLIENT, r.clientId()))) {
            modifies.add(CLIENT);
        }
    }

    private BigDecimal appliquerPrix(ComFolder dossier, ModifierDossierRequest r, List<String> modifies) {
        if (PrixEdition.touchePrix(r)) {
            boolean forcer = Boolean.TRUE.equals(r.forcerRemise());
            BigDecimal tvaAvant = dossier.getVATRatio();
            if (prix.recalculer(dossier, r, PlafondRemise.niveauDe(rolesAppelant()), forcer)) {
                modifies.add("remise");
            }
            if (r.tva() != null && (tvaAvant == null || tvaAvant.compareTo(r.tva()) != 0)) {
                modifies.add("tva");
            }
        } else if (modifies.contains("integrerModificationsAuPrix")) {
            prix.recalculerSansRemise(dossier);
        } else {
            return null;
        }
        return prix.ecartSchema(dossier);
    }

    private void appliquerNote(ComFolder dossier, ModifierDossierRequest r, Set<String> aVider, List<String> modifies) {
        String note = aVider.contains(NOTE) ? null : ChampsEdition.sansBlancs(r.note());
        if ((note != null || aVider.contains(NOTE)) && notes.appliquer(dossier, note)) {
            modifies.add(NOTE);
        }
    }

    private Set<String> aVider(ModifierDossierRequest r) {
        Set<String> noms = r.champsAVider() == null ? Set.of() : new LinkedHashSet<>(r.champsAVider());
        Map<String, Champ<?>> parNom = champs.tous().stream()
                .collect(Collectors.toMap(Champ::nom, Function.identity()));
        for (String nom : noms) {
            Champ<?> c = parNom.get(nom);
            boolean vidable = NOTE.equals(nom) || (c != null && c.vidable());
            if (!vidable) {
                throw new InvalidRequestException("champsAVider : « %s » ne peut pas être vidé.".formatted(nom));
            }
        }
        return noms;
    }

    private Set<GroupeEdition> groupesTouches(ModifierDossierRequest r, Set<String> aVider) {
        Set<GroupeEdition> groupes = EnumSet.noneOf(GroupeEdition.class);
        for (Champ<?> c : champs.tous()) {
            if (c.present(r) || aVider.contains(c.nom())) {
                groupes.add(c.groupe());
            }
        }
        if (r.clientId() != null) {
            groupes.add(GroupeEdition.CLIENT);
        }
        if (PrixEdition.touchePrix(r)) {
            groupes.add(GroupeEdition.PRIX);
        }
        if (r.note() != null || aVider.contains(NOTE)) {
            groupes.add(GroupeEdition.NOTE);
        }
        if (groupes.isEmpty()) {
            throw new InvalidRequestException("Aucun champ à modifier.");
        }
        return groupes;
    }

    private static void valider(ModifierDossierRequest r) {
        PrixEdition.valider(r);
        longueur("numeroTitre", r.numeroTitre(), LONGUEUR_TEXTE);
        longueur("agenceCredit", r.agenceCredit(), LONGUEUR_TEXTE);
        longueur("annulationCauses", r.annulationCauses(), LONGUEUR_LONGUE);
        longueur(NOTE, r.note(), LONGUEUR_LONGUE);
    }

    private static void longueur(String nom, String valeur, int max) {
        if (valeur != null && valeur.trim().length() > max) {
            throw new InvalidRequestException("%s : %d caractères au plus.".formatted(nom, max));
        }
    }

    private static void autoriser(Set<GroupeEdition> groupes, Set<String> roles) {
        for (GroupeEdition g : groupes) {
            if (!g.autorise(roles)) {
                throw new ForbiddenException("Modification « %s » réservée aux rôles : %s."
                        .formatted(g.name(), String.join(", ", g.roles().stream().sorted().toList())));
            }
        }
    }

    private static Set<String> rolesAppelant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && a.startsWith(PREFIXE_ROLE))
                .map(a -> a.substring(PREFIXE_ROLE.length()))
                .collect(Collectors.toSet());
    }

    private static String tronquer(String texte) {
        return texte.length() <= LONGUEUR_HISTORIQUE ? texte : texte.substring(0, LONGUEUR_HISTORIQUE - 1) + "…";
    }
}
