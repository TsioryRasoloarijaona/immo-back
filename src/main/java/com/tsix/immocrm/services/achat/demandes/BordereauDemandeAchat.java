package com.tsix.immocrm.services.achat.demandes;

import com.tsix.immocrm.domain.TypeExport;
import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.dto.projet.BordereauSaisieDto;
import com.tsix.immocrm.dto.projet.GroupeSaisieDto;
import com.tsix.immocrm.dto.projet.LigneSaisieDto;
import com.tsix.immocrm.entities.buy.BuySelectionGroup;
import com.tsix.immocrm.entities.buy.BuySelectionLine;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.patrimoine.importation.LectureClasseur;
import com.tsix.immocrm.services.projet.ValidationBordereau;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.ExportTableur;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Bordereau d'une DIA ({@code BuySelectionGroup} / {@code BuySelectionLine}) : lecture, saisie, import, export et copies. */
@Component
class BordereauDemandeAchat {

    private static final String GROUPE = "Groupe";
    private static final String CODE = "Code";
    private static final String CATEGORIE = "Catégorie";
    private static final List<ColonneFixe> COLONNES = List.of(ColonneFixe.texte("groupe", GROUPE), ColonneFixe.texte("code", CODE),
            ColonneFixe.texte("designation", "Désignation"), ColonneFixe.texte("unite", "Unité"),
            ColonneFixe.de("quantite", "Quantité", TypeExport.NOMBRE), ColonneFixe.de("prixUnitaireHT", "PU HT", TypeExport.NOMBRE),
            ColonneFixe.de("tauxTVA", "TVA %", TypeExport.NOMBRE), ColonneFixe.de("tauxFODEC", "FODEC %", TypeExport.NOMBRE),
            ColonneFixe.texte("categorie", CATEGORIE), ColonneFixe.texte("notes", "Notes"));

    private final EntityManager em;
    private final ExportTableur export;
    private final UtilisateurAffiche utilisateur;
    private final Clock clock;

    BordereauDemandeAchat(EntityManager em, ExportTableur export, UtilisateurAffiche utilisateur, Clock clock) {
        this.em = em;
        this.export = export;
        this.utilisateur = utilisateur;
        this.clock = clock;
    }

    /** Groupe à copier ; {@code id} et {@code parentId} sont ceux de la source, remplacés à l'écriture. */
    record GroupeSource(UUID id, UUID parentId, Integer ordre, String code, String description, String notes) {
    }

    /** Ligne à copier ; TVA et FODEC en fraction. */
    record LigneSource(UUID groupeId, Integer ordre, String code, String description, String unite, BigDecimal quantite,
                       BigDecimal prixUnitaireHT, BigDecimal tva, BigDecimal fodec, UUID categorie, String notes) {
    }

    private record LigneExport(String groupe, String code, String designation, String unite, BigDecimal quantite,
                               BigDecimal prixUnitaireHT, BigDecimal tauxTVA, BigDecimal tauxFODEC, String categorie, String notes) {
    }

    List<BuySelectionGroup> groupes(UUID dia) {
        return em.createQuery("select g from BuySelectionGroup g where g.buySelectionId = :id order by g.sorting, g.code",
                BuySelectionGroup.class).setParameter("id", dia).getResultList();
    }

    List<BuySelectionLine> lignes(UUID dia) {
        return em.createQuery("select l from BuySelectionLine l where l.buySelectionId = :id order by l.sorting, l.code",
                BuySelectionLine.class).setParameter("id", dia).getResultList();
    }

    BordereauSaisieDto lire(UUID dia) {
        return new BordereauSaisieDto(groupes(dia).stream().map(g -> new GroupeSaisieDto(g.getId(), g.getParentId(), g.getSorting(),
                        g.getCode(), g.getDescription(), g.getNotes())).toList(),
                lignes(dia).stream().map(l -> new LigneSaisieDto(l.getId(), l.getBuySelectionGroupId(), l.getSorting(), l.getCode(),
                        l.getDescription(), l.getUnity(), l.getQuantity(), l.getUnitPriceHT(), pourcent(l.getVatRatio()),
                        pourcent(l.getFodecRatio()), l.getStlCategoryId(), null, l.getNotes())).toList());
    }

    /** Bordereau entier : mêmes règles que le bordereau estimatif d'un lot (code unique) ; ids remplacés à l'écriture. */
    void enregistrer(UUID dia, BordereauSaisieDto corps) {
        ValidationBordereau.valider(corps, true, this::categoriesExistantes);
        vider(dia);
        ecrire(dia, corps.groupes().stream().map(g -> new GroupeSource(g.id(), g.parentId(), g.ordre(), g.code(), g.description(),
                        g.notes())).toList(),
                corps.lignes().stream().map(l -> new LigneSource(l.groupeId(), l.ordre(), l.code(), l.designation(), l.unite(),
                        l.quantite(), l.prixUnitaireHT(), fraction(l.tauxTVA()), fraction(l.tauxFODEC()), l.categorieId(),
                        l.notes())).toList());
    }

    /** Classeur au format de l'export ; groupe et catégorie désignés par leur code ; les groupes de la DIA sont gardés. */
    void importer(UUID dia, MultipartFile fichier) {
        LectureClasseur.Feuille feuille = LectureClasseur.lire(fichier, null);
        List<BuySelectionGroup> groupes = groupes(dia);
        Map<String, UUID> groupesParCode = groupes.stream().filter(g -> g.getCode() != null)
                .collect(Collectors.toMap(BuySelectionGroup::getCode, BuySelectionGroup::getId, (a, b) -> a));
        Set<String> codesCategories = feuille.lignes().stream().map(l -> texte(l, CATEGORIE)).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, UUID> categories = codesCategories.isEmpty() ? Map.of() : em.createQuery(
                        "select c.code, c.id from StlCategory c where c.code in :codes", Object[].class)
                .setParameter("codes", codesCategories).getResultStream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (UUID) r[1], (a, b) -> a));
        List<LigneSaisieDto> lignes = new ArrayList<>();
        for (LectureClasseur.LigneLue l : feuille.lignes()) {
            lignes.add(new LigneSaisieDto(UUID.randomUUID(), reference(l, GROUPE, groupesParCode), lignes.size() + 1, texte(l, CODE),
                    texte(l, "Désignation"), texte(l, "Unité"), nombre(l, "Quantité"), nombre(l, "PU HT"), nombre(l, "TVA %"),
                    nombre(l, "FODEC %"), reference(l, CATEGORIE, categories), null, texte(l, "Notes")));
        }
        enregistrer(dia, new BordereauSaisieDto(groupes.stream().map(g -> new GroupeSaisieDto(g.getId(), g.getParentId(),
                g.getSorting(), g.getCode(), g.getDescription(), g.getNotes())).toList(), lignes));
    }

    ResponseEntity<StreamingResponseBody> exporter(UUID dia) {
        Map<UUID, String> groupes = groupes(dia).stream()
                .collect(HashMap::new, (m, g) -> m.put(g.getId(), g.getCode()), HashMap::putAll);
        Map<UUID, String> categories = new HashMap<>();
        List<LigneExport> lignes = lignes(dia).stream().map(l -> new LigneExport(groupes.get(l.getBuySelectionGroupId()), l.getCode(),
                l.getDescription(), l.getUnity(), l.getQuantity(), l.getUnitPriceHT(), pourcent(l.getVatRatio()),
                pourcent(l.getFodecRatio()), categories.computeIfAbsent(l.getStlCategoryId(), this::codeCategorie), l.getNotes()))
                .toList();
        return export.reponse("bordereau-demande-achat", "Bordereau de la demande d'achat", COLONNES, lignes);
    }

    /** Groupes et lignes de la DIA supprimés (désélection R16, saisie entière). */
    void vider(UUID dia) {
        List.of("DELETE FROM BuySelectionLine WHERE BuySelectionId = :id", "DELETE FROM BuySelectionGroup WHERE BuySelectionId = :id")
                .forEach(sql -> em.createNativeQuery(sql).setParameter("id", dia).executeUpdate());
    }

    /** Écrit groupes et lignes sous de nouvelles PKey ; parents et groupes des lignes suivent (R18, R19, saisie). */
    void ecrire(UUID dia, List<GroupeSource> groupes, List<LigneSource> lignes) {
        Map<UUID, UUID> nouveaux = groupes.stream().collect(Collectors.toMap(GroupeSource::id, g -> UUID.randomUUID()));
        String auteur = utilisateur.courant().nom();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        for (GroupeSource s : groupes) {
            BuySelectionGroup g = new BuySelectionGroup();
            g.setId(nouveaux.get(s.id()));
            g.setBuySelectionId(dia);
            g.setParentId(s.parentId() == null ? null : nouveaux.get(s.parentId()));
            g.setSorting(s.ordre());
            g.setCode(s.code());
            g.setDescription(s.description());
            g.setNotes(s.notes());
            g.setUserCreate(auteur);
            g.setUserCreateDateTime(maintenant);
            em.persist(g);
        }
        for (LigneSource s : lignes) {
            BuySelectionLine l = new BuySelectionLine();
            BigDecimal pu = zero(s.prixUnitaireHT());
            l.setId(UUID.randomUUID());
            l.setBuySelectionId(dia);
            l.setBuySelectionGroupId(s.groupeId() == null ? null : nouveaux.get(s.groupeId()));
            l.setSorting(s.ordre());
            l.setCode(s.code());
            l.setDescription(s.description());
            l.setUnity(s.unite());
            l.setQuantity(s.quantite());
            l.setUnitPriceHT(pu);
            l.setVatRatio(zero(s.tva()));
            l.setFodecRatio(zero(s.fodec()));
            l.setUnitPriceTTC(MontantsLot.prixTTC(pu, zero(s.fodec()), zero(s.tva())));
            l.setStlCategoryId(s.categorie());
            l.setNotes(s.notes());
            l.setUserCreate(auteur);
            l.setUserCreateDateTime(maintenant);
            em.persist(l);
        }
        em.flush();
    }

    private Set<UUID> categoriesExistantes(Set<UUID> ids) {
        return ids.isEmpty() ? Set.of() : Set.copyOf(em.createQuery("select c.id from StlCategory c where c.id in :ids", UUID.class)
                .setParameter("ids", ids).getResultList());
    }

    private String codeCategorie(UUID id) {
        return id == null ? null : em.createQuery("select c.code from StlCategory c where c.id = :id", String.class)
                .setParameter("id", id).getResultStream().findFirst().orElse(null);
    }

    private static UUID reference(LectureClasseur.LigneLue l, String colonne, Map<String, UUID> parCode) {
        String code = texte(l, colonne);
        if (code == null) {
            return null;
        }
        return Optional.ofNullable(parCode.get(code)).orElseThrow(() -> new InvalidRequestException(
                "Ligne %d : %s « %s » inconnu(e)".formatted(l.numero(), colonne.toLowerCase(), code)));
    }

    private static String texte(LectureClasseur.LigneLue l, String colonne) {
        return Objects.toString(l.valeurs().get(colonne), null);
    }

    private static BigDecimal nombre(LectureClasseur.LigneLue l, String colonne) {
        Object v = l.valeurs().get(colonne);
        if (v == null || v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        throw new InvalidRequestException("Ligne %d : %s doit être un nombre".formatted(l.numero(), colonne));
    }

    private static BigDecimal pourcent(BigDecimal fraction) {
        return fraction == null ? null : fraction.movePointRight(2);
    }

    private static BigDecimal fraction(BigDecimal pourcent) {
        return pourcent == null ? null : pourcent.movePointLeft(2);
    }

    private static BigDecimal zero(BigDecimal v) {
        return Objects.requireNonNullElse(v, BigDecimal.ZERO);
    }
}
