package org.apache.ctakes.utils.distsem;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class DistanceMetrics {

  
  public static void main(String[] args) throws IOException{
    System.out.println("Reading embeddings...");
    WordEmbeddings words = WordVectorReader.getEmbeddings(args[0]);
    
    String prompt = "Enter a single word to get neighbors, or two comma separated words for similarity score (or <ctrl>+d to exit):";
    Scanner scanner = new Scanner(System.in);
    String line;
    System.out.println("Word Distance similarities:");
    System.out.println(prompt);
    while(scanner.hasNextLine()){
      line = scanner.nextLine().trim();
      String[] input = line.split(",");
      
      if(input.length == 1){
        if(words.containsKey(input[0])){
          List<String> simWords = words.getSimilarWords(input[0], 20);
          for(String word : simWords){
            System.out.print(word);
            System.out.print('\t');
            System.out.print(words.getSimilarity(input[0], word));
            System.out.println();
          }
        }else{
          System.err.println("Do not have vectors for your word: " + input[0]);
          System.err.flush();
        }
      }else if(input.length == 2){
        double sim = words.getSimilarity(input[0].trim(), input[1].trim());
        System.out.print("Similarity of (");
        System.out.print(input[0].trim());
        System.out.print(',');
        System.out.print(input[1].trim());
        System.out.print(" = ");
        System.out.println(sim);
      }else{
        System.err.println("Input should be one or two words only!");
      }
      System.out.println(prompt);
    }
    scanner.close();
  }
}
