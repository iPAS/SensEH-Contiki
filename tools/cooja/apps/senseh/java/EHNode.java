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
  

  public EHNode(int nodeID, Simulation simulation, String configFilePath) {
    this.nodeID= nodeID+1;
    this.simulation = simulation;
    ehSys = new EHSystem (configFilePath, (this.nodeID));
    mote = simulation.getMote(nodeID);
    storageMotePin = new Pin (ehSys.getStorage(), (SkyMote)mote); 
    consumption = new PowerConsumption(simulation, mote, ehSys.getVoltage());
  }

  public void printStats(){
	System.out.println (this.nodeID + "\t" + ehSys.getTotalHarvestedEnergy() + "\t" + consumption.getTotalConsumedEnergy()); 
	//System.out.println ("harv\t"+ehSys.getTotalHarvestedEnergy());
	//System.out.println ("cons\t"+consumption.getTotalConsumedEnergy());  
  }

  public void updateCharge(){
    chargeStorage();
    dischargeConsumption();
    consumption.setVoltage(ehSys.getVoltage()); 
  }

  private void chargeStorage(){
    ehSys.harvestCharge();
  }  

  private void dischargeConsumption(){
    double energyConsumed= ehSys.getChargeInterval() /*seconds*/ * consumption.getAveragePower() /*mW*/;
    //System.out.println (ehSys.getChargeInterval());
    ehSys.consumeCharge(energyConsumed);
    //System.out.println ("Consumed energy: "+ energyConsumed + "mJ");
    consumption.reset();
  }

  public EHSystem getEHSystem (){
	return ehSys; 
  }
}
