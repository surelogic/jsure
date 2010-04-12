package edu.cmu.cs.fluid.dcf.views.coe;

import static com.surelogic.xml.results.coe.CoE_Constants.ATTR_CVSREVISION;
import static com.surelogic.xml.results.coe.CoE_Constants.ATTR_CVSSOURCE;
import static com.surelogic.xml.results.coe.CoE_Constants.ATTR_LINE_NUM;
import static com.surelogic.xml.results.coe.CoE_Constants.ATTR_SOURCE;
import static com.surelogic.xml.results.coe.CoE_Constants.INFO;
import static com.surelogic.xml.results.coe.CoE_Constants.INFO_WARNING;
import static com.surelogic.xml.results.coe.CoE_Constants.REDDOT;

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
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;

import com.surelogic.common.CommonImages;
import com.surelogic.common.eclipse.EclipseUtility;
import com.surelogic.jsure.client.eclipse.views.JSureHistoricalSourceView;
import com.surelogic.tree.diff.Diff;
import com.surelogic.tree.diff.IDiffNode;

import edu.cmu.cs.fluid.ir.IRNode;
import edu.cmu.cs.fluid.java.ISrcRef;
import edu.cmu.cs.fluid.java.JavaNode;
import edu.cmu.cs.fluid.sea.Category;
import edu.cmu.cs.fluid.sea.Drop;
import edu.cmu.cs.fluid.sea.IRReferenceDrop;

/**
 * Class used to represent derived viewer nodes.
 */
public final class Content implements Cloneable, IDiffNode<Content> {
	/**
	 * Status for diffing
	 */
	private Status f_status = Status.SAME;

	private int f_numIssues = -1;

	/**
	 * The drop referenced, or {@code null}.
	 */
	final Drop f_referencedDrop;

	/**
	 * This items children for the viewer, meant to be accessed by
	 * {@link #getChildren()}.
	 * 
	 * @see #getChildren()
	 */
	private Collection<Content> f_children;

	/**
	 * The message to display in the viewer, meant to be accessed by
	 * {@link #getMessage()}.
	 * 
	 * @see #getMessage()
	 */
	private final String f_message;

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
	Content cloneOf = null;

	Content(String msg, Collection<Content> content, Drop drop) {
		f_message = msg;
		f_children = content;
		f_referencedDrop = drop;
		if (drop instanceof IRReferenceDrop) {
			f_sourceRef = ((IRReferenceDrop) f_referencedDrop).getSrcRef();
		}
	}

	Content(String msg, Collection<Content> content) {
		this(msg, content, null);
	}

	Content(String msg) {
		this(msg, new HashSet<Content>(), null);
	}

	Content(String msg, Drop drop) {
		this(msg, new HashSet<Content>(), drop);
	}

	Content(String msg, IRNode location, Drop drop) {
		this(msg, new HashSet<Content>(), drop);
		if (location != null) {
			f_sourceRef = JavaNode.getSrcRef(location);
		}
	}

	Content(String msg, IRNode location) {
		this(msg, location, null);
	}

	Content cloneAsLeaf() {
		Content clone = shallowCopy();
		if (clone != null) {
			clone.f_status = Status.BACKEDGE;
			clone.cloneOf = this;
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
		for (Content c : f_children) {
			size += c.freezeCount();
		}
		f_numIssues = size;
	}

	public int recomputeCounts() {
		if (f_numIssues < 0) {
			// No counts previously recorded here
			for (Content c : f_children) {
				c.recomputeCounts();
			}
			return -1;
		}
		boolean counted = false;
		int size = 0;
		for (Content c : f_children) {
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

			if (ref.getLineNumber() > 0) {
				result += "  at  " + name + " line " + ref.getLineNumber();
			} else if (!name.equals("?")) {
				result += "  at  " + name;
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
	@SuppressWarnings("restriction")
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
			s = JSureHistoricalSourceView.tryToMapPath(s);
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

	private void updateAttrs(final Map<String, String> attrs,
			final boolean includeCVS, final ISrcRef srcRef,
			final IResource srcFile) {
		attrs.put(ATTR_SOURCE, srcFile.getFullPath() + ".html");
		attrs.put(ATTR_LINE_NUM, Integer.toString(srcRef.getLineNumber()));
		if (includeCVS) {
			final ICVSFile cvsFile = CVSWorkspaceRoot
					.getCVSFileFor((IFile) srcRef.getEnclosingFile());
			try {
				attrs.put(ATTR_CVSSOURCE, cvsFile
						.getRepositoryRelativePath());
				ResourceSyncInfo fileInfo = cvsFile.getSyncInfo();
				if (fileInfo != null && fileInfo.getRevision() != null) {
					attrs.put(ATTR_CVSREVISION, fileInfo.getRevision());
				}
			} catch (CoreException e) {
				// do nothing
			}
		}
	}

	public static Object[] filterNonInfo(Object[] items) {
		Set<Content> result = new HashSet<Content>();
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof Content) {
				Content item = (Content) items[i];
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
		return filterNonInfo(f_children.toArray());
	}

	public Object[] getChildren() {
		return f_children.toArray();
	}

	public Category getCategory() {
		if (f_referencedDrop instanceof IRReferenceDrop) {
			return ((IRReferenceDrop) f_referencedDrop).getCategory();
		}
		return null;
	}

	public void resetChildren(Collection<Content> c) {
		if (c == null) {
			throw new IllegalArgumentException("New children is null");
		}
		f_children = c;
	}

	public void addChild(Content child) {
		f_children.add(child);
	}

	public int numChildren() {
		return f_children.size();
	}

	public Collection<Content> children() {
		return f_children;
	}

	public static Collection<Content> diffChildren(Collection<Content> last,
			Collection<Content> now) {
		Collection<Content> diffs = Diff.diff(last, now, false);
		for (Content c : diffs) {
			c.recomputeCounts();
		}
		return diffs;
	}

	public Collection<Content> getChildrenAsCollection() {
		return f_children;
	}

	public Collection<Content> setChildren(Collection<Content> c) {
		try {
			return f_children;
		} finally {
			f_children = c;
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

	public Object identity() {
		return id;
	}

	private class Identity {
		private Content content() {
			return Content.this;
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
			if (o instanceof Identity) {
				Content c = ((Identity) o).content();
				if (f_referencedDrop == c.f_referencedDrop) {
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
				Content c1 = o1.content();
				Content c2 = o2.content();
				if (c1.f_referencedDrop == c2.f_referencedDrop) {
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

	public boolean isShallowMatch(Content n) {
		return this.f_baseImageName.equals(n.f_baseImageName)
				&& this.f_imageFlags == n.f_imageFlags
				&& this.f_isInfo == n.f_isInfo
				&& this.f_isInfoDecorated == n.f_isInfoDecorated
				&& this.f_isInfoWarning == n.f_isInfoWarning
				&& this.f_isInfoWarningDecorate == n.f_isInfoWarningDecorate
				&& this.f_isPromiseWarning == n.f_isPromiseWarning;
	}

	public Content shallowCopy() {
		Content clone;
		try {
			clone = (Content) clone();
			clone.f_children = Collections.emptySet();
			return clone;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Content deepCopy() {
		Content copy = shallowCopy();
		copy.f_children = new ArrayList<Content>();
		for (Content c : f_children) {
			copy.f_children.add(c.deepCopy());
		}
		return copy;
	}
}