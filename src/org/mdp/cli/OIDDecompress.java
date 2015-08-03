package org.mdp.cli;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
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
 * Convert from OIDs back to String URLs based on a previous 
 * OIDCompression output.
 * 
 * @author Aidan
 */
public class OIDDecompress {
	
	public static int TICKS = 100000;
	
	public static void main(String args[]) throws IOException, ClassNotFoundException, AlreadyBoundException, InstantiationException, IllegalAccessException{
		Option inO = new Option("i", "input file");
		inO.setArgs(1);
		inO.setRequired(true);
		
		Option ingzO = new Option("igz", "input file is GZipped");
		ingzO.setArgs(0);
		
		Option dictO = new Option("d", "input directory file");
		dictO.setArgs(1);
		dictO.setRequired(true);
		
		Option dictgzO = new Option("dgz", "input directory file is GZipped");
		dictgzO.setArgs(0);
		
		Option nO = new Option("n", "columns to decompress, 0 for first, 1 for second ... comma separated (default all)");
		nO.setArgs(Integer.MAX_VALUE);
		nO.setValueSeparator(',');
		
		Option outO = new Option("o", "output file");
		outO.setArgs(1);
		outO.setRequired(true);
		
		Option outgzO = new Option("ogz", "output file should be GZipped");
		outgzO.setArgs(0);
		
		Option helpO = new Option("h", "print help");
				
		Options options = new Options();
		options.addOption(inO);
		options.addOption(ingzO);
		options.addOption(dictO);
		options.addOption(dictgzO);
		options.addOption(nO);
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
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF8"));
		
		System.err.println("Reading from "+in);
		
		String din = cmd.getOptionValue(dictO.getOpt());
		InputStream dis = new FileInputStream(din);
		if(cmd.hasOption(dictgzO.getOpt())){
			dis = new GZIPInputStream(dis);
		}
		BufferedReader dbr = new BufferedReader(new InputStreamReader(dis, "UTF8"));
		
		System.err.println("Reading dictionary from "+din);
		
		String out = cmd.getOptionValue(outO.getOpt());
		OutputStream os = new FileOutputStream(out);
		if(cmd.hasOption(outgzO.getOpt())){
			os = new GZIPOutputStream(os);
		}
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(os),Charset.forName("UTF8")));
		
		System.err.println("Writing decoded output to "+out);
		
		TreeSet<Integer> indexes = null;
		if(cmd.hasOption(nO.getOpt())){
			indexes = new TreeSet<Integer>();
			String[] opts = cmd.getOptionValues(nO.getOpt());
			for(String opt:opts){
				indexes.add(Integer.parseInt(opt));
			}
		}
		
		String line = null;
		System.err.println("Loading dictionary ...");
		
		int read = 1;
		int size;
		try{
			size = Integer.parseInt(dbr.readLine());
		} catch(Exception e){
			System.err.println("Dictionary size not on first line. Use OIDCompress for dictionary.");
			br.close();
			pw.close();
			dbr.close();
			throw e;
		}
		
		String[] dict = new String[size];
		
		while((line = dbr.readLine())!=null){
			String[] tab = line.split("\t");
			dict[Integer.parseInt(tab[0])] = tab[1];
			read++;
			if(read%TICKS==0)
				System.err.println("... read "+read);
		}
		
		System.err.println("Read "+read+". Loaded dictionary of size "+dict.length+". Decompressing OIDs ...");
		
		read = 0;
		line = null;
		
		// print the number of nodes as the first line
		while((line = br.readLine())!=null){
			// then print the encoded edges
			String[] tab = line.split("\t");
			
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<tab.length; i++){
				if(indexes == null || indexes.contains(i)){
					sb.append(dict[Integer.parseInt(tab[i])]);
				} else{
					sb.append(tab[i]);
				}
				if(i!=tab.length-1) sb.append("\t");
			}
			
			pw.println(sb.toString());
			read++;
			if(read%TICKS==0)
				System.err.println("... read "+read);
		}
		
		
		System.err.println("Finished! Written "+read+" decoded lines.");
		
		dbr.close();
		pw.close();
		br.close();
	}
}
