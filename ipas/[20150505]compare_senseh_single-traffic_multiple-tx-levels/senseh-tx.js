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

//TIMEOUT(120000, showStats());


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
  //log.testOK();
};

sum = function(prev, curr, i, arr) {
  return prev + curr;
}


marks = [];
for (var i = 0; i < sim.getMotesCount(); i++)
  marks.push(1);

while (1) { // Wait them all to be ready
  //log.log(msg + "\n");
  if (msg.search("Please input Tx level") > -1) {
    marks[id-1] = 0;
    if (marks.reduce(sum) == 0) 
      break;
  }
  YIELD();
}


/*
 * Main script
 */
for (var tx_level = 0; tx_level <= 31; tx_level++) {
  log.log("Tx level = " + tx_level + "\n");
  
  plugin.restartConsumedEnergyStatistics();
  plugin.restartStoredEnergyStatistics();
  plugin.restartHarvestedEnergyStatistics();  
  
  motes = sim.getMotes();
  for(var i = 0; i < motes.length; i++){
    write(motes[i], tx_level); 
  }

  GENERATE_MSG(20000, "wait"); // Yield for another, then wait 
  YIELD_THEN_WAIT_UNTIL(msg.equals("wait")); // The node will be back to work again   
  showStats();  
}

log.testOK();

