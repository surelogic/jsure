package com.surelogic.xml;

import com.surelogic.common.CommonImages;
import com.surelogic.common.logging.IErrorListener;

import edu.cmu.cs.fluid.java.operator.StringLiteral;
import edu.cmu.cs.fluid.tree.Operator;
import edu.cmu.cs.fluid.util.ArrayUtil;
import edu.cmu.cs.fluid.util.UniqueID;

public class CommentElement extends AbstractJavaElement implements IMergeableElement {
	public static final char MARKER = '%';
	public static final String SEPARATOR = ":";
	public static final String END_MARKER = MARKER+"--";
	private final UniqueID uid;
	private int revision;
	private boolean modified;
	private String comment;

	private CommentElement(UniqueID uid, int rev, boolean mod, String c) {
		this.uid = uid;
		revision = rev;
		modified = mod;
		comment = c;
	}
	
	public static CommentElement make(String c) {		
		UniqueID uid = null;
		int rev = 0;
		boolean mod = false;
		
		if (c == null) {
			c = "";						
		} 
		else if (c.length() > 0) {
			if (c.charAt(0) == MARKER) {
				final int nextMarker = c.indexOf(END_MARKER, 1);
				if (nextMarker > 0) {
					// Probably found revision info
					String temp = c.substring(1, nextMarker);
					String[] tokens = temp.split(SEPARATOR);
					if (tokens.length == 3) {
						// Looks right
						try {
							uid = UniqueID.parseUniqueID(tokens[0]);
							rev = Integer.parseInt(tokens[1]);
							mod = Boolean.parseBoolean(tokens[2]);
							c = c.substring(nextMarker+END_MARKER.length());
						} catch(NumberFormatException e) {
							// Ignore the bad info
						}
					}					
				}
			}
		}
		if (uid == null) {
			uid = new UniqueID();
		}
		return new CommentElement(uid, rev, mod, c);
	}
	
	public UniqueID getUID() {
		return uid;
	}
	
	public int getRevision() {
		return revision;
	}
	
	public boolean isModified() {
		return modified;
	}
	
	public void incrRevision() {
		if (!isModified()) {
			throw new IllegalStateException("Not dirty");
		}
		modified = false;
		this.revision++;
	}
	
	@Override
	public int hashCode() {
		// TODO 
		return comment.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof CommentElement) {
			CommentElement other = (CommentElement) o;
			// TODO
			return comment.equals(other.comment);
		}
		return false;
	}
	
	public Operator getOperator() {
		return StringLiteral.prototype;
	}
	
	public String getImageKey() {
		return CommonImages.IMG_COMMENT;
	}

	public String getLabel() {
		return comment;
	}

	public boolean hasChildren() {
		return false;
	}

	public Object[] getChildren() {
		return ArrayUtil.empty;
	}

	@Override
	public boolean canModify() {
		return true;
	}

	@Override
	public void modify(String value, IErrorListener l) {
		comment = value;
		markAsDirty();
	}
	
	@Override
	void markAsDirty() {
		super.markAsDirty();
		modified = true;
	}
	
	@Override
	public CommentElement cloneMe() {
		return new CommentElement(uid, revision, modified, comment);
	}

	public void mergeAttached(IMergeableElement other) {
		if (other instanceof CommentElement) {
			// TODO what is there to do?  nothing's attached
		} else {
			throw new IllegalArgumentException("Trying to merge "+other);
		}
	}
}
