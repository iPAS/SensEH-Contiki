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
import java.util.Properties;
import java.util.Collection;
import java.util.ArrayList;
import java.io.File;

import org.jdom.Element;

import se.sics.cooja.ClassDescription;
import se.sics.cooja.GUI;
import se.sics.cooja.Plugin;
import se.sics.cooja.PluginType;
import se.sics.cooja.RadioConnection;
import se.sics.cooja.RadioMedium;
import se.sics.cooja.RadioPacket;
import se.sics.cooja.Simulation;
import se.sics.cooja.VisPlugin;
import se.sics.cooja.interfaces.Radio;
//import se.sics.cooja.plugins.analyzers.PacketAnalyser;
import se.sics.cooja.plugins.analyzers.PcapExporter;
import se.sics.cooja.util.StringUtils;
import sun.security.util.Length;

/**
 * Radio Logger which exports a pcap file only.
 * It was designed to support radio logging in COOJA's headless mode.
 * Based on Fredrik Osterlind's RadioLogger.java
 *
 * @author Laurent Deru
 */
@ClassDescription("Headless radio logger")
@PluginType(PluginType.SIM_PLUGIN)
public class RadioLoggerHeadless extends VisPlugin {
    private static final long serialVersionUID = -6927091711697081353L;

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
            e.printStackTrace();
        }

        radioMedium.addRadioMediumObserver(radioMediumObserver = new Observer() {
            @Override
            public void update(Observable obs, Object obj) {
                RadioConnection conn = radioMedium.getLastConnection();
                if (conn == null) {
                    return;
                }
                RadioPacket radioTxPacket = conn.getSource().getLastPacketTransmitted();

                /**
                 * From receiver's view
                 * [iPAS]: xxx  */
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
                 * From sender's view 
                 * [iPAS]: xxx  */
                try {
                    pcapSendingExporter.exportPacketData( radioTxPacket.getPacketData() );
                } catch (IOException e) {
                    System.err.println("Cannot export PCAP for senders");
                    e.printStackTrace();
                }
            }
        });
    }

    /** ***********************************************************************
     * Called before close.
     */
    @Override
    public void closePlugin() {
        if (radioMediumObserver != null)
            radioMedium.deleteRadioMediumObserver(radioMediumObserver);
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
                final String fext = ".pcap";
                String fname = element.getText();
                if (fname.lastIndexOf(fext) >= 0)
                    fname = fname.substring(0, fname.lastIndexOf(fext));

                /**
                 * Setup the exporter  */
                pcapFile = simulation.getGUI().restorePortablePath(new File(fname + fext));
                try {
                    pcapSendingExporter.openPcap(pcapFile);

                    for (int i = 0; i < motesCount; i++)
                        pcapReceivingExporter[i].openPcap(
                                simulation.getGUI().restorePortablePath(
                                        new File(fname + "_" + (i+1) + fext)));

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
