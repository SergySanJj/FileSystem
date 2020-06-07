package com.filesys.disk;

import java.io.Serializable;
import java.util.concurrent.CyclicBarrier;

public class Disk implements Serializable {
    public static final int diskCylindersCount = 4;
    public Cylinder[] cylinders;

    public Disk() {
        cylinders = new Cylinder[diskCylindersCount];
        for (int i = 0; i < cylinders.length; i++) cylinders[i] = new Cylinder();
    }

    public int size() {
        return diskCylindersCount * cylinders[0].size();
    }


    public class Cylinder implements Serializable {
        public static final int cylinderSize = 4;
        public Track[] tracks;

        public Cylinder() {
            tracks = new Track[cylinderSize];
            for (int i = 0; i < cylinders.length; i++) tracks[i] = new Track();
        }

        public int size() {
            return cylinderSize * tracks[0].size();
        }

        public class Track implements Serializable {
            public static final int trackSize = 8;
            public Sector[] sectors;

            public Track() {
                sectors = new Sector[trackSize];
                for (int i = 0; i < cylinders.length; i++) sectors[i] = new Sector();
            }

            public int size() {
                return sectors.length * Sector.sectorSize;
            }

            public class Sector implements Serializable {
                public static final int sectorSize = 64;
                public byte[] bytes = new byte[sectorSize];
            }
        }
    }


}
