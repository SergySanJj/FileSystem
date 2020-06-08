package com.filesys;

public class Main {
    public static void main(String[] args) throws Exception {
        String diskName = "testDisk";
        DiskIO dio = new DiskIO();
        dio.initialize(diskName);
        FileSystem fs = new FileSystem(dio);
        if (DiskIO.diskExists(diskName)){
            fs.initFileSystem();
            fs.loadFileSystem();
        } else {
            fs.initFileSystem();
            fs.initEmptyFileSystem();
        }

        fs.saveFileSystem(diskName);

        System.out.println("Total logical blocks " + dio.getLogicalBlocks() + " with total capacity of " + dio.getLogicalBlocks()*dio.getBlockSize() + " bytes");

    }
}
