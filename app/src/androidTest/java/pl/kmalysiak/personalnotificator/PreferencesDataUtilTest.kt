package pl.kmalysiak.personalnotificator

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.kmalysiak.personalnotificator.appstate.PreferencesDataUtil

@RunWith(AndroidJUnit4::class)
class PreferencesDataUtilTest {

    private lateinit var context: Context
    private lateinit var util: PreferencesDataUtil

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        util = PreferencesDataUtil(context)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    // updatePreferences — logika kluczy

    @Test
    fun updatePreferences_addsNewKeysWithDefaultTrue() {
        util.updatePreferences(listOf("ruch", "wejście"))
        assertTrue(util.getByName("ruch"))
        assertTrue(util.getByName("wejście"))
    }

    @Test
    fun updatePreferences_removesKeysMissingFromNewList() {
        util.updatePreferences(listOf("ruch", "wejście", "alarm"))
        util.updatePreferences(listOf("ruch"))
        val keys = util.getForDisplay().map { it.name }
        assertEquals(listOf("ruch"), keys)
    }

    @Test
    fun updatePreferences_doesNotOverwriteExistingValue() {
        util.updatePreferences(listOf("ruch"))
        util.setPreference("ruch", false)
        util.updatePreferences(listOf("ruch", "wejście"))
        assertFalse("istniejący klucz powinien zachować wartość false", util.getByName("ruch"))
        assertTrue("nowy klucz powinien dostać domyślne true", util.getByName("wejście"))
    }

    @Test
    fun updatePreferences_emptyList_removesAllKeys() {
        util.updatePreferences(listOf("ruch", "wejście"))
        util.updatePreferences(emptyList())
        assertTrue(util.getForDisplay().isEmpty())
    }

    @Test
    fun updatePreferences_defaultFalse_addsKeysWithFalse() {
        util.updatePreferences(listOf("ruch"), defaultValue = false)
        assertFalse(util.getByName("ruch"))
    }

    @Test
    fun updatePreferences_calledTwiceWithSameList_isIdempotent() {
        util.updatePreferences(listOf("ruch", "wejście"))
        util.setPreference("ruch", false)
        util.updatePreferences(listOf("ruch", "wejście"))
        assertFalse("wartość nie powinna się zmienić przy ponownym wywołaniu", util.getByName("ruch"))
        assertTrue(util.getByName("wejście"))
    }

    // TTS prefs

    @Test
    fun loadTtsReady_returnsNullWhenNotSet() {
        assertNull(util.loadTtsReady())
    }

    @Test
    fun saveTtsReady_true_roundTrip() {
        util.saveTtsReady(true)
        assertEquals(true, util.loadTtsReady())
    }

    @Test
    fun saveTtsReady_false_roundTrip() {
        util.saveTtsReady(false)
        assertEquals(false, util.loadTtsReady())
    }

    @Test
    fun saveTtsReady_overwritesPreviousValue() {
        util.saveTtsReady(true)
        util.saveTtsReady(false)
        assertEquals(false, util.loadTtsReady())
    }

    // TTS volume

    @Test
    fun loadTtsVolume_defaultIs1f() {
        assertEquals(1.0f, util.loadTtsVolume())
    }

    @Test
    fun saveTtsVolume_roundTrip() {
        util.saveTtsVolume(0.42f)
        assertEquals(0.42f, util.loadTtsVolume())
    }

    @Test
    fun saveTtsVolume_boundary0f() {
        util.saveTtsVolume(0f)
        assertEquals(0f, util.loadTtsVolume())
    }
}
