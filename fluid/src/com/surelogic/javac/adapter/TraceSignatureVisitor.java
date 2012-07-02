package com.surelogic.javac.adapter;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * ASM signature visitor which reports all calls.
 */
public class TraceSignatureVisitor extends SignatureVisitor
{
    private final String m_indent;
    
    public TraceSignatureVisitor(String indent) {
    	super(Opcodes.ASM4);
        m_indent = indent;
    }

    private void printIndented(String text) {
        System.out.print(m_indent);
        System.out.println(text);
    }
    
    public void visitFormalTypeParameter(String name) {
        printIndented("visitFormalTypeParameter(" + name + ')');
    }

    public SignatureVisitor visitClassBound() {
        printIndented("visitClassBound()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    public SignatureVisitor visitInterfaceBound() {
        printIndented("visitInterfaceBound()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    public SignatureVisitor visitSuperclass() {
        printIndented("visitSuperclass()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    public SignatureVisitor visitInterface() {
        printIndented("visitInterface()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    public SignatureVisitor visitParameterType() {
        printIndented("visitParameterType()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    public SignatureVisitor visitReturnType() {
        printIndented("visitReturnType()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    public SignatureVisitor visitExceptionType() {
        printIndented("visitExceptionType()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    public void visitBaseType(char desc) {
        printIndented("visitBaseType(" + desc + ')');
    }

    public void visitTypeVariable(String name) {
        printIndented("visitTypeVariable(" + name + ')');
    }

    public SignatureVisitor visitArrayType() {
        printIndented("visitArrayType()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    public void visitClassType(String name) {
        printIndented("visitClassType(" + name + ')');
    }

    public void visitInnerClassType(String name) {
        printIndented("visitInnerClassType(" + name + ')');
    }

    public void visitTypeArgument() {
        printIndented("visitTypeArgument()");
    }

    public SignatureVisitor visitTypeArgument(char wildcard) {
        printIndented("visitTypeArgument(" + wildcard + ")");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    public void visitEnd() {
        printIndented("visitEnd()");
    }
}