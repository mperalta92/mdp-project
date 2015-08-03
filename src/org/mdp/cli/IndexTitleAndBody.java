package org.mdp.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.rmi.AlreadyBoundException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * Main method to index plain-text abstracts from DBpedia using Lucene.
 * 
 * @author Aidan
 * @modified by mperalta92
 */
public class IndexTitleAndBody {

	public enum FieldNames {
		URL, TITLE, MODIFIED, BODY
	}

	public static int TICKS = 10000;

	public static void main(String args[]) throws IOException, ClassNotFoundException, AlreadyBoundException, InstantiationException, IllegalAccessException{
		Option inO = new Option("i", "input file");
		inO.setArgs(1);
		inO.setRequired(true);

		Option ingzO = new Option("igz", "input file is GZipped");
		ingzO.setArgs(0);

		Option outO = new Option("o", "output index directory");
		outO.setArgs(1);

		Option helpO = new Option("h", "print help");

		Options options = new Options();
		options.addOption(inO);
		options.addOption(ingzO);
		options.addOption(outO);
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

		String dir = cmd.getOptionValue("o");
		System.err.println("Opening directory at  "+dir);
		File fDir = new File(dir);
		if(fDir.exists()){
			if(fDir.isFile()){
				throw new IOException("Cannot open directory at "+dir+" since its already a file.");
			} 
		} else{
			if(!fDir.mkdirs()){
				throw new IOException("Cannot open directory at "+dir+". Try create the directory manually.");
			}
		}
		
		String in = cmd.getOptionValue(inO.getOpt());
		System.err.println("Opening input at  "+in);
		InputStream is = new FileInputStream(in);
		if(cmd.hasOption(ingzO.getOpt())){
			is = new GZIPInputStream(is);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8));

		indexTitleAndAbstract(br, fDir);

		br.close();
	}

	public static void indexTitleAndAbstract(BufferedReader input, File indexDir) throws IOException{
		Directory dir = FSDirectory.open(indexDir);
		Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
		
		IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_48, analyzer);
		iwc.setOpenMode(OpenMode.CREATE);
		
		IndexWriter writer = new IndexWriter(dir, iwc);
		
		String line = null;
		int read = 0;
		while((line = input.readLine())!=null){
			read++;
			if(read%TICKS==0){
				System.err.println("... read "+read);
			}
			
			line = line.trim();
			
			if(!line.isEmpty()){
				String[] tabs = line.split("\t");
				if(tabs.length>=2){
					Document d = new Document();
					
					Field url = new StringField(FieldNames.URL.name(), tabs[0], Field.Store.YES);
					d.add(url);
					
					Field title = new TextField(FieldNames.TITLE.name(), tabs[1], Field.Store.YES);
					d.add(title);
					
					// some documents have an empty abstract
					if(tabs.length==3){
						Field abst = new TextField(FieldNames.BODY.name(), tabs[2], Field.Store.YES);
						d.add(abst);
					}
					
					Field modified = new LongField(FieldNames.MODIFIED.name(), System.currentTimeMillis(), Field.Store.NO);
					d.add(modified);
					
					writer.addDocument(d);
				} else{
					System.err.println("Skipping partial line : '"+line+"'");
				}
			}
		}
		System.err.println("Finished! Read "+read+" lines");
		
		writer.close();
	}
}