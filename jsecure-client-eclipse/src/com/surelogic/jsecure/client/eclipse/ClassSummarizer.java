package com.surelogic.jsecure.client.eclipse;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.*;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.signature.*;

import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.serialization.OSerializableStream;
import com.surelogic.common.Pair;
import com.surelogic.common.PerformanceProperties;
import com.surelogic.common.StringCache;
import com.surelogic.common.java.Config;
import com.surelogic.common.java.IJavaFile;
import com.surelogic.common.java.JavaClassPath;
import com.surelogic.common.java.JavaProject;
import com.surelogic.common.java.JavaProjectSet;
import com.surelogic.common.jobs.SLProgressMonitor;
import com.surelogic.common.jobs.SLStatus;

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
	/**
	 * for methods, the parent class and method signature
	 */
	public static final String INDEX_KEY = "indexKey";
	/**
	 * Signature of the method
	 */
	public static final String NODE_NAME = "nodeName";
	/**
	 * Qualified name of the enclosing class
	 */
	public static final String PARENT_CLASS = "parentClass";
	public static final String PROJECT = "project";
	
	// These two are actually redundant
	public static final String CALLED = "called";
	public static final String NUM_TIMES_CALLED = "numCalled";
	public static final String FROM_SOURCE = "fromSource";
	public static final String INVOKE_DYNAMIC = "invokeDynamic";
	
	/**
	 * For display purposes
	 */
	public static final String DECL_LABEL = "declLabel";
	public static final String CLASS_LABEL = "classLabel";
	public static final String ICON = "icon";
	
	public static final String LINE = "line";
	public static final String CALLS_HERE = "callsHere";
	public static final String USES_HERE = "usesHere";
	/**
	 * Property of the edge
	 */
	public static final String WRITES_FIELD = "writesField";
	public static final String READS_FIELD = "readsField";
	
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
	
	public class Clazz extends SignatureVisitor {
		final String name;
		
		final Set<String> dependencies = new HashSet<String>();
		
		Clazz(String id) {
			super(Opcodes.ASM4);
			name = id;
		}

		void processDescriptor(String desc) {
			final Type t = Type.getType(desc);
			processType(t);
		}
		
		void processType(final Type t) {
			switch (t.getSort()) {
			case Type.OBJECT:
				processTypeName(t.getClassName());
				break;
			case Type.METHOD:
				processType(t.getReturnType());
				for(Type at : t.getArgumentTypes()) {
					processType(at);
				}
				break;
			default:
			}
		}
		
		void processSignature(String sig) {
			if (sig == null) {
				return;
			}
			new SignatureReader(sig).accept(this);
		}

		@Override
		public void visitClassType(String qname) {			
			processTypeName(qname);
		}

		void processTypeName(String type) {
			final int lastSlash = type.lastIndexOf('/');
			type = type.replace('/', '.');
			dependencies.add(type);
			
			for(int i = type.lastIndexOf('$'); i>lastSlash; i = type.lastIndexOf('$')) {
				type = type.substring(0, i);
				dependencies.add(type);
			}
		}

		/*
		@Override
		public void visitInnerClassType(String arg0) {
			// TODO Auto-generated method stub
			
		}
		*/
	}

	// Should match the code above in processTypeName()
	private String getProject(String clazzName)  {
		final int lastSlash = clazzName.lastIndexOf('/');
		String type = clazzName.replace('/', '.');
		String proj = projectMap.get(type);
		if (proj == null) {
			for(int i = type.lastIndexOf('$'); i>lastSlash && proj == null; i = type.lastIndexOf('$')) {
				type = type.substring(0, i);
				proj = projectMap.get(type);
			}
		}
		return proj;
	}
	
	Clazz result = null;	

	final TransactionalGraph graphDb;
	final Map<Id,Vertex> keyedMap = new HashMap<Id, Vertex>();
	final PerformanceProperties perf;
	int numMethods;
	int numDistinctCalls;
	int numDistinctUses;
	int numClasses;
	int numFromSource;
		
	final JavaClassPath<JavaProjectSet<JavaProject>> classPath;
	// Class to project
	final Map<String,String> projectMap = new HashMap<String, String>();
	// The project that the projectMap is precomputed for
	String mappedProject = null;
	boolean fromSource;
	
	public ClassSummarizer(final File runDir, JavaClassPath<JavaProjectSet<JavaProject>> classes) {
		super(Opcodes.ASM4);
		classPath = classes;
		
		File dbLoc = new File(runDir, DB_PATH);
		/*
		OGraphDatabase odb = new OGraphDatabase("local:"+dbLoc.getAbsolutePath());
		if (!odb.exists()) {
			odb.create();
		}
		odb.open(null, null);
		*/
		//OGlobalConfiguration.BLUEPRINTS_TX_MODE.setValue(1);
		OrientGraph graph = new OrientGraph("local:"+dbLoc.getAbsolutePath());
		graphDb = graph;
		graph.createKeyIndex(INDEX_KEY, Vertex.class);
		graph.getRawGraph().declareIntent(new OIntentMassiveInsert());		
		/*
		final OClass vertexClass = graph.getVertexBaseType();
		vertexClass.createProperty(INDEX_KEY, OType.CUSTOM);
		*/
		//graph.createKeyIndex(arg0, Edge.class);
		registerShutdownHook( graphDb );
		
		perf = new PerformanceProperties("jsecure.", runDir.getName(), runDir, "scan.properties");
		perf.startTiming();
	}
	
	public Clazz summarize(IJavaFile file, boolean fromSource) throws IOException {		
		init(file, fromSource);
		try {
			// Updating operations go here
			final ClassReader cr2 = new ClassReader(file.getStream());
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
	
	public void init(IJavaFile file, boolean fromSource) {
		result = null;
		if (file.getProject().equals(mappedProject)) {
			return; // Already computed
		}
		mappedProject = file.getProject();
		for(Pair<String,String> key : classPath.getMapKeys()) {
			if (mappedProject.equals(key.first())) {				
				projectMap.put(key.second(), classPath.getMapping(key).getProject());
			}
		}
		this.fromSource = fromSource;
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
		/*
		System.out.println("Class Major Version: "+version);
		System.out.println("Super class: "+superName);
		*/
		result = new Clazz(name);
		if (superName != null) {
			result.processTypeName(superName);
		}
		result.processSignature(signature);				
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
		result.processDescriptor(desc);
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
		result.processSignature(signature);
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
		result.processDescriptor(desc);
		result.processSignature(signature);
        MethodVisitor oriMv= new MethodVisitor(Opcodes.ASM4) {
        	int lastLine = -1;
        	        
        	@Override
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
        		result.processDescriptor(desc);
        		
        		final Vertex field = findField(owner, name, -1);
        		addReference(func, RelTypes.USES, field, lastLine, opcode);
        		// TODO where do I store the source refs?
        	}
        	
        	@Override
        	public void visitMethodInsn(int opcode, String owner, String name,
        			String desc) {
        		//System.out.println("\tMethod: "+owner+", "+name+", "+desc);
        		getHandle(opcode, owner, name, desc);
        		super.visitMethodInsn(opcode, owner, name, desc);
        		result.processDescriptor(desc);
        		
        		final Vertex callee = findFunctionVertex(owner, name, desc, -1);
        		addReference(func, RelTypes.CALLS, callee, lastLine, opcode);
        		// TODO where do I store the source refs?
        	}
        	
        	@Override
        	public void visitInvokeDynamicInsn(String name, String desc,
        			Handle bsm, Object... bsmArgs) {
         		System.out.println("\tDynamic: "+name+", "+desc+" via "+bsm.getOwner()+", "+bsm.getName()+", "+bsm.getDesc());
        		super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        		result.processDescriptor(desc);
        		func.setProperty(INVOKE_DYNAMIC, Boolean.TRUE);
        	}
        	
        	@Override
        	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        		result.processDescriptor(desc);
        		return super.visitAnnotation(desc, visible);
        	}
        	
        	@Override
            public void visitLocalVariable(String name, String desc, String signature,
                    Label start, Label end, int index) {
            	super.visitLocalVariable(name, desc, signature, start, end, index);
            	result.processSignature(signature);
            }
        	
        	@Override
            public void visitMultiANewArrayInsn(String desc, int dims) {
           		result.processDescriptor(desc);
        		super.visitMultiANewArrayInsn(desc, dims);
        	}
        	
        	@Override
            public AnnotationVisitor visitParameterAnnotation(int parameter,
                    String desc, boolean visible) {
        		result.processDescriptor(desc);
        		return super.visitParameterAnnotation(parameter, desc, visible);
        	}
        	
        	@Override
            public void visitTryCatchBlock(Label start, Label end, Label handler,
                    String type) {
        		super.visitTryCatchBlock(start, end, handler, type);
        		if (type != null) {
        			result.processTypeName(type);
        		}
        	}
        		
        	@Override
            public void visitTypeInsn(int opcode, String type) {
        		super.visitTypeInsn(opcode, type);
        		result.processTypeName(type);
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
		if (perf.isTiming()) {
			// Make sure we only do this once
			doWholeProgramAnalysis();
		}		
		graphDb.shutdown();
		if (perf.isTiming()) {
			perf.stopTiming("totalTimeInMillis");
		
			perf.setIntProperty("vertices", keyedMap.size());
			perf.setIntProperty("methods", numMethods);
			perf.setIntProperty("calls", numDistinctCalls);
			perf.setIntProperty("uses", numDistinctUses);
			perf.setIntProperty("classes", numClasses);
			perf.setIntProperty("fromSource", numFromSource);
			perf.store();
		}
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
	
	public static class Id implements OSerializableStream {		
		private static final long serialVersionUID = -2645241538925404431L;
		
		final String className, nodeName, project;
		
		Id(String proj, String clazz, String node) {
			project = proj;
			className = clazz;
			nodeName = node;
		}
		/* Including this causes an unmarshalling exception
		public String toString() {
		*/
		public String getLabel() {
			if (project == null) {
				return className+", "+nodeName;
			}
			return project+", "+className+", "+nodeName;
		}		
		
		public boolean equals(final Object o) {
			if (o instanceof Id) {
				final Id other = (Id) o;
				return className.equals(other.className) && 
					   nodeName.equals(other.nodeName) &&
					   (project == null ? other.project == null : project.equals(other.project));
			}
			return false;
		}
		
		public int hashCode() {
			return className.hashCode() + nodeName.hashCode() + 
					(project == null ? 0 : project.hashCode());
		}
		
		public Id fromStream(byte[] buf)
				throws OSerializationException {
			ByteArrayInputStream bytes = new ByteArrayInputStream(buf);
			try {
				ObjectInputStream in = new ObjectInputStream(bytes);
				return (Id) in.readObject();			
			} catch (Exception e) {
				throw new OSerializationException("Unable to deserialize Id", e);
			}
		}

		public byte[] toStream() throws OSerializationException {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try {
				ObjectOutputStream out = new ObjectOutputStream(bytes);
				out.writeObject(this);
				out.flush();
				return bytes.toByteArray();
			} catch (IOException e) {
				throw new OSerializationException("Unable to serialize: "+getLabel(), e);
			}
		}
	}	
	
	private Vertex findVertex(VertexType type, String clazzName, String nodeName, int access) {		
		final String project = getProject(clazzName);
		if (project == null) {
			if (!clazzName.startsWith("[")) {
				throw new NullPointerException();
			}
		}
		
		// Check if already created
		final Id id = new Id(project, clazzName, nodeName);
		//for(Vertex v : graphDb.getVertices(INDEX_KEY, id)) {
		final Vertex v = keyedMap.get(id);
		if (v != null) {		
			if (access != -1 && v.getProperty(ICON) == null) {
			    v.setProperty(ICON, encodeIconForDecl(type.encodeType(nodeName), access));
			    v.setProperty(FROM_SOURCE, fromSource);
			}
			return v; // return the first!
		}
		
		// Need to create
		Vertex node = graphDb.addVertex(null/*"class:"+type*/);
	    node.setProperty( INDEX_KEY, id.getLabel());
	    
	    if (clazzName == null) {
	    	throw new NullPointerException("Null clazzName");
	    }
	    node.setProperty(PARENT_CLASS, clazzName);
	    node.setProperty(NODE_NAME, nodeName);
	    node.setProperty(CLASS_LABEL, convertToClassLabel(clazzName));
	    node.setProperty(DECL_LABEL, convertToDeclLabel(type, nodeName));
	    if (access != -1) {
	    	node.setProperty(ICON, encodeIconForDecl(type.encodeType(nodeName), access));
	    	node.setProperty(FROM_SOURCE, fromSource);
	    }
	    keyedMap.put(id, node);
    	node.setProperty(INVOKE_DYNAMIC, Boolean.FALSE);
	    
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
	
	private void addReference(Vertex caller, RelTypes rel, Vertex callee, int line, int opcode) {
		final String label = rel.toString();
		// TODO is this really necessary?
		// Check if edge already exists
		for(Edge e : caller.getEdges(Direction.OUT, label)) {
			if (callee.equals(e.getVertex(Direction.IN))) {
				// Already created
				//TODO check opcode
				return;
			}
		}
		final Edge edge = graphDb.addEdge(null, caller, callee, label);
		switch (rel) {
		case CALLS:
			callee.setProperty(CALLED, Boolean.TRUE);			
			Integer num = callee.getProperty(NUM_TIMES_CALLED);
			callee.setProperty(NUM_TIMES_CALLED, num == null ? 1 : num+1);
			Edge e = graphDb.addEdge(null, caller, callee, rel.getRefHere());			
			numDistinctCalls++;
			break;
		case USES:
			if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) {
				edge.setProperty(READS_FIELD, Boolean.TRUE);
			}
			else if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
				edge.setProperty(WRITES_FIELD, Boolean.TRUE);
			}
			numDistinctUses++;
			break;
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

	public static SLStatus summarize(final File runDir, 
			                         final JavaClassPath<JavaProjectSet<JavaProject>> classes, 
			                         final SLProgressMonitor monitor) {
		final ClassSummarizer summarizer = new ClassSummarizer(runDir, classes);
		//ZipFile lastZip = null;
		try {
	
			summarizer.process(classes, monitor);
			summarizer.dump();

		} catch(IOException e) {
			return SLStatus.createErrorStatus(e);
		} finally {
			summarizer.close();
		}
		return SLStatus.OK_STATUS;
	}

	private void process(JavaClassPath<JavaProjectSet<JavaProject>> classes,
			SLProgressMonitor monitor) throws IOException {
		final List<IJavaFile> sources = new ArrayList<IJavaFile>();
		for(final Pair<String,String> key: classes.getMapKeys()) {			
			final IJavaFile info = classes.getMapping(key);
			if (info.getType() == IJavaFile.Type.CLASS_FOR_SRC) {
				sources.add(info);
			}
		}
		JavaClassPath.Processor p = new JavaClassPath.Processor() {
			public Iterable<String> process(IJavaFile info) throws IOException {
				return ClassSummarizer.this.summarize(info, true).dependencies;				
			}
		};
		classes.process(p, sources);
	}
	
	public void processAllClasses(JavaClassPath<JavaProjectSet<JavaProject>> classes,
			SLProgressMonitor monitor) throws IOException {
		int fromJars = 0;
		monitor.begin(classes.getMapKeys().size());
		
		//TODO Change this to summarize on demand! (not all)
		for(final Pair<String,String> key: classes.getMapKeys()) {
			//System.out.println("Got key: "+key);
			monitor.worked(1);
			
			final IJavaFile info = classes.getMapping(key);
			if (info.getType() == IJavaFile.Type.CLASS_FOR_SRC) {
				// TODO what about the jars?
				summarize(info, true);
			}
			else if (info.getType() != IJavaFile.Type.SOURCE) {
				if (key.first().startsWith(Config.JRE_NAME)) {
					// Skip classes only referenced from the JRE
					continue;
				}
				System.out.println("Summarizing "+key);
				fromJars++;							
				summarize(info, false);
				// TODO eliminate duplicates between projects?
			}
		}		
		System.out.println("Summarized from jars: "+fromJars);
	}
	
	private void doWholeProgramAnalysis() {
		// TODO Auto-generated method stub
		
	}
}