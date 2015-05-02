/**
 * \file
 *          Test-application for SensEH - Memory mode
 * \author
 *          Riccardo Dall'Ora
 */

#include "contiki.h"
#include "dev/serial-line.h"
#include "battery.h"

#include "energy-harv.h"

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#if COOJA_SIM
#include <stdio.h>
#define PRINTF(...) printf(__VA_ARGS__)
#else
#define PRINTF(...)
#endif

/*---------------------------------------------------------------------------*/
PROCESS(senseh_test_process, "SensEH test process");

AUTOSTART_PROCESSES(&senseh_test_process);
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(senseh_test_process, ev, data)
{
    static uint16_t sensor_value;

    PROCESS_BEGIN();

	set_num_cells(NUM_CELLS);
	initialize_energy_data(); // Update the consumption and residue energy

    while(battery_is_charged()) {
        PROCESS_WAIT_EVENT_UNTIL(ev == serial_line_event_message);

        sensor_value = atoi(data);

        get_harvested_energy(sensor_value, LIGHT_TIMER);
        print_incoming_power();
        print_battery_data();
    }

    PROCESS_END();
}
/*---------------------------------------------------------------------------*/
