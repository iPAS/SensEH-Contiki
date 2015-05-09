
#define DEBUG 0

#include "contiki.h"
#include "random.h"

#include "net/rime.h"
#include "dev/cc2420.h"

#include "dev/serial-line.h"

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#define NODE_COUNT  2

/*
PROCESS_BEGIN();      // Declares the beginning of a process' protothread.
PROCESS_END();        // Declares the end of a process' protothread.
PROCESS_EXIT();       // Exit the process.
PROCESS_WAIT_EVENT(); // Wait for any event.
PROCESS_WAIT_EVENT_UNTIL(); // Wait for an event, but with a condition.
PROCESS_YIELD();      // Wait for any event, equivalent to PROCESS_WAIT_EVENT().
PROCESS_WAIT_UNTIL(); // Wait for a given condition; may not yield the process.
PROCESS_PAUSE();      // Temporarily yield the process.
 */

/*---------------------------------------------------------------------------*/
PROCESS(unicast_process, "Sending Unicast with TX-Power Adjustable");
PROCESS(serial_input_process, "Receiving Input Commands");
AUTOSTART_PROCESSES(&unicast_process);
/*---------------------------------------------------------------------------*/
static void recv_uc(struct unicast_conn *c, const rimeaddr_t *from) {
    printf("Received '%s' from %d.%d\n", (char *) packetbuf_dataptr(), from->u8[0], from->u8[1]);
}
static const struct unicast_callbacks unicast_callbacks = { recv_uc };
static struct unicast_conn uc;
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(unicast_process, ev, data) {
    const char message[] = "0 1 2 3 4 5 6 7 8 9 A B C D E F";

    process_start(&serial_input_process, NULL);
    //process_post(&serial_input_process, PROCESS_EVENT_POLL, NULL);

    PROCESS_EXITHANDLER(unicast_close(&uc));
    PROCESS_BEGIN();

    unicast_open(&uc, 146, &unicast_callbacks);

    while (1) {
        static struct etimer et;
        etimer_set(&et, CLOCK_SECOND + (random_rand() % CLOCK_SECOND)); // Delay 1-2 seconds
        PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&et));

        static rimeaddr_t addr = { { 1, 1 } };
        rimeaddr_t dest;
        dest.u8[0] = addr.u8[0];
        dest.u8[1] = addr.u8[1];
        packetbuf_copyfrom(message, sizeof(message));

        do {
            addr.u8[0] = (addr.u8[0] == NODE_COUNT) ? 1 : addr.u8[0]+1;
        } while (rimeaddr_cmp(&dest, &rimeaddr_node_addr));

        unicast_send(&uc, &dest);
    }

    PROCESS_END();
}

/*---------------------------------------------------------------------------*/
PROCESS_THREAD(serial_input_process, ev, data) {
    PROCESS_BEGIN();
    while (1) {
        printf("Please input Tx level [0-31]:\n");
        PROCESS_WAIT_EVENT_UNTIL(ev == serial_line_event_message); // Wait .. before the process continue
        uint8_t tx_pow = atoi(data);
        //packetbuf_set_attr(PACKETBUF_ATTR_RADIO_TXPOWER, txpow);
        cc2420_set_txpower(tx_pow);

        printf("Tx level has been set to: %d\n", tx_pow);

//        static struct etimer et;
//        etimer_set(&et, CLOCK_SECOND * 60);
//        PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&et));
    }
    PROCESS_END();
}
