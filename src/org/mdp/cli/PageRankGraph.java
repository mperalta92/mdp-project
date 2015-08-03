package org.mdp.cli;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.rmi.AlreadyBoundException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Apply PageRank over a graph.
 * 
 * @author Aidan
 * @modified by mperalta92
 */
public class PageRankGraph {
	
	public static int TICKS = 100000;
	
	// damping factor
	public static double D = 0.85d;
	
	// number of iterations
	public static double ITERS = 25;
	
	public static void main(String args[]) throws IOException, ClassNotFoundException, AlreadyBoundException, InstantiationException, IllegalAccessException{
		Option inO = new Option("i", "input file");
		inO.setArgs(1);
		inO.setRequired(true);
		
		Option ingzO = new Option("igz", "input file is GZipped");
		ingzO.setArgs(0);
		
		Option outO = new Option("o", "output file");
		outO.setArgs(1);
		outO.setRequired(true);
		
		Option outgzO = new Option("ogz", "output file should be GZipped");
		outgzO.setArgs(0);
		
		Option helpO = new Option("h", "print help");
				
		Options options = new Options();
		options.addOption(inO);
		options.addOption(ingzO);
		options.addOption(outO);
		options.addOption(outgzO);
		options.addOption(helpO);

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("***ERROR: " + e.getClass() + ": " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("parameters:", options );
			return;
		}
		
		// print help options and return
		if (cmd.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("parameters:", options );
			return;
		}
		
		String in = cmd.getOptionValue(inO.getOpt());
		InputStream is = new FileInputStream(in);
		if(cmd.hasOption(ingzO.getOpt())){
			is = new GZIPInputStream(is);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		
		String out = cmd.getOptionValue(outO.getOpt());
		OutputStream os = new FileOutputStream(out);
		if(cmd.hasOption(outgzO.getOpt())){
			os = new GZIPOutputStream(os);
		}
		PrintWriter pw = new PrintWriter(new BufferedOutputStream(os));
		
		System.err.println("Writing ranks to "+out);
		
		System.err.println("Reading from "+in);
		
		String line = null;

		int read = 1;
		int size;
		try{
			size = Integer.parseInt(br.readLine());
		} catch(Exception e){
			System.err.println("Graph size not on first line. Use OIDCompress to encode graph first.");
			br.close();
			pw.close();
			throw e;
		}
		
		int[][] graph = new int[size][];
		
		while((line = br.readLine())!=null){
			line = line.trim();
			if(!line.isEmpty()){
				String[] tab = line.split("\t");
				try{
					int from = Integer.parseInt(tab[0]);
					int to = Integer.parseInt(tab[1]);
					
					// could make more efficient than resizing array
					// every time but okay for now ...
					int[] outlinks = graph[from];
					if(outlinks == null){
						outlinks = new int[0];
					}
					
					int[] newoutlinks = new int[outlinks.length+1];
					System.arraycopy(outlinks, 0, newoutlinks, 0, outlinks.length);
					newoutlinks[outlinks.length] = to;
					graph[from] = newoutlinks;
				} catch(Exception e){
					System.err.println("Error reading edge from line "+line);
				}
			}
			read++;
			if(read%TICKS==0)
				System.err.println("... read "+read);
		}
		System.err.println("Finished loading graph! Read "+read+" lines: "+graph.length+" nodes and "+read+" edges.");
		
		System.err.println("Ranking graph ...");
		double[] ranks = rankGraph(graph);
		
		System.err.println("Writing output ...");
		int written = 0;
		for(written=0; written<ranks.length; written++){
			pw.println(written+"\t"+ranks[written]);
			if(written%TICKS==0)
				System.err.println("... written "+written);
		}
		System.err.println("Finished writing ranks! Wrote "+written+" ranks.");
		
		pw.close();
		br.close();
	}
	
	public static double[] rankGraph(int[][] graph){

		//TODO Code this method 
		 int numberOfNodes= graph.length;
	        double [] newRank = new double[numberOfNodes];
	        double [] oldRank = new double[numberOfNodes];
	        for(int i=0;i<numberOfNodes;++i){
	            oldRank[i]=1.0/(double)numberOfNodes;
	        }
	        double r0=0;
	        double r1=0;
	        for(int i=0; i<ITERS; ++i){
	            for(int j=0;j<numberOfNodes;++j){
	                if(graph[j]==null){
	                    r1+=oldRank[j];
	                }else{
	                    r0+=oldRank[j];
	                }
	            }
	            double totalUniversalRank=r0*(1-D)+r1;
	            for(int k=0;k<numberOfNodes;++k){
	                newRank[k]=totalUniversalRank/numberOfNodes;   
	            }
	           //----Strong rank----
	            for(int j=0;j<numberOfNodes;++j){
	            	if (graph[j]!=null) {
						int[] out_j = graph[j];
						int m = out_j.length;
						double oldrank_j = oldRank[j];
						double toShare_j = oldrank_j * D;
						double split_j = toShare_j / m;
						for (int k = 0; k < out_j.length; k++) {
							newRank[out_j[k]] += split_j;
						}
					}
	            }
	            double sumaNewRanks=0;
	            double sumaOldRanks=0;
	            double epsilon=0;
	            for(int j=0;j<numberOfNodes;j++){
	            	sumaNewRanks+=newRank[j];
	            	sumaOldRanks+=oldRank[j];	            	
	            }
	            epsilon=sumaOldRanks-sumaNewRanks;
	            System.err.println("... sum new Ranks: "+sumaNewRanks);
	            System.err.println("... epsilon: "+epsilon);
	            
	        }

		return newRank;
	}
	
}