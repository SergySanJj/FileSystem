package com.filesys;

import java.nio.ByteBuffer;

public class FileDescriptor {
    int fileLengthInBytes;
    int[] blockNumbers;
    public static final int MAX_NUMBER_OF_BLOCKS = 3;

    public static final int descriptorSize = 16;

    public FileDescriptor() {
        fileLengthInBytes = 0;
        blockNumbers = new int[]{-1, -1, -1};
    }

    public FileDescriptor(int fileLengthInBytes, int[] blockNumbers) {
        this.fileLengthInBytes = fileLengthInBytes;
        this.blockNumbers = blockNumbers;
    }

    public static void serializeToDisk(FileDescriptor[] fileDescriptors, DiskIO dio) {
        ByteBuffer block;
        int fdsPerBlock = DiskIO.getBlockSize() / FileDescriptor.descriptorSize;
        int fdsTotalBlocks = fileDescriptors.length * FileDescriptor.descriptorSize / DiskIO.getBlockSize();
        for (int i = 0; i < fdsTotalBlocks; i++) {
            block = ByteBuffer.allocate(DiskIO.getBlockSize());
            for (int j = 0; j < fdsPerBlock; j++) {
                int currentDescriptor = i * fdsPerBlock + j;
                if (fileDescriptors[currentDescriptor] == null) {
                    block.putInt(-1);
                    block.putInt(-1);
                    block.putInt(-1);
                    block.putInt(-1);
                } else {
                    block.putInt(fileDescriptors[currentDescriptor].fileLengthInBytes);
                    block.putInt(fileDescriptors[currentDescriptor].blockNumbers[0]);
                    block.putInt(fileDescriptors[currentDescriptor].blockNumbers[1]);
                    block.putInt(fileDescriptors[currentDescriptor].blockNumbers[2]);
                }
            }
            dio.write_block(i + 1, block);
        }
    }

    public static void deserializeFromDisk(DiskIO dio, FileDescriptor[] fileDescriptors) {
        ByteBuffer blockBuffer;
        int fdsPerBlock = DiskIO.getBlockSize() / FileDescriptor.descriptorSize;
        int fdsTotalBlocks = fileDescriptors.length * FileDescriptor.descriptorSize / DiskIO.getBlockSize();
        int fileLen;
        int[] blocks;
        for (int i = 0; i < fdsTotalBlocks; i++) {
            blockBuffer = ByteBuffer.allocate(DiskIO.getBlockSize());
            dio.read_block(i + 1, blockBuffer);
            for (int j = 0; j < fdsPerBlock; j++) {
                fileLen = blockBuffer.getInt();
                if (fileLen == -1) {
                    blockBuffer.getInt();
                    blockBuffer.getInt();
                    blockBuffer.getInt();
                } else {
                    blocks = new int[FileDescriptor.MAX_NUMBER_OF_BLOCKS];
                    blocks[0] = blockBuffer.getInt();
                    blocks[1] = blockBuffer.getInt();
                    blocks[2] = blockBuffer.getInt();
                    fileDescriptors[i * fdsPerBlock + j] = new FileDescriptor(fileLen, blocks);
                }
            }
        }
    }
}
