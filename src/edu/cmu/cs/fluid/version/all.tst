// spot check

version v(e#2,1);
assert n(1,e#1,2).TestPersistent.val = 2;
version v(e#1,3);
assert n(1,e#1,1).TestPersistent.val = 0;
assert n(1,e#0,4).TestPersistent.val = 1;
version v(e#1,2);
assert n(0,e#0,1).TestPersistent.syntax.Digraph.children[0] = n(1,e#0,2);
assert n(0,e#0,2).TestPersistent.cfg.SymmetricDigraph.parents[0] = n(0,e#0,6);
assert n(1,e#0,4).TestPersistent.cfg.Digraph.children[0] = n(0,e#0,1);
assert n(1,e#1,1).TestPersistent.cfg.EdgeDigraph.isEdge = true;
version v(e#0,1);
assert n(0,e#0,2).TestPersistent.val = 0;
assert n(1,e#0,3).TestPersistent.cfg.SymmetricDigraph.parents[0] = 
       n(0,e#0,7);
// assert n(0,e#0,1).TestPersistent.treeChanged{v(e#1,1)} = true;
version v(e#1,1);
assert n(0,e#0,1).TestPersistent.treeChanged = true;

// systematic tests

version v(e#0,1);

assert n(0,e#1,1).TestPersistent.val = 0;
assert n(0,e#1,2).TestPersistent.val = 0;
assert n(0,e#1,3).TestPersistent.val = 0;
assert n(0,e#1,4).TestPersistent.val = 0;
assert n(0,e#1,5).TestPersistent.val = 0;

assert n(0,e#0,1).TestPersistent.val = 0;
assert n(0,e#0,2).TestPersistent.val = 0;
assert n(0,e#0,3).TestPersistent.val = 0;
assert n(0,e#0,4).TestPersistent.val = 0;
assert n(0,e#0,5).TestPersistent.val = -1;
assert n(0,e#0,6).TestPersistent.val = 0;
assert n(0,e#0,7).TestPersistent.val = 0;

assert n(1,e#1,1).TestPersistent.val = 0;
assert n(1,e#1,2).TestPersistent.val = 0;
assert n(1,e#1,3).TestPersistent.val = 0;
assert n(1,e#1,4).TestPersistent.val = 0;

assert n(1,e#0,1).TestPersistent.val = 0;
assert n(1,e#0,2).TestPersistent.val = 0;
assert n(1,e#0,3).TestPersistent.val = 0;
assert n(1,e#0,4).TestPersistent.val = 1;
assert n(1,e#0,5).TestPersistent.val = 0;
assert n(1,e#0,6).TestPersistent.val = 1;
assert n(1,e#0,7).TestPersistent.val = -1;

assert n(0,e#0,1).TestPersistent.syntax.Digraph.children = { n(1,e#0,1), n(0,e#0,2), n(1,e#0,2) };
assert #001 .TestPersistent.syntax.Digraph.children = { #101, #002, #102 };
assert #002 .TestPersistent.syntax.Digraph.children = { #003, #103 };
assert #003 .TestPersistent.syntax.Digraph.children = { };
assert #004 .TestPersistent.syntax.Digraph.children = { };
assert #005 .TestPersistent.syntax.Digraph.children = undefined;
assert #006 .TestPersistent.syntax.Digraph.children = undefined;
assert #007 .TestPersistent.syntax.Digraph.children = undefined;

assert #101 .TestPersistent.syntax.Digraph.children = { };
assert #102 .TestPersistent.syntax.Digraph.children = { null };
assert #103 .TestPersistent.syntax.Digraph.children = { };
assert #104 .TestPersistent.syntax.Digraph.children = undefined;
assert #105 .TestPersistent.syntax.Digraph.children = undefined;
assert #106 .TestPersistent.syntax.Digraph.children = undefined;
assert #107 .TestPersistent.syntax.Digraph.children = undefined;

assert #001 .TestPersistent.treeChanged = true;
assert #002 .TestPersistent.treeChanged = true;
assert #003 .TestPersistent.treeChanged = true;
assert #004 .TestPersistent.treeChanged = true;
assert #005 .TestPersistent.treeChanged = false;
assert #006 .TestPersistent.treeChanged = false;
assert #007 .TestPersistent.treeChanged = false;

assert #101 .TestPersistent.treeChanged = true;
assert #102 .TestPersistent.treeChanged = true;
assert #103 .TestPersistent.treeChanged = true;
assert #104 .TestPersistent.treeChanged = false;
assert #105 .TestPersistent.treeChanged = false;
assert #106 .TestPersistent.treeChanged = false;
assert #107 .TestPersistent.treeChanged = false;


version v(e#1,1);

assert n(0,e#1,1).TestPersistent.val = 1;
assert n(0,e#1,2).TestPersistent.val = 0;
assert n(0,e#1,3).TestPersistent.val = 0;
assert n(0,e#1,4).TestPersistent.val = 0;
assert n(0,e#1,5).TestPersistent.val = 0;

assert n(0,e#0,1).TestPersistent.val = 0;
assert n(0,e#0,2).TestPersistent.val = 0;
assert n(0,e#0,3).TestPersistent.val = 0;
assert n(0,e#0,4).TestPersistent.val = 0;
assert n(0,e#0,5).TestPersistent.val = -1;
assert n(0,e#0,6).TestPersistent.val = 0;
assert n(0,e#0,7).TestPersistent.val = 0;

assert n(1,e#1,1).TestPersistent.val = -1;
assert n(1,e#1,2).TestPersistent.val = 0;
assert n(1,e#1,3).TestPersistent.val = 0;
assert n(1,e#1,4).TestPersistent.val = 0;

assert n(1,e#0,1).TestPersistent.val = 0;
assert n(1,e#0,2).TestPersistent.val = 0;
assert n(1,e#0,3).TestPersistent.val = 0;
assert n(1,e#0,4).TestPersistent.val = 1;
assert n(1,e#0,5).TestPersistent.val = 0;
assert n(1,e#0,6).TestPersistent.val = 1;
assert n(1,e#0,7).TestPersistent.val = -1;

assert #001 .TestPersistent.syntax.Digraph.children = { #101, #002, #102 };
assert #002 .TestPersistent.syntax.Digraph.children = { #003, #103 };
assert #003 .TestPersistent.syntax.Digraph.children = { };
assert #004 .TestPersistent.syntax.Digraph.children = { };
assert #005 .TestPersistent.syntax.Digraph.children = undefined;
assert #006 .TestPersistent.syntax.Digraph.children = undefined;
assert #007 .TestPersistent.syntax.Digraph.children = undefined;

assert #101 .TestPersistent.syntax.Digraph.children = { };
assert #102 .TestPersistent.syntax.Digraph.children = { #004 };
assert #103 .TestPersistent.syntax.Digraph.children = { };
assert #104 .TestPersistent.syntax.Digraph.children = undefined;
assert #105 .TestPersistent.syntax.Digraph.children = undefined;
assert #106 .TestPersistent.syntax.Digraph.children = undefined;
assert #107 .TestPersistent.syntax.Digraph.children = undefined;

assert #011 .TestPersistent.treeChanged = false;
assert #012 .TestPersistent.treeChanged = false;
assert #013 .TestPersistent.treeChanged = false;
assert #014 .TestPersistent.treeChanged = false;
assert #015 .TestPersistent.treeChanged = false;
assert #001 .TestPersistent.treeChanged = true;
assert #002 .TestPersistent.treeChanged = false;
assert #003 .TestPersistent.treeChanged = false;
assert #004 .TestPersistent.treeChanged = false;
assert #005 .TestPersistent.treeChanged = false;
assert #006 .TestPersistent.treeChanged = false;
assert #007 .TestPersistent.treeChanged = false;

assert #111 .TestPersistent.treeChanged = false;
assert #112 .TestPersistent.treeChanged = false;
assert #113 .TestPersistent.treeChanged = false;
assert #114 .TestPersistent.treeChanged = false;
assert #101 .TestPersistent.treeChanged = false;
assert #102 .TestPersistent.treeChanged = true;
assert #103 .TestPersistent.treeChanged = false;
assert #104 .TestPersistent.treeChanged = false;
assert #105 .TestPersistent.treeChanged = false;
assert #106 .TestPersistent.treeChanged = false;
assert #107 .TestPersistent.treeChanged = false;


// end of v(e#1,1) tests

version v(e#1,2);

assert n(0,e#1,1).TestPersistent.val = 1;
assert n(0,e#1,2).TestPersistent.val = 0;
assert n(0,e#1,3).TestPersistent.val = 0;
assert n(0,e#1,4).TestPersistent.val = 0;
assert n(0,e#1,5).TestPersistent.val = 0;

assert n(0,e#0,1).TestPersistent.val = 0;
assert n(0,e#0,2).TestPersistent.val = 0;
assert n(0,e#0,3).TestPersistent.val = 0;
assert n(0,e#0,4).TestPersistent.val = 0;
assert n(0,e#0,5).TestPersistent.val = -1;
assert n(0,e#0,6).TestPersistent.val = 0;
assert n(0,e#0,7).TestPersistent.val = 0;

assert n(1,e#1,1).TestPersistent.val = -1;
assert n(1,e#1,2).TestPersistent.val = 0;
assert n(1,e#1,3).TestPersistent.val = 0;
assert n(1,e#1,4).TestPersistent.val = 0;

assert n(1,e#0,1).TestPersistent.val = 0;
assert n(1,e#0,2).TestPersistent.val = 0;
assert n(1,e#0,3).TestPersistent.val = 0;
assert n(1,e#0,4).TestPersistent.val = 1;
assert n(1,e#0,5).TestPersistent.val = 0;
assert n(1,e#0,6).TestPersistent.val = 1;
assert n(1,e#0,7).TestPersistent.val = -1;

assert #001 .TestPersistent.syntax.Digraph.children = { #102, #002, #101 };
assert #002 .TestPersistent.syntax.Digraph.children = { #003, #103 };
assert #003 .TestPersistent.syntax.Digraph.children = { };
assert #004 .TestPersistent.syntax.Digraph.children = { };
assert #005 .TestPersistent.syntax.Digraph.children = undefined;
assert #006 .TestPersistent.syntax.Digraph.children = undefined;
assert #007 .TestPersistent.syntax.Digraph.children = undefined;

assert #101 .TestPersistent.syntax.Digraph.children = { };
assert #102 .TestPersistent.syntax.Digraph.children = { #004 };
assert #103 .TestPersistent.syntax.Digraph.children = { };
assert #104 .TestPersistent.syntax.Digraph.children = undefined;
assert #105 .TestPersistent.syntax.Digraph.children = undefined;
assert #106 .TestPersistent.syntax.Digraph.children = undefined;
assert #107 .TestPersistent.syntax.Digraph.children = undefined;

assert #011 .TestPersistent.treeChanged = false;
assert #012 .TestPersistent.treeChanged = false;
assert #013 .TestPersistent.treeChanged = false;
assert #014 .TestPersistent.treeChanged = false;
assert #015 .TestPersistent.treeChanged = false;
assert #001 .TestPersistent.treeChanged = true;
assert #002 .TestPersistent.treeChanged = false;
assert #003 .TestPersistent.treeChanged = false;
assert #004 .TestPersistent.treeChanged = false;
assert #005 .TestPersistent.treeChanged = false;
assert #006 .TestPersistent.treeChanged = false;
assert #007 .TestPersistent.treeChanged = false;

assert #111 .TestPersistent.treeChanged = false;
assert #112 .TestPersistent.treeChanged = false;
assert #113 .TestPersistent.treeChanged = false;
assert #114 .TestPersistent.treeChanged = false;
assert #101 .TestPersistent.treeChanged = false;
assert #102 .TestPersistent.treeChanged = false;
assert #103 .TestPersistent.treeChanged = false;
assert #104 .TestPersistent.treeChanged = false;
assert #105 .TestPersistent.treeChanged = false;
assert #106 .TestPersistent.treeChanged = false;
assert #107 .TestPersistent.treeChanged = false;

// end of v(e#1,2) tests


version v(e#1,3);

assert n(0,e#1,1).TestPersistent.val = 0;
assert n(0,e#1,2).TestPersistent.val = 0;
assert n(0,e#1,3).TestPersistent.val = 0;
assert n(0,e#1,4).TestPersistent.val = 0;
assert n(0,e#1,5).TestPersistent.val = 0;

assert n(0,e#0,1).TestPersistent.val = 0;
assert n(0,e#0,2).TestPersistent.val = 0;
assert n(0,e#0,3).TestPersistent.val = 0;
assert n(0,e#0,4).TestPersistent.val = 0;
assert n(0,e#0,5).TestPersistent.val = -1;
assert n(0,e#0,6).TestPersistent.val = 0;
assert n(0,e#0,7).TestPersistent.val = 0;

assert n(1,e#1,1).TestPersistent.val = 0;
assert n(1,e#1,2).TestPersistent.val = -1;
assert n(1,e#1,3).TestPersistent.val = 0;
assert n(1,e#1,4).TestPersistent.val = 0;

assert n(1,e#0,1).TestPersistent.val = 0;
assert n(1,e#0,2).TestPersistent.val = 0;
assert n(1,e#0,3).TestPersistent.val = 0;
assert n(1,e#0,4).TestPersistent.val = 1;
assert n(1,e#0,5).TestPersistent.val = 0;
assert n(1,e#0,6).TestPersistent.val = 1;
assert n(1,e#0,7).TestPersistent.val = 0;

assert #001 .TestPersistent.syntax.Digraph.children = { #101, #002, #102 };
assert #002 .TestPersistent.syntax.Digraph.children = { #003, #103, #004 };
assert #003 .TestPersistent.syntax.Digraph.children = { };
assert #004 .TestPersistent.syntax.Digraph.children = { };
assert #005 .TestPersistent.syntax.Digraph.children = undefined;
assert #006 .TestPersistent.syntax.Digraph.children = undefined;
assert #007 .TestPersistent.syntax.Digraph.children = undefined;

assert #013 .TestPersistent.syntax.Digraph.children = { };

assert #101 .TestPersistent.syntax.Digraph.children = { };
assert #102 .TestPersistent.syntax.Digraph.children = { null };
assert #103 .TestPersistent.syntax.Digraph.children = { };
assert #104 .TestPersistent.syntax.Digraph.children = undefined;
assert #105 .TestPersistent.syntax.Digraph.children = undefined;
assert #106 .TestPersistent.syntax.Digraph.children = undefined;
assert #107 .TestPersistent.syntax.Digraph.children = undefined;

assert #011 .TestPersistent.treeChanged = false;
assert #012 .TestPersistent.treeChanged = false;
assert #013 .TestPersistent.treeChanged = true;
assert #014 .TestPersistent.treeChanged = false;
assert #015 .TestPersistent.treeChanged = false;
assert #001 .TestPersistent.treeChanged = true;
assert #002 .TestPersistent.treeChanged = true;
assert #003 .TestPersistent.treeChanged = false;
assert #004 .TestPersistent.treeChanged = false;
assert #005 .TestPersistent.treeChanged = false;
assert #006 .TestPersistent.treeChanged = false;
assert #007 .TestPersistent.treeChanged = false;

assert #111 .TestPersistent.treeChanged = false;
assert #112 .TestPersistent.treeChanged = false;
assert #113 .TestPersistent.treeChanged = false;
assert #114 .TestPersistent.treeChanged = false;
assert #101 .TestPersistent.treeChanged = false;
assert #102 .TestPersistent.treeChanged = false;
assert #103 .TestPersistent.treeChanged = false;
assert #104 .TestPersistent.treeChanged = false;
assert #105 .TestPersistent.treeChanged = false;
assert #106 .TestPersistent.treeChanged = false;
assert #107 .TestPersistent.treeChanged = false;

// end of v(e#1,3) tests

version v(e#1,4);
			    
assert n(0,e#1,1).TestPersistent.val = 0;
assert n(0,e#1,2).TestPersistent.val = 0;
assert n(0,e#1,3).TestPersistent.val = 0;
assert n(0,e#1,4).TestPersistent.val = -1;
assert n(0,e#1,5).TestPersistent.val = -1;

assert n(0,e#0,1).TestPersistent.val = 0;
assert n(0,e#0,2).TestPersistent.val = 0;
assert n(0,e#0,3).TestPersistent.val = 0;
assert n(0,e#0,4).TestPersistent.val = 0;
assert n(0,e#0,5).TestPersistent.val = -1;
assert n(0,e#0,6).TestPersistent.val = 0;
assert n(0,e#0,7).TestPersistent.val = 0;

assert n(1,e#1,1).TestPersistent.val = 0;
assert n(1,e#1,2).TestPersistent.val = -1;
assert n(1,e#1,3).TestPersistent.val = 1;
assert n(1,e#1,4).TestPersistent.val = 1;

assert n(1,e#0,1).TestPersistent.val = 0;
assert n(1,e#0,2).TestPersistent.val = 0;
assert n(1,e#0,3).TestPersistent.val = 0;
assert n(1,e#0,4).TestPersistent.val = 1;
assert n(1,e#0,5).TestPersistent.val = 0;
assert n(1,e#0,6).TestPersistent.val = 1;
assert n(1,e#0,7).TestPersistent.val = 0;

assert #013 .TestPersistent.syntax.Digraph.children = { };

assert #001 .TestPersistent.syntax.Digraph.children = { #101, #002, #102 };
assert #002 .TestPersistent.syntax.Digraph.children = { #003, #103, #004 };
assert #003 .TestPersistent.syntax.Digraph.children = { };
assert #004 .TestPersistent.syntax.Digraph.children = { };
assert #005 .TestPersistent.syntax.Digraph.children = undefined;
assert #006 .TestPersistent.syntax.Digraph.children = undefined;
assert #007 .TestPersistent.syntax.Digraph.children = undefined;

assert #101 .TestPersistent.syntax.Digraph.children = { };
assert #102 .TestPersistent.syntax.Digraph.children = { #013 };
assert #103 .TestPersistent.syntax.Digraph.children = { };
assert #104 .TestPersistent.syntax.Digraph.children = undefined;
assert #105 .TestPersistent.syntax.Digraph.children = undefined;
assert #106 .TestPersistent.syntax.Digraph.children = undefined;
assert #107 .TestPersistent.syntax.Digraph.children = undefined;

assert #011 .TestPersistent.treeChanged = false;
assert #012 .TestPersistent.treeChanged = false;
assert #013 .TestPersistent.treeChanged = false;
assert #014 .TestPersistent.treeChanged = false;
assert #015 .TestPersistent.treeChanged = false;
assert #001 .TestPersistent.treeChanged = true;
assert #002 .TestPersistent.treeChanged = false;
assert #003 .TestPersistent.treeChanged = false;
assert #004 .TestPersistent.treeChanged = false;
assert #005 .TestPersistent.treeChanged = false;
assert #006 .TestPersistent.treeChanged = false;
assert #007 .TestPersistent.treeChanged = false;

assert #111 .TestPersistent.treeChanged = false;
assert #112 .TestPersistent.treeChanged = false;
assert #113 .TestPersistent.treeChanged = false;
assert #114 .TestPersistent.treeChanged = false;
assert #101 .TestPersistent.treeChanged = false;
assert #102 .TestPersistent.treeChanged = true;
assert #103 .TestPersistent.treeChanged = false;
assert #104 .TestPersistent.treeChanged = false;
assert #105 .TestPersistent.treeChanged = false;
assert #106 .TestPersistent.treeChanged = false;
assert #107 .TestPersistent.treeChanged = false;

// end of v(E#1,4) tests


version v(e#2,1);
			    

assert n(0,e#1,1).TestPersistent.val = 0;
assert n(0,e#1,2).TestPersistent.val = 0;
assert n(0,e#1,3).TestPersistent.val = 0;
assert n(0,e#1,4).TestPersistent.val = 0;
assert n(0,e#1,5).TestPersistent.val = 0;

assert n(0,e#0,1).TestPersistent.val = 0;
assert n(0,e#0,2).TestPersistent.val = 0;
assert n(0,e#0,3).TestPersistent.val = 0;
assert n(0,e#0,4).TestPersistent.val = 0;
assert n(0,e#0,5).TestPersistent.val = -1;
assert n(0,e#0,6).TestPersistent.val = 0;
assert n(0,e#0,7).TestPersistent.val = 0;

assert n(1,e#1,1).TestPersistent.val = 0;
assert n(1,e#1,2).TestPersistent.val = 2;
assert n(1,e#1,3).TestPersistent.val = 0;
assert n(1,e#1,4).TestPersistent.val = 0;

assert n(1,e#0,1).TestPersistent.val = 0;
assert n(1,e#0,2).TestPersistent.val = 0;
assert n(1,e#0,3).TestPersistent.val = 0;
assert n(1,e#0,4).TestPersistent.val = 1;
assert n(1,e#0,5).TestPersistent.val = 0;
assert n(1,e#0,6).TestPersistent.val = 1;
assert n(1,e#0,7).TestPersistent.val = 0;

assert #001 .TestPersistent.syntax.Digraph.children = { #101, #002, #102 };
assert #002 .TestPersistent.syntax.Digraph.children = { #003, #103 };
assert #003 .TestPersistent.syntax.Digraph.children = { };
assert #004 .TestPersistent.syntax.Digraph.children = { };
assert #005 .TestPersistent.syntax.Digraph.children = undefined;
assert #006 .TestPersistent.syntax.Digraph.children = undefined;
assert #007 .TestPersistent.syntax.Digraph.children = undefined;

assert #013 .TestPersistent.syntax.Digraph.children = { #004 };

assert #101 .TestPersistent.syntax.Digraph.children = { };
assert #102 .TestPersistent.syntax.Digraph.children = { null };
assert #103 .TestPersistent.syntax.Digraph.children = { };
assert #104 .TestPersistent.syntax.Digraph.children = undefined;
assert #105 .TestPersistent.syntax.Digraph.children = undefined;
assert #106 .TestPersistent.syntax.Digraph.children = undefined;
assert #107 .TestPersistent.syntax.Digraph.children = undefined;

assert #011 .TestPersistent.treeChanged = false;
assert #012 .TestPersistent.treeChanged = false;
assert #013 .TestPersistent.treeChanged = true;
assert #014 .TestPersistent.treeChanged = false;
assert #015 .TestPersistent.treeChanged = false;
assert #001 .TestPersistent.treeChanged = true;
assert #002 .TestPersistent.treeChanged = true;
assert #003 .TestPersistent.treeChanged = false;
assert #004 .TestPersistent.treeChanged = false;
assert #005 .TestPersistent.treeChanged = false;
assert #006 .TestPersistent.treeChanged = false;
assert #007 .TestPersistent.treeChanged = false;

assert #111 .TestPersistent.treeChanged = false;
assert #112 .TestPersistent.treeChanged = false;
assert #113 .TestPersistent.treeChanged = false;
assert #114 .TestPersistent.treeChanged = false;
assert #101 .TestPersistent.treeChanged = false;
assert #102 .TestPersistent.treeChanged = false;
assert #103 .TestPersistent.treeChanged = false;
assert #104 .TestPersistent.treeChanged = false;
assert #105 .TestPersistent.treeChanged = false;
assert #106 .TestPersistent.treeChanged = false;
assert #107 .TestPersistent.treeChanged = false;

// end of v(e#2,1) tests
