package edu.cmu.cs.fluid.java.bind;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.surelogic.common.util.AppendIterator;
import com.surelogic.common.util.SingletonIterator;

/**
 * A representation of a collection of method signatures
 * all with the same name and number of parameters.
 * @author boyland
 */
public interface SingleMethodGroupSignatures extends Collection<IJavaFunctionType> {
	public String getName();
	public int getArity();
	public SingleMethodGroupSignatures add(SingleMethodGroupSignatures other);
	public SingleMethodGroupSignatures subst(IJavaTypeSubstitution theta);

	public static class EmptyMethodGroupSignatures 
	extends AbstractCollection<IJavaFunctionType> 
	implements SingleMethodGroupSignatures
	{
		public static SingleMethodGroupSignatures instance =
				new EmptyMethodGroupSignatures();

		@Override
		public String getName() {
			return null;
		}

		@Override
		public int getArity() {
			return -1;
		}

		@Override
		public SingleMethodGroupSignatures add(SingleMethodGroupSignatures other) {
			return other;
		}
		
		@Override
		public SingleMethodGroupSignatures subst(IJavaTypeSubstitution theta) {
			return this;
		}

		@Override
		public Iterator<IJavaFunctionType> iterator() {
			return Collections.<IJavaFunctionType>emptyList().iterator();
		}

		@Override
		public int size() {
			return 0;
		}
		
		@Override
		public String toString() {
			return "SIG{}";
		}
	}
	
	public static abstract class AbstractMethodGroupSignatures 
	extends AbstractCollection<IJavaFunctionType> 
	implements SingleMethodGroupSignatures
	{
		@Override
		public SingleMethodGroupSignatures add(SingleMethodGroupSignatures other) {
			if (other == null) return null;
			if (other.size() == 0) return this;
			if (other.getName().equals(getName()) && other.getArity() == getArity()) {
				return new JoinedMethodGroupSignatures(this,other);
			}
			return null;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			boolean started = false;
			for (IJavaFunctionType ft : this) {
				if (!started) {
					sb.append(getName() + "@{");
					started = true;
				} else {
					sb.append(",");
				}
				sb.append(ft.toSourceText());
			}
			sb.append("}");
			return sb.toString();
		}
	}
	
	public static class SingletonMethodGroupSignature
	extends AbstractMethodGroupSignatures
	{
		private final String name;
		private final IJavaFunctionType signature;
		
		public SingletonMethodGroupSignature(String n, IJavaFunctionType sig) {
			name = n;
			signature = sig;
		}
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public int getArity() {
			return signature.getParameterTypes().size();
		}


		@Override
		public SingleMethodGroupSignatures subst(IJavaTypeSubstitution theta) {
			if (theta == IJavaTypeSubstitution.NULL) return this;
			return new SingletonMethodGroupSignature(name,signature.subst(theta));
		}

		@Override
		public Iterator<IJavaFunctionType> iterator() {
			return new SingletonIterator<IJavaFunctionType>(signature);
		}

		@Override
		public int size() {
			return 1;
		}
	}
	
	static class JoinedMethodGroupSignatures
	extends AbstractMethodGroupSignatures
	{

		private final SingleMethodGroupSignatures sigs1, sigs2;
		private final int size;
		
		JoinedMethodGroupSignatures(SingleMethodGroupSignatures s1, SingleMethodGroupSignatures s2) {
			if (!s1.getName().equals(s2.getName()) ||
					s1.getArity() != s2.getArity()) {
				throw new IllegalArgumentException("can only combine if name and arity are the same");
			}
			sigs1 = s1;
			sigs2 = s2;
			size = s1.size() + s2.size();
		}
		
		@Override
		public String getName() {
			return sigs2.getName();
		}

		@Override
		public int getArity() {
			return sigs1.getArity();
		}
		
		@Override
		public SingleMethodGroupSignatures subst(IJavaTypeSubstitution theta) {
			IJavaFunctionType[] sigs = this.toArray(new IJavaFunctionType[size()]);
			for (int i=0; i < sigs.length; ++i) {
				sigs[i] = sigs[i].subst(theta);
			}
			return new ArrayMethodGroupSignatures(getName(),sigs);
		}

		@Override
		public Iterator<IJavaFunctionType> iterator() {
			return new AppendIterator<IJavaFunctionType>(sigs1.iterator(),sigs2.iterator());
		}

		@Override
		public int size() {
			return size;
		}
		
	}
	
	static class ArrayMethodGroupSignatures
	extends AbstractList<IJavaFunctionType>
	implements SingleMethodGroupSignatures
	{
		private final String name;
		private final IJavaFunctionType[] sigs;
		
		public ArrayMethodGroupSignatures(String n, IJavaFunctionType[] ss) {
			if (ss.length < 1) {
				throw new IllegalArgumentException("array must not be empty");
			}
			name = n;
			sigs = ss;
		}
		
		@Override
		public String getName() {
			return name;
		}

		@Override
		public int getArity() {
			return sigs[0].getParameterTypes().size();
		}

		@Override
		public SingleMethodGroupSignatures add(SingleMethodGroupSignatures other) {
			if (other == null) return null;
			if (other.size() == 0) return this;
			if (other.getName().equals(getName()) && other.getArity() == getArity()) {
				return new JoinedMethodGroupSignatures(this,other);
			}
			return null;			
		}

		@Override
		public SingleMethodGroupSignatures subst(IJavaTypeSubstitution theta) {
			if (theta == IJavaTypeSubstitution.NULL) return this;
			IJavaFunctionType[] news = sigs.clone();
			for (int i=0; i < sigs.length; ++i) {
				news[i] = sigs[i].subst(theta);
			}
			return new ArrayMethodGroupSignatures(name,news);
		}

		@Override
		public int size() {
			return sigs.length;
		}
		
		@Override
		public IJavaFunctionType get(int i) {
			return sigs[i];
		}

		// duplicating this code is annoying...  in Java 8, a default method would work.
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			boolean started = false;
			for (IJavaFunctionType ft : this) {
				if (!started) {
					sb.append(getName() + "@{");
					started = true;
				} else {
					sb.append(",");
				}
				sb.append(ft.toSourceText());
			}
			sb.append("}");
			return sb.toString();
		}
	}
}
