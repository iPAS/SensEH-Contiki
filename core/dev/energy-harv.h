/**
 * \file
 *         Energy Harvester function
 * \author
 *         Riccardo Dall'Ora <r.dallora[at]unitn[dot]it>
 */

#ifndef __HARVEFF_H__
#define __HARVEFF_H__

#include "contiki.h"
#include "LUT-points.h"


void set_num_cells(uint16_t cells);

float get_power(uint16_t raw_light);
void get_harvested_energy(uint16_t raw_light, float time_interval);

void print_incoming_power(void);
void reset_energy_harvested(void);

#endif // __HARVEFF_H__
