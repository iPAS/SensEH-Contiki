import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;


/**
 * SensEH Project
 * Originated by
 * @author raza
 * @see http://usmanraza.github.io/SensEH-Contiki/
 *
 * 'EHSystem' reads the configuration of the full energy harvesting system
 *   and initializes the system.
 *
 * Adopted and adapted by
 * @author ipas
 * @since 2015-05-01
 */
public class EHSystem { // EHSystem put all the pieces together

    private double totalHarvestedEnergy;

    private EnergySource source;
    private Harvester harvester;
    private EnergyStorage storage;
    private EnvironmentalDataProvider enviornmentalDataProvider;

    private double chargeInterval;
    private int nodeID;


    public double getChargeInterval() {
        return chargeInterval;
    }

    public EnergyStorage getStorage() {
        return storage;
    }

    public Harvester getHarvester() {
        return harvester;
    }

    public double getVoltage() {
        return storage.getVoltage() * storage.getNumStorages();
    }

    public double getTotalHarvestedEnergy() {
        return totalHarvestedEnergy;
    }

    public void setTotalHarvestedEnergy(double energy_mj) {  // [iPAS]: for hacking only
        totalHarvestedEnergy = energy_mj;
    }


    public EHSystem(String configFile, int node){
        this.nodeID = node;
        totalHarvestedEnergy = 0;
        source = null;
        harvester = null;
        storage = null;
        enviornmentalDataProvider = null;

        // Load configuration
        Properties config = new Properties();
        try {
            FileInputStream fis = new FileInputStream(configFile);
            //config.load(fis);

            // [iPAS]: Replace [APPS_DIR] with real path
            String coojaStructure = "/tools/cooja/apps";
            String appsDir = "[APPS_DIR not found!]";
            int i = configFile.indexOf(coojaStructure);

            if (i >= 0)   // If be correct
                appsDir = configFile.substring(0, i) + coojaStructure;
            else
                System.err.println(configFile + " NOT in [APPS_DIR] !");

            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            String line;

            Pattern p = Pattern.compile("\\[APPS_DIR\\]");
            StringBuffer sb = new StringBuffer();

            while ((line = br.readLine()) != null) {
                Matcher m = p.matcher(line);

                boolean isFound = m.find();
                while (isFound) {  // Loop through and create a new String with the replacements
                    m.appendReplacement(sb, appsDir);
                    isFound = m.find();
                }

                m.appendTail(sb); // Add the last segment of input to the new String
                sb.append('\n');
            }
            br.close();

            config.load(new StringReader(sb.toString()));
            // iPAS: ---

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

    public void harvestCharge(){ // TODO: Check the units of different quantities
        // Read the next value from environmental trace file
        double envValue = enviornmentalDataProvider.getNext(); // average luxs

        // Calculate the output power for the source for given environmental conditions
        // TODO: handle the out of range values of outputpower.
        // If envValue is too large, we should get maximum output power
        //  that can be taken from source. It should not be arbitrary large
        double sourceOutputPower = source.getOutputPower(envValue) / 1000; // microWatts / 1000 = milliWatts
        //System.out.println ("Power  = "+ sourceOutputPower + " mW");

        // Get current cumulative voltage for all batteries
        double volts = storage.getVoltage() * storage.getNumStorages();
        //System.out.println ("Current Voltage  = "+ volts + " V");

        // Get the efficiency of the harvester at given volts and output power
        double harvEfficiency = harvester.getEfficiency(sourceOutputPower, volts);
        //System.out.println ("harvester efficiency  = "+ (harvEfficiency *100)+ "%");

        // Calculating the charge actually going to the battery
        double energy = source.getOutputEnergy(envValue, chargeInterval) * harvEfficiency / 1000; // mJ

        //System.out.println ("energy to battery  = " + energy);
        //System.out.println (", lux: " + envValue + ", harv pow " +
        //    (harvEfficiency*sourceOutputPower*1000) + " harv en  " + energy);
        //System.out.println ("harvester efficiency = " + (harvEfficiency *100) + "%");

        // Add the charge to the battery
        storage.charge(energy);
        totalHarvestedEnergy += energy;
        //System.out.println (storage.getVoltage());
    }

    // To be called by EHNode.dischargeConsumption() periodically
    //  to drain the power used by PowerConsumption and Leakage Models from Storage.
    // TODO: However, the Leakage Model class have not implemented yet.
    public void consumeCharge(double energyConsumed) {
        storage.discharge(energyConsumed);
    }


    // --------------------------------------------------------------------------
    /**
     * Main for testing
     * @param args
     */
    public static void main(String[] args) {
        EHSystem ehSys = new EHSystem(
                "/home/raza/raza@murphysvn/code/java/eclipseIndigo/Senseh/EH.config", 1);
        for (int i = 0; i < 1; i++) {
            ehSys.harvestCharge();
        }
    }

}
