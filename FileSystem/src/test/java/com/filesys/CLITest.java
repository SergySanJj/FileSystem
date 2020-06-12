package com.filesys;

import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public class CLITest {
    @Test
    public void fileCommandTests() throws FileNotFoundException {
        File fileRes = new File("res.txt");
        File fileIn = new File("commands.txt");
        CLI cli = new CLI(new PrintStream(fileRes), new InputStreamReader(new FileInputStream(fileIn)));
        cli.start();
    }
}