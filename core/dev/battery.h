/**
 * \file
 *         Battery functions
 * \author
 *         Riccardo Dall'Ora <r.dallora[at]unitn[dot]it>
 */

#ifndef __BATTERYMODEL_H__
#define __BATTERYMODEL_H__

#include "contiki.h"
#include "LUT-points.h"


#ifndef NUM_BATTERIES
#define NUM_BATTERIES			2
#endif // NUM_BATTERIES

#ifndef BATT_CAPACITY
#define BATT_CAPACITY			2100
#endif // BATT_CAPACITY

#ifndef BATT_NOMINAL_V
#define BATT_NOMINAL_V			1.2
#endif // BATT_NOMINAL_V

#ifndef BATT_MIN_V
#define BATT_MIN_V				1.0
#endif // BATT_MIN_V


struct energy_storage {
	uint8_t num_batteries;
	uint32_t capacity;
	float nominal_voltage;
	uint32_t max_energy;
	float min_operating_voltage;
};

struct metric_data {
    uint32_t batt;
    uint16_t harv;
    uint16_t cons;
};


void initialize_battery(void);
void initialize_energy_data(void);

float get_charge(void);
float get_voltage(void);
uint16_t get_num_batteries(void);

void charge_battery(float energy);
void charge(float energy_mj);
void discharge(float energy_mj);

float residual_battery_percentage(void);

uint8_t battery_is_charged(void);

void print_battery_data(void);

#endif // __BATTERYMODEL_H__
