package com.github.secretx33.infernalmobsreloaded.asm

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter

class InjectAdder {

    fun patchClass(classFile: ByteArray): ByteArray? {
        val reader = ClassReader(classFile)
        val writer = ClassWriter(reader, 0)
        val visitor = AddInjectAnnotationAdapter(writer)
        reader.accept(visitor, 0)
        return writer.toByteArray()
    }

    class AddInjectAnnotationAdapter(writer: ClassWriter): ClassVisitor(Opcodes.ASM9, writer) {

        private val tracer = TraceClassVisitor(cv, PrintWriter(System.out))
        private var isInjectAnnotationPresent = false

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String?,
            signature: String?,
            exceptions: Array<String?>?
        ): MethodVisitor {
            val methodVisitor = tracer.visitMethod(access, name, descriptor, signature, exceptions)
            if (name != "<init>") return methodVisitor
            println("visitMethod(access = [${access}], name = [${name}], descriptor = [${descriptor}], signature = [${signature}], exceptions = [${exceptions}])")
            return methodVisitor
        }

        override fun visitEnd() {
//            if(isInjectAnnotationPresent) {
//                cv.visitEnd()
//                return
//            }
//            val constructorVisit = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", )
//            val annotVisitor = methodVisitor.visitAnnotation("javax/inject/Inject", true)
//            annotVisitor?.visitEnd()
//            cv.visitEnd()

            println("")
            tracer.visitEnd()
            println("visitEnd called, ${tracer.p.text}")
        }
    }
}
