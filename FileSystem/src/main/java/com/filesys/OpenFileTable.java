package com.filesys;

public class OpenFileTable {
    static class OFTEntry {
        byte[] RWBuffer;
        int currentPosition;
        int FDIndex;

        boolean bufferModified;
        int fileBlockInBuffer;

        OFTEntry() {
            RWBuffer = new byte[DiskIO.getBlockSize()];
            currentPosition = -1;
            FDIndex = -1;

            bufferModified = false;
            fileBlockInBuffer = -1;
        }
    }

    public OFTEntry[] entries;

    public OpenFileTable() {
        entries = new OFTEntry[4];
    }

    public int getOFTEntryIndex(int FDIndex) {
        for (int i = 1; i < entries.length; i++) {
            if (entries[i] != null && entries[i].FDIndex == FDIndex) return i;
        }
        return -1;
    }
}
