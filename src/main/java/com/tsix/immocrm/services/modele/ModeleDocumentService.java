package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.dto.modele.CatalogueChampsDto;
import com.tsix.immocrm.dto.modele.CategorieModeleDto;
import com.tsix.immocrm.dto.modele.CreerModeleDocumentRequest;
import com.tsix.immocrm.dto.modele.MiseEnPageDto;
import com.tsix.immocrm.dto.modele.ModeleDocumentAdminDto;
import com.tsix.immocrm.dto.modele.ModeleDocumentDetailDto;
import com.tsix.immocrm.dto.modele.ModeleDocumentResumeDto;
import com.tsix.immocrm.dto.modele.ModifierModeleDocumentRequest;
import com.tsix.immocrm.dto.modele.VersionModeleDto;
import com.tsix.immocrm.entities.app.AppModeleDocument;
import com.tsix.immocrm.entities.app.AppModeleDocumentVersion;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.app.AppModeleDocumentRepository;
import com.tsix.immocrm.repositories.app.AppModeleDocumentVersionRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Modèles de documents : catalogue, liste, versions (FEATURE-modeles-documents.md §6). Jamais d'écrasement. */
@Service
@Transactional(readOnly = true)
public class ModeleDocumentService {

    static final String AUTEUR_SYSTEME = "Système";
    private static final int ORDRE_DEFAUT = 100;
    private static final int CODE_MAX = 60;
    private static final int LIBELLE_MAX = 200;

    private final AppModeleDocumentRepository modeleRepository;
    private final AppModeleDocumentVersionRepository versionRepository;
    private final LectureModeles lecture;
    private final CataloguesModeles catalogues;
    private final MiseEnPageModele miseEnPageModele;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public ModeleDocumentService(AppModeleDocumentRepository modeleRepository,
                                 AppModeleDocumentVersionRepository versionRepository, LectureModeles lecture,
                                 CataloguesModeles catalogues, MiseEnPageModele miseEnPageModele,
                                 UtilisateurAffiche utilisateurAffiche, Clock clock) {
        this.modeleRepository = modeleRepository;
        this.versionRepository = versionRepository;
        this.lecture = lecture;
        this.catalogues = catalogues;
        this.miseEnPageModele = miseEnPageModele;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    public List<CategorieModeleDto> categories() {
        return catalogues.categories().stream().map(c -> new CategorieModeleDto(c.name(), c.libelle())).toList();
    }

    /** 404 (et non 400) : la catégorie est un segment de chemin. */
    public CatalogueChampsDto champs(String categorie) {
        CategorieModele connue = CategorieModele.depuisCode(categorie)
                .filter(c -> catalogues.categories().contains(c))
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie inconnue : " + categorie));
        return catalogues.de(connue.name()).catalogue().versDto(connue);
    }

    public List<ModeleDocumentResumeDto> listerActifs(String categorie) {
        catalogues.de(categorie);
        return modeleRepository.findByCategorieAndActifTrueOrderByOrdreAscLibelleAsc(categorie).stream()
                .map(m -> new ModeleDocumentResumeDto(m.getId(), m.getCode(), m.getLibelle()))
                .toList();
    }

    public List<ModeleDocumentAdminDto> listerTous(String categorie) {
        List<AppModeleDocument> modeles = categorie == null || categorie.isBlank()
                ? modeleRepository.findAll(Sort.by("categorie", "ordre", "libelle"))
                : modeleRepository.findByCategorieOrderByOrdreAscLibelleAsc(verifierCategorie(categorie));
        return modeles.stream().map(this::versAdmin).toList();
    }

    public ModeleDocumentDetailDto detail(UUID id) {
        return versDetail(trouver(id));
    }

    public List<VersionModeleDto> versions(UUID id) {
        AppModeleDocument modele = trouver(id);
        return versionRepository.findByModeleIdOrderByNumeroDesc(id).stream()
                .map(v -> new VersionModeleDto(v.getNumero(), v.getCreeLe(), v.getCreePar(),
                        v.getId().equals(modele.getVersionCouranteId())))
                .toList();
    }

    @Transactional
    public ModeleDocumentDetailDto creer(CreerModeleDocumentRequest request) {
        String categorie = verifierCategorie(request.categorie());
        String code = verifierCode(request.code());
        String libelle = verifierLibelle(request.libelle());
        if (modeleRepository.existsByCategorieAndCode(categorie, code)) {
            throw new ConflictException("Un modèle %s existe déjà dans la catégorie %s.".formatted(code, categorie));
        }
        String miseEnPageJson = miseEnPageValide(categorie, request.contenu(), request.miseEnPage());

        LocalDateTime maintenant = LocalDateTime.now(clock);
        String auteur = auteur();
        AppModeleDocument modele = new AppModeleDocument();
        modele.setId(UUID.randomUUID());
        modele.setCategorie(categorie);
        modele.setCode(code);
        modele.setLibelle(libelle);
        modele.setOrdre(request.ordre() != null ? request.ordre() : ORDRE_DEFAUT);
        modele.setActif(true);
        modele.setCreeLe(maintenant);
        modele.setCreePar(auteur);
        modele.setModifieLe(maintenant);
        modele.setModifiePar(auteur);
        // Id posé à la main : save() fait un merge et renvoie l'instance gérée, la seule à modifier ensuite.
        AppModeleDocument gere = modeleRepository.saveAndFlush(modele);
        ajouterVersion(gere, request.contenu(), miseEnPageJson, maintenant, auteur);
        return versDetail(gere);
    }

    @Transactional
    public ModeleDocumentDetailDto modifier(UUID id, ModifierModeleDocumentRequest request) {
        AppModeleDocument modele = trouver(id);
        LocalDateTime maintenant = LocalDateTime.now(clock);
        String auteur = auteur();
        if (request.libelle() != null) {
            modele.setLibelle(verifierLibelle(request.libelle()));
        }
        if (request.ordre() != null) {
            modele.setOrdre(request.ordre());
        }
        if (request.actif() != null) {
            modele.setActif(request.actif());
        }
        if (request.contenu() != null || request.miseEnPage() != null) {
            Optional<AppModeleDocumentVersion> courante = versionCourante(modele);
            String contenu = request.contenu() != null ? request.contenu()
                    : courante.map(AppModeleDocumentVersion::getContenu).orElse("");
            MiseEnPageDto miseEnPage = request.miseEnPage() != null ? request.miseEnPage()
                    : courante.map(v -> miseEnPageModele.lire(v.getMiseEnPage())).orElse(null);
            String json = miseEnPageValide(modele.getCategorie(), contenu, miseEnPage);
            ajouterVersion(modele, contenu, json, maintenant, auteur);
        }
        modele.setModifieLe(maintenant);
        modele.setModifiePar(auteur);
        modeleRepository.save(modele);
        return versDetail(modele);
    }

    /** Recopie la version {@code numero} en nouvelle version courante ; l'historique reste intact. */
    @Transactional
    public ModeleDocumentDetailDto restaurer(UUID id, int numero) {
        AppModeleDocument modele = trouver(id);
        AppModeleDocumentVersion source = versionRepository.findByModeleIdAndNumero(id, numero)
                .orElseThrow(() -> new ResourceNotFoundException("Version %d introuvable pour ce modèle.".formatted(numero)));
        LocalDateTime maintenant = LocalDateTime.now(clock);
        String auteur = auteur();
        ajouterVersion(modele, source.getContenu(), source.getMiseEnPage(), maintenant, auteur);
        modele.setModifieLe(maintenant);
        modele.setModifiePar(auteur);
        modeleRepository.save(modele);
        return versDetail(modele);
    }

    private AppModeleDocument trouver(UUID id) {
        return lecture.trouver(id);
    }

    private Optional<AppModeleDocumentVersion> versionCourante(AppModeleDocument modele) {
        return lecture.versionCourante(modele);
    }

    /** Valide contenu et mise en page contre le catalogue de la catégorie ; renvoie le JSON à stocker. */
    private String miseEnPageValide(String categorie, String contenu, MiseEnPageDto miseEnPage) {
        MiseEnPageDto normalisee = miseEnPageModele.normaliser(miseEnPage);
        ModelePrepare.preparer(catalogues.de(categorie).catalogue(), contenu, normalisee);
        return miseEnPageModele.ecrire(normalisee);
    }

    private void ajouterVersion(AppModeleDocument modele, String contenu, String miseEnPageJson,
                                LocalDateTime maintenant, String auteur) {
        AppModeleDocumentVersion version = new AppModeleDocumentVersion();
        version.setId(UUID.randomUUID());
        version.setModeleId(modele.getId());
        version.setNumero(versionRepository.dernierNumero(modele.getId()) + 1);
        version.setContenu(contenu);
        version.setMiseEnPage(miseEnPageJson);
        version.setCreeLe(maintenant);
        version.setCreePar(auteur);
        versionRepository.saveAndFlush(version);
        modele.setVersionCouranteId(version.getId());
    }

    private String verifierCategorie(String categorie) {
        catalogues.de(categorie);
        return categorie;
    }

    private static String verifierCode(String code) {
        if (code == null || code.isBlank() || code.length() > CODE_MAX
                || !code.chars().allMatch(c -> c == '_' || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))) {
            throw new InvalidRequestException(
                    "code est requis : majuscules, chiffres et _ seulement, 60 caractères au plus.");
        }
        return code;
    }

    private static String verifierLibelle(String libelle) {
        if (libelle == null || libelle.isBlank() || libelle.length() > LIBELLE_MAX) {
            throw new InvalidRequestException("libelle est requis (200 caractères au plus).");
        }
        return libelle.trim();
    }

    private String auteur() {
        return Objects.requireNonNullElse(utilisateurAffiche.courant().nom(), AUTEUR_SYSTEME);
    }

    private ModeleDocumentAdminDto versAdmin(AppModeleDocument m) {
        return new ModeleDocumentAdminDto(m.getId(), m.getCategorie(), m.getCode(), m.getLibelle(), m.getOrdre(),
                Boolean.TRUE.equals(m.getActif()), versionCourante(m).map(AppModeleDocumentVersion::getNumero).orElse(null),
                m.getModifieLe(), m.getModifiePar());
    }

    private ModeleDocumentDetailDto versDetail(AppModeleDocument m) {
        Optional<AppModeleDocumentVersion> courante = versionCourante(m);
        return new ModeleDocumentDetailDto(m.getId(), m.getCategorie(), m.getCode(), m.getLibelle(), m.getOrdre(),
                Boolean.TRUE.equals(m.getActif()), courante.map(AppModeleDocumentVersion::getNumero).orElse(null),
                courante.map(AppModeleDocumentVersion::getContenu).orElse(""),
                courante.map(v -> miseEnPageModele.lire(v.getMiseEnPage()))
                        .orElseGet(() -> miseEnPageModele.normaliser(null)),
                m.getModifieLe(), m.getModifiePar());
    }
}
