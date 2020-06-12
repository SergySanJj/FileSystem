package com.filesys;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class CLI {
    private FileSystem fs;
    private DiskIO dio;
    private ExecutorService executor = Executors.newFixedThreadPool(1);

    private String[] commandArgs;
    PrintStream printStream;
    InputStreamReader inputStream;

    public CLI(PrintStream printStream, InputStreamReader inputStream) {
        this.inputStream = inputStream;
        this.printStream = printStream;
    }

    private Map<String, Runnable> actionMap = new HashMap<>();

    {
        actionMap.put("in", () -> {
            if (argNum(2)) {
                String diskName = commandArgs[1];
                dio = new DiskIO(printStream);
                dio.initialize(diskName);
                fs = new FileSystem(dio, printStream);
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
        actionMap.put("op", () -> {
            if (argNum(2)) {
                String fileName = commandArgs[1];
                fs.open(fileName);
            }
        });
        actionMap.put("wr", () -> {
            if (argNum(4)) {
                try {
                    Integer index = Integer.parseInt(commandArgs[1]);
                    char character = commandArgs[2].charAt(0);
                    Integer count = Integer.parseInt(commandArgs[3]);

                    byte[] result = new byte[count];
                    Arrays.fill(result, (byte) character);
                    printStream.println("<" + fs.write(index, result, count) + "> bytes written");
                } catch (Exception e) {
                    printStream.println("Error occurred:\n\tWrite operation args must be integer char integer");

                }
            }
        });
        actionMap.put("rd", () -> {
            if (argNum(3)) {
                try {
                    Integer index = Integer.parseInt(commandArgs[1]);
                    Integer count = Integer.parseInt(commandArgs[2]);
                    if (count < 0) {
                        return;
                    }
                    ByteBuffer readBuffer = ByteBuffer.allocate(count);
                    int cnt = fs.read(index, readBuffer, count);
                    if (cnt == FileSystem.ERR) {
                        printStream.println("<File is empty>");
                        return;
                    }
                    char[] symbols = new char[cnt];
                    for (int i = 0; i < cnt; i++) {
                        symbols[i] = (char) readBuffer.get();
                    }
                    printStream.println("<" + cnt + "> bytes read: <" + String.valueOf(symbols)+ ">");
                } catch (Exception e) {
                    printStream.println("Error occurred:\n\tRead operation args must be integer");

                }

            }
        });
        actionMap.put("sk", () -> {
            if (argNum(3)) {
                try {
                    Integer index = Integer.parseInt(commandArgs[1]);
                    Integer pos = Integer.parseInt(commandArgs[2]);
                    fs.fileSeek(index, pos);
                } catch (Exception e) {
                    printStream.println("Error occurred:\n\tSeek operation args must be integer");
                }
            }
        });
        actionMap.put("cl", () -> {
            if (argNum(2)) {
                try {
                    Integer fileIndex = Integer.parseInt(commandArgs[1]);
                    fs.closeFile(fileIndex);
                } catch (Exception e) {
                    printStream.println("Error occurred:\n\tClose operation arg must be integer");
                }
            }
        });
        actionMap.put("de", () -> {
            if (argNum(2)) {
                String fileName = commandArgs[1];
                fs.destroy(fileName);
            }
        });
        actionMap.put("dr", () -> {
            if (argNum(1)) {
                fs.displayDirectory();
            }
        });
        actionMap.put("drop", () -> {
            if (argNum(2)) {
                String diskName = commandArgs[1];
                FileSystem.dropDisk(diskName, printStream);
            }
        });
    }

    public void start() {
        executor.execute(() -> {
            printStream.println("CLI started");
            BufferedReader br = new BufferedReader(inputStream);
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
                printStream.println("Finishing CLI");
                executor.awaitTermination(100, TimeUnit.NANOSECONDS);
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
        printStream.println("Available commands: \n" +
                " (in <diskName>), (sv <diskName>),\n" +
                " (dr), (op <fileName>), (cl <fileIndex>), (de <fileName>),\n" +
                " (rd <fileIndex> <count>), (wr <fileIndex> <char> <count>), (sk <fileIndex> <pos>),\n" +
                " (drop <diskName>)\n" +
                " (end)");
    }
}
