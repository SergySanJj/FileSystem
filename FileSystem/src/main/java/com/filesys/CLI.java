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
        });
        actionMap.put("sv", () -> {
            String diskName = commandArgs[1];
            fs.saveFileSystem(diskName);
        });
    }

    public void start() {
        executor.execute(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            boolean endSession = false;
            while (!endSession) {
                String s = null;
                try {
                    s = br.readLine();
                    commandArgs = s.split(" ");
                    if (commandArgs[0].equals("end")) {
                        endSession = true;
                    } else
                        commandHandler(commandArgs);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Thread.currentThread().interrupt();
        });

        executor.shutdown();
        try {
            System.out.println("Finishing CLI");
            executor.awaitTermination(1000, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private void commandHandler(String[] ss) {
        System.out.println("Got command: " + Arrays.toString(ss));
        this.actionMap.get(ss[0]).run();
    }
}
