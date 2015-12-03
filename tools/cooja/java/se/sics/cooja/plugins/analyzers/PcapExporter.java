package se.sics.cooja.plugins.analyzers;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PcapExporter {

    DataOutputStream out;
    public File pcapFile;


    public PcapExporter() throws IOException {
    }

    public void openPcap() throws IOException {
        pcapFile = new File("radiolog-" + System.currentTimeMillis() + ".pcap");
        openPcap(pcapFile);
    }
    public void openPcap(File file) throws IOException {
        pcapFile = file;
        out = new DataOutputStream(new FileOutputStream(file));
        openPcap(out);
    }
    public void openPcap(DataOutputStream out) throws IOException {
        /* pcap header */
        out.writeInt(0xa1b2c3d4);
        out.writeShort(0x0002);
        out.writeShort(0x0004);
        out.writeInt(0);
        out.writeInt(0);
        out.writeInt(4096);
        out.writeInt(195); /* 195 for LINKTYPE_IEEE802_15_4 */
        out.flush();
        System.out.println("Opened PCAP file!");
    }
    public void closePcap() throws IOException {
        out.close();
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
