package com.filesys;

import com.filesys.disk.Disk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        String diskName = "testDisk";
        DiskIO dio = new DiskIO();
        dio.initialize(diskName);
        String st = "some really long and long intere";
        ByteBuffer byteBuffer = ByteBuffer.allocate(40);
        byteBuffer.put(st.getBytes());
        dio.write_block(17, byteBuffer);
        ByteBuffer res = ByteBuffer.allocate(40);
        dio.read_block(17, res);

        String v = new String( res.array(), StandardCharsets.UTF_8);
        System.out.println(v);
        dio.saveAs(diskName);
        dio.initialize(diskName);
        dio.read_block(17,res);
        v= new String( res.array(), StandardCharsets.UTF_8);
        System.out.println(v);

    }
}
