package com.tsix.immocrm.services.dossier.workflow;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.str.StrEntityLinkedEntity;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.str.StrChampObligatoireRepository;
import com.tsix.immocrm.utils.TierNomFormatter;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Champs obligatoires d'une transition ou d'un statut, sur le dossier et sur ses tiers liés (client, acquéreurs) :
 * {@code Ressource.VerifMadatoriesForWorkflow} et {@code CfgTier.VerifMadatoriesForWorkflow}. Rend le texte OMRAN
 * des champs manquants, vide si tout est renseigné.
 */
@Component
public class ChampsObligatoires {

    static final String LIBELLE_DOSSIER = "Dossier de vente";
    static final String FIN_DE_LIGNE = "\r\n";
    private static final String LIEN_PARENT = "LINKED_IS_PARENT";
    private static final String LIEN_SOUS_ENFANT = "LINKED_IS_UNDERCHILD";
    private static final String CLIENT = "CfgCustomerId";
    private static final String ACQUEREURS = "Purchasers.CfgTierId";
    private static final String SALAIRE = "AdmSalaryId";

    private final StrChampObligatoireRepository champRepository;
    private final ComFolderCustomerRepository acquereurRepository;

    public ChampsObligatoires(StrChampObligatoireRepository champRepository,
                              ComFolderCustomerRepository acquereurRepository) {
        this.champRepository = champRepository;
        this.acquereurRepository = acquereurRepository;
    }

    /** Une ligne de paramétrage : champ, libellé, entité du champ, lien vers l'entité liée (nul = le dossier). */
    record ChampRequis(String code, String libelle, UUID entite, StrEntityLinkedEntity lien) {
    }

    String manquantsDeTransition(ComFolder dossier, UUID transition) {
        return manquants(dossier, lignes(champRepository.deTransition(transition)));
    }

    String manquantsDuStatut(ComFolder dossier, UUID statut) {
        return manquants(dossier, lignes(champRepository.deStatut(statut)));
    }

    private String manquants(ComFolder dossier, List<ChampRequis> champs) {
        if (champs.isEmpty()) {
            return "";
        }
        StringBuilder texte = new StringBuilder(champsDuDossier(dossier, champs.stream()
                .filter(c -> c.lien() == null && StatutDossier.CONFIG_ENTITY_ID.equals(c.entite()))
                .toList()));
        texte.append(tiersLies(dossier, champs, CLIENT, LIEN_PARENT));
        texte.append(tiersLies(dossier, champs, ACQUEREURS, LIEN_SOUS_ENFANT));
        return texte.toString();
    }

    private String champsDuDossier(ComFolder dossier, List<ChampRequis> champs) {
        StringBuilder lignes = new StringBuilder();
        for (ChampRequis champ : champs) {
            if (champManquant(dossier, champ.code())) {
                lignes.append(ligneManquante(champ.libelle()));
            }
        }
        return lignes.isEmpty() ? "" : LIBELLE_DOSSIER + FIN_DE_LIGNE + lignes;
    }

    private boolean champManquant(ComFolder dossier, String code) {
        if (ChampsDossier.DECISION_MEILLEURTAUX.equals(code)) {
            return champRepository.decisionMeilleurtauxRenseignee(dossier.getId()).stream()
                    .noneMatch(Integer.valueOf(1)::equals);
        }
        return ChampsDossier.champ(code)
                .map(c -> vide(c.lecture().apply(dossier)))
                .orElse(false);
    }

    /** Champs d'un lien donné (client ou acquéreurs), vérifiés sur chaque tiers concerné. */
    private String tiersLies(ComFolder dossier, List<ChampRequis> champs, String propriete, String typeLien) {
        List<ChampRequis> duLien = champs.stream()
                .filter(c -> c.lien() != null && propriete.equals(c.lien().getLinkPropertyName())
                        && typeLien.equals(c.lien().getLinkType())
                        && Objects.equals(c.entite(), c.lien().getStrLinkedEntityId()))
                .toList();
        if (duLien.isEmpty()) {
            return "";
        }
        List<CfgTier> tiers = CLIENT.equals(propriete)
                ? Stream.ofNullable(dossier.getCfgCustomer()).toList()
                : acquereurRepository.findByComFolderId(dossier.getId()).stream()
                        .map(ComFolderCustomer::getCfgTier).filter(Objects::nonNull).toList();
        StringBuilder texte = new StringBuilder();
        for (CfgTier tier : tiers) {
            texte.append(champsDuTiers(tier, duLien));
        }
        return texte.toString();
    }

    /** Personne physique seulement ; jamais la raison sociale ni le registre ; salaire selon la profession. */
    private String champsDuTiers(CfgTier tier, List<ChampRequis> champs) {
        Map<String, Object> valeurs = champRepository.tiers(tier.getId()).stream().findFirst()
                .map(ChampsObligatoires::parNom)
                .orElseGet(Map::of);
        if (!Boolean.TRUE.equals(valeurs.get("ESTPERSONNE"))) {
            return "";
        }
        boolean sansSalaire = valeurs.get("PROFESSIONID") != null && !Boolean.TRUE.equals(valeurs.get("AVECSALAIRE"));
        StringBuilder lignes = new StringBuilder();
        for (ChampRequis champ : champs) {
            String cle = champ.code().toUpperCase(Locale.ROOT);
            boolean ignore = !valeurs.containsKey(cle) || (sansSalaire && SALAIRE.equals(champ.code()));
            if (!ignore && videPourTiers(valeurs.get(cle))) {
                lignes.append(ligneManquante(champ.libelle()));
            }
        }
        return lignes.isEmpty() ? "" : "Client : " + TierNomFormatter.nom(tier) + FIN_DE_LIGNE + lignes;
    }

    private static Map<String, Object> parNom(Tuple ligne) {
        Map<String, Object> valeurs = new HashMap<>();
        for (TupleElement<?> element : ligne.getElements()) {
            valeurs.put(element.getAlias().toUpperCase(Locale.ROOT), ligne.get(element));
        }
        return valeurs;
    }

    static String ligneManquante(String libelle) {
        return "Le champ '" + libelle + "' est obligatoire !" + FIN_DE_LIGNE;
    }

    /** {@code Ressource.VerifMadatoriesForWorkflow} : nul, texte vide, montant ou entier à zéro. */
    static boolean vide(Object valeur) {
        if (valeur == null) {
            return true;
        }
        if (valeur instanceof String texte) {
            return texte.isEmpty();
        }
        if (valeur instanceof BigDecimal montant) {
            return montant.signum() == 0;
        }
        if (valeur instanceof Integer entier) {
            return entier == 0;
        }
        return false;
    }

    /** {@code CfgTier.VerifMadatoriesForWorkflow} ne teste ni montant ni entier nul. */
    static boolean videPourTiers(Object valeur) {
        return valeur == null || (valeur instanceof String texte && texte.isEmpty());
    }

    private static List<ChampRequis> lignes(List<Object[]> brutes) {
        List<ChampRequis> champs = new ArrayList<>();
        for (Object[] l : brutes) {
            champs.add(new ChampRequis((String) l[0], l[1] != null ? (String) l[1] : (String) l[0], (UUID) l[2],
                    (StrEntityLinkedEntity) l[3]));
        }
        champs.sort(Comparator.comparing(ChampRequis::libelle));
        return champs;
    }
}
