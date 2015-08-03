package org.mdp.cli;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class ExtractPostLinks extends DefaultHandler{
	static String out = "src/resources/PostLinks.tsv";
	static Integer max = 1;
	
	static PrintWriter pw; 
	public static void main(String args[])throws Exception{
		pw = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(out)),StandardCharsets.UTF_8));	
	ExtractPostLinks handler = new ExtractPostLinks();
	handler.parse(new InputSource("src/resources/stackexchange/PostLinks.xml"));
	
	}

	private void parse(InputSource inputSource) throws SAXException, IOException {
		XMLReader parser = XMLReaderFactory.createXMLReader();
		parser.setContentHandler(this);
		parser.parse(inputSource);		
	}
	 @Override
	  public void characters(char[] ch, int start, int length) throws SAXException {
	    //if there were text as part of the elements, we would deal with it here
	    //by adding it to a StringBuffer, but we don't have to for this task
	    super.characters(ch, start, length);
	  }
	 
	  @Override
	  public void endElement(String uri, String localName, String qName) throws SAXException {
	    //this is where we would get the info from the StringBuffer if we had to,
	    //but all we need is attributes
	    super.endElement(uri, localName, qName);
	  }
	 
	  @Override
	  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
	    if(qName.equals("row")){
	    	String post_id=attributes.getValue("PostId");
	    	String rpost_id=attributes.getValue("RelatedPostId");
	    	StringBuilder line = new StringBuilder();
	    	line.append(post_id);
	    	line.append("\t");
	    	line.append(rpost_id);
	    	line.append("\n");
	    	pw.write(line.toString());
	    	if(Integer.parseInt(post_id)>max)
	    		max=Integer.parseInt(post_id);
	    	if(Integer.parseInt(rpost_id)>max)
	    		max=Integer.parseInt(rpost_id);
	    }
	    System.out.println(""+max);
	
}}

