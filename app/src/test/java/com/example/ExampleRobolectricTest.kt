package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppLanguage
import com.example.model.CameraMode
import com.example.model.CameraSettings
import com.example.processing.ImageEnhancer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ZMRAWE CAM 100x", appName)
    }

    @Test
    fun `image enhancer offline pipeline executes correctly`() = runBlocking {
        // Create 64x64 synthetic test bitmap with contrasting edges
        val testBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 0 until 64) {
            for (x in 0 until 64) {
                testBitmap.setPixel(x, y, if (x < 32) Color.BLACK else Color.WHITE)
            }
        }

        val enhanced = ImageEnhancer.enhanceImage(
            source = testBitmap,
            zoomFactor = 100.0f,
            isNightMode = false,
            enableDenoise = true,
            enableSharpen = true,
            enableSuperRes = true,
            enableHdr = true,
            ultraQuality = false
        )

        assertNotNull(enhanced)
        assertEquals(64, enhanced.width)
        assertEquals(64, enhanced.height)
    }

    @Test
    fun `camera settings default configuration is offline optimized`() {
        val settings = CameraSettings()
        assertTrue(settings.aiClarityBoostEnabled)
        assertTrue(settings.superResolutionEnabled)
        assertTrue(settings.noiseReductionEnabled)
        assertTrue(settings.smartSharpeningEnabled)
        assertEquals(AppLanguage.SYSTEM, settings.language)
    }
}
