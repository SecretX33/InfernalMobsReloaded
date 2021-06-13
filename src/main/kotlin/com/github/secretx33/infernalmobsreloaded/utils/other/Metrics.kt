package com.github.secretx33.infernalmobsreloaded.utils.other

import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.*
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.*
import java.util.function.*
import java.util.logging.Level
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection

/**
 * Creates a new Metrics instance.
 *
 * @param plugin Your plugin instance.
 * @param serviceId The id of the service. It can be found at [What is my plugin id?](https://bstats.org/what-is-my-plugin-id)
 */
class Metrics(private val plugin: JavaPlugin, serviceId: Int) {

    private val metricsBase: MetricsBase

    init {
        // Get the config file
        val bStatsFolder = File(plugin.dataFolder.parentFile, "bStats")
        val configFile = File(bStatsFolder, "config.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        if (!config.isSet("serverUuid")) {
            createDefaultConfig(config, configFile)
        }
        metricsBase = makeMetricsBase(config, serviceId)
    }

    private fun createDefaultConfig(config: YamlConfiguration, configFile: File) {
        config.addDefault("enabled", true)
        config.addDefault("serverUuid", UUID.randomUUID().toString())
        config.addDefault("logFailedRequests", false)
        config.addDefault("logSentData", false)
        config.addDefault("logResponseStatusText", false)
        // Inform the server owners about bStats
        config
            .options()
            .header(
            "bStats (https://bStats.org) collects some basic information for plugin authors, like how\n"
                    + "many people use their plugin and their total player count. It's recommended to keep bStats\n"
                    + "enabled, but if you're not comfortable with this, you can turn this setting off. There is no\n"
                    + "performance penalty associated with having metrics enabled, and data sent to bStats is fully\n"
                    + "anonymous.")
            .copyDefaults(true)
        runCatching { config.save(configFile) }
    }

    private fun makeMetricsBase(config: YamlConfiguration, serviceId: Int): MetricsBase {
        // Load the data
        val enabled = config.getBoolean("enabled", true)
        val serverUUID = config.getString("serverUuid") ?: UUID.randomUUID().toString()
        val logErrors = config.getBoolean("logFailedRequests", false)
        val logSentData = config.getBoolean("logSentData", false)
        val logResponseStatusText = config.getBoolean("logResponseStatusText", false)

        return MetricsBase (
            platform = "bukkit",
            serverUuid = serverUUID,
            serviceId = serviceId,
            bMetricsEnabled = enabled,
            { builder: JsonObjectBuilder -> appendPlatformData(builder) },
            { builder: JsonObjectBuilder -> appendServiceData(builder) },
            { plugin.isEnabled },
            { message: String?, error: Throwable? -> plugin.logger.log(Level.WARNING, message, error) },
            { message: String -> plugin.logger.log(Level.INFO, message) },
            logErrors = logErrors,
            logSentData = logSentData,
            logResponseStatusText = logResponseStatusText,
        )
    }

    /**
     * Adds a custom chart.
     *
     * @param chart The chart to add.
     */
    fun addCustomChart(chart: CustomChart) = metricsBase.addCustomChart(chart)

    private fun appendPlatformData(builder: JsonObjectBuilder) {
        builder.appendField("playerAmount", playerAmount)
        builder.appendField("onlineMode", if(Bukkit.getOnlineMode()) 1 else 0)
        builder.appendField("bukkitVersion", Bukkit.getVersion())
        builder.appendField("bukkitName", Bukkit.getName())
        builder.appendField("javaVersion", System.getProperty("java.version"))
        builder.appendField("osName", System.getProperty("os.name"))
        builder.appendField("osArch", System.getProperty("os.arch"))
        builder.appendField("osVersion", System.getProperty("os.version"))
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors())
    }

    private fun appendServiceData(builder: JsonObjectBuilder) {
        builder.appendField("pluginVersion", plugin.description.version)
    }

    private val playerAmount: Int
        get() = try {
                // Around MC 1.8 the return type was changed from an array to a collection,
                // This fixes java.lang.NoSuchMethodError:
                // org.bukkit.Bukkit.getOnlinePlayers()Ljava/util/Collection;
                val onlinePlayersMethod = Class.forName("org.bukkit.Server").getMethod("getOnlinePlayers")
                if (onlinePlayersMethod.returnType == MutableCollection::class.java) (onlinePlayersMethod.invoke(Bukkit.getServer()) as Collection<*>).size
                else (onlinePlayersMethod.invoke(Bukkit.getServer()) as Array<*>).size
            } catch (e: Exception) {
                // Just use the new method if the reflection failed
                Bukkit.getOnlinePlayers().size
            }

    /**
     * Creates a new MetricsBase class instance.
     *
     * @param platform The platform of the service.
     * @param serviceId The id of the service.
     * @param serverUuid The server uuid.
     * @param bMetricsEnabled Whether or not data sending is enabled.
     * @param appendPlatformDataConsumer A consumer that receives a `JsonObjectBuilder` and
     * appends all platform-specific data.
     * @param appendServiceDataConsumer A consumer that receives a `JsonObjectBuilder` and
     * appends all service-specific data.
     * @param pluginEnabled A supplier to check if the service is still enabled.
     * @param errorLogger A consumer that accepts log message and an error.
     * @param infoLogger A consumer that accepts info log messages.
     * @param logErrors Whether or not errors should be logged.
     * @param logSentData Whether or not the sent data should be logged.
     * @param logResponseStatusText Whether or not the response status text should be logged.
     */
    class MetricsBase (
        private val platform: String,
        private val serverUuid: String,
        private val serviceId: Int,
        private val bMetricsEnabled: Boolean,
        private val appendPlatformDataConsumer: Consumer<JsonObjectBuilder>,
        private val appendServiceDataConsumer: Consumer<JsonObjectBuilder>,
        private val pluginEnabled: Supplier<Boolean>,
        private val errorLogger: BiConsumer<String?, Throwable?>,
        private val infoLogger: Consumer<String>,
        private val logErrors: Boolean,
        private val logSentData: Boolean,
        private val logResponseStatusText: Boolean
    ) {
        private val customCharts: MutableSet<CustomChart> = HashSet()

        init {
            checkRelocation()
            if(bMetricsEnabled) {
                startSubmitting()
            }
        }

        fun addCustomChart(chart: CustomChart) {
            customCharts.add(chart)
        }

        private fun startSubmitting() {
            // Submitting data or service is disabled
            if (!bMetricsEnabled || !pluginEnabled.get()) return

            // Many servers tend to restart at a fixed time at xx:00 which causes an uneven distribution
            // of requests on the
            // bStats backend. To circumvent this problem, we introduce some randomness into the initial
            // and second delay.
            // WARNING: You must not modify and part of this Metrics class, including the submit delay or
            // frequency!
            // WARNING: Modifying this code will get your plugin banned on bStats. Just don't do it!
            val initialDelay = (ONE_MINUTE * (3 + Math.random() * 3)).toLong() // 3 - 6 minutes after the server started
            val secondDelay = (ONE_MINUTE * (Math.random() * 30)).toLong()     // 1 - 30 minutes after the initial delay

            CoroutineScope(Dispatchers.Default).launch {
                delay(initialDelay)
                submitData()
                delay(secondDelay)

                while(isActive) {
                    submitData()
                    delay(THIRTY_MINUTES)
                }
            }
        }

        private suspend fun submitData() {
            val baseJsonBuilder = JsonObjectBuilder()
            appendPlatformDataConsumer.accept(baseJsonBuilder)
            val serviceJsonBuilder = JsonObjectBuilder()
            appendServiceDataConsumer.accept(serviceJsonBuilder)

            val chartData: Array<JsonObjectBuilder.JsonObject> = customCharts
                .mapNotNull { customChart: CustomChart -> customChart.getRequestJsonObject(errorLogger, logErrors) }
                .toTypedArray()
            serviceJsonBuilder.apply {
                appendField("id", serviceId)
                appendField("customCharts", chartData)
            }
            baseJsonBuilder.apply {
                appendField("service", serviceJsonBuilder.build())
                appendField("serverUUID", serverUuid)
                appendField("metricsVersion", METRICS_VERSION)
            }

            val data = baseJsonBuilder.build()

            // Send the data
            withContext(Dispatchers.IO) {
                runCatching {
                    sendData(data)
                }.exceptionOrNull()
                    ?.takeIf { logErrors }
                    ?.let { errorLogger.accept("Could not submit bStats metrics data", it) }
            }
        }

        private fun sendData(data: JsonObjectBuilder.JsonObject) {
            if(logSentData) {
                infoLogger.accept("Sent bStats metrics data: $data")
            }
            val url = String.format(REPORT_URL, platform)
            val connection = URL(url).openConnection() as HttpsURLConnection
            // Compress the data to save bandwidth
            val compressedData = compress(data.toString())

            connection.apply {
                requestMethod = "POST"
                addRequestProperty("Accept", "application/json")
                addRequestProperty("Connection", "close")
                addRequestProperty("Content-Encoding", "gzip")
                addRequestProperty("Content-Length", compressedData.size.toString())
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Metrics-Service/1")
                doOutput = true
            }

            DataOutputStream(connection.outputStream).use { outputStream -> outputStream.write(compressedData) }
            val builder = StringBuilder()
            BufferedReader(InputStreamReader(connection.inputStream)).use { bufferedReader ->
                var line: String?
                while ((bufferedReader.readLine().also { line = it }) != null) {
                    builder.append(line)
                }
            }
            if (logResponseStatusText) {
                infoLogger.accept("Sent data to bStats and received response: $builder")
            }
        }

        /** Checks that the class was properly relocated.  */
        private fun checkRelocation() {
            // You can use the property to disable the check in your test environment
            if (System.getProperty("bstats.relocatecheck") == null
                || System.getProperty("bstats.relocatecheck") != "false") {
                // Maven's Relocate is clever and changes strings, too. So we have to use this little
                // "trick" ... :D
                val defaultPackage = listOf("com", "bstats").joinToString(".")
                // We want to make sure no one just copy & pastes the example and uses the wrong package
                // names
                if (MetricsBase::class.java.getPackage().name.startsWith(defaultPackage)) {
                    throw IllegalStateException("bStats Metrics class has not been relocated correctly!")
                }
            }
        }

        /**
         * Gzips the given string.
         *
         * @param str The string to gzip.
         * @return The gzipped string.
         */
        private fun compress(str: String): ByteArray {
            val outputStream = ByteArrayOutputStream()
            GZIPOutputStream(outputStream).use { gzip -> gzip.write(str.toByteArray(StandardCharsets.UTF_8)) }
            return outputStream.toByteArray()
        }

        companion object {
            /** The version of the Metrics class.  */
            const val METRICS_VERSION = "2.2.1"
            private const val REPORT_URL = "https://bStats.org/api/v2/data/%s"
            private const val ONE_MINUTE = 1000L * 60L
            private const val THIRTY_MINUTES = ONE_MINUTE * 30L
        }
    }

    /**
     * Advanced bar chart.
     *
     * @param chartId The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    class AdvancedBarChart (chartId: String, private val callable: Callable<Map<String, IntArray>>) : CustomChart(chartId) {

        override val chartData: JsonObjectBuilder.JsonObject?
            get() {
                val valuesBuilder = JsonObjectBuilder()
                // Null = skip the chart
                val map = callable.call()?.takeUnless { it.isEmpty() } ?: return null

                var allSkipped = true
                for (entry: Map.Entry<String, IntArray> in map.entries) {
                    if (entry.value.isEmpty()) {
                        // Skip this invalid
                        continue
                    }
                    allSkipped = false
                    valuesBuilder.appendField(entry.key, entry.value)
                }
                if(allSkipped) return null // Null = skip the chart
                return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
            }

    }

    /**
     * SimpleBarChart class.
     *
     * @param chartId The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    class SimpleBarChart(chartId: String, private val callable: Callable<Map<String, Int>>) : CustomChart(chartId) {
        override val chartData: JsonObjectBuilder.JsonObject?
            get() {
                val valuesBuilder = JsonObjectBuilder()
                // Null = skip the chart
                val map = callable.call()?.takeUnless { it.isEmpty() } ?: return null
                for (entry: Map.Entry<String, Int> in map.entries) {
                    valuesBuilder.appendField(entry.key, intArrayOf(entry.value))
                }
                return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
            }

    }

    /**
     * MultiLineChart class.
     *
     * @param chartId The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    open class MultiLineChart(chartId: String, private val callable: Callable<Map<String, Int>>) : CustomChart(chartId) {
        override val chartData: JsonObjectBuilder.JsonObject?
            get() {
                val valuesBuilder = JsonObjectBuilder()
                // Null = skip the chart
                val map = callable.call()?.takeUnless { it.isEmpty() } ?: return null

                var allSkipped = true
                for (entry: Map.Entry<String, Int> in map.entries) {
                    if (entry.value == 0) {
                        // Skip this invalid
                        continue
                    }
                    allSkipped = false
                    valuesBuilder.appendField(entry.key, entry.value)
                }
                if(allSkipped) return null // Null = skip the chart
                return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
            }

    }

    /**
     * AdvancedPie class.
     *
     * @param chartId The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    class AdvancedPie(chartId: String, private val callable: Callable<Map<String, Int>>) : CustomChart(chartId) {

        // Null = skip the chart
        override val chartData: JsonObjectBuilder.JsonObject?
            get() {
                val valuesBuilder = JsonObjectBuilder()
                // Null = skip the chart
                val map = callable.call()?.takeUnless { it.isEmpty() } ?: return null

                var allSkipped = true
                for (entry: Map.Entry<String, Int> in map.entries) {
                    if (entry.value == 0) {
                        // Skip this invalid
                        continue
                    }
                    allSkipped = false
                    valuesBuilder.appendField(entry.key, entry.value)
                }
                if(allSkipped) return null // Null = skip the chart
                return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
            }

    }

    abstract class CustomChart protected constructor(private val chartId: String) {

        fun getRequestJsonObject(errorLogger: BiConsumer<String?, Throwable?>, logErrors: Boolean): JsonObjectBuilder.JsonObject? {
            val builder = JsonObjectBuilder()
            builder.appendField("chartId", chartId)
            try {
                // If the data is null we don't send the chart.
                val data = chartData ?: return null
                builder.appendField("data", data)
            } catch (t: Throwable) {
                if (logErrors) {
                    errorLogger.accept("Failed to get data for custom chart with id $chartId", t)
                }
                return null
            }
            return builder.build()
        }

        protected abstract val chartData: JsonObjectBuilder.JsonObject?
    }


    /**
     * SingleLineChart class.
     *
     * @param chartId The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    class SingleLineChart(chartId: String, private val callable: Callable<Int>) : CustomChart(chartId) {
        override val chartData: JsonObjectBuilder.JsonObject?
            get() {
                val value = callable.call()
                if(value == 0) return null // Null = skip the chart
                return JsonObjectBuilder().appendField("value", value).build()
            }

    }

    /**
     * SimplePie class.
     *
     * @param chartId The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    class SimplePie(chartId: String, private val callable: Callable<String>) : CustomChart(chartId) {
        override val chartData: JsonObjectBuilder.JsonObject?
            get() {
                val value = callable.call()
                return if (value == null || value.isEmpty()) {
                    // Null = skip the chart
                    null
                } else JsonObjectBuilder().appendField("value", value).build()
            }
    }

    /**
     * DrilldownPie class.
     *
     * @param chartId The id of the chart.
     * @param callable The callable which is used to request the chart data.
     */
    class DrilldownPie(chartId: String, private val callable: Callable<Map<String, Map<String, Int>>>) : CustomChart(chartId) {
        override val chartData: JsonObjectBuilder.JsonObject?
            get() {
                val valuesBuilder = JsonObjectBuilder()
                // Null = skip the chart
                val map = callable.call()?.takeUnless { it.isEmpty() } ?: return null

                var reallyAllSkipped = true
                for (entryValues: Map.Entry<String, Map<String, Int>> in map.entries) {
                    val valueBuilder = JsonObjectBuilder()
                    var allSkipped = true
                    for (valueEntry: Map.Entry<String, Int> in map[entryValues.key]!!.entries) {
                        valueBuilder.appendField(valueEntry.key, valueEntry.value)
                        allSkipped = false
                    }
                    if (!allSkipped) {
                        reallyAllSkipped = false
                        valuesBuilder.appendField(entryValues.key, valueBuilder.build())
                    }
                }
                // Null = skip the chart
                if(reallyAllSkipped) return null
                return JsonObjectBuilder().appendField("values", valuesBuilder.build()).build()
            }

    }

    /**
     * An extremely simple JSON builder.
     *
     * While this class is neither feature-rich nor the most performant one, it's sufficient enough
     * for its use-case.
     */
    class JsonObjectBuilder {
        private var builder: StringBuilder? = StringBuilder().append("{")
        private var hasAtLeastOneField = false

        /**
         * Appends a null field to the JSON.
         *
         * @param key The key of the field.
         * @return A reference to this object.
         */
        fun appendNull(key: String): JsonObjectBuilder {
            appendFieldUnescaped(key, "null")
            return this
        }

        /**
         * Appends a string field to the JSON.
         *
         * @param key The key of the field.
         * @param value The value of the field.
         * @return A reference to this object.
         */
        fun appendField(key: String, value: String): JsonObjectBuilder {
            appendFieldUnescaped(key, "\"" + value.escape() + "\"")
            return this
        }

        /**
         * Appends an integer field to the JSON.
         *
         * @param key The key of the field.
         * @param value The value of the field.
         * @return A reference to this object.
         */
        fun appendField(key: String, value: Int): JsonObjectBuilder {
            appendFieldUnescaped(key, value.toString())
            return this
        }

        /**
         * Appends an object to the JSON.
         *
         * @param key The key of the field.
         * @param `object` The object.
         * @return A reference to this object.
         */
        fun appendField(key: String, obj: JsonObject): JsonObjectBuilder {
            appendFieldUnescaped(key, obj.toString())
            return this
        }

        /**
         * Appends a string array to the JSON.
         *
         * @param key The key of the field.
         * @param values The string array.
         * @return A reference to this object.
         */
        fun appendField(key: String, values: Array<String>): JsonObjectBuilder {
            val escapedValues = values.joinToString(",") { "\"${key.escape()}\"" }
            appendFieldUnescaped(key, "[$escapedValues]")
            return this
        }

        /**
         * Appends an integer array to the JSON.
         *
         * @param key The key of the field.
         * @param values The integer array.
         * @return A reference to this object.
         */
        fun appendField(key: String, values: IntArray): JsonObjectBuilder {
            val escapedValues = values.joinToString(",") { it.toString() }
            appendFieldUnescaped(key, "[$escapedValues]")
            return this
        }

        /**
         * Appends an object array to the JSON.
         *
         * @param key The key of the field.
         * @param values The integer array.
         * @return A reference to this object.
         */
        fun appendField(key: String, values: Array<JsonObject>): JsonObjectBuilder {
            val escapedValues = values.joinToString(",") { it.toString() }
            appendFieldUnescaped(key, "[$escapedValues]")
            return this
        }

        /**
         * Appends a field to the object.
         *
         * @param key The key of the field.
         * @param escapedValue The escaped value of the field.
         */
        private fun appendFieldUnescaped(key: String, escapedValue: String) {
            if(builder == null) {
                throw IllegalStateException("JSON has already been built")
            }
            if(hasAtLeastOneField) {
                builder?.append(",")
            }
            builder?.append("\"")?.append(key.escape())?.append("\":")?.append(escapedValue)
            hasAtLeastOneField = true
        }

        /**
         * Builds the JSON string and invalidates this builder.
         *
         * @return The built JSON string.
         */
        fun build(): JsonObject {
            if (builder == null) {
                throw IllegalStateException("JSON has already been built")
            }
            val obj = JsonObject(builder!!.append("}").toString())
            builder = null
            return obj
        }

        /**
         * A super simple representation of a JSON object.
         *
         *
         * This class only exists to make methods of the [JsonObjectBuilder] type-safe and not
         * allow a raw string inputs for methods like [JsonObjectBuilder.appendField].
         */
        class JsonObject(private val value: String) {
            override fun toString(): String {
                return value
            }
        }

        companion object {
            /**
             * Escapes the given string like stated in https://www.ietf.org/rfc/rfc4627.txt.
             *
             *
             * This method escapes only the necessary characters '"', '\'. and '\u0000' - '\u001F'.
             * Compact escapes are not used (e.g., '\n' is escaped as "\u000a" and not as "\n").
             *
             * @param value The value to escape.
             * @return The escaped value.
             */
            private fun String.escape(): String {
                val builder = StringBuilder()
                for (char in this) {
                    when(char) {
                        '"' -> builder.append("\\\"")
                        '\\' -> builder.append("\\\\")
                        '\u000F' -> builder.append("\\u000").append(Integer.toHexString(char.code))
                        '\u001F' -> builder.append("\\u00").append(Integer.toHexString(char.code))
                        else -> builder.append(char)
                    }
                }
                return builder.toString()
            }
        }
    }
}
