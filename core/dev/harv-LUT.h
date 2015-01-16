/**
 * \file
 *         Harvester lookup tables
 * \author
 *         Riccardo Dall'Ora <r.dallora[at]unitn[dot]it>
 */


#ifndef __HARVLUT_H__
#define __HARVLUT_H__

#include "contiki.h"

#define HARV_X_VALUES		7
#define HARV_Y_VALUES		6


extern const float solarcell_out_values[HARV_X_VALUES];
extern const float batt_voltage_values[HARV_Y_VALUES];

extern const float eff_values[HARV_X_VALUES][HARV_Y_VALUES];

#endif // __HARVLUT_H__
