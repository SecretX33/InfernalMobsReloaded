
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter

class InjectAnnotationAdder(writer: ClassWriter) : ClassVisitor(Opcodes.ASM9, writer) {

    private val tracer = TraceClassVisitor(cv, PrintWriter(System.out))
    private var isInjectAnnotationPresent = false
    var wasModified = false

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String?,
        signature: String?,
        exceptions: Array<String?>?
    ): MethodVisitor {
        val methodVisitor = tracer.visitMethod(access, name, descriptor, signature, exceptions)
        if (name != "<init>") return methodVisitor
        methodVisitor.visitAnnotation("javax/Inject", true).visitEnd()
        return methodVisitor
    }

    override fun visitEnd() {
        if(isInjectAnnotationPresent) {
            tracer.visitEnd()
            return
        }
//        val constructorVisit = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", )
//        val annotVisitor = methodVisitor.visitAnnotation("javax/inject/Inject", true)
//        annotVisitor?.visitEnd()
        println("")
        tracer.visitEnd()
        println("${tracer.p.text}")
    }
}
