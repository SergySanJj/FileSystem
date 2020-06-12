package com.filesys;

import java.nio.ByteBuffer;

public class OpenFileTable {
    public FileHandler[] getHandlers() {
        return handlers;
    }

    public void setHandlers(FileHandler[] handlers) {
        this.handlers = handlers;
    }

    public static class FileHandler {
        ByteBuffer currData;
        int currentPosition;
        int fileDescr;

        boolean bufferModified;
        int fileBlockInBuffer;

        public FileHandler() {
            currData = ByteBuffer.allocate(DiskIO.getBlockSize());
            currentPosition = -1;
            fileDescr = -1;

            bufferModified = false;
            fileBlockInBuffer = -1;
        }
    }

    private FileHandler[] handlers;

    public OpenFileTable() {
        setHandlers(new FileHandler[4]);
    }

    public int handlerIndex(int descriptorIndex) {
        for (int i = 1; i < getHandlers().length; i++) {
            if (getHandlers()[i] != null && getHandlers()[i].fileDescr == descriptorIndex) return i;
        }
        return -1;
    }

    public int findFreeHandler() {
        for (int i = 1; i < 4; i++) {
            if (getHandlers()[i] == null) return i;
        }
        return -1;
    }
}
