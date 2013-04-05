package com.surelogic.jsecure.client.eclipse;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;

import org.objectweb.asm.*;

import com.surelogic.common.StringCache;

/**
 * Creates a Clazz after visiting
 * 
 * @author edwin
 */
public class ClassSummarizer extends ClassVisitor {
	public class Clazz {
		
	}
	
	Clazz result = null;

	public ClassSummarizer() {
		super(Opcodes.ASM4);
	}

	public Clazz summarize(ZipFile jar, String className) throws IOException {
		ZipEntry e = jar.getEntry(className);
		if (e != null) {
			return summarize(jar.getInputStream(e));		
		}
		return null;
	}
	
	public Clazz summarize(File classFile) throws IOException {
		return summarize(new FileInputStream(classFile));
	}
	
	public Clazz summarize(InputStream is) throws IOException {		
		init();
		
		final ClassReader cr2 = new ClassReader(is);
		cr2.accept(this, 0);		
		return finish();
	}
	
	public void init() {
		result = null;
	}

	public Clazz finish() {
		try {
			return result;
		} finally {
			result = null;
		}
	}

	/*
	 * Called when a class is visited. This is the method called first
	 */
	@Override
	public void visit(int version, int access, String name,
			String signature, String superName, String[] interfaces) {
		System.out.println("Visiting class: "+name);
		System.out.println("Class Major Version: "+version);
		System.out.println("Super class: "+superName);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	/*
	 * Invoked only when the class being visited is an inner class
	 */
	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		System.out.println("Outer class: "+owner);
		super.visitOuterClass(owner, name, desc);
	}

	/*
	 *Invoked when a class level annotation is encountered
	 */
	@Override
	public AnnotationVisitor visitAnnotation(String desc,
			boolean visible) {
		//System.out.println("Annotation: "+desc);
		return super.visitAnnotation(desc, visible);
	}

	/*
	 * When a class attribute is encountered 
	 */
	@Override
	public void visitAttribute(Attribute attr) {
		//System.out.println("Class Attribute: "+attr.type);
		super.visitAttribute(attr);
	}

	/*
	 *When an inner class is encountered 
	 */
	@Override
	public void visitInnerClass(String name, String outerName,
			String innerName, int access) {
		//System.out.println("Inner Class: "+ innerName+" defined in "+outerName);
		super.visitInnerClass(name, outerName, innerName, access);
	}

	/*
	 * When a field is encountered
	 */
	@Override
	public FieldVisitor visitField(int access, String name,
			String desc, String signature, Object value) {
		System.out.println("Field: "+name+" "+desc+" value:"+value);
		return super.visitField(access, name, desc, signature, value);
	}


	@Override
	public void visitEnd() {
		System.out.println("Method ends here");
		super.visitEnd();
	}

	/*
	 * When a method is encountered
	 */
	@Override
	public MethodVisitor visitMethod(int access, String name,
			String desc, String signature, String[] exceptions) {
		System.out.println("Method: "+name+" "+desc);
		//return super.visitMethod(access, name, desc, signature, exceptions);
        MethodVisitor oriMv= new MethodVisitor(Opcodes.ASM4) {
        	@Override
        	public void visitFieldInsn(int opcode, String owner, String name,
        			String desc) {
        		System.out.println("\tField:  "+owner+", "+name+", "+desc);
        		getHandle(opcode, owner, name, desc);
        		super.visitFieldInsn(opcode, owner, name, desc);
        	}
        	
        	@Override
        	public void visitMethodInsn(int opcode, String owner, String name,
        			String desc) {
         		System.out.println("\tMethod: "+owner+", "+name+", "+desc);
        		getHandle(opcode, owner, name, desc);
        		super.visitMethodInsn(opcode, owner, name, desc);
        	}
        	
        	@Override
        	public void visitInvokeDynamicInsn(String name, String desc,
        			Handle bsm, Object... bsmArgs) {
         		System.out.println("\tDynamic: "+name+", "+desc+" via "+bsm.getOwner()+", "+bsm.getName()+", "+bsm.getDesc());
        		super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        	}
        };
        return oriMv;
	}

	public static void printStats() {
		System.out.println("Ended up with "+handleCache.size()+" cached out of "+totalHandles.get());
		System.out.println("\tand "+stringCache.size()+" strings cached");
	}
	
	
	static StringCache stringCache = new StringCache();
	static ConcurrentMap<Handle,Handle> handleCache = new ConcurrentHashMap<Handle, Handle>();
	static AtomicInteger totalHandles = new AtomicInteger();
	
	private Handle getHandle(int opcode, String owner, String name,	String desc) {
		// Don't need to distinguish reads from writes?
		Handle temp = new Handle(opcode, stringCache.aliasIfPossible(owner), 
									     stringCache.aliasIfPossible(name), 
				                         stringCache.aliasIfPossible(desc));
		totalHandles.incrementAndGet();
		Handle rv = handleCache.putIfAbsent(temp, temp);
		if (rv == null) {
			rv = temp;
		} 			
		return rv;
	}
	
	/*
	 * When the optional source is encountered
	 */
	@Override
	public void visitSource(String source, String debug) {
		System.out.println("Source: "+source);
		super.visitSource(source, debug);
	}
}