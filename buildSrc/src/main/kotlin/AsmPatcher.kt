
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

private val kotlinHiddenClasses = listOf<String>("kotlin.Any")

class AnnotationScanner(val cw: ClassWriter, val patch: Map<String, String>) : ClassVisitor(Opcodes.ASM9, cw) {
    val reverRelocation = patch.values.map { "$it/kotlin/Any" }.toSet()
    var wasPatched = false

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        return MetadataVisitor(cw.visitAnnotation(descriptor, visible))
    }

    inner class MetadataVisitor(val av: AnnotationVisitor, val thatArray: Boolean = false) : AnnotationVisitor(Opcodes.ASM9, av) {
        override fun visit(name: String?, value: Any?) {
            val newValue = when {
                thatArray && value is String && value.startsWith("(") -> {
                    patch.entries.fold(value) { n, u ->
                        n.replace(u.key, u.value)
                    }.also {
                        if (it != value) {
                            wasPatched = true
                        }
                    }
                }
                else -> value
            }
            av.visit(name, newValue)
        }

        override fun visitArray(name: String?): AnnotationVisitor? {
            return if (name == "d2") {
                MetadataVisitor(av.visitArray(name), true)
            } else {
                av.visitArray(name)
            }
        }

        override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor {
            return av.visitAnnotation(name, descriptor)
        }

        override fun visitEnum(name: String?, descriptor: String?, value: String?) {
            av.visitEnum(name, descriptor, value)
        }

        override fun visitEnd() {
            av.visitEnd()
        }
    }
}
