/**
 * \file
 *         Energy consumption estimation
 * \author
 *         Riccardo Dall'Ora <r.dallora[at]unitn[dot]it>
 */

#include "energy-cons.h"
#include "sys/energest.h"

#define I_LPM           0.0545
#define I_CPU           1.8
#define I_RX            20.0
#ifndef I_TX
#define I_TX            16.5
#endif

#ifndef VOLTS
#define VOLTS			2.4
#endif // VOLTS

#if COOJA_SIM
#include <stdio.h>
#define PRINTF(...) printf(__VA_ARGS__)
#else
#define PRINTF(...)
#endif


static struct energest_times prev_times;


/*---------------------------------------------------------------------------*/
float update_consumption(void)
{
    uint32_t lpm_time;
    uint32_t cpu_time;
    uint32_t rx_time;
    uint32_t tx_time;
    uint32_t lpm;
    uint32_t cpu;
    uint32_t rx;
    uint32_t tx;
    float consumed_energy;

    lpm_time = energest_type_time(ENERGEST_TYPE_LPM);
    cpu_time = energest_type_time(ENERGEST_TYPE_CPU);
    rx_time = energest_type_time(ENERGEST_TYPE_LISTEN);
    tx_time = energest_type_time(ENERGEST_TYPE_TRANSMIT);
    lpm = lpm_time - prev_times.lpm_time;
    cpu = cpu_time - prev_times.cpu_time;
    rx = rx_time - prev_times.rx_time;
    tx = tx_time - prev_times.tx_time;

    consumed_energy = (I_LPM * lpm + I_CPU * cpu + I_RX * rx + I_TX * tx) * VOLTS / RTIMER_ARCH_SECOND;  /* mJ = mA * seconds * volts */

    prev_times.lpm_time = lpm_time;
    prev_times.cpu_time = cpu_time;
    prev_times.rx_time = rx_time;
    prev_times.tx_time = tx_time;

    return consumed_energy;
}
/*---------------------------------------------------------------------------*/
void reset_energest_times(void)
{
    prev_times.lpm_time = energest_type_time(ENERGEST_TYPE_LPM);
    prev_times.cpu_time = energest_type_time(ENERGEST_TYPE_CPU);
    prev_times.rx_time = energest_type_time(ENERGEST_TYPE_LISTEN);
    prev_times.tx_time = energest_type_time(ENERGEST_TYPE_TRANSMIT);
}
/*---------------------------------------------------------------------------*/
