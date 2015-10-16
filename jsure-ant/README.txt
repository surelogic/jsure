--------------------
-- JSure Ant Task --
--------------------

This Ant task can scan a project and produce JSure results. It is, by design
intended to be similar to the javac Ant task. This task results in a Zip
file that can be loaded into Eclipse to examine its results.

Requirements: Ant 1.7 (or higher) running on a Java 7 (or higher) JRE

NOTE in the examples below you have to change jsure.ant.home to point
to your unzipped "jsure-ant" directory (the directory this README.txt
is located within).

-- Reference --

The "jsure-scan" task is similar to the Ant javac task so you specify the
source and build classpath per that task. You do not need to specify a destdir.

Attributes added by jsure-scan task are:

o "projectname" (required) set this to the name of your project. This
   value is used to name the resulting Zip file
   e.g., projectname="JSureTutorial_BoundedFIFO"

o "jsureanthome" (required) set this to the location of this task. Typically
   you copy the pattern illustrated in the Ant script below and set this
   path as the property "jsure.ant.home" so that it can be used to specify
   the task classpath as well as the value of this attribute.
   e.g., <property name="jsure.ant.home" location="C:\\Users\\Tim\\jsure-ant" />
         ...
         jsureanthome="${jsure.ant.home}"
         
o "jsurescandir" (optional) This sets a directory to create the scan Zip file
   within. If it is not set then output is written to the current directory.
   This may be useful if you want to gather results in a particular location
   on your disk.
   e.g., jsurescandir="C:\\Users\\Tim\\myscans"
   
o "surelogictoolspropertiesfile" (optional) This sets the location of a
  'surelogic-tools.properties' file to be read to control the scan. This file
  can control aspects of the JSure scan (please see the JSure documentation).
  If this attribute is not set the tool looks for a 'surelogic-tools.properties'
  file in the current directory and uses that if it is found.
  e.g., surelogictoolspropertiesfile="C:\\Users\\Tim\\surelogic-tools.properties"

-- Example --

For the JSureTutorial_BoundedFIFO project create a jsure-scan.xml at the
project root:

<?xml version="1.0" encoding="UTF-8"?>
<project name="JSureTutorial_BoundedFIFO" default="scan" basedir=".">

  <!-- (CHANGE) path to the unzipped the JSure Ant task -->
  <property name="jsure.ant.home" location="C:\\Users\\Tim\\jsure-ant" />

  <!-- (COPY) JSure Ant task setup stuff -->
  <path id="jsure-ant.classpath">
    <dirset  dir="${jsure.ant.home}" includes="lib/com.surelogic.*" />
    <fileset dir="${jsure.ant.home}" includes="lib/**/*.jar" />
  </path>
  <taskdef name="jsure-scan" classname="com.surelogic.jsure.ant.JSureScan">
    <classpath refid="jsure-ant.classpath" />
  </taskdef>


  <path id="tf.classpath">
    <fileset dir="${basedir}" includes="**/*.jar" />
  </path>

  <target name="scan">
    <javac srcdir="${basedir}/src"
           destdir="${basedir}/bin"
           source="1.7"
           includeantruntime="false">
       <classpath refid="tf.classpath" />
    </javac>

    <jsure-scan srcdir="${basedir}/src"
                source="1.7"
                includeantruntime="false"
                jsureanthome="${jsure.ant.home}"
                projectname="JSureTutorial_BoundedFIFO">
       <classpath refid="tf.classpath" />
    </jsure-scan>
  </target>
</project>

Note we include a javac compile to illustrate how this is similar (and different)
from a jsure-scan.

To run a scan open a prompt to this directory and run "ant" or
run as Ant task in your Eclipse. The output will look like

Buildfile: C:\Users\Tim\Source\eclipse-work\meta-work\JSureTutorial_BoundedFIFO\build.xml
scan:
[jsure-scan] Project to scan w/JSure = JSureTutorial_BoundedFIFO
[jsure-scan] Scan output directory   = .
[jsure-scan] Scan JSureTutorial_Bounde-2015.05.18-at-12.37.55.231 examining 3 Java files
BUILD SUCCESSFUL
Total time: 8 seconds

Next you can load the 'JSureTutorial_Bounde-2015.05.18-at-12.37.55.231.jsure-scan.zip' file into
your Eclipse by choosing the "JSure" -> "Import Ant/Maven Scan..." menu item from the
Eclipse main menu. The file is located at the root of the JSureTutorial_BoundedFIFO
project on your disk.

#