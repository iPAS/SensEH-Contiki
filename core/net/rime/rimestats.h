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
 */

/**
 * \file
 *         Header file for Rime statistics
 * \author
 *         Adam Dunkels <adam@sics.se>
 */

#ifndef __RIMESTATS_H__
#define __RIMESTATS_H__

struct rimestats {
  unsigned long tx, rx; /* In rime.c */

  unsigned long reliabletx, reliablerx, /* Be used in runicast.c */
                rexmit,
                acktx, noacktx,
                ackrx, timedout, badackrx;


  unsigned long lltx, llrx; /* Be used in either cc2420.c or other MACs  */

  unsigned long tot_tx_byte; /* Total sent bytes */
  unsigned long tot_rx_byte; /* Total received bytes */

  unsigned long toolong, tooshort, badsynch, badcrc; /* Reasons for dropping incoming packets: */

  unsigned long contentiondrop, /* Packet dropped due to contention, such as in cc2420.c */
                sendingdrop; /* Packet dropped when we were sending a packet, such as in xmac.c */


  /* iPAS: XMAC statistic */
  unsigned long xmac_rx_all;
  unsigned long xmac_rx_ok;
  unsigned long xmac_rx_fail;

  unsigned long xmac_rx_unicast; /* Received packets of us */
  unsigned long xmac_rx_broadcast; /* Received packets of the other */
  unsigned long xmac_rx_other; /* Received packets of the other */

  unsigned long xmac_rx_strobe_unicast;
  unsigned long xmac_rx_strobe_broadcast;
  unsigned long xmac_rx_strobe_other;

  unsigned long xmac_rx_announcement;
  unsigned long xmac_rx_acknowledgement;
  unsigned long xmac_rx_unknown;
};

#if RIMESTATS_CONF_ENABLED
/* Don't access this variable directly, use RIMESTATS_ADD and RIMESTATS_GET */
extern struct rimestats rimestats;
#define RIMESTATS_ADD(x) rimestats.x++
#define RIMESTATS_ADD_WITH(x, y) rimestats.x+=y
#define RIMESTATS_GET(x) rimestats.x

#else /* RIMESTATS_CONF_ENABLED */
#define RIMESTATS_ADD(x)
#define RIMESTATS_ADD_WITH(x, y)
#define RIMESTATS_GET(x) 0
#endif /* RIMESTATS_CONF_ENABLED */

#endif /* __RIMESTATS_H__ */
