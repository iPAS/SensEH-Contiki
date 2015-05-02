/*
 * Copyright (c) 2007, Swedish Institute of Computer Science.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE INSTITUTE AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * This file is part of the Contiki operating system.
 *
 * $Id: example-abc.c,v 1.7 2009/06/09 19:31:55 fros4943 Exp $
 */

/**
 * \file
 *         Testing the transmission power adjustment in Rime with Cooja
 * \author
 *         iPAS <ipas@inbox.com>
 *
 *  PA_LEVEL TXCTRL  Output Power[dBm]  Current Consumption[mA]
 *  31       0xA0FF       0                 17.4
 *  27       0xA0FB      -1                 16.5
 *  23       0xA0F7      -3                 15.2
 *  19       0xA0F3      -5                 13.9
 *  15       0xA0EF      -7                 12.5
 *  11       0xA0EB      -10                11.2
 *  7        0xA0E7      -15                9.9
 *  3        0xA0E3      -25                8.5
 *  Table 9. Output power settings and typical current consumption @ 2.45 GHz
 *
 *  In UDMG.java:
 *  Calculate ranges: grows with radio output power
 *
 *  moteTransmissionRange =
 *  	TRANSMITTING_RANGE * sender.getCurrentOutputPowerIndicator() / sender.getOutputPowerIndicatorMax()
 *
 *  TRANSMITTING_RANGE is a maximum distance of sending with PowerIndicatorMax()=31
 *
 *  Much linearly!!
 */

#include "contiki.h"
#include "net/rime.h"
#include "random.h"

#include "dev/button-sensor.h"
#include "dev/leds.h"
#include "dev/cc2420.h"

#include <stdio.h>

/*---------------------------------------------------------------------------*/
#define NODE_COUNT 2

/*---------------------------------------------------------------------------*/
PROCESS(main_process, "unicast to others");
AUTOSTART_PROCESSES(&main_process);
/*---------------------------------------------------------------------------*/
static void recv_uc(struct unicast_conn *c, const rimeaddr_t *from) {
    printf("Received '%s' from %d.%d\n", (char *) packetbuf_dataptr(),
            from->u8[0], from->u8[1]);
}
static const struct unicast_callbacks unicast_callbacks = { recv_uc };
static struct unicast_conn uc;
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(main_process, ev, data) {
    const char message[] = "0 1 2 3 4 5 6 7 8 9 A B C D E F";

    PROCESS_EXITHANDLER(unicast_close(&uc));
    PROCESS_BEGIN();

    unicast_open(&uc, 146, &unicast_callbacks);

    while (1) {
        static struct etimer et;
        etimer_set(&et, CLOCK_SECOND * 2 + random_rand() % (CLOCK_SECOND * 2)); // Delay 2-4 seconds
        PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&et));

        static rimeaddr_t addr = { { 1, 0 } };
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
        cc2420_set_txpower(txpow); // ipas: set txpower
        printf("Current txpower: %d\n", txpow);

        if (!rimeaddr_cmp(&dest, &rimeaddr_node_addr)) {
            unicast_send(&uc, &dest);
        }

        addr.u8[0] = (addr.u8[0] == 1 + NODE_COUNT) ? 1 : addr.u8[0] + 1;
    }

    PROCESS_END();
}
/*---------------------------------------------------------------------------*/

