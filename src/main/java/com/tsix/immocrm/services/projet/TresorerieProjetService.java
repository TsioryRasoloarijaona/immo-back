package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.dto.projet.FiltresTresorerieProjet;
import com.tsix.immocrm.dto.projet.LigneCelluleTresorerieDto;
import com.tsix.immocrm.dto.projet.NoeudTresorerieDto;
import com.tsix.immocrm.dto.projet.ProjetChoixDto;
import com.tsix.immocrm.dto.projet.TresorerieProjetDto;
import com.tsix.immocrm.dto.projet.TresorerieProjetDto.OngletsTresorerieDto;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.services.droits.DroitsOmran;
import com.tsix.immocrm.services.perimetre.PerimetreAchat;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import com.tsix.immocrm.utils.TierNomFormatter;
import com.tsix.immocrm.web.export.ColonneFixe;
import com.tsix.immocrm.web.export.FeuilleExport;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.tsix.immocrm.domain.TypeExport.MONTANT;
import static com.tsix.immocrm.domain.TypeExport.NOMBRE;
import static com.tsix.immocrm.services.projet.travaux.ValeursNatives.decimal;
import static com.tsix.immocrm.services.projet.travaux.ValeursNatives.jour;
import static com.tsix.immocrm.services.projet.travaux.ValeursNatives.uuid;

/**
 * Trésorerie projet (FEATURE-n5-tableaux-de-bord-projet.md R1–R8, R14, R15) : prévisions de lot et restes des pièces
 * d'achat, lus sur tables ; toute échéance ≤ fin du mois courant est reportée à aujourd'hui (D5 remplacé).
 */
@Service
@Transactional(readOnly = true)
public class TresorerieProjetService {

    private static final String DROIT = "Yes.Prj.Front.Tdb.ExpensesByYear";
    private static final String SEP = "|";
    private static final String ONGLET_PROJET = "P";
    private static final String ONGLET_FOURNISSEUR = "F";
    private static final String TYPE_LOT = "PrjMarketPrevision";
    private static final String TYPE_ACHAT = "BuyPrevision";
    private static final String LIBELLE = "libelle";
    private static final BigDecimal SEUIL_RESTE = new BigDecimal("0.5");
    private static final LocalDate ORIGINE = LocalDate.of(1900, 1, 1);

    private static final String FILTRES = " IN (:projets) AND %s IN (:societes) AND CAST(%s AS date) >= :debut"
            + " AND CAST(%s AS date) < :fin";
    private static final String SQL_LOTS = """
            SELECT pp.PKey, pp.PaymentDate, pp.Amount * ISNULL(v.VentilationRate, 1), COALESCE(m.Description, m.Code),
                   p.PKey, COALESCE(p.Description, p.Code), t.SocialReason, t.Description, t.FirstName, t.LastName
            FROM PrjMarketPaymentPrevision pp
            JOIN PrjMarket m ON m.PKey = pp.PrjMarketId
            JOIN PrjProject p ON p.PKey = m.PrjProjectId
            LEFT JOIN PrjProjectCfgTrancheVentilation v ON v.PrjProjectId = p.PKey
            LEFT JOIN CfgTier t ON t.PKey = m.CfgSupplierId
            WHERE pp.Amount <> 0 AND p.PKey""" + FILTRES.formatted("p.CfgCompanyId", "pp.PaymentDate", "pp.PaymentDate");
    private static final String SQL_ACHATS = """
            SELECT d.PKey, d.MaturityDate, d.AmountToPay - ISNULL((%s), 0), d.Code,
                   p.PKey, COALESCE(p.Description, p.Code), t.SocialReason, t.Description, t.FirstName, t.LastName,
                   COALESCE(d.SupplierDescription, N''), d.DocType
            FROM BuyDocument d
            JOIN PrjProject p ON p.PKey = d.PrjProjectId
            LEFT JOIN CfgTier t ON t.PKey = d.CfgTierId
            WHERE d.CanBePayed = 1 AND d.PrjMarketId IS NOT NULL
              AND (d.IsFinancialRefund IS NULL OR d.IsFinancialRefund = 1) AND d.PrjProjectId"""
            .formatted(SoldeDocumentAchat.SQL_REGLE) + FILTRES.formatted("d.CfgCompanyId", "d.MaturityDate", "d.MaturityDate");

    private static final List<String> MOIS = IntStream.rangeClosed(1, 12)
            .mapToObj(m -> StringUtils.capitalize(Month.of(m).getDisplayName(TextStyle.FULL, Locale.FRENCH))).toList();
    private static final List<ColonneFixe> COLONNES = Stream.of(
            Stream.of(ColonneFixe.texte(LIBELLE, "Libellé"), ColonneFixe.de("niveau", "Niveau", NOMBRE)),
            IntStream.rangeClosed(1, 12).mapToObj(m -> ColonneFixe.de("m" + m, MOIS.get(m - 1), MONTANT)),
            Stream.of(ColonneFixe.de("total", "Total", MONTANT), ColonneFixe.de("regle", "Réglé", MONTANT)))
            .flatMap(Function.identity()).toList();

    private static final Niveau PAR_PROJET = new Niveau(l -> l.projet().toString(), Ligne::projetLibelle);
    private static final Niveau PAR_GROUPE = new Niveau(Ligne::groupe, Ligne::groupe);

    private final DroitsOmran droits;
    private final PerimetreProjetService perimetre;
    private final EntityManager em;
    private final Clock clock;

    TresorerieProjetService(DroitsOmran droits, PerimetreProjetService perimetre, EntityManager em, Clock clock) {
        this.droits = droits;
        this.perimetre = perimetre;
        this.em = em;
        this.clock = clock;
    }

    public TresorerieProjetDto lire(FiltresTresorerieProjet filtres) {
        Contexte c = contexte(filtres);
        return new TresorerieProjetDto(c.annee(), choix(c.perimetre()), MOIS, onglets(c));
    }

    public List<LigneCelluleTresorerieDto> cellule(FiltresTresorerieProjet filtres, int mois, String noeud) {
        Contexte c = contexte(filtres);
        if (mois < 1 || mois > 12) {
            throw new InvalidRequestException("Le mois doit être compris entre 1 et 12.");
        }
        return lignes(c).stream()
                .filter(l -> l.echeance().date().getMonthValue() == mois && cles(l).contains(noeud))
                .sorted(Comparator.comparing((Ligne l) -> l.echeance().date())
                        .thenComparing(l -> l.source().libelle(), Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(TresorerieProjetService::detail).toList();
    }

    /** Lève le 400 « aucun projet » et le 403 avant toute écriture du flux d'export. */
    public List<FeuilleExport> feuillesExport(FiltresTresorerieProjet filtres) {
        OngletsTresorerieDto o = onglets(contexte(filtres));
        return List.of(feuille("Projet", o.projet()), feuille("Fournisseur", o.fournisseur()));
    }

    private Contexte contexte(FiltresTresorerieProjet filtres) {
        droits.exiger(DROIT);
        if (filtres.projets().isEmpty()) {
            throw new InvalidRequestException("Veuillez sélectionner au moins un projet !");
        }
        LocalDate aujourdhui = LocalDate.now(clock);
        int annee = filtres.annee() == null ? aujourdhui.getYear() : filtres.annee();
        PerimetreAchat p = perimetre.achat(filtres.projets())
                .deSocietes(filtres.societe() == null ? List.of() : List.of(filtres.societe()));
        return new Contexte(annee, aujourdhui, p);
    }

    private List<ProjetChoixDto> choix(PerimetreAchat p) {
        Set<UUID> visibles = perimetre.projetsVisibles();
        if (visibles.isEmpty() || p.societes().isEmpty()) {
            return List.of();
        }
        List<?> lignes = em.createNativeQuery("SELECT PKey, Code, Description FROM PrjProject"
                        + " WHERE PKey IN (:projets) AND CfgCompanyId IN (:societes) ORDER BY Description")
                .setParameter("projets", visibles).setParameter("societes", p.societes()).getResultList();
        return lignes.stream().map(Object[].class::cast)
                .map(r -> new ProjetChoixDto(uuid(r[0]), (String) r[1], (String) r[2])).toList();
    }

    private OngletsTresorerieDto onglets(Contexte c) {
        List<Ligne> lignes = lignes(c);
        return new OngletsTresorerieDto(racines(lignes, ONGLET_PROJET, List.of(PAR_PROJET, PAR_GROUPE), c.aujourdhui()),
                racines(lignes, ONGLET_FOURNISSEUR, List.of(PAR_GROUPE), c.aujourdhui()));
    }

    // ------------------------------------------------------------------ lecture des lignes

    private List<Ligne> lignes(Contexte c) {
        PerimetreAchat p = c.perimetre();
        if (p.projets().isEmpty() || p.societes().isEmpty() || c.annee() < c.aujourdhui().getYear()) {
            return List.of();
        }
        // L'année courante reçoit aussi toutes les échéances passées, reportées à aujourd'hui.
        LocalDate debut = c.annee() == c.aujourdhui().getYear() ? ORIGINE : LocalDate.of(c.annee(), 1, 1);
        LocalDate fin = LocalDate.of(c.annee() + 1, 1, 1);
        List<Ligne> lignes = new ArrayList<>();
        lire(SQL_LOTS, p, debut, fin).forEach(r -> lot(r, c).ifPresent(lignes::add));
        lire(SQL_ACHATS, p, debut, fin).forEach(r -> achat(r, c).ifPresent(lignes::add));
        return lignes.stream().filter(l -> l.echeance().date().getYear() == c.annee()).toList();
    }

    private List<Object[]> lire(String sql, PerimetreAchat p, LocalDate debut, LocalDate fin) {
        List<?> brut = em.createNativeQuery(sql).setParameter("projets", p.projets())
                .setParameter("societes", p.societes()).setParameter("debut", debut).setParameter("fin", fin)
                .getResultList();
        return brut.stream().map(Object[].class::cast).toList();
    }

    private static Optional<Ligne> lot(Object[] r, Contexte c) {
        BigDecimal montant = decimal(r[2]);
        if (montant == null || montant.signum() == 0) {
            return Optional.empty();
        }
        String lot = (String) r[3];
        return Optional.of(new Ligne(new Source(uuid(r[0]), TYPE_LOT, "PrjMarketPaymentPrevision", lot),
                echeance(jour(r[1]), c), uuid(r[4]), (String) r[5], groupe(r, lot), montant, false));
    }

    private static Optional<Ligne> achat(Object[] r, Contexte c) {
        BigDecimal reste = decimal(r[2]);
        if (reste == null || reste.abs().compareTo(SEUIL_RESTE) <= 0) {
            return Optional.empty();
        }
        return Optional.of(new Ligne(new Source(uuid(r[0]), TYPE_ACHAT, "BuyDocument", (String) r[3]),
                echeance(jour(r[1]), c), uuid(r[4]), (String) r[5], groupe(r, (String) r[10]), reste,
                "REFUND".equals(r[11])));
    }

    /** Fournisseur (colonnes 6 à 9 : raison sociale, description, prénom, nom), sinon {@code repli}. */
    private static String groupe(Object[] r, String repli) {
        String nom = TierNomFormatter.nom((String) r[6], (String) r[7], (String) r[8], (String) r[9]);
        if (StringUtils.hasText(nom)) {
            return nom;
        }
        return repli == null ? "" : repli;
    }

    private static Echeance echeance(LocalDate origine, Contexte c) {
        boolean reporte = !origine.isAfter(c.aujourdhui().with(TemporalAdjusters.lastDayOfMonth()));
        return new Echeance(reporte ? c.aujourdhui() : origine, origine, reporte);
    }

    // ------------------------------------------------------------------ arbre

    /** Clés des nœuds qui comptent la ligne, dans les deux onglets ; mêmes clés que {@link #noeud}. */
    private static List<String> cles(Ligne l) {
        String sens = sens(l.credit());
        String projet = ONGLET_PROJET + SEP + sens;
        String duProjet = projet + SEP + l.projet();
        String fournisseur = ONGLET_FOURNISSEUR + SEP + sens;
        return List.of(projet, duProjet, duProjet + SEP + l.groupe(), fournisseur, fournisseur + SEP + l.groupe());
    }

    private static String sens(boolean credit) {
        return credit ? "C" : "D";
    }

    private static List<NoeudTresorerieDto> racines(List<Ligne> lignes, String onglet, List<Niveau> niveaux,
                                                    LocalDate aujourdhui) {
        List<NoeudTresorerieDto> racines = new ArrayList<>();
        for (boolean credit : new boolean[] {true, false}) {
            List<Ligne> duSens = lignes.stream().filter(l -> l.credit() == credit).toList();
            noeud(new Position(onglet + SEP + sens(credit), credit ? "Encaissement" : "Décaissement", credit, 0),
                    duSens, niveaux, aujourdhui).ifPresent(racines::add);
        }
        return racines;
    }

    /** Nœud cumulant ses lignes ; absent si ses 12 mois sont nuls (R4). */
    private static Optional<NoeudTresorerieDto> noeud(Position pos, List<Ligne> lignes, List<Niveau> niveaux,
                                                      LocalDate aujourdhui) {
        BigDecimal[] montants = new BigDecimal[12];
        Arrays.fill(montants, BigDecimal.ZERO);
        BigDecimal regle = BigDecimal.ZERO;
        for (Ligne l : lignes) {
            int m = l.echeance().date().getMonthValue() - 1;
            montants[m] = montants[m].add(l.montant());
            if (l.echeance().date().isBefore(aujourdhui)) {
                regle = regle.add(l.montant());
            }
        }
        if (Stream.of(montants).allMatch(v -> v.signum() == 0)) {
            return Optional.empty();
        }
        List<NoeudTresorerieDto> enfants = new ArrayList<>();
        if (!niveaux.isEmpty()) {
            Niveau niveau = niveaux.get(0);
            Map<String, List<Ligne>> parCle = new LinkedHashMap<>();
            lignes.forEach(l -> parCle.computeIfAbsent(niveau.cle().apply(l), k -> new ArrayList<>()).add(l));
            parCle.forEach((cle, groupe) -> noeud(new Position(pos.cle() + SEP + cle,
                            niveau.libelle().apply(groupe.get(0)), pos.credit(), pos.niveau() + 1),
                    groupe, niveaux.subList(1, niveaux.size()), aujourdhui).ifPresent(enfants::add));
            enfants.sort(Comparator.comparing(NoeudTresorerieDto::libelle, String.CASE_INSENSITIVE_ORDER));
        }
        List<BigDecimal> mois = List.of(montants);
        return Optional.of(new NoeudTresorerieDto(pos.cle(), pos.libelle(), pos.credit(), pos.niveau(), mois,
                mois.stream().reduce(BigDecimal.ZERO, BigDecimal::add), regle, enfants));
    }

    private static LigneCelluleTresorerieDto detail(Ligne l) {
        Echeance e = l.echeance();
        Source s = l.source();
        return new LigneCelluleTresorerieDto(e.date(), e.origine(), s.libelle(), l.groupe(), l.montant(), s.type(),
                e.reporte(), s.objetId(), s.namespace());
    }

    // ------------------------------------------------------------------ export

    private static FeuilleExport feuille(String nom, List<NoeudTresorerieDto> racines) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        racines.forEach(r -> aplatir(r, lignes));
        return new FeuilleExport(nom, COLONNES, lignes, null);
    }

    private static void aplatir(NoeudTresorerieDto n, List<Map<String, Object>> lignes) {
        Map<String, Object> ligne = new HashMap<>();
        ligne.put(LIBELLE, n.libelle());
        ligne.put("niveau", n.niveau());
        for (int m = 1; m <= 12; m++) {
            ligne.put("m" + m, n.montants().get(m - 1));
        }
        ligne.put("total", n.total());
        ligne.put("regle", n.regle());
        lignes.add(ligne);
        n.enfants().forEach(e -> aplatir(e, lignes));
    }

    private record Contexte(int annee, LocalDate aujourdhui, PerimetreAchat perimetre) {
    }

    private record Source(UUID objetId, String type, String namespace, String libelle) {
    }

    private record Echeance(LocalDate date, LocalDate origine, boolean reporte) {
    }

    private record Ligne(Source source, Echeance echeance, UUID projet, String projetLibelle, String groupe,
                         BigDecimal montant, boolean credit) {
    }

    private record Niveau(Function<Ligne, String> cle, Function<Ligne, String> libelle) {
    }

    private record Position(String cle, String libelle, boolean credit, int niveau) {
    }
}
