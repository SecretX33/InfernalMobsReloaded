
import com.github.jengelman.gradle.plugins.shadow.relocation.RelocateClassContext
import com.github.jengelman.gradle.plugins.shadow.relocation.RelocatePathContext
import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator
import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Action
import org.gradle.api.tasks.SourceSetContainer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

object ConstructorAnnotationAsm {

    fun patch(projectDir: File, sourceSets: SourceSetContainer, predicate: (clazz: String) -> Boolean) {
        println("ConstructorAnnotationAsm.patch called")
        sourceSets.flatMap { it.runtimeClasspath.files }
            .flatMap { it.listFilesRecursively() }
            .filter { it.isFile
                    && it.extension == "class"
                    && !it.nameWithoutExtension.contains('$')
                    && it.isFromMain(projectDir)
                    && predicate(it.className(projectDir)) }
            .forEach { it.patchFile() }
    }

    private fun File.isFromMain(projectDir: File): Boolean = relativeTo(projectDir).toString().split(File.separatorChar)[3] == "main"

    private fun File.className(projectDir: File): String = absoluteFile
        .relativeTo(projectDir)
        .toString()
        .let { it.substring(0, it.length - extension.length - 1) }
        .split(File.separatorChar)
        .let { it.subList(4, it.size) }
        .joinToString(".")

    private fun File.listFilesRecursively(): Set<File> =
        listFiles()?.flatMapTo(hashSetOf()) {
            if (it.isDirectory) it.listFilesRecursively() else setOf(it)
        } ?: emptySet()


    private fun File.patchFile() {
        println("ConstructorAnnotationAsm.patchFile called")
        inputStream().buffered().use { ins ->
            val cr = ClassReader(ins)
            val cw = ClassWriter(cr, 0)
            val scanner = InjectAnnotationAdder(cw)
            cr.accept(scanner, 0)
            if (scanner.wasModified) {
                ins.close()
                delete()
                writeBytes(cw.toByteArray())
            }
        }
    }
}

class KotlinRelocator(private val task: ShadowJar, private val delegate: SimpleRelocator) : Relocator by delegate {

    override fun relocatePath(context: RelocatePathContext?): String {
        return delegate.relocatePath(context).also {
            foundRelocatedSubPaths.getOrPut(task) { hashSetOf() }.add(it.substringBeforeLast('/'))
        }
    }

    override fun relocateClass(context: RelocateClassContext?): String {
        return delegate.relocateClass(context).also {
            val packageName = it.substringBeforeLast('.')
            foundRelocatedSubPaths.getOrPut(task) { hashSetOf() }.add(packageName.replace('.', '/'))
        }
    }

    companion object {
        private val foundRelocatedSubPaths: MutableMap<ShadowJar, MutableSet<String>> = hashMapOf()
        private val relocationPaths = mutableMapOf<String, String>()

        internal fun storeRelocationPath(pattern: String, destination: String) {
            relocationPaths[pattern.replace('.', '/') + "/"] = destination.replace('.', '/') + "/"
        }
        private fun patchFile(file: Path) {
            if(Files.isDirectory(file) || !file.toString().endsWith(".class")) return
            Files.newInputStream(file).use { ins ->
                val cr = ClassReader(ins)
                val cw = ClassWriter(cr, 0)
                val scanner = AnnotationScanner(cw, relocationPaths)
                cr.accept(scanner, 0)
                if (scanner.wasPatched) {
                    ins.close()
                    Files.delete(file)
                    Files.write(file, cw.toByteArray())
                }
            }
        }

        fun patchMetadata(task: ShadowJar) {
            val zip = task.archiveFile.get().asFile.toPath()
            FileSystems.newFileSystem(zip, null as ClassLoader?).use { fs ->
                foundRelocatedSubPaths[task]?.forEach {
                    val packagePath = fs.getPath(it)
                    if (Files.exists(packagePath) && Files.isDirectory(packagePath)) {
                        Files.list(packagePath).forEach { file ->
                            patchFile(file)
                        }
                    }
                }
            }
        }
    }
}

fun ShadowJar.kotlinRelocate(pattern: String, destination: String, configure: Action<SimpleRelocator>) {
    val delegate = SimpleRelocator(pattern, destination, ArrayList(), ArrayList())
    configure.execute(delegate)
    KotlinRelocator.storeRelocationPath(pattern, destination)
    relocate(KotlinRelocator(this, delegate))
}

fun ShadowJar.kotlinRelocate(pattern: String, destination: String) {
    kotlinRelocate(pattern, destination) {}
}
