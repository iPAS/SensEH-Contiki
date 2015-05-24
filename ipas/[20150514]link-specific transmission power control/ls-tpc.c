
#define NODE_COUNT   10

#define UNICAST_SEND 1
#define TX_PERIOD    5

#define DEBUG 0

#include "contiki.h"
#include "random.h"

#include "net/rime.h"
#include "dev/cc2420.h"

#include "dev/serial-line.h"

#include <stdlib.h>
#include <string.h>
#include <stdio.h>


#define UNICAST_CHANNEL   199
#define BROADCAST_CHANNEL 200
#define USER_DATA_LENGTH  16
#define SET_TXPOWER_DELAY 100

typedef enum {false=0, true} bool;

typedef enum {ANNOUNCEMENT, RSSI_REPORT, REQUEST_PING, RESPONSE_PING} message_type_t;
typedef uint8_t txpower_t;

struct announcement_t {
    message_type_t type;
    txpower_t txpower;
};

struct report_t {
    message_type_t type;
    txpower_t txpower;
    uint16_t rssi;
    uint16_t lqi;
};

struct neighbor_t {
    struct report_t report;
};

static struct neighbor_t neighbor[NODE_COUNT];
static uint8_t neighbor_found[NODE_COUNT];
static uint8_t neighbor_found_count = 0;

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
PROCESS(main_process, "Sending with TX-Power-Adjusted Specific for Each Node");
AUTOSTART_PROCESSES(&main_process);
/*---------------------------------------------------------------------------*/
bool have_found_node(uint16_t addr) {
    uint8_t i;
    for (i = 0; i < neighbor_found_count; i++)
        if (neighbor_found[i] == addr)
            break;
    if (i == neighbor_found_count)
        return false;
    else
        return true;
}

static struct unicast_conn uc;
static void
uc_recv(struct unicast_conn *c, const rimeaddr_t *from) {
    message_type_t type = *((message_type_t *)packetbuf_dataptr());
    //printf("[UC] receiving from %d.%d\n", from->u8[0], from->u8[1]);

    if (type == RSSI_REPORT) {
        struct report_t *report = (struct report_t *)packetbuf_dataptr();
        printf("[UC] having found %d.%d at tx:%d rssi:%d lqi:%d\n",
                from->u8[0], from->u8[1], report->txpower, report->rssi, report->lqi);

        uint16_t addr = from->u8[0] + ((uint16_t)from->u8[1] << 8) - 1; // Minus 1 because of indexing
        memcpy(&(neighbor[addr].report), report, sizeof(struct report_t)); // Recognize friend

        if (!have_found_node(addr))
            neighbor_found[neighbor_found_count++] = addr; // Recognize nodes found

    } else {
        //printf("[UC] receiving from %d.%d: %s\n", from->u8[0], from->u8[1], (char *)packetbuf_dataptr());
        printf("[UC] receiving from %d.%d\n", from->u8[0], from->u8[1]);
    }
}
static const struct unicast_callbacks uc_callback = { uc_recv };

/*---------------------------------------------------------------------------*/
static struct broadcast_conn bc;
static void
bc_recv(struct broadcast_conn *c, const rimeaddr_t *from)
{
    message_type_t type = *((message_type_t *)packetbuf_dataptr());
    //printf("[UC] receiving from %d.%d\n", from->u8[0], from->u8[1]);
    rimeaddr_t dest;
    rimeaddr_copy(&dest, from);

    /**
     * Existence announcement
     */
    if (type == ANNOUNCEMENT) {
        struct announcement_t *announcment = (struct announcement_t *)packetbuf_dataptr();
        struct report_t report = {
              .type     = RSSI_REPORT,
              .txpower  = announcment->txpower,
              .rssi     = packetbuf_attr(PACKETBUF_ATTR_RSSI),
              .lqi      = packetbuf_attr(PACKETBUF_ATTR_LINK_QUALITY),
        };                        ;
        printf("[BC] receiving from %d.%d: pow:%d rssi:%d lqi:%d\n",
                from->u8[0], from->u8[1], announcment->txpower, report.rssi, report.lqi);

        txpower_t oldpow = cc2420_get_txpower();
        if (oldpow != report.txpower) {
            cc2420_set_txpower(report.txpower);
            clock_delay(SET_TXPOWER_DELAY); // Delay the CPU for a multiple of 2.83 us. after the change
        }
        packetbuf_copyfrom(&report, sizeof(report));
        unicast_send(&uc, &dest);
        if (oldpow != report.txpower) cc2420_set_txpower(oldpow);

    } else {
        printf("[BC] receiving from %d.%d: %s\n", from->u8[0], from->u8[1], (char *)packetbuf_dataptr());
    }
}
static const struct broadcast_callbacks bc_callback = { bc_recv };

/*---------------------------------------------------------------------------*/
PROCESS_THREAD(main_process, ev, data) {
    const char message[] = "0 1 2 3 4 5 6 7 8 9 A B C D E F";
    static struct etimer et;

    PROCESS_EXITHANDLER(unicast_close(&uc));
    PROCESS_EXITHANDLER(broadcast_close(&bc));

    PROCESS_BEGIN();
    unicast_open(&uc, UNICAST_CHANNEL, &uc_callback);
    broadcast_open(&bc, BROADCAST_CHANNEL, &bc_callback);

    /**
     * Find levels matched neighbors
     */
    static txpower_t txpower;
    for (txpower = 31; txpower > 0; txpower--) {
        etimer_set(&et, (TX_PERIOD * CLOCK_SECOND) + (random_rand() % CLOCK_SECOND));
        PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&et));

        //printf("[BC] sending with tx:%d\n", pow);
        struct announcement_t announcement;
        announcement.type    = ANNOUNCEMENT;
        announcement.txpower = txpower;

        cc2420_set_txpower(txpower);
        clock_delay(SET_TXPOWER_DELAY); // Delay the CPU for a multiple of 2.83 us. after the change
        packetbuf_copyfrom(&announcement, sizeof(announcement));
        broadcast_send(&bc);
    }

    PROCESS_WAIT_EVENT_UNTIL(ev == serial_line_event_message); // Wait .. before the process continue

    /**
     * Main loop
     * 1) If this node is the first, then sending data to the last one.
     * 2) If it's the other, then relaying the data by flooding.
     */
    random_init(random_rand() * rimeaddr_node_addr.u8[0]);

    while (1) {
        etimer_set(&et, (TX_PERIOD * CLOCK_SECOND) + (random_rand() % CLOCK_SECOND)); // Delay the sending
        PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&et));

        if (neighbor_found_count > 0) {
            static uint8_t slot = 0;
            uint8_t addr;
            addr = neighbor_found[slot];
            rimeaddr_t dest;
            dest.u8[0] = addr + 1;
            dest.u8[1] = 0;

            slot = (random_rand() ^ 0x55) % neighbor_found_count;
//            if (++slot >= neighbor_found_count) slot = 0;

            if (!rimeaddr_cmp(&rimeaddr_node_addr, &dest)) {
                //printf("%d send to %d tx:%d\n", rimeaddr_node_addr.u8[0], addr+1, neighbor[addr].report.txpower);

                if (LS_TPC) {
                    cc2420_set_txpower(neighbor[addr].report.txpower);
                    clock_delay(SET_TXPOWER_DELAY); // Delay the CPU for a multiple of 2.83 us. after the change
                }

                packetbuf_copyfrom(message, sizeof(message));

                if (UNICAST_SEND)
                    unicast_send(&uc, &dest);
                else
                    broadcast_send(&bc);
            }
        }
    }
    PROCESS_END();
}
