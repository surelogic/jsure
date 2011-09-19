package test;

import com.surelogic.Borrowed;
import com.surelogic.BorrowedInRegion;
import com.surelogic.Immutable;
import com.surelogic.ReadOnly;
import com.surelogic.Unique;
import com.surelogic.UniqueInRegion;

@SuppressWarnings("unused")
public class TestReceiverConstructor {
	// No annotations
	// ----------------------------------------------------------------------
	public TestReceiverConstructor() { /* empty */ }

	
	
	// One annotation
	// ----------------------------------------------------------------------
	@Borrowed("this")
	public TestReceiverConstructor(int a) { /* empty */ }
	
	@Unique("return")
	public TestReceiverConstructor(int a, int b) { /* empty */ }
	
	@ReadOnly("this")
	public TestReceiverConstructor(int a, int b, int c) { /* empty */ }
	
	@Immutable("this")
	public TestReceiverConstructor(int a, int b, int c, int d) { /* empty */ }
	
	@Unique("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e) { /* empty */ }
	
	@Unique(value="this", allowRead=true)
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f) { /* empty */ }
	
	
	
	// Two annotations
	// ----------------------------------------------------------------------
	@Borrowed("this")
	@ReadOnly("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g) { /* empty */ }
	
	@Borrowed("this")
	@Immutable("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h) { /* empty */ }
	
	@Borrowed("this")
	@Unique("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i) { /* empty */ }
	
	@Borrowed("this")
	@Unique(value="this", allowRead=true)
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j) { /* empty */ }
		

	@Unique("return")
	@ReadOnly("this")
	public TestReceiverConstructor(Object oops, int a, int b, int c, int d, int e, int f, int g, int h, int i, int j) { /* empty */ }
	
	@Unique("return")
	@Immutable("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k) { /* empty */ }
	
	@Unique("return, this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l) { /* empty */ }
		

	@ReadOnly("this")
	@Immutable("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m) { /* empty */ }
	
	@ReadOnly("this")
	@Unique("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n) { /* empty */ }
	
	@ReadOnly("this")
	@Unique(value="this", allowRead=true)
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o) { /* empty */ }
	
	
	@Immutable("this")
	@Unique("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p) { /* empty */ }
	
	@Immutable("this")
	@Unique(value="this", allowRead=true)
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p, int q) { /* empty */ }



	// Three annotations
	// ----------------------------------------------------------------------
	@Borrowed("this")
	@ReadOnly("this")
	@Immutable("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r) { /* empty */ }
	
	@Borrowed("this")
	@ReadOnly("this")
	@Unique("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s) { /* empty */ }
	
	@Borrowed("this")
	@ReadOnly("this")
	@Unique(value="this", allowRead=true)
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s, int t) { /* empty */ }


	@Borrowed("this")
	@Immutable("this")
	@Unique("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s, int t, int u) { /* empty */ }
	
	@Borrowed("this")
	@Immutable("this")
	@Unique(value="this", allowRead=true)
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s, int t, int u, int v) { /* empty */ }

	
	@Unique("return")
	@ReadOnly("this")
	@Immutable("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s, int t, int u, int v, int w) { /* empty */ }
	
	@Unique("return, this")
	@ReadOnly("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s, int t, int u, int v, int w, int x) { /* empty */ }
	

	@Unique("return, this")
	@Immutable("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s, int t, int u, int v, int w, int x, int y) { /* empty */ }
	
	
	@ReadOnly("this")
	@Immutable("this")
	@Unique("this")
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s, int t, int u, int v, int w, int x, int y, int z) { /* empty */ }
	
	@ReadOnly("this")
	@Immutable("this")
	@Unique(value="this", allowRead=true)
	public TestReceiverConstructor(int a, int b, int c, int d, int e, int f, int g, int h, int i, int j, int k, int l, int m, int n, int o, int p, int q, int r, int s, int t, int u, int v, int w, int x, int y, int z, int aa) { /* empty */ }
}
