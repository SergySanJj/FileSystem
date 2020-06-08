package com.filesys.disk;

import java.util.concurrent.Executor;

public class BlockLocator {
    int cylinderNum;
    int trackNum;
    int sectorNum;
    int byteNum;

    int blockSize;
    int blocksCount;

    Disk disk;

    public BlockLocator(Disk disk, int blockSize, int blockNum) {
        this.disk = disk;
        this.blockSize = blockSize;
        blocksCount = disk.size() / blockSize;

        if (blockNum>=blocksCount)
        {
            System.out.println("Block is out of range");
            throw new IndexOutOfBoundsException();
        }

        int blocksPerCylinder = blocksCount / disk.cylinderCount();
        cylinderNum = blockNum / blocksPerCylinder;
        int blocksPerTrack = blocksPerCylinder / disk.trackCount();
        trackNum = (blockNum - cylinderNum * blocksPerCylinder) / blocksPerTrack;
        int blocksPerSector = blocksPerTrack / disk.sectorCount();
        sectorNum = (blockNum - cylinderNum * blocksPerCylinder - trackNum * blocksPerTrack) / blocksPerSector;
        int blocksPerBytes = blocksPerSector / disk.bytesCount();
        byteNum = (blockNum - cylinderNum * blocksPerCylinder - trackNum * blocksPerTrack - sectorNum * blocksPerSector);

    }

    public void nextByte() {
        if (byteNum < disk.bytesCount() - 1) {
            byteNum++;
        } else {
            byteNum = 0;
            if (sectorNum < disk.sectorCount() - 1) {
                sectorNum++;
            } else {
                sectorNum = 0;
                if (trackNum < disk.trackCount() - 1) {
                    trackNum++;
                } else {
                    trackNum = 0;
                    if (cylinderNum < disk.cylinderCount() - 1) {
                        cylinderNum++;
                    } else {
                        System.out.println("Disk overlap");
                        cylinderNum = 0;
                    }
                }
            }
        }
    }

    public void write(byte value) {
        disk.cylinders[cylinderNum].tracks[trackNum].sectors[sectorNum].bytes[byteNum] = value;
        nextByte();
    }

    public byte read() {
        byte val = disk.cylinders[cylinderNum].tracks[trackNum].sectors[sectorNum].bytes[byteNum];
        nextByte();
        return val;
    }
}
