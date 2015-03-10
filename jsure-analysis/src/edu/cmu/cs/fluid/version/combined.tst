// 
// Test Persistence
//
// This tests the structures built in TestPersistent
// It uses the new test system that permits loads during
// the test.
//
// Before this test starts, we assume all bundles,
// eras and versioned regins are loaded.
// However, we don't know any nodes yet since
// these are imported as we load in other things.

// We need to load something to get going.
// So we load the graph structure in the initial era
// for region #0, and the tree as an snapshot
// at the start of the initial era

load delta Ch(0,1) e#0;
load snapshot Ch(1,0) v(e#0,1);

// now we check everything here after this very partial load
version v(e#0,1);

assert n(0,e#0,1).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,2).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,3).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,4).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,5).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,6).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,7).TestPersistent.syntax.Digraph.children = unknown;

assert n(1,e#0,1).TestPersistent.syntax.Digraph.children[0] = exception;
assert n(1,e#0,2).TestPersistent.syntax.Digraph.children[0] = null;
assert n(1,e#0,3).TestPersistent.syntax.Digraph.children[0] = exception;
assert n(1,e#0,4).TestPersistent.syntax.Digraph.children = undefined;
assert n(1,e#0,5).TestPersistent.syntax.Digraph.children = undefined;
assert n(1,e#0,6).TestPersistent.syntax.Digraph.children = undefined;
assert n(1,e#0,7).TestPersistent.syntax.Digraph.children = undefined;

//assert n(0,e#1,1).TestPersistent.syntax.Digraph.children = undefined;
//assert n(0,e#1,2).TestPersistent.syntax.Digraph.children = undefined;
//assert n(0,e#1,3).TestPersistent.syntax.Digraph.children = undefined;
//assert n(0,e#1,4).TestPersistent.syntax.Digraph.children = undefined;
//assert n(0,e#1,5).TestPersistent.syntax.Digraph.children = undefined;

//assert n(1,e#1,1).TestPersistent.syntax.Digraph.children = undefined;
//assert n(1,e#1,2).TestPersistent.syntax.Digraph.children = undefined;
//assert n(1,e#1,3).TestPersistent.syntax.Digraph.children = undefined;
//assert n(1,e#1,4).TestPersistent.syntax.Digraph.children = undefined;

assert n(0,e#0,1).TestPersistent.syntax.Tree.parents = unknown;
assert n(0,e#0,2).TestPersistent.syntax.Tree.parents = unknown;
assert n(0,e#0,3).TestPersistent.syntax.Tree.parents = unknown;
assert n(0,e#0,4).TestPersistent.syntax.Tree.parents = unknown;
assert n(0,e#0,5).TestPersistent.syntax.Tree.parents = unknown;
assert n(0,e#0,6).TestPersistent.syntax.Tree.parents = unknown;
assert n(0,e#0,7).TestPersistent.syntax.Tree.parents = unknown;

assert n(1,e#0,1).TestPersistent.syntax.Tree.parents[0] = n(0,e#0,1);
assert n(1,e#0,2).TestPersistent.syntax.Tree.parents[0] = n(0,e#0,1);
assert n(1,e#0,3).TestPersistent.syntax.Tree.parents[0] = n(0,e#0,2);
assert n(1,e#0,4).TestPersistent.syntax.Tree.parents = undefined;
assert n(1,e#0,5).TestPersistent.syntax.Tree.parents = undefined;
assert n(1,e#0,6).TestPersistent.syntax.Tree.parents = undefined;
assert n(1,e#0,7).TestPersistent.syntax.Tree.parents = undefined;

//assert n(0,e#1,1).TestPersistent.syntax.Tree.parents = undefined;
//assert n(0,e#1,2).TestPersistent.syntax.Tree.parents = undefined;
//assert n(0,e#1,3).TestPersistent.syntax.Tree.parents = undefined;
//assert n(0,e#1,4).TestPersistent.syntax.Tree.parents = undefined;
//assert n(0,e#1,5).TestPersistent.syntax.Tree.parents = undefined;

//assert n(1,e#1,1).TestPersistent.syntax.Tree.parents = undefined;
//assert n(1,e#1,2).TestPersistent.syntax.Tree.parents = undefined;
//assert n(1,e#1,3).TestPersistent.syntax.Tree.parents = undefined;
//assert n(1,e#1,4).TestPersistent.syntax.Tree.parents = undefined;

assert n(0,e#0,1).TestPersistent.cfg.SymmetricDigraph.parents[0] = null;
assert n(0,e#0,1).TestPersistent.cfg.SymmetricDigraph.parents[1] = n(0,e#0,5);
assert n(0,e#0,2).TestPersistent.cfg.SymmetricDigraph.parents[0] = n(1,e#0,5);
assert n(0,e#0,2).TestPersistent.cfg.SymmetricDigraph.parents[1] = n(1,e#0,7);
assert n(0,e#0,3).TestPersistent.cfg.SymmetricDigraph.parents[0] = n(1,e#0,6);
assert n(0,e#0,3).TestPersistent.cfg.SymmetricDigraph.parents[1] = null;
assert n(0,e#0,4).TestPersistent.cfg.SymmetricDigraph.parents[0] = null;
assert n(0,e#0,4).TestPersistent.cfg.SymmetricDigraph.parents[1] = null;
assert n(0,e#0,5).TestPersistent.cfg.SymmetricDigraph.parents[0] = n(1,e#0,2);
assert n(0,e#0,5).TestPersistent.cfg.SymmetricDigraph.parents[1] = exception;
assert n(0,e#0,6).TestPersistent.cfg.SymmetricDigraph.parents[0] = n(0,e#0,2);
assert n(0,e#0,6).TestPersistent.cfg.SymmetricDigraph.parents[1] = exception;
assert n(0,e#0,7).TestPersistent.cfg.SymmetricDigraph.parents[0] = n(0,e#0,3);
assert n(0,e#0,7).TestPersistent.cfg.SymmetricDigraph.parents[1] = exception;

assert n(1,e#0,1).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,2).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,3).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,4).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,5).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,6).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,7).TestPersistent.cfg.SymmetricDigraph.parents = unknown;

assert n(0,e#0,1).TestPersistent.val = unknown;
assert n(0,e#0,2).TestPersistent.val = unknown;
assert n(0,e#0,3).TestPersistent.val = unknown;
assert n(0,e#0,4).TestPersistent.val = unknown;
assert n(0,e#0,5).TestPersistent.val = unknown;
assert n(0,e#0,6).TestPersistent.val = unknown;
assert n(0,e#0,7).TestPersistent.val = unknown;

assert n(1,e#0,1).TestPersistent.val = unknown;
assert n(1,e#0,2).TestPersistent.val = unknown;
assert n(1,e#0,3).TestPersistent.val = unknown;
assert n(1,e#0,4).TestPersistent.val = unknown;
assert n(1,e#0,5).TestPersistent.val = unknown;
assert n(1,e#0,6).TestPersistent.val = unknown;
assert n(1,e#0,7).TestPersistent.val = unknown;

// try the initial era: things are undefind even if unknown since
// we assume that slots are never defined in alpha. (Not sure if
// this is a good idea)

version v(e#0,0); // should be the initial version

assert n(1,e#0,1).TestPersistent.syntax.Digraph.children = undefined;
assert n(1,e#0,2).TestPersistent.syntax.Digraph.children = undefined;
assert n(1,e#0,3).TestPersistent.syntax.Digraph.children = undefined;

// should be undefined in some feelings:
assert n(1,e#0,1).TestPersistent.syntax.Tree.parents = undefined;
assert n(1,e#0,2).TestPersistent.syntax.Tree.parents = undefined;
assert n(1,e#0,3).TestPersistent.syntax.Tree.parents = undefined;

// nothing should be known in other eras

version v(e#1,1);

assert n(0,e#0,1).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,2).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,3).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,4).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,5).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,6).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,7).TestPersistent.syntax.Digraph.children = unknown;

assert n(1,e#0,1).TestPersistent.syntax.Digraph.children = unknown;
assert n(1,e#0,2).TestPersistent.syntax.Digraph.children = unknown;
assert n(1,e#0,3).TestPersistent.syntax.Digraph.children = unknown;
assert n(1,e#0,4).TestPersistent.syntax.Digraph.children = unknown;
assert n(1,e#0,5).TestPersistent.syntax.Digraph.children = unknown;
assert n(1,e#0,6).TestPersistent.syntax.Digraph.children = unknown;
assert n(1,e#0,7).TestPersistent.syntax.Digraph.children = unknown;

//assert n(0,e#1,1).TestPersistent.syntax.Digraph.children = unknown;
//assert n(0,e#1,2).TestPersistent.syntax.Digraph.children = unknown;
//assert n(0,e#1,3).TestPersistent.syntax.Digraph.children = unknown;
//assert n(0,e#1,4).TestPersistent.syntax.Digraph.children = unknown;
//assert n(0,e#1,5).TestPersistent.syntax.Digraph.children = unknown;

//assert n(1,e#1,1).TestPersistent.syntax.Digraph.children = unknown;
//assert n(1,e#1,2).TestPersistent.syntax.Digraph.children = unknown;
//assert n(1,e#1,3).TestPersistent.syntax.Digraph.children = unknown;
//assert n(1,e#1,4).TestPersistent.syntax.Digraph.children = unknown;

assert n(0,e#0,1).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(0,e#0,2).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(0,e#0,3).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(0,e#0,4).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(0,e#0,5).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(0,e#0,6).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(0,e#0,7).TestPersistent.cfg.SymmetricDigraph.parents = unknown;

assert n(1,e#0,1).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,2).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,3).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,4).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,5).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,6).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,7).TestPersistent.cfg.SymmetricDigraph.parents = unknown;

// now let's load some more

load snapshot Ch(0,2) v(e#1,2);

version v(e#0,1);

// check that we can now refer to nodes previously unknown:

assert n(0,e#1,1).TestPersistent.syntax.Digraph.children = undefined;
assert n(0,e#1,2).TestPersistent.syntax.Digraph.children = undefined;
assert n(0,e#1,3).TestPersistent.syntax.Digraph.children = undefined;
assert n(0,e#1,4).TestPersistent.syntax.Digraph.children = undefined;
assert n(0,e#1,5).TestPersistent.syntax.Digraph.children = undefined;

version v(e#1,2);

assert n(0,e#1,1).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#1,2).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#1,3).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#1,4).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#1,5).TestPersistent.syntax.Digraph.children = unknown;

assert n(0,e#0,1).TestPersistent.val = 0;
assert n(0,e#0,2).TestPersistent.val = 0;
assert n(0,e#0,3).TestPersistent.val = 0;
assert n(0,e#0,4).TestPersistent.val = 0;
assert n(0,e#0,5).TestPersistent.val = -1;
assert n(0,e#0,6).TestPersistent.val = 0;
assert n(0,e#0,7).TestPersistent.val = 0;

assert n(0,e#1,1).TestPersistent.val = 1;
assert n(0,e#1,2).TestPersistent.val = 0;
assert n(0,e#1,3).TestPersistent.val = 0;
assert n(0,e#1,4).TestPersistent.val = 0;
assert n(0,e#1,5).TestPersistent.val = 0;

assert n(1,e#0,1).TestPersistent.val = unknown;
assert n(1,e#0,2).TestPersistent.val = unknown;
assert n(1,e#0,3).TestPersistent.val = unknown;
assert n(1,e#0,4).TestPersistent.val = unknown;
assert n(1,e#0,5).TestPersistent.val = unknown;
assert n(1,e#0,6).TestPersistent.val = unknown;
assert n(1,e#0,7).TestPersistent.val = unknown;

// of course the other things are still unknown
assert n(0,e#0,1).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(0,e#0,2).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(0,e#0,3).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(0,e#0,4).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(0,e#0,5).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(0,e#0,6).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(0,e#0,7).TestPersistent.cfg.SymmetricDigraph.parents = unknown;

assert n(1,e#0,1).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,2).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,3).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,4).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,5).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,6).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,7).TestPersistent.cfg.SymmetricDigraph.parents = unknown;


// load a delta that uses the preceding snapshot:
load delta Ch(1,0) e#1;
// and one that has support of a delta
load delta Ch(0,1) e#1;
// and one that has no support
// (it should cause the snapshot/delta before to be loaded.)
load delta Ch(0,2) e#1;

version v(e#0,1);

assert n(0,e#1,1).TestPersistent.val = 0;
assert n(0,e#1,2).TestPersistent.val = 0;
assert n(0,e#1,3).TestPersistent.val = 0;
assert n(0,e#1,4).TestPersistent.val = 0;
assert n(0,e#0,1).TestPersistent.val = 0;
assert n(0,e#0,2).TestPersistent.val = 0;
assert n(0,e#0,3).TestPersistent.val = 0;
assert n(0,e#0,4).TestPersistent.val = 0;
assert n(0,e#0,5).TestPersistent.val = -1;
assert n(0,e#0,6).TestPersistent.val = 0;
assert n(0,e#0,7).TestPersistent.val = 0;

assert n(1,e#0,1).TestPersistent.val = unknown;
assert n(1,e#0,2).TestPersistent.val = unknown;
assert n(1,e#0,3).TestPersistent.val = unknown;
assert n(1,e#0,4).TestPersistent.val = unknown;
assert n(1,e#0,5).TestPersistent.val = unknown;
assert n(1,e#0,6).TestPersistent.val = unknown;
assert n(1,e#0,7).TestPersistent.val = unknown;

version v(e#1,1);

assert n(0,e#0,1).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,2).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,3).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,4).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,5).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,6).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#0,7).TestPersistent.syntax.Digraph.children = unknown;

assert n(1,e#0,1).TestPersistent.syntax.Digraph.children[0] = exception;
assert n(1,e#0,2).TestPersistent.syntax.Digraph.children[0] = n(0,e#0,4);
assert n(1,e#0,3).TestPersistent.syntax.Digraph.children[0] = exception;
assert n(1,e#0,4).TestPersistent.syntax.Digraph.children = undefined;
assert n(1,e#0,5).TestPersistent.syntax.Digraph.children = undefined;
assert n(1,e#0,6).TestPersistent.syntax.Digraph.children = undefined;
assert n(1,e#0,7).TestPersistent.syntax.Digraph.children = undefined;

assert n(0,e#1,1).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#1,2).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#1,3).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#1,4).TestPersistent.syntax.Digraph.children = unknown;
assert n(0,e#1,5).TestPersistent.syntax.Digraph.children = unknown;

assert n(1,e#1,1).TestPersistent.syntax.Digraph.children = undefined;
assert n(1,e#1,2).TestPersistent.syntax.Digraph.children = undefined;
assert n(1,e#1,3).TestPersistent.syntax.Digraph.children = undefined;
assert n(1,e#1,4).TestPersistent.syntax.Digraph.children = undefined;

assert n(0,e#0,1).TestPersistent.syntax.Tree.parents = unknown;
assert n(0,e#0,2).TestPersistent.syntax.Tree.parents = unknown;
assert n(0,e#0,3).TestPersistent.syntax.Tree.parents = unknown;
assert n(0,e#0,4).TestPersistent.syntax.Tree.parents = unknown;
assert n(0,e#0,5).TestPersistent.syntax.Tree.parents = unknown;
assert n(0,e#0,6).TestPersistent.syntax.Tree.parents = unknown;
assert n(0,e#0,7).TestPersistent.syntax.Tree.parents = unknown;

assert n(1,e#0,1).TestPersistent.syntax.Tree.parents[0] = n(0,e#0,1);
assert n(1,e#0,2).TestPersistent.syntax.Tree.parents[0] = n(0,e#0,1);
assert n(1,e#0,3).TestPersistent.syntax.Tree.parents[0] = n(0,e#0,2);
assert n(1,e#0,4).TestPersistent.syntax.Tree.parents = undefined;
assert n(1,e#0,5).TestPersistent.syntax.Tree.parents = undefined;
assert n(1,e#0,6).TestPersistent.syntax.Tree.parents = undefined;
assert n(1,e#0,7).TestPersistent.syntax.Tree.parents = undefined;

//assert n(0,e#1,1).TestPersistent.syntax.Tree.parents = undefined;
//assert n(0,e#1,2).TestPersistent.syntax.Tree.parents = undefined;
//assert n(0,e#1,3).TestPersistent.syntax.Tree.parents = undefined;
//assert n(0,e#1,4).TestPersistent.syntax.Tree.parents = undefined;
//assert n(0,e#1,5).TestPersistent.syntax.Tree.parents = undefined;

//assert n(1,e#1,1).TestPersistent.syntax.Tree.parents = undefined;
//assert n(1,e#1,2).TestPersistent.syntax.Tree.parents = undefined;
//assert n(1,e#1,3).TestPersistent.syntax.Tree.parents = undefined;
//assert n(1,e#1,4).TestPersistent.syntax.Tree.parents = undefined;

assert n(0,e#0,1).TestPersistent.cfg.SymmetricDigraph.parents[0] = null;
assert n(0,e#0,1).TestPersistent.cfg.SymmetricDigraph.parents[1] = n(0,e#0,5);
assert n(0,e#0,2).TestPersistent.cfg.SymmetricDigraph.parents[0] = n(1,e#0,5);
assert n(0,e#0,2).TestPersistent.cfg.SymmetricDigraph.parents[1] = n(1,e#0,7);
assert n(0,e#0,3).TestPersistent.cfg.SymmetricDigraph.parents[0] = n(1,e#0,6);
assert n(0,e#0,3).TestPersistent.cfg.SymmetricDigraph.parents[1] = null;
assert n(0,e#0,4).TestPersistent.cfg.SymmetricDigraph.parents[0] = n(0,e#1,1);
assert n(0,e#0,4).TestPersistent.cfg.SymmetricDigraph.parents[1] = null;
assert n(0,e#0,5).TestPersistent.cfg.SymmetricDigraph.parents[0] = n(1,e#0,2);
assert n(0,e#0,5).TestPersistent.cfg.SymmetricDigraph.parents[1] = exception;
assert n(0,e#0,6).TestPersistent.cfg.SymmetricDigraph.parents[0] = n(0,e#0,2);
assert n(0,e#0,6).TestPersistent.cfg.SymmetricDigraph.parents[1] = exception;
assert n(0,e#0,7).TestPersistent.cfg.SymmetricDigraph.parents[0] = n(0,e#0,3);
assert n(0,e#0,7).TestPersistent.cfg.SymmetricDigraph.parents[1] = exception;

assert n(1,e#0,1).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,2).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,3).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,4).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,5).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,6).TestPersistent.cfg.SymmetricDigraph.parents = unknown;
assert n(1,e#0,7).TestPersistent.cfg.SymmetricDigraph.parents = unknown;


// now load a snapshot on the root of e#3
load snapshot Ch(0,0) v(e#1,3);

// now load in a delta supported by this snapshot.
load delta Ch(0,0) e#2;

// test that the delta loaded fine:
version v(e#2,1);

assert #001 .TestPersistent.syntax.Digraph.children = { #101, #002, #102 };
assert #002 .TestPersistent.syntax.Digraph.children = { #003, #103 };
assert #003 .TestPersistent.syntax.Digraph.children = { };
assert #004 .TestPersistent.syntax.Digraph.children = { };
assert #005 .TestPersistent.syntax.Digraph.children = undefined;
assert #006 .TestPersistent.syntax.Digraph.children = undefined;
assert #007 .TestPersistent.syntax.Digraph.children = undefined;

// ...
// should have more tests
// ...

// force everything to be loaded

load delta Ch(0,0) e#0;
load delta Ch(1,0) e#0;
load delta Ch(0,1) e#0;
load delta Ch(1,1) e#0;
load delta Ch(0,2) e#0;
load delta Ch(1,2) e#0;

load delta Ch(0,0) e#1; 
load delta Ch(1,0) e#1;
load delta Ch(0,1) e#1;
load delta Ch(1,1) e#1; 
load delta Ch(0,2) e#1;
load delta Ch(1,2) e#1;

load delta Ch(1,0) e#2;
load delta Ch(0,1) e#2;
load delta Ch(1,1) e#2;
load delta Ch(0,2) e#2;
load delta Ch(1,2) e#2;

load vchunk 3 e#0;
load vchunk 3 e#1;
load vchunk 3 e#2;

read "all.tst";
