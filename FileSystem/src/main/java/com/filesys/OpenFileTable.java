package com.filesys;

import java.nio.ByteBuffer;

public class OpenFileTable {
    public static class OFTEntry {
        ByteBuffer RWBuffer;
        int currentPosition;
        int FDIndex;

        boolean bufferModified;
        int fileBlockInBuffer;

        public OFTEntry() {
            RWBuffer = ByteBuffer.allocate(DiskIO.getBlockSize());
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

    public int getFreeOFTEntryIndex() {
        for (int i = 1; i < 4; i++) {
            if (entries[i] == null) return i;
        }
        return -1;
    }
}
