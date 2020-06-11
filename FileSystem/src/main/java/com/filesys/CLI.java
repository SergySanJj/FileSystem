package com.filesys;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class CLI {
    private FileSystem fs;
    private ExecutorService executor = Executors.newFixedThreadPool(1);

    private String[] commandArgs;

    public CLI() {
    }

    private Map<String, Runnable> actionMap = new HashMap<>();

    {
        actionMap.put("in", () -> {
            if (argNum(2)) {
                String diskName = commandArgs[1];
                DiskIO dio = new DiskIO();
                dio.initialize(diskName);
                fs = new FileSystem(dio);
                if (DiskIO.diskExists(diskName)) {
                    fs.initFileSystem();
                    fs.loadFileSystem();
                } else {
                    fs.initFileSystem();
                    fs.initEmptyFileSystem();
                }
            }
        });
        actionMap.put("sv", () -> {
            if (argNum(2)) {
                String diskName = commandArgs[1];
                fs.saveFileSystem(diskName);
            }
        });
        actionMap.put("cr", () -> {
            if (argNum(2)) {
                String fileName = commandArgs[1];
                fs.createFile(fileName);
            }
        });
        actionMap.put("dr", () -> {
            if (argNum(1)) {
                fs.displayDirectory();
            }
        });
    }

    public void start() {
        executor.execute(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            boolean endSession = false;
            while (!endSession) {
                try {
                    String s = br.readLine();
                    commandArgs = s.split(" ");
                    if (commandArgs[0].equals("end")) {
                        endSession = true;
                    } else
                        commandHandler(commandArgs);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            executor.shutdown();
            try {
                System.out.println("Finishing CLI");
                executor.awaitTermination(1000, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });


    }

    private void commandHandler(String[] ss) {
        if (actionMap.containsKey(ss[0])) {
            this.actionMap.get(ss[0]).run();
        } else {
            printHelp();
        }
    }

    private boolean argNum(int n) {
        if (this.commandArgs.length == n) {
            return true;
        } else {
            printHelp();
            return false;
        }
    }

    private void printHelp() {
        System.out.println("Available commands: \n" +
                " (in <diskName>), (sv <diskName>),\n" +
                " (dr), (op <fileName>), (cl <fileName>), (de <fileName>),\n" +
                " (rd <index> <count>), (wr <index> <char> <count>), (sk <index> <pos>)");
    }
}
