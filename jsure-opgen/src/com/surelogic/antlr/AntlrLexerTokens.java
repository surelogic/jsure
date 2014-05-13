package com.surelogic.antlr;

import java.io.*;
import java.util.*;

public class AntlrLexerTokens {
  SortedMap<Integer,String> map = new TreeMap<Integer,String>();
  int start = 0;
  int end   = 0;
  
  AntlrLexerTokens(String name) throws IOException {
    FileReader r                  = new FileReader(name);
    LineNumberReader lr           = new LineNumberReader(r);
    String line;
    try {
    	while ((line = lr.readLine()) != null) {
    		String token;
    		Integer num;
    		if (line.startsWith("'='=")) {
    			token = "'='";
    			num = Integer.valueOf(line.substring(4));
    		} else {
    			StringTokenizer st = new StringTokenizer(line, "=");
    			token = st.nextToken();
    			num  = Integer.valueOf(st.nextToken());
    			if (st.hasMoreTokens()) {
    				throw new IllegalArgumentException("Wrong format: "+line);
    			}
    		}

    		//System.out.println(token+" => "+num);
    		map.put(num, token);
    		if ("START_IMAGINARY".equals(token)) {
    			start = num;
    		} 
    		else if ("END_IMAGINARY".equals(token)) {
    			end = num;
    		}
    	}
    } finally {
    	lr.close();
    }
  }

  public Iterable<Map.Entry<Integer,String>> tokens() {
	//return map.subMap(start+1, end).entrySet();
	return map.entrySet(); 
  }
}
