/**
 * \brief
 *          Project configuration for multihop network-flow experiment.
 *
 * \author
 *          iPAS <ipas@mail.com>
 *
 * \original
 *          [CONTIKI_DIR]/core/contiki-default-conf.h
 */
#ifndef __PROJECT_CONF_H__

/*---------------------------------------------------------------------------*/
/* Netstack configuration
 *
 * The netstack configuration is typically overridden by the platform
 * configuration, as defined in contiki-conf.h
 */

/* NETSTACK_CONF_RADIO specifies the radio driver. The radio driver
   typically depends on the radio used on the target hardware. */
//#define NETSTACK_CONF_RADIO nullradio_driver
#define NETSTACK_CONF_RADIO cc2420_driver

/* NETSTACK_CONF_FRAMER specifies the over-the-air frame format used
   by Contiki radio packets. For IEEE 802.15.4 radios, use the
   framer_802154 driver. */
//#define NETSTACK_CONF_FRAMER framer_nullmac
//#define NETSTACK_CONF_FRAMER framer_802154

/* NETSTACK_CONF_RDC specifies the Radio Duty Cycling (RDC) layer. The
   nullrdc_driver never turns the radio off and is compatible with all
   radios, but consumes a lot of power. The contikimac_driver is
   highly power-efficent and allows sleepy routers, but is not
   compatible with all radios. */
//#define NETSTACK_CONF_RDC   nullrdc_driver
#define NETSTACK_CONF_RDC   xmac_driver
//#define NETSTACK_CONF_RDC   contikimac_driver

/* NETSTACK_CONF_MAC specifies the Medium Access Control (MAC)
   layer. The nullmac_driver does not provide any MAC
   functionality. The csma_driver is the default CSMA MAC layer, but
   is not compatible with all radios. */
//#define NETSTACK_CONF_MAC   nullmac_driver
#define NETSTACK_CONF_MAC   csma_driver

/* NETSTACK_CONF_RDC_CHANNEL_CHECK_RATE specifies the channel check
   rate of the RDC layer. This defines how often the RDC will wake up
   and check for radio channel activity. A higher check rate results
   in higher communication performance at the cost of a higher power
   consumption. */
#define NETSTACK_CONF_RDC_CHANNEL_CHECK_RATE 8

/* CONTIKIMAC_CONF_WITH_PHASE_OPTIMIZATION specifies if ContikiMAC
   should optimize for the phase of neighbors. The phase optimization
   may reduce power consumption but is not compatible with all timer
   settings and is therefore off by default. */
#define CONTIKIMAC_CONF_WITH_PHASE_OPTIMIZATION 0

#endif // __PROJECT_CONF_H__
