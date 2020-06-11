package com.filesys;

import com.filesys.disk.Directory;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.BitSet;

public class FileSystem {
    public static final int bitmapByteLength = 8;
    public static final int fileDescriptorCount = 24;

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

    public void loadFileSystem() {
        ByteBuffer blockBuffer = ByteBuffer.allocate(DiskIO.getBlockSize());
        dio.read_block(0, blockBuffer);
        bitmap = BitSet.valueOf(blockBuffer);

        fileDescriptors = new FileDescriptor[fileDescriptorCount];
        FileDescriptor.deserializeFromDisk(dio, fileDescriptors);

        try {
            Directory.deserializeFromDisk(directory, dio);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void closeFile(int oftEntryIndex) {
        if (oftEntryIndex <= 0) {
            errorDrop("Open files indexing starts from 1");
            return;
        } else if (checkOFTIndex(oftEntryIndex) == STATUS_ERROR) {
            errorDrop("Open file index does not exist");
            return;
        }

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

        System.out.println("file " + oftEntryIndex + " closed");
    }

    public void createFile(String fileName) {
        fileName = fillToFileNameLen(fileName);
        if (fileName.length() > Directory.FILE_NAME_LENGTH) {
            errorDrop("File name must be less than " + Directory.FILE_NAME_LENGTH + " long");
            return;
        } else if (directory.entries.size() == FileSystem.fileDescriptorCount - 1) {
            errorDrop("No more files can be created");
            return;
        }

        int FDIndex = getFreeDescriptorIndex();
        if (FDIndex == -1) {
            errorDrop("No more files can be created");
            return;
        }

        boolean doFileExist = false;
        for (Directory.DirEntry dirEntry : directory.entries) {
            if (dirEntry.file_name.equals(fileName)) {
                doFileExist = true;
                break;
            }
        }
        if (doFileExist) {
            errorDrop("File " + fileName + " already exists");
            return;
        }

        try {
            directory.addEntry(fileName, FDIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileDescriptors[FDIndex] = new FileDescriptor();
        System.out.println("File " + fileName + " created");
    }

    private int getFreeDescriptorIndex() {
        for (int i = 0; i < fileDescriptorCount; i++) {
            if (fileDescriptors[i] == null) return i;
        }
        return -1;
    }

    public void open(String symbolicFileName) {

        int FDIndex = directory.getFileDescriptorIndex(symbolicFileName);
        if (FDIndex == -1) {
            errorDrop("No such file " + symbolicFileName);
            return;
        }

        if (oft.getOFTEntryIndex(FDIndex) != -1) {
            errorDrop("File has been already opened.");
            return;
        }

        int OFTEntryIndex = oft.getFreeOFTEntryIndex();
        if (OFTEntryIndex == -1) {
            return;
        }

        oft.entries[OFTEntryIndex] = new OpenFileTable.OFTEntry();
        oft.entries[OFTEntryIndex].FDIndex = FDIndex;
        oft.entries[OFTEntryIndex].currentPosition = 0;

        if (fileDescriptors[FDIndex].fileLengthInBytes > 0) {
            ByteBuffer temp = ByteBuffer.allocate(DiskIO.getBlockSize());
            try {
                dio.read_block(fileDescriptors[FDIndex].blockNumbers[0], temp);
                oft.entries[OFTEntryIndex].fileBlockInBuffer = 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
            oft.entries[OFTEntryIndex].RWBuffer = temp;
        }
        System.out.println("file " + symbolicFileName + " opened, index=" + OFTEntryIndex);
    }


    public void destroy(String symbolicFileName) {
        int FDIndex = directory.getFileDescriptorIndex(symbolicFileName);
        if (FDIndex == -1) {
            errorDrop("No such file " + symbolicFileName);
            return;
        }

        // close file if it is open
        int OFTEntryIndex = oft.getOFTEntryIndex(FDIndex);
        if (OFTEntryIndex != -1) {
            closeFile(OFTEntryIndex);
        }

        // clear file blocks on disk
        int[] fileBlocks = fileDescriptors[FDIndex].blockNumbers;
        for (int block : fileBlocks) {
            if (block != -1) {
                try {
                    dio.write_block(block, ByteBuffer.allocate(64));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // clear bits of bitmap for now empty blocks
                bitmap.set(block, false);
            }
        }

        // remove file from directory
        int dirEntryIndex = directory.getDirectoryEntryIndex(FDIndex, fileDescriptorCount);
        directory.entries.remove(dirEntryIndex);

        // clear file descriptor
        fileDescriptors[FDIndex] = null;

        System.out.println("File " + symbolicFileName + " deleted");
    }

    public void displayDirectory() {
        for (Directory.DirEntry dirEntry : directory.entries) {
            String fileName = dirEntry.file_name;
            int fileLength = fileDescriptors[dirEntry.FDIndex].fileLengthInBytes;

            System.out.println("\t" + fileName + " <" + fileLength + ">");
        }
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

    private void errorDrop(String msg) {
        System.out.println("Error occurred: \n\t" + msg);
    }

    public static String fillToFileNameLen(String fileName) {
        while (fileName.length() < Directory.FILE_NAME_LENGTH)
            fileName += " ";
        return fileName;
    }

    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_ERROR = -3;
    private static final int END_OF_FILE = 3 * DiskIO.getBlockSize();
}


