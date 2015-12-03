import org.apache.log4j.Logger;
import se.sics.cooja.ClassDescription;
import se.sics.cooja.Mote;
import se.sics.cooja.Simulation;
import se.sics.cooja.mspmote.SkyMote;


/**
 * SensEH Project
 * Originated by
 * @author raza
 * @see http://usmanraza.github.io/SensEH-Contiki/
 *
 * Adopted and adapted by
 * @author ipas
 * @since 2015-05-01
 */
@ClassDescription("A node with Energy Harvesting")
public class EHNode{

    private static Logger logger = Logger.getLogger(EHNode.class);

    private Simulation simulation;
    private int nodeID;
    private Mote mote;
    private EHSystem ehSys;
    private Pin storageMotePin;
    private PowerConsumption consumption;

    private double lastEnergyConsumed;
    private double lastTotalEnergyConsumed;

    private String configFilePath;
    private SensEHGUI senseh;


    public EHSystem getEHSystem() {
        return ehSys;
    }

    public PowerConsumption getPowerConsumption() {
        return consumption;
    }

    public int getNodeID() {
        return nodeID;
    }

    public double getLastEnergyConsumed() {
        return lastEnergyConsumed;
    }

    public double getLastTotalEnergyConsumed() {
        return lastTotalEnergyConsumed;
    }

    public EHNode(int nodeID, Simulation simulation, String configFilePath, SensEHGUI senseh) {
        this.nodeID = nodeID + 1;
        this.simulation = simulation;
        mote = simulation.getMote(nodeID);
        this.configFilePath = configFilePath;
        this.senseh = senseh;

        ehSys = new EHSystem(this.configFilePath, this.nodeID);
        storageMotePin = new Pin(ehSys.getStorage(), (SkyMote)mote);
        consumption = new PowerConsumption(simulation, mote, ehSys.getVoltage());
    }

    public void updateCharge(){  // [iPAS]: the EH system model of the node
        chargeStorage();
        dischargeConsumption();
        consumption.setVoltage(ehSys.getVoltage());  // Assume that it's fixed, and regulated.

        // TODO [iPAS]: mote.get ... stop mote if drained out
        if (((Battery)ehSys.getStorage()).isDepleted()) {
            //((SkyMote)mote).getCPU().stop()
            //((SkyMote)mote).getCPU().isRunning()
            //((SkyMote)mote).getCPU().reset();

            if (!SensEHGUI.QUIET) {
                String str = "Mote #" + mote.getID() + "'s battery is empty at (ms): "
                        + simulation.getSimulationTimeMillis();
                this.senseh.log.addMessage(str);
                logger.info(str);
            }
        }
    }

    private void chargeStorage(){
        ehSys.harvestCharge();
    }

    private void dischargeConsumption(){
        double energyConsumed = ehSys.getChargeInterval()  /*sec*/
                              * consumption.getAveragePower()  /*mW*/;
        consumption.snapStatistics();  // Snap the consumed energy at the time
        ehSys.consumeCharge(energyConsumed);
        consumption.reset();
    }

}
