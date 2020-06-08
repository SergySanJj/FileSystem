package com.filesys.disk;

import com.filesys.DiskIO;
import com.filesys.FileDescriptor;
import com.filesys.FileSystem;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class Directory {
    public final static int FILE_NAME_LENGTH = 4;

    public static final int dirSize = 4 + 4;

    public class DirEntry {
        public String file_name;
        public int FDIndex;

        public DirEntry(String file_name, int FDIndex) throws IllegalArgumentException {
            if (file_name.length() != FILE_NAME_LENGTH)
                throw new IllegalArgumentException("file_name.length() != " + FILE_NAME_LENGTH);
            this.file_name = file_name;
            this.FDIndex = FDIndex;
        }
    }

    public LinkedList<DirEntry> entries;

    public Directory() {
        entries = new LinkedList<>();
    }

    public void addEntry(String file_name, int FDIndex) throws Exception {
        if (file_name.length() != FILE_NAME_LENGTH)
            throw new IllegalArgumentException("file_name.length != " + FILE_NAME_LENGTH);
        if (entries.size() == FileSystem.fileDescriptorCount - 1)
            throw new Exception("Directory is full");
        entries.add(new DirEntry(file_name, FDIndex));
    }

    public static void serializeToDisk(Directory directory, DiskIO dio) {
        ByteBuffer block = null;
        int entriesPerBlock = DiskIO.getBlockSize() / Directory.dirSize;
        int curDirBlock = 0;
        for (int i = 0; i < directory.entries.size(); i++) {
            if (block == null) {
                block = ByteBuffer.allocate(DiskIO.getBlockSize());
            }
            String fileName = directory.entries.get(i).file_name;
            for (int k = 0; k < fileName.length(); k++) {
                block.put((byte) fileName.charAt(k));
            }
            block.putInt(directory.entries.get(i).FDIndex);

            if ((i + 1) % entriesPerBlock == 0) {
                dio.write_block(5 + curDirBlock, block);
                curDirBlock++;
                block = null;
            }
        }
        if (block == null) {
            block = ByteBuffer.allocate(DiskIO.getBlockSize());
        }
        block.put((byte) 0);
        dio.write_block(5 + curDirBlock, block);
    }

    public static void deserializeFromDisk(Directory directory, DiskIO dio) throws Exception {
        ByteBuffer blockBuffer;
        byte b;
        int numberOfEntriesInOneBlock = DiskIO.getBlockSize() / Directory.dirSize;
        directory.entries.clear();

        boolean reading = true;
        for (int i = 0; i < FileDescriptor.MAX_NUMBER_OF_BLOCKS && reading; i++) {
            blockBuffer = ByteBuffer.allocate(DiskIO.getBlockSize());
            dio.read_block(5 + i, blockBuffer);
            for (int j = 0; j < numberOfEntriesInOneBlock; j++) {
                b = blockBuffer.get();
                if (b == 0) {
                    reading = false;
                    break;
                } else {
                    String fileName = "";
                    fileName += (char) b;
                    fileName += (char) blockBuffer.get();
                    fileName += (char) blockBuffer.get();
                    fileName += (char) blockBuffer.get();

                    directory.addEntry(fileName, blockBuffer.getInt());
                }
            }
        }
    }
}
