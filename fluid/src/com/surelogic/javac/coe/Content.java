package com.surelogic.javac.coe;

import java.util.*;

import com.surelogic.tree.diff.*;

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
	private Status status = Status.SAME;

	private int numIssues = -1;

	/**
	 * The drop referenced, or null
	 */
	final Drop referencedDrop;

	/**
	 * This items children for the viewer, meant to be accessed by
	 * {@link #getChildren()}.
	 * 
	 * @see #getChildren()
	 */
	private Collection<Content> children;

	/**
	 * The message to display in the viewer, meant to be accessed by
	 * {@link #getMessage()}.
	 * 
	 * @see #getMessage()
	 */
	private final String message;

	private String m_baseImageName;

	private int m_imageFlags = 0;

	/**
	 * The fAST node that this item references, or null, if the associated drop
	 * defines the reference location.
	 */
	// private IRNode referencedLocation;
	private ISrcRef sourceRef = null;

	/**
	 * <code>true</code> if this content is from an inference (or InfoDrop),
	 * <code>false</code> otherwise.
	 */
	boolean isInfo = false;

	boolean isInfoDecorated = false;

	/**
	 * <code>true</code> if this content is from an inference (or InfoDrop)
	 * that is a warning, <code>false</code> otherwise.
	 */
	boolean isInfoWarning = false;

	boolean isInfoWarningDecorate = false;

	boolean donePropagatingWarningDecorators = false;

	/**
	 * <code>true</code> if this content is from an promise warning drop (or
	 * PromiseWarningDrop), <code>false</code> otherwise.
	 */
	boolean isPromiseWarning = false;

	Content(String msg, Collection<Content> content) {
		message = msg;
		children = content;
		referencedDrop = null;
	}

	Content(String msg) {
		this(msg, new HashSet<Content>());
	}

	Content(String msg, IRNode location) {
		this(msg);
		if (location != null) {
			sourceRef = JavaNode.getSrcRef(location);
		}
	}

	Content(String msg, Drop drop) {
		message = msg;
		children = new HashSet<Content>();
		referencedDrop = drop;
		if (drop instanceof IRReferenceDrop) {
			sourceRef = ((IRReferenceDrop) referencedDrop).getSrcRef();
		}
	}

	Content cloneAsLeaf() {
		Content clone = shallowCopy();
		if (clone != null) {
			clone.status = Status.BACKEDGE;
		}
		return clone;
	}

	public void setCount(int count) {
		numIssues = count;
	}

	public int freezeCount() {
		return numIssues = children.size();
	}

	public void freezeChildrenCount() {
		int size = 0;
		for (Content c : children) {
			size += c.freezeCount();
		}
		numIssues = size;
	}

	public int recomputeCounts() {
		if (numIssues < 0) {
			// No counts previously recorded here
			for (Content c : children) {
				c.recomputeCounts();
			}
			return -1;
		}
		boolean counted = false;
		int size = 0;
		for (Content c : children) {
			int count = c.recomputeCounts();
			if (count > 0) {
				size += count;
				counted = true;
			}
		}
		if (counted) {
			return numIssues = size;
		}
		return freezeCount();
	}

	public ISrcRef getSrcRef() {
		/*
		 * if (referencedDrop instanceof IRReferenceDrop) { return
		 * ((IRReferenceDrop) referencedDrop).getSrcRef(); } return
		 * (referencedLocation != null ? JavaNode .getSrcRef(referencedLocation) :
		 * null);
		 */
		return sourceRef;
	}

	public String getMessage() {
		String result = message;
		final ISrcRef ref = getSrcRef();
		if (ref != null) {
			String name = "?";

			Object f = ref.getEnclosingFile();
			/* FIX
			if (f instanceof IResource) {
				IResource file = (IResource) f;
				name = file.getName();
			} else
			*/ 
			if (f != null) {
				name = f.toString();
			}

			if (ref.getLineNumber() > 0) {
				result += "  at  " + name + " line " + ref.getLineNumber();
			} else if (!name.equals("?")) {
				result += "  at  " + name;
			}
		} else if (numIssues > 0) {
			if (message.contains("s)")) {
				result = numIssues + " " + result;
			} else {
				result += " (" + numIssues
						+ (numIssues > 1 ? " issues)" : " issue)");
			}
		}
		if (status != Status.SAME) {
			result += " -- " + status;
		}
		return result;
	}

	public void setBaseImageName(String name) {
		if (name == null)
			throw new IllegalArgumentException("the base image can't be null");
		m_baseImageName = name;
	}

	public String getBaseImageName() {
		return m_baseImageName;
	}

	public void setImageFlags(int flags) {
		m_imageFlags = flags;
	}

	public int getImageFlags() {
		return m_imageFlags;
	}

	public static Object[] filterNonInfo(Object[] items) {
		Set<Content> result = new HashSet<Content>();
		for (int i = 0; i < items.length; i++) {
			if (items[i] instanceof Content) {
				Content item = (Content) items[i];
				if (!item.isInfo) {
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
		return filterNonInfo(children.toArray());
	}

	public Object[] getChildren() {
		return children.toArray();
	}

	public Category getCategory() {
		if (referencedDrop instanceof IRReferenceDrop) {
			return ((IRReferenceDrop) referencedDrop).getCategory();
		}
		return null;
	}

	public void resetChildren(Collection<Content> c) {
		if (c == null) {
			throw new IllegalArgumentException("New children is null");
		}
		children = c;
	}

	public void addChild(Content child) {
		children.add(child);
	}

	public int numChildren() {
		return children.size();
	}

	public Collection<Content> children() {
		return children;
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
		return children;
	}

	public Collection<Content> setChildren(Collection<Content> c) {
		try {
			return children;
		} finally {
			children = c;
		}
	}

	public Status getStatus() {
		return status;
	}

	public Status setStatus(Status s) {
		try {
			return status;
		} finally {
			status = s;
		}
	}

	@Override
	public String toString() {
		if (sourceRef != null) {
			return message + " at " + sourceRef.getEnclosingFile() + ":"
					+ sourceRef.getLineNumber();
		}
		return message;
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
			if (message == null) {
				return 0;
			}
			return message.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Identity) {
				Content c = ((Identity) o).content();
				if (referencedDrop == c.referencedDrop) {
					return true;
				}
				if (message.equals(c.message)) {
					return sourceRef == c.sourceRef
							|| (sourceRef != null && c.sourceRef != null && sourceRef
									.getEnclosingFile().equals(
											c.sourceRef.getEnclosingFile()));
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
				if (c1.referencedDrop == c2.referencedDrop) {
					return 0;
				}
				if (c1.message.equals(c2.message)) {
					if (c1.sourceRef == c2.sourceRef) {
						return 0;
					}
					if (c1.sourceRef != null
							&& c2.sourceRef != null
							&& c1.sourceRef.getEnclosingFile().equals(
									c2.sourceRef.getEnclosingFile())) {
						String cmt1 = c1.sourceRef.getComment();
						String cmt2 = c2.sourceRef.getComment();
						if (cmt1 == null) {
							// near match, or completely off
							return (cmt2 == null) ? 1 : Integer.MAX_VALUE;
						}
						if (cmt2 == null) {
							// Completely off since cmt1 != null
							return Integer.MAX_VALUE;
						}
						return c1.sourceRef.getOffset()
								- c2.sourceRef.getOffset();
					}
				}
				return Integer.MAX_VALUE;
			}
		};
	}

	public boolean isShallowMatch(Content n) {
		return this.m_baseImageName.equals(n.m_baseImageName)
				&& this.m_imageFlags == n.m_imageFlags
				&& this.isInfo == n.isInfo
				&& this.isInfoDecorated == n.isInfoDecorated
				&& this.isInfoWarning == n.isInfoWarning
				&& this.isInfoWarningDecorate == n.isInfoWarningDecorate
				&& this.isPromiseWarning == n.isPromiseWarning;
	}

	public Content shallowCopy() {
		Content clone;
		try {
			clone = (Content) clone();
			clone.children = Collections.emptySet();
			return clone;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public Content deepCopy() {
		Content copy = shallowCopy();
		copy.children = new ArrayList<Content>();
		for (Content c : children) {
			copy.children.add(c.deepCopy());
		}
		return copy;
	}
}