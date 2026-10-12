package com.garfiec.librechat.core.model.config

import kotlinx.serialization.Serializable

/**
 * TCHAT: the image parameters an [ImageSpec] honours (`imageList[].options` on a Tchat server),
 * measured through the gateway rather than taken from provider docs, because a relay can accept a
 * field and ignore it. Null lists mean the model honours none of that kind.
 */
@Serializable
data class ImageOptions(
    /** Image API `size` values, `WIDTHxHEIGHT`. OpenAI-protocol models only. */
    val sizes: List<String>? = null,
    /** Gemini `imageConfig.aspectRatio` values such as `16:9`. */
    val aspectRatios: List<String>? = null,
    /** Gemini `imageConfig.imageSize` values such as `2K`. */
    val imageSizes: List<String>? = null,
) {
    /** Whether there is anything to offer; a spec with nothing gets no options panel. */
    val isEmpty: Boolean get() = sizes.isNullOrEmpty() && aspectRatios.isNullOrEmpty() && imageSizes.isNullOrEmpty()

    fun valuesFor(field: ImageChoiceField): List<String> = when (field) {
        ImageChoiceField.SIZE -> sizes
        ImageChoiceField.ASPECT_RATIO -> aspectRatios
        ImageChoiceField.IMAGE_SIZE -> imageSizes
    }.orEmpty()
}

/** One parameter a person can pin, in the order the options panel shows them. */
enum class ImageChoiceField { SIZE, ASPECT_RATIO, IMAGE_SIZE }

/**
 * What the person pinned for one image model, sent as `ephemeralAgent.image_options`. A pinned
 * field overrides the chat model's tool argument; an unset one leaves the choice to the chat model.
 */
@Serializable
data class ImageChoice(
    val size: String? = null,
    val aspectRatio: String? = null,
    val imageSize: String? = null,
) {
    val isEmpty: Boolean get() = size == null && aspectRatio == null && imageSize == null

    operator fun get(field: ImageChoiceField): String? = when (field) {
        ImageChoiceField.SIZE -> size
        ImageChoiceField.ASPECT_RATIO -> aspectRatio
        ImageChoiceField.IMAGE_SIZE -> imageSize
    }

    fun with(field: ImageChoiceField, value: String?): ImageChoice = when (field) {
        ImageChoiceField.SIZE -> copy(size = value)
        ImageChoiceField.ASPECT_RATIO -> copy(aspectRatio = value)
        ImageChoiceField.IMAGE_SIZE -> copy(imageSize = value)
    }

    /**
     * Only the values [options] still offers. A stored choice can outlive a config change, and the
     * server would drop the rest anyway; null when nothing survives.
     */
    fun sanitizedFor(options: ImageOptions?): ImageChoice? {
        if (options == null) return null
        val kept = ImageChoiceField.entries.fold(ImageChoice()) { acc, field ->
            val value = this[field]
            if (value != null && value in options.valuesFor(field)) acc.with(field, value) else acc
        }
        return kept.takeUnless { it.isEmpty }
    }
}

/** `1536x1024` → `3:2 · 1536×1024`, so a size reads as the shape it makes. */
fun imageSizeLabel(size: String): String {
    val match = Regex("""^(\d+)x(\d+)$""").matchEntire(size) ?: return size
    val width = match.groupValues[1].toInt()
    val height = match.groupValues[2].toInt()
    tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    val divisor = gcd(width, height)
    return "${width / divisor}:${height / divisor} · $width×$height"
}
