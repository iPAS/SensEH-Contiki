/**
 * \file
 *         Battery lookup tables
 * \author
 *         Riccardo Dall'Ora <r.dallora[at]unitn[dot]it>
 */

#ifndef __BATTERYLUT_H__
#define __BATTERYLUT_H__

#include "contiki.h"

#define BATTERY_VALUES		9


extern const float charge_values[BATTERY_VALUES];
extern const float voltage_values[BATTERY_VALUES];


#endif // __BATTERYLUT_H__
