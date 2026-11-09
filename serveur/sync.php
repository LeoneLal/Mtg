<?php
/**
 * API de synchronisation de MTG Duel.
 *
 * Un seul point d'entrée : l'application envoie tout son état, le serveur fusionne
 * enregistrement par enregistrement (le plus récent gagne) et renvoie l'état complet.
 *
 * Déploiement : déposer ce fichier et config.php dans le répertoire web du site,
 * puis renseigner l'adresse https://<votre-site>/sync.php dans les réglages de l'app.
 */

declare(strict_types=1);

const TAILLE_MAX_OCTETS = 8 * 1024 * 1024; // 8 Mo : très large pour des milliers de parties
const TABLE = 'mtg_enregistrements';
const TYPES = ['player', 'deck', 'game'];

header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');

function repondre(int $code, array $corps): void
{
    http_response_code($code);
    echo json_encode($corps, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function erreur(int $code, string $message): void
{
    repondre($code, ['error' => $message]);
}

// --------------------------------------------------------------- configuration

$configuration = __DIR__ . '/config.php';
if (!is_file($configuration)) {
    erreur(500, "config.php est absent : copiez-y les identifiants de la base et le jeton partagé.");
}
require $configuration;

foreach (['DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD', 'SYNC_TOKEN'] as $requis) {
    if (!defined($requis) || constant($requis) === '') {
        erreur(500, "La constante $requis n'est pas définie dans config.php.");
    }
}
if (strlen((string) SYNC_TOKEN) < 16) {
    erreur(500, 'SYNC_TOKEN doit faire au moins 16 caractères.');
}

// ---------------------------------------------------------------- authentification

function jetonFourni(): string
{
    $entetes = function_exists('getallheaders') ? getallheaders() : [];
    $entetes = array_change_key_case($entetes, CASE_LOWER);

    $autorisation = $entetes['authorization']
        ?? ($_SERVER['HTTP_AUTHORIZATION'] ?? ($_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? ''));
    if (preg_match('/^Bearer\s+(.+)$/i', trim((string) $autorisation), $trouve) === 1) {
        return trim($trouve[1]);
    }

    // Repli : certains hébergements effacent l'en-tête Authorization.
    return trim((string) ($entetes['x-sync-token'] ?? ($_SERVER['HTTP_X_SYNC_TOKEN'] ?? '')));
}

if (!hash_equals((string) SYNC_TOKEN, jetonFourni())) {
    erreur(401, 'Jeton absent ou invalide.');
}

// ------------------------------------------------------------------ base de données

try {
    $pdo = new PDO(
        sprintf('pgsql:host=%s;port=%d;dbname=%s', DB_HOST, (int) DB_PORT, DB_NAME),
        DB_USER,
        DB_PASSWORD,
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]
    );
} catch (Throwable $e) {
    erreur(500, 'Connexion à la base impossible : ' . $e->getMessage());
}

/**
 * La table est créée au premier appel. Si l'utilisateur de la base n'a pas le droit de
 * créer dans le schéma (fréquent depuis PostgreSQL 15 quand le schéma appartient à
 * quelqu'un d'autre), on se contente de vérifier qu'elle existe déjà.
 */
try {
    $pdo->exec(
        'CREATE TABLE IF NOT EXISTS ' . TABLE . ' (
            kind       text   NOT NULL,
            id         text   NOT NULL,
            updated_at bigint NOT NULL,
            payload    jsonb  NOT NULL,
            PRIMARY KEY (kind, id)
        )'
    );
} catch (Throwable $creation) {
    $existante = $pdo->query('SELECT to_regclass(' . $pdo->quote(TABLE) . ')')->fetchColumn();
    if ($existante === null || $existante === false) {
        erreur(500, sprintf(
            'La table %s est absente et sa création a échoué (%s). Créez-la depuis phpPgAdmin '
            . 'avec le contenu de schema.sql, ou donnez le droit CREATE sur le schéma à %s.',
            TABLE,
            $creation->getMessage(),
            DB_USER
        ));
    }
}

/** Lit tout l'état partagé. */
function etatComplet(PDO $pdo): array
{
    $etat = ['schemaVersion' => 2, 'players' => [], 'decks' => [], 'games' => []];
    $cles = ['player' => 'players', 'deck' => 'decks', 'game' => 'games'];

    $requete = $pdo->query('SELECT kind, payload FROM ' . TABLE . ' ORDER BY kind, id');
    foreach ($requete as $ligne) {
        $cle = $cles[$ligne['kind']] ?? null;
        if ($cle === null) {
            continue;
        }
        $decode = json_decode((string) $ligne['payload'], true);
        if (is_array($decode)) {
            $etat[$cle][] = $decode;
        }
    }
    return $etat;
}

// --------------------------------------------------------- consultation (GET)

if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'GET') {
    $etat = etatComplet($pdo);
    repondre(200, [
        'ok' => true,
        'players' => count($etat['players']),
        'decks' => count($etat['decks']),
        'games' => count($etat['games']),
    ]);
}

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    erreur(405, 'Méthode non autorisée.');
}

// ------------------------------------------------------- synchronisation (POST)

$brut = file_get_contents('php://input');
if ($brut === false || $brut === '') {
    erreur(400, 'Corps de requête vide.');
}
if (strlen($brut) > TAILLE_MAX_OCTETS) {
    erreur(413, 'Données trop volumineuses.');
}

$entrant = json_decode($brut, true);
if (!is_array($entrant)) {
    erreur(400, 'JSON invalide.');
}

$aEnregistrer = [];
foreach (['player' => 'players', 'deck' => 'decks', 'game' => 'games'] as $type => $cle) {
    foreach ((array) ($entrant[$cle] ?? []) as $enregistrement) {
        if (!is_array($enregistrement)) {
            continue;
        }
        $id = $enregistrement['id'] ?? null;
        if (!is_string($id) || $id === '' || strlen($id) > 128) {
            continue;
        }
        $aEnregistrer[] = [
            'kind' => $type,
            'id' => $id,
            'updated_at' => (int) ($enregistrement['updatedAt'] ?? 0),
            'payload' => json_encode($enregistrement, JSON_UNESCAPED_UNICODE),
        ];
    }
}

try {
    $pdo->beginTransaction();

    $insertion = $pdo->prepare(
        'INSERT INTO ' . TABLE . ' (kind, id, updated_at, payload)
         VALUES (:kind, :id, :updated_at, CAST(:payload AS jsonb))
         ON CONFLICT (kind, id) DO UPDATE
            SET updated_at = EXCLUDED.updated_at,
                payload    = EXCLUDED.payload
          WHERE ' . TABLE . '.updated_at < EXCLUDED.updated_at'
    );

    foreach ($aEnregistrer as $enregistrement) {
        $insertion->execute($enregistrement);
    }

    $resultat = etatComplet($pdo);
    $pdo->commit();
} catch (Throwable $e) {
    if ($pdo->inTransaction()) {
        $pdo->rollBack();
    }
    erreur(500, 'Écriture impossible : ' . $e->getMessage());
}

repondre(200, $resultat);
