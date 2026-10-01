-- ============================================================================
-- Visibilite des entites juridiques : de la liste figee au drapeau en base.
--
-- ATTENTION — ce script est le SEUL contenu de cette migration, et il modifie
-- des DONNEES d'une table legataire (CfgCompany), pas une structure. Il n'est
-- pas joue automatiquement : appliquez-le a la main, sur chaque base concernee.
--
-- Avant ce changement, la visibilite venait d'une constante Java figee a
-- ADMOD + GHRIZA. Le code lit desormais CfgCompany.IsActive, qui existait deja
-- dans le schema legataire mais que PERSONNE NE MAINTENAIT : au moment du
-- basculement, les NEUF entites de la base reelle etaient a 1, y compris
-- YESTEST et quatre entites sans aucun projet.
--
-- Sans ce script, le basculement elargit donc les listes de 2 entites a 9 et
-- expose des donnees de test. Avec lui, restent visibles les entites en
-- exploitation :
--
--   ADMOD     AYAD Private Ferme Bretone (tranche APF)     33 dossiers
--   GHRIZA    AYAD Residence Bel Air     (tranche ARS)     83 dossiers
--   MAYLINE   AYAD Residence Tribeca     (tranche ART)     26 dossiers  -- ajoutee le 01/10/2026
--
-- BENACEUR (200 dossiers) reste DESACTIVEE : ses donnees existent en base mais
-- sont hors du perimetre produit. Elle pese pourtant plus que les trois entites
-- retenues reunies, ce qui surprend a la lecture des volumes — c'est une
-- decision produit, pas une deduction des donnees.
--
-- MAYLINE / TRIBECA (01/10/2026) : la tranche ART est en exploitation dans
-- OMRAN (IsForSale = 1, 80 articles, 3 794 prospections dont 64 en septembre
-- 2026, 21 utilisateurs habilites sur le projet). Elle n'apparaissait pas dans
-- l'application parce que MAYLINE etait a 0 sur les copies. La reactiver ici
-- la fait entrer dans les referentiels, les listes et les tableaux de bord pour
-- les utilisateurs habilites sur ART.
--
-- OU LE JOUER : sur les COPIES de lecture uniquement — yesRemsDev de chaque VPS,
-- yesRemsProd (copie locale), toute base de demonstration. JAMAIS sur la source
-- de verite (192.168.11.115 / 197.230.172.136) : elle garde ses neuf entites a 1
-- pour OMRAN, et CfgCompany n'est pas sous change tracking, donc chaque copie
-- conserve la valeur qu'on lui donne ici. Une copie ou il n'est pas joue
-- affiche les neuf.
--
-- POUR AJOUTER UNE ENTITE SANS TOUT REJOUER (p.ex. MAYLINE sur une copie deja
-- traitee) :  UPDATE dbo.CfgCompany SET IsActive = 1 WHERE Code = 'MAYLINE';
--
-- RISQUE A CONNAITRE : IsActive appartient au schema legataire, donc
-- potentiellement lu par l'application Windows (OMRAN Estate) toujours en
-- service. Le fait que les neuf entites soient a 1 sur la source suggere
-- qu'elle ne s'en sert pas, mais ce n'est pas verifie. C'est la raison de ne
-- jamais jouer ce script sur la source.
-- ============================================================================

-- Formule inversee : on desactive TOUT, puis on reactive la liste retenue. Une
-- enumeration des entites a masquer laisserait passer toute entite creee plus
-- tard dans l'application Windows, qui apparaitrait alors sans qu'on l'ait
-- decide. Ici le defaut est l'invisibilite, comme cote Java ou le filtre est
-- IsActive = true et non != false.
UPDATE dbo.CfgCompany SET IsActive = 0;
GO

UPDATE dbo.CfgCompany
SET IsActive = 1
WHERE Code IN ('ADMOD', 'GHRIZA', 'MAYLINE');
GO

-- Controle : doit renvoyer exactement ADMOD, GHRIZA et MAYLINE.
SELECT Code, Description, IsActive
FROM dbo.CfgCompany
WHERE IsActive = 1
ORDER BY Code;
GO
