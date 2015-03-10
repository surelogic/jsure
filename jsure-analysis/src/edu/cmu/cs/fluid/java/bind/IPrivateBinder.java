package edu.cmu.cs.fluid.java.bind;

// Adds a few internal interface for use within this package (and *.project)
public interface IPrivateBinder extends IBinder {
	public IJavaMemberTable typeMemberTable(IJavaSourceRefType tdecl);
	public IJavaScope typeScope(IJavaType t);
}
