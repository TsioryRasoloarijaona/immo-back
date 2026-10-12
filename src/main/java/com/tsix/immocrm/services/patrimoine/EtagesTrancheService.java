package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.patrimoine.EtageTrancheDto;
import com.tsix.immocrm.dto.patrimoine.EtageTrancheEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheLevel;
import com.tsix.immocrm.entities.lnd.LndLevel;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.repositories.cfg.CfgTrancheLevelRepository;
import com.tsix.immocrm.repositories.lnd.LndLevelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Onglet Étages d'une tranche, {@code CfgTrancheLevel} (FEATURE-patrimoine-tranche-onglets.md §2.2). */
@Service
@Transactional(readOnly = true)
public class EtagesTrancheService {

    private static final String CHAMP_NIVEAU = "niveauTerrainId";
    private static final Comparator<EtageTrancheDto> PAR_CODE =
            Comparator.comparing(e -> Objects.toString(e.niveauCode(), ""));

    private final TrancheVisible trancheVisible;
    private final CfgTrancheLevelRepository etageRepository;
    private final LndLevelRepository niveauRepository;
    private final SignatureLigne signatureLigne;

    public EtagesTrancheService(TrancheVisible trancheVisible, CfgTrancheLevelRepository etageRepository,
                                LndLevelRepository niveauRepository, SignatureLigne signatureLigne) {
        this.trancheVisible = trancheVisible;
        this.etageRepository = etageRepository;
        this.niveauRepository = niveauRepository;
        this.signatureLigne = signatureLigne;
    }

    public List<EtageTrancheDto> lister(UUID trancheId) {
        trancheVisible.trouver(trancheId);
        return versDtos(etageRepository.findByCfgTrancheId(trancheId));
    }

    @Transactional
    public List<EtageTrancheDto> enregistrerEtages(UUID trancheId, List<EtageTrancheEcritureDto> lignes) {
        CfgTranche tranche = trancheVisible.trouver(trancheId);
        SignatureLigne.Signature signature = signatureLigne.courante();
        ReconciliationLignes.Resultat<CfgTrancheLevel> resultat = ReconciliationLignes.reconcilier(
                etageRepository.findByCfgTrancheId(trancheId), CfgTrancheLevel::getId, lignes,
                new ReconciliationLignes.Onglet<EtageTrancheEcritureDto, CfgTrancheLevel>() {
                    @Override
                    public UUID idLigne(EtageTrancheEcritureDto ligne) {
                        return ligne.id();
                    }

                    @Override
                    public Object cle(EtageTrancheEcritureDto ligne, String prefixe) {
                        return ReglesSaisie.requis(ligne.niveauTerrainId(), prefixe + CHAMP_NIVEAU, "Niveau");
                    }

                    @Override
                    public CfgTrancheLevel nouvelle() {
                        CfgTrancheLevel etage = new CfgTrancheLevel();
                        etage.setId(UUID.randomUUID());
                        etage.setCfgTranche(tranche);
                        etage.setUserCreate(signature.nom());
                        etage.setUserCreateDateTime(signature.instant());
                        return etage;
                    }

                    @Override
                    public void appliquer(CfgTrancheLevel etage, EtageTrancheEcritureDto ligne, int rang,
                                          String prefixe) {
                        etage.setLndLevel(niveau(ligne.niveauTerrainId(), prefixe + CHAMP_NIVEAU));
                        etage.setUserUpdate(signature.nom());
                        etage.setUserUpdateDateTime(signature.instant());
                    }
                });
        etageRepository.deleteAll(resultat.supprimees());
        return versDtos(etageRepository.saveAll(resultat.gardees()));
    }

    private LndLevel niveau(UUID id, String champ) {
        return niveauRepository.findById(id)
                .orElseThrow(() -> new ChampInvalideException(champ, "Niveau introuvable : " + id));
    }

    private static List<EtageTrancheDto> versDtos(List<CfgTrancheLevel> etages) {
        return etages.stream().map(EtagesTrancheService::versDto).sorted(PAR_CODE).toList();
    }

    private static EtageTrancheDto versDto(CfgTrancheLevel etage) {
        LndLevel niveau = etage.getLndLevel();
        return niveau == null
                ? new EtageTrancheDto(etage.getId(), null, null, null)
                : new EtageTrancheDto(etage.getId(), niveau.getId(), niveau.getCode(), niveau.getDescription());
    }
}
