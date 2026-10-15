package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.DisponibiliteArticle;
import com.tsix.immocrm.domain.SchemaFinancementSaisi;
import com.tsix.immocrm.dto.prospection.DossierLieDto;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.entities.stl.StlQuoteDocument;
import com.tsix.immocrm.entities.stl.StlQuoteItem;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.OffreDejaTransformeeException;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteDocumentRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteItemRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Offre d'origine d'un dossier ({@code ComFolder.StlQuoteId}) et contrôles de {@code GenerateComFolderFromQuote}
 * d'OMRAN ({@code Yes.Stl.Credit/Quote.cs}), tous avant la première écriture. Voir FEATURE-offre-vers-dossier.md B1.
 */
@Component
public class OffreOrigineDossier {

    static final String SANS_ARTICLE = "Vous n'avez pas sélectionné un article !";
    static final String ARTICLE_INDISPONIBLE = "Cet article n'est pas disponible en stock !";
    static final String VOCATION_DIFFERENTE = "Cet article n'a pas la vocation correspondante !";
    static final String PROSPECTION_DEJA_VENDUE = "Un dossier de vente est déjà généré sur cette prospection !";

    private final StlQuoteRepository quoteRepository;
    private final StlQuoteItemRepository quoteItemRepository;
    private final StlQuoteDocumentRepository schemaRepository;
    private final ComFolderRepository folderRepository;
    private final PerimetreCommercialService perimetreService;

    public OffreOrigineDossier(StlQuoteRepository quoteRepository, StlQuoteItemRepository quoteItemRepository,
                               StlQuoteDocumentRepository schemaRepository, ComFolderRepository folderRepository,
                               PerimetreCommercialService perimetreService) {
        this.quoteRepository = quoteRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.schemaRepository = schemaRepository;
        this.folderRepository = folderRepository;
        this.perimetreService = perimetreService;
    }

    /** Offre retenue, ses sous-articles et son schéma saisi ; le dossier en reprend prix et échéancier. */
    public record OffreRetenue(StlQuote offre, List<StlQuoteItem> sousArticles, List<StlQuoteDocument> schema) {

        public PrixSousArticles prixSousArticles() {
            return PrixSousArticles.deLOffre(sousArticles);
        }
    }

    /**
     * Vide sans offre. 400 : offre inconnue, d'une autre prospection, premier article autre que celui de l'offre, ou
     * contrôles 1 à 4 d'OMRAN ; 403 : article hors
     * périmètre ; 409 : offre déjà transformée (avant les contrôles : son article est forcément occupé) ou contrôle 5.
     */
    public Optional<OffreRetenue> verifier(UUID offreId, ComProspection prospection, UUID articlePrincipalId) {
        if (offreId == null) {
            return Optional.empty();
        }
        StlQuote offre = quoteRepository.findById(offreId)
                .orElseThrow(() -> new InvalidRequestException("Offre introuvable: " + offreId));
        if (offre.getComProspection() == null || !prospection.getId().equals(offre.getComProspection().getId())) {
            throw new InvalidRequestException("L'offre " + offre.getCode() + " n'appartient pas à cette prospection");
        }
        StkItem article = offre.getStkItem();
        if (article == null) {
            throw new InvalidRequestException(SANS_ARTICLE);
        }
        // Avant tout message qui nomme l'article ou un dossier : hors périmètre, l'appelant n'en apprend rien.
        perimetreService.verifierEcritureSurTranche(article.getStkHierarchy(),
                "Création de dossier depuis l'offre " + offre.getCode());
        // OMRAN crée toujours le dossier sur l'article de l'offre (val.stkItemId = stkItemId).
        if (!article.getId().equals(articlePrincipalId)) {
            throw new InvalidRequestException("Le dossier doit porter l'article de l'offre " + offre.getCode()
                    + " (" + article.getCode() + ") en premier article");
        }
        dejaTransformee(offre);

        List<StlQuoteItem> sousArticles = quoteItemRepository.findByStlQuoteIdOrderByCodeAsc(offre.getId());
        verifierDisponibilite(article, sousArticles);
        if (offre.getStkVocation() == null || article.getStkVocation() == null
                || !Objects.equals(article.getStkVocation().getId(), offre.getStkVocation().getId())) {
            throw new InvalidRequestException(VOCATION_DIFFERENTE);
        }
        if (folderRepository.existsByComProspectionId(prospection.getId())) {
            throw new ConflictException(PROSPECTION_DEJA_VENDUE);
        }
        return Optional.of(new OffreRetenue(offre, sousArticles,
                schemaRepository.findByStlQuoteIdOrderByCodeAsc(offre.getId())));
    }

    /**
     * {@code Sale.cs:VerifSchema} : l'échéancier repris de l'offre doit égaler le prix après remise du dossier, calculé
     * depuis la requête ; sinon 400, avant toute écriture. Sans schéma saisi sur l'offre, rien à vérifier.
     */
    public void verifierEcheancier(OffreRetenue offre, BigDecimal prixApresRemise) {
        if (offre.schema().isEmpty()) {
            return;
        }
        // Prix absent = 0, comme SchemaFinancementSaisi.ecart.
        BigDecimal prixDossier = Objects.requireNonNullElse(prixApresRemise, BigDecimal.ZERO);
        BigDecimal ecart = SchemaFinancementSaisi.ecart(prixDossier, offre.schema());
        if (ecart.signum() != 0) {
            BigDecimal finance = SchemaFinancementSaisi.montantFinance(offre.schema()).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal prix = prixDossier.setScale(2, RoundingMode.HALF_EVEN);
            throw new InvalidRequestException("L'échéancier repris de l'offre " + offre.offre().getCode() + " ("
                    + finance.toPlainString() + " DH) ne correspond plus au prix du dossier (" + prix.toPlainString()
                    + " DH) : gardez le prix de l'offre ou créez le dossier sans l'offre");
        }
    }

    private void dejaTransformee(StlQuote offre) {
        List<Object[]> dossiers = folderRepository.dossiersDeLOffre(offre.getId());
        if (!dossiers.isEmpty()) {
            Object[] premier = dossiers.get(0);
            throw new OffreDejaTransformeeException(offre.getCode(),
                    new DossierLieDto((UUID) premier[0], (String) premier[1], false));
        }
    }

    /** Contrôles 2 et 3 : l'article puis chaque sous-article de l'offre, libres ({@link DisponibiliteArticle}). */
    private static void verifierDisponibilite(StkItem article, List<StlQuoteItem> sousArticles) {
        if (DisponibiliteArticle.estOccupe(article)) {
            throw new InvalidRequestException(ARTICLE_INDISPONIBLE);
        }
        for (StlQuoteItem ligne : sousArticles) {
            if (ligne.getStkItem() != null && DisponibiliteArticle.estOccupe(ligne.getStkItem())) {
                throw new InvalidRequestException("Le sous article " + ligne.getCode() + " n'est pas libre !");
            }
        }
    }
}
