package com.surelogic.jsecure.client.eclipse;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.*;

import javax.script.*;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import com.surelogic.common.PerformanceProperties;
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
	public static final String INDEX_KEY = "indexKey";
	/**
	 * Signature of the method
	 */
	public static final String NODE_NAME = "nodeName";
	/**
	 * Qualified name of the enclosing class
	 */
	public static final String PARENT_CLASS = "parentClass";

	public static final String CALLED = "called";
	
	/**
	 * For display purposes
	 */
	public static final String DECL_LABEL = "declLabel";
	public static final String CLASS_LABEL = "classLabel";
	public static final String ICON = "icon";
	
	public static final String LINE = "line";
	public static final String CALLS_HERE = "callsHere";
	public static final String USES_HERE = "usesHere";
	
	public static enum RelTypes /*implements RelationshipType*/ {
		// X calls method/constructor Y
	    CALLS(CALLS_HERE), 
	    // A uses field B
	    USES(USES_HERE);
	    
	    private final String here;
	    
	    private RelTypes(String here) {
	    	this.here = here;
	    }
	    
	    // Used to preserve the source info	    
	    public String getRefHere() {
	    	return here;
	    }
 	}
	
	public static enum VertexType {
		/*CLASS,*/ FUNCTION() {
			@Override
			DeclType encodeType(String nodeName) {
				return nodeName.contains("<init>") ? DeclType.CO : DeclType.ME;
			}	
		}, 
		FIELD() {
			@Override
			DeclType encodeType(String nodeName) {
				return DeclType.FL;
			}			
		};
		abstract DeclType encodeType(String nodeName);
	}
	
	public class Clazz {
		final String name;
		
		Clazz(String id) {
			name = id;
		}
	}
	
	Clazz result = null;	

	final TransactionalGraph graphDb;
	final Map<String,Vertex> keyedMap = new HashMap<String, Vertex>();
	final PerformanceProperties perf;
	int numMethods;
	int numDistinctCalls;
	int numDistinctUses;
	int numClasses;
	int numFromSource;
	
	public ClassSummarizer(final File runDir) {
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
		//graph.createKeyIndex(arg0, Edge.class);
		registerShutdownHook( graphDb );
		
		perf = new PerformanceProperties("jsecure.", runDir.getName(), runDir, "scan.properties");
		perf.startTiming();
	}

	public Clazz summarize(ZipFile jar, String className, boolean fromSource) throws IOException {
		ZipEntry e = jar.getEntry(className);
		if (e != null) {
			return summarize(jar.getInputStream(e), fromSource);		
		}
		return null;
	}
	
	public Clazz summarize(File classFile, boolean fromSource) throws IOException {
		return summarize(new FileInputStream(classFile), fromSource);
	}
	
	public Clazz summarize(InputStream is, boolean fromSource) throws IOException {		
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
		numClasses++;
		if (fromSource) {
			numFromSource++;
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
		/*
		System.out.println("Visiting class: "+name);
		System.out.println("Class Major Version: "+version);
		System.out.println("Super class: "+superName);
		*/
		result = new Clazz(name);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	/*
	 * Invoked only when the class being visited is an inner class
	 */
	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		//System.out.println("Outer class: "+owner);
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
		//System.out.println("Field: "+name+" "+desc+" value:"+value);
		return super.visitField(access, name, desc, signature, value);
	}


	@Override
	public void visitEnd() {
		//System.out.println("Method ends here");
		super.visitEnd();
	}

	/*
	 * When a method is encountered
	 */
	@Override
	public MethodVisitor visitMethod(final int access, final String name,
			final String desc, String signature, String[] exceptions) {
		final Vertex func = findFunctionVertex(result.name, name, desc, access);
		//System.out.println("Method: "+name+' '+desc);
		
		//return super.visitMethod(access, name, desc, signature, exceptions);
        MethodVisitor oriMv= new MethodVisitor(Opcodes.ASM4) {
        	int lastLine = -1;
        	        
        	public void visitLineNumber(int line, Label start) {
        		//System.out.println("Line "+line+" -- "+start);
        		lastLine = line;
        	}
        	
        	@Override
        	public void visitFieldInsn(int opcode, String owner, String name,
        			String desc) {
        		//System.out.println("\tField:  "+owner+", "+name+", "+desc);
        		getHandle(opcode, owner, name, desc);
        		super.visitFieldInsn(opcode, owner, name, desc);        		
        		
        		final Vertex field = findField(owner, name, -1);
        		addReference(func, RelTypes.USES, field, lastLine);
        		// TODO where do I store the source refs?
        	}
        	
        	@Override
        	public void visitMethodInsn(int opcode, String owner, String name,
        			String desc) {
        		//System.out.println("\tMethod: "+owner+", "+name+", "+desc);
        		getHandle(opcode, owner, name, desc);
        		super.visitMethodInsn(opcode, owner, name, desc);
        		
        		final Vertex callee = findFunctionVertex(owner, name, desc, -1);
        		addReference(func, RelTypes.CALLS, callee, lastLine);
        		// TODO where do I store the source refs?
        	}
        	
        	@Override
        	public void visitInvokeDynamicInsn(String name, String desc,
        			Handle bsm, Object... bsmArgs) {
         		System.out.println("\tDynamic: "+name+", "+desc+" via "+bsm.getOwner()+", "+bsm.getName()+", "+bsm.getDesc());
        		super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        		// TODO should I record these uses?
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
		//System.out.println("Source: "+source);
		super.visitSource(source, debug);
	}

	public void close() {		
		System.out.println("Shutting down");		
		graphDb.shutdown();
		perf.stopTiming("totalTimeInMillis");
		
		perf.setIntProperty("vertices", keyedMap.size());
		perf.setIntProperty("methods", numMethods);
		perf.setIntProperty("calls", numDistinctCalls);
		perf.setIntProperty("uses", numDistinctUses);
		perf.setIntProperty("classes", numClasses);
		perf.setIntProperty("fromSource", numFromSource);
		perf.store();
	}
	
	private void registerShutdownHook( final TransactionalGraph graphDb ) {
	    // Registers a shutdown hook for the Neo4j instance so that it
	    // shuts down nicely when the VM exits (even if you "Ctrl-C" the
	    // running example before it's completed)
	    Runtime.getRuntime().addShutdownHook( new Thread()
	    {
	        @Override
	        public void run()
	        {
	        	close();
	        }
	    } );
	}
	
	private Vertex findFunctionVertex(String clazzName, String name, String desc, int mods) {
		return findVertex(VertexType.FUNCTION, clazzName, name+' '+desc, mods);
	}
	
	private Vertex findField(String clazzName, String fieldName, int mods) {
		return findVertex(VertexType.FIELD, clazzName, fieldName, mods);
	}
	
	private Vertex findVertex(VertexType type, String clazzName, String nodeName, int access) {		
		// Check if already created
		final String id = clazzName+", "+nodeName;
		//for(Vertex v : graphDb.getVertices(INDEX_KEY, id)) {
		final Vertex v = keyedMap.get(id);
		if (v != null) {		

			if (access != -1 && v.getProperty(ICON) == null) {
			    v.setProperty(ICON, encodeIconForDecl(type.encodeType(nodeName), access));
			}
			return v; // return the first!
		}
		
		// Need to create
		Vertex node = graphDb.addVertex(null/*"class:"+type*/);
	    node.setProperty( INDEX_KEY, id );
	    if (clazzName == null) {
	    	throw new NullPointerException("Null clazzName");
	    }
	    node.setProperty(PARENT_CLASS, clazzName);
	    node.setProperty(NODE_NAME, nodeName);
	    node.setProperty(CLASS_LABEL, convertToClassLabel(clazzName));
	    node.setProperty(DECL_LABEL, convertToDeclLabel(type, nodeName));
	    if (access != -1) {
	    	node.setProperty(ICON, encodeIconForDecl(type.encodeType(nodeName), access));
	    }
	    keyedMap.put(id, node);
	    
	    if (type == VertexType.FUNCTION) {
	    	numMethods++;
	    }
	    return node;
	}
	
	private String convertToClassLabel(final String clazzName) {
		//final int lastSlash = clazzName.lastIndexOf('/');
		return clazzName.replace('/', '.');
	}

	private String convertToDeclLabel(final VertexType type, final String nodeName) {
		if (type == VertexType.FUNCTION) {
			final int space = nodeName.indexOf(' ');
			String name = nodeName.substring(0, space);
			String desc = nodeName.substring(space+1);
			Method m = new Method(name, desc);
			StringBuilder sb = new StringBuilder();
			sb.append(name).append('(');
			
			boolean first = true;
			for(final Type t : m.getArgumentTypes()) {
				if (first) {
					first = false;
				} else {
					sb.append(", ");
				}
				switch (t.getSort()) {
				case Type.ARRAY:
					sb.append(t.getElementType().getClassName());
					for(int i=0; i<t.getDimensions(); i++) {
						sb.append("[]");
					}
					break;
				default:
					sb.append(t.getClassName());					
				}
			}
			sb.append(')');
			return sb.toString();
		}
		return nodeName;
	}
	
	private void addReference(Vertex caller, RelTypes rel, Vertex callee, int line) {
		final String label = rel.toString();
		// TODO is this really necessary?
		// Check if edge already exists
		for(Edge e : caller.getEdges(Direction.OUT, label)) {
			if (callee.equals(e.getVertex(Direction.IN))) {
				// Already created
				return;
			}
		}
		graphDb.addEdge(null, caller, callee, label);
		if (rel == RelTypes.CALLS) {
			callee.setProperty(CALLED, Boolean.TRUE);			
			graphDb.addEdge(null, caller, callee, rel.getRefHere());
			numDistinctCalls++;
		} else {
			numDistinctUses++;
		}
	}

	public void dump() {
		/*
		System.out.println("Dumping database:");
		for (Vertex n : graphDb.getVertices()) {
			System.out.println(n.getProperty(INDEX_KEY)+" calls ...");
			for(Edge e : n.getEdges(Direction.OUT, RelTypes.CALLS.toString())) {
				System.out.println("\t"+e.getVertex(Direction.OUT).getProperty(INDEX_KEY));
			}
		}
		*/
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
	
	enum DeclType {
		CO, ME, FL
	}
	
	public String encodeIconForDecl(DeclType type, int access) {
		final StringBuilder sb = new StringBuilder();
		sb.append('@');
		sb.append(type.toString()).append(':');
		sb.append(encodeAccess(access));
		encodeModifiers(sb, access);
		//System.out.println("Encoding: "+sb);
		return sb.toString();
	}
	
	private String encodeAccess(int access) {
	    if ((access & Opcodes.ACC_PRIVATE) != 0) {
	    	return "PR";
	    }
	    if ((access & Opcodes.ACC_PROTECTED) != 0) {
	    	return "PO";
	    }
	    if ((access & Opcodes.ACC_PUBLIC) != 0) {
	    	return "PU";
	    }
	    return "DE";
	}
	
	private void encodeModifiers(final StringBuilder sb, int access) {
		boolean first = true;
		if ((access & Opcodes.ACC_ABSTRACT) != 0) {
			if (first) {
				first = false;
				sb.append(':');
			}
			sb.append('A');
		}
		if ((access & Opcodes.ACC_FINAL) != 0) {
			if (first) {
				first = false;
				sb.append(':');
			}
			sb.append('F');
		}
		/*
		    if ((access & Opcodes.ACC_NATIVE) != 0) {
		    }
		 */
		if ((access & Opcodes.ACC_STATIC) != 0) {
			if (first) {
				first = false;
				sb.append(':');
			}
			sb.append('S');
		}
		/*
		if ((access & Opcodes.ACC_SYNCHRONIZED) != 0) {
		}
		if ((access & Opcodes.ACC_TRANSIENT) != 0) {
		}
		*/
		if ((access & Opcodes.ACC_VOLATILE) != 0) {
			if (first) {
				first = false;
				sb.append(':');
			}
			sb.append('V');
		}
		if ((access & Opcodes.ACC_BRIDGE) != 0 || (access & Opcodes.ACC_SYNTHETIC) != 0 ) {
			if (first) {
				first = false;
				sb.append(':');
			}
			sb.append('I');
		}
	}
}