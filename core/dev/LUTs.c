/**
 * \file
 *         Lookup table values
 * \author
 *         Riccardo Dall'Ora <r.dallora[at]unitn[dot]it>
 */

#include "battery-LUT.h"
#include "cell-LUT.h"
#include "harv-LUT.h"


/* BATTERY LOOKUP TABLE */
const float
charge_values[BATTERY_VALUES] =
{
	0.00, 60.03, 136.73, 230.12, 403.54, 940.49, 2641.39, 2968.23, 3141.66
};

const float
voltage_values[BATTERY_VALUES] =
{
	1.00, 1.05, 1.10, 1.15, 1.20, 1.25, 1.30, 1.35, 1.40
};




/* SOLAR CELL LOOKUP TABLE */
const float
lux_values[POWER_VALUES] =
{
	0, 50, 130, 200, 1000
};

const float
power_values[POWER_VALUES] =
{
	0, 54.6, 163.8, 252, 1260
};



/* EH LOOKUP TABLE */
const float solarcell_out_values[HARV_X_VALUES] =
{
	5, 50, 100, 150, 200, 250, 300
};

const float batt_voltage_values[HARV_Y_VALUES] =
{
	2.0, 2.1, 2.2, 2.3, 2.4, 2.5
};


const float
eff_values[HARV_X_VALUES][HARV_Y_VALUES] =
{
	{0.775, 0.775, 0.775, 0.776, 0.778, 0.779} ,
	{0.775, 0.775, 0.776, 0.775, 0.778, 0.78} ,
	{0.777, 0.777, 0.778, 0.782, 0.784, 0.785} ,
	{0.777, 0.778, 0.78, 0.789, 0.795, 0.788} ,
	{0.78, 0.78, 0.787, 0.791, 0.798, 0.79} ,
	{0.786, 0.787, 0.789, 0.798, 0.799, 0.794} ,
	{0.787, 0.789, 0.792, 0.8, 0.803, 0.809} ,
};
