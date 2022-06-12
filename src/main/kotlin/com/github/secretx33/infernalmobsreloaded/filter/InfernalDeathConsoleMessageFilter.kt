package com.github.secretx33.infernalmobsreloaded.filter

import com.github.secretx33.infernalmobsreloaded.config.Config
import com.github.secretx33.infernalmobsreloaded.config.ConfigKeys
import com.github.secretx33.infernalmobsreloaded.manager.InfernalMobsManager
import com.github.secretx33.infernalmobsreloaded.utils.other.CustomKoinComponent
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.Marker
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.filter.AbstractFilter
import org.apache.logging.log4j.message.Message
import toothpick.InjectConstructor
import javax.inject.Singleton

@Singleton
@InjectConstructor
class InfernalDeathConsoleMessageFilter(
    private val config: Config,
    private val mobsManager: InfernalMobsManager,
) : AbstractFilter(Filter.Result.DENY, Filter.Result.NEUTRAL), CustomKoinComponent {
    private val useRawMessage = false

    private fun filter(msg: String?): Filter.Result {
        if (msg == null || !isFilterEnabled) return onMismatch
        val mobName = pattern.find(msg)?.groupValues?.get(1) ?: return onMismatch
        if (mobsManager.isInfernalMobDisplayName(mobName)) return onMatch
        return onMismatch
    }

    override fun filter(
        logger: Logger?,
        level: Level?,
        marker: Marker?,
        msg: String?,
        vararg params: Any?
    ): Filter.Result = filter(msg)

    override fun filter(
        logger: Logger?,
        level: Level?,
        marker: Marker?,
        msg: Any?,
        t: Throwable?
    ): Filter.Result = filter(msg?.toString())

    override fun filter(
        logger: Logger,
        level: Level?,
        marker: Marker?,
        msg: Message?,
        t: Throwable?
    ): Filter.Result {
        val text = if (useRawMessage) msg?.format else msg?.formattedMessage
        return filter(text)
    }

    override fun filter(event: LogEvent): Filter.Result {
        val text = if (useRawMessage) event.message?.format else event.message?.formattedMessage
        return filter(text)
    }

    private val isFilterEnabled: Boolean get() = config.get(ConfigKeys.PREVENT_NAMED_ENTITY_MESSAGES)

    override fun toString(): String =
        "InfernalDeathConsoleMessageFilter(mobsManager=$mobsManager, useRawMessage=$useRawMessage)"

    private companion object {
        val pattern = """^Named entity \w+\['([^']+)'.*'""".toRegex()
    }
}
