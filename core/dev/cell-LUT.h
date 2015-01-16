/**
 * \file
 *         Solar cell lookup tables
 * \author
 *         Riccardo Dall'Ora <r.dallora[at]unitn[dot]it>
 */

#ifndef __CELLLUT_H__
#define __CELLLUT_H__

#include "contiki.h"

#define POWER_VALUES			5


extern const float lux_values[POWER_VALUES];
extern const float power_values[POWER_VALUES];

#endif // __CELLLUT_H__
