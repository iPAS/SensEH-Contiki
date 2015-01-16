/**
 * \file
 *         Get points on lookup table
 * \author
 *         Riccardo Dall'Ora <r.dallora[at]unitn[dot]it>
 */


#include "lookup-table.h"

#include <string.h>


#if COOJA_SIM
#include <stdio.h>
#define PRINTF(...) printf(__VA_ARGS__)
#else
#define PRINTF(...)
#endif


float
get_y(lut_type lut, float x)
{
	float y, slope;
	struct points piece_points[2];

	get_piecewise_line_points(x, piece_points, lut);
	slope = (piece_points[1].y - piece_points[0].y) /
			(piece_points[1].x - piece_points[0].x);

	y = slope * (x - piece_points[0].x) + piece_points[0].y;
	return y;
}


void
get_piecewise_line_points(float x, struct points *points_ptr,
							lut_type lut)
{
	struct points *piece_points = points_ptr;
	uint8_t x_in_range;
	uint16_t i, points_length;

	points_length = 0;

	if(lut == BATTERY) {
		points_length = BATTERY_VALUES;
	} else if(lut == POWER) {
		points_length = POWER_VALUES;
	}

	float values_x[points_length];
	float values_y[points_length];

	if(lut == BATTERY) {
		memcpy(values_x, charge_values, sizeof(charge_values));
		memcpy(values_y, voltage_values, sizeof(voltage_values));
	} else if(lut == POWER) {
		memcpy(values_x, lux_values, sizeof(lux_values));
		memcpy(values_y, power_values, sizeof(power_values));
	}

	x_in_range = 0;
	i = 0;

	while(i < points_length) {
		if(x <= values_x[i]) {
			x_in_range = 1;
			break;
		}
		i++;
	}
	if(x_in_range) {
		uint16_t prev = i - 1;
		uint16_t next = i;

		if(i == 0) {
			prev = 0;
			next = 1;
		}
		piece_points[0].x = values_x[prev];
		piece_points[0].y = values_y[prev];
		piece_points[1].x = values_x[next];
		piece_points[1].y = values_y[next];
	}

	return;
}


float
get_z(lut_type lut, float x, float y)
{
	uint16_t xi[2];
	uint16_t yi[2];
	uint16_t x_length, y_length;
	float x0, y0, x1, y1,
		z00, z10, z01, z11,
		r1, r2, z;

	table_axis axis;

	if(lut == EFFICIENCY) {
		x_length = HARV_X_VALUES;
		y_length = HARV_Y_VALUES;
	}

	float x_values[x_length];
	float y_values[y_length];
	float z_values[x_length][y_length];

	if(lut == EFFICIENCY) {
		memcpy(x_values, solarcell_out_values, sizeof(solarcell_out_values));
		memcpy(y_values, batt_voltage_values, sizeof(batt_voltage_values));
		memcpy(z_values, eff_values, sizeof(eff_values));
	}

	axis = X_AXIS;
	get_piecewise_line_points_3D(xi, x_values, x, lut, axis);
	axis = Y_AXIS;
	get_piecewise_line_points_3D(yi, y_values, y, lut, axis);


	x0 = x_values[xi[0]];
	y0 = y_values[yi[0]];
	x1 = x_values[xi[1]];
	y1 = y_values[yi[1]];


	z00 = z_values[xi[0]][yi[0]];
	z01 = z_values[xi[0]][yi[1]];
	z10 = z_values[xi[1]][yi[0]];
	z11 = z_values[xi[1]][yi[1]];


	r1 = z00;
	r2 = z01;
	if(x0 != x1) {
		r1 = ((x1 - x) * z00 + (x - x0) *z10) / (x1 - x0);
		r2 = ((x1 - x) * z01 + (x - x0) * z11) / (x1 - x0);
	}

	z = r1;

	if(y0 != y1) {
		z = ((y1 - y) * r1 + (y - y0) * r2) / (y1 - y0);
	}

	return z;
}


void
get_piecewise_line_points_3D(uint16_t *points_pointer, float *array, float value,
						lut_type lut, table_axis axis)
{
	uint16_t *array_indices = points_pointer;
	uint16_t i, points_length;
	uint8_t value_in_range;

	points_length = 0;

	if(lut == EFFICIENCY) {
		if(axis == X_AXIS) {
			points_length = HARV_X_VALUES;
		} else if(axis == Y_AXIS) {
			points_length = HARV_Y_VALUES;
		}
	}

	i = value_in_range = 0;

	while(i < points_length) {
		if(value <= array[i]) {
			value_in_range = 1;
			break;
		}
		i++;
	}

	if(value_in_range) {
		if(i == 0) {
			array_indices[0] = array_indices[1] = 0;
		}
		else {
			array_indices[0] = i - 1;
			array_indices[1] = i;
		}
	}

	return;
}
