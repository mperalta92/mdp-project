package org.mdp.cli;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class ExtractTitleAndBody extends DefaultHandler{
	static String out = "C:/Users/RicardoMatias/Documents/procesamiento_masivo_de_datos/resources/Posts.tsv";
	
	static PrintWriter pw; 
	public static void main(String[] args)throws Exception {
		pw = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(out)),StandardCharsets.UTF_8));	
		ExtractTitleAndBody handler = new ExtractTitleAndBody();
		handler.parse(new InputSource("C:/Users/RicardoMatias/Documents/procesamiento_masivo_de_datos/resources/stackexchange/Posts.xml"));
		
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
	    	if (attributes.getValue("Title")!=null) {
				StringBuilder line = new StringBuilder();
				line.append(attributes.getValue("Id"));
				line.append("\t");
				line.append(attributes.getValue("Title"));
				line.append("\t");
				String body=attributes.getValue("Body");
				body= extractNewLines(body);
				line.append(body);
				line.append("\n");
				pw.write(line.toString());
			}
	    }

}
	private String extractNewLines(String body) {
		char[] aux=body.toCharArray();
		StringBuilder result=new StringBuilder();
		for(int i=0;i<aux.length;i++){
			if(aux[i]!='\n' && aux[i]!='\r' && aux[i]!='\t'){
				result.append(aux[i]);
			}else{
				result.append(" ");
			}
		}
		return result.toString();
		
	}
}
