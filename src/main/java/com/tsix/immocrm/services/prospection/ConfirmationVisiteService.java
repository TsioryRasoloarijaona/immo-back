package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.domain.MotifEnvoiVisite;
import com.tsix.immocrm.dto.prospection.CanalEnvoiDto;
import com.tsix.immocrm.dto.prospection.ResultatEnvoiVisiteDto;
import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.mail.EnvoiEmail;
import com.tsix.immocrm.mail.MessageEmail;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private final ComActionMessageRepository actionMessageRepository;
    private final EnvoiEmail envoiEmail;
    private final EnvoiWhatsApp envoiWhatsApp;

    public ConfirmationVisiteService(ComActionMessageRepository actionMessageRepository,
                                     EnvoiEmail envoiEmail,
                                     EnvoiWhatsApp envoiWhatsApp) {
        this.actionMessageRepository = actionMessageRepository;
        this.envoiEmail = envoiEmail;
        this.envoiWhatsApp = envoiWhatsApp;
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
                TierNomFormatter.nom(prospect),
                email(prospect, prospection),
                numeroWhatsApp(prospect),
                prospect != null && StringUtils.hasText(prospect.getMobile()),
                rendezVous.format(FORMAT_DATE),
                rendezVous.format(FORMAT_HEURE),
                lieu(projet, tranche),
                adresse != null ? adresse.ligne() : null,
                adresse != null ? adresse.ville() : null,
                lienCarte(adresse),
                commercial != null ? TierNomFormatter.nom(commercial) : null,
                commercial != null ? nettoyer(commercial.getMobile()) : null);
    }

    /**
     * R4 bis — la « localisation exacte », sous la seule forme que les données permettent :
     * un lien de recherche Google Maps construit sur l'adresse retenue. Aucune coordonnée GPS
     * n'existe en base ({@code MapLatitude}/{@code MapLongitude} : 0 ligne sur 1 068
     * {@code StkItem}, et les colonnes n'existent ni sur le projet ni sur la tranche) — le
     * jour où elles seront saisies, ce lien pourra devenir un {@code query=lat,long}.
     *
     * <p>Le lien suit l'adresse : validée par {@link #adresseValide} ou absente. Construire un
     * lien sur une adresse rejetée enverrait le prospect chercher « adresse » sur une carte.
     */
    private static String lienCarte(Adresse adresse) {
        if (adresse == null) {
            return null;
        }
        String requete = adresse.ville() != null
                ? adresse.ligne() + ", " + adresse.ville()
                : adresse.ligne();
        return "https://www.google.com/maps/search/?api=1&query="
                + URLEncoder.encode(requete, StandardCharsets.UTF_8);
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

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nomProspect", contenu.nomProspect());
        variables.put("date", contenu.date());
        variables.put("heure", contenu.heure());
        variables.put("lieu", contenu.lieu());
        variables.put("adresse", contenu.adresse());
        variables.put("ville", contenu.ville());
        variables.put("lienCarte", contenu.lienCarte());
        variables.put("nomCommercial", contenu.nomCommercial());
        variables.put("mobileCommercial", contenu.mobileCommercial());

        // Aucune pièce jointe, et c'est un choix : le message n'emporte plus de plan — il
        // porte l'adresse et sa localisation. Voir FEATURE-mail-visite-localisation.md.
        envoiEmail.envoyer(new MessageEmail(contenu.email(), sujet(contenu), TEMPLATE,
                variables, corpsTexte(contenu), List.of()));

        return CanalEnvoiDto.envoye(contenu.email(), 0);
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
     * WhatsApp interprète. Le lien de localisation y est particulièrement à sa place — le
     * message se lit sur le téléphone qui servira à s'y rendre.
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
        if (contenu.lienCarte() != null) {
            texte.append("*Localisation* : ").append(contenu.lienCarte()).append('\n');
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

    // ------------------------------------------------------------------ divers

    /**
     * Alternative texte brut du mail, construite en Java comme celle du mot de passe temporaire —
     * un second résolveur Thymeleaf en mode TEXT serait beaucoup de plomberie pour quinze lignes.
     */
    private static String corpsTexte(ContenuVisite contenu) {
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
        if (contenu.lienCarte() != null) {
            texte.append("Localisation : ").append(contenu.lienCarte()).append('\n');
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
     * @param aUnMobileBrut la fiche porte un mobile, exploitable ou non — permet de distinguer
     *                      « pas de numéro » de « numéro impossible à composer »
     * @param lienCarte     lien Google Maps sur l'adresse retenue — la « localisation exacte »,
     *                      dans la seule forme que les données permettent (aucun GPS en base)
     */
    private record ContenuVisite(
            String nomProspect,
            String email,
            String numeroWhatsApp,
            boolean aUnMobileBrut,
            String date,
            String heure,
            String lieu,
            String adresse,
            String ville,
            String lienCarte,
            String nomCommercial,
            String mobileCommercial) {
    }
}
