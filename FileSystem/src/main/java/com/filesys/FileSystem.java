package com.filesys;

import com.filesys.disk.Directory;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.BitSet;

public class FileSystem {
    public static final int bitmapByteLength = 8;
    public static final int fileDescriptorCount = 16;

    private DiskIO dio;

    private BitSet bitmap;
    private FileDescriptor[] fileDescriptors;
    private Directory directory;
    private OpenFileTable oft;

    public FileSystem(DiskIO dio) {
        this.dio = dio;
    }

    public void initFileSystem() {
        bitmap = new BitSet(bitmapByteLength * 8);
        bitmap.set(0, 8, true);

        fileDescriptors = new FileDescriptor[fileDescriptorCount];
        directory = new Directory();

        oft = new OpenFileTable();
        oft.entries[0] = new OpenFileTable.OFTEntry();
        oft.entries[0].FDIndex = 0;
    }

    public void initEmptyFileSystem() {
        int fdsPerBlock = FileDescriptor.descriptorSize * fileDescriptorCount / DiskIO.getBlockSize();
        fileDescriptors[0] = new FileDescriptor(0, new int[]{fdsPerBlock + 1, fdsPerBlock + 2, fdsPerBlock + 3});
    }

    public void loadFileSystem() throws Exception {
        ByteBuffer blockBuffer = ByteBuffer.allocate(DiskIO.getBlockSize());
        dio.read_block(0, blockBuffer);
        bitmap = BitSet.valueOf(blockBuffer);

        fileDescriptors = new FileDescriptor[fileDescriptorCount];
        FileDescriptor.deserializeFromDisk(dio, fileDescriptors);

        Directory.deserializeFromDisk(directory, dio);
    }


    public void saveFileSystem(String diskName) {
        // close files
        for (int i = 1; i < oft.entries.length; i++) {
            if (oft.entries[i] != null)
                closeFile(i);
        }
        // save fs data
        dio.write_block(0, ByteBuffer.wrap(bitsetToByteArray(bitmap)));
        FileDescriptor.serializeToDisk(fileDescriptors, dio);
        Directory.serializeToDisk(directory, dio);

        // save disk
        dio.saveAs(diskName);
    }

    public int closeFile(int oftEntryIndex) {
        if (oftEntryIndex == 0) {
            return STATUS_ERROR;
        }

        if (checkOFTIndex(oftEntryIndex) == STATUS_ERROR) return STATUS_ERROR;

        OpenFileTable.OFTEntry oftEntry = oft.entries[oftEntryIndex];

        if (fileDescriptors[oftEntry.FDIndex].fileLengthInBytes > 0 && oftEntry.bufferModified) {
            FileDescriptor fileDescriptor = fileDescriptors[oftEntry.FDIndex];

            int currentFileBlock = oftEntry.fileBlockInBuffer;
            if (isPointedToByteAfterLastByte(oftEntry.FDIndex)) {
                currentFileBlock--;
            }
            int currentDiskBlock = fileDescriptor.blockNumbers[currentFileBlock];

            try {
                dio.write_block(currentDiskBlock, oft.entries[oftEntryIndex].RWBuffer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        oft.entries[oftEntryIndex] = null;
        return STATUS_SUCCESS;
    }

    private int checkOFTIndex(int OFTEntryIndex) {
        if (OFTEntryIndex == STATUS_ERROR) {
            return STATUS_ERROR;
        }

        if (OFTEntryIndex <= 0 || OFTEntryIndex >= oft.entries.length || oft.entries[OFTEntryIndex] == null) {
            return STATUS_ERROR;
        }
        return STATUS_SUCCESS;
    }

    private boolean isPointedToByteAfterLastByte(int FDIndex) {
        int fileLength = fileDescriptors[FDIndex].fileLengthInBytes;
        int position = oft.entries[oft.getOFTEntryIndex(FDIndex)].currentPosition;

        boolean fileNotEmpty = (fileLength != 0);
        boolean positionOutOfFile = (position == fileLength);
        return (fileNotEmpty && positionOutOfFile);
    }

    private static byte[] bitsetToByteArray(BitSet bits) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < bits.size(); i++) {
            if (bits.get(i)) {
                stringBuilder.append('1');
            } else {
                stringBuilder.append('0');
            }
        }

        for (int i = 0; i < 448; i++) {
            stringBuilder.append('0');
        }

        return binaryStringToBytes(stringBuilder.toString());
    }

    private static byte[] binaryStringToBytes(String data) {
        byte[] temp = new BigInteger(data, 2).toByteArray();
        byte[] output = new byte[64];
        for (int i = 0; i < 64; i++) {
            output[i] = temp[i + 1];
        }
        return output;
    }

    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_ERROR = -3;
    private static final int END_OF_FILE = 3 * DiskIO.getBlockSize();
}


