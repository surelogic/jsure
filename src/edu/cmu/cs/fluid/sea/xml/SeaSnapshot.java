/*$Header: /cvs/fluid/fluid/src/edu/cmu/cs/fluid/sea/xml/SeaSnapshot.java,v 1.11 2008/06/23 17:27:49 chance Exp $*/
package edu.cmu.cs.fluid.sea.xml;

import java.io.*;
import java.util.*;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;
import org.xml.sax.Attributes;

import com.surelogic.common.regression.RegressionUtility;
import com.surelogic.common.xml.Entities;
import com.surelogic.jsure.xml.*;

import static com.surelogic.jsure.xml.JSureXMLReader.*;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.*;

public class SeaSnapshot extends AbstractSeaXmlCreator {	
	public static final String SUFFIX = RegressionUtility.JSURE_SNAPSHOT_SUFFIX;
	
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

	public void snapshotDrop(Drop d) {
		if (idMap.containsKey(d)) {
			return;
		}
		final String id = computeId(d);
		d.preprocessRefs(this);
		reset();
		
		final String name = d.getEntityName();	
		final String type = d.getClass().getSimpleName();
		Entities.start(name, b);
		Entities.addAttribute(TYPE_ATTR, type, b);
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
		addAttribute(HASH_ATTR, s.getHash());
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
			return new Info(name, a);
		}
		
		@Override
		protected boolean define(final int id, Entity e) {
			add(id, (Info) e);
			return true;
		}
		
		@Override
		protected void handleRef(String fromLabel, int fromId, Entity to) {
			/*
			if ("deponent".equals(to.getName())) {
				return; // skip these
			}
			*/
			final String refType = to.getName();
			final Info fromE = entities.get(fromId);
			final int toId = Integer.valueOf(to.getId());
			final Info toE = entities.get(toId); // The entity above is really the ref info
			if (Drop.DEPONENT.equals(refType)) {
				fromE.addDeponent(toE);
				toE.addDependent(fromE);
			} else if (ResultDrop.CHECKED_PROMISE.equals(refType)) {
				fromE.addCheckedPromise(toE);
			} else if (ResultDrop.TRUSTED_PROMISE.equals(refType)) {
				fromE.addTrustedPromise(toE);
			} else if (ResultDrop.OR_TRUSTED_PROMISE.equals(refType)) {
				final String label = to.getAttribute(ResultDrop.OR_LABEL);
				fromE.addOrTrustedPromise(label, toE);
			} else {
				System.out.println("NOT Handled: " + refType + " ref from " + fromLabel + " to " + to.getId());
			}
		}
	}
	
	public static class Info extends Entity implements IDropInfo {
		final List<Info> dependents = new ArrayList<Info>();
		final List<Info> deponents  = new ArrayList<Info>();
		final List<Info> checkedPromises;
		final List<Info> trustedPromises;
		final MultiMap<String,Info> orTrustedPromises;
		
		void addDeponent(Info info) {
			deponents.add(info);
		}

		void addDependent(Info info) {
			dependents.add(info);
		}
		
		void addCheckedPromise(Info info) {
			checkedPromises.add(info);
		}
		
		void addTrustedPromise(Info info) {
			trustedPromises.add(info);
		}
		
		void addOrTrustedPromise(String label, Info info) {
			orTrustedPromises.put(label, info);
		}
		
		Info(String name, Attributes a) {
			super(name, a);
			
			final boolean isResultDrop = RESULT_DROP.equals(name);			
			if (isResultDrop || PROMISE_DROP.equals(name)) {
				checkedPromises = new ArrayList<Info>();
				trustedPromises = new ArrayList<Info>();
				orTrustedPromises = new MultiHashMap<String, Info>();
			} else {
				checkedPromises = Collections.emptyList();
				trustedPromises = Collections.emptyList();
				orTrustedPromises = null;
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
			// TODO Auto-generated method stub
			return 0;
		}



		public <T> T getAdapter(Class<T> type) {
			// TODO Auto-generated method stub
			return null;
		}

		public Category getCategory() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getMessage() {
			// TODO Auto-generated method stub
			return null;
		}

		public ISrcRef getSrcRef() {
			// TODO Auto-generated method stub
			return null;
		}

		public String getType() {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean isConsistent() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isInstance(Class<?> type) {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean proofUsesRedDot() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean provedConsistent() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean requestTopLevel() {
			// TODO Auto-generated method stub
			return false;
		}		
	}
}
