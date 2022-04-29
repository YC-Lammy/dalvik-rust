package obbstorage;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import Fuse.Fuse;

import java.io.File;
import java.io.FileOutputStream;

import obbstorage.fat32.BlockDevice;
import obbstorage.fat32.FsDirectory;
import obbstorage.fat32.FsDirectoryEntry;
import obbstorage.fat32.FsFile;
import obbstorage.fat32.fat.FatFileSystem;
import obbstorage.fat32.fat.SuperFloppyFormatter;
import obbstorage.fat32.util.FileDisk;
import obbstorage.obbstorage.EncryptedBlockFile;
import obbstorage.obbstorage.ObbFile;
import obbstorage.obbstorage.PBKDF;


public class ObbMountStorage {
    private String mPath;

    static public void dumpDirectory(FsDirectory dir, int tabStop, File curDirectory) throws IOException {
        Iterator<FsDirectoryEntry> i = dir.iterator();
        while (i.hasNext()) {
            final FsDirectoryEntry e = i.next();
            if (e.isDirectory()) {
                for (int idx = 0; idx < tabStop; idx++)
                    System.out.print(' ');
                if (e.getName().equals(".") || e.getName().equals(".."))
                    continue;
                for (int idx = 0; idx < tabStop; idx++)
                    System.out.print("  ");
                System.out.println("[" + e + "]");
                dumpDirectory(e.getDirectory(), tabStop + 1, new File(curDirectory, e.getName()));
            } else {
                for (int idx = 0; idx < tabStop; idx++) System.out.print("  ");
                System.out.println(e);
                    if (!curDirectory.exists()) {
                        if (false == curDirectory.mkdirs()) {
                            throw new IOException("Unable to create directory: " + curDirectory);
                        }
                    }
                    File curFile = new File(curDirectory, e.getName());
                    if (curFile.exists()) {
                        throw new IOException("File exists: " + curFile);
                    } else {
                        FsFile f = e.getFile();
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(curFile);
                            FileChannel outputChannel = fos.getChannel();
                            ByteBuffer sTempBuf = ByteBuffer.allocate(1024 * 1024);
                            int capacity = sTempBuf.capacity();
                            long length = f.getLength();
                            for (long pos = 0; pos < length; pos++) {
                                int readLength = (int) (length - pos > capacity ? capacity : length - pos);
                                sTempBuf.rewind();
                                sTempBuf.limit(readLength);
                                f.read(pos, sTempBuf);
                                sTempBuf.rewind();
                                while (sTempBuf.remaining() > 0)
                                    outputChannel.write(sTempBuf);
                                pos += readLength;
                            }
                        } finally {
                            if (null != fos) fos.close();
                        }
                    }
                
            }
        }
    }

    public static Path MountOBB(File obbInput, String key) throws Exception{
        ObbFile obbfile = new ObbFile();
        obbfile.readFrom(obbInput);

        BlockDevice fd;

        if (0 != (obbfile.mFlags & ObbFile.OBB_SALTED)){
            if (key == null){
                throw new IOException("Encryted file, key not provided");
            }
            byte[] fishkey = PBKDF.getKey(key, obbfile.mSalt);
            EncryptedBlockFile ebf = new EncryptedBlockFile(fishkey, obbInput, "r");
            fd = new FileDisk(ebf, ebf.getEncryptedFileChannel(), false);
        } else{
            fd = new FileDisk(obbInput, false);
        }

        FatFileSystem fs = SuperFloppyFormatter.get(fd).format();
        
        String name = Long.toString(System.currentTimeMillis());

        return Fuse.get_instance().mountDirectory(name, fs.getRoot());
        //dumpDirectory(fs.getRoot(), 0, target);
        //java.nio.file.FileSystems.newFileSystem(new URI("android://"+ran).normalize(), Map.of("fs", fs, "fd", fd));
    }

    public String mount_path(){
        return mPath;
    } 
}

