package fr.leonelal.mtgduel

import fr.leonelal.mtgduel.data.AppState
import fr.leonelal.mtgduel.data.Deck
import fr.leonelal.mtgduel.data.Player
import fr.leonelal.mtgduel.data.SyncClient
import fr.leonelal.mtgduel.data.SyncSettings
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Le client est confronté à un vrai serveur HTTP, pas à une imitation en mémoire. */
class SyncClientTest {

    private lateinit var serveur: MockWebServer
    private lateinit var requete: RecordedRequest

    private val client = SyncClient()

    @Before
    fun demarrerServeur() {
        serveur = MockWebServer()
        serveur.start()
    }

    @After
    fun arreterServeur() {
        serveur.shutdown()
    }

    private fun repondre(code: Int, corps: String) {
        serveur.enqueue(MockResponse().setResponseCode(code).setBody(corps))
    }

    private fun reglages() = SyncSettings(
        enabled = true,
        url = serveur.url("/sync.php").toString(),
        token = "jeton-de-test"
    )

    @Test
    fun `l etat local est envoye au format attendu par l API`() = runBlocking {
        repondre(200, """{"schemaVersion":2,"players":[],"decks":[],"games":[]}""")
        val local = AppState(
            players = listOf(Player("p1", "Moi", 1), Player("p2", "Mon copain", 1)),
            allDecks = listOf(Deck(id = "d1", name = "Atraxa", ownerId = "p1", createdAt = 5, updatedAt = 5))
        )
        client.push(reglages(), local)
        requete = serveur.takeRequest()
        val corpsRecu = requete.body.readUtf8()

        assertEquals("POST", requete.method)
        assertEquals("Bearer jeton-de-test", requete.getHeader("Authorization"))
        val envoye = Json.parseToJsonElement(corpsRecu)
        // Les noms de champs doivent correspondre exactement à ceux lus par sync.php.
        assertContains(corpsRecu, "\"schemaVersion\"")
        assertContains(corpsRecu, "\"players\"")
        assertContains(corpsRecu, "\"decks\"")
        assertContains(corpsRecu, "\"games\"")
        assertContains(corpsRecu, "\"updatedAt\"")
        assertContains(corpsRecu, "\"deleted\"")
        assertTrue(envoye.toString().contains("Atraxa"))
    }

    @Test
    fun `la reponse du serveur est relue en etat applicatif`() = runBlocking {
        repondre(200, """
            {"schemaVersion":2,
             "players":[{"id":"p1","name":"Leo","updatedAt":9}],
             "decks":[{"id":"d9","name":"Krenko","ownerId":"p2","colors":["R"],"note":"",
                       "archived":false,"createdAt":1,"updatedAt":9,"deleted":false}],
             "games":[]}
        """.trimIndent())
        val recu = client.push(reglages(), AppState())
        assertEquals("Leo", recu.playerName("p1"))
        assertEquals("Krenko", recu.allDecks.single().name)
    }

    @Test
    fun `un jeton refuse donne un message comprehensible`() {
        repondre(401, """{"error":"Jeton absent ou invalide."}""")
        val erreur = runCatching { runBlocking { client.push(reglages(), AppState()) } }.exceptionOrNull()
        assertTrue(erreur?.message?.contains("Jeton refusé") == true, "message obtenu : ${erreur?.message}")
    }

    @Test
    fun `une adresse inexistante donne un message comprehensible`() {
        repondre(404, "not found")
        val erreur = runCatching { runBlocking { client.push(reglages(), AppState()) } }.exceptionOrNull()
        assertTrue(erreur?.message?.contains("Adresse introuvable") == true, "message obtenu : ${erreur?.message}")
    }

    @Test
    fun `les champs inconnus renvoyes par une future version du serveur sont ignores`() = runBlocking {
        repondre(200, """{"schemaVersion":3,"players":[],"decks":[],"games":[],"nouveaute":true}""")
        val recu = client.push(reglages(), AppState())
        assertEquals(3, recu.schemaVersion)
    }
}
