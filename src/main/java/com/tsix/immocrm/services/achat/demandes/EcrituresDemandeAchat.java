package com.tsix.immocrm.services.achat.demandes;

import com.tsix.immocrm.dto.achat.demandes.DatesDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.ExecutionTransitionDemandeAchat;
import com.tsix.immocrm.dto.achat.demandes.SaisieDemandeAchat;
import com.tsix.immocrm.entities.adm.AdmModule;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.buy.BuySelection;
import com.tsix.immocrm.entities.buy.BuySelectionKind;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.prj.PrjMarketType;
import com.tsix.immocrm.entities.stl.StlCategory;
import com.tsix.immocrm.entities.stl.StlCurrency;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import com.tsix.immocrm.services.workflow.MoteurWorkflowStr;
import com.tsix.immocrm.utils.Champs;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Création, modification d'en-tête, suppression et transition d'une DIA (FEATURE-n6-demandes-achat.md R1-R7, R12, R14, D4). */
@Component
class EcrituresDemandeAchat {

    private static final SaisieDemandeAchat VIDE = new SaisieDemandeAchat(null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null);
    private static final String SANS_STATUT = "Aucun Statut n'est sélectionné ou vous n'avez pas les droits sur les états de création "
            + "de l'entité en cours !";
    // Filles sans FK CASCADE dans la base de test : effacées avant la demande, enfants avant parents.
    private static final List<String> FILLES = List.of("DELETE FROM BuySelectionStockLine WHERE BuySelectionId = :id",
            "DELETE FROM BuySelectionLine WHERE BuySelectionId = :id", "DELETE FROM BuySelectionGroup WHERE BuySelectionId = :id",
            "DELETE FROM BuySelectionPaymentTerm WHERE BuySelectionId = :id",
            "DELETE FROM BuySelectionSupplier WHERE BuySelectionId = :id");

    private final EntityManager em;
    private final MoteurWorkflowStr moteur;
    private final PerimetreProjetService perimetres;
    private final UtilisateurAffiche utilisateur;
    private final Clock clock;
    private final GenerationBordereauLot generation;

    EcrituresDemandeAchat(EntityManager em, MoteurWorkflowStr moteur, PerimetreProjetService perimetres,
                          UtilisateurAffiche utilisateur, Clock clock, GenerationBordereauLot generation) {
        this.em = em;
        this.moteur = moteur;
        this.perimetres = perimetres;
        this.utilisateur = utilisateur;
        this.clock = clock;
        this.generation = generation;
    }

    BuySelection demande(UUID id) {
        return em.find(BuySelection.class, id);
    }

    /** R1, R3, R6 (400) puis R2 (403) ; rien n'est écrit avant le dernier contrôle. */
    UUID creer(SaisieDemandeAchat saisie) {
        SaisieDemandeAchat r = Objects.requireNonNullElse(saisie, VIDE);
        BuySelectionKind procedure = valider(r);
        StrEntity entite = entiteDe(procedure);
        StrStatus depart = depart(entite);
        BuySelection d = new BuySelection();
        d.setId(UUID.randomUUID());
        d.setCode(codeLibre());
        d.setGroupId(d.getId());
        d.setIsTheLast(true);
        d.setProcedure(procedure);
        d.setStrEntity(entite);
        d.setStrStatus(depart);
        d.setCfgCompanyId(r.societeId());
        appliquer(d, r);
        persister(d);
        return d.getId();
    }

    /** Nouvelle DIA écrite avec sa ligne d'historique « Création » (création, R18, R19). */
    void persister(BuySelection d) {
        d.setUserCreate(utilisateur.courant().nom());
        d.setUserCreateDateTime(LocalDateTime.now(clock));
        em.persist(d);
        moteur.historiser(d, MoteurWorkflowStr.CREATION, null, null);
        em.flush();
    }

    /** R5 : procédure changée seulement en statut Nouveau ; nouvelle entité → statut de départ, sans historique. */
    void modifier(UUID id, SaisieDemandeAchat saisie) {
        SaisieDemandeAchat r = Objects.requireNonNullElse(saisie, VIDE);
        BuySelectionKind procedure = valider(r);
        BuySelection d = derniere(id);
        StrEntity entite = d.getStrEntity();
        StrStatus statut = d.getStrStatus();
        if (!procedure.getId().equals(Champs.de(d.getProcedure(), BuySelectionKind::getId))) {
            if (!MoteurWorkflowStr.TYPE_NOUVEAU.equals(Champs.de(statut, StrStatus::getStrStatusTypeId))) {
                throw new ConflictException("La procédure ne peut être changée que dans un statut de type Nouveau.");
            }
            StrEntity nouvelle = entiteDe(procedure);
            if (!nouvelle.getId().equals(Champs.de(entite, StrEntity::getId))) {
                entite = nouvelle;
                statut = depart(nouvelle);
            }
        }
        d.setProcedure(procedure);
        d.setStrEntity(entite);
        d.setStrStatus(statut);
        d.setCfgCompanyId(r.societeId());
        appliquer(d, r);
        d.setUserUpdate(utilisateur.courant().nom());
        d.setUserUpdateDateTime(LocalDateTime.now(clock));
        em.flush();
    }

    /** R7, R14, puis pièce d'achat liée (NO_ACTION) : 409 avant toute écriture. */
    void supprimer(UUID id) {
        BuySelection d = derniere(id);
        moteur.verifierSuppression(d);
        if (em.createQuery("select count(b) from BuyDocument b where b.buySelectionId = :id", Long.class).setParameter("id", id)
                .getSingleResult() > 0) {
            throw new ConflictException("Cette demande est citée par une pièce d'achat et ne peut pas être supprimée.");
        }
        FILLES.forEach(sql -> em.createNativeQuery(sql).setParameter("id", id).executeUpdate());
        em.remove(d);
        em.flush();
    }

    /** R12 ; hook Fin succès sur un lot non multi : génération du bordereau définitif du lot (R20). */
    void executer(UUID id, ExecutionTransitionDemandeAchat e) {
        if (e == null || e.transitionId() == null) {
            throw new InvalidRequestException("transitionId : obligatoire");
        }
        BuySelection d = derniere(id);
        moteur.executer(d, new MoteurWorkflowStr.Execution(e.transitionId(), e.notes(), e.dateUtilisateur(), e.secondValideur()),
                cible -> {
                    PrjMarket lot = d.getPrjMarketId() == null ? null : em.find(PrjMarket.class, d.getPrjMarketId());
                    if (MoteurWorkflowStr.TYPE_FIN_SUCCES.equals(cible.getStrStatusTypeId()) && lot != null
                            && !Boolean.TRUE.equals(lot.getIsMultiSupplier())) {
                        generation.generer(d, lot);
                    }
                });
        d.setUserUpdate(utilisateur.courant().nom());
        d.setUserUpdateDateTime(LocalDateTime.now(clock));
        em.flush();
    }

    /** R7 : une DIA qui n'est plus la dernière de son groupe est en lecture seule. */
    BuySelection derniere(UUID id) {
        BuySelection d = demande(id);
        if (!Boolean.TRUE.equals(d.getIsTheLast())) {
            throw new ConflictException("Cette demande n'est plus la dernière de son groupe : elle est en lecture seule.");
        }
        return d;
    }

    /** R1, références, longueurs, périmètre de la société et du projet, R6 ; renvoie la procédure. */
    private BuySelectionKind valider(SaisieDemandeAchat r) {
        BuySelectionKind procedure = existant(BuySelectionKind.class, obligatoire(r.procedureId(), "procedureId"), "procedureId");
        if (!perimetres.achat(List.of()).societes().contains(obligatoire(r.societeId(), "societeId"))) {
            throw new InvalidRequestException("societeId : société non habilitée");
        }
        if (r.description() == null || r.description().isBlank()) {
            throw new InvalidRequestException("description : obligatoire");
        }
        longueur(r.description().trim(), 255, "description");
        longueur(r.descriptionLongue(), 500, "descriptionLongue");
        longueur(r.notes(), 1000, "notes");
        if (r.projetId() != null && !perimetres.estVisible(r.projetId())) {
            throw new InvalidRequestException("projetId : projet non habilité");
        }
        existant(PrjMarketType.class, r.typeLotId(), "typeLotId");
        existant(StlCategory.class, r.categorieId(), "categorieId");
        existant(StlCurrency.class, r.deviseId(), "deviseId");
        existant(AdmModule.class, r.moduleId(), "moduleId");
        existant(CfgTier.class, r.declencheurId(), "declencheurId");
        existant(StlSettlementType.class, r.paiementId(), "paiementId");
        verifierMontant(procedure, r.prixEstimeTTC());
        return procedure;
    }

    /** R6 : la procédure attendue pour le montant ne doit pas dépasser la procédure choisie. */
    private void verifierMontant(BuySelectionKind procedure, BigDecimal ttc) {
        if (Boolean.TRUE.equals(procedure.getIsWithoutVerification())) {
            return;
        }
        BigDecimal attendue = procedureParMontant(Objects.requireNonNullElse(ttc, BigDecimal.ZERO))
                .map(BuySelectionKind::getMaxAmount).orElse(null);
        if (attendue != null && attendue.compareTo(Objects.requireNonNullElse(procedure.getMaxAmount(), BigDecimal.ZERO)) > 0) {
            throw new InvalidRequestException("La procédure d'achat selectionnée ne gère pas le montant estimé pour cette demande d'achat");
        }
    }

    /** {@code BuySelectionKind.SelectKindByAmount} : plus petit plafond qui couvre le montant, sinon le plus grand (R6, R18). */
    Optional<BuySelectionKind> procedureParMontant(BigDecimal montant) {
        List<BuySelectionKind> parPlafond = em.createQuery("select k from BuySelectionKind k order by k.maxAmount, k.code",
                BuySelectionKind.class).getResultList();
        return parPlafond.stream().filter(k -> k.getMaxAmount() != null && k.getMaxAmount().compareTo(montant) >= 0).findFirst()
                .or(() -> parPlafond.stream().reduce((a, b) -> b));
    }

    /** R3. */
    static StrEntity entiteDe(BuySelectionKind procedure) {
        return Optional.ofNullable(procedure.getStrEntityManagment()).orElseThrow(() -> new InvalidRequestException(SANS_STATUT));
    }

    /** R2 : premier statut Nouveau visible de l'appelant. */
    StrStatus depart(StrEntity entite) {
        return moteur.statutDeDepart(entite.getId()).orElseThrow(() -> new ForbiddenException(
                "Vous n'avez pas les droits sur les états de création de l'entité '%s' !".formatted(entite.getDescription())));
    }

    private void appliquer(BuySelection d, SaisieDemandeAchat r) {
        DatesDemandeAchat dates = Objects.requireNonNullElse(r.dates(),
                new DatesDemandeAchat(null, null, null, null, null, null, null, null));
        d.setDescription(r.description().trim());
        d.setDocDate(Objects.requireNonNullElseGet(r.date(), () -> LocalDate.now(clock)).atStartOfDay());
        d.setPrjProjectId(r.projetId());
        d.setPrjMarketTypeId(r.typeLotId());
        d.setStlCategoryId(r.categorieId());
        d.setStlCurrencyId(r.deviseId());
        d.setAdmModuleId(r.moduleId() != null ? r.moduleId() : Objects.requireNonNullElseGet(d.getAdmModuleId(), this::moduleAchat));
        d.setCfgTierTriggerId(r.declencheurId() != null ? r.declencheurId()
                : Objects.requireNonNullElseGet(d.getCfgTierTriggerId(), this::employeCourant));
        d.setDescriptionLong(r.descriptionLongue());
        d.setDefaultVat(r.tvaDefaut());
        d.setPriceEstimatedHT(r.prixEstimeHT());
        d.setPriceEstimatedTTC(r.prixEstimeTTC());
        d.setStlSettlementTypeId(r.paiementId());
        d.setPaymentDelay(r.delaiPaiement());
        d.setNotes(r.notes());
        d.setTextLetterSelection(r.texteLettre());
        d.setLaunchOfTenderDate(debut(dates.lancement()));
        d.setWithdrawalTenderDocumentsDate(debut(dates.retraitDossier()));
        d.setTenderMaturityDate(debut(dates.echeanceOffres()));
        d.setTendersResortDate(debut(dates.report()));
        d.setBidOpeningDate(debut(dates.ouverturePlis()));
        d.setTendersReportDate(debut(dates.rapportDepouillement()));
        d.setTenderDate(debut(dates.adjudication()));
        d.setContractSignatureDate(debut(dates.signatureMarche()));
    }

    UUID moduleAchat() {
        return em.createQuery("select m.id from AdmModule m where m.code = 'BUY' order by m.id", UUID.class).setMaxResults(1)
                .getResultStream().findFirst().orElse(null);
    }

    UUID employeCourant() {
        return Champs.de(utilisateur.courant().compte(), AdmUser::getCfgEmployee, CfgTier::getId);
    }

    /** D4 / décision 13 : {@code yy/} de l'année + plus grand numéro toutes années confondues + 1, sous verrou. */
    String codeLibre() {
        String prefixe = "%02d/".formatted(LocalDate.now(clock).getYear() % 100);
        Number max = (Number) em.createNativeQuery("SELECT MAX(CAST(SUBSTRING(Code, 4, 5) AS int)) FROM BuySelection "
                        + "WITH (UPDLOCK, HOLDLOCK) WHERE Code LIKE :motif")
                .setParameter("motif", "[0-9][0-9]/[0-9][0-9][0-9][0-9][0-9]").getSingleResult();
        return prefixe + "%05d".formatted(max == null ? 1 : max.intValue() + 1);
    }

    private static LocalDateTime debut(LocalDate jour) {
        return jour == null ? null : jour.atStartOfDay();
    }

    private <T> T existant(Class<T> classe, UUID id, String champ) {
        if (id == null) {
            return null;
        }
        return Optional.ofNullable(em.find(classe, id)).orElseThrow(() -> new InvalidRequestException(champ + " : introuvable"));
    }

    private static <T> T obligatoire(T valeur, String champ) {
        if (valeur == null) {
            throw new InvalidRequestException(champ + " : obligatoire");
        }
        return valeur;
    }

    private static void longueur(String valeur, int max, String champ) {
        if (valeur != null && valeur.length() > max) {
            throw new InvalidRequestException(champ + " : " + max + " caractères au plus");
        }
    }
}
