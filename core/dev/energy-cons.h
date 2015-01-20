#ifndef __ENERGYCONS_H__
#define __ENERGYCONS_H__

#include "contiki.h"


struct energest_times {
    uint32_t lpm_time;
    uint32_t cpu_time;
    uint32_t rx_time;
    uint32_t tx_time;
};


float update_consumption(void);

void reset_energest_times(void);

#endif // __ENERGYCONS_H__
