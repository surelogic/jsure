Requirements
=============
   Java 7 or above 
   Apache Ant 1.7.0 or above 

To try the SmallWorld example
===============================
1. Copy examples/SmallWorld into your Eclipse workspace

2. Edit SmallWorld/test-build.xml to specify:
   -- sl.home (where jsure-ant.zip is unzipped)
   -- datadir (where the .jsure-data directory is inside of your workspace)
   
3. Run 'ant -f test-build.xml' from the command line within the SmallWorld directory


To use JSure in your own Ant script
===================================
1. Copy the definitions needed to set up the jsure-scan task from test-build.xml, and
   update it for your configuration. (see the build file for more details)
   
2. Setup a target (e.g 'scan') that uses the 'jsure-scan' task.  Since it uses many of 
   the same properties as the 'javac' task, you can probably copy your existing compile 
   target and modify it for this.
