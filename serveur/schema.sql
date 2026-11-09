-- Schéma de la base de synchronisation de MTG Duel.
-- sync.php crée cette table automatiquement au premier appel ; ce fichier sert
-- de référence, ou à la créer à la main depuis phpPgAdmin.

CREATE TABLE IF NOT EXISTS mtg_enregistrements (
    kind       text   NOT NULL,          -- 'player', 'deck' ou 'game'
    id         text   NOT NULL,          -- identifiant de l'élément côté application
    updated_at bigint NOT NULL,          -- date de dernière modification, en millisecondes
    payload    jsonb  NOT NULL,          -- l'élément complet, tel que l'application le stocke
    PRIMARY KEY (kind, id)
);

-- Pour repartir de zéro côté serveur :
-- TRUNCATE mtg_enregistrements;
