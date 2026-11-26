package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.EnregistrerDecompteRequest.ExtraSaisie;
import com.tsix.immocrm.dto.projet.EnregistrerDecompteRequest.RetenueSaisie;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketLine;
import com.tsix.immocrm.entities.prj.PrjWorkingOut;
import com.tsix.immocrm.entities.prj.PrjWorkingOutLineExtra;
import com.tsix.immocrm.entities.prj.PrjWorkingOutRetainedLine;
import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Travaux supplémentaires et retenues forfaitaires d'un décompte, liste = état final (FEATURE-lc6-decompte-extras-taux.md
 * R1-R3, R6) : {@link #preparer} valide tout sans rien modifier, {@link Plan#appliquer} écrit.
 */
final class EcritureExtrasDecompte {

    private static final int LIBELLE_MAX = 500;

    private final DonneesDecompte donnees;
    private final EcritureDecompte ecriture;

    EcritureExtrasDecompte(DonneesDecompte donnees, EcritureDecompte ecriture) {
        this.donnees = donnees;
        this.ecriture = ecriture;
    }

    /** Saisie rapprochée de la ligne existante (null = nouvelle) et, pour une nouvelle ligne du BDP, de son modèle. */
    private record Paire<S, E>(S saisie, E existant, PrjMarketLine modele) {
    }

    private record Rapprochement<S, E>(List<Paire<S, E>> paires, List<E> supprimes) {
    }

    /** Saisies validées ; listes null = inchangées. */
    final class Plan {
        private final PrjWorkingOut w;
        private final Rapprochement<ExtraSaisie, PrjWorkingOutLineExtra> extras;
        private final Rapprochement<RetenueSaisie, PrjWorkingOutRetainedLine> retenues;

        private Plan(PrjWorkingOut w, Rapprochement<ExtraSaisie, PrjWorkingOutLineExtra> extras,
                     Rapprochement<RetenueSaisie, PrjWorkingOutRetainedLine> retenues) {
            this.w = w;
            this.extras = extras;
            this.retenues = retenues;
        }

        void appliquer() {
            if (extras != null) {
                ecriture.supprimer(extras.supprimes());
                int rang = 1;
                for (Paire<ExtraSaisie, PrjWorkingOutLineExtra> p : extras.paires()) {
                    ecrireExtra(w, p, rang++);
                }
            }
            if (retenues != null) {
                ecriture.supprimer(retenues.supprimes());
                int rang = 1;
                for (Paire<RetenueSaisie, PrjWorkingOutRetainedLine> p : retenues.paires()) {
                    ecrireRetenue(w, p, rang++);
                }
            }
        }
    }

    Plan preparer(PrjWorkingOut w, List<ExtraSaisie> extras, List<RetenueSaisie> retenues) {
        Rapprochement<ExtraSaisie, PrjWorkingOutLineExtra> e = null;
        if (extras != null) {
            Map<UUID, PrjMarketLine> bordereau = donnees.bordereau(w.getPrjMarket().getId()).stream()
                    .collect(Collectors.toMap(PrjMarketLine::getId, Function.identity(), (a, b) -> a));
            e = rapprocher(donnees.enfants(PrjWorkingOutLineExtra.class, w.getId()), PrjWorkingOutLineExtra::getId,
                    extras, ExtraSaisie::id, "Travail supplémentaire inconnu : ",
                    (s, existant) -> modele(s, existant, bordereau));
        }
        Rapprochement<RetenueSaisie, PrjWorkingOutRetainedLine> r = null;
        if (retenues != null) {
            r = rapprocher(donnees.enfants(PrjWorkingOutRetainedLine.class, w.getId()), PrjWorkingOutRetainedLine::getId,
                    retenues, RetenueSaisie::id, "Retenue forfaitaire inconnue : ", EcritureExtrasDecompte::verifierRetenue);
        }
        return new Plan(w, e, r);
    }

    @FunctionalInterface
    private interface Verification<S, E> {
        PrjMarketLine verifier(S saisie, E existant);
    }

    /** Id d'une autre liste que celle du décompte → 400 ; lignes existantes absentes de la saisie = supprimées. */
    private static <S, E> Rapprochement<S, E> rapprocher(List<E> existants, Function<E, UUID> idExistant, List<S> saisies,
                                                         Function<S, UUID> idSaisie, String inconnu,
                                                         Verification<S, E> verification) {
        Map<UUID, E> parId = existants.stream().collect(Collectors.toMap(idExistant, Function.identity(), (a, b) -> a));
        List<Paire<S, E>> paires = new ArrayList<>();
        for (S s : saisies) {
            if (s == null) {
                throw new InvalidRequestException("Ligne vide.");
            }
            UUID id = idSaisie.apply(s);
            E existant = id == null ? null : parId.get(id);
            if (id != null && existant == null) {
                throw new InvalidRequestException(inconnu + id + ".");
            }
            paires.add(new Paire<>(s, existant, verification.verifier(s, existant)));
        }
        List<E> gardes = paires.stream().map(Paire::existant).filter(Objects::nonNull).toList();
        return new Rapprochement<>(paires, existants.stream().filter(x -> !gardes.contains(x)).toList());
    }

    /** R3 : seule une nouvelle ligne est recopiée du bordereau définitif du lot ; R2 : désignation obligatoire. */
    private static PrjMarketLine modele(ExtraSaisie s, PrjWorkingOutLineExtra existant, Map<UUID, PrjMarketLine> bordereau) {
        PrjMarketLine m = null;
        if (existant == null && s.ligneMarcheId() != null) {
            m = bordereau.get(s.ligneMarcheId());
            if (m == null) {
                throw new InvalidRequestException("Ligne de bordereau inconnue : " + s.ligneMarcheId() + ".");
            }
        }
        String designation = m == null ? s.designation() : m.getDescription();
        if (designation == null || designation.isBlank()) {
            throw new InvalidRequestException("La désignation d'un travail supplémentaire est obligatoire.");
        }
        return m;
    }

    private static PrjMarketLine verifierRetenue(RetenueSaisie s, PrjWorkingOutRetainedLine existant) {
        if (s.libelle() == null || s.libelle().isBlank()) {
            throw new InvalidRequestException("Le libellé d'une retenue forfaitaire est obligatoire.");
        }
        if (s.libelle().length() > LIBELLE_MAX) {
            throw new InvalidRequestException("Libellé de retenue forfaitaire trop long (" + LIBELLE_MAX + " caractères au plus).");
        }
        return null;
    }

    private void ecrireExtra(PrjWorkingOut w, Paire<ExtraSaisie, PrjWorkingOutLineExtra> p, int rang) {
        ExtraSaisie s = p.saisie();
        PrjWorkingOutLineExtra e = p.existant() == null ? new PrjWorkingOutLineExtra() : p.existant();
        PrjMarketLine m = p.modele();
        if (m == null) {
            e.setCode(s.code());
            e.setDescription(s.designation());
            e.setUnity(s.unite());
            e.setNotes(s.notes());
            e.setUnitPriceHT(s.prixUnitaireHT());
            e.setVatRatio(ratio(s.tauxTVA()));
            e.setStlCategoryId(s.categorieId() == null ? categorieDuLot(w.getPrjMarket()) : s.categorieId());
        } else {
            e.setCode(m.getCode());
            e.setDescription(m.getDescription());
            e.setUnity(m.getUnity());
            e.setNotes(m.getNotes());
            e.setUnitPriceHT(m.getUnitPriceHT());
            e.setVatRatio(m.getVatRatio());
            e.setStlCategoryId(m.getStlCategoryId() == null ? categorieDuLot(w.getPrjMarket()) : m.getStlCategoryId());
            e.setPrjMarketLineId(m.getId());
        }
        e.setSorting(rang);
        e.setQuantity(Objects.requireNonNullElse(s.quantite(), BigDecimal.ZERO));
        e.setFodecRatio(ratio(Objects.requireNonNullElse(s.tauxFODEC(), BigDecimal.ZERO)));
        e.setUnitPriceTTC(Objects.requireNonNullElse(e.getUnitPriceHT(), BigDecimal.ZERO)
                .multiply(BigDecimal.ONE.add(Objects.requireNonNullElse(e.getVatRatio(), BigDecimal.ZERO)))
                .setScale(6, RoundingMode.HALF_UP));
        if (p.existant() == null) {
            e.setId(UUID.randomUUID());
            e.setPrjWorkingOutId(w.getId());
            ecriture.creer(e);
        } else {
            ecriture.modifier(e);
        }
    }

    private void ecrireRetenue(PrjWorkingOut w, Paire<RetenueSaisie, PrjWorkingOutRetainedLine> p, int rang) {
        PrjWorkingOutRetainedLine r = p.existant() == null ? new PrjWorkingOutRetainedLine() : p.existant();
        r.setDescription(p.saisie().libelle());
        r.setAmount(p.saisie().montant());
        r.setSorting(rang);
        if (p.existant() == null) {
            r.setId(UUID.randomUUID());
            r.setPrjWorkingOutId(w.getId());
            ecriture.creerSansAudit(r);
        }
    }

    private static UUID categorieDuLot(PrjMarket lot) {
        return lot.getStlCategory() == null ? null : lot.getStlCategory().getId();
    }

    private static BigDecimal ratio(BigDecimal pourcentage) {
        return pourcentage == null ? null : pourcentage.movePointLeft(2);
    }
}
