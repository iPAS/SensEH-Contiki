/**
 * \file
 *         Energy Harvester parameters computation
 * \author
 *         Riccardo Dall'Ora <r.dallora[at]unitn[dot]it>
 */


#include "energy-harv.h"
#include "battery.h"
#include "cell-LUT.h"
#include "lookup-table.h"

#define CALIBRATION_CONSTANT		0.596


#if COOJA_SIM
#include <stdio.h>
#define PRINTF(...) printf(__VA_ARGS__)
#else
#define PRINTF(...)
#endif


static uint8_t num_cells;

static float cell_power;
static float energy_harvested;


void
set_num_cells(uint16_t cells)
{
	num_cells = cells;
	PRINTF("Num cells: %u\n", num_cells);
}


float
get_power(uint16_t raw_light)
{
	float power, efficiency, lux, voltage, cell_pow_mw;

	lut_type lut;
	lux = raw_light * CALIBRATION_CONSTANT;

	lut = POWER;
	power = get_y(lut, lux);

	cell_pow_mw = power / 1000;
	voltage = get_voltage() * get_num_batteries();

	lut = EFFICIENCY;
	efficiency = get_z(lut, cell_pow_mw, voltage);

	return (cell_pow_mw * efficiency);
}


void
get_harvested_energy(uint16_t raw_light, float time_interval)
{
	float harv_energy;
	cell_power = num_cells * get_power(raw_light);
	harv_energy = cell_power * time_interval;

	energy_harvested += harv_energy;
	PRINTF("Energy harv in period: %umJ\n", (uint16_t) harv_energy);

	charge_battery(harv_energy);
}


void print_incoming_power(void)
{
    PRINTF("Cells Pow: %uuW - Total energy harv: %lumJ\n",
           (uint16_t) (cell_power * 1000), (uint32_t) energy_harvested);
}


void reset_energy_harvested(void)
{
	energy_harvested = 0;
}
