package com.tsix.immocrm.services.prospection;

import com.tsix.immocrm.domain.MotifEnvoiVisite;
import com.tsix.immocrm.dto.prospection.CanalEnvoiDto;
import com.tsix.immocrm.dto.prospection.CreerDevisRequest;
import com.tsix.immocrm.dto.prospection.DevisDto;
import com.tsix.immocrm.dto.prospection.EnvoyerOffrePrixRequest;
import com.tsix.immocrm.dto.prospection.ResultatEnvoiOffreDto;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.mail.EnvoiEmail;
import com.tsix.immocrm.mail.MessageEmail;
import com.tsix.immocrm.mail.PieceJointeEmail;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import com.tsix.immocrm.services.prospection.document.MontantFormatter;
import com.tsix.immocrm.services.prospection.document.OffreDevisPdfRenderer;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import com.tsix.immocrm.whatsapp.EnvoiWhatsApp;
import com.tsix.immocrm.whatsapp.MessageWhatsApp;
import com.tsix.immocrm.whatsapp.NumeroWhatsApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Crée l'offre de prix d'une prospection puis l'expédie au prospect. Voir
 * {@code instructions/FEATURE-offre-apres-visite.md} §4 et §5.
 *
 * <p><b>Pourquoi cet envoi est explicite et non automatique.</b> Le prix vient du lot
 * ({@code StkItem.ComSalePrice}), et le lot est inconnu sur <b>93 %</b> des prospections à l'étape
 * « 4 APRES VISITES » (94 sur 1 266). Sans lot il n'y a aucun prix à annoncer : un déclenchement
 * automatique ne produirait rien pour neuf prospections sur dix, sans que personne le sache. Le
 * remerciement, lui, ne dépend d'aucune donnée et reste automatique — voir
 * {@link RemerciementVisiteService}.
 *
 * <p><b>Le calcul des montants n'est pas réécrit ici.</b> Il appartient entièrement à
 * {@link DevisService}, y compris la numérotation legacy par {@code AdmSequence}. Ce service
 * décide seulement <i>quand</i> créer une offre et <i>comment</i> l'acheminer.
 */
@Service
public class OffrePrixService {

    private static final Logger log = LoggerFactory.getLogger(OffrePrixService.class);

    private static final String TEMPLATE = "mail/offre-prix";
    private static final String TYPE_PDF = "application/pdf";

    private final ComProspectionRepository prospectionRepository;
    private final StlQuoteRepository quoteRepository;
    private final DevisService devisService;
    private final OffreDevisDocumentService documentService;
    private final OffreDevisPdfRenderer pdfRenderer;
    private final EnvoiEmail envoiEmail;
    private final EnvoiWhatsApp envoiWhatsApp;
    private final TransactionTemplate transactions;

    public OffrePrixService(ComProspectionRepository prospectionRepository,
                            StlQuoteRepository quoteRepository,
                            DevisService devisService,
                            OffreDevisDocumentService documentService,
                            OffreDevisPdfRenderer pdfRenderer,
                            EnvoiEmail envoiEmail,
                            EnvoiWhatsApp envoiWhatsApp,
                            PlatformTransactionManager transactionManager) {
        this.prospectionRepository = prospectionRepository;
        this.quoteRepository = quoteRepository;
        this.devisService = devisService;
        this.documentService = documentService;
        this.pdfRenderer = pdfRenderer;
        this.envoiEmail = envoiEmail;
        this.envoiWhatsApp = envoiWhatsApp;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /**
     * {@code POST /api/prospections/{id}/offre-prix}.
     *
     * <p><b>La transaction est bornée à la création de l'offre</b>, volontairement, et n'englobe
     * pas les envois. Un {@code @Transactional} sur cette méthode tiendrait une transaction en
     * écriture ouverte pendant un appel SMTP et un appel HTTP à UltraMsg — deux services distants
     * dont la latence n'a rien à faire dans une transaction de base. Le contrôle de périmètre
     * commercial n'est pas refait : {@code PerimetreInterceptor} couvre déjà
     * {@code /api/prospections/{id}/**} et rend 404 hors périmètre.
     */
    public ResultatEnvoiOffreDto envoyer(UUID prospectionId, EnvoyerOffrePrixRequest request) {
        Preparation preparation = transactions.execute(statut -> preparer(prospectionId, request));

        byte[] pdf = rendre(preparation.offreId(), prospectionId);

        CanalEnvoiDto email = parEmail(preparation, pdf);
        CanalEnvoiDto whatsapp = parWhatsApp(preparation, email.envoye());

        return ResultatEnvoiOffreDto.de(preparation.offreId(), preparation.offreCode(),
                preparation.reutilisee(), preparation.prixFinalTTC(), email, whatsapp);
    }

    // ------------------------------------------------------------------ préparation (transaction)

    /**
     * Résout l'état, crée ou réutilise l'offre, et fige tout ce dont les envois auront besoin.
     * Rien de paresseux ne franchit cette frontière : les envois travaillent sur des valeurs.
     */
    private Preparation preparer(UUID prospectionId, EnvoyerOffrePrixRequest request) {
        ComProspection prospection = prospectionRepository.findById(prospectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prospection introuvable: " + prospectionId));

        exigerEtapeApresVisite(prospection);
        StkItem article = exigerArticle(prospection);

        boolean conditionsFournies = request != null
                && (estRenseigne(request.remisePercent()) || estRenseigne(request.prix()));

        // R : une offre déjà émise n'est réutilisée que si l'appel n'impose aucune condition
        // nouvelle. Réécrire ses montants modifierait un document que le prospect détient déjà.
        Optional<StlQuote> existante = conditionsFournies
                ? Optional.empty()
                : quoteRepository.findFirstByComProspectionIdOrderByDocDateDesc(prospectionId);

        UUID offreId;
        String offreCode;
        BigDecimal prixFinal;
        boolean reutilisee = existante.isPresent();

        if (existante.isPresent()) {
            StlQuote offre = existante.get();
            offreId = offre.getId();
            offreCode = offre.getCode();
            prixFinal = offre.getPriceAfterDiscount();
        } else {
            DevisDto cree = devisService.creerDevis(prospectionId, new CreerDevisRequest(
                    prospection.getStkHierarchy() != null ? prospection.getStkHierarchy().getId() : null,
                    article.getId(),
                    null,
                    null,
                    null,
                    request != null ? request.modePaiementId() : null,
                    request != null ? request.prix() : null,
                    request != null ? request.remisePercent() : null,
                    request != null ? request.notes() : null));
            offreId = cree.id();
            offreCode = cree.code();
            prixFinal = cree.montants() != null ? cree.montants().prixVenteTTC() : null;
        }

        CfgTier prospect = prospection.getCfgProspect();
        CfgTranche tranche = prospection.getStkHierarchy();
        CfgProject projet = Champs.de(tranche, CfgTranche::getCfgProject);
        CfgTier commercial = prospection.getCfgCommercial();

        String email = ProspectionService.emailProspect(prospect, prospection);
        String emailPropre = email != null && email.trim().contains("@") ? email.trim() : null;

        return new Preparation(
                offreId, offreCode, reutilisee, prixFinal,
                request != null ? request.remisePercent() : null,
                TierNomFormatter.nom(prospect),
                emailPropre,
                prospect != null
                        ? NumeroWhatsApp.international(prospect.getMobileIndicatif(), prospect.getMobile())
                        : null,
                prospect != null && StringUtils.hasText(prospect.getMobile()),
                lieu(projet, tranche),
                nettoyer(article.getCode()),
                article.getArea(),
                commercial != null ? TierNomFormatter.nom(commercial) : null,
                commercial != null && StringUtils.hasText(commercial.getMobile())
                        ? commercial.getMobile().trim() : null);
    }

    /**
     * R6 — l'offre de prix suit la visite, elle ne la précède pas. 409 et non 400 : la requête est
     * bien formée, c'est l'état de la prospection qui s'y oppose — même lecture que le refus d'une
     * transition de dossier.
     */
    private static void exigerEtapeApresVisite(ComProspection prospection) {
        ComProspectionKind kind = prospection.getComProspectionKind();
        String code = kind != null && kind.getCode() != null ? kind.getCode().trim() : null;
        if (!ProspectionService.ETAPE_APRES_VISITE.equalsIgnoreCase(code)) {
            throw new ConflictException("L'offre de prix ne peut être envoyée qu'après la visite : "
                    + "cette prospection est à l'étape « " + (code != null ? code : "non renseignée")
                    + " » et non « " + ProspectionService.ETAPE_APRES_VISITE + " ».");
        }
    }

    /**
     * R5 — pas de lot, pas d'offre. Le message nomme la sortie, parce que c'est le cas majoritaire
     * attendu (93 % des prospections de l'étape) et qu'un 409 sans mode d'emploi y serait vécu
     * comme une panne.
     */
    private static StkItem exigerArticle(ComProspection prospection) {
        StkItem article = prospection.getStkItem();
        if (article == null) {
            throw new ConflictException("Cette prospection n'a pas de lot : impossible d'établir un "
                    + "prix. Rattachez l'article avec PATCH /api/prospections/{id}/tranche-principale, "
                    + "puis relancez l'envoi.");
        }
        return article;
    }

    // ------------------------------------------------------------------ document

    /**
     * R8 — un PDF illisible ne doit pas priver le prospect de son prix. Le WhatsApp porte le
     * montant en texte et reste tenté ; le mail partira sans pièce jointe.
     */
    private byte[] rendre(UUID offreId, UUID prospectionId) {
        try {
            return pdfRenderer.render(documentService.collect(prospectionId, offreId));
        } catch (RuntimeException e) {
            log.error("Fiche Offre {} illisible : l'offre part sans document.", offreId, e);
            return null;
        }
    }

    /**
     * Le numéro d'offre porte une barre oblique ({@code 26/00314}) : la reprendre telle quelle
     * fabriquerait un nom de fichier invalide, voire un chemin.
     */
    private static String nomFichier(String offreCode) {
        String propre = offreCode != null ? offreCode.replace('/', '-').replace('\\', '-') : "offre";
        return "Offre-" + propre + ".pdf";
    }

    // ------------------------------------------------------------------ canal email

    private CanalEnvoiDto parEmail(Preparation p, byte[] pdf) {
        if (p.email() == null) {
            log.info("Offre {} non envoyée par email : aucune adresse exploitable.", p.offreCode());
            return CanalEnvoiDto.nonEnvoye(MotifEnvoiVisite.DESTINATAIRE_ABSENT, null);
        }
        if (!envoiEmail.estActif()) {
            return CanalEnvoiDto.nonEnvoye(MotifEnvoiVisite.SERVICE_INACTIF, p.email());
        }

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("nomProspect", p.nomProspect());
        variables.put("lieu", p.lieu());
        variables.put("codeArticle", p.codeArticle());
        variables.put("surface", p.surface() != null ? MontantFormatter.surface(p.surface()) : null);
        variables.put("prixFinal", MontantFormatter.montant(p.prixFinalTTC()));
        variables.put("remise", estRenseigne(p.remisePercent())
                ? MontantFormatter.pourcentage(p.remisePercent()) : null);
        variables.put("offreCode", p.offreCode());
        variables.put("nomCommercial", p.nomCommercial());
        variables.put("mobileCommercial", p.mobileCommercial());
        variables.put("avecDocument", pdf != null);

        var pieces = pdf != null
                ? java.util.List.of(new PieceJointeEmail(nomFichier(p.offreCode()), TYPE_PDF, pdf))
                : java.util.List.<PieceJointeEmail>of();

        envoiEmail.envoyer(new MessageEmail(p.email(), sujet(p), TEMPLATE, variables,
                corpsTexte(p, pdf != null), pieces));

        return CanalEnvoiDto.envoye(p.email(), pieces.size());
    }

    private static String sujet(Preparation p) {
        return p.lieu() != null
                ? "Votre offre de prix — " + p.lieu()
                : "Votre offre de prix";
    }

    // ------------------------------------------------------------------ canal WhatsApp

    /**
     * <b>Le WhatsApp porte le montant en clair</b>, et c'est la décision centrale de ce chantier.
     * À l'étape « 4 APRES VISITES » le rapport s'inverse par rapport à la prise de rendez-vous :
     * <b>19,7 % d'email contre 99,7 % de WhatsApp</b> (249 et 1 262 sur 1 266). Se contenter
     * d'annoncer un mail priverait quatre prospects sur cinq de toute information — et pire, leur
     * ferait chercher un message qui n'existe pas.
     *
     * @param mailParti le mail n'est mentionné que s'il est <b>réellement</b> parti
     */
    private CanalEnvoiDto parWhatsApp(Preparation p, boolean mailParti) {
        if (p.numeroWhatsApp() == null) {
            MotifEnvoiVisite motif = p.aUnMobileBrut()
                    ? MotifEnvoiVisite.NUMERO_INEXPLOITABLE
                    : MotifEnvoiVisite.DESTINATAIRE_ABSENT;
            log.info("Offre {} non envoyée par WhatsApp : {}.", p.offreCode(), motif);
            return CanalEnvoiDto.nonEnvoye(motif, null);
        }
        if (!envoiWhatsApp.estActif()) {
            return CanalEnvoiDto.nonEnvoye(MotifEnvoiVisite.SERVICE_INACTIF, p.numeroWhatsApp());
        }

        boolean parti = envoiWhatsApp.envoyer(
                new MessageWhatsApp(p.numeroWhatsApp(), texteWhatsApp(p, mailParti)));

        return parti
                ? CanalEnvoiDto.envoye(p.numeroWhatsApp(), 0)
                : CanalEnvoiDto.nonEnvoye(MotifEnvoiVisite.ECHEC_ENVOI, p.numeroWhatsApp());
    }

    private static String texteWhatsApp(Preparation p, boolean mailParti) {
        StringBuilder texte = new StringBuilder("Bonjour");
        if (p.nomProspect() != null) {
            texte.append(' ').append(p.nomProspect());
        }
        texte.append(",\n\nSuite à votre visite, voici notre proposition.\n\n");
        if (p.lieu() != null) {
            texte.append("*Projet* : ").append(p.lieu()).append('\n');
        }
        if (p.codeArticle() != null) {
            texte.append("*Lot* : ").append(p.codeArticle());
            if (p.surface() != null) {
                texte.append(" — ").append(MontantFormatter.surface(p.surface())).append(" m²");
            }
            texte.append('\n');
        }
        if (estRenseigne(p.remisePercent())) {
            texte.append("*Remise* : ").append(MontantFormatter.pourcentage(p.remisePercent())).append('\n');
        }
        texte.append("*Prix* : ").append(MontantFormatter.montant(p.prixFinalTTC())).append(" TTC\n");
        texte.append("*Référence de l'offre* : ").append(p.offreCode()).append('\n');

        if (mailParti) {
            texte.append("\nLe détail complet vous a été envoyé par email, en pièce jointe.\n");
        }
        if (p.nomCommercial() != null) {
            texte.append("\n*Votre conseiller* : ").append(p.nomCommercial());
            if (p.mobileCommercial() != null) {
                texte.append(" — ").append(p.mobileCommercial());
            }
            texte.append('\n');
        }
        texte.append("\nCette proposition reste à votre disposition pour toute question.");
        return texte.toString();
    }

    // ------------------------------------------------------------------ divers

    private static String corpsTexte(Preparation p, boolean avecDocument) {
        StringBuilder texte = new StringBuilder("Bonjour");
        if (p.nomProspect() != null) {
            texte.append(' ').append(p.nomProspect());
        }
        texte.append(",\n\nSuite à votre visite, nous avons le plaisir de vous adresser notre "
                + "offre de prix.\n\n");
        if (p.lieu() != null) {
            texte.append("Projet : ").append(p.lieu()).append('\n');
        }
        if (p.codeArticle() != null) {
            texte.append("Lot : ").append(p.codeArticle());
            if (p.surface() != null) {
                texte.append(" — ").append(MontantFormatter.surface(p.surface())).append(" m²");
            }
            texte.append('\n');
        }
        if (estRenseigne(p.remisePercent())) {
            texte.append("Remise : ").append(MontantFormatter.pourcentage(p.remisePercent())).append('\n');
        }
        texte.append("Prix : ").append(MontantFormatter.montant(p.prixFinalTTC())).append(" TTC\n");
        texte.append("Référence de l'offre : ").append(p.offreCode()).append('\n');
        if (avecDocument) {
            texte.append("\nLe détail figure dans la fiche jointe à ce message.\n");
        }
        texte.append("\nPour toute question, contactez votre conseiller");
        if (p.nomCommercial() != null) {
            texte.append(" : ").append(p.nomCommercial());
            if (p.mobileCommercial() != null) {
                texte.append(" — ").append(p.mobileCommercial());
            }
        }
        texte.append(".\n");
        return texte.toString();
    }

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

    private static boolean estRenseigne(BigDecimal valeur) {
        return valeur != null && valeur.signum() != 0;
    }

    private static String nettoyer(String valeur) {
        return StringUtils.hasText(valeur) ? valeur.trim() : null;
    }

    /**
     * Tout ce dont les envois ont besoin, figé pendant la transaction. Aucune entité JPA ne
     * franchit cette frontière : les canaux travaillent sur des valeurs, hors session Hibernate.
     */
    private record Preparation(
            UUID offreId,
            String offreCode,
            boolean reutilisee,
            BigDecimal prixFinalTTC,
            BigDecimal remisePercent,
            String nomProspect,
            String email,
            String numeroWhatsApp,
            boolean aUnMobileBrut,
            String lieu,
            String codeArticle,
            BigDecimal surface,
            String nomCommercial,
            String mobileCommercial) {
    }
}
