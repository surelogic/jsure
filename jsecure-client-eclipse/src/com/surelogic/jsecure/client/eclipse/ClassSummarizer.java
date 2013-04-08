package com.surelogic.jsecure.client.eclipse;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.objectweb.asm.*;

import com.surelogic.common.StringCache;

/**
 * Creates a Clazz after visiting
 * 
 * @author edwin
 */
public class ClassSummarizer extends ClassVisitor {
	public static final String DB_PATH = "neo4j-db";
	public static final String FUNC_IDX = "function-index";
	public static final String INDEX_KEY = "index-key";
	public static final String NODE_NAME = "node-name";
	public static final String PARENT_CLASS = "parent-class";
	public static final String FIELD_IDX = "field-index";
	
	public static enum RelTypes implements RelationshipType {
		// X calls method/constructor Y
	    CALLS, 
	    // A uses field B
	    USES
	}
	
	public class Clazz {
		final String name;
		
		Clazz(String id) {
			name = id;
		}
	}
	
	Clazz result = null;	

	final GraphDatabaseService graphDb;
	final Index<Node> funcIndex;
	final Index<Node> fieldIndex;
	
	public ClassSummarizer(File runDir) {
		super(Opcodes.ASM4);
		
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( new File(runDir, DB_PATH).getAbsolutePath() );
		funcIndex = graphDb.index().forNodes( FUNC_IDX );
		fieldIndex = graphDb.index().forNodes( FIELD_IDX );
		registerShutdownHook( graphDb );
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
		
		final Transaction tx = graphDb.beginTx();
		try {
			final ClassReader cr2 = new ClassReader(is);
			cr2.accept(this, 0);		
			// Updating operations go here
		    tx.success();
		} finally {
		    tx.finish();
		}
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
		result = new Clazz(name);
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
	public MethodVisitor visitMethod(int access, final String name,
			final String desc, String signature, String[] exceptions) {
		final String id = name+" "+desc;
		final Node func = findFunctionNode(result.name, id);
		System.out.println("Method: "+id);
		
		//return super.visitMethod(access, name, desc, signature, exceptions);
        MethodVisitor oriMv= new MethodVisitor(Opcodes.ASM4) {
        	@Override
        	public void visitFieldInsn(int opcode, String owner, String name,
        			String desc) {
        		System.out.println("\tField:  "+owner+", "+name+", "+desc);
        		getHandle(opcode, owner, name, desc);
        		super.visitFieldInsn(opcode, owner, name, desc);        		
        		
        		final Node field = findField(owner, name);
        		addReference(func, RelTypes.USES, field);
        	}
        	
        	@Override
        	public void visitMethodInsn(int opcode, String owner, String name,
        			String desc) {
         		System.out.println("\tMethod: "+owner+", "+name+", "+desc);
        		getHandle(opcode, owner, name, desc);
        		super.visitMethodInsn(opcode, owner, name, desc);
        		
        		final Node callee = findFunctionNode(owner, name, desc);
        		addReference(func, RelTypes.CALLS, callee);
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

	public void close() {
		graphDb.shutdown();
	}
	
	private static void registerShutdownHook( final GraphDatabaseService graphDb ) {
	    // Registers a shutdown hook for the Neo4j instance so that it
	    // shuts down nicely when the VM exits (even if you "Ctrl-C" the
	    // running example before it's completed)
	    Runtime.getRuntime().addShutdownHook( new Thread()
	    {
	        @Override
	        public void run()
	        {
	            graphDb.shutdown();
	        }
	    } );
	}
	
	private Node findFunctionNode(String clazzName, String name, String desc) {
		return findFunctionNode(clazzName, name+' '+desc);
	}
	
	private Node findFunctionNode(String clazzName, String funcName) {
		return findNode(funcIndex, clazzName, funcName);
	}
	
	private Node findField(String clazzName, String fieldName) {
		return findNode(fieldIndex, clazzName, fieldName);
	}
	
	private Node findNode(Index<Node> index, String clazzName, String nodeName) {
		String id = clazzName+", "+nodeName;
		IndexHits<Node> hits = index.get(INDEX_KEY, id);
		if (hits.hasNext()) {
			return hits.getSingle();
		}
		// Need to create
		Node node = graphDb.createNode();
	    node.setProperty( INDEX_KEY, id );
	    node.setProperty(PARENT_CLASS, clazzName);
	    node.setProperty(NODE_NAME, nodeName);
	    index.add( node, INDEX_KEY, id );
	    return node;
	}
	
	private void addReference(Node caller, RelationshipType rel, Node callee) {
		caller.createRelationshipTo(callee, rel);
	}
}