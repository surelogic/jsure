package com.surelogic.antlr;

import java.io.IOException;
import java.util.Map;

public class GenerateFactoryRefs {
  public static void main(String[] args) throws IOException {
    AntlrLexerTokens tokens = new AntlrLexerTokens(args[0]);
    String pkg  = args[1].replace('/', '.');
    String name = args[2];
    
    System.out.println("package "+pkg+";\n");
    System.out.println("import com.surelogic.aast.java.*;");
    System.out.println("import com.surelogic.aast.promise.*;");
    System.out.println("import com.surelogic.parse.*;\n");
    System.out.println("public class "+name+" {");
    System.out.println("  public static void register(IASTFactory f) {");
    for(Map.Entry<Integer,String> e : tokens.tokens()) {
      if (e.getValue().equals("Nothing")) {
        System.out.println("    // Omitting "+e.getValue());
        continue;
      }
      if (e.getValue().endsWith("s")) {
        System.out.println("    // Omitting plural "+e.getValue());
        continue;
      }
      System.out.println("    "+e.getValue()+"Node.factory.register(f);");
    }
    System.out.println("  }");
    System.out.println("}");
  }
}
