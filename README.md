# File System
Minimalistic file system

# CLI commands

- cd \<fileName\>
  - create a new file with the name \<fileName\>
  - Output: File \<name\> created
  
- de \<name\>
  - destroy the named file \<fileName\>
  - Output: File \<fileName\> deleted
 
- op \<name\>
  - open the named file \<fileName\> for reading and writing; display an OFT index value
  - Output: File \<fileName\> opened, index=\<index\>
 
- cl \<index\>
  - close the specified file \<index\>
  - Output: File \<index\> closed
 
- rd \<index\> \<count\>
  - sequentially read a number of bytes \<count\> from the specified file \<index\> and display them on the terminal
  - Output: \<count\> bytes read: \<xx...x\>
 
- wr \<index\> \<char\> \<count\>
  - sequentially write \<count\> number of \<char\>s into the specified file \<index\> at its current position
  - Output: \<count\> bytes written
 
- sk \<index\> \<pos\>
  - seek: set the current position of the specified file \<index\> to \<pos\>
  - Output: Current position is \<pos\>
 
- dr
  - directory: list the names of all files and their lengths
  - Output: file0 \<len0\>,..., fileN \<lenN\>
 
- in \<diskName\>
  - create a disk and initialize it using the file \<diskName\> (copy of disk)
  - If file does not exist, create and open directory; output: Disk initialized
  - If file does exist, open directory; output: Disk restored
 
- sv \<disk_cont\>
  - close all files and save the contents of the disk in the file \<disk_cont\>
  - Output: \<File \<index1\> closed\>, .., \<File \<indexN\> closed\>, Disk saved
 
 - drop \<diskName\>
   - delete saved disk image with name \<diskName\> if such exists
   - Output: \<diskName\> deleted

- end
  - ends CLI session
 
- If any command fails, output: help message with commands list
