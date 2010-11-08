/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/sea/xml/SeaSnapshot.java,v 1.11 2008/06/23 17:27:49 chance Exp $*/
package edu.cmu.cs.fluid.sea.xml;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.xml.sax.Attributes;

import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.common.xml.Entities;
import com.surelogic.jsure.xml.*;

import static com.surelogic.jsure.xml.JSureXMLReader.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.*;
import edu.cmu.cs.fluid.sea.*;
import edu.cmu.cs.fluid.sea.drops.*;
import edu.cmu.cs.fluid.sea.drops.effects.*;
import edu.cmu.cs.fluid.sea.drops.threadroles.IThreadRoleDrop;

public class SeaSnapshot extends AbstractSeaXmlCreator {	
	public static final String SUFFIX = RegressionUtility.JSURE_SNAPSHOT_SUFFIX;
	public static final boolean useFullType = true;
	
	static final Map<String,Class<? extends Drop>> classMap = new HashMap<String, Class<? extends Drop>>();
	static {
		classMap.put("PackageDrop", PackageDrop.class);
		classMap.put("ResultDrop", ResultDrop.class);
		classMap.put("RegionEffectsPromiseDrop", RegionEffectsPromiseDrop.class);
	}
	private final Map<Drop,String> idMap = new HashMap<Drop,String>();
	
	public SeaSnapshot(File location) throws IOException {
		super(location);
	}
	
	private String computeId(Drop d) {
		String id = idMap.get(d);
		if (id == null) {
			int size = idMap.size();
			id = Integer.toString(size);
			idMap.put(d, id);
		}
		return id;
	}
	
	public void snapshot(String project, final Sea sea) throws IOException {
		reset();
		Entities.start(ROOT, b);
		addAttribute(UID_ATTR, UUID.randomUUID().toString());
		addAttribute(PROJECT_ATTR, project);
		b.append(">\n");
		flushBuffer(pw);

		for(Drop d : sea.getDrops()) {
			snapshotDrop(d);
		}
		pw.println("</"+ROOT+">\n");
		pw.close();
		//pw = null;
		//JSureXMLReader.readSnapshot(location, null);
	}
	
	private static void ensureClassMapping(Class<? extends Drop> cls) {
		if (classMap.containsKey(cls.getSimpleName())) {
			return;
		}
		classMap.put(cls.getSimpleName(), cls);
	}
	
	private static String[] packages = {
		"edu.cmu.cs.fluid.sea.drops.promises.",
		"edu.cmu.cs.fluid.sea.",
		"edu.cmu.cs.fluid.sea.drops.",
		"edu.cmu.cs.fluid.sea.drops.threadroles.",
		"edu.cmu.cs.fluid.sea.drops.modules.",
		"edu.cmu.cs.fluid.sea.drops.callgraph.",
		"edu.cmu.cs.fluid.sea.drops.layers.",
	};
	
	@SuppressWarnings("unchecked")
	static Class<?> findType(String type) {
		Class<?> thisType = classMap.get(type);
		if (thisType == null) {
			if (useFullType) {
				try {						
					thisType = Class.forName(type);					
				} catch(ClassNotFoundException e) {
					// Keep going
				}
			} else {
				for(String prefix : packages) {

					try {						
						thisType = Class.forName(prefix+type);
						break;
					} catch(ClassNotFoundException e) {
						// Keep going
					}
				}
			}
			if (thisType == null) {
				throw new IllegalStateException("Unknown type: "+type);
			} else {
				ensureClassMapping((Class<? extends Drop>) thisType);
			}
		}
		return thisType;
	}
	
	private static boolean preprocessRefs = false;
	
	public void snapshotDrop(Drop d) {
		if (preprocessRefs && idMap.containsKey(d)) {
			return;
		}
		
		if (!d.isValid()) {
			System.out.println("Ignoring invalid: "+d.getMessage());
			return; // ignore invalid drops
		}
		if (IThreadRoleDrop.class.isInstance(d)) {
			// Ignoring these for now
			return;
		}
		final String id = computeId(d);
		if (preprocessRefs) {
			d.preprocessRefs(this);
		}
		reset();
		
		final String name = d.getEntityName();	
		final String type = d.getClass().getSimpleName();
		ensureClassMapping(d.getClass());
		Entities.start(name, b);
		Entities.addAttribute(TYPE_ATTR, type, b);
		if (useFullType) {
			Entities.addAttribute(FULL_TYPE_ATTR, d.getClass().getName(), b);
		}
		Entities.addAttribute(ID_ATTR, id, b);
		d.snapshotAttrs(this);
		b.append(">\n");
		d.snapshotRefs(this);
		b.append("</"+name+">\n");		
		flushBuffer(pw);	
    }
	
	public void refDrop(String name, Drop d) {
		refDrop(name, d, null, null);
	}
	
	public void refDrop(String name, Drop d, String attr, String value) {
		b.append("  ");
		Entities.start(name, b);
		Entities.addAttribute(ID_ATTR, computeId(d), b);
		if (attr != null) {
			Entities.addAttribute(attr, value, b);
		}
		b.append("/>\n");
	}
	
	public void addSrcRef(IRNode context, ISrcRef srcRef) {
		addSrcRef(context, srcRef, "    ");
	}

	private void addSrcRef(IRNode context, ISrcRef s, String indent) {
		if (s == null) {
			return;
		}
		b.append(indent);
		Entities.start(SOURCE_REF, b);
		addLocation(s);		
		addAttribute(HASH_ATTR, SeaSummary.computeHash(context));
		addAttribute(CUNIT_ATTR, s.getCUName());
		addAttribute(PKG_ATTR, s.getPackage());
		b.append("/>\n");
	}
	
	public void addSupportingInfo(ISupportingInformation si) {
		b.append("    ");
		Entities.start(SUPPORTING_INFO, b);
		addAttribute(Drop.MESSAGE, si.getMessage());
		b.append(">\n");
		addSrcRef(si.getLocation(), si.getSrcRef(), "      ");		
		b.append("</"+SUPPORTING_INFO+">\n");
	}
	/*
	private void outputPromiseDropAttrs(StringBuilder b, PromiseDrop d) {
		d.isAssumed();
		d.isCheckedByAnalysis();
		d.isFromSrc();
	}
	*/
	public static Collection<Info> loadSnapshot(File location) throws Exception {
		XMLListener l = new XMLListener();
		new JSureXMLReader(l).read(location);
		return l.getEntities();
	}
	
	static class XMLListener extends AbstractXMLResultListener {
		private final List<Info> entities = new ArrayList<Info>();
		
		Collection<Info> getEntities() {
			List<Info> rv = new ArrayList<Info>();
			for(Info i : entities) {
				if (i != null) {
					rv.add(i);
				}
			}
			return rv;
		}
		
		private void add(int id, Info info) {
			if (id >= entities.size()) {
				// Need to expand the array
				while (id > entities.size()) {
					entities.add(null);
				}
				entities.add(info);
			} else {
				Info old = entities.set(id, info);
				if (old != null) {
					throw new IllegalStateException("Replacing id: "+id);
				}
			}
		}
		
		@Override
		public Entity makeEntity(String name, Attributes a) {
			final String type = Entity.getValue(a, useFullType ? FULL_TYPE_ATTR : TYPE_ATTR);
			if (type != null) {
				final Class<?> thisType = findType(type);
				if (thisType != null && ProofDrop.class.isAssignableFrom(thisType)) {			
					return new ProofInfo(name, a);
				}
			}
			return new Info(name, a);			
		}
		
		@Override
		protected boolean define(final int id, Entity e) {
			if (!e.getName().endsWith("drop")) {
				System.out.println("Got "+e.getName());
			}
			final Info i = (Info) e;
			add(id, i);
			i.finishInit();
			return true;
		}
		
		@Override
		protected void handleRef(String fromLabel, int fromId, Entity to) {
			final String refType = to.getName();
			final Info fromE = entities.get(fromId);
			final int toId = Integer.valueOf(to.getId());
			final Info toE = entities.get(toId); // The entity above is really the ref info
			if (Drop.DEPONENT.equals(refType)) {
				fromE.addDeponent(toE);
				toE.addDependent(fromE);
			} else if (IRReferenceDrop.PROPOSED_PROMISE.equals(refType)) {
				fromE.addProposal(toE);
			} else if (fromE instanceof ProofInfo) {
				final ProofInfo fromPI = (ProofInfo) fromE;
				final ProofInfo toPI = (ProofInfo) toE;
				
				if (PromiseDrop.CHECKED_BY_RESULTS.equals(refType)) { 
					fromPI.addCheckedByResult(toPI);					
			    } else if (ResultDrop.CHECKED_PROMISE.equals(refType)) {
					fromPI.addCheckedPromise(toPI);
				} else if (ResultDrop.TRUSTED_PROMISE.equals(refType)) {
					fromPI.addTrustedPromise(toPI);
				} else if (ResultDrop.OR_TRUSTED_PROMISE.equals(refType)) {
					final String label = to.getAttribute(ResultDrop.OR_LABEL);
					fromPI.addOrTrustedPromise(label, toPI);
				}
			} else {
				throw new IllegalStateException("NOT Handled: " + refType + " ref from " + 
						                        fromLabel + " to " + to.getId());
			}
		}
	}
	
	public static class Info extends Entity implements IDropInfo {
		final List<Info> dependents; 
		final List<Info> deponents;
		final List<Info> proposals;
		Category category;
		ISrcRef ref;
		List<ISupportingInformation> supportingInfos;

		void addProposal(Info info) {
			proposals.add(info);
		}
		
		void addDeponent(Info info) {
			deponents.add(info);
		}

		void addDependent(Info info) {
			dependents.add(info);
		}
		
		Info(String name, Attributes a) {
			super(name, a);
			if (name.endsWith("drop")) {
				dependents = new ArrayList<Info>();
				deponents  = new ArrayList<Info>();
				proposals  = new ArrayList<Info>();
			} else {
				dependents = Collections.emptyList();
				deponents = Collections.emptyList();
				proposals = Collections.emptyList();
			}
			/*
			final String name = e.getName();
			final boolean warning;
			final String aType;

			if (isResultDrop || PROMISE_DROP.equals(name)) {
				final String consistent = e.getAttribute(PROVED_ATTR);			
				warning = !"true".equals(consistent);

				if (isResultDrop) {
					aType = e.getAttribute(RESULT_ATTR);
				} else {
					final String type = e.getAttribute(TYPE_ATTR);
					aType = "MethodControlFlow".equals(type) ? "UniquenessAssurance"
							: type;
				}
			} else if (IR_DROP.equals(name)) {
				final String type = e.getAttribute(TYPE_ATTR);
				warning = "WarningDrop".equals(type);

				final String result = e.getAttribute(RESULT_ATTR);
				aType = result != null ? result : "JSure";
			} else {
				return false;
			}
			if (aType.startsWith("Color")) {
				return false;
			}
			if (createSourceLocation(builder.primarySourceLocation(), e.getSource())) {
				final String msg = e.getAttribute(MESSAGE_ATTR);
				builder.message(msg);
				if (warning) {
					builder.severity(Severity.ERROR).priority(Priority.HIGH);
				} else {
					builder.severity(Severity.INFO).priority(Priority.LOW);
				}			
				builder.findingType("JSure", "1.1", aType);
				builder.scanNumber(id);
				builder.assurance(assuranceType);
				// e.getAttribute(CATEGORY_ATTR));
				builder.build();
				return true;
			}			
			*/
			category = Category.getInstance(getAttribute(CATEGORY_ATTR));			
		}
		
		void finishInit() {
			if (getSource() != null) {
				ref = makeSrcRef(getSource());
			} else {
				ref = null;
			}
			if (!getInfos().isEmpty()) {
				supportingInfos = new ArrayList<ISupportingInformation>();
				for(com.surelogic.jsure.xml.Info i : getInfos()) {
					supportingInfos.add(makeSupportingInfo(i));
				}
			} else {
				supportingInfos = Collections.emptyList();
			}
		}
		
		private ISupportingInformation makeSupportingInfo(final com.surelogic.jsure.xml.Info i) {
			return new ISupportingInformation() {
				final ISrcRef ref = makeSrcRef(i.source);
				
				public IRNode getLocation() {
					return null;
				}
				public String getMessage() {
					return i.message;
				}
				public ISrcRef getSrcRef() {
					return ref;
				}
				public boolean sameAs(IRNode link, int num, Object[] args) {
					throw new UnsupportedOperationException();
				}
				public boolean sameAs(IRNode link, String message) {
					throw new UnsupportedOperationException();
				}
				
			};
		}

		private static ISrcRef makeSrcRef(final SourceRef ref) {
			final int line = Integer.valueOf(ref.getLine());
			return new AbstractSrcRef() {
				@Override
				public ISrcRef createSrcRef(int offset) {
					return this;
				}

				public String getCUName() {
					return ref.getAttribute(CUNIT_ATTR);
				}
				@Override
				public Object getEnclosingFile() {
					return ref.getAttribute(FILE_ATTR);
				}
				@Override
				public String getRelativePath() {
					return ref.getAttribute(PATH_ATTR);
				}
				@Override
				public URI getEnclosingURI() {
					String uri = ref.getAttribute(URI_ATTR);
					if (uri != null) {
						try {
							return new URI(uri);
						} catch (URISyntaxException e) {
							System.out.println("Couldn't parse as URI: "+uri);
						}
					}
					return null;
				}
				@Override
				public int getOffset() {
					String offset = ref.getAttribute(OFFSET_ATTR);
					if (offset == null) {
						throw new UnsupportedOperationException();
					} else {
						return Integer.valueOf(offset);
					}
				}
				
				public Long getHash() {
					String hash = ref.getAttribute(HASH_ATTR);
					if (hash == null) {
						throw new UnsupportedOperationException();
					} else {
						return Long.valueOf(hash);
					}
				}

				@Override
				public int getLineNumber() {
					return line;
				}

				public String getPackage() {
					return ref.getAttribute(PKG_ATTR);
				}				
			};
		}
		
		/*
		private boolean createSourceLocation(SourceLocationBuilder loc, SourceRef s) {
			if (s != null) {
				final String cu = s.getAttribute(CUNIT_ATTR);
				loc.compilation(cu);
				if (cu.endsWith(".java")) {
		       loc.className(cu.substring(0, cu.length() - 5));
				} else {
				  loc.className(cu);
				}
				loc.packageName(s.getAttribute(PKG_ATTR));

				final int line = Integer.parseInt(s.getLine());
				loc.lineOfCode(line);
				loc.endLine(line);
				loc.hash(Long.decode(s.getAttribute(HASH_ATTR)));
				loc.identifier("unknown");
				loc.type(IdentifierType.CLASS);
				loc.build();
				return true;
			}
			return false;
		}
		*/
		
		public int count() {
			String value = getAttribute(PleaseCount.COUNT);
			if (value == null) {
				return 0;
			}
			return Integer.valueOf(value);
		}
		
		public boolean requestTopLevel() {
			return "true".equals(getAttribute(MaybeTopLevel.REQUEST_TOP_LEVEL));
		}

		public <T> T getAdapter(Class<T> type) {
			return null;
		}

		public boolean isValid() {
			return true; 
		}

		public void setCategory(Category c) {
			category = c;
		}		
		
		public Category getCategory() {
			return category;
		}

		public String getMessage() {
			return getAttribute(MESSAGE_ATTR);
		}

		public ISrcRef getSrcRef() {
			return ref;		
		}

		public String getType() {
			return getAttribute(TYPE_ATTR);
		}

		public boolean isInstance(Class<?> type) {
			final String thisTypeName = getAttribute(useFullType ? FULL_TYPE_ATTR : TYPE_ATTR);
			final Class<?> thisType = findType(thisTypeName);
			return type.isAssignableFrom(thisType);		
		}

		public boolean hasMatchingDeponents(IDropPredicate p) {
			for(Info i : deponents) {
				if (p.match(i)) {
					return true;
				}
			}
			return false;
		}

		public void addMatchingDependentsTo(Set<IDropInfo> s, IDropPredicate p) {
			for(Info i : dependents) {
				if (p.match(i)) {
					s.add(i);
				}
			}
		}

		public String getJavaAnnotation() {
			return getAttribute(ProposedPromiseDrop.JAVA_ANNOTATION);
		}

		public Collection<? extends IDropInfo> getProposals() {
			return proposals;
		}

		public Collection<ISupportingInformation> getSupportingInformation() {
			return supportingInfos;
		}
	}
	
	static class ProofInfo extends Info implements IProofDropInfo {
		/**
		 * Only for PromiseDrops
		 */
		final List<ProofInfo> checkedByResults;
		/**
		 * Only for ResultDrops
		 */
		final List<ProofInfo> checkedPromises;
		final List<ProofInfo> trustedPromises;
		final MultiMap<String,ProofInfo> orTrustedPromises;
		
		void addCheckedByResult(ProofInfo info) {
			if (PromiseDrop.useCheckedByResults) {
				checkedByResults.add(info);
			}
		}
		
		void addCheckedPromise(ProofInfo info) {
			if (!PromiseDrop.useCheckedByResults) {
				info.checkedByResults.add(this);
			}
			checkedPromises.add(info);
		}
		
		void addTrustedPromise(ProofInfo info) {
			trustedPromises.add(info);
		}
		
		void addOrTrustedPromise(String label, ProofInfo info) {
			orTrustedPromises.put(label, info);
		}
		
		ProofInfo(String name, Attributes a) {
			super(name, a);
			
			if (isInstance(ResultDrop.class)) {
				checkedPromises = new ArrayList<ProofInfo>();
				trustedPromises = new ArrayList<ProofInfo>();
				orTrustedPromises = new MultiHashMap<String, ProofInfo>();
				checkedByResults = Collections.emptyList();
			} else {
				checkedPromises = Collections.emptyList();
				trustedPromises = Collections.emptyList();
				orTrustedPromises = null;
				if (isInstance(PromiseDrop.class)) {
					checkedByResults = new ArrayList<ProofInfo>();
				} else {
					checkedByResults = Collections.emptyList();
				}
			}
		}


		public Collection<? extends IProofDropInfo> getChecks() {
			return checkedPromises;
		}

		public Collection<? extends IProofDropInfo> getTrusts() {
			return trustedPromises;
		}
		
		public boolean isConsistent() {
			return "true".equals(getAttribute(ResultDrop.CONSISTENT));
		}

		public boolean proofUsesRedDot() {
			return "true".equals(getAttribute(USES_RED_DOT_ATTR));
			
		}

		public boolean provedConsistent() {
			return "true".equals(getAttribute(PROVED_ATTR));
		}

		public Collection<? extends IProofDropInfo> getCheckedBy() {
			return checkedByResults;
		}

		public Collection<? extends IProofDropInfo> getTrustsComplete() {
			Collection<ProofInfo> rv = new HashSet<ProofInfo>(trustedPromises);
			rv.addAll(orTrustedPromises.values());
			return rv;
		}

		public Collection<String> get_or_TrustLabelSet() {
			return orTrustedPromises.keySet();
		}

		public Collection<? extends IProofDropInfo> get_or_Trusts(String key) {
			return orTrustedPromises.get(key);
		}

		public boolean hasOrLogic() {
			return orTrustedPromises != null && !orTrustedPromises.isEmpty();
		}
		
		public boolean get_or_proofUsesRedDot() {
			return "true".equals(getAttribute(ResultDrop.OR_USES_RED_DOT));
		}

		public boolean get_or_provedConsistent() {
			return "true".equals(getAttribute(ResultDrop.OR_PROVED));
		}

		public boolean isVouched() {
			return "true".equals(getAttribute(ResultDrop.VOUCHED));
		}
		
		public boolean isAssumed() {
			return "true".equals(getAttribute(PromiseDrop.ASSUMED));
		}

		public boolean isCheckedByAnalysis() {
			return "true".equals(getAttribute(PromiseDrop.CHECKED_BY_ANALYSIS));
		}

		public boolean isIntendedToBeCheckedByAnalysis() {
			return "true".equals(getAttribute(PromiseDrop.TO_BE_CHECKED_BY_ANALYSIS));
		}

		public boolean isFromSrc() {
			return "true".equals(getAttribute(PromiseDrop.FROM_SRC));
		}
		
		public boolean isVirtual() {
			return "true".equals(getAttribute(PromiseDrop.VIRTUAL));
		}
	}
}
