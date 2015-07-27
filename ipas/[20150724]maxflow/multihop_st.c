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
 *         Revised from example-multihop.c of Adam Dunkels <adam@sics.se>
 *
 * \details
 *         This example shows how to use the multihop Rime module, how
 *         to use the announcement mechanism, how to manage a list
 *         with the list module, and how to allocate memory with the
 *         memb module.
 *
 *         The multihop module provides hooks for forwarding packets
 *         in a multi-hop fashion, but does not implement any routing
 *         protocol. A routing mechanism must be provided by the
 *         application or protocol running on top of the multihop
 *         module. In this case, this example program provides the
 *         routing mechanism.
 *
 *         The routing mechanism implemented by this example program
 *         is very simple: it forwards every incoming packet to a
 *         random neighbor. The program maintains a list of neighbors,
 *         which it populated through the use of the announcement
 *         mechanism.
 *
 *         The neighbor list is populated by incoming announcements
 *         from neighbors. The program maintains a list of neighbors,
 *         where each entry is allocated from a MEMB() (memory block
 *         pool). Each neighbor has a timeout so that they do not
 *         occupy their list entry for too long.
 *
 *         When a packet arrives to the node, the function forward()
 *         is called by the multihop layer. This function picks a
 *         random neighbor to send the packet to. The packet is
 *         forwarded by every node in the network until it reaches its
 *         final destination (or is discarded in transit due to a
 *         transmission error or a collision).
 *
 */

#include "contiki.h"
#include "net/rime.h"
#include "lib/list.h"
#include "lib/memb.h"
#include "lib/random.h"
#include "dev/button-sensor.h"
#include "dev/leds.h"

#include <stdio.h>

#define CHANNEL 135
#define NODE_S
#define NODE_T

#define NEIGHBOR_TIMEOUT 60 * CLOCK_SECOND
#define MAX_NEIGHBORS 16
LIST(neighbor_table);
struct neighbor {
    struct neighbor *next;
    rimeaddr_t addr;
    struct ctimer ctimer;
};
MEMB(neighbor_mem, struct neighbor, MAX_NEIGHBORS); // Declare memory block for dynamic allocation

static struct announcement greeting_ann;

/** ---------------------------------------------------------------------------
 * This function is called by the ctimer presented in each neighbor table entry.
 * The neighbor whose time is up has to be removed from the table.
 */
static void remove_neighbor(void *n) {
    struct neighbor *e = n;
    list_remove(neighbor_table, e);
    memb_free(&neighbor_mem, e);
}

/** ---------------------------------------------------------------------------
 * This function is called when an incoming announcement arrives.
 * The function checks the neighbor table to see if the neighbor is already presented in the list.
 * But, if it's not, a new entry is allocated and is added to the table.
 */
static void received_announcement(
        struct announcement *a, const rimeaddr_t *from, uint16_t id, uint16_t value) {
    struct neighbor *e;
    printf("%d.%d: got announcement from %d.%d, id %d, value %d\n",
            rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1], from->u8[0], from->u8[1],
            id, value);

    /* The announcement are updated to the neighbor list,
       or add a new entry to the table. */

    /* Try finding out that we have known each other already. */
    for(e = list_head(neighbor_table); e != NULL; e = e->next) {
        if(rimeaddr_cmp(from, &e->addr)) { // Our neighbor was found, so we update the timeout.
            ctimer_set(&e->ctimer, NEIGHBOR_TIMEOUT, remove_neighbor, e);
            return;
        }
    }

    /* The neighbor was not found in the list,
       so we add a new entry by allocating memory from the neighbor_mem pool,
       fill in the necessary fields,
       and add it to the list. */
    e = memb_alloc(&neighbor_mem);
    if(e != NULL) {
        rimeaddr_copy(&e->addr, from);
        list_add(neighbor_table, e);
        ctimer_set(&e->ctimer, NEIGHBOR_TIMEOUT, remove_neighbor, e);
    }
}

/** ---------------------------------------------------------------------------
 * This function is called at the final recepient of the message.
 */
static void
recv(struct multihop_conn *c, const rimeaddr_t *sender,
     const rimeaddr_t *prevhop,
     uint8_t hops)
{
    printf("%d.%d: received message from %d.%d, '%s'\n",
            rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1], sender->u8[0], sender->u8[1],
            (char *)packetbuf_dataptr());
}

/** ---------------------------------------------------------------------------
 * This function is called to forward a packet.
 * The function picks a random neighbor from the neighbor list and returns its address.
 * The multihop layer sends the packet to this address.
 * If no neighbor is found, the function returns NULL to signal to the multihop layer
 * that the packet should be dropped.
 */
static rimeaddr_t * forward(
        struct multihop_conn *c, const rimeaddr_t *originator, const rimeaddr_t *dest,
        const rimeaddr_t *prevhop, uint8_t hops) {
    /* Find a random neighbor to send to. */
    int num, i;
    struct neighbor *n;

    if(list_length(neighbor_table) > 0) {
        num = random_rand() % list_length(neighbor_table);
        i = 0;
        for(n = list_head(neighbor_table); n != NULL && i != num; n = n->next) {
            ++i;
        }

        if(n != NULL) {
            printf("%d.%d: forwarding packet to %d.%d (%d in list), hops %d\n",
                    rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1],
                    n->addr.u8[0], n->addr.u8[1], num, packetbuf_attr(PACKETBUF_ATTR_HOPS));
            return &n->addr;
        }
    }

    printf("%d.%d: not found a neighbor to foward to %d.%d\n",
            rimeaddr_node_addr.u8[0], rimeaddr_node_addr.u8[1], dest->u8[0], dest->u8[1]);
    return NULL;
}

/** ---------------------------------------------------------------------------
 * Multihop process.
 */
static const struct multihop_callbacks callbacks = {recv, forward};
static struct multihop_conn conn;

PROCESS(multihop_process, "Multihop Flow S-T");
AUTOSTART_PROCESSES(&multihop_process);

PROCESS_THREAD(multihop_process, ev, data)
{
    PROCESS_EXITHANDLER(multihop_close(&conn));
    PROCESS_BEGIN();

    memb_init(&neighbor_mem); // Initialize the memory for the neighbor table entries.
    list_init(neighbor_table); // Initialize the list used for the neighbor table.
    multihop_open(&conn, CHANNEL, &callbacks); // Open the connection on Rime channel CHANNEL.

    /* Register an announcement with the same announcement ID as the Rime channel
       that we use to open the multihop connection above. */
    announcement_register(&greeting_ann, CHANNEL, received_announcement);
    announcement_set_value(&greeting_ann, 0); // Set a dummy value to start sending out announcments.

    /* Activate the button sensor.
       We use the button to drive traffic when the button is pressed, a packet is sent. */
    SENSORS_ACTIVATE(button_sensor);

    /* Loop forever,
       send a packet when the button is pressed. */
    while(1) {
        /* Wait until we get a sensor event with the button sensor as data. */
        PROCESS_WAIT_EVENT_UNTIL(ev == sensors_event && data == &button_sensor);

        /* Copy the "Hello" to the packet buffer. */
        packetbuf_copyfrom("Hello", 6);

        /* Set the Rime address of the final receiver of the packet to 1.0. */
        rimeaddr_t to;
        to.u8[0] = 1;
        to.u8[1] = 0;

        /* Send the packet. */
        multihop_send(&conn, &to);
    }

    PROCESS_END();
}
