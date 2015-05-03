/*
 * Example Contiki test script (JavaScript).
 * A Contiki test script acts on mote output, such as via printf()'s.
 * The script may operate on the following variables:
 * Mote mote, int id, String msg
 */

TIMEOUT(60000, log.testOK());
counter=0;

while (counter < 30) {
  counter++;
  
  GENERATE_MSG(1000, "wait");
  YIELD_THEN_WAIT_UNTIL(msg.equals("wait"));

  log.log("------------------------------------------------\n");
  log.log("Round: " + counter + "\n");

  /* Extract SensEH statistics */
  //plugin = mote.getSimulation().getGUI().getStartedPlugin("SensEHGUI");
  plugin = sim.getGUI().getStartedPlugin("SensEHGUI");
  if (plugin != null) {
    //log.log("SensEH:\n" + plugin.getStatistics() + "\n");
	log.log("SensEH:\n" + plugin.radioStatistics() + "\n");
  } else {
    log.log("No SensEH plugin\n");
  }

  /* Extract PowerTracker statistics */
  //plugin = mote.getSimulation().getGUI().getStartedPlugin("PowerTracker");
  plugin = sim.getGUI().getStartedPlugin("PowerTracker");
  if (plugin != null) {
    log.log("PowerTracker:\n" + plugin.radioStatistics() + "\n");
  } else {
    log.log("No PowerTracker plugin\n");
  }

}
