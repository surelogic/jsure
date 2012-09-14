package com.surelogic.opgen.syntax;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.surelogic.opgen.util.LineWordParser;


/**
 * Parses and stores the syntax for a given operator
 * @author chance
 */
public class SyntaxBuilder {
  public static void main(String[] args) {
    String name = (args.length > 0) ? args[0] : "Test.op";
    OpSyntax s = (new SyntaxBuilder()).parse(name);
    s.printState(System.out);
  }
  
  private static final Pattern opStart = Pattern.compile("^[a-z ]*operator");
  private static final Pattern opLineMatch = Pattern.compile("^([a-z ]*)operator[ \t]*([A-Za-z][A-Za-z0-9_]+)(.*)");
  private static final Pattern tagMatch = Pattern.compile("<.*>");
  private static final Pattern childMatch = Pattern.compile("^@?([a-zA-Z][A-Za-z0-9_]+:)?([A-Za-z][A-Za-z0-9_]+\\??)(\\([a-zA-Z0-9_]+\\))?$"); 
  private static final Pattern slotMatch = Pattern.compile("^\\$@?([a-zA-Z][A-Za-z0-9_]+:)?([A-Za-z][A-Za-z0-9_]+)(\\([a-zA-Z0-9_,]+\\))?$"); 
  private static final Pattern propMatch = Pattern.compile("^([a-zA-Z][A-Za-z0-9_]+)=\"?([A-Za-z%][A-Za-z0-9_%\\.:{}\\(\\)]+)?\"?,?$"); 
  private static final Pattern ifaceMatch = Pattern.compile("^([A-Z][A-Za-z0-9_]+)Interface,?$"); 

  /**
   * Used to figure out what kind of name this is
   */
  //private static final Pattern nameTypeMatch = Pattern.compile("([A-Z][a-z0-9_]+)Name,?$");
  /**
   * Used to figure out what kind of call this is
   */
  //private static final Pattern callTypeMatch = Pattern.compile("([A-Z][a-z0-9_]+)Call,?$");
  
  /**
   * The child type corresponding to any node
   * (e.g., the parent of operators/nodes that don't explicitly specify one)
   */
  public static final String ANY_NODE = "ANY_NODE";
  private String defaultParentOp = "edu.cmu.cs.fluid.parse.JJNode";
  
  public SyntaxBuilder() {
	  // Nothing to do
  }
  
  /**
   * @param parentOp Expected to be fully qualified
   */
  public SyntaxBuilder(String parentOp) {
    defaultParentOp = parentOp;
  }

  private static boolean startsWith(String s, Pattern p) {
    return p.matcher(s).find();
  }
  
  private static boolean matches(String s, Pattern p) {
    return p.matcher(s).matches();
  }
  
  private LineWordParser parser;
  
  /**
   * The text that appears before and after the syntax block
   */
  private String beforeText, afterOp, afterText;
  private String modifiers;
  private String opname;
  private String superop = defaultParentOp;
  private boolean isroot = false;
  private boolean isConcrete = true;
  
  private final List<String> superifaces = new ArrayList<String>();
  private final List<SyntaxElement> syntax = new ArrayList<SyntaxElement>();
  private final List<Attribute> attributes = new ArrayList<Attribute>();
  private final List<Child> childs = new ArrayList<Child>();
  private Modifier variability = null;
  private boolean precedence = false;
  private int variableChildIndex = -1;
  private Child variableChild = null;
  
  private final Map<Property,String> props = new HashMap<Property,String>();
  private long lastModified = -1;

  private final Properties globalProps = new Properties();
  
  private void clear() {
    lastModified = -1;
    parser = null;
    beforeText = "";
    afterOp = "";
    afterText = "";
    modifiers = "";
    opname = "";
    superop = defaultParentOp;
    isroot = false;
    isConcrete = true;
    
    superifaces.clear();
    syntax.clear();
    attributes.clear();
    childs.clear();
    props.clear();
    variability = null;
    precedence = false;
    variableChildIndex = -1;
    variableChild = null;
  }
  
  public List<OpSyntax> parseOpFiles(String[] args) {
    List<OpSyntax> syntax = new ArrayList<OpSyntax>();
    if (args.length > 0) {
      for(String arg : args) {
        parseOpFiles(syntax, arg, null);
      }
    } else {
      parseOpFiles(syntax, "ops", null);
    }
    return syntax;
  }
  
  public List<OpSyntax> parseOpFiles(List<String> args) {
    List<OpSyntax> syntax = new ArrayList<OpSyntax>();
    if (args.size() > 0) {
      for(String arg : args) {
        parseGlobalProperties(arg);
        parseOpFiles(syntax, arg, null);
      }
    } else {
      parseGlobalProperties("ops");
      parseOpFiles(syntax, "ops", null);
    }
    return syntax;
  }
  
  private void parseGlobalProperties(String name) {    
    try {
      InputStream is = new FileInputStream(name+File.separatorChar+"opgen.properties");
      globalProps.load(is);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public Properties getGlobalProperties() {
    return globalProps;
  }
  
  public void parseOpFiles(List<OpSyntax> syntax, String name, String pkgName) {
    File f = new File(name);
    
    if (f.isFile()) {
      if (name.endsWith(".op")) {
        syntax.add(parse(pkgName, name));
      } else if (!name.endsWith("GNUmakefile") && !name.endsWith(".cvsignore") && 
                 !name.endsWith(".html") && !name.endsWith("header")) {
        System.err.println("SyntaxBuilder ignoring on non .op file: "+name);
      }
    } else if (f.isDirectory()) {      
      if (name.endsWith("CVS")) {
        return; // Ignore these
      }
      // System.err.println("Recursing on "+name);
      
      // Call myself recursively on each file in the directory
      String[] files = f.list();  
      if (pkgName == null) {
        pkgName = "";
      } else if (pkgName.equals("")){
        pkgName = f.getName();
      } else {
        pkgName = pkgName+"."+f.getName(); 
      }
      for (String file : files) {
        parseOpFiles(syntax, name+File.separator+file, pkgName);      
      }
    }
  }
  
  public final OpSyntax parse(String opFilename) {
    return parse(null, opFilename);
  }
  
  public OpSyntax parse(String pkgName, String opFilename) {
    clear();
    recordFileDetails(opFilename);

    try {
      StringBuilder before = new StringBuilder();
      String line;
      
      parser = new LineWordParser(opFilename);

      // Copy everything before the operator definition
      while ((line = parser.readLine()) != null && !startsWith(line, opStart)) {
        if (!line.startsWith("#")) {  
          before.append(line);
          before.append('\n');
        }
      }
      beforeText = before.toString();
      try {
      parseOperatorLine(line);
      } catch (NullPointerException e) {
        //throw new RuntimeException(opFilename);
        throw e;
      }
      parseSyntax();
      parseProperties();
      
      // Copy the rest of the file  
      StringBuilder after = new StringBuilder();
      while ((line = parser.readLine()) != null) {
        after.append(line);
        after.append('\n');
      }
      afterText = after.toString();
      parser.close();        
      parser = null;
      if (pkgName != null && pkgName.length() == 0) {
        pkgName = null;
      }
      return new OpSyntax(pkgName, opFilename, lastModified,
                          beforeText, afterOp, afterText,
                          modifiers, opname, superop,
                          isroot, isConcrete, superifaces,
                          syntax, attributes, childs,
                          variability, precedence,
                          variableChildIndex, variableChild, 
                          props);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return null;
    } catch (IOException e) { 
      e.printStackTrace();
      return null;
    }
  }

  private void recordFileDetails(String opFilename) {
    File f = new File(opFilename);
    lastModified = f.lastModified();
  }

  /**
   * Match against the beginning of the operator definition
   */
  private void parseOperatorLine(String line) throws IOException {
    Matcher m = opLineMatch.matcher(line);
    if (m.matches()) {
      modifiers = m.group(1);
      opname = m.group(2);
      parser.pushLine(m.group(3));
      
      final StringBuilder afterOp = new StringBuilder();
      String next = parser.readWord();
      if (next.equals("extends")) {
        superop = parser.readWord();
        next = parser.readWord();
      } else {
        // No extends clause, so it is a root
        isroot = true;
      }
      //System.out.println("'"+next+"'");
      
      if (next.equals("{")) {
        return; // Nothing after the op line
      }
      
      boolean hasImplements = next.equals("implements");
      afterOp.append(next);
      afterOp.append(' ');
      
      // Copy everything else up to open brace of operator definition
      while (!(next = parser.readWord()).equals("{")) {
        //System.out.println("'"+next+"'");
  
        // look for tokens of the form FooInterface
        Matcher m2;
        if (hasImplements && (m2 = ifaceMatch.matcher(next)).matches()) { 
          superifaces.add(m2.group(1));
        }
        afterOp.append(next);  
        afterOp.append(' ');
      }
      this.afterOp = afterOp.toString();
    } else {
      throw new Error("Did not match operator line");
    }
  }
  
  private void parseSyntax() throws IOException {    
    // Look for start of "syntax" block
    String word = parser.readWord();
    if (word.equals("syntax")) {
      int total = 0, numChildren = 0, numAttrs = 0;
      List<Integer> open = new ArrayList<Integer>();
      
      // Confirm opening brace
      word = parser.readWord();
      if (!word.equals("{")) {
        throw new Error("expected { in syntax");
      }
      // Loop until we find the closing brace
      while (!(word = parser.readWord()).equals("}")) {
        Matcher m;
        if (word.equals("*") || word.equals("*/") || 
            word.equals("+") || word.equals("+/")) {
          // Deal with variability tags
          Variability v = new Variability(total, word);
          syntax.add(v);
          variability = v;
        }
        else if (word.equals("?") || word.equals("?/")) {
          // Deal with option tags
          if (variability == null || !variability.text.startsWith("+")) {
            throw new Error("? and ?/ only permitted for + variable syntax");
          }
          Option o = new Option(total, word);
          syntax.add(o);
          variability = o;
        }
        else if (word.equals("(")){
          syntax.add(new OpenParen(total, -1));
          open.add(total);
        }
        else if (word.equals(")")) {    
          int openIndex = pop(open);
          syntax.set(openIndex, new OpenParen(openIndex, total));
          syntax.add(new CloseParen(total, openIndex));
        }
        else if (matches(word, tagMatch)) {             
          // Not currently doing anything with "<tag>" 
          syntax.add(new Tag(total, word));
        }
        else if ((m = childMatch.matcher(word)).matches()) {            
          // Match against "foo:FooChild" or "@foo:FooChild"
          Child c = Child.create(numChildren, word, m);
          syntax.add(c);
          childs.add(c);
          variableChildIndex = total;
          numChildren += 1;
        }
        else if ((m = slotMatch.matcher(word)).matches()) {  
          // Match against "$foo:FooType"
          Attribute a = Attribute.create(numAttrs, word, m);
          syntax.add(a);
          attributes.add(a);
          numAttrs += 1;
        }
        else if (word.startsWith("\"")) {
          syntax.add(new Token(total, word));
        }
        else {
          throw new Error("bad syntax element: "+word);
        }     
        total += 1;
      }
      if (open.size() != 0) {
        throw new Error("unmatched left parenthesis");
      }
      
      if (numChildren != childs.size()) {
        throw new Error(numChildren+" != "+childs.size());
      }
      if (total != syntax.size()) {
        throw new Error(total+" != "+syntax.size());
      }
      if (numAttrs != attributes.size()) {
        throw new Error(numAttrs+" != "+attributes.size());
      }
      
      if (variability != null) {
        if (numChildren < 1) {
          throw new Error("'*' requires at least one child");
        }
        variableChild = (Child) syntax.get(variableChildIndex);
        numChildren -= 1;
      } else {
        variableChildIndex = -1;
      }
    } else {
      // Not a concrete production
      isConcrete = false;
      
      parser.pushWord(word);
    }
  }

  private void parseProperties() throws IOException {    
    // Look for start of "syntax" block
    String word = parser.readWord();
    if (word.equals("properties")) {
      // Confirm opening brace
      word = parser.readWord();
      if (!word.equals("{")) {
        throw new Error("expected { in syntax");
      }
      // Loop until we find the closing brace
      while (!(word = parser.readWord()).equals("}")) {
        Matcher m;      
        if ((m = propMatch.matcher(word)).matches()) {
          Property p = Property.props.get(m.group(1));
          if (p != null) {
            String val = m.group(2);
            props.put(p, val == null ? "" : val);
          } else {
            System.err.println("Ignoring property: "+word);
          }
        } else {
          throw new Error("Unknown token in properties block: "+word);
        }
      }
    } else {
      parser.pushWord(word);
    }
    /*
    Matcher m  = null;
    String val = null;
    if ((m = nameTypeMatch.matcher(opname)).matches()) {
      val = m.group(1);
    } 
    else if ((m = callTypeMatch.matcher(opname)).matches()) {
      val = m.group(1);
    }
    if (val != null) {
      props.put(KnownProperties.BINDING.val, val);
    }
    */
  }
  
  private int pop(List<Integer> open) {
    int last = open.size() - 1;
    if (last < 0) {
      throw new Error("unmatched right parenthesis");
    }
    return open.remove(last);
  }
}
