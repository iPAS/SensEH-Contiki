/**
 * \file
 *         Battery parameters computation
 * \author
 *         Riccardo Dall'Ora <r.dallora[at]unitn[dot]it>
 */

#include "battery.h"
#include "battery-LUT.h"
#include "energy-cons.h"
#include "lookup-table.h"

#include "lib/random.h"

#define UPDATE_TIMEOUT      CLOCK_SECOND * 60

#if COOJA_SIM
#include <stdio.h>
#define PRINTF(...) printf(__VA_ARGS__)
#else
#define PRINTF(...)
#endif


static struct energy_storage battery;
static struct ctimer update_timer;

static float remaining_energy;

static uint32_t energy_spent;


uint8_t battery_is_charged(void)
{
    if(remaining_energy > 10000) {
        return 1;
    }
    else {
        PRINTF("Battery DEPLETED\n");
        remaining_energy = 0;
        ctimer_stop(&update_timer);
    }

    return 0;
}


void update_battery(void *n)
{
    float cons_value;

    cons_value = update_consumption();
    energy_spent += cons_value;

    discharge(cons_value);

    ctimer_set(&update_timer, UPDATE_TIMEOUT, update_battery, NULL);
}


void charge_battery(float energy)
{
    update_battery(NULL);

    charge(energy);
}


void
initialize_battery(void)
{
	battery.num_batteries = NUM_BATTERIES;
	battery.capacity = BATT_CAPACITY;
	battery.nominal_voltage = BATT_NOMINAL_V;
	battery.max_energy = battery.capacity * 3600 * battery.nominal_voltage;
	battery.min_operating_voltage = BATT_MIN_V;

	remaining_energy = battery.max_energy;
}


void
initialize_energy_data(void)
{
    initialize_battery();
    energy_spent = 0;

    reset_energest_times();

    ctimer_set(&update_timer, UPDATE_TIMEOUT + random_rand() % CLOCK_SECOND, update_battery, NULL);
}


float
get_charge(void)
{
	return (remaining_energy / (battery.nominal_voltage * 3600));
}


float
get_voltage(void)
{
	lut_type lut;
	float chrg;

	chrg = get_charge();
	lut = BATTERY;

	return get_y(lut, chrg);
}


uint16_t
get_num_batteries(void)
{
	return battery.num_batteries;
}


void
charge(float energy_mj)
{
	remaining_energy += (energy_mj / battery.num_batteries);

	if(remaining_energy > battery.max_energy) {
		remaining_energy = battery.max_energy;
	}
}


void
discharge(float energy_mj)
{
	remaining_energy -= (energy_mj / battery.num_batteries);

	if(remaining_energy < 0) {
		remaining_energy = 0;
	}
}


float
residual_battery_percentage(void)
{
	return 100 * get_charge() / battery.capacity;
}


void print_battery_data(void)
{
    PRINTF("Energy spent: %lumJ - My battery level %lumJ\n",
           (uint32_t) energy_spent, (uint32_t) (remaining_energy * battery.num_batteries));
}
