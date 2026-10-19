package com.tsix.immocrm.fixtures;

import com.tsix.immocrm.domain.InteractionKpi;
import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComActionMessageType;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.repositories.adm.AdmEventRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageSubCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageTypeRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import com.tsix.immocrm.services.prospection.ContexteInteraction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Jeu du suivi des commerciaux (FEATURE-suivi-commerciaux.md §2, §6) : interactions par auteur, personnes, visites,
 * ventes et versements. Référentiels créés avec les codes exacts de la base ; tout est annulé avec la transaction.
 */
@Component
public class FixtureSuiviCommerciaux {

    /** Mardi hors ramadan (Casablanca en UTC+1) : un décalage de fuseau y changerait de jour. */
    public static final LocalDate JOUR = LocalDate.of(2026, 4, 21);
    public static final LocalDate DIMANCHE = LocalDate.of(2026, 4, 26);

    public static final String RDV_PRIS = "Rendez vous pris";
    public static final String NUMERO_ERRONE = "Numéro erroné ";
    public static final String A_RAPPELER = "A rappeler par le commercial";

    public static final UUID CATEGORIE_VENTE_2222 = UUID.fromString("69f8e7d6-2222-2222-2222-1aa8b1f02008");
    public static final UUID CATEGORIE_VENTE_3333 = UUID.fromString("69f8e7d6-3333-3333-3333-1aa8b1f02008");
    /** StrEntity « ComProspection » (prospection de vente), même PKey en test et en CI. */
    private static final UUID ENTITE_PROSPECTION_VENTE = UUID.fromString("E7C73F70-1111-40D9-AA78-A2825C42B601");

    private final FixtureEtatVentes etatVentes;
    private final FixtureDossierVente dossierVente;
    private final FixtureInteraction interaction;
    private final Depots depots;

    /** Regroupe les dépôts écrits par la fixture (S107). */
    @Component
    public record Depots(CfgTierRepository tiers, ComProspectionRepository prospections,
                         ComActionMessageRepository messages, ComActionMessageTypeRepository types,
                         ComActionMessageSubCategoryRepository resultats,
                         ComActionMessageCategoryRepository categories, AdmEventRepository evenements,
                         ComFolderRepository dossiers, StkItemRepository articles,
                         StlSettlementTypeRepository typesReglement, StlSettlementRepository reglements,
                         StlSettlementLineRepository lignesReglement) {
    }

    public FixtureSuiviCommerciaux(FixtureEtatVentes etatVentes, FixtureDossierVente dossierVente,
                                   FixtureInteraction interaction, Depots depots) {
        this.etatVentes = etatVentes;
        this.dossierVente = dossierVente;
        this.interaction = interaction;
        this.depots = depots;
    }

    public static String prefixe(String racine) {
        return FixtureEtatVentes.prefixe(racine);
    }

    public static LocalDateTime a(LocalDate jour, int heure, int minute) {
        return jour.atTime(heure, minute);
    }

    // ------------------------------------------------------------------ patrimoine, comptes

    public CfgTranche tranche(String code, boolean entiteActive) {
        CfgProject projet = etatVentes.projet("PRJ-" + code, entiteActive);
        return etatVentes.tranche(code, projet, true);
    }

    /** Compte actif rattaché à un employé, rôles écrits dans {@code AdmUserRole}. */
    public AdmUser compte(String code, String... roles) {
        return dossierVente.compte(code, roles);
    }

    public void affecter(AdmUser compte, CfgTranche tranche) {
        etatVentes.affecter(compte, tranche.getCfgProject());
    }

    /** Clé du commercial sur le fil : l'employé du compte (comme {@code Cle} du KPI). */
    public static String cle(AdmUser compte) {
        return compte.getCfgEmployee().getId().toString();
    }

    /** Personne jointe : tous ses champs de nom portent {@code code}, pour la retrouver quel que soit l'affichage. */
    public CfgTier personne(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setDescription(code);
        tier.setSocialReason(code);
        tier.setFirstName(code);
        tier.setLastName(code);
        return depots.tiers().save(tier);
    }

    public ComProspection prospection(CfgTranche tranche, CfgTier personne, AdmUser commercial, LocalDateTime date) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-" + personne.getCode());
        p.setStrEntityId(ENTITE_PROSPECTION_VENTE);
        p.setCfgProspect(personne);
        p.setCfgCommercial(commercial != null ? commercial.getCfgEmployee() : null);
        p.setStkHierarchy(tranche);
        p.setDocDate(date);
        p.setIsClosed(false);
        return depots.prospections().save(p);
    }

    /** Prospection propre à une nouvelle personne de code {@code code}. */
    public ComProspection prospection(CfgTranche tranche, String code) {
        return prospection(tranche, personne(code), null, a(JOUR, 9, 0));
    }

    // ------------------------------------------------------------------ référentiels d'interaction

    public ComActionMessageType type(String code) {
        ComActionMessageType type = new ComActionMessageType();
        type.setId(UUID.randomUUID());
        type.setCode(code);
        type.setDescription(code);
        return depots.types().save(type);
    }

    public ComActionMessageType appelSortant() {
        return type(InteractionKpi.TYPE_APPEL_SORTANT);
    }

    public ComActionMessageType appelEntrant() {
        return type(InteractionKpi.TYPE_APPEL_ENTRANT);
    }

    public ComActionMessageType visite() {
        return type(InteractionKpi.TYPE_VISITE);
    }

    public ComActionMessageSubCategory resultat(String code) {
        ComActionMessageSubCategory r = new ComActionMessageSubCategory();
        r.setId(UUID.randomUUID());
        r.setCode(code);
        r.setDescription(code);
        return depots.resultats().save(r);
    }

    public ComActionMessageCategory categorieProspection() {
        return interaction.categorieProspection();
    }

    public ComActionMessageCategory categorie(UUID id) {
        return depots.categories().findById(id).orElseGet(() -> {
            ComActionMessageCategory c = new ComActionMessageCategory();
            c.setId(id);
            c.setCode("Dossier " + id.toString().substring(9, 13));
            c.setDescription(c.getCode());
            return depots.categories().save(c);
        });
    }

    // ------------------------------------------------------------------ interactions

    /**
     * Interaction de catégorie Prospection, auteur = {@code AdmUserId}, personne = prospect de la prospection
     * ({@code CfgTierId}) ; {@code StkItemId} vide (aucun article).
     */
    public ComActionMessage interaction(AdmUser auteur, ComProspection prospection, ComActionMessageType type,
                                        ComActionMessageSubCategory resultat, LocalDateTime date) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setAdmUser(auteur);
        m.setCfgEmployee(auteur.getCfgEmployee());
        m.setComProspection(prospection);
        m.setCfgTier(prospection != null ? prospection.getCfgProspect() : null);
        m.setComActionMessageType(type);
        m.setComActionMessageCategory(categorieProspection());
        m.setComActionMessageSubCategory(resultat);
        m.setDocDate(date);
        m.setUserCreate(auteur.getCode());
        m.setUserCreateDateTime(date);
        return depots.messages().save(m);
    }

    public ComActionMessage enregistrer(ComActionMessage m) {
        return depots.messages().save(m);
    }

    /** Visite de vente (catégorie de dossier) sans article : la tranche passe par le dossier. */
    public ComActionMessage visiteVente(AdmUser auteur, ComFolder dossier, ComActionMessageType typeVisite,
                                        UUID categorie, LocalDateTime date) {
        ComActionMessage m = new ComActionMessage();
        m.setId(UUID.randomUUID());
        m.setAdmUser(auteur);
        m.setCfgEmployee(auteur.getCfgEmployee());
        m.setComFolder(dossier);
        m.setCfgTier(dossier.getCfgCustomer());
        m.setComActionMessageType(typeVisite);
        m.setComActionMessageCategory(categorie(categorie));
        m.setDocDate(date);
        m.setUserCreate(auteur.getCode());
        m.setUserCreateDateTime(date);
        return depots.messages().save(m);
    }

    /** Rendez-vous planifié : {@code AdmEvent.StartHour} si {@code debutEvenement} non nul, sinon seule l'alerte. */
    public ComActionMessage planifier(ComActionMessage appel, LocalDateTime alerte, LocalDateTime debutEvenement) {
        appel.setAlertValidityDate(alerte);
        if (debutEvenement != null) {
            AdmEvent evenement = new AdmEvent();
            evenement.setId(UUID.randomUUID());
            evenement.setSubject("RDV " + appel.getId());
            evenement.setStartHour(debutEvenement);
            evenement.setEndHour(debutEvenement.plusHours(1));
            appel.setAdmEvent(depots.evenements().save(evenement));
        }
        return depots.messages().save(appel);
    }

    // ------------------------------------------------------------------ ventes et versements

    /**
     * Dossier du commercial sur un article de {@code tranche} ; prix identique sur le dossier ({@code Price},
     * {@code TotalToPay}) et l'article ({@code ComSalePrice}).
     */
    public ComFolder vente(String code, AdmUser commercial, CfgTranche tranche, ComProspection prospection,
                           LocalDateTime dateAccord, LocalDateTime dateSaisie, BigDecimal prix) {
        StkItem article = etatVentes.article(code, tranche, etatVentes.categorie("CAT-" + code),
                etatVentes.type("TYP-" + code));
        article.setComSalePrice(prix);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-" + code);
        folder.setStkVocation(etatVentes.vente());
        folder.setStrStatus(etatVentes.statut(StatutDossier.RESERVATION));
        folder.setCfgCommercial(commercial.getCfgEmployee());
        folder.setCfgCustomer(personne("CLI-" + code));
        folder.setComProspection(prospection);
        folder.setAgreementDate(dateAccord);
        folder.setDocDate(dateSaisie);
        folder.setPrice(prix);
        folder.setTotalToPay(prix);
        ComFolder sauve = depots.dossiers().save(folder);

        article.setComFolderId(sauve.getId());
        sauve.setStkItem(depots.articles().save(article));
        return depots.dossiers().save(sauve);
    }

    /** Une échéance du dossier réglée par une ligne de {@code montant} ; règlement du même montant (pas de reliquat). */
    public StlSettlementLine versement(ComFolder dossier, String montant, boolean credit, boolean comptabilise,
                                       LocalDateTime dateEcheance, LocalDateTime dateSaisie) {
        BigDecimal valeur = new BigDecimal(montant);
        ComDocument echeance = dossierVente.echeance(dossier,
                dossierVente.categorieEcheance("ECH-" + UUID.randomUUID().toString().substring(0, 8), false, false),
                valeur, dateSaisie);

        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("TYP-REG-" + UUID.randomUUID().toString().substring(0, 8));
        type.setIsNotContabilized(!comptabilise);
        depots.typesReglement().save(type);

        StlSettlement s = new StlSettlement();
        s.setId(UUID.randomUUID());
        s.setCode("REG-" + UUID.randomUUID().toString().substring(0, 8));
        s.setStlSettlementType(type);
        s.setTier(dossier.getCfgCustomer());
        s.setIsCredit(credit);
        s.setEcheanceDate(dateEcheance);
        s.setDocDate(dateSaisie);
        s.setAmount(valeur);
        s = depots.reglements().save(s);

        StlSettlementLine l = new StlSettlementLine();
        l.setId(UUID.randomUUID());
        l.setStlSettlement(s);
        l.setComDocument(echeance);
        l.setAmount(valeur);
        return depots.lignesReglement().save(l);
    }

    public void relire() {
        etatVentes.relire();
    }
}
