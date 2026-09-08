package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.domain.MotifEnvoiVisite;
import com.tsix.immocrm.dto.prospection.CanalEnvoiDto;
import com.tsix.immocrm.dto.prospection.ResultatEnvoiVisiteDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.app.AppFileObject;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.mail.EmailProperties;
import com.tsix.immocrm.mail.EnvoiEmail;
import com.tsix.immocrm.mail.MessageEmail;
import com.tsix.immocrm.mail.PieceJointeEmail;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileSummaryRepository;
import com.tsix.immocrm.repositories.app.AppFileObjectRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.services.piecejointe.PieceJointeSupport;
import com.tsix.immocrm.storage.StockageFichiers;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import com.tsix.immocrm.whatsapp.EnvoiWhatsApp;
import com.tsix.immocrm.whatsapp.MessageWhatsApp;
import com.tsix.immocrm.whatsapp.NumeroWhatsApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Construit et expédie la confirmation de visite envoyée au prospect quand un rendez-vous est
 * planifié. Voir {@code instructions/FEATURE-mail-confirmation-visite.md}.
 *
 * <p><b>Une résolution, deux canaux, deux déclencheurs.</b> Le contenu est résolu une fois
 * ({@link ContenuVisite}) puis mis en forme séparément pour l'email et pour WhatsApp ; l'envoi
 * automatique (après commit) et le renvoi manuel (endpoint) empruntent tous deux ce chemin. Les
 * règles R1 à R7 ne peuvent donc diverger ni entre les canaux, ni entre les déclencheurs.
 *
 * <p><b>Les deux canaux sont tentés indépendamment</b>, et cela répond à un écart mesuré :
 * 99 % des prospections avec rendez-vous portent un numéro exploitable contre 57 % une adresse
 * email. Lier les deux — n'envoyer le WhatsApp qu'en l'absence d'email, par exemple — priverait
 * du canal le plus lu la moitié des prospects qui en disposent.
 */
@Service
@Transactional(readOnly = true)
public class ConfirmationVisiteService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmationVisiteService.class);

    private static final String TEMPLATE = "mail/confirmation-visite";
    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter FORMAT_HEURE =
            DateTimeFormatter.ofPattern("HH'h'mm", Locale.FRENCH);

    /**
     * Longueur en deçà de laquelle une adresse est tenue pour un remplissage de formulaire plutôt
     * que pour une adresse (R4). Calibrée sur le cas réel : le projet {@code YESTEST} porte
     * littéralement {@code "adresse"}, sept caractères.
     */
    private static final int LONGUEUR_MINIMALE_ADRESSE = 8;

    /**
     * Seuls les types de pièce jointe dont le code commence ainsi sont expédiables (R6) —
     * {@code PLAN}, {@code PLAN_ARCHITECTE}, {@code PLAN_ETAGE}. Ce n'est pas un confort de
     * filtrage : sans lui, le niveau projet enverrait au prospect n'importe quel document qu'un
     * utilisateur y aurait déposé, contrats signés compris.
     */
    private static final String PREFIXE_TYPE_PLAN = "PLAN";

    private final ComActionMessageRepository actionMessageRepository;
    private final AdmAttachedFileSummaryRepository attachedFileSummaryRepository;
    private final AppFileObjectRepository fileObjectRepository;
    private final StockageFichiers stockage;
    private final EnvoiEmail envoiEmail;
    private final EnvoiWhatsApp envoiWhatsApp;
    private final EmailProperties properties;

    public ConfirmationVisiteService(ComActionMessageRepository actionMessageRepository,
                                     AdmAttachedFileSummaryRepository attachedFileSummaryRepository,
                                     AppFileObjectRepository fileObjectRepository,
                                     StockageFichiers stockage,
                                     EnvoiEmail envoiEmail,
                                     EnvoiWhatsApp envoiWhatsApp,
                                     EmailProperties properties) {
        this.actionMessageRepository = actionMessageRepository;
        this.attachedFileSummaryRepository = attachedFileSummaryRepository;
        this.fileObjectRepository = fileObjectRepository;
        this.stockage = stockage;
        this.envoiEmail = envoiEmail;
        this.envoiWhatsApp = envoiWhatsApp;
        this.properties = properties;
    }

    /**
     * Chemin de l'écouteur : l'interaction vient d'être écrite, son identifiant est le seul
     * élément transporté par l'événement.
     */
    public ResultatEnvoiVisiteDto envoyer(UUID interactionId) {
        ComActionMessage interaction = actionMessageRepository.findById(interactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Interaction introuvable: " + interactionId));
        return envoyerPour(interaction);
    }

    /**
     * Chemin de l'endpoint {@code POST /api/prospections/{id}/interactions/{interactionId}/email-visite}.
     *
     * <p>Le contrôle de périmètre commercial n'est <b>pas</b> refait ici : {@code PerimetreInterceptor}
     * couvre déjà {@code /api/prospections/{id}/**} et rend 404 pour une prospection hors périmètre.
     * L'appartenance de l'interaction à cette prospection-là, en revanche, se vérifie — sans quoi
     * l'endpoint permettrait de faire envoyer le rendez-vous d'autrui en énumérant les identifiants.
     */
    public ResultatEnvoiVisiteDto renvoyer(UUID prospectionId, UUID interactionId) {
        ComActionMessage interaction = actionMessageRepository.findById(interactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Interaction introuvable: " + interactionId));

        ComProspection prospection = interaction.getComProspection();
        if (prospection == null || !prospection.getId().equals(prospectionId)) {
            throw new ResourceNotFoundException(
                    "Interaction introuvable pour cette prospection: " + interactionId);
        }
        if (interaction.getAdmEvent() == null) {
            // 409 et non 400 : la requête est bien formée, c'est l'état de l'interaction qui s'y
            // oppose — même lecture que le refus d'une transition de statut de dossier.
            throw new ConflictException(
                    "Cette interaction ne planifie aucun rendez-vous : rien à confirmer.");
        }
        return envoyerPour(interaction);
    }

    /**
     * Résout le contenu puis expédie sur les deux canaux. Ne lève jamais pour un motif métier :
     * un prospect sans adresse est une issue, pas une erreur.
     */
    private ResultatEnvoiVisiteDto envoyerPour(ComActionMessage interaction) {
        AdmEvent rendezVous = interaction.getAdmEvent();
        if (rendezVous == null || rendezVous.getStartHour() == null) {
            // Atteignable seulement par l'écouteur, dont l'événement n'est publié que sur création
            // d'un AdmEvent — donc jamais en pratique. Vérifié quand même : l'alternative serait un
            // NullPointerException dans un fil asynchrone, c'est-à-dire une trace illisible.
            log.warn("Confirmation de visite ignorée : l'interaction {} ne porte pas de rendez-vous.",
                    interaction.getId());
            return ResultatEnvoiVisiteDto.de(
                    CanalEnvoiDto.nonEnvoye(MotifEnvoiVisite.DESTINATAIRE_ABSENT, null),
                    CanalEnvoiDto.nonEnvoye(MotifEnvoiVisite.DESTINATAIRE_ABSENT, null));
        }

        ContenuVisite contenu = resoudre(interaction.getComProspection(), rendezVous.getStartHour());

        return ResultatEnvoiVisiteDto.de(
                parEmail(contenu, interaction.getId()),
                parWhatsApp(contenu, interaction.getId()));
    }

    // ------------------------------------------------------------------ résolution du contenu

    private ContenuVisite resoudre(ComProspection prospection, LocalDateTime rendezVous) {
        CfgTier prospect = prospection != null ? prospection.getCfgProspect() : null;
        CfgTranche tranche = prospection != null ? prospection.getStkHierarchy() : null;
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);
        CfgTier commercial = prospection != null ? prospection.getCfgCommercial() : null;

        Adresse adresse = adresse(tranche, projet);

        return new ContenuVisite(
                prospection,
                projet,
                tranche,
                TierNomFormatter.nom(prospect),
                email(prospect, prospection),
                numeroWhatsApp(prospect),
                prospect != null && StringUtils.hasText(prospect.getMobile()),
                rendezVous.format(FORMAT_DATE),
                rendezVous.format(FORMAT_HEURE),
                lieu(projet, tranche),
                adresse != null ? adresse.ligne() : null,
                adresse != null ? adresse.ville() : null,
                commercial != null ? TierNomFormatter.nom(commercial) : null,
                commercial != null ? nettoyer(commercial.getMobile()) : null);
    }

    /**
     * R1 — la fiche du prospect, à défaut le champ saisi au premier contact, puis un contrôle de
     * forme minimal.
     *
     * <p>Le contrôle se réduit à la présence d'un {@code @} : ni plus — une expression régulière
     * d'adresse email complète rejette des adresses valides et coûte plus qu'elle ne rapporte —
     * ni moins, puisque 33 lignes réelles portent une valeur non vide qui n'est pas une adresse
     * (un numéro de téléphone, le plus souvent). Le serveur SMTP refuserait le message, et
     * l'échec serait invisible depuis l'application.
     */
    private static String email(CfgTier prospect, ComProspection prospection) {
        if (prospection == null) {
            return null;
        }
        String brut = ProspectionService.emailProspect(prospect, prospection);
        if (brut == null) {
            return null;
        }
        String propre = brut.trim();
        return propre.contains("@") ? propre : null;
    }

    /**
     * R1 bis — le numéro WhatsApp, composé du mobile de la fiche et de son indicatif.
     *
     * <p><b>Aucun repli sur {@code ComProspection.TempTel}</b>, contrairement à l'email. Deux
     * raisons, l'une mesurée et l'autre de principe : le mobile de la fiche couvre déjà
     * 5 070 des 5 079 prospections avec rendez-vous, si bien que le repli en apporterait <i>huit</i> ;
     * et {@code TempTel} ne porte pas d'indicatif, ce qui obligerait à supposer le Maroc — une
     * supposition qui, sur les 225 tiers d'indicatif « 33 », enverrait le message à un inconnu.
     * Huit prospections ne valent pas ce risque.
     */
    private static String numeroWhatsApp(CfgTier prospect) {
        if (prospect == null) {
            return null;
        }
        return NumeroWhatsApp.international(prospect.getMobileIndicatif(), prospect.getMobile());
    }

    /** R3 — le projet, précisé de sa tranche lorsque celle-ci porte un libellé différent. */
    private static String lieu(CfgProject projet, CfgTranche tranche) {
        String nomProjet = nettoyer(Champs.de(projet, CfgProject::getDescription));
        String nomTranche = nettoyer(Champs.de(tranche, CfgTranche::getDescription));

        if (nomProjet == null) {
            return nomTranche;
        }
        if (nomTranche == null || nomTranche.equalsIgnoreCase(nomProjet)) {
            return nomProjet;
        }
        return nomProjet + " — " + nomTranche;
    }

    /**
     * R4 — la tranche d'abord parce qu'elle est plus précise, le projet ensuite parce qu'il est
     * toujours renseigné (5 071/5 071 contre 1 639). Le code postal et la ville accompagnent
     * <b>le porteur retenu</b> : panacher l'adresse de la tranche avec la ville du projet
     * fabriquerait une adresse qui n'existe nulle part.
     */
    private static Adresse adresse(CfgTranche tranche, CfgProject projet) {
        Adresse depuisTranche = adresseValide(
                Champs.de(tranche, CfgTranche::getAddress),
                Champs.de(tranche, CfgTranche::getZipCode),
                Champs.de(tranche, CfgTranche::getCity),
                Champs.de(tranche, CfgTranche::getDescription));
        if (depuisTranche != null) {
            return depuisTranche;
        }
        return adresseValide(
                Champs.de(projet, CfgProject::getAddress),
                Champs.de(projet, CfgProject::getZipCode),
                Champs.de(projet, CfgProject::getCity),
                Champs.de(projet, CfgProject::getDescription));
    }

    /**
     * Rejette les deux formes de fausse adresse observées en données réelles, et rien d'autre :
     *
     * <ul>
     *   <li>trop courte pour être une adresse — {@code "adresse"} sur le projet {@code YESTEST} ;</li>
     *   <li>égale au libellé de son propre porteur — {@code "Mansouria Beach Resort"} sur
     *       {@code MBR}, qui recopie le nom du projet.</li>
     * </ul>
     *
     * <p>Le filtre est volontairement étroit. Le but n'est pas de valider des adresses marocaines
     * — personne ne sait le faire de façon fiable — mais d'empêcher deux cas connus d'atteindre un
     * client. Un doute se tranche en faveur de l'omission : un message sans bloc adresse reste
     * utile, un message avec une fausse adresse envoie quelqu'un au mauvais endroit.
     */
    private static Adresse adresseValide(String adresse, String codePostal, String ville,
                                         String libellePorteur) {
        String propre = nettoyer(adresse);
        if (propre == null || propre.length() < LONGUEUR_MINIMALE_ADRESSE) {
            return null;
        }
        String libelle = nettoyer(libellePorteur);
        if (libelle != null && propre.equalsIgnoreCase(libelle)) {
            return null;
        }

        String cp = nettoyer(codePostal);
        String cite = nettoyer(ville);
        String ligneVille = (cp != null && cite != null) ? cp + " " + cite
                : (cite != null ? cite : cp);
        return new Adresse(propre, ligneVille);
    }

    // ------------------------------------------------------------------ canal email

    private CanalEnvoiDto parEmail(ContenuVisite contenu, UUID interactionId) {
        if (contenu.email() == null) {
            log.info("Confirmation de visite non envoyée par email pour l'interaction {} : aucune "
                    + "adresse exploitable sur la prospection.", interactionId);
            return CanalEnvoiDto.nonEnvoye(MotifEnvoiVisite.DESTINATAIRE_ABSENT, null);
        }
        if (!envoiEmail.estActif()) {
            return CanalEnvoiDto.nonEnvoye(MotifEnvoiVisite.SERVICE_INACTIF, contenu.email());
        }

        // Les plans ne sont lus qu'ici : le téléchargement depuis le stockage coûte cher, et il
        // serait absurde de le payer pour un prospect sans adresse email.
        List<PieceJointeEmail> plans = plans(contenu);

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nomProspect", contenu.nomProspect());
        variables.put("date", contenu.date());
        variables.put("heure", contenu.heure());
        variables.put("lieu", contenu.lieu());
        variables.put("adresse", contenu.adresse());
        variables.put("ville", contenu.ville());
        variables.put("nomCommercial", contenu.nomCommercial());
        variables.put("mobileCommercial", contenu.mobileCommercial());
        variables.put("plans", plans.stream().map(PieceJointeEmail::nom).toList());

        envoiEmail.envoyer(new MessageEmail(contenu.email(), sujet(contenu), TEMPLATE,
                variables, corpsTexte(contenu, !plans.isEmpty()), plans));

        return CanalEnvoiDto.envoye(contenu.email(), plans.size());
    }

    private static String sujet(ContenuVisite contenu) {
        return StringUtils.hasText(contenu.lieu())
                ? "Votre visite du " + contenu.date() + " — " + contenu.lieu()
                : "Confirmation de votre rendez-vous du " + contenu.date();
    }

    // ------------------------------------------------------------------ canal WhatsApp

    private CanalEnvoiDto parWhatsApp(ContenuVisite contenu, UUID interactionId) {
        if (contenu.numeroWhatsApp() == null) {
            // Distinguer les deux permet au commercial de savoir s'il doit obtenir un numéro ou
            // en réparer un : 43 fiches réelles portent un numéro que rien ne peut envoyer.
            MotifEnvoiVisite motif = contenu.aUnMobileBrut()
                    ? MotifEnvoiVisite.NUMERO_INEXPLOITABLE
                    : MotifEnvoiVisite.DESTINATAIRE_ABSENT;
            log.info("Confirmation de visite non envoyée par WhatsApp pour l'interaction {} : {}.",
                    interactionId, motif);
            return CanalEnvoiDto.nonEnvoye(motif, null);
        }
        if (!envoiWhatsApp.estActif()) {
            return CanalEnvoiDto.nonEnvoye(MotifEnvoiVisite.SERVICE_INACTIF, contenu.numeroWhatsApp());
        }

        boolean parti = envoiWhatsApp.envoyer(
                new MessageWhatsApp(contenu.numeroWhatsApp(), texteWhatsApp(contenu)));

        return parti
                ? CanalEnvoiDto.envoye(contenu.numeroWhatsApp(), 0)
                : CanalEnvoiDto.nonEnvoye(MotifEnvoiVisite.ECHEC_ENVOI, contenu.numeroWhatsApp());
    }

    /**
     * Le message WhatsApp reprend les mêmes informations que le mail, dans une forme adaptée au
     * canal : court, sans salutation cérémonieuse, et avec la mise en gras {@code *…*} que
     * WhatsApp interprète.
     *
     * <p>Il ne mentionne pas les plans, puisqu'il n'en porte aucun — annoncer une pièce jointe
     * absente serait pire que se taire.
     */
    private static String texteWhatsApp(ContenuVisite contenu) {
        StringBuilder texte = new StringBuilder();
        texte.append("Bonjour");
        if (contenu.nomProspect() != null) {
            texte.append(' ').append(contenu.nomProspect());
        }
        texte.append(",\n\nVotre visite est confirmée.\n\n");
        texte.append("*Date* : ").append(contenu.date())
                .append(" à ").append(contenu.heure()).append('\n');
        if (contenu.lieu() != null) {
            texte.append("*Projet* : ").append(contenu.lieu()).append('\n');
        }
        if (contenu.adresse() != null) {
            texte.append("*Adresse* : ").append(contenu.adresse());
            if (contenu.ville() != null) {
                texte.append(", ").append(contenu.ville());
            }
            texte.append('\n');
        }
        if (contenu.nomCommercial() != null) {
            texte.append("*Votre conseiller* : ").append(contenu.nomCommercial());
            if (contenu.mobileCommercial() != null) {
                texte.append(" — ").append(contenu.mobileCommercial());
            }
            texte.append('\n');
        }
        texte.append("\nMerci de vous munir d'une pièce d'identité. Prévoyez environ une heure ")
                .append("sur place.\n\nPour modifier ou annuler, contactez votre conseiller.");
        return texte.toString();
    }

    // ------------------------------------------------------------------ pièces jointes

    /**
     * R6 — les plans de l'article s'il est connu, sinon ceux du projet puis de la tranche. On
     * s'arrête au premier niveau qui donne quelque chose : cumuler enverrait au prospect le plan
     * de son lot <i>et</i> celui d'un autre.
     *
     * <p>L'article n'est renseigné que sur 3,4 % des prises de rendez-vous ; le repli est donc le
     * cas courant, et il est aujourd'hui vide — voir §7 de la spec, qui décrit ce qu'il reste à
     * téléverser pour que ce chemin produise enfin quelque chose.
     */
    private List<PieceJointeEmail> plans(ContenuVisite contenu) {
        StkItem article = contenu.prospection() != null ? contenu.prospection().getStkItem() : null;
        if (article != null) {
            List<PieceJointeEmail> depuisArticle = plansDe(article.getId());
            if (!depuisArticle.isEmpty()) {
                return depuisArticle;
            }
        }
        if (contenu.projet() != null) {
            List<PieceJointeEmail> depuisProjet = plansDe(contenu.projet().getId());
            if (!depuisProjet.isEmpty()) {
                return depuisProjet;
            }
        }
        return contenu.tranche() != null ? plansDe(contenu.tranche().getId()) : List.of();
    }

    /**
     * Les pièces d'un porteur qui sont des plans réellement téléchargeables, dans la limite du
     * plafond de taille (R7).
     */
    private List<PieceJointeEmail> plansDe(UUID porteurId) {
        List<PieceJointeEmail> retenues = new ArrayList<>();
        long cumul = 0;
        long plafond = properties.piecesJointesMaxOctets();

        for (AdmAttachedFileSummary piece : attachedFileSummaryRepository.findByCategoryId(porteurId)) {
            if (!Boolean.TRUE.equals(piece.getHasFile()) || !estUnPlan(piece)) {
                continue;
            }
            Optional<AppFileObject> objet =
                    fileObjectRepository.findByAdmAttachedFileIdAndDeletedAtIsNull(piece.getId());
            if (objet.isEmpty()) {
                continue;
            }
            long taille = objet.get().getSizeBytes() != null ? objet.get().getSizeBytes() : 0L;
            if (cumul + taille > plafond) {
                log.info("Plan « {} » écarté de la confirmation de visite : {} octets porteraient "
                                + "le total au-delà du plafond de {} Mo.",
                        piece.getDescription(), taille, properties.getPiecesJointesMaxMo());
                continue;
            }

            lire(objet.get(), piece).ifPresent(retenues::add);
            cumul += taille;
        }
        return retenues;
    }

    /**
     * R8 — un plan illisible ne doit pas priver le client de l'heure de son rendez-vous. Le
     * stockage est un service distant, hors de la transaction et hors de notre contrôle ; qu'il
     * soit indisponible est une éventualité normale, pas un cas exceptionnel.
     */
    private Optional<PieceJointeEmail> lire(AppFileObject objet, AdmAttachedFileSummary piece) {
        try {
            byte[] contenu = stockage.lire(objet.getStorageKey());
            String nom = PieceJointeSupport.nomTelechargement(piece.getDescription(), piece.getExtension());
            return Optional.of(new PieceJointeEmail(nom, objet.getContentType(), contenu));
        } catch (RuntimeException e) {
            log.error("Plan « {} » (clé {}) illisible : la confirmation de visite part sans lui.",
                    piece.getDescription(), objet.getStorageKey(), e);
            return Optional.empty();
        }
    }

    private static boolean estUnPlan(AdmAttachedFileSummary piece) {
        String code = Champs.de(piece.getType(), AdmAttachedFileType::getCode);
        return code != null && code.trim().toUpperCase(Locale.ROOT).startsWith(PREFIXE_TYPE_PLAN);
    }

    // ------------------------------------------------------------------ divers

    /**
     * Alternative texte brut du mail, construite en Java comme celle du mot de passe temporaire —
     * un second résolveur Thymeleaf en mode TEXT serait beaucoup de plomberie pour quinze lignes.
     */
    private static String corpsTexte(ContenuVisite contenu, boolean avecPlans) {
        StringBuilder texte = new StringBuilder();
        texte.append("Bonjour");
        if (contenu.nomProspect() != null) {
            texte.append(' ').append(contenu.nomProspect());
        }
        texte.append(",\n\nNous avons le plaisir de confirmer votre visite.\n\n");
        texte.append("Date : ").append(contenu.date()).append(" à ").append(contenu.heure()).append('\n');
        if (contenu.lieu() != null) {
            texte.append("Projet : ").append(contenu.lieu()).append('\n');
        }
        if (contenu.adresse() != null) {
            texte.append("Adresse : ").append(contenu.adresse());
            if (contenu.ville() != null) {
                texte.append(", ").append(contenu.ville());
            }
            texte.append('\n');
        }
        if (contenu.nomCommercial() != null) {
            texte.append("Votre conseiller : ").append(contenu.nomCommercial());
            if (contenu.mobileCommercial() != null) {
                texte.append(" — ").append(contenu.mobileCommercial());
            }
            texte.append('\n');
        }
        texte.append("\nMerci de vous munir d'une pièce d'identité. Prévoyez environ une heure ")
                .append("sur place.\n");
        if (avecPlans) {
            texte.append("\nVous trouverez le ou les plans en pièce jointe.\n");
        }
        texte.append("\nPour modifier ou annuler ce rendez-vous, contactez votre conseiller au ")
                .append("numéro ci-dessus.\n");
        return texte.toString();
    }

    /**
     * Une valeur utilisable, ou {@code null}. Les colonnes legacy contiennent aussi bien
     * {@code NULL} que des chaînes d'espaces — les deux doivent produire la même absence, sans
     * quoi une chaîne vide traverserait le template et laisserait un libellé sans valeur en face.
     */
    private static String nettoyer(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }

    /** Adresse retenue, déjà validée et déjà mise en forme sur deux lignes. */
    private record Adresse(String ligne, String ville) {
    }

    /**
     * Le contenu de la confirmation, résolu une fois pour tous les canaux.
     *
     * <p>Porte aussi les trois entités dont dépend la résolution des pièces jointes
     * ({@code prospection}, {@code projet}, {@code tranche}) : elles ne sont traversées que si le
     * canal email aboutit, et les garder ici évite de refaire la remontée
     * prospection → tranche → projet une seconde fois.
     *
     * @param aUnMobileBrut la fiche porte un mobile, exploitable ou non — permet de distinguer
     *                      « pas de numéro » de « numéro impossible à composer »
     */
    private record ContenuVisite(
            ComProspection prospection,
            CfgProject projet,
            CfgTranche tranche,
            String nomProspect,
            String email,
            String numeroWhatsApp,
            boolean aUnMobileBrut,
            String date,
            String heure,
            String lieu,
            String adresse,
            String ville,
            String nomCommercial,
            String mobileCommercial) {
    }
}
