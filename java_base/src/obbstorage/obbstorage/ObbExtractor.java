package obbstorage.obbstorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import obbstorage.fat32.BlockDevice;
import obbstorage.fat32.FsDirectory;
import obbstorage.fat32.FsDirectoryEntry;
import obbstorage.fat32.FsFile;
import obbstorage.fat32.fat.BootSector;
import obbstorage.fat32.fat.FatFileSystem;
import obbstorage.fat32.fat.FatType;
import obbstorage.fat32.fat.FatUtils;
import obbstorage.fat32.util.FileDisk;

public class ObbExtractor {

    private ByteBuffer tempBuf = ByteBuffer.allocate(1024 * 1024);

    public boolean extract(String obbPath, String secureKey, String extractionPath, boolean verboseMode) {

        boolean isEncrypted;
        byte[] fishKey = null;
        File directoryFile = null;
        boolean hasOutputDirectory = false;

        if (null == obbPath || obbPath.length() == 0) {
            return false;
        }

        if (extractionPath != null && extractionPath.length() > 0) {
            directoryFile = new File(extractionPath);
            hasOutputDirectory = true;
        }

        ObbFile obbFile = new ObbFile();
        obbFile.readFrom(obbPath);

        System.out.print("Package Name: ");
        System.out.println(obbFile.mPackageName);
        System.out.print("Package Version: ");
        System.out.println(obbFile.mPackageVersion);

        if (0 != (obbFile.mFlags & ObbFile.OBB_SALTED)) {
            System.out.print("SALT: ");
            BigInteger bi = new BigInteger(obbFile.mSalt);
            System.out.println(bi.toString(16));
            System.out.println();
            if (null == secureKey || secureKey.length() == 0) {
                System.out.println("Encrypted file. Please add password.");
                return false;
            }

            try {
                fishKey = PBKDF.getKey(secureKey, obbFile.mSalt);
                bi = new BigInteger(fishKey);
                System.out.println(bi.toString(16));
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            isEncrypted = true;
        } else {
            isEncrypted = false;
        }
        File obbInputFile = new File(obbPath);

        BlockDevice fd;
        try {
            if (isEncrypted) {
                EncryptedBlockFile ebf = new EncryptedBlockFile(fishKey, obbInputFile, "r");
                fd = new FileDisk(ebf, ebf.getEncryptedFileChannel(), true);
            } else {
                fd = new FileDisk(obbInputFile, true);
            }
            final FatFileSystem fatFs = FatFileSystem.read(fd, true);
            final BootSector bs = fatFs.getBootSector();
            final FsDirectory rootDir = fatFs.getRoot();
            if (verboseMode) {
                printVerboseInfo(bs, rootDir);
            }
            dumpDirectory(rootDir, 0, directoryFile, hasOutputDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return true;
    }

    protected void dumpDirectory(FsDirectory dir, int tabStop, File curDirectory, boolean hasOutputDirectory)
            throws
            IOException {
        Iterator<FsDirectoryEntry> i = dir.iterator();
        while (i.hasNext()) {
            final FsDirectoryEntry e = i.next();
            if (e.isDirectory()) {
                for (int idx = 0; idx < tabStop; idx++) {
                    System.out.print(' ');
                }
                if (e.getName().equals(".") || e.getName().equals("..")) {
                    continue;
                }
                for (int idx = 0; idx < tabStop; idx++) {
                    System.out.print("  ");
                }
                System.out.println("[" + e + "]");
                dumpDirectory(e.getDirectory(), tabStop + 1, new File(curDirectory, e.getName()),
                        hasOutputDirectory);
            } else {
                for (int idx = 0; idx < tabStop; idx++) {
                    System.out.print("  ");
                }

                System.out.println(e);

                if (!hasOutputDirectory) {
                    continue;
                }

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
                        int capacity = tempBuf.capacity();
                        long length = f.getLength();
                        for (long pos = 0; pos < length; pos++) {
                            int readLength = (int) (length - pos > capacity ? capacity : length - pos);
                            tempBuf.rewind();
                            tempBuf.limit(readLength);
                            f.read(pos, tempBuf);
                            tempBuf.rewind();
                            while (tempBuf.remaining() > 0) {
                                outputChannel.write(tempBuf);
                            }
                            pos += readLength;
                        }
                    } finally {
                        if (null != fos) fos.close();
                    }
                }
            }
        }
    }

    private void printVerboseInfo(BootSector bs, FsDirectory rootDir) {
        System.out.print("Filesystem Type: ");
        FatType ft = bs.getFatType();
        if (ft == FatType.FAT32) {
            System.out.println("FAT32");
        } else if (ft == FatType.FAT16) {
            System.out.println("FAT16");
        } else if (ft == FatType.FAT12) {
            System.out.println("FAT12");
        } else {
            System.out.println("Unknown");
        }
        System.out.print("           OEM Name: ");
        System.out.println(bs.getOemName());
        System.out.print("   Bytes Per Sector: ");
        System.out.println(bs.getBytesPerSector());
        System.out.print("Sectors per cluster: ");
        System.out.println(bs.getSectorsPerCluster());
        System.out.print("   Reserved Sectors: ");
        System.out.println(bs.getNrReservedSectors());
        System.out.print("               Fats: ");
        System.out.println(bs.getNrFats());
        System.out.print("   Root Dir Entries: ");
        System.out.println(bs.getRootDirEntryCount());
        System.out.print("  Medium Descriptor: ");
        System.out.println(bs.getMediumDescriptor());
        System.out.print("            Sectors: ");
        System.out.println(bs.getSectorCount());
        System.out.print("    Sectors Per Fat: ");
        System.out.println(bs.getSectorsPerFat());
        System.out.print("              Heads: ");
        System.out.println(bs.getNrHeads());
        System.out.print("     Hidden Sectors: ");
        System.out.println(bs.getNrHiddenSectors());
        System.out.print("         Fat Offset: ");
        System.out.println(FatUtils.getFatOffset(bs, 0));
        System.out.println("          RootDir: " + rootDir);
    }
}
