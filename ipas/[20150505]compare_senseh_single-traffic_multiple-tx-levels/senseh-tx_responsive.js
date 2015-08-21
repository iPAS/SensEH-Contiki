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
  plugin.restartConsumption();  
  //log.testOK();
};

sum = function(prev, cur, i, arr) {
  return prev + cur;
};


/*
 * Main script
 */
for (var tx_level = 0; tx_level <= 31; tx_level++) {

  moteCount = sim.getMotesCount();
  var markers = [];
  for (var i = 0; i < moteCount; i++)
    markers.push(1);
  
  while (1) { 
    //log.log("Set " + moteCount + " with Tx level: " + tx_level + "\n");
    log.log("N" + id + ": " + msg + "\n");
    
    if (msg.search("Please input Tx level") > -1 || msg.equals("wait")) {
      if (markers[id-1] == 1) {
        log.log("N" + id + "> " + msg + "\n"); 
        write(mote, tx_level);
        markers[id-1] = 0;
      }
      
    }
    
    if (markers.reduce(sum) == 0) { // All nodes have been configured.
      GENERATE_MSG(60000, "wait"); // Yield for another, then wait 
      YIELD_THEN_WAIT_UNTIL(msg.equals("wait")); // The node will be back to work again   
      showStats();
      YIELD();
      break;      
    }    
    
    YIELD();
  }
 
}

log.testOK();

