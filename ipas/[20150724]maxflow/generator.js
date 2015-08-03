/**
 * A Contiki test script acts on mote output, such as via printf()'s.
 * The script may operate on the following variables:
 *   se.sics.cooja.Mote mote -- The Mote Mote.java
 *   int id -- The id of the mote
 *   long time -- The current time
 *   String msg -- The message
 *   se.sics.cooja.Simulation sim -- The simulation Simulation.java
 *   se.sics.cooja.Gui gui -- The GUI Gui.java
 */

sending_periods    = [250, 500, 750, 1000, 1250, 1500, 1750, 2000]; 
sent_packet_counts = [100, 200, 300, 400, 500, 600, 700, 800, 900, 1000];
// sending_periods    = [250, 500, 750, 1000]; // for reminding
// sent_packet_counts = [100, 200, 300, 400, 500];
// sending_periods    = [2000]; // for reminding
// sent_packet_counts = [1000];


// -----------------------------------------------------------------< LINE 20 >
// 49500000 + 80*10000
//TIMEOUT(49510000, too_long_test());
//TIMEOUT(50310000, too_long_test());
TIMEOUT(60000000, too_long_test());
too_long_test = function() {
    log.log("Time run out before it succeeds!\n");
    log.testFailed();
} 

/**
 * Setup energy profiler.
 */
plugin = sim.getGUI().getStartedPlugin("SensEHGUI");
if (plugin == null) {  
    log.log("No SensEH plugin!\n");
    log.testFailed();
}


/**
 * Initialization.
 * After about 1 second, all nodes wake up. 
 * Wait for the last mote be ready to be responsive for packet generation. 
 * Then, wait about 9 seconds, so that they announce themselves to neighbors.
 */
while (1) { 
    if (id == sim.getMotesCount())
        break;
    YIELD();
}
mote_s = mote; // Last one is 's' node.
/* 10 seconds on initialization + 
 1 minutes for testing on determined throughput 
 Anyway, waiting 1 munite, until all motes announce themselves.
 */
 /* Wait for self introduction, 3 minutes.
  */
GENERATE_MSG(180000, "wait"); // Yield for another, then wait. 
YIELD_THEN_WAIT_UNTIL(msg.equals("wait")); // Back to work again


/*
 * Packet generating.
 * Sending until timeout.
 */
for (var j in sending_periods) {
    for (var k in sent_packet_counts) {

        packet_generating_period = sending_periods[j]; // 250 .. 2000
        packet_generated_count = sent_packet_counts[k]; // 100 .. 1000

        start_time = time;
        plugin.restartConsumedEnergyStatistics();
        plugin.restartStoredEnergyStatistics();
        plugin.restartHarvestedEnergyStatistics();

        for (var i = 0; i < packet_generated_count; i++) {
            mote_s.getInterfaces().getButton().clickButton();            
            GENERATE_MSG(packet_generating_period, "wait"); // Yield for another, then wait 
            YIELD_THEN_WAIT_UNTIL(msg.equals("wait")); // Back to work again 
        }
        sim_period = time - start_time;


        /**
         * Show result
         */
        log.log("Generating " + packet_generated_count + 
                " packets every " + packet_generating_period + " milliseconds" +
                " within " + (sim_period/1000000) + " seconds" + "\n");
        motes = sim.getMotes();
        for (var m = 0; m < motes.length; m++) {
            write(motes[m], "stat reset");
            YIELD_THEN_WAIT_UNTIL(msg.search(id + ".0: stat") > -1);
            //mote.getMemory().getByteValueOf("");    
            log.log(msg + "\n");
        }
        log.log(plugin.getStatistics() + "\n"); // Extract SensEH statistics
        

        /**
         * Wait for next experiment
         */
        GENERATE_MSG(60000, "wait"); // Yield for another, then wait 
        YIELD_THEN_WAIT_UNTIL(msg.equals("wait")); // Back to work again
    }
}


log.testOK();
