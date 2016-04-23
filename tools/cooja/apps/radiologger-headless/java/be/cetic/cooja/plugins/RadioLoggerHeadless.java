/*
 * Copyright (c) 2013, CETIC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 */

package be.cetic.cooja.plugins;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.Collection;
import java.util.ArrayList;
import java.io.File;
import java.io.RandomAccessFile;

import org.jdom.Element;

import se.sics.cooja.ClassDescription;
import se.sics.cooja.GUI;
import se.sics.cooja.PluginType;
import se.sics.cooja.RadioConnection;
import se.sics.cooja.RadioMedium;
import se.sics.cooja.RadioPacket;
import se.sics.cooja.Simulation;
import se.sics.cooja.VisPlugin;
import se.sics.cooja.interfaces.Radio;
//import se.sics.cooja.plugins.analyzers.PacketAnalyser;
import se.sics.cooja.plugins.analyzers.PcapExporter;

/**
 * Radio Logger which exports a pcap file only.
 * It was designed to support radio logging in COOJA's headless mode.
 * Based on Fredrik Osterlind's RadioLogger.java
 *
 * Based on RadioLoggerHeadless.java by primary
 * @author Laurent Deru
 * The work is the stepping stone for keeping separately between destined- and
 * overheard packets in this source code.
 *
 * @author Pasakorn Tiwatthanont
 * @revised 2015/06 - 2016
 */
@ClassDescription("Headless radio logger")
@PluginType(PluginType.SIM_PLUGIN)
public class RadioLoggerHeadless extends VisPlugin {
    private static final long serialVersionUID = -6927091711697081353L;

    private static final String pcapExt = ".pcap";

    private final Simulation simulation;
    private RadioMedium     radioMedium;
    private Observer        radioMediumObserver;
    private File pcapFile;
    private PcapExporter    pcapSendingExporter;
    private PcapExporter [] pcapReceivingExporter;
    private int motesCount;


    /** ***********************************************************************
     * Constructor
     *
     * @param simulationToControl
     * @param cooja
     */
    public RadioLoggerHeadless(final Simulation simulationToControl, final GUI cooja) {
        super("Radio messages", cooja, false);
        System.err.println("Starting headless radio logger");

        simulation = simulationToControl;
        radioMedium = simulation.getRadioMedium();
        motesCount = simulation.getMotesCount();

        try {
            pcapSendingExporter = new PcapExporter();
            pcapReceivingExporter = new PcapExporter[motesCount];
            for (int i = 0; i < motesCount; i++)
                pcapReceivingExporter[i] = new PcapExporter();
        } catch (IOException e) {
            System.err.println("RadioLogger opens PCAP files error!");
            e.printStackTrace();
        }

        startRadioObservation();
    }

    /** ***********************************************************************
     * Start the observer
     */
    public void startRadioObservation() {

        radioMediumObserver = new Observer() {
            @Override
            public void update(Observable obs, Object obj) {
                RadioConnection conn = radioMedium.getLastConnection();
                if (conn == null)
                    return;

                RadioPacket radioTxPacket = conn.getSource().getLastPacketTransmitted();

                /**
                 * [iPAS]: From receiver's view  */
                //for (Radio radioRx : conn.getAllDestinations()) {  // All destination radios including interfered ones
                for (Radio radioRx : conn.getDestinations()) {  // All non-interfered radios
                    //RadioPacket radioRxPacket = radioRx.getLastPacketReceived();  // It is always null !?
                    try {
                        int i = radioRx.getMote().getID() - 1;
                        pcapReceivingExporter[i].exportPacketData( radioTxPacket.getPacketData() );
                    } catch (IOException e) {
                        System.err.println("Cannot export PCAP for receivers");
                        e.printStackTrace();
                    }
                }

                /**
                 * [iPAS]: From sender's view  */
                try {
                    pcapSendingExporter.exportPacketData( radioTxPacket.getPacketData() );
                } catch (IOException e) {
                    System.err.println("Cannot export PCAP for senders");
                    e.printStackTrace();
                }
            }
        };

        radioMedium.addRadioMediumObserver(radioMediumObserver);  // Add to the medium
    }

    /** ***********************************************************************
     * Stop the observer
     */
    public void stopRadioObservation() {
        if (radioMediumObserver != null)
            radioMedium.deleteRadioMediumObserver(radioMediumObserver);
    }

    /** ***********************************************************************
     * Called before close.
     */
    @Override
    public void closePlugin() {
        stopRadioObservation();
    }

    /** ***********************************************************************
     * Create PCAP output files
     */
    private void createPcapFiles(String fpath) throws IOException {
        pcapFile = simulation.getGUI().restorePortablePath(new File(fpath + pcapExt));
        pcapSendingExporter.openPcap(pcapFile);

        for (int i = 0; i < motesCount; i++)
            pcapReceivingExporter[i].openPcap(
                    simulation.getGUI().restorePortablePath(
                            new File(fpath + "_" + (i + 1) + pcapExt)));
    }

    /** ***********************************************************************
     * Restart statistical values
     */

    /*
     * XXX: This method is not work, still don't know why.
     * Guess that multiple accessing to the file is a cause.
     *
    private void setFileZero(File f) {
        try {
            System.err.println("Clear PCAP file " + f.getName());

            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            raf.setLength(0);
        } catch (Exception e) {
            System.err.println("Exception on clearing" + e);
        }
    }

    public void restartStatistics() {
        try {  // Make it clean & recreate the headers
            setFileZero(pcapSendingExporter.pcapFile);
            pcapSendingExporter.exportHeader();
            for (int i = 0; i < motesCount; i++) {
                setFileZero(pcapReceivingExporter[i].pcapFile);
                pcapReceivingExporter[i].exportHeader();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    public void restartStatistics() {
        try {  // Make it clean & recreate the headers
            pcapSendingExporter.reOpenPcap();
            for (int i = 0; i < motesCount; i++) {
                pcapReceivingExporter[i].reOpenPcap();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** ***********************************************************************
     * Called on opening the plug-in.
     */
    @Override
    public boolean setConfigXML(Collection<Element> configXML, boolean visAvailable) {
        for (Element element : configXML) {
            String name = element.getName();
            if (name.equals("pcap_file")) {

                /**
                 * Read file-path configuration  */
                String fpath = element.getText();
                if (fpath.lastIndexOf(pcapExt) >= 0)
                    fpath = fpath.substring(0, fpath.lastIndexOf(pcapExt));

                /**
                 * Setup the exporter  */
                try {
                    createPcapFiles(fpath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }

        return true;
    }

    /** ***********************************************************************
     * Called before closing the plug-in.
     */
    @Override
    public Collection<Element> getConfigXML() {
        ArrayList<Element> configXML = new ArrayList<Element>();
        Element element;
        element = new Element("pcap_file");
        if (pcapFile == null) {
            pcapFile = (pcapSendingExporter.pcapFile != null) ?
                        pcapSendingExporter.pcapFile :
                        new File("determine_file_here.pcap");
        }
        File file = simulation.getGUI().createPortablePath(pcapFile);
        element.setText(file.getPath().replaceAll("\\\\", "/"));
        element.setAttribute("EXPORT", "discard");
        configXML.add(element);

        return configXML;
    }

}
