package com.tsix.immocrm.services.achat.demandes;

import com.tsix.immocrm.dto.achat.demandes.ElementDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.FournisseurDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.ModaliteDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.SaisieFournisseurDemandeAchat;
import com.tsix.immocrm.entities.buy.BuyDocument;
import com.tsix.immocrm.entities.buy.BuySelectionPaymentTerm;
import com.tsix.immocrm.entities.buy.BuySelectionSupplier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Présélection des fournisseurs (R15) et modalités de paiement d'une DIA ; droits, R7 et phase contrôlés par l'appelant. */
@Component
class PreselectionDemandeAchat {

    private static final String DEVIS_DU_TIERS = """
            select b from BuyDocument b where b.buySelectionId = :id and b.cfgTierId = :tiers and b.docType = 'QUOTE'""";

    private final EntityManager em;
    private final UtilisateurAffiche utilisateur;
    private final Clock clock;

    PreselectionDemandeAchat(EntityManager em, UtilisateurAffiche utilisateur, Clock clock) {
        this.em = em;
        this.utilisateur = utilisateur;
        this.clock = clock;
    }

    List<FournisseurDemandeAchat> fournisseurs(UUID dia) {
        return em.createQuery("""
                        select s, t from BuySelectionSupplier s left join CfgTier t on t.id = s.cfgTierId
                        where s.buySelectionId = :id order by t.description, s.id""", Object[].class)
                .setParameter("id", dia).getResultStream()
                .map(r -> fournisseur((BuySelectionSupplier) r[0], (CfgTier) r[1])).toList();
    }

    BuySelectionSupplier preselection(UUID dia, UUID fid) {
        return Optional.ofNullable(em.find(BuySelectionSupplier.class, fid)).filter(s -> dia.equals(s.getBuySelectionId()))
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur présélectionné introuvable : " + fid));
    }

    /** Présélection du tiers sur la DIA, vide s'il n'est pas présélectionné. */
    Optional<BuySelectionSupplier> duTiers(UUID dia, UUID tiers) {
        return em.createQuery("select s from BuySelectionSupplier s where s.buySelectionId = :id and s.cfgTierId = :tiers",
                BuySelectionSupplier.class).setParameter("id", dia).setParameter("tiers", tiers).getResultStream().findFirst();
    }

    FournisseurDemandeAchat ajouter(UUID dia, SaisieFournisseurDemandeAchat saisie) {
        CfgTier tiers = valider(saisie);
        exigerAbsent(dia, tiers.getId());
        BuySelectionSupplier s = new BuySelectionSupplier();
        s.setId(UUID.randomUUID());
        s.setBuySelectionId(dia);
        s.setIsRejected(false);
        s.setUserCreate(utilisateur.courant().nom());
        s.setUserCreateDateTime(LocalDateTime.now(clock));
        remplir(s, tiers, saisie);
        em.persist(s);
        em.flush();
        return fournisseur(s, tiers);
    }

    FournisseurDemandeAchat modifier(UUID dia, UUID fid, SaisieFournisseurDemandeAchat saisie) {
        BuySelectionSupplier s = preselection(dia, fid);
        CfgTier tiers = valider(saisie);
        if (!tiers.getId().equals(s.getCfgTierId())) {
            exigerAbsent(dia, tiers.getId());
        }
        remplir(s, tiers, saisie);
        s.setUserUpdate(utilisateur.courant().nom());
        s.setUserUpdateDateTime(LocalDateTime.now(clock));
        em.flush();
        return fournisseur(s, tiers);
    }

    void supprimer(UUID dia, UUID fid) {
        BuySelectionSupplier s = preselection(dia, fid);
        if (!devis(dia, s.getCfgTierId()).isEmpty()) {
            throw new ConflictException("Vous ne pouvez pas supprimer un fournisseur ayant des offres.");
        }
        em.remove(s);
        em.flush();
    }

    /** Écarte (ou réactive) le fournisseur et toutes ses offres sur la DIA ({@code IsRejected}). */
    FournisseurDemandeAchat ecarter(UUID dia, UUID fid, boolean ecarte) {
        BuySelectionSupplier s = preselection(dia, fid);
        List<BuyDocument> offres = devis(dia, s.getCfgTierId());
        if (ecarte && offres.stream().anyMatch(o -> Boolean.TRUE.equals(o.getIsSelected()))) {
            throw new ConflictException("Vous ne pouvez pas écarter le fournisseur de l'offre sélectionnée.");
        }
        s.setIsRejected(ecarte);
        s.setUserUpdate(utilisateur.courant().nom());
        s.setUserUpdateDateTime(LocalDateTime.now(clock));
        offres.forEach(o -> o.setIsRejected(ecarte));
        em.flush();
        return fournisseur(s, s.getCfgTierId() == null ? null : em.find(CfgTier.class, s.getCfgTierId()));
    }

    List<ModaliteDemandeAchat> modalites(UUID dia) {
        return em.createQuery("select m from BuySelectionPaymentTerm m where m.buySelectionId = :id order by m.sorting, m.code",
                        BuySelectionPaymentTerm.class).setParameter("id", dia).getResultStream()
                .map(PreselectionDemandeAchat::modalite).toList();
    }

    ModaliteDemandeAchat ajouterModalite(UUID dia, ModaliteDemandeAchat saisie) {
        if (saisie == null || saisie.ordre() == null || saisie.code() == null || saisie.code().isBlank()) {
            throw new InvalidRequestException("ordre, code : obligatoires");
        }
        longueur(saisie.code(), 255, "code");
        BuySelectionPaymentTerm m = new BuySelectionPaymentTerm();
        m.setId(UUID.randomUUID());
        m.setBuySelectionId(dia);
        m.setSorting(saisie.ordre());
        m.setCode(saisie.code().trim());
        m.setRatio(saisie.ratio());
        m.setUserCreate(utilisateur.courant().nom());
        m.setUserCreateDateTime(LocalDateTime.now(clock));
        em.persist(m);
        em.flush();
        return modalite(m);
    }

    void supprimerModalite(UUID dia, UUID mid) {
        BuySelectionPaymentTerm m = Optional.ofNullable(em.find(BuySelectionPaymentTerm.class, mid))
                .filter(x -> dia.equals(x.getBuySelectionId()))
                .orElseThrow(() -> new ResourceNotFoundException("Modalité introuvable : " + mid));
        em.remove(m);
        em.flush();
    }

    List<BuyDocument> devis(UUID dia, UUID tiers) {
        return em.createQuery(DEVIS_DU_TIERS, BuyDocument.class).setParameter("id", dia).setParameter("tiers", tiers).getResultList();
    }

    private void exigerAbsent(UUID dia, UUID tiers) {
        if (duTiers(dia, tiers).isPresent()) {
            throw new ConflictException("Ce fournisseur est déja ajouté");
        }
    }

    private CfgTier valider(SaisieFournisseurDemandeAchat saisie) {
        if (saisie == null || saisie.fournisseurId() == null) {
            throw new InvalidRequestException("fournisseurId : obligatoire");
        }
        longueur(saisie.referenceOdt(), 255, "referenceOdt");
        longueur(saisie.notes(), 1000, "notes");
        return Optional.ofNullable(em.find(CfgTier.class, saisie.fournisseurId()))
                .orElseThrow(() -> new InvalidRequestException("fournisseurId : introuvable"));
    }

    private static void remplir(BuySelectionSupplier s, CfgTier tiers, SaisieFournisseurDemandeAchat saisie) {
        s.setCfgTierId(tiers.getId());
        s.setOfficeOrderRef(saisie.referenceOdt());
        s.setSelectionLetterSendDate(debut(saisie.lettreEnvoyee()));
        s.setTenderAcquisitionDate(debut(saisie.retraitDossier()));
        s.setTenderDepositDate(debut(saisie.depot()));
        s.setNotes(saisie.notes());
    }

    private static FournisseurDemandeAchat fournisseur(BuySelectionSupplier s, CfgTier t) {
        return new FournisseurDemandeAchat(s.getId(), t == null ? null : new ElementDemandeAchat(t.getId(), t.getCode(), t.getDescription()),
                s.getOfficeOrderRef(), jour(s.getSelectionLetterSendDate()), jour(s.getTenderAcquisitionDate()),
                jour(s.getTenderDepositDate()), Boolean.TRUE.equals(s.getIsRejected()), s.getNotes());
    }

    private static ModaliteDemandeAchat modalite(BuySelectionPaymentTerm m) {
        return new ModaliteDemandeAchat(m.getId(), m.getSorting(), m.getCode(), m.getRatio());
    }

    static LocalDate jour(LocalDateTime t) {
        return t == null ? null : t.toLocalDate();
    }

    private static LocalDateTime debut(LocalDate jour) {
        return jour == null ? null : jour.atStartOfDay();
    }

    private static void longueur(String valeur, int max, String champ) {
        if (valeur != null && valeur.length() > max) {
            throw new InvalidRequestException(champ + " : " + max + " caractères au plus");
        }
    }
}
