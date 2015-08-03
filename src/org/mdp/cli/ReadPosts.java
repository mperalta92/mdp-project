package org.mdp.cli;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;


public class ReadPosts {

	public static void main(String[] args) throws IOException {
		String src ="C:/Users/RicardoMatias/Documents/procesamiento_masivo_de_datos/resources/Posts.tsv";
		BufferedReader in = new BufferedReader(new FileReader(src));
		int lines=60;
		for(int i=0;i<lines;i++){
			System.out.println(in.readLine());
		}
		
		in.close();

	}

}
