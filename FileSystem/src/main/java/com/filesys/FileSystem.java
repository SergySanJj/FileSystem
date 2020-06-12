package com.filesys;

import com.filesys.disk.Directory;

import java.io.File;
import java.io.PrintStream;
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

    private PrintStream printStream;

    public FileSystem(DiskIO dio, PrintStream printStream) {
        this.printStream = printStream;
        this.dio = dio;
    }

    public void initFileSystem() {
        bitmap = new BitSet(bitmapByteLength * 8);
        bitmap.set(0, 8, true);

        fileDescriptors = new FileDescriptor[fileDescriptorCount];
        directory = new Directory();

        oft = new OpenFileTable();
        oft.getHandlers()[0] = new OpenFileTable.FileHandler();
        oft.getHandlers()[0].fileDescr = 0;
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
        for (int i = 1; i < oft.getHandlers().length; i++) {
            if (oft.getHandlers()[i] != null)
                closeFile(i);
        }

        dio.write_block(0, ByteBuffer.wrap(bitsetToByteArray(bitmap)));
        FileDescriptor.serializeToDisk(fileDescriptors, dio);
        Directory.serializeToDisk(directory, dio);

        dio.saveAs(diskName);
    }

    public void closeFile(int fileHandlerIndex) {
        if (fileHandlerIndex <= 0) {
            errorDrop("Open files indexing starts from 1");
            return;
        } else if (checkOFTIndex(fileHandlerIndex) == ERR) {
            errorDrop("Open file index does not exist");
            return;
        }

        OpenFileTable.FileHandler fileHandler = oft.getHandlers()[fileHandlerIndex];

        if (fileDescriptors[fileHandler.fileDescr].fileLen > 0 && fileHandler.bufferModified) {
            FileDescriptor fileDescriptor = fileDescriptors[fileHandler.fileDescr];

            int currentFileBlock = fileHandler.fileBlockInBuffer;
            if (isEOF(fileHandler.fileDescr)) {
                currentFileBlock--;
            }

            try {
                int currentDiskBlock = fileDescriptor.blockNumbers[currentFileBlock];
                dio.write_block(currentDiskBlock, oft.getHandlers()[fileHandlerIndex].currData);
            } catch (Exception e) {// pass
            }
        }

        oft.getHandlers()[fileHandlerIndex] = null;

        printStream.println("File " + fileHandlerIndex + " closed");
    }

    public void createFile(String fileName) {
        fileName = fillToFileNameLen(fileName);
        if (fileName.length() > Directory.maxFileName) {
            errorDrop("File name must be less than " + Directory.maxFileName + " long");
            return;
        } else if (directory.getFiles().size() == FileSystem.fileDescriptorCount - 1) {
            errorDrop("No more files can be created");
            return;
        }

        int descriptorIndex = getFreeDescriptorIndex();
        if (descriptorIndex == -1) {
            errorDrop("No more files can be created");
            return;
        }

        if (fileExists(fileName)) {
            errorDrop("File " + fileName + " already exists");
            return;
        }

        try {
            directory.addEntry(fileName, descriptorIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fileDescriptors[descriptorIndex] = new FileDescriptor();
        printStream.println("File " + fileName + " created");
    }

    public boolean fileExists(String fileName) {
        boolean fileExists = false;
        for (Directory.DirFile dirFile : directory.getFiles()) {
            if (dirFile.getFileName().equals(fileName)) {
                fileExists = true;
                break;
            }
        }
        return fileExists;
    }

    private int getFreeDescriptorIndex() {
        for (int i = 0; i < fileDescriptorCount; i++) {
            if (fileDescriptors[i] == null) return i;
        }
        return -1;
    }

    public void open(String fileName) {

        int descriptorIndex = directory.getFileDescriptorIndex(fileName);
        if (descriptorIndex == -1) {
            errorDrop("No such file " + fileName);
            return;
        }

        if (oft.handlerIndex(descriptorIndex) != -1) {
            errorDrop("File has been already opened.");
            return;
        }

        int oftIndex = oft.findFreeHandler();
        if (oftIndex == -1) {
            return;
        }

        oft.getHandlers()[oftIndex] = new OpenFileTable.FileHandler();
        oft.getHandlers()[oftIndex].fileDescr = descriptorIndex;
        oft.getHandlers()[oftIndex].currentPosition = 0;

        if (fileDescriptors[descriptorIndex].fileLen > 0) {
            ByteBuffer temp = ByteBuffer.allocate(DiskIO.getBlockSize());
            try {
                dio.read_block(fileDescriptors[descriptorIndex].blockNumbers[0], temp);
                oft.getHandlers()[oftIndex].fileBlockInBuffer = 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
            oft.getHandlers()[oftIndex].currData = temp;
        }
        printStream.println("File " + fileName + " opened, index=" + oftIndex);
    }


    public void destroy(String fileName) {
        int descriptorIndex = directory.getFileDescriptorIndex(fileName);
        if (descriptorIndex == -1) {
            errorDrop("No such file " + fileName);
            return;
        }

        int oftIndex = oft.handlerIndex(descriptorIndex);
        if (oftIndex != -1) {
            closeFile(oftIndex);
        }

        int[] fileBlocks = fileDescriptors[descriptorIndex].blockNumbers;
        for (int block : fileBlocks) {
            if (block != -1) {
                try {
                    dio.write_block(block, ByteBuffer.allocate(64));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                bitmap.set(block, false);
            }
        }

        int dirEntryIndex = directory.getDirectoryEntryIndex(descriptorIndex, fileDescriptorCount);
        directory.getFiles().remove(dirEntryIndex);

        fileDescriptors[descriptorIndex] = null;

        printStream.println("File " + fileName + " deleted");
    }

    public void displayDirectory() {
        for (Directory.DirFile dirFile : directory.getFiles()) {
            String fileName = dirFile.getFileName();
            int fileLength = fileDescriptors[dirFile.getDescriptorIndex()].fileLen;

            printStream.println("\t" + fileName + " <" + fileLength + ">");
        }
    }

    public int write(int oftIndex, byte[] memArea, int count) {
        if (checkOFTIndex(oftIndex) == ERR || count < 0) return ERR;

        if (count == 0) {
            return 0;
        }

        OpenFileTable.FileHandler fileHandler = oft.getHandlers()[oftIndex];
        FileDescriptor fileDescriptor = fileDescriptors[fileHandler.fileDescr];


        if (fileHandler.currentPosition == fileEnd) {
            return 0;
        }

        if (saveBuffer(this, fileHandler, fileDescriptor) == ERR) return ERR;

        int buffPos = fileHandler.currentPosition % DiskIO.getBlockSize();
        int resPos = 0;

        int writtenCount = 0;

        freeData(fileHandler, fileDescriptor);

        for (int i = 0; i < count && i < memArea.length; i++) {
            if (buffPos == DiskIO.getBlockSize()) {
                if (fileHandler.fileBlockInBuffer < 2) {
                    buffPos = 0;
                    saveBuffer(this, fileHandler, fileDescriptor);
                } else {
                    break;
                }
            }

            fileHandler.currData.put(buffPos, memArea[resPos]);
            fileHandler.bufferModified = true;

            writtenCount++;
            buffPos++;
            resPos++;
            fileHandler.currentPosition++;
        }

        return writtenCount;
    }

    public void freeData(OpenFileTable.FileHandler fileHandler, FileDescriptor fileDescriptor) {
        if (fileDescriptor.fileLen == 0) {
            int newBlock = getFreeDataBlockNumber();
            fileHandler.fileBlockInBuffer = 0;
            fileDescriptor.blockNumbers[fileHandler.fileBlockInBuffer] = newBlock;
            fileDescriptor.fileLen += DiskIO.getBlockSize();
            bitmap.set(newBlock, true);
        }
    }

    private int getFreeDataBlockNumber() {
        for (int i = 8; i < 64; i++) {
            if (!bitmap.get(i)) {
                return i;
            }
        }
        return -1;
    }

    private static int saveBuffer(FileSystem fileSystem,
                                  OpenFileTable.FileHandler fileHandler,
                                  FileDescriptor fileDescriptor) {
        if (fileHandler.fileBlockInBuffer == -1)
            return Success;
        if (fileHandler.fileBlockInBuffer != (fileHandler.currentPosition / DiskIO.getBlockSize())) {
            if (fileHandler.bufferModified) {
                int diskBlock = fileDescriptor.blockNumbers[fileHandler.fileBlockInBuffer];
                try {
                    fileSystem.dio.write_block(diskBlock, fileHandler.currData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                int newFileBlock = fileHandler.currentPosition / DiskIO.getBlockSize();

                if (fileDescriptor.blockNumbers[newFileBlock] == -1) {
                    int newDiskBlock = fileSystem.getFreeDataBlockNumber();
                    if (newDiskBlock == -1) {
                        return ERR;
                    }
                    fileDescriptor.blockNumbers[newFileBlock] = newDiskBlock;
                    fileDescriptor.fileLen += DiskIO.getBlockSize();
                    fileSystem.bitmap.set(newDiskBlock, true);
                }

                ByteBuffer temp = ByteBuffer.allocate(DiskIO.getBlockSize());
                fileSystem.dio.read_block(fileDescriptor.blockNumbers[newFileBlock], temp);
                fileHandler.currData = temp;
                fileHandler.bufferModified = false;
                fileHandler.fileBlockInBuffer = newFileBlock;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return Success;
    }

    public int read(int oftInde, ByteBuffer result, int count) {
        if (checkOFTIndex(oftInde) == ERR || count < 0)
            return ERR;

        if (count == 0) {
            return 0;
        }

        OpenFileTable.FileHandler fileHandler = oft.getHandlers()[oftInde];
        FileDescriptor fileDescriptor = fileDescriptors[fileHandler.fileDescr];

        if (isEOF(fileHandler.fileDescr) || fileDescriptor.fileLen == 0) {
            return ERR;
        }

        if (saveBuffer(this, fileHandler, fileDescriptor) == ERR) return ERR;

        int buffPos = fileHandler.currentPosition % DiskIO.getBlockSize();
        int resPos = 0;

        int readCount = 0;

        for (int i = 0; i < count && i < result.array().length; i++) {
            if (fileHandler.currentPosition == fileDescriptor.fileLen) {
                break;
            } else {
                if (buffPos == DiskIO.getBlockSize()) {
                    saveBuffer(this, fileHandler, fileDescriptor);
                    buffPos = 0;
                }

                result.put(resPos, fileHandler.currData.get(buffPos));

                readCount++;
                buffPos++;
                resPos++;
                fileHandler.currentPosition++;
            }
        }

        return readCount;
    }

    public static void dropDisk(String diskName, PrintStream printStream) {
        File file = new File(diskName + ".txt");

        if (file.delete()) {
            printStream.println(diskName + " deleted");
        } else {
            printStream.println("Drop operation failed");
        }
    }


    public void fileSeek(int oftInde, int pos) {
        if (checkOFTIndex(oftInde) == ERR) {
            errorDrop("No index " + oftInde);
            return;
        }
        FileDescriptor fileDescriptor = fileDescriptors[oft.getHandlers()[oftInde].fileDescr];
        if (pos > fileDescriptor.fileLen || pos < 0) {
            errorDrop("Pos value overflow " + pos + " of [0.." + fileDescriptor.fileLen + "]");
            return;
        }
        oft.getHandlers()[oftInde].currentPosition = pos;
        printStream.println("Current position is " + oft.getHandlers()[oftInde].currentPosition);
    }

    private int checkOFTIndex(int oftIndex) {
        if (oftIndex == ERR)
            return ERR;
        if (oftIndex <= 0 || oftIndex >= oft.getHandlers().length || oft.getHandlers()[oftIndex] == null)
            return ERR;
        return Success;
    }

    private boolean isEOF(int descriptorIndex) {
        int fileLength = fileDescriptors[descriptorIndex].fileLen;
        int position = oft.getHandlers()[oft.handlerIndex(descriptorIndex)].currentPosition;

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
        System.arraycopy(temp, 1, output, 0, 64);
        return output;
    }

    private void errorDrop(String msg) {
        printStream.println("Error occurred: \n\t" + msg);
    }

    public static String fillToFileNameLen(String fileName) {
        while (fileName.length() < Directory.maxFileName)
            fileName += " ";
        return fileName;
    }

    public static final int Success = 1;
    public static final int ERR = -3;
    private static final int fileEnd = 3 * DiskIO.getBlockSize();
}


