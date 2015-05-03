import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import org.apache.log4j.Logger;
//import org.jdom.Element;
import se.sics.cooja.ClassDescription;
import se.sics.cooja.Mote;
//import se.sics.cooja.SimEventCentral.MoteCountListener;
import se.sics.cooja.Simulation;
import se.sics.cooja.interfaces.Radio;
import se.sics.mspsim.core.OperatingModeListener;

// TODO: Comment out the next two imports -- Only for debugging purposes
import se.sics.cooja.emulatedmote.Radio802154;
import se.sics.cooja.mspmote.interfaces.Msp802154Radio;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.Chip;
import se.sics.cooja.mspmote.SkyMote;
import se.sics.mspsim.core.MSP430Constants;

import se.sics.cooja.interfaces.Radio.RadioEvent;


/**
 * SensEH Project
 * Originated by 
 * @author raza
 * @see http://usmanraza.github.io/SensEH-Contiki/
 * 
 * Adopted and adapted by 
 * @author ipas
 * @since 2015-05-01
 */
@ClassDescription("Power Consumption Model for SensEH")
public class PowerConsumption implements OperatingModeListener, Observer, MSP430Constants{

  private static Logger logger = Logger.getLogger(PowerConsumption.class);

  private Simulation simulation;
  private Mote mote; 
  private Chip cpu;
  private Radio radio;

  // cpu modes 
  private int lastCPUMode;

  // cpu statistics
  long [] cpuModeTimes;
  long [] cpuModeTimesTotal;
  long [] cpuModeTimesTotalSnapshot;  
  private long lastCPUUpdateTime;

  protected void accumulateCPUModeTime(int mode, long t) {
    cpuModeTimes[mode] += t;
    cpuModeTimesTotal[mode] += t;
  }

  //--------------------------------------------------------------------------
  // radio states
  private boolean radioWasOn;
  private RadioState lastRadioState;
  private long lastRadioUpdateTime;

  // radio statistics, timers are recorded in micro-second
  public static class RadioTimes {
    public long duration;
    public long on;
    public long tx;
    public long rx;
    public long interfered;
    public long idle;
    public long off;
    public long [] multiTx;
    
    public RadioTimes(int multi) {
      multiTx = new long[multi];
      reset();
    }
    
    public void reset() {
      duration = on = tx = rx = interfered = idle = off = 0;
      for (int i = 0; i < multiTx.length; i++)
        multiTx[i] = 0;
    }
    
    public void setMemberValuesAs(final RadioTimes rt) {
      duration   = rt.duration;
      on         = rt.on;         
      tx         = rt.tx;         
      rx         = rt.rx;         
      interfered = rt.interfered; 
      idle       = rt.idle;       
      off        = rt.off;
      for (int i = 0; i < multiTx.length; i++)
        multiTx[i] = rt.multiTx[i];
    }
    
    public void addMemberValuesWith(final RadioTimes rt) {
      duration   += rt.duration;
      on         += rt.on;         
      tx         += rt.tx;         
      rx         += rt.rx;         
      interfered += rt.interfered; 
      idle       += rt.idle;       
      off        += rt.off;
      for (int i = 0; i < multiTx.length; i++)
        multiTx[i] += rt.multiTx[i];      
    }
    
    public void accumulateDuration(long t) {
      duration += t;
    }

    public void accumulateRadioOn(long t) {
      on += t;      
    }

    public void accumulateRadioTx(long t, int i) {
      tx += t;
      multiTx[i] += t;
    }
    
    public void accumulateRadioRx(long t) {
      rx += t;
    }

    public void accumulateRadioInterfered(long t) {
      interfered += t;
    }

    public void accumulateRadioIdle(long t) {
      idle += t;
    }

    public void accumulateRadioOff(long t) {
      off += t;
    }
    
  }
  
  private RadioTimes radioTimes;
  public RadioTimes radioTimesTotal;  
  public RadioTimes radioTimesTotalSnapshot;
  private int lastRadioTxIndicator; // iPAS: states in multiple transmission powers

  //--------------------------------------------------------------------------  
  // All modes of CPU, even Radio, are reported in mA, then powers are all in mW 
    
  // Taken from SensorInfo.java, some taken from cooja/apps/powertracker/java/PowerTracker.java
  public static final long TICKS_PER_SECOND = 4096L;
  public double VOLTAGE; // Riccardo =2.4 | we can use our computed current battery voltage!!!!    
  public static final double [] CURRENT_CPU = {1.800, 0.0545, /*WRONG! TODO FIXME*/ 0.0500 , 0.0110, 0.0011, 0.0002}; 
  
  double getPowerCPU(int mode) {
    return CURRENT_CPU[mode] * VOLTAGE;
  }
  
  void setVoltage(double v) {
    VOLTAGE = v;
  }

  public enum RadioState {
    IDLE, RECEIVING, TRANSMITTING, INTERFERED
  }
  
  public static class RadioCurrent { // mA
    public static final double TX = 17.7;
    public static final double RX = 20.0;
    public static final double INTERFERED = 20.0;
    public static final double IDLE = 20.0;
    public static final double OFF = 0.0;    
  } 
  
  //--------------------------------------------------------------------------  
  public PowerConsumption(final Simulation simulation, final Mote mote, double supplyVoltage) {
    this.simulation = simulation;
    this.mote = mote;
    this.VOLTAGE = supplyVoltage;

    cpu = ((SkyMote) mote).getCPU();
    radio = mote.getInterfaces().getRadio();

    // to let cpu and radio inform their state changes to power consumption model
    cpu.addOperatingModeListener(this);
    radio.addObserver(this);    
    cpuModeTimesTotal = new long[MSP430Constants.MODE_NAMES.length];
    cpuModeTimesTotalSnapshot = new long[MSP430Constants.MODE_NAMES.length];
    
    radioTimes = new RadioTimes(radio.getOutputPowerIndicatorMax()+1);
    radioTimesTotal = new RadioTimes(radio.getOutputPowerIndicatorMax()+1);
    radioTimesTotalSnapshot = new RadioTimes(radio.getOutputPowerIndicatorMax()+1);
    
    initStats();
  }

  private void initStats() {
    lastCPUUpdateTime = lastRadioUpdateTime = simulation.getSimulationTime();

    lastCPUMode = cpu.getMode();
    cpuModeTimes = new long[MSP430Constants.MODE_NAMES.length];

    radioWasOn = radio.isRadioOn();
    if (radio.isTransmitting()) {
      lastRadioState = RadioState.TRANSMITTING;
    } else if (radio.isReceiving()) {
      lastRadioState = RadioState.RECEIVING;
    } else if (radio.isInterfered()) {
      lastRadioState = RadioState.INTERFERED;
    } else {
      lastRadioState = RadioState.IDLE;
    }
    
    radioTimes.reset();
    
    lastRadioTxIndicator = radio.getCurrentOutputPowerIndicator();
  }

  public void reset(){
    initStats();
  }
  
  public void dispose() {
    radio.deleteObserver(this);
    radio = null;
    mote = null;
  }
  
  // --------------------------------------------------------------------------
  public void modeChanged(Chip source, int mode) { // Operating mode of the CPU has changed.
    // Some implement of the OperatingModeListener interface.
    if (source instanceof MSP430) {
      updateCPUStats();
    } else {
      System.err.println("PowerConsumption: Wrong CPU!!! Exiting....");
      System.exit(-1);
    }
  }
  
  public void updateCPUStats() {
    long now = simulation.getSimulationTime();
    accumulateCPUModeTime(lastCPUMode, now - lastCPUUpdateTime);
    /*
    System.out.println( "CPU State changed from " + 
        MSP430Constants.MODE_NAMES[lastCPUMode] + " to " + 
        MSP430Constants.MODE_NAMES[cpu.getMode()] );
    */
    lastCPUMode = cpu.getMode();
    lastCPUUpdateTime = now;
  }

  // --------------------------------------------------------------------------
  public void update(Observable o, Object arg) { // some implement of the Observer interface    
    updateRadioStats();
  }
  
  public void updateRadioStats() {
    RadioEvent radioEv = radio.getLastEvent();
    if (radioEv == RadioEvent.CUSTOM_DATA_TRANSMITTED || 
        radioEv == RadioEvent.PACKET_TRANSMITTED)
      return;
 
    long now = simulation.getSimulationTime();
    long duration = now - lastRadioUpdateTime;
    
    radioTimes.accumulateDuration(duration);
    radioTimesTotal.accumulateDuration(duration);
    
    // Radio On/Off 
    if (radioWasOn) { // Since previous time
      radioTimes.accumulateRadioOn(duration);
      radioTimesTotal.accumulateRadioOn(duration);
      
      // Radio TX/RX
      if (lastRadioState == RadioState.TRANSMITTING) {    
        radioTimes.accumulateRadioTx(duration, lastRadioTxIndicator);
        radioTimesTotal.accumulateRadioTx(duration, lastRadioTxIndicator);     
        
      } else if (lastRadioState == RadioState.RECEIVING) {
        radioTimes.accumulateRadioRx(duration);
        radioTimesTotal.accumulateRadioRx(duration);
      
      } else if (lastRadioState == RadioState.INTERFERED) {
        radioTimes.accumulateRadioInterfered(duration);
        radioTimesTotal.accumulateRadioInterfered(duration);
        
      } else if (lastRadioState == RadioState.IDLE) {
        radioTimes.accumulateRadioIdle(duration);
        radioTimesTotal.accumulateRadioIdle(duration);
        
      } else {
        System.err.println("PowerConsumption: Wrong lastRadioState!!! Exiting....");
        System.exit(-1);
      }
    } else {
      radioTimes.accumulateRadioOff(duration);
      radioTimesTotal.accumulateRadioOff(duration);
    }
    
    if (radio.isRadioOn()){ // Currently
      // Await next radio event 
      if (radio.isTransmitting()) {
        lastRadioState = RadioState.TRANSMITTING;

        /*
        System.out.format("TX power of Mote %d: %f %d\n", // iPAS
            mote.getID(), 
            radio.getCurrentOutputPower(), 
            radio.getCurrentOutputPowerIndicator()
            ); // It's a level of 0-31 where the max, '31', == 0 dBm          
        */
        lastRadioTxIndicator = radio.getCurrentOutputPowerIndicator();
          
      } /*else if (!radio.isRadioOn()) { lastRadioState = RadioState.IDLE; }*/ 
        else if (radio.isReceiving()) {
        lastRadioState = RadioState.RECEIVING;
      } else if (radio.isInterfered()) {
        lastRadioState = RadioState.INTERFERED;
      } else {
        lastRadioState = RadioState.IDLE;
      }
    }

    radioWasOn = radio.isRadioOn();
    lastRadioUpdateTime = now;
  }

  // -------------------------------------------------------------------------- 
  private double calculateCPUEnergy(long [] times) { // usec x mW
    return times[MSP430Constants.MODE_ACTIVE] * getPowerCPU(MSP430Constants.MODE_ACTIVE)
         + times[MSP430Constants.MODE_LPM0]   * getPowerCPU(MSP430Constants.MODE_LPM0)
         + times[MSP430Constants.MODE_LPM1]   * getPowerCPU(MSP430Constants.MODE_LPM1)
         + times[MSP430Constants.MODE_LPM2]   * getPowerCPU(MSP430Constants.MODE_LPM2)
         + times[MSP430Constants.MODE_LPM3]   * getPowerCPU(MSP430Constants.MODE_LPM3)
         + times[MSP430Constants.MODE_LPM4]   * getPowerCPU(MSP430Constants.MODE_LPM4);        
  }

  private double calculateRadioEnergyTx(RadioTimes rt) { // usec x mW
    return rt.tx * RadioCurrent.TX * VOLTAGE; 
  }
  
  private double calculateRadioEnergyStandby(RadioTimes rt) { // usec x mW
    return (
        (rt.rx         * RadioCurrent.RX         ) +        
        (rt.interfered * RadioCurrent.INTERFERED ) +
        (rt.idle       * RadioCurrent.IDLE       )) * VOLTAGE;
  }
  
  private double calculateRadioEnergySleep(RadioTimes rt) { // usec x mW
    return rt.off * RadioCurrent.OFF * VOLTAGE;
  }
  
  private double calculateRadioEnergy(RadioTimes rt) { // usec x mW
    return calculateRadioEnergyTx(rt) + calculateRadioEnergyStandby(rt) + calculateRadioEnergySleep(rt);
  }
  
  // --------------------------------------------------------------------------
  public long getCPUTimeInActive() {
    return cpuModeTimes[MSP430Constants.MODE_ACTIVE];
  }
  
  public long getCPUTimeInLPM() {
    return cpuModeTimes[MSP430Constants.MODE_LPM0]
        + cpuModeTimes[MSP430Constants.MODE_LPM1]
        + cpuModeTimes[MSP430Constants.MODE_LPM2]
        + cpuModeTimes[MSP430Constants.MODE_LPM3]
        + cpuModeTimes[MSP430Constants.MODE_LPM4];
  }
  
  public double getTotalConsumedEnergy() {     
    return ( // mJ on micro seconds div by 1e6 to converse
        calculateCPUEnergy(cpuModeTimesTotal) + 
        calculateRadioEnergy(radioTimesTotal)) / (double)1000000.0;
  }
  
  public double getAveragePower() { // return average power consumption by radio and cpu in mW 
    updateCPUStats();
    updateRadioStats();
    return 
        calculateCPUEnergy(cpuModeTimes) / (getCPUTimeInActive() + getCPUTimeInLPM()) + 
        calculateRadioEnergy(radioTimes) / radioTimes.duration;
  }

  // --------------------------------------------------------------------------
  public static double percentage(long num, long den) {
    return (double)(100 * num) / den;
  }

  public double percentageTimeRadioTx(RadioTimes rt) {
    return percentage(rt.tx, rt.duration);
  }

  public double percentageTimeRadioStandby(RadioTimes rt) {
    return percentage(rt.on - rt.tx, rt.duration);
  }

  public double percentageTimeCPUActive() {
    return (double)(100 * getCPUTimeInActive()) / (getCPUTimeInActive() + getCPUTimeInLPM());
  }

  public double percentageTimeCPUInLPM() {
    return (double)(100 * getCPUTimeInLPM()) / (getCPUTimeInActive() + getCPUTimeInLPM());
  }

  // --------------------------------------------------------------------------
  public String radioStatistics() {
    return radioStatistics(true, true, true, false, radioTimesTotal, String.format("Mote %d: ", mote.getID()));
  }

  public static String radioStatistics(boolean radioHW, 
                                       boolean radioRXTX, 
                                       boolean showDuration,
                                       boolean showIdle,
                                       RadioTimes rt, String prefix) {
    // the idea is also brought from PowerTracker
    StringBuilder sb = new StringBuilder();      
    String fm = "%d us %2.2f %%\n";

    if (showDuration) {
      sb.append(prefix + String.format("MONITORED %d us\n", rt.duration));
    }
    if (radioHW) {
      sb.append(prefix + String.format("ON " + fm, rt.on, percentage(rt.on, rt.duration)));
    }
    if (radioRXTX) {
      sb.append(prefix + String.format("TX " + fm, rt.tx, percentage(rt.tx, rt.duration)));
      sb.append(prefix + String.format("RX " + fm, rt.rx, percentage(rt.rx, rt.duration)));
      sb.append(prefix + String.format("INT " + fm, rt.interfered, percentage(rt.interfered, rt.duration)));
    }
    if (showIdle) {
      sb.append(prefix + String.format("IDLE " + fm, rt.idle, percentage(rt.idle, rt.duration)));
    }
    
    return sb.toString();
  }
   
  //--------------------------------------------------------------------------
  public void snapStatistics() { // Keep snapshorts of CPU and Radio times
    for (int i = 0; i < cpuModeTimesTotal.length; i++)
      cpuModeTimesTotal[i] = cpuModeTimesTotalSnapshot[i];  
    radioTimesTotalSnapshot.setMemberValuesAs(radioTimesTotal);
  }
  
  public String getSnappedStatistics() {
    StringBuilder sb = new StringBuilder();

    double energyCPU = calculateCPUEnergy(cpuModeTimesTotalSnapshot) / 1e6;
    double energyRadioTx = calculateRadioEnergyTx(radioTimesTotalSnapshot) / 1e6;
    double energyRadioStandby = calculateRadioEnergyStandby(radioTimesTotalSnapshot) / 1e6;
    double energyRadioSleep = calculateRadioEnergySleep(radioTimesTotalSnapshot) / 1e6;
    
    sb.append("cpu:mJ="  + energyCPU + ", ");
    sb.append("tx:mJ=" + energyRadioTx + ", ");
    sb.append("stby:mJ=" + energyRadioStandby + ", ");
    sb.append("sleep:mJ=" + energyRadioSleep + ", ");
    
    return sb.toString();
  }
  
}
