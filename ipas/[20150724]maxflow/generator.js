/*
 * A Contiki test script acts on mote output, such as via printf()'s.
 * The script may operate on the following variables:
 *   se.sics.cooja.Mote mote -- The Mote Mote.java
 *   int id -- The id of the mote
 *   long time -- The current time
 *   String msg -- The message
 *   se.sics.cooja.Simulation sim -- The simulation Simulation.java
 *   se.sics.cooja.Gui gui -- The GUI Gui.java
 */
sending_periods = [1000, 2000]; //, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000];

test_period = 60000; // Time limiting 
test_packet = 100; // Packet number limiting

timeout = 10000;
if (test_packet == 0) 
    timeout += test_period * sending_periods.length;
else
    for (p in sending_periods)
        timeout += test_packet * sending_periods[p];

if (timeout > 2147483647)
    log.log("Timeout is " + timeout + "\n"); 
//TIMEOUT(timeout, log.testFailed()); // DEFAULT=20min; thus, setting new
TIMEOUT(3600000, log.testFailed()); // One hour. Why can it be used with variable?

/*
 * Wait for the one be ready
 */
while (1) { 
    if (id == sim.getMotesCount())
        break;
    YIELD();
}

/*
 * Main script
 */
motes  = sim.getMotes();
mote_s = motes[ id-1 ]; // Last one is 's' node.

for (p in sending_periods) {
    GENERATE_MSG(10000, "wait"); // Yield for another, then wait 
    YIELD_THEN_WAIT_UNTIL(msg.equals("wait")); // Back to work again
    
    log.log("Test with sampling period: " + sending_periods[p] + "\n");
    
    /* 
     * Sending until timeout
     */
    if (test_packet == 0) {
        log.log("> sending for " + test_period + " mS\n");        
        ending = time + (test_period*1000);            
        while (1) {
            mote_s.getInterfaces().getButton().clickButton();            
            GENERATE_MSG(sending_periods[p], "wait"); // Yield for another, then wait 
            YIELD_THEN_WAIT_UNTIL(msg.equals("wait")); // Back to work again                      
            if (time >= ending) break;
        }
    }
    /* 
     * Sending packets until every 'test_packet' are sent.
     */
    else {
        log.log("> sending for " + test_packet + " packets\n");
        for (i=0; i < test_packet; i++) {
            mote_s.getInterfaces().getButton().clickButton();            
            GENERATE_MSG(sending_periods[p], "wait"); // Yield for another, then wait 
            YIELD_THEN_WAIT_UNTIL(msg.equals("wait")); // Back to work again
        }
    }    
}

log.testOK();
