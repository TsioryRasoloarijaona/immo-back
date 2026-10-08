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
 * Confirmation de visite (email et WhatsApp, tentés indépendamment), résolue une fois pour les
 * deux canaux et les deux déclencheurs. Voir {@code instructions/FEATURE-mail-confirmation-visite.md}.
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

    // R4 : calibrée pour rejeter le projet YESTEST, dont l'adresse est littéralement « adresse ».
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

    /** Chemin de l'écouteur, après commit. */
    public ResultatEnvoiVisiteDto envoyer(UUID interactionId) {
        ComActionMessage interaction = actionMessageRepository.findById(interactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Interaction introuvable: " + interactionId));
        return envoyerPour(interaction);
    }

    /**
     * Périmètre déjà couvert par {@code PerimetreInterceptor} ; l'appartenance de l'interaction à
     * la prospection se vérifie ici, sinon on ferait envoyer le rendez-vous d'autrui.
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
            // 409 et non 400 : la requête est bien formée, c'est l'état de l'interaction qui s'y oppose.
            throw new ConflictException(
                    "Cette interaction ne planifie aucun rendez-vous : rien à confirmer.");
        }
        return envoyerPour(interaction);
    }

    /** Ne lève jamais pour un motif métier : un prospect sans adresse est une issue, pas une erreur. */
    private ResultatEnvoiVisiteDto envoyerPour(ComActionMessage interaction) {
        AdmEvent rendezVous = interaction.getAdmEvent();
        if (rendezVous == null || rendezVous.getStartHour() == null) {
            // Jamais en pratique (l'événement suppose un AdmEvent), mais évite un NPE illisible
            // dans un fil asynchrone.
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
     * R4 bis — lien de recherche sur l'adresse retenue, aucune coordonnée GPS n'existant en base ;
     * il suit l'adresse validée, ou n'existe pas. Voir {@code FEATURE-mail-visite-localisation.md}.
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
     * R1 — fiche, puis champ du premier contact. Un {@code @} suffit : une regex complète rejette
     * des adresses valides, mais des valeurs réelles sont des numéros de téléphone.
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
     * Pas de repli sur {@code ComProspection.TempTel}, contrairement à l'email : sans indicatif, il
     * faudrait supposer le Maroc. Voir {@code FEATURE-mail-confirmation-visite.md} §4 bis W2.
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
     * R4 — tranche (plus précise), puis projet (toujours renseigné). Code postal et ville suivent
     * le porteur retenu : les panacher fabriquerait une adresse qui n'existe nulle part.
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
     * Volontairement étroit : rejette les deux fausses adresses réelles connues (trop courte,
     * ou égale au libellé du porteur comme sur {@code MBR}). Dans le doute, omettre le bloc.
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
        String ligneVille;
        if (cp != null && cite != null) {
            ligneVille = cp + " " + cite;
        } else {
            ligneVille = cite != null ? cite : cp;
        }
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

        // Aucune pièce jointe, par choix : voir FEATURE-mail-visite-localisation.md.
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
            // Distingue « obtenir un numéro » de « réparer un numéro » pour le commercial.
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

    /** Mêmes informations que le mail, plus court ; {@code *…*} est le gras de WhatsApp. */
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

    /** Texte brut construit en Java : un second résolveur Thymeleaf (mode TEXT) n'en vaut pas la peine. */
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

    /** Les colonnes legacy portent {@code NULL} ou des espaces : les deux deviennent {@code null}. */
    private static String nettoyer(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }

    private record Adresse(String ligne, String ville) {
    }

    /** @param aUnMobileBrut la fiche porte un mobile, exploitable ou non */
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
