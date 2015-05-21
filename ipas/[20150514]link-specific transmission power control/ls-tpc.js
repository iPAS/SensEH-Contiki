/*
 * Example Contiki test script (JavaScript).
 * A Contiki test script acts on mote output, such as via printf()'s.
 * The script may operate on the following variables:
 *   se.sics.cooja.Mote mote -- The Mote Mote.java
 *   int id -- The id of the mote
 *   long time -- The current time
 *   String msg -- The message
 *   se.sics.cooja.Simulation sim -- The simulation Simulation.java
 *   se.sics.cooja.Gui gui -- The GUI Gui.java
 */

// Test for 5 mins after tx-matching process
TIMEOUT(541000, showStats()); // DEFAULT=20min; thus, setting new

/*
 * Initialize variables
 */
plugin = sim.getGUI().getStartedPlugin("SensEHGUI");
if (plugin == null) {  
  log.log("No SensEH plugin\n");
  log.testFailed();
}

showStats = function() {
  log.log(plugin.getStatistics() + "\n"); // Extract SensEH statistics 
  log.testOK();
};

GENERATE_MSG(240000, "wait"); // Wait for tx-matching process finishing after 4 minutes 
YIELD_THEN_WAIT_UNTIL(msg.equals("wait")); // The node will be back to work again   

plugin.restartConsumedEnergyStatistics();
plugin.restartStoredEnergyStatistics();
plugin.restartHarvestedEnergyStatistics();  

motes = sim.getMotes();
for(var i = 0; i < motes.length; i++)
  write(motes[i], "Go!");
