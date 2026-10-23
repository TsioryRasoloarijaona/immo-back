package com.tsix.immocrm.services.patrimoine.importation;

import com.tsix.immocrm.domain.ColonneLibre;
import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.entities.CodeLibelleOmran;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.patrimoine.importation.ChampsArticle.ChampImport;
import com.tsix.immocrm.web.export.ExportTableur;
import com.tsix.immocrm.web.export.FeuilleExport;
import jakarta.persistence.EntityManager;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.patrimoine.ArticleService.NATURE_BATIMENT;

/**
 * Classeur modèle d'import (FEATURE-patrimoine-import-export.md §1.1) : en-têtes OMRAN, une ligne par objet de la
 * tranche (comme {@code ImportItems}), puis une feuille d'aide (Code, Libellé) par référentiel cité.
 */
@Service
@Transactional(readOnly = true)
public class ModeleImport {

    private static final String DE_LA_TRANCHE = "select i from StkItem i where i.stkHierarchy.id = :tranche order by i.code";
    private static final DateTimeFormatter HORODATAGE = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final List<Colonne> AIDE = List.of(
            new Colonne("Code", TypeExport.TEXTE), new Colonne("Libellé", TypeExport.TEXTE));

    private final TrancheVisibleRepository tranches;
    private final StkItemRepository items;
    private final EntityManager em;
    private final ExportTableur export;
    private final UtilisateurAffiche utilisateur;
    private final Clock clock;

    public ModeleImport(TrancheVisibleRepository tranches, StkItemRepository items, EntityManager em,
                        ExportTableur export, UtilisateurAffiche utilisateur, Clock clock) {
        this.tranches = tranches;
        this.items = items;
        this.em = em;
        this.export = export;
        this.utilisateur = utilisateur;
        this.clock = clock;
    }

    /** Colonne du modèle : nom = libellé = en-tête OMRAN. */
    private record Colonne(String nom, TypeExport typeExport) implements ColonneLibre {
        @Override
        public String libelle() {
            return nom;
        }

        @Override
        public boolean parDefaut() {
            return true;
        }
    }

    public ResponseEntity<byte[]> classeur(String type, UUID trancheId) {
        boolean sous = ImportPatrimoineService.sousArticles(type);
        CfgTranche tranche = tranches.exigerVisible(ImportPatrimoineService.exigerTranche(trancheId));
        List<ChampImport> champs = ChampsArticle.du(sous);
        String nom = (sous ? "SOUS_ARTICLES" : "ARTICLES");
        List<FeuilleExport> feuilles = new ArrayList<>();
        feuilles.add(new FeuilleExport(nom, champs.stream().map(ModeleImport::colonne).toList(), lignes(tranche, sous, champs),
                null));
        ResolutionCodes codes = new ResolutionCodes(em, trancheId);
        Set<Class<? extends CodeLibelleOmran>> referentiels = new LinkedHashSet<>();
        champs.stream().filter(c -> c.reference() != null).forEach(c -> referentiels.add(c.reference().classe()));
        referentiels.forEach(classe -> feuilles.add(new FeuilleExport(classe.getSimpleName(), AIDE,
                codes.toutes(classe).stream().map(ModeleImport::aide).toList(), null)));
        String fichier = tranche.getCode() + "_STOCK_" + nom + "_" + LocalDateTime.now(clock).format(HORODATAGE) + ".xlsx";
        return ResponseEntity.ok()
                .contentType(ExportTableur.XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fichier).build().toString())
                .body(octets(nom, feuilles));
    }

    private List<Map<String, Object>> lignes(CfgTranche tranche, boolean sous, List<ChampImport> champs) {
        return em.createQuery(DE_LA_TRANCHE, StkItem.class).setParameter("tranche", tranche.getId())
                .getResultList().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsUnderItem()) == sous
                        && NATURE_BATIMENT.equals(Champs.de(item.getStkKind(), StkKind::getCode)))
                .map(item -> ligne(item, sous ? List.of() : sousArticles(item), champs))
                .toList();
    }

    private List<StkItem> sousArticles(StkItem article) {
        return items.findByParentId(article.getId()).stream()
                .sorted(Comparator.comparing(StkItem::getCode, Comparator.nullsLast(String::compareTo)))
                .limit(ChampsArticle.SOUS_ARTICLES_PAR_LIGNE).toList();
    }

    private static Map<String, Object> ligne(StkItem item, List<StkItem> sousArticles, List<ChampImport> champs) {
        Map<String, Object> ligne = new HashMap<>();
        for (ChampImport c : champs) {
            StkItem objet = c.rang() == 0 ? item : rang(sousArticles, c.rang());
            if (objet != null) {
                ligne.put(c.entete(), ChampsArticle.pourModele(c, objet));
            }
        }
        return ligne;
    }

    private static StkItem rang(List<StkItem> sousArticles, int rang) {
        return rang <= sousArticles.size() ? sousArticles.get(rang - 1) : null;
    }

    private static Map<String, Object> aide(CodeLibelleOmran r) {
        Map<String, Object> ligne = new HashMap<>();
        ligne.put("Code", r.getCode());
        ligne.put("Libellé", r.getDescription());
        return ligne;
    }

    private static Colonne colonne(ChampImport c) {
        TypeExport type = switch (c.type()) {
            case NOMBRE, ENTIER, TVA -> TypeExport.NOMBRE;
            case DATE -> TypeExport.DATE;
            case BOOLEEN, BOOLEEN_INVERSE -> TypeExport.BOOLEEN;
            default -> TypeExport.TEXTE;
        };
        return new Colonne(c.entete(), type);
    }

    private byte[] octets(String titre, List<FeuilleExport> feuilles) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        try {
            export.ecrire(sortie, titre, Objects.toString(utilisateur.courant().nom(), ""), feuilles);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sortie.toByteArray();
    }
}
