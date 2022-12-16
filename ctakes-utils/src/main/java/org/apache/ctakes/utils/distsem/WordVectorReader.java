package org.apache.ctakes.utils.distsem;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;

public class WordVectorReader {
  private WordEmbeddings embeddings = null;
  private int dimensionality = 0;
  private int numWords = 0;
  
  public WordVectorReader(InputStream in) throws IOException{
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
    String line = reader.readLine();
    Scanner scanner = new Scanner(line);
    numWords = scanner.nextInt();
    dimensionality = scanner.nextInt();
    scanner.close();
    embeddings = new WordEmbeddings(dimensionality);
    
    while((line = reader.readLine()) != null){
      embeddings.add(line.trim());
    }
    reader.close();
  }
  
  public WordEmbeddings getEmbeddings(){
    return this.embeddings;
  }
  
  public static WordEmbeddings getEmbeddings(String fn) throws IOException{
    WordVectorReader reader = new WordVectorReader(new FileInputStream(fn));
    return reader.getEmbeddings();
  }
  
  public static WordEmbeddings getEmbeddings(InputStream in) throws IOException {
    WordVectorReader reader = new WordVectorReader(in);
    return reader.getEmbeddings();
  }
}
