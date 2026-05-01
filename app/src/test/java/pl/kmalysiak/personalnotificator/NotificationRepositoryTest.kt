package pl.kmalysiak.personalnotificator

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.kmalysiak.personalnotificator.appstate.AppStateRepository
import pl.kmalysiak.personalnotificator.model.dao.NotificationEntityDao
import pl.kmalysiak.personalnotificator.model.dao.NotificationRepository
import pl.kmalysiak.personalnotificator.model.dao.NotificationEntity
import pl.kmalysiak.personalnotificator.appstate.SecurePrefsService
import pl.kmalysiak.personalnotificator.ws.WebSocketService
import java.time.LocalDateTime

class NotificationRepositoryTest {

    private val dao = mockk<NotificationEntityDao>(relaxed = true)
    private val appStateRepo = AppStateRepository()
    private val securePrefs = mockk<SecurePrefsService>()
    private val context = mockk<Context>(relaxed = true)

    private val repo = NotificationRepository(dao, appStateRepo, securePrefs, context)

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.startForegroundService(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `save inserts entity to dao`() = runTest {
        val notification = buildNotification()

        repo.save(notification)

        coVerify { dao.insert(notification) }
    }

    @Test
    fun `save calls keepLatest500`() = runTest {
        repo.save(buildNotification())

        coVerify { dao.keepLatest500() }
    }

    @Test
    fun `save emits notificationSaved signal`() = runTest {
        var received = false
        val job = launch { appStateRepo.notificationSaved.collect { received = true } }

        repo.save(buildNotification())

        job.cancel()
        assertTrue(received)
    }

    @Test
    fun `save with non-reconnect type does not update lastReconnectRequest`() = runTest {
        val before = appStateRepo.lastReconnectRequest.value

        repo.save(buildNotification(entType = "ruch"))

        assertEquals(before, appStateRepo.lastReconnectRequest.value)
    }

    @Test
    fun `save with reconnect_request updates lastReconnectRequest`() = runTest {
        val timestamp = 1_700_000_000L
        every { securePrefs.loadIdToken() } returns "token"

        repo.save(buildNotification(entType = "reconnect_request", timestamp = timestamp))

        assertTrue(appStateRepo.lastReconnectRequest.value != LocalDateTime.MIN)
    }

    @Test
    fun `save with reconnect_request starts WebSocketService`() = runTest {
        every { securePrefs.loadIdToken() } returns "token123"
        val intentSlot = slot<Intent>()
        every { ContextCompat.startForegroundService(context, capture(intentSlot)) } just Runs

        repo.save(buildNotification(entType = "reconnect_request"))

        assertEquals(
            WebSocketService::class.java.name,
            intentSlot.captured.component?.className
        )
    }

    @Test
    fun `save with reconnect_request passes idToken in intent`() = runTest {
        every { securePrefs.loadIdToken() } returns "secret-token"
        val intentSlot = slot<Intent>()
        every { ContextCompat.startForegroundService(context, capture(intentSlot)) } just Runs

        repo.save(buildNotification(entType = "reconnect_request"))

        assertEquals("secret-token", intentSlot.captured.getStringExtra("idToken"))
    }

    private fun buildNotification(
        entType: String = "ruch",
        timestamp: Long = 1_000_000L
    ) = NotificationEntity(
        entFriendlyName = "czujnik",
        entType = entType,
        content = "wykryto ruch",
        timestamp = timestamp,
        timestampReceived = timestamp + 1
    )
}
