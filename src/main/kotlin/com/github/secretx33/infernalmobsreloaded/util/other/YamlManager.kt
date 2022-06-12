package com.github.secretx33.infernalmobsreloaded.util.other

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.max

class YamlManager (
    private val plugin: Plugin,
    path: String,
) : YamlConfiguration() {

    val fileName: String = path.replace('\\', '/').split('/').last().appendIfMissing(".yml")
    private val relativePath: String = path.replace('\\', '/').appendIfMissing(".yml")
    private val file: File = File(plugin.dataFolder.absolutePath, relativePath)

    init { reload() }

    fun reload() {
        if (!plugin.dataFolder.exists()) plugin.dataFolder.mkdir()

        try {
            file.createIfMissing()
            load(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun File.createIfMissing() {
        if (exists()) return

        createParentDirs()
        createNewFile()
        val internalFile: InputStream = plugin.getResource(fileName)
            ?: plugin.getResource(relativePath)
            ?: plugin.javaClass.classLoader.getResourceAsStream(fileName)
            ?: plugin.javaClass.classLoader.getResourceAsStream(relativePath)
            ?: throw IllegalArgumentException("resource $fileName was not found")

        writeBytes(internalFile.readBytes())
    }

    fun save() = CoroutineScope(Dispatchers.IO).launch {
        try {
            file.createIfMissing()
            val oldFile = file.getLines()
            val comments = parseFileComments(oldFile)
            // commit all changes made to the file, erasing the comments in the process
            save(file)
            // re-add comments to the file
            val newFile = addCommentsToFile(comments)
            // and write the file on the disk
            file.writeLines(newFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFullKeyOfLine(index: Int, lines: List<String>): String {
        if (index < 0 || index >= lines.size) return ""

        val line = lines[index]
        val key = StringBuilder()
        val depth = lineDepth(line)
        var keyIsFromList = false

        key.append(
            KEY_PATTERN.matchOrNull(line, 1)
            ?: LIST_PATTERN.matchOrNull(line, 1)?.replace("-", "")?.trim().also { keyIsFromList = true }
            ?: return "")

        if (depth <= 0 || index == 0) return key.toString()

        for(i in (index - 1) downTo 0) {
            if (lines[i].isBlank() || COMMENT_PATTERN.matcher(lines[i]).matches()) continue
            val subDepth = lineDepth(lines[i])
            // if subKey has less depth than the original key, it means that this key is its parent
            // the list part is here because yaml is cancer and align the list entries with its parent key
            if (subDepth < depth || keyIsFromList && depth == subDepth && KEY_PATTERN.matches(lines[i])) {
                key.insert(0, '.')
                return key.insert(0, getFullKeyOfLine(i, lines)).toString()
            }
        }
        return key.toString()
    }

    private fun parseFileComments(fileLines: List<String>): List<Comment> {
        var lastStoredIndex = -1
        val comments = ArrayList<Comment>(fileLines.size)

        for(index in fileLines.indices) {
            if (lastStoredIndex >= index) continue
            val line = fileLines[index]
            var commentMatcher = COMMENT_PATTERN.matcher(line)

            // if entire line is comment
            if (line.isBlank() || commentMatcher.matches()) {
                val commentArray = ArrayList<String>()
                var currentLine = line
                lastStoredIndex = index - 1

                while(currentLine.isBlank() || commentMatcher.matches()) {
                    lastStoredIndex++
                    commentArray.add(currentLine)
                    // breaks if we are on the last line already
                    if (fileLines.lastIndex < (lastStoredIndex + 1)) break
                    // prepare the check on the next line
                    currentLine = fileLines[lastStoredIndex + 1]
                    commentMatcher = COMMENT_PATTERN.matcher(currentLine)
                }

                val commentType = if (commentArray.size > 1) CommentType.FULL_MULTILINE else CommentType.FULL_LINE

                comments.add(
                    Comment(index = index,
                    type = commentType,
                    lineAbove = getFullKeyOfLine(index - 1, fileLines),
                    lineBelow = getFullKeyOfLine(lastStoredIndex + 1, fileLines),
                    content = commentArray)
                )
                continue
            }

            // do nothing if there is no comment on this line
            if (!commentMatcher.find()) continue

            // if it's a dangling comment on a key or entry of a list
            val keyOrEntryMatcher = KEY_PATTERN.matchEntire(line) ?: LIST_PATTERN.matchEntire(line)
            keyOrEntryMatcher?.apply {
                comments.add(
                    Comment(index = index,
                    type = CommentType.DANGLING,
                    lineAbove = getFullKeyOfLine(index - 1, fileLines),
                    lineBelow = getFullKeyOfLine(index + 1, fileLines),
                    key = groupValues[1],
                    path = getFullKeyOfLine(index, fileLines),
                    content = listOf(commentMatcher.group()))
                )
            }
        }
        return comments
    }

    private fun addCommentsToFile(comments: List<Comment>): List<String> {
        // read the new file, removing all comments (from header)
        val newFile = file.getLines().filter { !COMMENT_PATTERN.matcher(it).matches() } as MutableList<String>

        val fullLineComments = comments.filter { it.type == CommentType.FULL_MULTILINE || it.type == CommentType.FULL_LINE }
        for(comment in fullLineComments) {
            var placed = false
            while(newFile.size < comment.index) newFile.add("")

            if (comment.lineBelow.isNotBlank()) {
                for(index in 0 until newFile.size) {
                    if (getFullKeyOfLine(index, newFile) != comment.lineBelow) continue
                    newFile.addAll(max(0,  index), comment.content)
                    placed = true
                    break
                }
            }
            if (placed) continue

            if (comment.lineAbove.isNotBlank()) {
                for(index in 0 until newFile.size) {
                    if (getFullKeyOfLine(index, newFile) != comment.lineAbove) continue
                    newFile.addAll(max(0, index + 1), comment.content)
                    placed = true
                    break
                }
            }
            if (placed) continue

            newFile.addAll(comment.index, comment.content)
        }

        val danglingComments = comments.filter { it.type == CommentType.DANGLING }
        danglingComments.forEach { comment ->
            val key = comment.key
            val path = comment.path

            for(lineIndex in 0 until newFile.size) {
                val line = newFile[lineIndex]
                val element = KEY_PATTERN.matchEntire(line)?.groupValues?.getOrNull(1) ?: LIST_PATTERN.matchEntire(line)?.groupValues?.getOrNull(1)
                if (element == key && getFullKeyOfLine(lineIndex, newFile) == path) {
                    newFile[lineIndex] += comment.content[0]
                }
            }
        }
        return newFile
    }

    override fun load(file: File) {
        try {
            FileInputStream(file).use { fis ->
                super.load(InputStreamReader(fis, CHARSET))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun save(file: File) {
        try {
            file.createParentDirs()
            file.writeText(saveToString(), CHARSET)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun lineDepth(line: String): Int = DEPTH_PATTERN.find(line)?.groupValues?.getOrNull(1)?.length?.div(2) ?: 0

    private fun Regex.matchOrNull(line: String, index: Int): String? = this.matchEntire(line)?.groupValues?.get(index)

    private fun List<String>.joinToArray(): ByteArray {
        val builder = StringBuilder()
        for (s in this) {
            builder.append(s).append('\n')
        }
        return builder.toString().toByteArray(charset = CHARSET)
    }

    private fun File.getLines(): List<String> = readLines(CHARSET).map { line -> line.replace("\t", "  ") }

    private fun File.writeLines(fileLines: List<String>){ runCatching { writeBytes(fileLines.joinToArray()) } }

    private fun File.createParentDirs() = com.google.common.io.Files.createParentDirs(this)

    private fun String.appendIfMissing(append: String, ignoreCase: Boolean = true): String {
        if (!this.endsWith(append, ignoreCase = ignoreCase))
            return "$this$append"
        return this
    }

    private companion object {
        val CHARSET = Charsets.UTF_8
        val COMMENT_PATTERN = """(\s*?#.*)$""".toPattern()
        val KEY_PATTERN = """(?i)^\s*([\w\d\-!@#$%^&*+]+?):.*$""".toRegex()
        val LIST_PATTERN = """(?i)^\s*(-\s?"?[\w\d]+"?).*$""".toRegex()
        val DEPTH_PATTERN = """^(\s+)[^\s]+""".toRegex()
    }

    private data class Comment(
        val type: CommentType,
        val index: Int,
        val lineAbove: String,
        val lineBelow: String,
        val key: String = "",
        val path: String = "",
        val content: List<String>
    ) {
        init {
            if (type == CommentType.FULL_MULTILINE) require(content.size > 1) { "$type comment has a content size of ${content.size}, this should not happen" }
            else require(content.size == 1) { "$type comment has a content size of ${content.size}, this should not happen" }
        }
    }

    private enum class CommentType {
        FULL_MULTILINE,
        FULL_LINE,
        DANGLING
    }
}
