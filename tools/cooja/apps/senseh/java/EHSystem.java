import java.util.Properties;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * @author raza
 * reads the configuration of the full energy harvesting system
 * and intializes the system
 */

// EHSystem put all the pieces together
public class EHSystem {

	private double totalHarvestedEnergy;
	private EnergySource source;
	private Harvester harvester;
	private EnergyStorage storage;
	private EnvironmentalDataProvider enviornmentalDataProvider; 
	
	private double chargeInterval;
	private int nodeID; 

	public double getChargeInterval (){
		return chargeInterval;
	}

	public EnergyStorage getStorage(){
    return storage;
  }

  public double getVoltage() {
    return storage.getVoltage() * storage.getNumStorages();
  }
  
  public Harvester getHarvester() {
    return harvester;
  }

  
	public EHSystem(String configFile, int node){
    this.nodeID = node;
    totalHarvestedEnergy = 0;
    source = null;
    harvester = null;
    storage = null;
    enviornmentalDataProvider = null;
		//System.out.println (configFile + "\t" +node); 

    // Load configuration
    Properties config = new Properties();
    try {
      FileInputStream fis = new FileInputStream(configFile);
      config.load(fis);
      fis.close();
    } catch (FileNotFoundException e) {
      System.err.println("Energy Harvesting System Configuration file " + configFile
                         + " could not be read.. Exiting...");
      System.exit(-1);
    } catch (IOException e) {
      System.err.println("Energy Harvesting System Configuration file " + configFile
                         + " could not be loaded.. Exiting...");
      System.exit(-1);
    }

    // Initializations

    // Initializing energy source
    if (config.getProperty("source.type").equalsIgnoreCase("photovoltaic")) {
      source = new PhotovoltaicCell(
          config.getProperty("source.name"), config.getProperty("source.outputpower.lookuptable"));
      PhotovoltaicCell pvSource = (PhotovoltaicCell) source;
      pvSource.setNumCells(Integer.parseInt(config.getProperty("source.num")));
    }
    System.out.println(config.getProperty("source.environment.tracefile.path") + nodeID + ".txt");
	  
    // Initializing environment for energy source
    enviornmentalDataProvider = new LightDataProvider(
        config.getProperty("source.environment.tracefile.path") + nodeID + ".txt",
        config.getProperty("source.environment.tracefile.format.delimiter"),
        Integer.parseInt(config.getProperty("source.environment.tracefile.format.columnno")));
    chargeInterval = Double.parseDouble( //in seconds, defines how frequently charge should be updated 
        config.getProperty("source.environment.sampleinterval"));
    
    // Initializing Harvester
    harvester = new Harvester(
        config.getProperty("harvester.name"),
        config.getProperty("harvester.efficiency.lookuptable"));

    //Initializing energy storage
    if (config.getProperty("storage.type").equalsIgnoreCase("battery")) {
      Battery battery = new Battery(
          config.getProperty("storage.name"), config.getProperty("storage.soc.lookuptable"),
          Double.parseDouble(config.getProperty("battery.capacity")),
          Double.parseDouble(config.getProperty("battery.nominalvoltage")),
          Double.parseDouble(config.getProperty("battery.minoperatingvoltage")));
      battery.setNumBatteries(Integer.parseInt(config.getProperty("storage.num")));
      storage = battery;
    }
    //else if (config.getProperty("storage.type").equalsIgnoreCase("capacitor")){} //TODO
	    
	}
		
	public void harvestCharge(){
	  // TODO: Check the units of different quantities
	  
		// read the next value from environmental trace file
    double envValue = enviornmentalDataProvider.getNext(); // read luxs
		
		// calculate the output power for the source for given environmental conditions
		// TODO: handle the out of range values of outputpower. 
		// If envValue is too large, we should get maximum output power 
		//  that can be taken from source. It should not be arbitrary large
    double sourceOutputPower = source.getOutputPower(envValue) / 1000; // microWatts / 1000 = milliWatts
		//System.out.println ("Power  = "+ sourceOutputPower + " mW");

    // get current cummulative voltage for all batteries
    double volts = storage.getVoltage() * storage.getNumStorages();
    //System.out.println ("Current Voltage  = "+ volts + " V");

    // get the efficiency of the harvester at given volts and output power
    double harvEfficiency = harvester.getEfficiency(sourceOutputPower, volts);
    //System.out.println ("harvester efficiency  = "+ (harvEfficiency *100)+ "%");

		// calculating the charge actually going to the battery
    double energy = source.getOutputEnergy(envValue, chargeInterval) 
        * harvEfficiency / 1000; // micro joules / 1000 = millijoules
    //System.out.println ("energy to battery  = "+ energy);
    //System.out.println (" , lux: " + envValue + ", harv pow "+ (harvEfficiency*sourceOutputPower*1000) + " harv en  "+ energy); 
    //System.out.println ("harvester efficiency  = "+ (harvEfficiency *100)+ "%");
    // add the charge to the battery
    storage.charge(energy);
    totalHarvestedEnergy += energy;
		//System.out.println (storage.getVoltage());
	}

  public double getTotalHarvestedEnergy() {
    return totalHarvestedEnergy;
  }
	
	// To be called by PowerConsumption and Leakage Models periodically
	public void consumeCharge (double energyConsumed){
		storage.discharge(energyConsumed); 
	}

	
	/**
   * Main for testing
   * @param args
   *//*  
  public static void main(String[] args) {
    EHSystem ehSys = new EHSystem(
        "/home/raza/raza@murphysvn/code/java/eclipseIndigo/Senseh/EH.config", 1);
    for (int i = 0; i < 1; i++) {
      ehSys.harvestCharge();
    }
  }*/

}
