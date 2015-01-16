import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import org.apache.log4j.Logger;
import org.jdom.Element;
import se.sics.cooja.ClassDescription;
import se.sics.cooja.GUI;
import se.sics.cooja.PluginType;
import se.sics.cooja.Simulation;
import se.sics.cooja.TimeEvent;
import se.sics.cooja.VisPlugin;
import se.sics.cooja.dialogs.MessageList;
import se.sics.cooja.util.StringUtils;
import java.util.ArrayList;
import java.util.Collection;

@ClassDescription("SensEH GUI")
@PluginType(PluginType.SIM_PLUGIN)
public class SensEHGUI extends VisPlugin {

  private static Logger logger = Logger.getLogger(SensEHGUI.class);

  private Simulation simulation;  
  private EHNode[] ehNodes; 

  private long startTime; /* us */
  private ChargeUpdateEvent chargeUpdateEvent;
  private long totalUpdates;


  private File ehConfigFile = null;

  private MessageList log = new MessageList();
  private static final boolean QUIET = false;


  public SensEHGUI(Simulation simulation, final GUI gui) {
    super("SensEH Plugin", gui, false);
    this.simulation = simulation;
    //consumption = new Consumption(simulation); 

    log.addPopupMenuItem(null, true); /* Create message list popup */
    add(new JScrollPane(log));

    if (!QUIET) {
      log.addMessage("Harvesting plugin started at (ms): " + simulation.getSimulationTimeMillis());
      logger.info("Harvesting plugin started at (ms): " + simulation.getSimulationTimeMillis());
    }
    setSize(500,200);
  }

  public void startPlugin() {
    super.startPlugin();

    if (ehConfigFile != null)
      return;
    

    JFileChooser fileChooser = new JFileChooser();
    File suggest = new File(GUI.getExternalToolsSetting("DEFAULT_EH_CONFIG", "/home/user/contiki-2.7/tools/cooja/apps/senseh/config/EH.config"));
    fileChooser.setSelectedFile(suggest);
    fileChooser.setDialogTitle("Select configuration file for harvesting system");
    int reply = fileChooser.showOpenDialog(GUI.getTopParentContainer());
    if (reply == JFileChooser.APPROVE_OPTION) {
      ehConfigFile = fileChooser.getSelectedFile();
      GUI.setExternalToolsSetting("DEFAULT_EH_CONFIG", ehConfigFile.getAbsolutePath());
    }
    if (ehConfigFile == null){
      throw new RuntimeException("No configuration file for harvesting system");
    }
    init(ehConfigFile.getAbsolutePath());
  }

  void init (String configFilePath){
    //setTitle("~~~ TITLE ~~~");
    ehNodes = new EHNode [simulation.getMotesCount()]; 
    for (int i=0; i < simulation.getMotesCount(); i++)
	ehNodes[i] = new EHNode (i, simulation, configFilePath); 
    schedulePeriodicChargeUpdate(); // schedule event to update the charge of all the nodes
   }

  private void schedulePeriodicChargeUpdate() {
      simulation.invokeSimulationThread(new ChargeUpdateTaskScheduler());
      //System.out.println("totalUpdates="+ totalUpdates);
  }

  private class ChargeUpdateTaskScheduler implements Runnable {

     public void run() {
       //System.out.println ("3");
       totalUpdates = 1;
       startTime = simulation.getSimulationTime();
       //logger.debug("periodStart: " + periodStart);
       chargeUpdateEvent = new ChargeUpdateEvent (0); 
       chargeUpdateEvent.execute(startTime + (long)(getChargeInterval()* (1000000)));


       //System.out.println ( "First Event @ " + (startTime + (long)(ehSys.getChargeInterval()* (1000000))) + " us");
       //totalUpdates++;
     }
  }

  private double getChargeInterval(){
       // We assume that charge update interval for ALL nodes is equal
	return ehNodes[0].getEHSystem().getChargeInterval();
  }

  private class ChargeUpdateEvent extends TimeEvent{
    public ChargeUpdateEvent (long t){
	super (t, "charge update event");
    }

    public void execute(long t) {

      // Detect early events: reschedule for later 
      //System.out.println ("t\t"+t + "\tSimTime\t"+simulation.getSimulationTime());
      if (simulation.getSimulationTime() < t) {
        simulation.scheduleEvent(this, startTime + (long)((totalUpdates)*getChargeInterval()* (1000000)));
        return;
      }

      for(int i=0; i<ehNodes.length; i++ ){
	ehNodes[i].updateCharge(); // charge with harvested energy and discharge with power consumption
	if (totalUpdates%30==0)
		ehNodes[i].printStats();
      }

      // Now schedule the next event
      totalUpdates++;
      long nextEventTime = startTime + totalUpdates*(long)(getChargeInterval()* (1000000)); 
      if (simulation.getSimulationTime() <  nextEventTime) {
        simulation.scheduleEvent(this, nextEventTime);
        return;
      }
    }
 }

  public void closePlugin() {
    chargeUpdateEvent.remove();
  }

 
  public Collection<Element> getConfigXML() {
    ArrayList<Element> configXML = new ArrayList<Element>();
    Element element;

    if (ehConfigFile != null) {
      element = new Element("eh_config_file");
      File file = simulation.getGUI().createPortablePath(ehConfigFile);
      element.setText(file.getPath().replaceAll("\\\\", "/"));
      element.setAttribute("EXPORT", "copy");
      configXML.add(element);
    }

    return configXML;
  }
  
  public boolean setConfigXML(Collection<Element> configXML, boolean visAvailable) {
    for (Element element : configXML) {
      String name = element.getName();

      if (name.equals("eh_config_file")) {
        ehConfigFile = simulation.getGUI().restorePortablePath(new File(element.getText()));
        init(ehConfigFile.getAbsolutePath());
      }
    }

    return true;
  }
}
