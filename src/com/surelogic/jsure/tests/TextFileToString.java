package com.surelogic.jsure.tests;
import java.io.*;
 
public class TextFileToString {
   /*=**********
   * constants *
   ************/
   public static final int END_OF_FILE = -1;
 
   //4k chunk size, you can set this to what you want
   public static final int CHUNK_SIZE = 4096;
 
   /*=***************************************************
   * readFile(): reads a text file and returns a string *
   *****************************************************/
   public static String readFile(Reader reader) throws IOException {
      StringBuffer buffer = new StringBuffer();
      char[] chunk = new char[CHUNK_SIZE];
 
      //read in a chunk of characters and stick them into a stringbuffer
      int character;
      while ((character=reader.read(chunk)) != END_OF_FILE) {
         buffer.append(chunk,0,character);
      }//end while
 
      return buffer.toString();
   }//end readFile
 
   /*=*************************************
   * main(): let's test our static method *
   ***************************************/
   public static void main(String args[]) throws Exception {
      //let's assume your file is in the same directory as the .class file
      Reader reader = new FileReader("test.txt");  //test.txt is the file to read
      String example = TextFileToString.readFile(reader);
      System.out.println(example);
   }//end main
}//end class
