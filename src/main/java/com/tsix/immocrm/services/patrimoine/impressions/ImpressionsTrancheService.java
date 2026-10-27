package com.tsix.immocrm.services.patrimoine.impressions;

import com.tsix.immocrm.domain.CategorieModele;
import com.tsix.immocrm.dto.dossier.SyntheseMontantDto;
import com.tsix.immocrm.dto.patrimoine.impressions.ApercuRelance2Dto;
import com.tsix.immocrm.dto.patrimoine.impressions.EcarteRelance2Dto;
import com.tsix.immocrm.dto.patrimoine.impressions.GroupeStatutRelance2Dto;
import com.tsix.immocrm.dto.patrimoine.impressions.GroupeStatutRelance2Dto.StatutRelance2Dto;
import com.tsix.immocrm.dto.patrimoine.impressions.LigneRelance2Dto;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.services.modele.CatalogueRelanceTitreFoncier;
import com.tsix.immocrm.services.modele.GenerationModeleService;
import com.tsix.immocrm.services.modele.InitialiseurModeles;
import com.tsix.immocrm.services.modele.LectureModeles;
import com.tsix.immocrm.services.patrimoine.impressions.SelectionRelance2.Lettre;
import com.tsix.immocrm.services.patrimoine.impressions.SelectionRelance2.Retenu;
import com.tsix.immocrm.services.patrimoine.impressions.SelectionRelance2.Selection;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.utils.TierNomFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Relance 2 par statut de la tranche, lecture seule (FEATURE-patrimoine-impressions-tranche.md). */
@Service
@Transactional(readOnly = true)
public class ImpressionsTrancheService {

    public static final String CODE_MODELE = "RELANCE_2";
    public static final String RESSOURCE_MODELE = "modeles/relance-titre-foncier/deuxieme-relance.html";
    static final String AUCUN_STATUT = "Aucun dossier de la tranche n'a un statut à relancer.";
    static final String AUCUN_RESTE = "Aucun dossier n'a de reste du prix à relancer.";
    private static final CategorieModele CATEGORIE = CategorieModele.RELANCE_TITRE_FONCIER;

    private final SelectionRelance2 selection;
    private final LectureModeles modeles;
    private final GenerationModeleService generation;
    private final CatalogueRelanceTitreFoncier catalogue;
    private final Clock clock;

    public ImpressionsTrancheService(SelectionRelance2 selection, LectureModeles modeles,
                                     GenerationModeleService generation, CatalogueRelanceTitreFoncier catalogue,
                                     Clock clock) {
        this.selection = selection;
        this.modeles = modeles;
        this.generation = generation;
        this.catalogue = catalogue;
        this.clock = clock;
    }

    public record Lettres(String nomFichier, byte[] pdf) {
    }

    public ApercuRelance2Dto apercu(UUID trancheId, List<String> statuts) {
        Selection s = selection.selectionner(trancheId, statuts);
        List<Lettre> lettres = s.lettres();
        boolean modeleActif = modeles.actifParCode(CATEGORIE.name(), CODE_MODELE).flatMap(modeles::versionCourante).isPresent();
        return new ApercuRelance2Dto(s.tranche().getId(), s.tranche().getCode(), LocalDate.now(clock), s.statuts(),
                s.retenus().size(), lettres.size(),
                s.retenus().stream().filter(Retenu::ecarte).map(ImpressionsTrancheService::ecarte).toList(),
                modeleActif, parStatut(s.retenus()), lettres.stream().map(ImpressionsTrancheService::ligne).toList());
    }

    /** 409 si aucun dossier retenu, ou si tous sont écartés (reste ≤ 0). */
    public Lettres lettres(UUID trancheId, List<String> statuts) {
        Selection s = selection.selectionner(trancheId, statuts);
        if (s.retenus().isEmpty()) {
            throw new ConflictException(AUCUN_STATUT);
        }
        List<Lettre> lettres = s.lettres();
        if (lettres.isEmpty()) {
            throw new ConflictException(AUCUN_RESTE);
        }
        byte[] pdf = generation.pdfParPage(CATEGORIE, CODE_MODELE, InitialiseurModeles.contenu(RESSOURCE_MODELE),
                lettres.stream().map(l -> catalogue.catalogue().valeurs(catalogue.donnees(l.retenu().dossier(),
                        l.acquereur(), l.coAcquereur(), l.retenu().montants()))).toList());
        String nom = "relance2_%s_%s.pdf".formatted(s.tranche().getCode(),
                LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE));
        return new Lettres(nom, pdf);
    }

    /** Un groupe par statut présent, dans l'ordre de {@link SelectionRelance2#STATUTS}. */
    private static List<GroupeStatutRelance2Dto> parStatut(List<Retenu> retenus) {
        Map<String, List<Retenu>> parCode = retenus.stream().collect(Collectors.groupingBy(
                r -> r.dossier().getStrStatus().getCode(), LinkedHashMap::new, Collectors.toList()));
        return SelectionRelance2.STATUTS.stream().filter(parCode::containsKey).map(code -> {
            List<Retenu> groupe = parCode.get(code);
            StrStatus statut = groupe.get(0).dossier().getStrStatus();
            return new GroupeStatutRelance2Dto(
                    new StatutRelance2Dto(statut.getId(), statut.getCode(), statut.getDescription()),
                    groupe.size(), (int) groupe.stream().flatMap(Retenu::lettres).count());
        }).toList();
    }

    private static LigneRelance2Dto ligne(Lettre l) {
        ComFolder d = l.retenu().dossier();
        SyntheseMontantDto prix = l.retenu().prix();
        return new LigneRelance2Dto(d.getId(), d.getCode(), article(d), statut(d), TierNomFormatter.nom(l.acquereur()),
                l.coAcquereur(), prix.montant(), prix.regle(), prix.reste(), jour(d.getRevival2()));
    }

    private static EcarteRelance2Dto ecarte(Retenu r) {
        ComFolder d = r.dossier();
        SyntheseMontantDto prix = r.prix();
        return new EcarteRelance2Dto(d.getId(), d.getCode(), article(d), statut(d),
                TierNomFormatter.nom(d.getCfgCustomer()), prix.montant(), prix.regle(), prix.reste());
    }

    private static String article(ComFolder d) {
        return Champs.de(d.getStkItem(), StkItem::getCode);
    }

    private static String statut(ComFolder d) {
        return Champs.de(d.getStrStatus(), StrStatus::getDescription);
    }

    private static LocalDate jour(LocalDateTime date) {
        return date == null ? null : date.toLocalDate();
    }
}
