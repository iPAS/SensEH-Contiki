
#define DEBUG 0

#include "contiki.h"
#include "random.h"

#include "net/rime.h"
#include "dev/cc2420.h"

#include "dev/serial-line.h"

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#define TX_PERIOD    5
#define NODE_COUNT   2
#define UNICAST_CHANNEL   199
#define BROADCAST_CHANNEL 200
#define USER_DATA_LENGTH  16
#define SET_TXPOWER_DELAY 100

typedef enum {true, false} bool;

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

struct request_ping_t {
    message_type_t type;
    uint16_t sequence;
    rimeaddr_t sink;
    rimeaddr_t source;
    uint8_t hop;
};
uint16_t last_request_sequence = 0;

struct response_ping_t {
    message_type_t type;
    uint16_t sequence;
    rimeaddr_t sink;
    rimeaddr_t source;
    uint8_t hop;
    uint8_t user_data[USER_DATA_LENGTH];
};
uint16_t last_response_sequence = 0;

struct neighbor_t {
    struct report_t report;
    rimeaddr_t nexthop;
};
static struct neighbor_t neighbor[NODE_COUNT];

const rimeaddr_t rimeaddr_first = {{1, 0}};
const rimeaddr_t rimeaddr_last = {{NODE_COUNT && 0x00FF, ((uint16_t)NODE_COUNT>>8) & 0x00FF}};

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
static struct unicast_conn uc;
static void
uc_recv(struct unicast_conn *c, const rimeaddr_t *from) {
    message_type_t type = *((message_type_t *)packetbuf_dataptr());
    //printf("[UC] receiving from %d.%d\n", from->u8[0], from->u8[1]);

    if (type == RSSI_REPORT) {
        struct report_t *report = (struct report_t *)packetbuf_dataptr();
        printf("[UC] having found %d.%d at tx:%d rssi:%d lqi:%d\n",
                from->u8[0], from->u8[1], report->txpower, report->rssi, report->lqi);
        uint16_t addr = from->u8[0] + ((uint16_t)from->u8[1] << 8);
        memcpy(&(neighbor[addr].report), report, sizeof(struct report_t)); // Recognize friend
    }
}
static const struct unicast_callbacks uc_callback = { uc_recv };

/*---------------------------------------------------------------------------*/
static bool
is_new_request(struct request_ping_t *request) {
    // TODO:
    return true;
}

static bool
is_new_response(struct response_ping_t *response) {
    // TODO:
    return true;
}

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
              .type = RSSI_REPORT,
              .txpower = announcment->txpower,
              .rssi = packetbuf_attr(PACKETBUF_ATTR_RSSI),
              .lqi = packetbuf_attr(PACKETBUF_ATTR_LINK_QUALITY),
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

    } else

    /**
     * Information request
     */
    if (type == REQUEST_PING) {
        struct request_ping_t *request = (struct request_ping_t *)packetbuf_dataptr();
        
        if (rimeaddr_cmp(&rimeaddr_node_addr, &rimeaddr_first)) { // on being the sink

        } else
        if (rimeaddr_cmp(&rimeaddr_node_addr, &rimeaddr_last)) { // on being the source
            if (is_new_request(request)) {

            }
        } else {
            if (is_new_request(request)) { // on being the other

            }
        }

    } else

    /**
     * Information response
     */
    if (type == RESPONSE_PING) {
        struct response_ping_t *response = (struct response_ping_t *)packetbuf_dataptr();

        if (rimeaddr_cmp(&rimeaddr_node_addr, &rimeaddr_first)) { // on being the sink

        } else
        if (rimeaddr_cmp(&rimeaddr_node_addr, &rimeaddr_last)) { // on being the source
            if (is_new_response(response)) {

            }
        } else {
            if (is_new_response(response)) { // on being the other

            }
        }

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
     * Initialization
     */
    int i;
    for (i = 0; i < NODE_COUNT; i++) {
        neighbor[i].nexthop.u8[0] = 0xFF; // Inf.
        neighbor[i].nexthop.u8[1] = 0xFF; // Inf.
    }

    /**
     * Find levels matched neighbors
     */
    static txpower_t txpower;
    for (txpower = 31; txpower > 0; txpower--) {
        etimer_set(&et, (TX_PERIOD * CLOCK_SECOND) + (random_rand() % CLOCK_SECOND));
        PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&et));

        //printf("[BC] sending with tx:%d\n", pow);
        struct announcement_t announcement;
        announcement.type = ANNOUNCEMENT;
        announcement.txpower = txpower;

        cc2420_set_txpower(txpower);
        clock_delay(SET_TXPOWER_DELAY); // Delay the CPU for a multiple of 2.83 us. after the change
        packetbuf_copyfrom(&announcement, sizeof(announcement));
        broadcast_send(&bc);
    }

    /**
     * Main loop
     * 1) If this node is the first, then sending data to the last one.
     * 2) If it's the other, then relaying the data by flooding.
     */
    while (1) {
        etimer_set(&et, (TX_PERIOD * CLOCK_SECOND) + (random_rand() % CLOCK_SECOND)); // Delay the sending
        PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&et));

        if (rimeaddr_cmp(&rimeaddr_node_addr, &rimeaddr_first)) {

        }
    }
    PROCESS_END();
}
