package com.garfiec.librechat.core.model.config

import com.garfiec.librechat.core.model.request.EphemeralAgent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImageOptionsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun specOptionsDecodeFromTheServersImageList() {
        val spec = json.decodeFromString(
            ImageSpec.serializer(),
            """{"name":"hd","label":"HD","model":"gpt-image-2-h","options":{"sizes":["1024x1024","3840x2160"]}}""",
        )
        assertEquals(listOf("1024x1024", "3840x2160"), spec.options?.sizes)
        assertFalse(spec.options!!.isEmpty)
        assertTrue(json.decodeFromString(ImageSpec.serializer(), """{"name":"a","label":"A","model":"m","options":{}}""").options!!.isEmpty)
        assertNull(json.decodeFromString(ImageSpec.serializer(), """{"name":"a","label":"A","model":"m"}""").options)
    }

    @Test
    fun aChoiceKeepsOnlyWhatTheModelOffers() {
        val options = ImageOptions(aspectRatios = listOf("1:1", "16:9"), imageSizes = listOf("1K"))
        assertEquals(
            ImageChoice(aspectRatio = "16:9"),
            ImageChoice(size = "1024x1024", aspectRatio = "16:9", imageSize = "4K").sanitizedFor(options),
        )
        assertNull(ImageChoice(imageSize = "4K").sanitizedFor(options))
        assertNull(ImageChoice(aspectRatio = "16:9").sanitizedFor(null))
    }

    @Test
    fun theRequestUsesTheServersFieldNames() {
        val encoded = Json.encodeToJsonElement(
            EphemeralAgent.serializer(),
            EphemeralAgent(imageOptions = ImageChoice(aspectRatio = "16:9", imageSize = "2K")),
        ).jsonObject
        assertEquals("""{"aspectRatio":"16:9","imageSize":"2K"}""", encoded["image_options"].toString())
    }

    @Test
    fun sizesReadAsTheShapeTheyMake() {
        assertEquals("3:2 · 1536×1024", imageSizeLabel("1536x1024"))
        assertEquals("16:9 · 3840×2160", imageSizeLabel("3840x2160"))
        assertEquals("auto", imageSizeLabel("auto"))
    }
}
