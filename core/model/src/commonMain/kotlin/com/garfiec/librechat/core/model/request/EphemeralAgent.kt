package com.garfiec.librechat.core.model.request

import com.garfiec.librechat.core.model.config.ImageChoice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class EphemeralAgent(
    val mcp: List<String>? = null,
    @SerialName("web_search") val webSearch: Boolean? = null,
    @SerialName("file_search") val fileSearch: Boolean? = null,
    @SerialName("execute_code") val executeCode: Boolean? = null,
    val artifacts: String? = null,
    /**
     * Equip this run with the memory tools (`set_memory`/`delete_memory`). Only honored when the
     * server has the `memory` agent capability enabled and the caller holds MEMORIES
     * USE+CREATE+UPDATE — the backend drops the tools otherwise.
     */
    val memory: Boolean? = null,
    /**
     * TCHAT: equip the run with the image toolkit. `true` means the default
     * `modelSpecs.imageList` entry; a string names an entry. Only Tchat servers read it.
     */
    @SerialName("image_gen") val imageGen: JsonPrimitive? = null,
    /** TCHAT: the parameters pinned for that image model; the server drops any it doesn't offer. */
    @SerialName("image_options") val imageOptions: ImageChoice? = null,
    /**
     * Turn skills on for an ephemeral (non-agent) run. The server consults it only when the
     * agent has no `skills_enabled` of its own, which a model spec that configures `skills`
     * supplies; without it an ephemeral run gets no skill catalog and no manual skills.
     */
    val skills: Boolean? = null,
)
