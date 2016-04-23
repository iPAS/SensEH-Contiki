package se.sics.cooja.plugins.analyzers;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Adopted from PcapExporter.java of Cooja simulator which
 *  is created by
 * @author Fredrik Osterlind.
 *
 * Revised from the original version by iPAS so that
 * it supports NEW RadioLoggerHeadless version.
 *
 * @author Pasakorn Tiwatthanont
 */
public class PcapExporter {

    DataOutputStream out;
    public File pcapFile;

    public PcapExporter() throws IOException {
    }

    /**
     * Open & close PCAP data file.
     * @throws IOException
     */
    public void openPcap() throws IOException {
        pcapFile = new File("radiolog-" + System.currentTimeMillis() + ".pcap");
        openPcap(pcapFile);
    }

    public void openPcap(DataOutputStream os) throws IOException {
        exportHeader(os);
        System.out.println("Opened PCAP file!");
    }

    public void openPcap(File file) throws IOException {
        pcapFile = file;
        out = new DataOutputStream(new FileOutputStream(file));
        openPcap(out);
    }

    public void closePcap() throws IOException {
        out.close();
    }

    public void reOpenPcap() throws IOException {
        closePcap();
        openPcap(pcapFile);
    }

    /**
     * Export header and packet data.
     * @throws IOException
     */
    public void exportHeader() throws IOException {
        exportHeader(out);
        System.out.println("Exported header!");
    }

    public void exportHeader(DataOutputStream out) throws IOException {
        /* pcap header */
        out.writeInt(0xa1b2c3d4);
        out.writeShort(0x0002);
        out.writeShort(0x0004);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(4096);
        out.writeInt(195); /* 195 for LINKTYPE_IEEE802_15_4 */
        out.flush();
    }

    public void exportPacketData(byte[] data) throws IOException {
        if (out == null) {
            openPcap();
        }
        try {
            /* pcap packet header */
            out.writeInt((int) System.currentTimeMillis() / 1000);
            out.writeInt((int) ((System.currentTimeMillis() % 1000) * 1000));
            out.writeInt(data.length);
            out.writeInt(data.length+2);
            /* and the data */
            out.write(data);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
