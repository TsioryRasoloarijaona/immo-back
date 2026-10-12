package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.domain.ClassementArticle;
import com.tsix.immocrm.dto.patrimoine.TerrainDisponibleDto;
import com.tsix.immocrm.dto.patrimoine.TerrainTrancheDto;
import com.tsix.immocrm.dto.patrimoine.TerrainTrancheEcritureDto;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheStkItem;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.ChampInvalideException;
import com.tsix.immocrm.repositories.cfg.CfgTrancheStkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Onglet Terrains d'une tranche, {@code CfgTrancheStkItem} (FEATURE-patrimoine-tranche-onglets.md §2.1). */
@Service
@Transactional(readOnly = true)
public class TerrainsTrancheService {

    private static final String CHAMP_TERRAIN = "terrainId";

    private final TrancheVisible trancheVisible;
    private final CfgTrancheStkItemRepository lienRepository;
    private final StkItemRepository itemRepository;
    private final SignatureLigne signatureLigne;

    public TerrainsTrancheService(TrancheVisible trancheVisible, CfgTrancheStkItemRepository lienRepository,
                                  StkItemRepository itemRepository, SignatureLigne signatureLigne) {
        this.trancheVisible = trancheVisible;
        this.lienRepository = lienRepository;
        this.itemRepository = itemRepository;
        this.signatureLigne = signatureLigne;
    }

    public List<TerrainTrancheDto> lister(UUID trancheId) {
        trancheVisible.trouver(trancheId);
        return lienRepository.findByCfgTrancheIdOrderBySortingAsc(trancheId).stream()
                .map(TerrainsTrancheService::versDto).toList();
    }

    public List<TerrainDisponibleDto> disponibles() {
        return itemRepository.parNatureEtVocation(ClassementArticle.NATURE_TERRAIN, ClassementArticle.VOCATION_PROJET)
                .stream()
                .map(i -> new TerrainDisponibleDto(i.getId(), i.getCode(), i.getDescription(), i.getArea()))
                .toList();
    }

    @Transactional
    public List<TerrainTrancheDto> enregistrerTerrains(UUID trancheId, List<TerrainTrancheEcritureDto> lignes) {
        CfgTranche tranche = trancheVisible.trouver(trancheId);
        SignatureLigne.Signature signature = signatureLigne.courante();
        ReconciliationLignes.Resultat<CfgTrancheStkItem> resultat = ReconciliationLignes.reconcilier(
                lienRepository.findByCfgTrancheIdOrderBySortingAsc(trancheId), CfgTrancheStkItem::getId, lignes,
                new ReconciliationLignes.Onglet<TerrainTrancheEcritureDto, CfgTrancheStkItem>() {
                    @Override
                    public UUID idLigne(TerrainTrancheEcritureDto ligne) {
                        return ligne.id();
                    }

                    @Override
                    public Object cle(TerrainTrancheEcritureDto ligne, String prefixe) {
                        return ReglesSaisie.requis(ligne.terrainId(), prefixe + CHAMP_TERRAIN, "Terrain");
                    }

                    @Override
                    public CfgTrancheStkItem nouvelle() {
                        CfgTrancheStkItem lien = new CfgTrancheStkItem();
                        lien.setId(UUID.randomUUID());
                        lien.setCfgTranche(tranche);
                        lien.setUserCreate(signature.nom());
                        lien.setUserCreateDateTime(signature.instant());
                        return lien;
                    }

                    @Override
                    public void appliquer(CfgTrancheStkItem lien, TerrainTrancheEcritureDto ligne, int rang,
                                          String prefixe) {
                        if (!memeId(lien.getStkItem(), ligne.terrainId())) {
                            lien.setStkItem(terrain(ligne.terrainId(), prefixe + CHAMP_TERRAIN));
                        }
                        BigDecimal ratio = ReglesSaisie.positifOuNul(ligne.ratio(), prefixe + "ratio");
                        lien.setRatio(ratio == null ? BigDecimal.ONE : ratio);
                        lien.setSorting(rang);
                        lien.setUserUpdate(signature.nom());
                        lien.setUserUpdateDateTime(signature.instant());
                    }
                });
        lienRepository.deleteAll(resultat.supprimees());
        return lienRepository.saveAll(resultat.gardees()).stream().map(TerrainsTrancheService::versDto).toList();
    }

    // Une ligne saisie dans OMRAN garde son terrain même s'il ne serait plus proposé aujourd'hui.
    private static boolean memeId(StkItem actuel, UUID id) {
        return actuel != null && actuel.getId().equals(id);
    }

    private StkItem terrain(UUID id, String champ) {
        StkItem item = itemRepository.findById(id)
                .orElseThrow(() -> new ChampInvalideException(champ, "Terrain introuvable : " + id));
        boolean terrainDeProjet = item.getStkKind() != null && item.getStkVocation() != null
                && ClassementArticle.NATURE_TERRAIN.equalsIgnoreCase(item.getStkKind().getCode())
                && ClassementArticle.VOCATION_PROJET.equalsIgnoreCase(item.getStkVocation().getCode());
        if (!terrainDeProjet) {
            throw new ChampInvalideException(champ, "L'article « %s » n'est pas un terrain de vocation Projet."
                    .formatted(item.getCode()));
        }
        return item;
    }

    private static TerrainTrancheDto versDto(CfgTrancheStkItem lien) {
        StkItem item = lien.getStkItem();
        return new TerrainTrancheDto(lien.getId(), item == null ? null : item.getId(),
                item == null ? null : item.getCode(), item == null ? null : item.getDescription(),
                item == null ? null : item.getArea(), lien.getRatio(), lien.getSorting());
    }
}
