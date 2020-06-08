package com.filesys;

import com.filesys.disk.BlockLocator;
import com.filesys.disk.Disk;

import java.io.*;
import java.nio.ByteBuffer;

public class DiskIO {
    private Disk disk;
    private String diskName;

    private int blockSize = 64;
    private int logicalBlocks;

    public void read_block(int blockNumber, ByteBuffer buffer) {
        BlockLocator bl = new BlockLocator(disk, blockSize, blockNumber);
        for (int i = 0; i < blockSize; i++) {
            buffer.put(i, bl.read());
        }
    }

    public void write_block(int blockNumber, ByteBuffer buffer) {
        BlockLocator bl = new BlockLocator(disk, blockSize, blockNumber);
        for (int i = 0; i < blockSize; i++) {
            bl.write(buffer.get(i));
        }
    }

    public DiskIO(int blockSize) {
        this.blockSize = blockSize;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public DiskIO() {
    }

    public void initialize(String diskName) {
        if (diskExists(diskName)) {
            load(diskName);
        } else {
            this.diskName = diskName;
            createNewDisk();
        }

        logicalBlocks = disk.size() / blockSize;
    }

    public int getLogicalBlocks() {
        return logicalBlocks;
    }

    public void saveAs(String diskName) {
        this.diskName = diskName;

        try {
            FileOutputStream f = new FileOutputStream(new File(diskName + ".txt"));
            ObjectOutputStream o = new ObjectOutputStream(f);

            o.writeObject(disk);
            o.close();
            f.close();

            System.out.println("Disk saved");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error occurred during saving " + diskName + " disk data");
        }
    }

    private void load(String diskName) {
        this.diskName = diskName;

        try {
            FileInputStream fi = new FileInputStream(new File(diskName + ".txt"));
            ObjectInputStream oi = new ObjectInputStream(fi);

            disk = (Disk) oi.readObject();

            oi.close();
            fi.close();

            System.out.println("Disk restored");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error occurred during loading " + diskName + " disk data");
        }
    }

    private void createNewDisk() {
        this.disk = new Disk();
        System.out.println("Disk initialized");
    }


    public static boolean diskExists(String diskName) {
        File f = new File(diskName + ".txt");
        return f.exists() && !f.isDirectory();
    }


}
