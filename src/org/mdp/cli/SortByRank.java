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
import java.util.TreeSet;
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
 * Sort articles by rank.
 * 
 * @author Aidan
 */
public class SortByRank {
	
	public static int TICKS = 100000;
	
	// damping factor
	public static double D = 0.85d;
	
	// number of iterations
	public static double ITERS = 10;
	
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
		
		System.err.println("Writing sorted ranks to "+out);
		
		System.err.println("Reading from "+in);
		
		String line = null;
		int read = 0;
		TreeSet<DoubleInt> sorted = new TreeSet<DoubleInt>();
		
		while((line = br.readLine())!=null){
			line = line.trim();
			if(!line.isEmpty()){
				String[] tab = line.split("\t");
				try{
					int node = Integer.parseInt(tab[0]);
					double rank = Double.parseDouble(tab[1]);
					
					DoubleInt di = new DoubleInt(rank, node);
					
					sorted.add(di);
				} catch(Exception e){
					System.err.println("Error reading edge from line "+line);
				}
			}
			read++;
			if(read%TICKS==0)
				System.err.println("... read "+read);
		}

		
		System.err.println("Finished loading ranks! Read "+read+" lines.");
		
		System.err.println("Writing sorted ranks ...");
		int written = 0;
		for(DoubleInt di:sorted){
			written ++;
			pw.println(di.i+"\t"+di.d);
			if(written%TICKS==0)
				System.err.println("... written "+written);
		}
		System.err.println("Finished writing ranks! Wrote "+written+" sorted ranks.");
		
		pw.close();
		br.close();
	}
	
	public static class DoubleInt implements Comparable<DoubleInt>{
		final double d;
		final int i;
		
		DoubleInt(double d, int i){
			this.d = d;
			this.i = i;
		}

		public int compareTo(DoubleInt o) {
			// descending order
			int comp = Double.compare(o.d, d);
			if(comp==0)
				return Integer.compare(i, o.i);
			return comp;
		}
		
		public int hashCode(){
			return new Double(d).hashCode() * i;
		}
		
		public boolean equals(Object o){
			if(o instanceof DoubleInt){
				DoubleInt di = (DoubleInt)o;
				if(di.d == d && di.i == i){
					return true;
				}
			}
			return false;
		}
		
	}
	
}