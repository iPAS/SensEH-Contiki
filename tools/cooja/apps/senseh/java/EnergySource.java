/**
 * 
 */

/**
 * @author raza
 *
 */
public abstract class EnergySource {
	// examples of envValue :- wind speed, light intensity
	public abstract double getOutputPower(double envValue); 
	public abstract double getOutputEnergy(double envValue, double timeInterval);

}
