package com.filesys;

import java.io.*;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
//        CLI cli = new CLI(System.out, new InputStreamReader(System.in));
//        cli.start();
        File fileRes = new File("res.txt");
        File fileIn = new File("commands.txt");
        CLI cli = new CLI(new PrintStream(fileRes), new InputStreamReader(new FileInputStream(fileIn)));
        cli.start();
    }
}
