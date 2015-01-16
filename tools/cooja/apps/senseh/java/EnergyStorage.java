/**
 * 
 */

/**
 * @author raza
 *
 */
public abstract class EnergyStorage {
	abstract double getVoltage();
	abstract void charge(double energy_mj);
	abstract void discharge (double energy_mj);
	abstract double getEnergy (); 
	abstract int getNumStorages();
}
