package com.surelogic.jsecure.client.eclipse;

import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;

import javax.script.*;

import org.objectweb.asm.*;

import com.surelogic.common.StringCache;

import com.tinkerpop.blueprints.*;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.pipes.Pipe;

/**
 * Creates a Clazz after visiting
 * 
 * @author edwin
 */
public class ClassSummarizer extends ClassVisitor {
	public static final String DB_PATH = "orientdb";
	//public static final String FUNC_IDX = "function-index";
	public static final String INDEX_KEY = "indexKey";
	public static final String NODE_NAME = "nodeName";
	public static final String PARENT_CLASS = "parentClass";
	//public static final String FIELD_IDX = "field-index";
	
	public static enum RelTypes /*implements RelationshipType*/ {
		// X calls method/constructor Y
	    CALLS, 
	    // A uses field B
	    USES
	}
	
	public static enum VertexType {
		CLASS, FUNCTION, FIELD
	}
	
	public class Clazz {
		final String name;
		
		Clazz(String id) {
			name = id;
		}
	}
	
	Clazz result = null;	

	final TransactionalGraph graphDb;

	public ClassSummarizer(File runDir) {
		super(Opcodes.ASM4);
		
		File dbLoc = new File(runDir, DB_PATH);
		/*
		OGraphDatabase odb = new OGraphDatabase("local:"+dbLoc.getAbsolutePath());
		if (!odb.exists()) {
			odb.create();
		}
		odb.open(null, null);
		*/
		OrientGraph graph = new OrientGraph("local:"+dbLoc.getAbsolutePath());
		graphDb = graph;
		graph.createKeyIndex(INDEX_KEY, Vertex.class);
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
		try {
			// Updating operations go here
			final ClassReader cr2 = new ClassReader(is);
			cr2.accept(this, 0);				
		} catch (RuntimeException e) {
		    graphDb.rollback();
		    throw e;
		} catch (IOException e) {
			graphDb.rollback();
			throw e;
		}
		graphDb.commit();
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
		final Vertex func = findFunctionVertex(result.name, id);
		System.out.println("Method: "+id);
		
		//return super.visitMethod(access, name, desc, signature, exceptions);
        MethodVisitor oriMv= new MethodVisitor(Opcodes.ASM4) {
        	@Override
        	public void visitFieldInsn(int opcode, String owner, String name,
        			String desc) {
        		System.out.println("\tField:  "+owner+", "+name+", "+desc);
        		getHandle(opcode, owner, name, desc);
        		super.visitFieldInsn(opcode, owner, name, desc);        		
        		
        		final Vertex field = findField(owner, name);
        		addReference(func, RelTypes.USES, field);
        	}
        	
        	@Override
        	public void visitMethodInsn(int opcode, String owner, String name,
        			String desc) {
         		System.out.println("\tMethod: "+owner+", "+name+", "+desc);
        		getHandle(opcode, owner, name, desc);
        		super.visitMethodInsn(opcode, owner, name, desc);
        		
        		final Vertex callee = findFunctionVertex(owner, name, desc);
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
		System.out.println("Shutting down");		
		graphDb.shutdown();
	}
	
	private static void registerShutdownHook( final TransactionalGraph graphDb ) {
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
	
	private Vertex findFunctionVertex(String clazzName, String name, String desc) {
		return findFunctionVertex(clazzName, name+' '+desc);
	}
	
	private Vertex findFunctionVertex(String clazzName, String funcName) {
		return findVertex(VertexType.FUNCTION, clazzName, funcName);
	}
	
	private Vertex findField(String clazzName, String fieldName) {
		return findVertex(VertexType.FIELD, clazzName, fieldName);
	}
	
	private Vertex findVertex(VertexType type, String clazzName, String nodeName) {
		// Check if already created
		final String id = clazzName+", "+nodeName;
		for(Vertex v : graphDb.getVertices(INDEX_KEY, id)) {
			return v; // return the first!
		}
		// Need to create
		Vertex node = graphDb.addVertex(null/*"class:"+type*/);
	    node.setProperty( INDEX_KEY, id );
	    node.setProperty(PARENT_CLASS, clazzName);
	    node.setProperty(NODE_NAME, nodeName);
	    return node;
	}
	
	private void addReference(Vertex caller, RelTypes rel, Vertex callee) {
		//Edge eLives = 
		graphDb.addEdge(null, caller, callee, rel.toString());
	}

	public void dump() {
		System.out.println("Dumping database:");
		for (Vertex n : graphDb.getVertices()) {
			System.out.println(n.getProperty(INDEX_KEY)+" calls ...");
			for(Edge e : n.getEdges(Direction.OUT, RelTypes.CALLS.toString())) {
				System.out.println("\t"+e.getVertex(Direction.OUT).getProperty(INDEX_KEY));
			}
		}
	}

	public void query(String query) {
		// let's execute a query now
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("gremlin-groovy");
		engine.getBindings(ScriptContext.ENGINE_SCOPE).put("g", graphDb);
		try {
			Pipe<?,?> result = (Pipe<?,?>) engine.eval(query);
			for(Object o : result) {
				System.out.println(o);
			}
		} catch (ScriptException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}