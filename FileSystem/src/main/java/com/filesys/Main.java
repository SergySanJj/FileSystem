package com.filesys;

import com.filesys.disk.Disk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hi");
        Disk disk = new Disk();
        disk.cylinders[0].tracks[0].sectors[0].bytes[0] = 12;
        disk.cylinders[0].tracks[0].sectors[0].bytes[3] = 12;

        String diskName = "testDisk";

    }
}
