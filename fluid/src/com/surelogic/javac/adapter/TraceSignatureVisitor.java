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
    
    @Override
    public void visitFormalTypeParameter(String name) {
        printIndented("visitFormalTypeParameter(" + name + ')');
    }

    @Override
    public SignatureVisitor visitClassBound() {
        printIndented("visitClassBound()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    @Override
    public SignatureVisitor visitInterfaceBound() {
        printIndented("visitInterfaceBound()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    @Override
    public SignatureVisitor visitSuperclass() {
        printIndented("visitSuperclass()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    @Override
    public SignatureVisitor visitInterface() {
        printIndented("visitInterface()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    @Override
    public SignatureVisitor visitParameterType() {
        printIndented("visitParameterType()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    @Override
    public SignatureVisitor visitReturnType() {
        printIndented("visitReturnType()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    @Override
    public SignatureVisitor visitExceptionType() {
        printIndented("visitExceptionType()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    @Override
    public void visitBaseType(char desc) {
        printIndented("visitBaseType(" + desc + ')');
    }

    @Override
    public void visitTypeVariable(String name) {
        printIndented("visitTypeVariable(" + name + ')');
    }

    @Override
    public SignatureVisitor visitArrayType() {
        printIndented("visitArrayType()");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    @Override
    public void visitClassType(String name) {
        printIndented("visitClassType(" + name + ')');
    }

    @Override
    public void visitInnerClassType(String name) {
        printIndented("visitInnerClassType(" + name + ')');
    }

    @Override
    public void visitTypeArgument() {
        printIndented("visitTypeArgument()");
    }

    @Override
    public SignatureVisitor visitTypeArgument(char wildcard) {
        printIndented("visitTypeArgument(" + wildcard + ")");
        return new TraceSignatureVisitor(m_indent + " ");
    }

    @Override
    public void visitEnd() {
        printIndented("visitEnd()");
    }
}