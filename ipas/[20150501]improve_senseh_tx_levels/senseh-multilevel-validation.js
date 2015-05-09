/*
 * Example Contiki test script (JavaScript).
 * A Contiki test script acts on mote output, such as via printf()'s.
 * The script may operate on the following variables:
 * Mote mote, int id, String msg
 */

TIMEOUT(120000, log.testOK());
counter=0;

while (counter < 120) {
  counter++;
  
  GENERATE_MSG(1000, "wait");
  YIELD_THEN_WAIT_UNTIL(msg.equals("wait"));

  log.log("------------------------------------------------\n");
  log.log("Round: " + counter + "\n");

  /* Extract SensEH statistics */
  plugin = sim.getGUI().getStartedPlugin("SensEHGUI");
  if (plugin != null) {
    log.log("SensEH: Total Energies Statistics\n" + plugin.radioTxStatistics() + "\n");
  } else {
    log.log("No SensEH plugin\n");
  }

}
