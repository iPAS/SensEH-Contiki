/**
 * \brief
 *         Sending data packets from S to T node.
 *         Using for an experiment about throughput and efficient energy-consumption study.
 *
 * \date   2015 07 27
 *
 * \author iPAS, Pasakorn Tiwatthanont <ipas@inbox.com>
 *
 * \copyright
 *         Revised from example-mesh.c of Adam Dunkels <adam@sics.se>
 */
#include "contiki.h"
#include "net/rime.h"
#include "net/rime/mesh.h"

#include "dev/button-sensor.h"

#include "dev/leds.h"

#include <stdio.h>
#include <string.h>

#define MESSAGE "Hello"

static struct mesh_conn conn;
/** ---------------------------------------------------------------------------
 * On sent
 */
static void sent(struct mesh_conn *c) {
    printf("%d.%d: sent\n", rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1]);
}

/** ---------------------------------------------------------------------------
 * On timeout
 */
static void timedout(struct mesh_conn *c) {
    printf("%d.%d: timeout\n", rimeaddr_node_addr.u8[0],  rimeaddr_node_addr.u8[1]);
}

/** ---------------------------------------------------------------------------
 * On received
 */
static void recv(struct mesh_conn *c, const rimeaddr_t *from, uint8_t hops) {
    printf("%d.%d: received far %d hops from %d.%d, %.*s (%d bytes)\n",
            rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1], hops, from->u8[0], from->u8[1],
            packetbuf_datalen(), (char *) packetbuf_dataptr(), packetbuf_datalen());
}

const static struct mesh_callbacks callbacks = { recv, sent, timedout };

/** ---------------------------------------------------------------------------
 * Main process.
 */
PROCESS(main_process, "Data Flow S-T via Mesh Routing");
AUTOSTART_PROCESSES(&main_process);

PROCESS_THREAD(main_process, ev, data) {
    PROCESS_EXITHANDLER(mesh_close(&conn));
    PROCESS_BEGIN();

    mesh_open(&conn, 132, &callbacks);

    SENSORS_ACTIVATE(button_sensor);

    while (1) {
        /* Wait for button click before sending the first message. */
        PROCESS_WAIT_EVENT_UNTIL(ev == sensors_event && data == &button_sensor);

        /* Send a message to node number 1. */
        static unsigned char msg_id = 0;
        char msg[15];
        sprintf(msg, "%s#%u", MESSAGE, msg_id++);
        packetbuf_copyfrom(msg, strlen(msg));
        rimeaddr_t to;
        to.u8[0] = 1;
        to.u8[1] = 0;
        mesh_send(&conn, &to);
    }

    PROCESS_END();
}
/*---------------------------------------------------------------------------*/
