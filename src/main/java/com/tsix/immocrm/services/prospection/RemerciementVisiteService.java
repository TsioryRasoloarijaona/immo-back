package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.domain.MotifEnvoiVisite;
import com.tsix.immocrm.dto.prospection.CanalEnvoiDto;
import com.tsix.immocrm.dto.prospection.ResultatEnvoiVisiteDto;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.mail.EnvoiEmail;
import com.tsix.immocrm.mail.MessageEmail;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Remercie le prospect après sa visite, sur les deux canaux. Voir
 * {@code instructions/FEATURE-offre-apres-visite.md} §3.
 *
 * <p><b>Volontairement pauvre.</b> Le remerciement ne porte ni plan, ni offre, ni adresse — la
 * visite a eu lieu, ces informations n'ont plus d'objet. C'est ce dénuement qui lui permet d'être
 * automatique : il ne dépend d'aucune donnée susceptible de manquer, là où l'offre de prix dépend
 * d'un lot inconnu sur 93 % des prospections de cette étape.
 *
 * <p>La résolution du destinataire est celle de {@link ConfirmationVisiteService} — mêmes règles,
 * même absence de repli sur {@code TempTel}. Elle n'est pas réécrite mais réempruntée, de sorte
 * qu'un prospect joignable pour sa convocation le reste pour son remerciement.
 */
@Service
@Transactional(readOnly = true)
public class RemerciementVisiteService {

    private static final Logger log = LoggerFactory.getLogger(RemerciementVisiteService.class);

    private static final String TEMPLATE = "mail/remerciement-visite";

    private final ComProspectionRepository prospectionRepository;
    private final EnvoiEmail envoiEmail;
    private final EnvoiWhatsApp envoiWhatsApp;

    public RemerciementVisiteService(ComProspectionRepository prospectionRepository,
                                     EnvoiEmail envoiEmail,
                                     EnvoiWhatsApp envoiWhatsApp) {
        this.prospectionRepository = prospectionRepository;
        this.envoiEmail = envoiEmail;
        this.envoiWhatsApp = envoiWhatsApp;
    }

    /** Chemin de l'écouteur : la prospection vient de passer à l'étape « 4 APRES VISITES ». */
    public ResultatEnvoiVisiteDto envoyer(UUID prospectionId) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));

        Contenu contenu = resoudre(prospection);
        return ResultatEnvoiVisiteDto.de(parEmail(contenu, prospectionId), parWhatsApp(contenu, prospectionId));
    }

    private static Contenu resoudre(ComProspection prospection) {
        CfgTier prospect = prospection.getCfgProspect();
        CfgTranche tranche = prospection.getStkHierarchy();
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);
        CfgTier commercial = prospection.getCfgCommercial();

        String email = ProspectionService.emailProspect(prospect, prospection);
        String emailPropre = email != null && email.trim().contains("@") ? email.trim() : null;

        return new Contenu(
                TierNomFormatter.nom(prospect),
                emailPropre,
                prospect != null
                        ? NumeroWhatsApp.international(prospect.getMobileIndicatif(), prospect.getMobile())
                        : null,
                prospect != null && StringUtils.hasText(prospect.getMobile()),
                lieu(projet, tranche),
                commercial != null ? TierNomFormatter.nom(commercial) : null,
                commercial != null && StringUtils.hasText(commercial.getMobile())
                        ? commercial.getMobile().trim() : null);
    }

    /** Le projet, précisé de sa tranche lorsqu'elle porte un libellé différent — règle R3. */
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

    private CanalEnvoiDto parEmail(Contenu contenu, UUID prospectionId) {
        if (contenu.email() == null) {
            log.info("Remerciement non envoyé par email pour la prospection {} : aucune adresse "
                    + "exploitable.", prospectionId);
            return CanalEnvoiDto.nonEnvoye(MotifEnvoiVisite.DESTINATAIRE_ABSENT, null);
        }
        if (!envoiEmail.estActif()) {
            return CanalEnvoiDto.nonEnvoye(MotifEnvoiVisite.SERVICE_INACTIF, contenu.email());
        }

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nomProspect", contenu.nomProspect());
        variables.put("lieu", contenu.lieu());
        variables.put("nomCommercial", contenu.nomCommercial());
        variables.put("mobileCommercial", contenu.mobileCommercial());

        envoiEmail.envoyer(new MessageEmail(contenu.email(), sujet(contenu), TEMPLATE,
                variables, corpsTexte(contenu)));

        return CanalEnvoiDto.envoye(contenu.email(), 0);
    }

    private CanalEnvoiDto parWhatsApp(Contenu contenu, UUID prospectionId) {
        if (contenu.numeroWhatsApp() == null) {
            MotifEnvoiVisite motif = contenu.aUnMobileBrut()
                    ? MotifEnvoiVisite.NUMERO_INEXPLOITABLE
                    : MotifEnvoiVisite.DESTINATAIRE_ABSENT;
            log.info("Remerciement non envoyé par WhatsApp pour la prospection {} : {}.",
                    prospectionId, motif);
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

    private static String sujet(Contenu contenu) {
        return contenu.lieu() != null
                ? "Merci de votre visite — " + contenu.lieu()
                : "Merci de votre visite";
    }

    private static String texteWhatsApp(Contenu contenu) {
        StringBuilder texte = new StringBuilder("Bonjour");
        if (contenu.nomProspect() != null) {
            texte.append(' ').append(contenu.nomProspect());
        }
        texte.append(",\n\nMerci d'avoir pris le temps de visiter");
        texte.append(contenu.lieu() != null ? " *" + contenu.lieu() + "*." : " notre projet.");
        texte.append("\n\nNous espérons que cette visite a répondu à vos attentes.\n");
        if (contenu.nomCommercial() != null) {
            texte.append("\n*Votre conseiller* : ").append(contenu.nomCommercial());
            if (contenu.mobileCommercial() != null) {
                texte.append(" — ").append(contenu.mobileCommercial());
            }
            texte.append('\n');
        }
        texte.append("\nN'hésitez pas à nous écrire pour toute question.");
        return texte.toString();
    }

    /** Alternative texte brut, construite en Java comme celle de la confirmation de visite. */
    private static String corpsTexte(Contenu contenu) {
        StringBuilder texte = new StringBuilder("Bonjour");
        if (contenu.nomProspect() != null) {
            texte.append(' ').append(contenu.nomProspect());
        }
        texte.append(",\n\nMerci d'avoir pris le temps de visiter ");
        texte.append(contenu.lieu() != null ? contenu.lieu() : "notre projet");
        texte.append(".\n\nNous espérons que cette visite a répondu à vos attentes.\n");
        if (contenu.nomCommercial() != null) {
            texte.append("\nVotre conseiller : ").append(contenu.nomCommercial());
            if (contenu.mobileCommercial() != null) {
                texte.append(" — ").append(contenu.mobileCommercial());
            }
            texte.append('\n');
        }
        texte.append("\nN'hésitez pas à nous écrire pour toute question.\n");
        return texte.toString();
    }

    private static String nettoyer(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }

    /**
     * @param aUnMobileBrut la fiche porte un mobile, exploitable ou non — distingue « pas de
     *                      numéro » de « numéro impossible à composer », qui n'appellent pas la
     *                      même correction.
     */
    private record Contenu(
            String nomProspect,
            String email,
            String numeroWhatsApp,
            boolean aUnMobileBrut,
            String lieu,
            String nomCommercial,
            String mobileCommercial) {
    }
}
