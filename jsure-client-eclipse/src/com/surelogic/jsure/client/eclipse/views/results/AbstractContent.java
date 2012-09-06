package com.surelogic.jsure.client.eclipse.views.results;

import static com.surelogic.common.jsure.xml.CoE_Constants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.surelogic.common.CommonImages;
import com.surelogic.common.core.EclipseUtility;
import com.surelogic.jsure.client.eclipse.views.source.HistoricalSourceView;
import com.surelogic.tree.diff.Diff;
import com.surelogic.tree.diff.IDiffNode;

import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.sea.*;

/**
 * Class used to represent derived viewer nodes.
 */
public abstract class AbstractContent<T extends IDropInfo, T2 extends AbstractContent<T, T2>> 
implements Cloneable, IDiffNode<T2> {
	/**
	 * Status for diffing
	 */
	private Status f_status = Status.SAME;

	private int f_numIssues = -1;

	/**
	 * The drop referenced, or {@code null}.
	 */
	private final T f_referencedDrop;
	
	/**
	 * This items children for the viewer, meant to be accessed by
	 * {@link #getChildren()}.
	 * 
	 * @see #getChildren()
	 */
	private Collection<T2> f_children;

	/**
	 * The message to display in the viewer, meant to be accessed by
	 * {@link #getMessage()}.
	 * 
	 * @see #getMessage()
	 */
	private final String f_message;

	/**
	 * Cache of getMessage();
	 */
	private String f_getMessage;
	
	private T2 parent = null;
	
	private String f_baseImageName;

	private int f_imageFlags = 0;

	/**
	 * The fAST node that this item references, or null, if the associated drop
	 * defines the reference location.
	 */
	private ISrcRef f_sourceRef = null;

	/**
	 * <code>true</code> if this content is from an inference (or InfoDrop),
	 * <code>false</code> otherwise.
	 */
	boolean f_isInfo = false;

	boolean f_isInfoDecorated = false;

	/**
	 * <code>true</code> if this content is from an inference (or InfoDrop) that
	 * is a warning, <code>false</code> otherwise.
	 */
	boolean f_isInfoWarning = false;

	boolean f_isInfoWarningDecorate = false;

	boolean f_donePropagatingWarningDecorators = false;

	/**
	 * <code>true</code> if this content is from an promise warning drop (or
	 * PromiseWarningDrop), <code>false</code> otherwise.
	 */
	boolean f_isPromiseWarning = false;

	/**
	 * A reference to the original node. Non-null only if it's a backedge
	 */
	T2 cloneOf = null;

	@SuppressWarnings("unchecked")
	AbstractContent(String msg, Collection<T2> content, T drop) {
		f_message = msg;
		f_children = content;
		for(T2 c : content) {
			c.setParent((T2) this);
		}		
		f_referencedDrop = drop;
		if (drop != null) {
			if (drop instanceof IRReferenceDrop) {	
				IRReferenceDrop ird = (IRReferenceDrop) drop;
				f_sourceRef = ird.getSrcRef();
			} else {
				f_sourceRef = drop.getSrcRef();
			}
		}		
	}
	
	AbstractContent(String msg, ISrcRef ref) {
		f_message = msg;
		f_children = Collections.emptyList();
		f_referencedDrop = null;
		f_sourceRef = ref;	
	}

	/*
	AbstractContent(String msg, Collection<T2> content) {
		this(msg, content, null);
	}

	AbstractContent(String msg) {
		this(msg, new HashSet<T2>(), null);
	}

	AbstractContent(String msg, T drop) {
		this(msg, new HashSet<T2>(), drop);
	}

	AbstractContent(String msg, IRNode location, T drop) {
		this(msg, new HashSet<T2>(), drop);
		if (location != null) {
			f_sourceRef = JavaNode.getSrcRef(location);
		}
	}

	AbstractContent(String msg, IRNode location) {
		this(msg, location, null);
	}
    */

	@SuppressWarnings("unchecked")
	T2 cloneAsLeaf() {
		T2 clone = shallowCopy();
		if (clone != null) {
			clone.f_status = Status.BACKEDGE;
			clone.cloneOf = (T2) this;
		}
		return clone;
	}

	public void setCount(int count) {
		f_numIssues = count;
	}

	public int freezeCount() {
		return f_numIssues = f_children.size();
	}

	public void freezeChildrenCount() {
		int size = 0;
		for (T2 c : f_children) {
			size += c.freezeCount();
		}
		f_numIssues = size;
	}

	public int recomputeCounts() {
		if (f_numIssues < 0) {
			// No counts previously recorded here
			for (T2 c : f_children) {
				c.recomputeCounts();
			}
			return -1;
		}
		boolean counted = false;
		int size = 0;
		for (T2 c : f_children) {
			int count = c.recomputeCounts();
			if (count > 0) {
				size += count;
				counted = true;
			}
		}
		if (counted) {
			return f_numIssues = size;
		}
		return freezeCount();
	}

	public ISrcRef getSrcRef() {
		/*
		 * if (referencedDrop instanceof IRReferenceDrop) { return
		 * ((IRReferenceDrop) referencedDrop).getSrcRef(); } return
		 * (referencedLocation != null ? JavaNode .getSrcRef(referencedLocation)
		 * : null);
		 */
		return f_sourceRef;
	}

	public String getMessage() {
		if (f_getMessage != null) {
			return f_getMessage;
		}
		String result = f_message;
		final ISrcRef ref = getSrcRef();
		if (ref != null) {
			String name = "?";

			Object f = ref.getEnclosingFile();
			if (f instanceof IResource) {
				IResource file = (IResource) f;
				name = file.getName();
			} else if (f instanceof String) {
				name = (String) f;
				int lastSlash = name.lastIndexOf('/');
				if (lastSlash >= 0) {
					name = name.substring(lastSlash + 1);
				}
			} else if (f != null) {
				name = f.toString();
			}

			final boolean referencesAResultDrop = getDropInfo() != null && getDropInfo().instanceOf(ResultDrop.class);
			if (ref.getLineNumber() > 0) {
				if (referencesAResultDrop) {
					result += " at line " + ref.getLineNumber();
				} else {
					result += " at " + name + " line " + ref.getLineNumber();
				}
			} else if (!name.equals("?")) {
				if (!referencesAResultDrop) {
					result += " at " + name;
				}
			}
		} else if (f_numIssues > 0) {
			if (f_message.contains("s)")) {
				result = f_numIssues + " " + result;
			} else {
				result += " (" + f_numIssues
						+ (f_numIssues > 1 ? " issues)" : " issue)");
			}
		}
		if (f_status != Status.SAME) {
			result += " -- " + f_status;
		}
		f_getMessage = result;
		return result;
	}

	public void setBaseImageName(String name) {
		if (name == null)
			throw new IllegalArgumentException("the base image can't be null");
		f_baseImageName = name;
	}

	public String getBaseImageName() {
		return f_baseImageName;
	}

	public void setImageFlags(int flags) {
		f_imageFlags = flags;
	}

	public int getImageFlags() {
		return f_imageFlags;
	}

	// For exporting the node (see XMLReport)
	public int getFlags() {
		int flagInt = getImageFlags();
		if (f_isInfoWarningDecorate) {
			flagInt |= INFO_WARNING;
		} else if (f_isInfoDecorated) {
			if (!CommonImages.IMG_INFO.equals(getBaseImageName()))
				flagInt |= INFO;
		}
		return flagInt;
	}

	// For exporting the node (see XMLReport)
	public boolean isRedDot() {
		if ((getImageFlags() & REDDOT) > 0) {
			return true;
		}
		return false;
	}

	// For exporting the node (see XMLReport)
	//
	// Suppress warnings about accessing the CVS information
	public Map<String, String> getLocationAttributes(
			final Map<String, String> attrs, final boolean includeCVS) {
		final ISrcRef srcRef = getSrcRef();
		if (srcRef != null && srcRef.getEnclosingFile() instanceof IResource) {
			final IResource srcFile = (IResource) srcRef.getEnclosingFile();
			updateAttrs(attrs, includeCVS, srcRef, srcFile);
			return attrs;		
		} else if (srcRef != null && srcRef.getEnclosingFile() instanceof String) {
			String s = (String) srcRef.getEnclosingFile();
			if (s.indexOf('/') < 0) {
				return null; // probably not a file
			}
			s = HistoricalSourceView.tryToMapPath(s);
			IFile file = EclipseUtility.resolveIFile(s);
			if (file != null) {
				updateAttrs(attrs, includeCVS, srcRef, file);
			} else {
				return null;
			}
			return attrs;
		} else {
			return null;
		}
	}

	@SuppressWarnings("restriction")
	private void updateAttrs(final Map<String, String> attrs,
			final boolean includeCVS, final ISrcRef srcRef,
			final IResource srcFile) {
		attrs.put(ATTR_SOURCE, srcFile.getFullPath() + ".html");
		attrs.put(ATTR_LINE_NUM, Integer.toString(srcRef.getLineNumber()));
		if (includeCVS) {
			final org.eclipse.team.internal.ccvs.core.ICVSFile cvsFile = 
					org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot
					.getCVSFileFor((IFile) srcRef.getEnclosingFile());
			try {
				attrs.put(ATTR_CVSSOURCE, cvsFile
						.getRepositoryRelativePath());
				org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo fileInfo = cvsFile.getSyncInfo();
				if (fileInfo != null && fileInfo.getRevision() != null) {
					attrs.put(ATTR_CVSREVISION, fileInfo.getRevision());
				}
			} catch (CoreException e) {
				// do nothing
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends IDropInfo, T2 extends AbstractContent<T, T2>> 
	Object[] filterNonInfo(Object[] items) {
		Set<T2> result = new HashSet<T2>();
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof AbstractContent) {
				T2 item = (T2) items[i];
				if (!item.f_isInfo) {
					result.add(item);
				}
			} else {
				System.out
						.println("FAILURE: filterNonI found non-Content item");
			}
		}
		return result.toArray();
	}

	public Object[] getNonInfoChildren() {
		return AbstractContent.<T,T2>filterNonInfo(f_children.toArray());
	}

	public Object[] getChildren() {
		return f_children.toArray();
	}

	public Category getCategory() {
		if (getDropInfo() != null && getDropInfo().instanceOf(IRReferenceDrop.class)) {
			return getDropInfo().getCategory();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public void resetChildren(Collection<T2> c) {
		if (c == null) {
			throw new IllegalArgumentException("New children is null");
		}
		f_children = c;
		for(T2 cc : c) {
			cc.setParent((T2) this);
		}
	}

	@SuppressWarnings("unchecked")
	public void addChild(T2 child) {
		if (f_children.size() == 0) {
			f_children = new ArrayList<T2>(1);
		}
		f_children.add(child);
		child.setParent((T2) this);
	}

	public int numChildren() {
		return f_children.size();
	}

	public Collection<T2> children() {
		return f_children;
	}

	public static <T extends IDropInfo, T2 extends AbstractContent<T, T2>> 
	Collection<T2> diffChildren(Collection<T2> last,
			Collection<T2> now) {
		Collection<T2> diffs = Diff.diff(last, now, false);
		for (T2 c : diffs) {
			c.recomputeCounts();
		}
		return diffs;
	}

	public Collection<T2> getChildrenAsCollection() {
		return f_children;
	}

	@SuppressWarnings("unchecked")
	public Collection<T2> setChildren(Collection<T2> c) {
		try {
			return f_children;
		} finally {
			f_children = c;
			for(T2 cc : c) {
				cc.setParent((T2) this);
			}
		}
	}

	public Status getStatus() {
		return f_status;
	}

	public Status setStatus(Status s) {
		try {
			return f_status;
		} finally {
			f_status = s;
		}
	}

	@Override
	public String toString() {
		if (f_sourceRef != null) {
			return f_message + " at " + f_sourceRef.getEnclosingFile() + ":"
					+ f_sourceRef.getLineNumber();
		}
		return f_message;
	}

	public T getDropInfo() {
		return f_referencedDrop;
	}
	
	public Object identity() {
		return id;
	}

	private class Identity {
		@SuppressWarnings("unchecked")
		private T2 content() {
			return (T2) AbstractContent.this;
		}

		@Override
		public int hashCode() {
			/*
			 * if (referencedDrop != null) { return
			 * referencedDrop.getMessage().hashCode(); }
			 */
			if (f_message == null) {
				return 0;
			}
			return f_message.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			//if (o instanceof Identity) {
			if (Identity.class.isInstance(o)) {
				@SuppressWarnings("unchecked")
				T2 c = ((Identity) o).content();
				if (getDropInfo() == c.getDropInfo()) {
					return true;
				}
				if (f_message.equals(c.f_message)) {
					return f_sourceRef == c.f_sourceRef
							|| (f_sourceRef != null && c.f_sourceRef != null && f_sourceRef
									.getEnclosingFile().equals(
											c.f_sourceRef.getEnclosingFile()));
				}
			}
			return false;
		}
	}

	private final Identity id = new Identity();

	public final Comparator<Identity> getComparator() {
		return new Comparator<Identity>() {

			public int compare(Identity o1, Identity o2) {
				T2 c1 = o1.content();
				T2 c2 = o2.content();
				if (c1.getDropInfo() == c2.getDropInfo()) {
					return 0;
				}
				if (c1.f_message.equals(c2.f_message)) {
					if (c1.f_sourceRef == c2.f_sourceRef) {
						return 0;
					}
					if (c1.f_sourceRef != null
							&& c2.f_sourceRef != null
							&& c1.f_sourceRef.getEnclosingFile().equals(
									c2.f_sourceRef.getEnclosingFile())) {
						String cmt1 = c1.f_sourceRef.getComment();
						String cmt2 = c2.f_sourceRef.getComment();
						if (cmt1 == null) {
							// near match, or completely off
							return (cmt2 == null) ? 1 : Integer.MAX_VALUE;
						}
						if (cmt2 == null) {
							// Completely off since cmt1 != null
							return Integer.MAX_VALUE;
						}
						return c1.f_sourceRef.getOffset()
								- c2.f_sourceRef.getOffset();
					}
				}
				return Integer.MAX_VALUE;
			}
		};
	}

	public boolean isShallowMatch(T2 n) {
		return this.f_baseImageName.equals(n.f_baseImageName)
				&& this.f_imageFlags == n.f_imageFlags
				&& this.f_isInfo == n.f_isInfo
				&& this.f_isInfoDecorated == n.f_isInfoDecorated
				&& this.f_isInfoWarning == n.f_isInfoWarning
				&& this.f_isInfoWarningDecorate == n.f_isInfoWarningDecorate
				&& this.f_isPromiseWarning == n.f_isPromiseWarning;
	}

	@SuppressWarnings("unchecked")
	public T2 shallowCopy() {
		T2 clone;
		try {
			clone = (T2) clone();
			clone.f_children = Collections.emptySet();
			clone.f_getMessage = null; // Invalidate cache
			return clone;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public T2 deepCopy() {
		T2 copy = shallowCopy();
		copy.f_children = new ArrayList<T2>();
		for (T2 c : f_children) {
			final T2 copyC = c.deepCopy();
			copy.f_children.add(copyC);
			copyC.setParent(copy);
		}
		return copy;
	}
	
	public T2 getParent() {
		return parent;
	}
	
	private void setParent(T2 p) {
		parent = p;
	}
}