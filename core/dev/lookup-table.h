/**
 * \file
 *         Lookup tables
 * \author
 *         Riccardo Dall'Ora <r.dallora[at]unitn[dot]it>
 */

#ifndef __LOOKUPTABLE_H__
#define __LOOKUPTABLE_H__


#include "contiki.h"
#include "LUT-points.h"
#include "battery-LUT.h"
#include "cell-LUT.h"
#include "harv-LUT.h"


typedef enum {
	BATTERY,
	POWER,
	EFFICIENCY,
} lut_type;


typedef enum {
	X_AXIS,
	Y_AXIS,
} table_axis;


float get_y(lut_type lut, float x);

void get_piecewise_line_points(float x, struct points *points_ptr,
							lut_type lut);

float get_z(lut_type lut, float x, float y);

void get_piecewise_line_points_3D(uint16_t *points_pointer, float *array,
							float value, lut_type lut, table_axis axis);

#endif // __LOOKUPTABLE_H__
