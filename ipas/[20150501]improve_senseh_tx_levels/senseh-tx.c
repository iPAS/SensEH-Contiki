/**
 * \file
 *          Test-application for SensEH - Memory mode
 * \author
 *          Riccardo Dall'Ora
 */

#include "contiki.h"
#include "dev/serial-line.h"

#include "net/rime.h"
#include "random.h"
#include "dev/cc2420.h"

#include <stdlib.h>
#include <string.h>
#include <stdio.h>


#include "battery.h"
#include "energy-harv.h"
#include "powertrace.h"

#if COOJA_SIM
#include <stdio.h>
#define PRINTF(...) printf(__VA_ARGS__)
#else
#define PRINTF(...)
#endif

#define NODE_COUNT  2

/*---------------------------------------------------------------------------*/
PROCESS(unicast_process, "Unicasting with TX-Power Adjustment");
PROCESS(eh_process, "SensEH");
AUTOSTART_PROCESSES(&unicast_process, &eh_process);
/*---------------------------------------------------------------------------*/
static void recv_uc(struct unicast_conn *c, const rimeaddr_t *from) {
    printf("Received '%s' from %d.%d\n", (char *) packetbuf_dataptr(),
            from->u8[0], from->u8[1]);
}
static const struct unicast_callbacks unicast_callbacks = { recv_uc };
static struct unicast_conn uc;
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(unicast_process, ev, data) {
    const char message[] = "0 1 2 3 4 5 6 7 8 9 A B C D E F";

    PROCESS_EXITHANDLER(unicast_close(&uc));
    PROCESS_BEGIN();

    unicast_open(&uc, 146, &unicast_callbacks);

    while (1) {
        static struct etimer et;
        etimer_set(&et, CLOCK_SECOND * 2 + random_rand() % (CLOCK_SECOND * 2)); // Delay 2-4 seconds
        PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&et));

        static rimeaddr_t addr = { { 1, 1 } };
        rimeaddr_t dest;
        dest.u8[0] = addr.u8[0];
        dest.u8[1] = addr.u8[1];
        packetbuf_copyfrom(message, sizeof(message));

        static int8_t txpow = 1;
        static int8_t dir = 1;
        if (dir > 0) {
            if (txpow >= 31)
                dir = -1;
        } else if (txpow <= 1)
            dir = 1;
        txpow += dir;

        //packetbuf_set_attr(PACKETBUF_ATTR_RADIO_TXPOWER, txpow);
        cc2420_set_txpower(txpow);
        //printf("Current txpower: %d\n", txpow);

        do {
            addr.u8[0] = (addr.u8[0] == NODE_COUNT) ? 1 : addr.u8[0]+1;
        } while (rimeaddr_cmp(&dest, &rimeaddr_node_addr));

        unicast_send(&uc, &dest);
    }

    PROCESS_END();
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(eh_process, ev, data) {
    static uint16_t sensor_value;

    PROCESS_BEGIN();

    set_num_cells(NUM_CELLS);
    initialize_energy_data(); // Update the consumption and residue energy

    while (1) {
//        static struct etimer et;
//        etimer_set(&et, CLOCK_SECOND * LIGHT_TIMER);
//        PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&et));

        if (battery_is_charged()) { // The battery is till not depleted.
            PROCESS_WAIT_EVENT_UNTIL(ev == serial_line_event_message);
                // Harvested energy is reported through the serial line.
            sensor_value = atoi(data);
            get_harvested_energy(sensor_value, LIGHT_TIMER);
            print_incoming_power();
            print_battery_data();
        }
    }

    PROCESS_END();
}
