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
