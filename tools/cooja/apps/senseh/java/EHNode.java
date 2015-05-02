import org.apache.log4j.Logger;
import se.sics.cooja.ClassDescription;
import se.sics.cooja.Mote;
import se.sics.cooja.Simulation;
import se.sics.cooja.mspmote.SkyMote;


@ClassDescription("A node with Energy Harvesting")
public class EHNode{

  private static Logger logger = Logger.getLogger(EHNode.class);

  private Simulation simulation;  
  private Mote mote;
  private EHSystem ehSys;
  private Pin storageMotePin;
  private PowerConsumption consumption; 
  private int nodeID; 
  
  public EHSystem getEHSystem() {
    return ehSys; 
  }
  
  public PowerConsumption getPowerConsumption() {
    return consumption;
  }
  

  public EHNode(int nodeID, Simulation simulation, String configFilePath) {
    this.nodeID = nodeID + 1;
    this.simulation = simulation;
    mote = simulation.getMote(nodeID);
    
    ehSys = new EHSystem(configFilePath, this.nodeID);
    storageMotePin = new Pin(ehSys.getStorage(), (SkyMote)mote);
    consumption = new PowerConsumption(simulation, mote, ehSys.getVoltage());
  }

  public void printStats() {
    System.out.println(this.nodeID + "\t" + 
        ehSys.getTotalHarvestedEnergy() + "\t" + 
        consumption.getTotalConsumedEnergy());
  }

  public void updateCharge(){ // iPAS: the EH system model of the node
    chargeStorage();
    dischargeConsumption();
    consumption.setVoltage(ehSys.getVoltage()); 
  }

  private void chargeStorage(){
    ehSys.harvestCharge();
  }  

  private void dischargeConsumption(){
    double energyConsumed = ehSys.getChargeInterval() /*sec*/ * consumption.getAveragePower() /*mW*/;
    ehSys.consumeCharge(energyConsumed);
    consumption.reset();
  }
}
