#!/usr/bin/env python3
"""
Migration des pièces jointes binaires vers un stockage objet S3 / MinIO.

Source   : dbo.AdmAttachedFile.AttachedFile (varbinary(max)) — seule colonne binaire
           réellement exploitée par le backend (~3 134 fichiers, ~8,05 Go).
Cible    : un bucket S3-compatible + une ligne dbo.AppFileObject par fichier.
Portée   : LECTURE SEULE sur le schéma legacy. Ce script n'écrit que dans
           AppFileObject et AppFileMigrationFailure. Il ne vide JAMAIS
           AdmAttachedFile.AttachedFile — c'est la phase 3, délibérément séparée et
           conditionnée à l'existence d'une sauvegarde du bucket.

Reprise  : la liste de travail exclut par anti-jointure tout id déjà présent dans
           AppFileObject, et une ligne n'y est écrite qu'après un envoi vérifié.
           Relancer le script après une coupure reprend donc exactement où il en
           était, sans état intermédiaire à nettoyer.

Intégrité: le SHA-256 est calculé avant l'envoi et transmis en ChecksumSHA256 ;
           S3/MinIO le recalcule côté serveur et rejette un transfert corrompu.

Ordre    : les fichiers sont traités du plus petit au plus grand, pour que les
           problèmes de configuration (identifiants, bucket, TLS) apparaissent en
           quelques secondes plutôt qu'après un envoi de 35 Mo.

Usage
-----
    export DB_PWD=...  S3_ACCESS_KEY=...  S3_SECRET_KEY=...

    # 1. inventaire, aucun envoi
    python migrate_blobs_to_s3.py --db-name yesRems --prefix test \\
        --bucket immo-crm --endpoint https://minio.example.com --dry-run

    # 2. échantillon
    python migrate_blobs_to_s3.py ... --prefix test --limit 50

    # 3. passe complète
    python migrate_blobs_to_s3.py ... --prefix test --workers 6
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import logging
import os
import re
import sys
import threading
import time
import urllib.parse
from collections import Counter, defaultdict
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass
from typing import Optional

try:
    import boto3
    import pyodbc
    from botocore.config import Config
    from botocore.exceptions import BotoCoreError, ClientError
except ImportError as exc:  # pragma: no cover
    sys.exit(f"Dépendance manquante ({exc}). Installer : pip install -r requirements.txt")


log = logging.getLogger("migration-s3")


# --------------------------------------------------------------------------------------
# Résolution du porteur
# --------------------------------------------------------------------------------------
#
# `AdmAttachedFile` est polymorphe : le rattachement se fait par `CategoryId` = PKey du
# porteur, sans clé étrangère. `Category` est purement indicatif et, en données réelles,
# partiellement libre — d'où cette table explicite plutôt qu'une déduction.
#
# Comptages relevés sur yesRems (lignes avec binaire présent) :
CATEGORY_TO_PORTEUR = {
    "Yes.Stl.Debit.Invoice": "facture",             # 1622 lignes, 5,5 Go
    "Yes.Stk.Building.Item": "article",             #  476 lignes, 780 Mo
    "Yes.Com.Sales.Sale": "dossier",                #  401 lignes, 1,1 Go
    "Yes.Stl.Credit.Settlement": "reglement",       #  368 lignes, 467 Mo
    "Yes.Stl.Debit.Settlement": "reglement",        #   92 lignes, 215 Mo
    "YesBasics.History.AdmEMailHistory": "email",   #   39 lignes
    "Yes.Com.Sales.ComDocument": "echeance",        #   11 lignes
    "Yes.Grh.GrhVacation": "grh-conge",             #   10 lignes
    "Yes.Grh.GrhEmployee": "grh-employe",           #    4 lignes
    "Yes.Buy.Order": "achat-commande",              #    4 lignes
    "Yes.Lnd.Prospection": "prospection",           #    3 lignes
    "Yes.Buy.Delivery": "achat-livraison",          #    2 lignes
    "Yes.Adm.Attachment.Attachment": "attachment",  #    1 ligne
    "Yes.Buy.CallOfTender.BuySelection": "achat-selection",
    "Yes.Cfg.CfgProject": "projet",
    "Yes.Cfg.CfgTranche": "tranche",
    "Yes.Com.Commun.ActionMessage": "action-message",
    "Yes.Com.Prospection.Prospection": "prospection",
}

# ~110 lignes portent un libellé humain écrit par l'application Windows, du type
# "Dossier de vente N° ARS 2023/00267 du 18/12/2023" (souvent avec un numéro vide).
# Toutes désignent un ComFolder.
PREFIXE_DOSSIER_LEGACY = "Dossier de vente"

# 11 lignes ont CategoryId NULL : elles ne sont rattachées à rien du tout (10 .jpg
# décrits "A".."J" et un en-tête "ENTETE AYAD DEVELOPPEMENT.png"). Elles sont migrées
# quand même — 16 Mo — sous une clé ne portant que leur propre identifiant, pour ne
# rien perdre sans inventer de parent.
PORTEUR_ORPHELIN = "orphelin"
PORTEUR_INCONNU = "autre"


# Miroir de web.ContentTypeParExtension (côté Java), plus .jfif et .heic qui existent en
# données réelles mais manquent à la table Java. Sans conséquence de désynchronisation :
# le type retenu ici est stocké dans AppFileObject.ContentType et c'est lui qui sera
# servi, l'extension ne servant plus au moment du téléchargement.
CONTENT_TYPE_BY_EXT = {
    ".pdf": "application/pdf",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".jfif": "image/jpeg",
    ".png": "image/png",
    ".gif": "image/gif",
    ".heic": "image/heic",
    ".doc": "application/msword",
    ".docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    ".xls": "application/vnd.ms-excel",
    ".xlsx": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    ".txt": "text/plain",
}
CONTENT_TYPE_DEFAUT = "application/octet-stream"

_EXTENSION_VALIDE = re.compile(r"^\.[a-z0-9]{1,10}$")


@dataclass(frozen=True)
class Tache:
    """Une ligne de la liste de travail — métadonnées seules, jamais le binaire."""

    file_id: str
    category: Optional[str]
    category_id: Optional[str]
    extension: str
    description: Optional[str]
    size_bytes: int
    porteur: str
    storage_key: str
    content_type: str


def normaliser_extension(brute: Optional[str]) -> str:
    """`.PDF` / `pdf` / `  .Jpg ` → `.pdf` / `.pdf` / `.jpg`. Rejette tout le reste."""
    if not brute:
        return ""
    ext = brute.strip().lower()
    if not ext.startswith("."):
        ext = "." + ext
    return ext if _EXTENSION_VALIDE.match(ext) else ""


def resoudre_porteur(category: Optional[str], category_id: Optional[str]) -> str:
    if not category_id:
        return PORTEUR_ORPHELIN
    libelle = (category or "").strip()
    if libelle in CATEGORY_TO_PORTEUR:
        return CATEGORY_TO_PORTEUR[libelle]
    if libelle.startswith(PREFIXE_DOSSIER_LEGACY):
        return "dossier"
    return PORTEUR_INCONNU


def construire_cle(prefix: str, porteur: str, category_id: Optional[str],
                   file_id: str, extension: str) -> str:
    """
    `{prefix}/pieces-jointes/{porteur}/{porteurId}/{fileId}{ext}`

    La clé se termine par l'identifiant immuable de la pièce jointe : renommer sa
    description ne déplace jamais l'objet. Le nom de fichier d'origine n'entre jamais
    dans la clé (accents, espaces, données personnelles) — il reste dans
    `AdmAttachedFile.Description` et n'est servi qu'en Content-Disposition.
    """
    if porteur == PORTEUR_ORPHELIN:
        return f"{prefix}/{PORTEUR_ORPHELIN}/{file_id}{extension}"
    return f"{prefix}/pieces-jointes/{porteur}/{category_id}/{file_id}{extension}"


def entete_ascii(valeur: Optional[str], limite: int = 512) -> str:
    """
    Les métadonnées et en-têtes S3 transitent en HTTP : elles doivent être ASCII.
    Les descriptions legacy sont accentuées, on les encode en pourcent-encodage UTF-8.
    """
    if not valeur:
        return ""
    return urllib.parse.quote(valeur.strip()[:limite], safe="")


def content_disposition(description: Optional[str], extension: str) -> str:
    """
    Nom de téléchargement proposé. `filename=` en repli ASCII pour les vieux clients,
    `filename*=` en UTF-8 (RFC 5987) pour le nom réel accentué.
    """
    brut = (description or "fichier").strip() or "fichier"
    if extension and not brut.lower().endswith(extension):
        brut = f"{brut}{extension}"
    repli = re.sub(r'[^A-Za-z0-9._-]', "_", brut)[:120] or "fichier"
    return f"attachment; filename=\"{repli}\"; filename*=UTF-8''{urllib.parse.quote(brut, safe='')}"


# --------------------------------------------------------------------------------------
# Connexions (une par thread — ni pyodbc ni les sessions boto3 ne sont partageables)
# --------------------------------------------------------------------------------------

def charger_env(chemin: str) -> int:
    """
    Charge un fichier `.env` (KEY=VALUE) dans l'environnement du processus.

    Les variables déjà définies dans l'environnement **gagnent** : sur le serveur de
    production, on exporte les secrets sans avoir à supprimer le fichier.

    Volontairement écrit à la main plutôt qu'avec `python-dotenv` : dix lignes contre
    une dépendance de plus sur un outil ponctuel.
    """
    if not os.path.isfile(chemin):
        return 0
    charges = 0
    with open(chemin, "r", encoding="utf-8") as fichier:
        for ligne in fichier:
            ligne = ligne.strip()
            if not ligne or ligne.startswith("#") or "=" not in ligne:
                continue
            cle, _, valeur = ligne.partition("=")
            cle = cle.strip()
            valeur = valeur.strip().strip("'\"")
            if cle and cle not in os.environ:
                os.environ[cle] = valeur
                charges += 1
    return charges


_local = threading.local()


class Contexte:
    def __init__(self, args: argparse.Namespace, db_password: str,
                 access_key: str, secret_key: str):
        self.args = args
        self.chaine_db = (
            f"DRIVER={{{args.driver}}};"
            f"SERVER={args.db_host};"
            f"DATABASE={args.db_name};"
            f"UID={args.db_user};"
            f"PWD={db_password};"
            f"TrustServerCertificate=yes;"
        )
        self.access_key = access_key
        self.secret_key = secret_key

    def db(self) -> "pyodbc.Connection":
        if not hasattr(_local, "db"):
            _local.db = pyodbc.connect(self.chaine_db, autocommit=True)
        return _local.db

    def s3(self):
        if not hasattr(_local, "s3"):
            _local.s3 = boto3.client(
                "s3",
                endpoint_url=self.args.endpoint,
                region_name=self.args.region,
                aws_access_key_id=self.access_key,
                aws_secret_access_key=self.secret_key,
                config=Config(
                    # MinIO n'accepte pas l'adressage par sous-domaine de bucket.
                    s3={"addressing_style": "path"},
                    retries={"max_attempts": 3, "mode": "standard"},
                    connect_timeout=15,
                    read_timeout=300,
                ),
            )
        return _local.s3


# --------------------------------------------------------------------------------------
# Phase 1 — liste de travail
# --------------------------------------------------------------------------------------

SQL_LISTE_TRAVAIL = """
SELECT a.PKey, a.Category, a.CategoryId, a.Extention, a.Description,
       DATALENGTH(a.AttachedFile) AS SizeBytes
FROM dbo.AdmAttachedFile a
WHERE a.AttachedFile IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM dbo.AppFileObject o WHERE o.AdmAttachedFileId = a.PKey)
ORDER BY DATALENGTH(a.AttachedFile) ASC
"""

# Variante sans anti-jointure, pour un inventaire à blanc sur une base où le schéma
# applicatif n'a pas encore été appliqué (db/migration/002). Réservée à --dry-run :
# sans AppFileObject il n'y a ni reprise ni idempotence possibles.
SQL_LISTE_TRAVAIL_SANS_LEDGER = """
SELECT a.PKey, a.Category, a.CategoryId, a.Extention, a.Description,
       DATALENGTH(a.AttachedFile) AS SizeBytes
FROM dbo.AdmAttachedFile a
WHERE a.AttachedFile IS NOT NULL
ORDER BY DATALENGTH(a.AttachedFile) ASC
"""


def colonne_existe(ctx: Contexte, table: str, colonne: str) -> bool:
    """
    Après `004_drop_attachedfile_filestream.sql`, `AdmAttachedFile.AttachedFile`
    n'existe plus : la base ne stocke plus aucun binaire, tout est sur S3. Les requêtes
    qui la nomment doivent donc être conditionnelles, sinon l'outil se casse sur la base
    qu'il vient lui-même de faire migrer.
    """
    curseur = ctx.db().cursor()
    curseur.execute("SELECT COL_LENGTH(?, ?)", f"dbo.{table}", colonne)
    present = curseur.fetchone()[0] is not None
    curseur.close()
    return present


def table_existe(ctx: Contexte, nom: str) -> bool:
    curseur = ctx.db().cursor()
    curseur.execute("SELECT OBJECT_ID(?, 'U')", f"dbo.{nom}")
    present = curseur.fetchone()[0] is not None
    curseur.close()
    return present


def construire_liste(ctx: Contexte) -> list[Tache]:
    if not colonne_existe(ctx, "AdmAttachedFile", "AttachedFile"):
        raise SystemExit(
            "La colonne AdmAttachedFile.AttachedFile n'existe plus (script 004 applique) : "
            "cette base ne contient plus aucun binaire a migrer. Utiliser --verify pour "
            "controler la coherence base <-> stockage."
        )

    if table_existe(ctx, "AppFileObject"):
        requete = SQL_LISTE_TRAVAIL
    elif ctx.args.dry_run:
        log.warning("Table AppFileObject absente — inventaire sur la totalité des "
                    "fichiers, sans exclusion des déjà-migrés.")
        requete = SQL_LISTE_TRAVAIL_SANS_LEDGER
    else:
        raise SystemExit(
            "Table dbo.AppFileObject absente. Appliquer d'abord db/migration/002 "
            "(et 003) sur cette base avec un compte disposant du droit CREATE TABLE."
        )

    curseur = ctx.db().cursor()
    curseur.execute(requete)
    taches: list[Tache] = []
    for pkey, category, category_id, extention, description, size in curseur.fetchall():
        file_id = str(pkey).lower()
        parent_id = str(category_id).lower() if category_id else None
        extension = normaliser_extension(extention)
        porteur = resoudre_porteur(category, parent_id)
        taches.append(Tache(
            file_id=file_id,
            category=category,
            category_id=parent_id,
            extension=extension,
            description=description,
            size_bytes=int(size or 0),
            porteur=porteur,
            storage_key=construire_cle(ctx.args.prefix, porteur, parent_id, file_id, extension),
            content_type=CONTENT_TYPE_BY_EXT.get(extension, CONTENT_TYPE_DEFAUT),
        ))
    curseur.close()
    if ctx.args.limit:
        taches = taches[: ctx.args.limit]
    return taches


# --------------------------------------------------------------------------------------
# Phase 2 — reconstruction du fichier, envoi, enregistrement
# --------------------------------------------------------------------------------------

SQL_BINAIRE = "SELECT AttachedFile FROM dbo.AdmAttachedFile WHERE PKey = ?"

SQL_INSERT_OBJET = """
INSERT INTO dbo.AppFileObject
    (AdmAttachedFileId, StorageKey, ContentType, SizeBytes, Sha256, UploadedAt, UploadedBy)
VALUES (?, ?, ?, ?, ?, SYSUTCDATETIME(), 'migration')
"""

SQL_INSERT_ECHEC = """
INSERT INTO dbo.AppFileMigrationFailure
    (AdmAttachedFileId, Attempt, ErrorType, ErrorMessage, StorageKey, FailedAt)
VALUES (?, ?, ?, ?, ?, SYSUTCDATETIME())
"""


def traiter(ctx: Contexte, tache: Tache) -> int:
    """Renvoie le nombre d'octets envoyés. Lève l'exception en cas d'échec définitif."""
    derniere: Optional[Exception] = None

    for tentative in range(1, ctx.args.retries + 1):
        try:
            # 1. reconstruction du fichier depuis le binaire — un seul fichier en
            #    mémoire à la fois (35 Mo au pire, 2,6 Mo en moyenne).
            curseur = ctx.db().cursor()
            curseur.execute(SQL_BINAIRE, tache.file_id)
            ligne = curseur.fetchone()
            curseur.close()
            if ligne is None or ligne[0] is None:
                raise RuntimeError("binaire absent au moment de la lecture")
            contenu = bytes(ligne[0])

            # 2. empreinte calculée avant l'envoi, transmise à S3 qui la revérifie.
            digest = hashlib.sha256(contenu).digest()

            # 3. envoi. ChecksumSHA256 fait rejeter le transfert par le serveur en cas
            #    d'altération — plus sûr qu'une comparaison a posteriori.
            ctx.s3().put_object(
                Bucket=ctx.args.bucket,
                Key=tache.storage_key,
                Body=contenu,
                ContentType=tache.content_type,
                ContentDisposition=content_disposition(tache.description, tache.extension),
                ChecksumAlgorithm="SHA256",
                ChecksumSHA256=base64.b64encode(digest).decode("ascii"),
                Metadata={
                    "source-pkey": tache.file_id,
                    "source-category": entete_ascii(tache.category, 255),
                    "source-category-id": tache.category_id or "",
                    "source-description": entete_ascii(tache.description, 255),
                },
            )

            # 4. la ligne n'est écrite qu'après un envoi vérifié : sa présence vaut
            #    "fait", et c'est ce qui rend la reprise fiable.
            curseur = ctx.db().cursor()
            curseur.execute(SQL_INSERT_OBJET, tache.file_id, tache.storage_key,
                            tache.content_type, len(contenu), digest.hex())
            curseur.close()
            return len(contenu)

        except (ClientError, BotoCoreError, pyodbc.Error, RuntimeError) as exc:
            derniere = exc
            if tentative < ctx.args.retries:
                attente = 2 ** tentative
                log.warning("%s — tentative %d/%d échouée (%s), nouvelle tentative dans %ds",
                            tache.file_id, tentative, ctx.args.retries, exc, attente)
                time.sleep(attente)

    journaliser_echec(ctx, tache, ctx.args.retries, derniere)
    raise derniere  # type: ignore[misc]


def journaliser_echec(ctx: Contexte, tache: Tache, tentative: int, exc: Optional[Exception]) -> None:
    try:
        curseur = ctx.db().cursor()
        curseur.execute(SQL_INSERT_ECHEC, tache.file_id, tentative,
                        type(exc).__name__ if exc else None, str(exc) if exc else None,
                        tache.storage_key)
        curseur.close()
    except pyodbc.Error:
        log.exception("Impossible de journaliser l'échec de %s", tache.file_id)


# --------------------------------------------------------------------------------------
# Rapports
# --------------------------------------------------------------------------------------

def mo(octets: int) -> str:
    return f"{octets / 1048576:.1f} Mo"


def rapport_inventaire(taches: list[Tache]) -> None:
    par_porteur: dict[str, list[int]] = defaultdict(list)
    for t in taches:
        par_porteur[t.porteur].append(t.size_bytes)

    total = sum(t.size_bytes for t in taches)
    log.info("=" * 72)
    log.info("INVENTAIRE — %d fichiers, %s", len(taches), mo(total))
    log.info("=" * 72)
    for porteur, tailles in sorted(par_porteur.items(), key=lambda kv: -sum(kv[1])):
        log.info("  %-18s %5d fichiers  %12s", porteur, len(tailles), mo(sum(tailles)))

    inconnues = Counter(t.category for t in taches if t.porteur == PORTEUR_INCONNU)
    if inconnues:
        log.warning("Catégories non cartographiées (rangées sous '%s/') :", PORTEUR_INCONNU)
        for categorie, n in inconnues.most_common():
            log.warning("    %-45s %d", repr(categorie), n)

    sans_extension = sum(1 for t in taches if not t.extension)
    if sans_extension:
        log.warning("%d fichiers sans extension exploitable → %s",
                    sans_extension, CONTENT_TYPE_DEFAUT)

    doublons = [cle for cle, n in Counter(t.storage_key for t in taches).items() if n > 1]
    if doublons:
        log.error("COLLISION DE CLÉS (%d) — à corriger avant tout envoi : %s",
                  len(doublons), doublons[:5])


# --------------------------------------------------------------------------------------

def executer(ctx: Contexte, taches: list[Tache]) -> int:
    total_octets = sum(t.size_bytes for t in taches)
    debut = time.monotonic()
    verrou = threading.Lock()
    etat = {"faits": 0, "octets": 0, "echecs": 0}

    def progresser(envoyes: int, echec: bool) -> None:
        with verrou:
            etat["faits"] += 1
            etat["octets"] += envoyes
            etat["echecs"] += 1 if echec else 0
            faits, octets = etat["faits"], etat["octets"]
        if faits % 25 == 0 or faits == len(taches):
            ecoule = max(time.monotonic() - debut, 0.001)
            debit = octets / 1048576 / ecoule
            restant = (total_octets - octets) / 1048576 / debit if debit > 0 else 0
            log.info("%d/%d — %s envoyés — %.1f Mo/s — reste ~%.0f min — %d échec(s)",
                     faits, len(taches), mo(octets), debit, restant / 60, etat["echecs"])

    with ThreadPoolExecutor(max_workers=ctx.args.workers) as pool:
        futurs = {pool.submit(traiter, ctx, t): t for t in taches}
        for futur in as_completed(futurs):
            tache = futurs[futur]
            try:
                progresser(futur.result(), echec=False)
            except Exception as exc:  # noqa: BLE001 — un échec ne doit jamais arrêter la passe
                log.error("ÉCHEC %s (%s) : %s", tache.file_id, tache.storage_key, exc)
                progresser(0, echec=True)

    ecoule = time.monotonic() - debut
    log.info("=" * 72)
    log.info("TERMINÉ — %d/%d fichiers, %s, en %.0f min, %d échec(s)",
             etat["faits"] - etat["echecs"], len(taches), mo(etat["octets"]),
             ecoule / 60, etat["echecs"])
    if etat["echecs"]:
        log.warning("Relancer la même commande : la liste de travail exclura "
                    "automatiquement les fichiers déjà migrés.")
    log.info("=" * 72)
    return 1 if etat["echecs"] else 0


ENV_PAR_DEFAUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env")


def verifier_acces(ctx: Contexte) -> int:
    """
    Aller-retour complet sur un objet témoin : écriture avec ChecksumSHA256, relecture,
    comparaison, suppression. Valide d'un coup l'endpoint, le TLS, les identifiants,
    les droits d'écriture ET de lecture, et le support du checksum côté serveur — sans
    toucher à la base ni dépendre du schéma applicatif.
    """
    cle = f"{ctx.args.prefix}/.preflight/temoin.txt"
    contenu = b"migration immo-crm - objet temoin, supprimable sans risque.\n"
    digest = hashlib.sha256(contenu).digest()
    s3 = ctx.s3()

    try:
        s3.head_bucket(Bucket=ctx.args.bucket)
        log.info("Bucket '%s' accessible.", ctx.args.bucket)

        s3.put_object(Bucket=ctx.args.bucket, Key=cle, Body=contenu,
                      ContentType="text/plain", ChecksumAlgorithm="SHA256",
                      ChecksumSHA256=base64.b64encode(digest).decode("ascii"))
        log.info("Écriture OK (checksum SHA-256 accepté et vérifié par le serveur).")

        relu = s3.get_object(Bucket=ctx.args.bucket, Key=cle)["Body"].read()
        if relu != contenu:
            log.error("Relecture incohérente : %d octets écrits, %d relus.",
                      len(contenu), len(relu))
            return 1
        log.info("Relecture OK (%d octets, contenu identique).", len(relu))

        s3.delete_object(Bucket=ctx.args.bucket, Key=cle)
        log.info("Suppression OK. Accès au stockage validé de bout en bout.")
        return 0

    except ClientError as exc:
        code = exc.response.get("Error", {}).get("Code", "?")
        log.error("Refus du serveur (%s) : %s", code, exc)
        if code in ("InvalidAccessKeyId", "SignatureDoesNotMatch"):
            log.error("→ vérifier S3_ACCESS_KEY / S3_SECRET_KEY dans le .env.")
        elif code in ("NoSuchBucket", "404"):
            log.error("→ le bucket '%s' n'existe pas sur cet endpoint.", ctx.args.bucket)
        elif code in ("AccessDenied", "403"):
            log.error("→ identifiants valides mais droits insuffisants sur le bucket.")
        return 1
    except BotoCoreError as exc:
        log.error("Endpoint injoignable (%s) : %s", type(exc).__name__, exc)
        return 1


SQL_A_VERIFIER = """
SELECT o.AdmAttachedFileId, o.StorageKey, o.SizeBytes, o.Sha256,
       DATALENGTH(a.AttachedFile) AS TailleSource
FROM dbo.AppFileObject o
LEFT JOIN dbo.AdmAttachedFile a ON a.PKey = o.AdmAttachedFileId
WHERE o.DeletedAt IS NULL
"""

SQL_A_VERIFIER_SANS_SOURCE = """
SELECT o.AdmAttachedFileId, o.StorageKey, o.SizeBytes, o.Sha256, CAST(NULL AS BIGINT)
FROM dbo.AppFileObject o
WHERE o.DeletedAt IS NULL
"""

SQL_RESTANTS = """
SELECT COUNT(*) FROM dbo.AdmAttachedFile a
WHERE a.AttachedFile IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM dbo.AppFileObject o WHERE o.AdmAttachedFileId = a.PKey)
"""


def verifier_migration(ctx: Contexte) -> int:
    """
    Contrôle exhaustif avant purge des binaires.

    Pour chaque ligne d'AppFileObject, un HEAD sur l'objet compare sa taille et son
    empreinte SHA-256 à ce qu'annonce la base. MinIO renvoyant le checksum enregistré à
    l'écriture, la couverture est totale **sans retélécharger les 8 Go** — 3 138 requêtes
    HEAD au lieu de 8 Go de trafic.

    Trois conditions doivent être réunies pour autoriser la purge :
      1. aucun fichier restant à migrer ;
      2. aucun échec non résolu ;
      3. aucun écart de taille ou d'empreinte sur les objets déjà migrés.
    """
    binaires_en_base = colonne_existe(ctx, "AdmAttachedFile", "AttachedFile")

    curseur = ctx.db().cursor()
    if binaires_en_base:
        curseur.execute(SQL_RESTANTS)
        restants = curseur.fetchone()[0]
        curseur.execute(SQL_A_VERIFIER)
    else:
        # Colonne supprimée (script 004) : plus rien ne peut rester à migrer, et il n'y
        # a plus de taille source à confronter — le contrôle porte alors sur la seule
        # cohérence base ↔ stockage, ce qui reste exactement ce qu'on veut vérifier
        # après une restauration.
        restants = 0
        curseur.execute(SQL_A_VERIFIER_SANS_SOURCE)
    lignes = curseur.fetchall()
    curseur.execute("SELECT COUNT(*) FROM dbo.AppFileMigrationFailure")
    echecs = curseur.fetchone()[0]
    curseur.close()

    if not binaires_en_base:
        log.info("Colonne AttachedFile absente : base deja purgee, controle base ↔ stockage seul.")
    log.info("%d objets à vérifier — %d fichier(s) non migré(s), %d échec(s) journalisé(s)",
             len(lignes), restants, echecs)

    anomalies: list[str] = []
    verrou = threading.Lock()
    compteur = {"n": 0}

    def controler(ligne) -> None:
        file_id, cle, taille_db, sha_db, taille_source = ligne
        try:
            tete = ctx.s3().head_object(Bucket=ctx.args.bucket, Key=cle, ChecksumMode="ENABLED")
        except ClientError as exc:
            with verrou:
                anomalies.append(f"{cle} — objet absent ou inaccessible ({exc.response.get('Error', {}).get('Code')})")
            return

        ecarts = []
        if tete["ContentLength"] != taille_db:
            ecarts.append(f"taille S3 {tete['ContentLength']} vs base {taille_db}")
        if taille_source is not None and taille_source != taille_db:
            ecarts.append(f"taille source {taille_source} vs base {taille_db}")
        sha_s3 = tete.get("ChecksumSHA256")
        if sha_s3 and sha_db:
            if base64.b64decode(sha_s3).hex() != sha_db.lower():
                ecarts.append("empreinte SHA-256 différente")
        elif not sha_s3:
            ecarts.append("aucun checksum enregistré côté serveur")
        if ecarts:
            with verrou:
                anomalies.append(f"{cle} — " + " ; ".join(ecarts))

        with verrou:
            compteur["n"] += 1
            if compteur["n"] % 500 == 0:
                log.info("  %d/%d vérifiés", compteur["n"], len(lignes))

    with ThreadPoolExecutor(max_workers=ctx.args.workers) as pool:
        list(pool.map(controler, lignes))

    log.info("=" * 72)
    if anomalies:
        log.error("%d ANOMALIE(S) :", len(anomalies))
        for detail in anomalies[:20]:
            log.error("    %s", detail)
        if len(anomalies) > 20:
            log.error("    … et %d autre(s)", len(anomalies) - 20)

    pret = not anomalies and restants == 0 and echecs == 0
    if pret:
        log.info("VÉRIFICATION CONFORME — %d objets, taille et empreinte identiques "
                 "en base et sur le stockage.", len(lignes))
        if binaires_en_base:
            log.info("La purge des binaires (migration/purge_attached_file.sql) peut être exécutée.")
        else:
            log.info("Base deja purgee et colonne supprimee : rien de plus à faire.")
    else:
        log.error("VÉRIFICATION NON CONFORME — NE PAS PURGER LES BINAIRES.")
        if restants:
            log.error("    %d fichier(s) restent à migrer : relancer une passe.", restants)
        if echecs:
            log.error("    %d échec(s) journalisé(s) dans AppFileMigrationFailure.", echecs)
    log.info("=" * 72)
    return 0 if pret else 1


def analyser_arguments(argv: Optional[list[str]] = None) -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Migration des pièces jointes binaires vers S3 / MinIO.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="Secrets attendus en variables d'environnement : "
               "DB_PWD, S3_ACCESS_KEY, S3_SECRET_KEY.",
    )
    p.add_argument("--db-host", default=os.environ.get("DB_HOST", "localhost,49687"))
    p.add_argument("--db-name", default=os.environ.get("DB_NAME", "yesRems"))
    p.add_argument("--db-user", default=os.environ.get("DB_USER", "tsix"))
    p.add_argument("--driver", default=os.environ.get("ODBC_DRIVER", "ODBC Driver 17 for SQL Server"))

    p.add_argument("--bucket", default=os.environ.get("S3_BUCKET"))
    p.add_argument("--endpoint", default=os.environ.get("S3_ENDPOINT"),
                   help="URL MinIO, ex. https://minio.example.com. Omettre pour AWS S3.")
    p.add_argument("--region", default=os.environ.get("S3_REGION", "us-east-1"))
    p.add_argument("--prefix", required=True,
                   help="Premier segment de clé, ex. 'test' ou 'prod'. Indispensable : la "
                        "base de prod étant une restauration, les PKey sont identiques et "
                        "des clés sans préfixe distinct s'écraseraient entre elles.")

    p.add_argument("--check", action="store_true",
                   help="Vérifie l'accès au bucket (aller-retour d'un objet témoin, "
                        "checksum compris) puis s'arrête. Ne touche pas à la base.")
    p.add_argument("--verify", action="store_true",
                   help="Contrôle exhaustif avant purge : compare taille et empreinte "
                        "SHA-256 de chaque objet à la base, par requêtes HEAD (sans "
                        "retélécharger). Sortie 0 seulement si tout est conforme.")
    p.add_argument("--dry-run", action="store_true", help="Inventaire seul, aucun envoi.")
    p.add_argument("--limit", type=int, help="N plus petits fichiers seulement (échantillon).")
    p.add_argument("--workers", type=int, default=4)
    p.add_argument("--retries", type=int, default=3)
    p.add_argument("--env-file", default=ENV_PAR_DEFAUT,
                   help="Fichier de secrets au format KEY=VALUE. Par défaut "
                        "migration/.env (ignoré par git). Les variables déjà "
                        "présentes dans l'environnement priment.")
    p.add_argument("--log-file")
    p.add_argument("--verbose", action="store_true")
    return p.parse_args(argv)


def main(argv: Optional[list[str]] = None) -> int:
    argv = list(sys.argv[1:] if argv is None else argv)

    # Le .env doit être chargé AVANT argparse, dont les valeurs par défaut lisent
    # os.environ — d'où cette pré-lecture manuelle de --env-file.
    chemin_env = ENV_PAR_DEFAUT
    if "--env-file" in argv:
        position = argv.index("--env-file")
        if position + 1 < len(argv):
            chemin_env = argv[position + 1]
    charges = charger_env(chemin_env)

    args = analyser_arguments(argv)

    # La console Windows n'est pas en UTF-8 par défaut : sans cela les messages et les
    # descriptions accentuées ressortent en mojibake.
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except (AttributeError, OSError):  # pragma: no cover
        pass

    handlers: list[logging.Handler] = [logging.StreamHandler(sys.stdout)]
    if args.log_file:
        handlers.append(logging.FileHandler(args.log_file, encoding="utf-8"))
    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s %(levelname)-7s %(message)s",
        datefmt="%H:%M:%S",
        handlers=handlers,
    )

    if charges:
        log.info("%d variable(s) chargée(s) depuis %s", charges, chemin_env)

    db_password = os.environ.get("DB_PWD")
    if not db_password:
        log.error("DB_PWD absent. Le renseigner dans %s (voir .env.example) "
                  "ou l'exporter dans l'environnement.", chemin_env)
        return 2

    access_key = os.environ.get("S3_ACCESS_KEY", "")
    secret_key = os.environ.get("S3_SECRET_KEY", "")
    if not args.dry_run and not (access_key and secret_key):
        log.error("S3_ACCESS_KEY / S3_SECRET_KEY absents. Les renseigner dans %s "
                  "(voir .env.example) ou les exporter dans l'environnement.", chemin_env)
        return 2

    if not args.bucket and not args.dry_run:
        log.error("Bucket non précisé : utiliser --bucket ou définir S3_BUCKET.")
        return 2

    ctx = Contexte(args, db_password, access_key, secret_key)

    log.info("Base %s sur %s — bucket '%s' préfixe '%s'%s",
             args.db_name, args.db_host, args.bucket, args.prefix,
             f" via {args.endpoint}" if args.endpoint else " (AWS S3)")

    if args.check:
        return verifier_acces(ctx)

    if args.verify:
        return verifier_migration(ctx)

    taches = construire_liste(ctx)
    if not taches:
        log.info("Rien à migrer : tous les fichiers ont déjà une ligne AppFileObject.")
        return 0

    rapport_inventaire(taches)

    if args.dry_run:
        log.info("--dry-run : aucun envoi effectué.")
        for t in taches[:5]:
            log.info("  exemple de clé : %s", t.storage_key)
        return 0

    return executer(ctx, taches)


if __name__ == "__main__":
    sys.exit(main())
