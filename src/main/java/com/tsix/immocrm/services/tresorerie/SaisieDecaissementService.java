package com.tsix.immocrm.services.tresorerie;

import com.tsix.immocrm.domain.StatutReglement;
import com.tsix.immocrm.domain.achat.CalculReglement;
import com.tsix.immocrm.dto.tresorerie.CodeFeuilleDto;
import com.tsix.immocrm.dto.tresorerie.DecaissementFicheDto;
import com.tsix.immocrm.dto.tresorerie.DecaissementFicheDto.LigneDecaissementDto;
import com.tsix.immocrm.dto.tresorerie.FiltresTresorerie.ModuleTresorerie;
import com.tsix.immocrm.dto.tresorerie.LigneSaisieDecaissementDto;
import com.tsix.immocrm.dto.tresorerie.PieceAAffecterDto;
import com.tsix.immocrm.dto.tresorerie.ReferentielsDecaissementDto;
import com.tsix.immocrm.dto.tresorerie.ReferentielsDecaissementDto.DroitsDecaissementDto;
import com.tsix.immocrm.dto.tresorerie.SaisieDecaissementDto;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stl.StlAccount;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementHistory;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.ForbiddenException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.services.achat.RequetesSaisieDecaissement;
import com.tsix.immocrm.services.achat.RequetesSaisieDecaissement.PieceSaisie;
import com.tsix.immocrm.services.achat.TresorerieService;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import com.tsix.immocrm.services.sequence.GenerateurCodeSequence;
import com.tsix.immocrm.services.tresorerie.RetenuesDecaissement.Retenues;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

/** Écriture d'un décaissement PRJ / BUY (FEATURE-n3bis-saisie-reglements.md §5) : tout contrôlé avant le 1er setter. */
@Service
@Transactional
public class SaisieDecaissementService {

    private static final String DROIT = "Yes.Stl.Front.Debit.SettlementList";
    private static final String DROIT_CREER = DROIT + ".toolNew";
    private static final String DROIT_SUPPRIMER = DROIT + ".toolDelete";
    private static final String DROIT_SANS_AFFECTATION = DROIT + ".CanSaveWithoutAffectation";
    private static final String DROIT_SOCIETE = DROIT + ".CanModifyCompany";

    private static final String MSG_SANS_LIGNE =
            "Vous ne pouvez pas enregistrer la pièce de règlement sans aucune lignes affectés !";
    private static final String MSG_RESTE = "Le reste à affecter doit être >= 0 !";
    private static final String MSG_LIGNE_ZERO =
            "Vous ne pouvez pas enregistrer la pièce de règlement avec des lignes qui ont un montant affecté=0 !";
    private static final String MSG_DEVISE = "Toutes les affectations doivent se faire avec la même devise de référence !";
    private static final String MSG_POINTAGE_SANS_COMPTE =
            "Si votre pièce possède une date de pointage, il est obligatoire de lui affecter un compte !";
    private static final String MSG_ECHEANCE = "La date échéance doit être supérieure ou égale à la date de la pièce !";
    private static final String MSG_POINTAGE_STATUT =
            "Si la pièce possède une date de pointage il faut que le statut soit égal à pointé !";
    private static final String MSG_DATE_POINTAGE = "La date de pointage est obligatoire !";
    private static final String MSG_REFERENCE = "Le champ référence est obligatoire !";
    private static final String MSG_NOTAIRE = "Le champ Notaire/Avocat est obligatoire !";
    private static final String MSG_COMPTE = "Le champ compte est obligatoire !";
    private static final String MSG_POINTE = "Vous ne pouvez pas supprimer un règlement pointé en comptabilité !";
    private static final String MSG_AFFECTATIONS = "Vous ne pouvez pas supprimer un règlement avec des affectations !";
    private static final String MSG_VIREMENT = "Ce règlement provient d'un virement interne : modifiez le virement.";
    private static final String MSG_NON_VALIDEE =
            "Vous ne pouvez pas effectuer un paiement sur une pièce qui n'est pas validée !";
    private static final String MSG_PIECE_INTROUVABLE = "Pièce d'achat introuvable : ";
    private static final String MSG_SANS_SEQUENCE = "Aucune séquence de code n'est configurée";
    private static final UUID SEQUENCE_PP = UUID.fromString("60C4F768-BE0C-49AA-A5FD-F784D28F2F0A");
    private static final String SEQUENCE_CRAS = "RSSheetCode";
    private static final List<StatutReglement> STATUTS_SAISIS =
            List.of(StatutReglement.IN_WORK, StatutReglement.POINTED, StatutReglement.NOTPAYED);

    private final TresorerieService tresorerie;
    private final RequetesSaisieDecaissement requetes;
    private final PerimetreProjetService perimetres;
    private final OutilsSaisieDecaissement outils;
    private final RetenuesDecaissement retenues;
    private final GenerateurCodeSequence sequences;
    private final EntityManager em;

    public SaisieDecaissementService(TresorerieService tresorerie, RequetesSaisieDecaissement requetes,
                                     PerimetreProjetService perimetres, OutilsSaisieDecaissement outils,
                                     RetenuesDecaissement retenues, GenerateurCodeSequence sequences,
                                     EntityManager em) {
        this.tresorerie = tresorerie;
        this.requetes = requetes;
        this.perimetres = perimetres;
        this.outils = outils;
        this.retenues = retenues;
        this.sequences = sequences;
        this.em = em;
    }

    /** En-tête saisi, résolu et contrôlé. */
    private record Entete(StatutReglement statut, CfgCompany societe, StlSettlementType type, CfgTier fournisseur,
                          CfgTier payeur, CfgTier banque, CfgTier notaire, CfgTier tiersAutrui, StlAccount compte,
                          BigDecimal montant, LocalDateTime dateDocument, LocalDateTime dateEcheance,
                          LocalDateTime datePointage, String reference) {
    }

    public DecaissementFicheDto creer(SaisieDecaissementDto saisie) {
        outils.droits().exiger(DROIT, DROIT_CREER);
        ModuleTresorerie module = requis(saisie.module(), "module est requis");
        UUID moduleId = requis(requetes.module(module.name()), "Module introuvable : " + module);
        Entete entete = entete(saisie, null);
        validerLignes(saisie, entete, null, outils.references().deviseParDefaut(), moduleId, BigDecimal.ZERO);
        Retenues r = retenues.preparer(saisie.retenues(), null, entete.fournisseur(), entete.montant());

        StlSettlement reglement = new StlSettlement();
        reglement.setId(UUID.randomUUID());
        reglement.setIsCredit(false);
        LocalDateTime maintenant = LocalDateTime.now(outils.clock());
        String auteur = auteur();
        reglement.setUserCreate(auteur);
        reglement.setUserCreateDateTime(maintenant);
        appliquer(reglement, entete, saisie, auteur, maintenant);
        outils.reglementOmran().completer(reglement, entete.societe(), moduleId);
        // completer met "" (encaissements) ; au décaissement, codes NULL tant que les POST ne les attribuent pas (RS-o)
        reglement.setPropositionSheetCode(null);
        reglement.setRSSheetCode(null);
        retenues.poser(reglement, entete.montant(), r, saisie.rsDeclaree(), saisie.dateDeclarationRs());
        em.persist(reglement);
        retenues.ecrire(reglement.getId(), r, auteur, maintenant);
        ecrireLignes(reglement, saisie.lignes(), auteur, maintenant);
        historiser(reglement, maintenant, auteur);
        em.flush();
        return tresorerie.decaissement(reglement.getId());
    }

    public DecaissementFicheDto modifier(UUID id, SaisieDecaissementDto saisie) {
        DecaissementFicheDto fiche = tresorerie.decaissement(id);
        outils.droits().exiger(DROIT_CREER);
        StlSettlement reglement = em.find(StlSettlement.class, id);
        if (requetes.virementInterne(id)) {
            throw new ConflictException(MSG_VIREMENT);
        }
        UUID societeAvant = reglement.getCfgCompany() == null ? null : reglement.getCfgCompany().getId();
        if (!Objects.equals(societeAvant, saisie.societeId())
                && !outils.droits().parmi(DROIT_SOCIETE).contains(DROIT_SOCIETE)) {
            throw new ForbiddenException("Vous n'avez pas le droit de changer la société du règlement.");
        }
        Set<UUID> visibles = fiche.lignes().stream().map(LigneDecaissementDto::id).collect(Collectors.toSet());
        List<StlSettlementLine> lignes = lignes(id);
        BigDecimal masque = CalculReglement.affecte(lignes.stream().filter(l -> !visibles.contains(l.getId()))
                .map(StlSettlementLine::getAmount).toList());
        Entete entete = entete(saisie, reglement);
        // règlement sans devise (saisi hors CRM) : la devise de référence est celle par défaut
        UUID devise = requireNonNullElse(reglement.getStlCurrencyId(), outils.references().deviseParDefaut());
        BigDecimal affecte = validerLignes(saisie, entete, id, devise, reglement.getAdmModuleId(), masque);

        Retenues r = retenues.preparer(saisie.retenues(), id, entete.fournisseur(), entete.montant());
        BigDecimal resteAvant = CalculReglement.resteAAffecter(
                requireNonNullElse(reglement.getAmountBase(), reglement.getAmount()),
                CalculReglement.affecte(lignes.stream().map(StlSettlementLine::getAmount).toList()));
        boolean comptaChange = !egal(resteAvant, CalculReglement.resteAAffecter(entete.montant(), affecte))
                || !Objects.equals(reglement.getSettlementDate(), entete.datePointage())
                || !Objects.equals(idDe(reglement.getStlSettlementType(), StlSettlementType::getId), entete.type().getId())
                || !Objects.equals(idDe(reglement.getStlAccount(), StlAccount::getId), idDe(entete.compte(), StlAccount::getId));
        boolean historique = !Objects.equals(reglement.getEcheanceDate(), entete.dateEcheance())
                || !Objects.equals(idDe(reglement.getStlSettlementType(), StlSettlementType::getId), entete.type().getId())
                || !Objects.equals(requireNonNullElse(reglement.getCode(), ""), entete.reference());

        LocalDateTime maintenant = LocalDateTime.now(outils.clock());
        String auteur = auteur();
        appliquer(reglement, entete, saisie, auteur, maintenant);
        reglement.setCfgCompany(entete.societe());
        reglement.setAmountBase(entete.montant());
        reglement.setAmountBaseConverted(entete.montant());
        retenues.poser(reglement, entete.montant(), r, saisie.rsDeclaree(), saisie.dateDeclarationRs());
        retenues.ecrire(id, r, auteur, maintenant);
        if (comptaChange) {
            reglement.setIsAccounted(false);
        }
        lignes.stream().filter(l -> visibles.contains(l.getId())).forEach(em::remove);
        ecrireLignes(reglement, saisie.lignes(), auteur, maintenant);
        if (historique) {
            historiser(reglement, maintenant, auteur);
        }
        em.flush();
        return tresorerie.decaissement(id);
    }

    public void supprimer(UUID id) {
        tresorerie.decaissement(id);
        outils.droits().exiger(DROIT_SUPPRIMER);
        StlSettlement reglement = em.find(StlSettlement.class, id);
        if (requetes.virementInterne(id)) {
            throw new ConflictException(MSG_VIREMENT);
        }
        if (reglement.getSettlementDate() != null) {
            throw new ConflictException(MSG_POINTE);
        }
        if (!lignes(id).isEmpty()) {
            throw new ConflictException(MSG_AFFECTATIONS);
        }
        retenues.supprimer(id);
        em.flush();
        List<String> tables = requetes.tablesQuiReferencent(id);
        if (!tables.isEmpty()) {
            throw new ConflictException("Ce règlement est référencé par " + String.join(", ", tables)
                    + " et ne peut pas être supprimé.");
        }
        em.createQuery("SELECT h FROM StlSettlementHistory h WHERE h.stlSettlementId = :id", StlSettlementHistory.class)
                .setParameter("id", id).getResultList().forEach(em::remove);
        em.flush();
        em.remove(reglement);
    }

    @Transactional(readOnly = true)
    public ReferentielsDecaissementDto referentiels(ModuleTresorerie module) {
        outils.droits().exiger(DROIT);
        Set<UUID> societes = perimetres.achat(List.of()).societes();
        Set<String> droits = outils.droits().parmi(DROIT_CREER, DROIT_SUPPRIMER, DROIT_SANS_AFFECTATION, DROIT_SOCIETE);
        return new ReferentielsDecaissementDto(requetes.types(), requetes.societes(societes),
                requetes.comptes(societes), requetes.devise(outils.references().deviseParDefaut()),
                STATUTS_SAISIS.stream().map(StatutReglement::name).toList(),
                new DroitsDecaissementDto(droits.contains(DROIT_CREER), droits.contains(DROIT_SUPPRIMER),
                        droits.contains(DROIT_SANS_AFFECTATION), droits.contains(DROIT_SOCIETE)),
                retenues.types(), retenues.gestionRs());
    }

    public CodeFeuilleDto codeFeuillePaiement(UUID id) {
        StlSettlement reglement = reglementPourCode(id);
        if (vide(reglement.getPropositionSheetCode())) {
            reglement.setPropositionSheetCode(sequences.suivantParId(SEQUENCE_PP, MSG_SANS_SEQUENCE + "."));
            em.flush();
        }
        return new CodeFeuilleDto(reglement.getPropositionSheetCode());
    }

    public CodeFeuilleDto codeFeuilleRs(UUID id) {
        StlSettlement reglement = reglementPourCode(id);
        if (vide(reglement.getRSSheetCode())) {
            UUID societe = idDe(reglement.getCfgCompany(), CfgCompany::getId);
            reglement.setRSSheetCode(sequences.suivantParType(SEQUENCE_CRAS, societe,
                    MSG_SANS_SEQUENCE + " pour cette société."));
            em.flush();
        }
        return new CodeFeuilleDto(reglement.getRSSheetCode());
    }

    private StlSettlement reglementPourCode(UUID id) {
        tresorerie.decaissement(id);
        outils.droits().exiger(DROIT_CREER);
        return em.find(StlSettlement.class, id);
    }

    private static boolean vide(String code) {
        return code == null || code.isBlank();
    }

    @Transactional(readOnly = true)
    public List<PieceAAffecterDto> piecesAAffecter(ModuleTresorerie module, UUID fournisseurId, UUID societeId,
                                                   UUID reglementId) {
        outils.droits().exiger(DROIT);
        requis(module, "module est requis");
        requis(fournisseurId, "fournisseurId est requis");
        requis(societeId, "societeId est requis");
        if (reglementId != null) {
            tresorerie.decaissement(reglementId);
        }
        return requetes.piecesAAffecter(perimetres.achat(List.of()), module.name(), fournisseurId, societeId,
                        reglementId).stream()
                .filter(p -> p.resteAPayer().signum() > 0)
                .map(p -> new PieceAAffecterDto(p.id(), p.code(), p.dateDocument(), p.dateEcheance(), p.description(),
                        p.aPayer(), p.regle(), p.resteAPayer(), p.deviseCode()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SaisieDecaissementDto proposition(UUID pieceId) {
        requis(pieceId, "pieceId est requis");
        PieceSaisie piece = requetes.pieces(perimetres.achat(List.of()), Set.of(pieceId), null).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PIECE_INTROUVABLE + pieceId));
        ModuleTresorerie module = Arrays.stream(ModuleTresorerie.values())
                .filter(m -> m.name().equals(piece.moduleCode())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(MSG_PIECE_INTROUVABLE + pieceId));
        outils.droits().exiger(DROIT, DROIT_CREER);
        exigerValidee(piece);
        LocalDate jour = LocalDate.now(outils.clock());
        // G-c comme OMRAN : la ligne vaut le reste même s'il est nul, l'enregistrement la refusera.
        return new SaisieDecaissementDto(module, piece.societeId(), jour, jour, null, null, null, null,
                piece.resteAPayer(), piece.fournisseurId(), piece.fournisseurId(), null, null, null, null,
                StatutReglement.IN_WORK, null, null, null,
                List.of(new LigneSaisieDecaissementDto(piece.id(), piece.resteAPayer())), null, false, null,
                piece.resteAPayer(), BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public SaisieDecaissementDto copie(UUID id) {
        SaisieDecaissementDto s = tresorerie.decaissement(id).saisie();
        outils.droits().exiger(DROIT, DROIT_CREER);
        // Comme Settlement.Copy d'OMRAN : référence, reçu, P.P et C.R.A.S vidés, aucune ligne copiée.
        return new SaisieDecaissementDto(s.module(), s.societeId(), s.dateDocument(), s.dateEcheance(), s.typeId(),
                null, s.compteId(), s.banqueId(), s.montant(), s.fournisseurId(), s.payeurId(), s.notaireId(),
                s.tiersAutruiId(), s.notes(), s.dateDecaissement(), s.statut(), null, null, null, List.of(),
                null, false, null, s.montant(), BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** PUT : statut absent = statut existant ; un règlement annulé reste annulé (pas de réactivation). */
    private static StatutReglement statut(StatutReglement saisi, StlSettlement existant) {
        StatutReglement avant = existant == null ? null : StatutReglement.lireCode(existant.getStatus()).orElse(null);
        StatutReglement statut = requireNonNullElse(saisi, requireNonNullElse(avant, StatutReglement.IN_WORK));
        if (avant == StatutReglement.CANCELED && statut == StatutReglement.CANCELED) {
            return statut;
        }
        if (avant == StatutReglement.CANCELED) {
            throw new InvalidRequestException("Un règlement annulé ne peut pas être réactivé.");
        }
        if (!STATUTS_SAISIS.contains(statut)) {
            throw new InvalidRequestException("Statut non saisissable : " + statut);
        }
        return statut;
    }

    private CfgTier payeur(UUID payeurId, CfgTier fournisseur, StlSettlement existant) {
        if (payeurId != null) {
            return trouver(CfgTier.class, payeurId, "Payeur");
        }
        return existant == null ? fournisseur : requireNonNullElse(existant.getTierPayer(), fournisseur);
    }

    private Entete entete(SaisieDecaissementDto saisie, StlSettlement existant) {
        StatutReglement statut = statut(saisie.statut(), existant);
        BigDecimal montant = requis(saisie.montant(), "montant est requis");
        if (montant.signum() <= 0) {
            throw new InvalidRequestException("montant doit être strictement positif");
        }
        CfgCompany societe = societe(saisie.societeId());
        StlSettlementType type = trouver(StlSettlementType.class, requis(saisie.typeId(), "typeId est requis"), "Type");
        CfgTier fournisseur = em.find(CfgTier.class, requis(saisie.fournisseurId(), "fournisseurId est requis"));
        if (fournisseur == null) {
            throw new ResourceNotFoundException("Fournisseur introuvable : " + saisie.fournisseurId());
        }
        LocalDate jourDocument = requireNonNullElse(saisie.dateDocument(), LocalDate.now(outils.clock()));
        LocalDate jourEcheance = requireNonNullElse(saisie.dateEcheance(), jourDocument);
        if (jourEcheance.isBefore(jourDocument)) {
            throw new InvalidRequestException(MSG_ECHEANCE);
        }
        LocalDateTime datePointage = saisie.dateDecaissement() == null ? null : saisie.dateDecaissement().atStartOfDay();
        verifierPointage(statut, datePointage, saisie.compteId());
        String reference = requireNonNullElse(saisie.reference(), "").trim();
        verifierObligatoires(type, reference, saisie);
        StlAccount compte = compte(saisie.compteId(), societe, datePointage, existant);
        if (Boolean.TRUE.equals(type.getIsReferenceUnique()) && !reference.isEmpty()
                && requetes.referencePrise(reference, type.getId(), saisie.compteId(), idDe(existant, StlSettlement::getId))) {
            throw new ConflictException("Cette référence existe déjà pour ce type et ce compte : " + reference);
        }
        CfgTier payeur = payeur(saisie.payeurId(), fournisseur, existant);
        return new Entete(statut, societe, type, fournisseur, payeur, tiers(saisie.banqueId(), "Banque"),
                tiers(saisie.notaireId(), "Notaire"), tiers(saisie.tiersAutruiId(), "Tiers autrui"), compte, montant,
                jourDocument.atStartOfDay(), jourEcheance.atStartOfDay(), datePointage, reference);
    }

    private static void verifierPointage(StatutReglement statut, LocalDateTime datePointage, UUID compteId) {
        if (datePointage != null && statut != StatutReglement.POINTED) {
            throw new InvalidRequestException(MSG_POINTAGE_STATUT);
        }
        if (datePointage == null && statut == StatutReglement.POINTED) {
            throw new InvalidRequestException(MSG_DATE_POINTAGE);
        }
        if (datePointage != null && compteId == null) {
            throw new InvalidRequestException(MSG_POINTAGE_SANS_COMPTE);
        }
    }

    private static void verifierObligatoires(StlSettlementType type, String reference, SaisieDecaissementDto saisie) {
        if (Boolean.TRUE.equals(type.getIsReferenceMandatory()) && reference.isEmpty()) {
            throw new InvalidRequestException(MSG_REFERENCE);
        }
        if (Boolean.TRUE.equals(type.getIsLawyerMandatory()) && saisie.notaireId() == null) {
            throw new InvalidRequestException(MSG_NOTAIRE);
        }
        if (Boolean.TRUE.equals(type.getIsBankMandatory()) && saisie.compteId() == null) {
            throw new InvalidRequestException(MSG_COMPTE);
        }
    }

    private CfgCompany societe(UUID societeId) {
        requis(societeId, "societeId est requis");
        if (!perimetres.achat(List.of()).societes().contains(societeId)) {
            throw new ResourceNotFoundException("Société non habilitée : " + societeId);
        }
        return trouver(CfgCompany.class, societeId, "Société");
    }

    /** Le rapprochement n'est contrôlé que si la date de pointage est posée ou change (sonde : pas de setter avant). */
    private StlAccount compte(UUID compteId, CfgCompany societe, LocalDateTime datePointage, StlSettlement existant) {
        if (compteId == null) {
            return null;
        }
        LocalDateTime pointageAvant = existant == null ? null : existant.getSettlementDate();
        if (datePointage != null && !datePointage.equals(pointageAvant)) {
            StlSettlement sonde = new StlSettlement();
            sonde.setCfgCompany(societe);
            return outils.pointage().verifier(sonde, compteId, datePointage);
        }
        StlAccount compte = trouver(StlAccount.class, compteId, "Compte");
        if (!Objects.equals(societe.getId(), idDe(compte.getCfgCompany(), CfgCompany::getId))) {
            throw new InvalidRequestException("Le compte " + compte.getCode()
                    + " n'appartient pas à la société du règlement.");
        }
        return compte;
    }

    /** Σ des lignes saisies + masquées ; 400 sur chaque règle de ligne (R-a…R-d, reste des pièces). */
    private BigDecimal validerLignes(SaisieDecaissementDto saisie, Entete entete, UUID reglementId, UUID devise,
                                     UUID module, BigDecimal masque) {
        List<LigneSaisieDecaissementDto> lignes = saisie.lignes();
        if (lignes.isEmpty() && masque.signum() == 0
                && !outils.droits().parmi(DROIT_SANS_AFFECTATION).contains(DROIT_SANS_AFFECTATION)) {
            throw new InvalidRequestException(MSG_SANS_LIGNE);
        }
        Set<UUID> ids = new HashSet<>();
        for (LigneSaisieDecaissementDto ligne : lignes) {
            requis(ligne.pieceId(), "pieceId est requis");
            if (ligne.montant() == null || ligne.montant().signum() <= 0 || CalculReglement.estNul(ligne.montant())) {
                throw new InvalidRequestException(MSG_LIGNE_ZERO);
            }
            if (!ids.add(ligne.pieceId())) {
                throw new InvalidRequestException("La pièce " + ligne.pieceId() + " est affectée deux fois.");
            }
        }
        requetes.verrouiller(ids);
        Map<UUID, PieceSaisie> pieces = requetes.pieces(perimetres.achat(List.of()), ids, reglementId).stream()
                .collect(Collectors.toMap(PieceSaisie::id, Function.identity()));
        for (LigneSaisieDecaissementDto ligne : lignes) {
            verifierLigne(ligne, pieces.get(ligne.pieceId()), entete, devise, module);
        }
        BigDecimal affecte = CalculReglement.affecte(lignes.stream().map(LigneSaisieDecaissementDto::montant).toList())
                .add(masque);
        if (CalculReglement.depasse(entete.montant(), affecte)) {
            throw new InvalidRequestException(MSG_RESTE);
        }
        return affecte;
    }

    private static void verifierLigne(LigneSaisieDecaissementDto ligne, PieceSaisie piece, Entete entete, UUID devise,
                                      UUID module) {
        if (piece == null) {
            throw new ResourceNotFoundException(MSG_PIECE_INTROUVABLE + ligne.pieceId());
        }
        exigerValidee(piece);
        if (!Objects.equals(piece.societeId(), entete.societe().getId())) {
            throw new InvalidRequestException("La pièce " + piece.code() + " n'appartient pas à la société du règlement.");
        }
        if (!Objects.equals(piece.moduleId(), module)) {
            throw new InvalidRequestException("La pièce " + piece.code() + " n'est pas du module du règlement.");
        }
        if (!Objects.equals(piece.fournisseurId(), entete.fournisseur().getId())) {
            throw new InvalidRequestException("La pièce " + piece.code() + " n'est pas du fournisseur du règlement.");
        }
        if (!Objects.equals(piece.deviseId(), devise)) {
            throw new InvalidRequestException(MSG_DEVISE);
        }
        if (CalculReglement.depasse(piece.resteAPayer(), ligne.montant())) {
            throw new InvalidRequestException("Le montant affecté à la pièce " + piece.code()
                    + " dépasse son reste à payer (" + piece.resteAPayer().toPlainString() + ").");
        }
    }

    /** G-a : statut de pièce sans « OK pour paiement » (ou pièce sans statut) = non validée. */
    private static void exigerValidee(PieceSaisie piece) {
        if (!piece.okPourPaiement()) {
            throw new InvalidRequestException(MSG_NON_VALIDEE);
        }
    }

    private static void appliquer(StlSettlement reglement, Entete entete, SaisieDecaissementDto saisie, String auteur,
                                  LocalDateTime maintenant) {
        reglement.setDocDate(entete.dateDocument());
        reglement.setEcheanceDate(entete.dateEcheance());
        reglement.setSettlementDate(entete.datePointage());
        reglement.setCode(entete.reference());
        reglement.setNotes(requireNonNullElse(saisie.notes(), ""));
        reglement.setStatus(entete.statut().name());
        reglement.setAmount(entete.montant());
        reglement.setReceiptCode(saisie.codeRecu());
        reglement.setStlSettlementType(entete.type());
        reglement.setTier(entete.fournisseur());
        reglement.setTierPayer(entete.payeur());
        reglement.setCfgBank(entete.banque());
        reglement.setCfgLawyer(entete.notaire());
        reglement.setCfgTierOther(entete.tiersAutrui());
        reglement.setStlAccount(entete.compte());
        reglement.setUserUpdate(auteur);
        reglement.setUserUpdateDateTime(maintenant);
    }

    private void ecrireLignes(StlSettlement reglement, List<LigneSaisieDecaissementDto> lignes, String auteur,
                              LocalDateTime maintenant) {
        for (LigneSaisieDecaissementDto saisie : lignes) {
            StlSettlementLine ligne = new StlSettlementLine();
            ligne.setId(UUID.randomUUID());
            ligne.setAmount(saisie.montant());
            ligne.setStlSettlement(reglement);
            ligne.setBuyDocumentId(saisie.pieceId());
            ligne.setUserCreate(auteur);
            ligne.setUserCreateDateTime(maintenant);
            ligne.setUserUpdate(auteur);
            ligne.setUserUpdateDateTime(maintenant);
            em.persist(ligne);
        }
    }

    private void historiser(StlSettlement reglement, LocalDateTime maintenant, String auteur) {
        StlSettlementHistory historique = new StlSettlementHistory();
        historique.setId(UUID.randomUUID());
        historique.setAdmUserId(PerimetreCommercialService.admUserIdCourant().orElse(null));
        historique.setDocDate(maintenant);
        historique.setEcheanceDate(reglement.getEcheanceDate());
        historique.setStlSettlementId(reglement.getId());
        historique.setStlSettlementTypeId(reglement.getStlSettlementType().getId());
        historique.setReference(reglement.getCode());
        historique.setUserCreate(auteur);
        historique.setUserCreateDateTime(maintenant);
        historique.setUserUpdate(auteur);
        historique.setUserUpdateDateTime(maintenant);
        em.persist(historique);
    }

    private List<StlSettlementLine> lignes(UUID reglementId) {
        return em.createQuery("SELECT l FROM StlSettlementLine l WHERE l.stlSettlement.id = :id", StlSettlementLine.class)
                .setParameter("id", reglementId).getResultList();
    }

    private String auteur() {
        return outils.utilisateurs().courant().nom();
    }

    private CfgTier tiers(UUID id, String libelle) {
        return id == null ? null : trouver(CfgTier.class, id, libelle);
    }

    private <T> T trouver(Class<T> classe, UUID id, String libelle) {
        T entite = em.find(classe, id);
        if (entite == null) {
            throw new InvalidRequestException(libelle + " introuvable : " + id);
        }
        return entite;
    }

    private static boolean egal(BigDecimal a, BigDecimal b) {
        return CalculReglement.arrondi(a).compareTo(CalculReglement.arrondi(b)) == 0;
    }

    private static <E> UUID idDe(E entite, Function<E, UUID> id) {
        return entite == null ? null : id.apply(entite);
    }

    private static <T> T requis(T valeur, String message) {
        if (valeur == null) {
            throw new InvalidRequestException(message);
        }
        return valeur;
    }
}
