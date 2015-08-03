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
import java.nio.charset.StandardCharsets;
import java.rmi.AlreadyBoundException;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.semanticweb.yars.nx.util.NxUtil;

/**
 * Convert full string URLs to numeric Object IDs.
 * 
 * @author Aidan
 */
public class OIDCompress {
	
	public static int TICKS = 100000;
	
	public static void main(String args[]) throws IOException, ClassNotFoundException, AlreadyBoundException, InstantiationException, IllegalAccessException{
		Option inO = new Option("i", "input file");
		inO.setArgs(1);
		inO.setRequired(true);
		
		Option ingzO = new Option("igz", "input file is GZipped");
		ingzO.setArgs(0);
		
		Option dictO = new Option("d", "output directory file");
		dictO.setArgs(1);
		dictO.setRequired(true);
		
		Option dictgzO = new Option("dgz", "output directory file should be GZipped");
		dictgzO.setArgs(0);
		
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
		BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		
		System.err.println("Reading from "+in);
		
		String dict = cmd.getOptionValue(dictO.getOpt());
		OutputStream dos = new FileOutputStream(dict);
		if(cmd.hasOption(dictgzO.getOpt())){
			dos = new GZIPOutputStream(dos);
		}
		PrintWriter dpw = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(dos),StandardCharsets.UTF_8));
		
		System.err.println("Writing dictionary to "+dict);
		
		String out = cmd.getOptionValue(outO.getOpt());
		OutputStream os = new FileOutputStream(out);
		if(cmd.hasOption(outgzO.getOpt())){
			os = new GZIPOutputStream(os);
		}
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(os),StandardCharsets.UTF_8));
		
		System.err.println("Writing encoded output to "+out);
		
		String line = null;
		TreeMap<String,Integer> allNodes = new TreeMap<String,Integer>();
		
		System.err.println("Preparing dictionary ...");
		
		int read = 0;
		while((line = br.readLine())!=null){
			String[] tab = line.split("\t");
			allNodes.put(tab[0],1);
			allNodes.put(tab[1],1);
			read++;
			if(read%TICKS==0)
				System.err.println("... read "+read);
		}
		
		br.close();
		System.err.println("Read "+read+". Loaded dictionary of size "+allNodes.size()+". Writing to file ...");
		
		
		int oid =  0;
		dpw.println(allNodes.size());
		for(String key:allNodes.keySet()){
			allNodes.put(key, oid);	
			dpw.println(oid+"\t"+key);
			
			oid++;
			if(oid%TICKS==0)
				System.err.println("... written "+oid);
		}
		System.err.println("Written dictionary of size "+oid+" to file.");
		
		is = new FileInputStream(in);
		if(cmd.hasOption(ingzO.getOpt())){
			is = new GZIPInputStream(is);
		}
		br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
		
		System.err.println("Reading again from "+in+" to encode ...");
		
		read = 0;
		line = null;
		
		// print the number of nodes as the first line
		pw.println(allNodes.size());
		while((line = br.readLine())!=null){
			// then print the encoded edges
			line = line.trim();
			if(!line.isEmpty()){
				String[] tab = line.split("\t");
				int oid1 = allNodes.get(tab[0]);
				int oid2 = allNodes.get(tab[1]);
				pw.println(oid1+"\t"+oid2);
				read++;
				if(read%TICKS==0)
					System.err.println("... read "+read);
			}
		}
		
		
		System.err.println("Finished! Written "+read+" encoded lines with "+allNodes.size()+" nodes");
		
		dpw.close();
		pw.close();
		br.close();
	}
	
	public static String unescapeNxExceptTab(String str){
		return NxUtil.unescape(str).replaceAll("\t", "\\t");
	}
}