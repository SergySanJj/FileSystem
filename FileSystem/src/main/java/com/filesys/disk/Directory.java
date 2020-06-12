package com.filesys.disk;

import com.filesys.DiskIO;
import com.filesys.FileDescriptor;
import com.filesys.FileSystem;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class Directory {
    public static final int maxFileName = 4;
    public static final int dirSize = 4 + 4;

    public List<DirFile> getFiles() {
        return files;
    }

    public void setFiles(List<DirFile> files) {
        this.files = files;
    }

    public static class DirFile {
        private String fileName;
        private int descriptorIndex;

        public DirFile(String fileName, int descriptorIndex) {
            this.setFileName(fileName);
            this.setDescriptorIndex(descriptorIndex);
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public int getDescriptorIndex() {
            return descriptorIndex;
        }

        public void setDescriptorIndex(int descriptorIndex) {
            this.descriptorIndex = descriptorIndex;
        }
    }

    private List<DirFile> files;

    public Directory() {
        setFiles(new LinkedList<>());
    }

    public void addEntry(String fileName, int descriptorIndex) {
        if (getFiles().size() == FileSystem.fileDescriptorCount - 1) {
            System.out.println("Error occurred:\n\tDirectory is full");
            return;
        }
        getFiles().add(new DirFile(fileName, descriptorIndex));
    }

    public static void serializeToDisk(Directory directory, DiskIO dio) {
        ByteBuffer block = null;
        int entriesPerBlock = DiskIO.getBlockSize() / Directory.dirSize;
        int curDirBlock = 0;
        for (int i = 0; i < directory.getFiles().size(); i++) {
            if (block == null) {
                block = ByteBuffer.allocate(DiskIO.getBlockSize());
            }
            String fileName = directory.getFiles().get(i).getFileName();
            for (int k = 0; k < fileName.length(); k++) {
                block.put((byte) fileName.charAt(k));
            }
            block.putInt(directory.getFiles().get(i).getDescriptorIndex());

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

    public static void deserializeFromDisk(Directory directory, DiskIO dio) {
        ByteBuffer blockBuffer;
        byte b;
        int numberOfEntriesInOneBlock = DiskIO.getBlockSize() / Directory.dirSize;
        directory.getFiles().clear();

        boolean reading = true;
        for (int i = 0; i < FileDescriptor.maxBlocks && reading; i++) {
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

    public int getFileDescriptorIndex(String fileName) {
        for (DirFile dirFile : getFiles()) {
            if (dirFile.getFileName().equals(FileSystem.fillToFileNameLen(fileName)))
                return dirFile.getDescriptorIndex();
        }
        return -1;
    }

    public int getDirectoryEntryIndex(int descriptorIndex, int fileDescriptorsCount) {
        for (int i = 0; i < fileDescriptorsCount - 1; i++) {
            if (getFiles().get(i) != null && getFiles().get(i).getDescriptorIndex() == descriptorIndex) return i;
        }
        return -1;
    }
}
