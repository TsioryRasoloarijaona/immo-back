package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Lignes {@code AdmAttachedFile} et pointeurs {@code AppFileObject} de FEATURE-admin-fichiers-attaches.md, écrites en
 * SQL dans la transaction du test : {@code IsCheckedButNotAttached} et {@code OpenBeeDocumentId} ne sont pas mappés.
 * {@code CategoryId} n'a pas de FK (polymorphe) ; {@code AdmAttachedFileTypeId} en a une en CI : créer le type d'abord.
 */
@Component
public class FixtureFichiersAttaches {

    public static final String DOSSIER = "Yes.Com.Sales.Sale";
    public static final String ARTICLE = "Yes.Stk.Building.Item";
    public static final String REGLEMENT_CREDIT = "Yes.Stl.Credit.Settlement";
    public static final String FACTURE_ACHAT = "Yes.Stl.Debit.Invoice";

    public static final String CATEGORIE = "Category";
    public static final String CATEGORIE_LIBELLE = "CategoryDescription";
    public static final String OBJET_ID = "CategoryId";
    public static final String DESCRIPTION = "Description";
    public static final String EXTENSION = "Extention";
    public static final String TYPE_ID = "AdmAttachedFileTypeId";
    public static final String GROUPE = "FoderGroup";
    public static final String CREE_PAR = "UserCreate";
    public static final String CREE_LE = "UserCreateDateTime";
    public static final String MODIFIE_PAR = "UserUpdate";
    public static final String MODIFIE_LE = "UserUpdateDateTime";
    public static final String COCHEE_SANS_FICHIER = "IsCheckedButNotAttached";

    private static final String PKEY = "PKey";

    private final EntityManager entityManager;

    public FixtureFichiersAttaches(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Type de pièce ({@code AdmAttachedFileType}) au même code et libellé. */
    public UUID type(String libelle) {
        UUID id = UUID.randomUUID();
        entityManager.createNativeQuery(
                        "INSERT INTO AdmAttachedFileType (PKey, Code, Description) VALUES (:id, :libelle, :libelle)")
                .setParameter("id", id)
                .setParameter("libelle", libelle)
                .executeUpdate();
        return id;
    }

    /** Pièce de dossier avec un fichier vivant, sans type ni groupe, créée le {@code creeLe}. */
    public Piece piece(String description, LocalDateTime creeLe) {
        return new Piece()
                .avec(CATEGORIE, DOSSIER)
                .avec(CATEGORIE_LIBELLE, "Dossier de vente test")
                .avec(OBJET_ID, UUID.randomUUID())
                .avec(DESCRIPTION, description)
                .avec(EXTENSION, ".pdf")
                .avec(GROUPE, "")
                .avec(CREE_PAR, "Karim Test")
                .avec(CREE_LE, creeLe)
                .avec("OpenBeeDocumentId", -1)
                .avec(COCHEE_SANS_FICHIER, false);
    }

    /** Clé de stockage posée sur le pointeur de la pièce. */
    public static String cle(UUID pieceId) {
        return "admin-fichiers/" + pieceId + ".pdf";
    }

    /** Empreinte des deux lignes d'une pièce, pour prouver qu'une lecture n'écrit rien. */
    public List<Object> empreinte(UUID pieceId) {
        entityManager.flush();
        return List.of(
                entityManager.createNativeQuery(
                                "SELECT CHECKSUM_AGG(BINARY_CHECKSUM(*)) FROM AdmAttachedFile WHERE PKey = :id")
                        .setParameter("id", pieceId).getSingleResult(),
                entityManager.createNativeQuery(
                                "SELECT CHECKSUM_AGG(BINARY_CHECKSUM(*)) FROM AppFileObject WHERE AdmAttachedFileId = :id")
                        .setParameter("id", pieceId).getSingleResult());
    }

    /** PKey en minuscules telles que le fil les rend, dans l'ordre SQL Server de {@code uniqueidentifier}. */
    public List<String> idsParPKey(String description) {
        return entityManager.createNativeQuery(
                        "SELECT CONVERT(varchar(36), PKey) FROM AdmAttachedFile WHERE Description = :d ORDER BY PKey ASC",
                        String.class)
                .setParameter("d", description)
                .getResultList().stream().map(id -> id.toString().toLowerCase(Locale.ROOT)).toList();
    }

    /** Pièce à insérer : colonnes posées seulement ; pointeur {@code AppFileObject} vivant par défaut. */
    public final class Piece {

        private final Map<String, Object> colonnes = new LinkedHashMap<>();
        private boolean fichier = true;
        private boolean fichierSupprime;

        private Piece() {
            colonnes.put(PKEY, UUID.randomUUID());
        }

        public Piece avec(String colonne, Object valeur) {
            colonnes.put(colonne, valeur);
            return this;
        }

        public Piece sans(String colonne) {
            colonnes.remove(colonne);
            return this;
        }

        /** Case cochée, aucun fichier : {@code IsCheckedButNotAttached = 1}, sans {@code AppFileObject}. */
        public Piece cocheeSansFichier() {
            colonnes.put(COCHEE_SANS_FICHIER, true);
            colonnes.remove(EXTENSION);
            fichier = false;
            return this;
        }

        /** Pointeur présent mais supprimé logiquement ({@code DeletedAt} renseigné). */
        public Piece fichierSupprime() {
            fichierSupprime = true;
            return this;
        }

        public UUID inserer() {
            UUID id = (UUID) colonnes.get(PKEY);
            String noms = String.join(", ", colonnes.keySet());
            String valeurs = ":" + String.join(", :", colonnes.keySet());
            Query insertion = entityManager.createNativeQuery(
                    "INSERT INTO AdmAttachedFile (" + noms + ") VALUES (" + valeurs + ")");
            colonnes.forEach(insertion::setParameter);
            insertion.executeUpdate();
            if (fichier) {
                pointeur(id);
            }
            return id;
        }

        private void pointeur(UUID id) {
            LocalDateTime depose = LocalDateTime.of(2026, 1, 5, 10, 0);
            entityManager.createNativeQuery("""
                            INSERT INTO AppFileObject (AdmAttachedFileId, StorageKey, ContentType, SizeBytes, UploadedAt,
                                                       UploadedBy)
                            VALUES (:id, :cle, 'application/pdf', 3, :depose, 'migration')""")
                    .setParameter("id", id)
                    .setParameter("cle", cle(id))
                    .setParameter("depose", depose)
                    .executeUpdate();
            if (fichierSupprime) {
                entityManager.createNativeQuery("""
                                UPDATE AppFileObject SET DeletedAt = :supprime, DeletedBy = 'test'
                                WHERE AdmAttachedFileId = :id""")
                        .setParameter("supprime", depose.plusDays(1))
                        .setParameter("id", id)
                        .executeUpdate();
            }
        }
    }
}
