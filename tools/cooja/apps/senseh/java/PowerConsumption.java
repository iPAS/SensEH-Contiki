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


@ClassDescription("Power Consumption Model for SensEH")
public class PowerConsumption implements Observer, OperatingModeListener, MSP430Constants{

  
  // Operating mode of the CPU has changed
  public void modeChanged(Chip source, int mode){
	if (source instanceof MSP430){
		updateCPUStats();
	}
	else {
		System.err.println ("PowerConsumption: Wong CPU!!! Exiting....");
		System.exit(-1);
	}
  }

  public void updateCPUStats (){

    long now = simulation.getSimulationTime();
    accumulateCPUModeTime (lastCPUMode, now - lastCPUUpdateTime );
    //System.out.println ("CPU State changed from "+ MSP430Constants.MODE_NAMES[lastCPUMode] + " to "+ MSP430Constants.MODE_NAMES[cpu.getMode()]);
    lastCPUMode = cpu.getMode();
    lastCPUUpdateTime = now;
  }

  private static Logger logger = Logger.getLogger(PowerConsumption.class);

  private Simulation simulation;
  private Mote mote; 
  private Chip cpu;
  private Radio radio;

  //cpu 
  /*public static String[] MODE_NAMES = {
    "active", "lpm0", "lpm1", "lpm2", "lpm3", "lpm4" 
  };*/
  private int lastCPUMode;
  // cpu statistics
  long cpuModeTimes[];
  long totalCpuModeTimes[];
  private long lastCPUUpdateTime;

  // radio states
  private boolean radioWasOn;
  private RadioState lastRadioState;
  private long lastRadioUpdateTime;


  // radio statistics
  long duration = 0;
  long radioOn = 0;
  long radioTx = 0;
  long radioRx = 0;
  long radioInterfered = 0;
  long radioIdle=0;
  long radioOff =0;

  long totalRadioOn =0;  
  long totalRadioTx = 0;
  long totalRadioRx = 0;
  long totalRadioInterfered = 0;
  long totalRadioIdle = 0;
  long totalRadioOff = 0; 


  double getPowerCPU (int mode){
	return CURRENT_CPU[mode]*VOLTAGE; 
  }

  double getPowerRadioTransmit (){
	return CURRENT_TRANSMIT*VOLTAGE;
  }

  double getPowerRadioListen (){
	return CURRENT_LISTEN*VOLTAGE;
  }


  void setVoltage(double v) {
	VOLTAGE= v; 
  }

  // Taken from SensorInfo.java
  // some taken from cooja/apps/powertracker/java/PowerTracker.java
  public static final long TICKS_PER_SECOND = 4096L;
  public double VOLTAGE; /*= 2.4;*/ // Riccardo 2.4 | we can use our computed current battery voltage!!!!
  //public static final double POWER_CPU = 1.800 * VOLTAGE;       /* mW */ // page 4 of data sheet 1800 micro W
  public static final double CURRENT_CPU[] = {1.800, 0.0545, /*WRONG! TODO FIXME*/ 0.0500 , 0.0110, 0.0011, 0.0002}; 
  
  //public static final double POWER_LPM = 0.0545*VOLTAGE;
  //{0.0500 * VOLTAGE, Not available ,0.0110 * VOLTAGE, 0.0011 * VOLTAGE, 0.0002 * VOLTAGE };      /* mW */ // Source: http://www.ti.com/lit/ds/symlink/msp430f1611.pdf  

  //public static final double POWER_TRANSMIT = 17.7 * VOLTAGE;   /* mW */ // 19.5 - 1.8 depends on tx power . Check Fig 7 https://eva.fing.edu.uy/pluginfile.php/62712/mod_resource/content/1/tmote-sky-datasheet.pdf 

  public static final double CURRENT_TRANSMIT = 17.7; //mA
  //public static final double POWER_LISTEN = 20.0 * VOLTAGE;     /* mW */ //21.8(cpu+listen)-1.8(cpu)
  public static final double CURRENT_LISTEN = 20.0;     //mA


  public enum RadioState {
    IDLE, RECEIVING, TRANSMITTING, INTERFERED
  }


  public PowerConsumption(final Simulation simulation, final Mote mote, double supplyVoltage) {
    this.simulation= simulation;
    this.mote = mote;
    this.VOLTAGE = supplyVoltage; 

    cpu = ((SkyMote)mote).getCPU();
    radio = mote.getInterfaces().getRadio();
   
     // to let cpu and radio inform their state changes to power consumption model
    cpu.addOperatingModeListener(this);
    radio.addObserver(this);

    totalRadioOn = totalRadioTx = totalRadioRx = totalRadioInterfered = totalRadioIdle= totalRadioOff= 0;
    totalCpuModeTimes = new long [MSP430Constants.MODE_NAMES.length];

    initStats();  
  }

  private void initStats(){

    lastCPUUpdateTime = lastRadioUpdateTime= simulation.getSimulationTime();

    lastCPUMode = cpu.getMode();
    cpuModeTimes = new long [MSP430Constants.MODE_NAMES.length];

    radioWasOn = radio.isRadioOn();
    duration = radioOn = radioTx = radioRx = radioInterfered = radioIdle = radioOff= 0;
    if (radio.isTransmitting()) {
      lastRadioState = RadioState.TRANSMITTING;
    } else if (radio.isReceiving()) {
      lastRadioState = RadioState.RECEIVING;
    } else if (radio.isInterfered()){
      lastRadioState = RadioState.INTERFERED;
    } else {
      lastRadioState = RadioState.IDLE;
    }
  }
  
  public void reset(){
    initStats();
  }

  public String radioStatistics() {
    return radioStatistics(true, true);
  }

  public String radioStatistics(boolean radioHW, boolean radioRXTX) {
    StringBuilder sb = new StringBuilder();      

    if (radioHW) {
      sb.append(String.format("ON " + (radioOn + " us ") + "%2.2f %%", 100.0*radioOn/duration) + "\n");
    }
    if (radioRXTX) {
      sb.append(String.format("TX " + (radioTx + " us ") + "%2.2f %%", 100.0*radioTx/duration) + "\n");
      sb.append(String.format("RX " + (radioRx + " us ") + "%2.2f %%", 100.0*radioRx/duration) + "\n");
      sb.append(String.format("INT " + (radioInterfered + " us ") + "%2.2f %%", 100.0*radioInterfered/duration) + "\n");
      sb.append(String.format("RX " + (radioIdle + " us ") + "%2.2f %%", 100.0*radioIdle/duration) + "\n");

    }
    return sb.toString();
  }


  public void update(Observable o, Object arg) {
    //if (o instanceof Msp802154Radio)
    updateRadioStats();
  }

  /*boolean radioStateChanged (){

    if ((lastRadioState != RadioState.TRANSMITTING) &&  radio.isTransmitting() ) {
	System.out.println ("Transmitting");
	return true;
    }

    if ((lastRadioState != RadioState.RECEIVING) &&  radio.isReceiving() ) {
	System.out.println ("Receiving");
	return true;
    }

    if ((lastRadioState != RadioState.INTERFERED) &&  radio.isInterfered() ) {
	System.out.println ("Interfering");
	return true;
    }

    if (radioWasOn && (!radio.isRadioOn()) ){
	System.out.println("Receive Checked");
	System.out.println("Radio Idle");
	return true;
    }

    return false;
  }*/


  
  public void updateRadioStats() {

    RadioEvent radioEv = radio.getLastEvent();
    if (radioEv==RadioEvent.CUSTOM_DATA_TRANSMITTED ||radioEv==RadioEvent.PACKET_TRANSMITTED)
	return; 

    //System.out.print(radioEv); 
    //if (radioEv != RadioEvent.CUSTOM_DATA_TRANSMITTED && radioEv != RadioEvent.PACKET_TRANSMITTED){
    	//System.out.println (radioEv);
	/*if (radioEv == RadioEvent.HW_ON){
		System.out.print ("Radio ON ON\t"+radioEv);
    	} else*/ //if  (radioEv == RadioEvent.HW_OFF){
	//	if (radio.isRadioOn())
        //		System.out.print ("\t ON");
	//	else
	//		System.out.print ("\t OFF");
    	//}
    //}
    //System.out.println();
 



    long now = simulation.getSimulationTime();
    //radioStateChanged ();

    accumulateDuration(now - lastRadioUpdateTime);
    /*
    System.out.println 
    ("Event:"+radioEv+"\tWas" + lastRadioState +"\t" + (radioWasOn?"radioWasON":"radioWasOFF") + "\t" + (radio.isRadioOn()?"radioIsOn":"radioIsOff") + "\t" +
      (radio.isTransmitting()?"isTransmitting":
        (radio.isReceiving()?"isReceiving":
          (radio.isInterfered()?"isInterfered":"isIdle"
          )
          + "\ttime\t" + (now - lastRadioUpdateTime)
        )
      )
    );*/ 

    // Radio on/off 
    if (radioWasOn) {
        accumulateRadioOn(now - lastRadioUpdateTime);
    	// Radio tx/rx 
    	if (lastRadioState == RadioState.TRANSMITTING) {
		
      		accumulateRadioTx(now - lastRadioUpdateTime);
      		//System.out.println ("Radio Status: TX" +radio.isTransmitting() + "\tRX" + radio.isReceiving() + "\tON" + radio.isRadioOn() + "\tINT" +  radio.isInterfered() ); 
    	} else if (lastRadioState == RadioState.RECEIVING) {
      		accumulateRadioRx(now - lastRadioUpdateTime);
    	} else if (lastRadioState == RadioState.INTERFERED) {
      		accumulateRadioIntefered(now - lastRadioUpdateTime);
    	} else if (lastRadioState == RadioState.IDLE) {
      		accumulateRadioIdle(now - lastRadioUpdateTime);
    	}
    } else {
	accumulateRadioOff(now - lastRadioUpdateTime);
    }

    if (radio.isRadioOn()){
    // Await next radio event 
    	if (radio.isTransmitting()) {
      		lastRadioState = RadioState.TRANSMITTING;
    	} /*else if (!radio.isRadioOn()) {
      		lastRadioState = RadioState.IDLE;
    	}*/ else if (radio.isInterfered()) {
      		lastRadioState = RadioState.INTERFERED;
    	} else if (radio.isReceiving()) {
      		lastRadioState = RadioState.RECEIVING;
    	} else {
      		lastRadioState = RadioState.IDLE;
    	}
    }
    radioWasOn = radio.isRadioOn();
    lastRadioUpdateTime = now;
  }

  protected void accumulateDuration(long t) {
    duration += t;
  }
  protected void accumulateRadioOn(long t) {
    radioOn += t;
    //System.out.println ("*** Radio ON: time "+ t+" ***" );
    totalRadioOn +=t;
  }
  protected void accumulateRadioTx(long t) {
    radioTx += t;
    //System.out.println ("RadioTx \t" + t); 
    totalRadioTx += t;
  }
  
  protected void accumulateRadioRx(long t) {
    radioRx += t;
    //System.out.println ("RadioRx \t" + t); 
    totalRadioRx += t;
  }

  protected void accumulateRadioIntefered(long t) {
    radioInterfered += t;
    //System.out.println ("RadioInt \t" + t); 
    totalRadioInterfered += t;
  }

  protected void accumulateRadioIdle(long t) {
    radioIdle += t;
    //System.out.println ("RadioListen \t" + t); 
    totalRadioIdle += t;
  }

  protected void accumulateRadioOff(long t) {
    radioOff += t;
    //System.out.println ("RadioOff \t" + t); 
    totalRadioOff += t;
  }

  protected void accumulateCPUModeTime(int mode, long t) {
    cpuModeTimes[mode]+= t;
    totalCpuModeTimes[mode]+= t;
  }

  public long getRadioONTimeInRxMode (){
    //return radioOn-radioTx-radioIdle;
    //return radioRx+radioInterfered;  
    //return radioOn - radioTx- radioIdle; 
    return radioOn - radioTx; 
  }

  public long getTotalRadioONTimeInRxMode (){
    //return totalRadioOn-totalRadioTx-totalRadioIdle; 
    //return totalRadioRx+ totalRadioInterfered;
    return totalRadioOn - totalRadioTx;
  }


  public double estimatePowerConsumption(){
   //System.out.println ("RADIO\t"+ duration + "\t" + radioOn +"\t"+ radioTx + "\t" + radioRx +"\t"+ radioInterfered+"\n");
   //System.out.println ("CPU\t"+ cpuModeTimes[MSP430Constants.MODE_ACTIVE] + "\t" + cpuModeTimes[MSP430Constants.MODE_LPM0] +"\t"+ cpuModeTimes[MSP430Constants.MODE_LPM1] + "\t" + cpuModeTimes[MSP430Constants.MODE_LPM2] +"\t"+ cpuModeTimes[MSP430Constants.MODE_LPM3]+"\n");  
   return    radioOn + radioTx +  radioRx + radioInterfered; 
  }

  public double getTotalConsumedEnergy (){

    long totalTimeInRadioRxMode = getTotalRadioONTimeInRxMode ();

    long totalTimeInLPM = totalCpuModeTimes[MSP430Constants.MODE_LPM0] + totalCpuModeTimes[MSP430Constants.MODE_LPM1] + totalCpuModeTimes[MSP430Constants.MODE_LPM2] + totalCpuModeTimes[MSP430Constants.MODE_LPM3] + totalCpuModeTimes[MSP430Constants.MODE_LPM4]; 

return (double)( 
totalCpuModeTimes[MSP430Constants.MODE_ACTIVE]* getPowerCPU (MSP430Constants.MODE_ACTIVE) + 
+ totalCpuModeTimes[MSP430Constants.MODE_LPM0]*getPowerCPU (MSP430Constants.MODE_LPM0)  + totalCpuModeTimes[MSP430Constants.MODE_LPM1]* getPowerCPU (MSP430Constants.MODE_LPM1)  + totalCpuModeTimes[MSP430Constants.MODE_LPM2] * getPowerCPU (MSP430Constants.MODE_LPM2)  + totalCpuModeTimes[MSP430Constants.MODE_LPM3]*getPowerCPU (MSP430Constants.MODE_LPM3)  + totalCpuModeTimes[MSP430Constants.MODE_LPM4]* getPowerCPU (MSP430Constants.MODE_LPM4) 
+totalTimeInRadioRxMode* getPowerRadioListen()
+totalRadioTx * getPowerRadioTransmit()
)/ 1000000.0;

  }
  
  // return average power consumption by radio and cpu in mW 
  public double getAveragePower() {
    //TODO: include all lpms
    updateRadioStats();
    updateCPUStats();     
    long timeInLPM = cpuModeTimes[MSP430Constants.MODE_LPM0] + cpuModeTimes[MSP430Constants.MODE_LPM1] + cpuModeTimes[MSP430Constants.MODE_LPM2] + cpuModeTimes[MSP430Constants.MODE_LPM3] + cpuModeTimes[MSP430Constants.MODE_LPM4]; 

    long timeInRadioRxMode = getRadioONTimeInRxMode ();

    long totalTimeInLPM = totalCpuModeTimes[MSP430Constants.MODE_LPM0] + totalCpuModeTimes[MSP430Constants.MODE_LPM1] + totalCpuModeTimes[MSP430Constants.MODE_LPM2] + totalCpuModeTimes[MSP430Constants.MODE_LPM3] + totalCpuModeTimes[MSP430Constants.MODE_LPM4]; 


   //System.out.println ("****CPU***\n"+ ( cpuModeTimes[MSP430Constants.MODE_ACTIVE] + timeInLPM) + "\t"+ cpuModeTimes[MSP430Constants.MODE_ACTIVE] + "\t" + timeInLPM +"\n");  
   //System.out.println (percentageTimeCPUActive() + "\t"+ percentageTimeCPUInLPM() +"\n");  


   //System.out.println ("*RADIO*\t"+ radioTx + "\t" + timeInRadioRxMode  + "\t" + radioOn);
   //System.out.println (percentageTimeInTx() + "\t"+ percentageTimeInRx() +"\n");  
   //System.out.println ("lpm " + timeInLPM +" ," +"cpu "+ cpuModeTimes[MSP430Constants.MODE_ACTIVE]+ " ,"+  "rx " + (radioOn-radioTx) + " ," + "tx "+  radioTx+" ,"); 


   //System.out.println ("TOTAL* lpm " + totalTimeInLPM +" ," +"cpu "+ totalCpuModeTimes[MSP430Constants.MODE_ACTIVE]+ " ,"+  "rx " + getTotalRadioONTimeInRxMode() + " ," + "tx "+  totalRadioTx+" ,"); 
   //System.out.println ("*TOTAL RADIO*\t" +  totalRadioTx+"\t" + getTotalRadioONTimeInRxMode() +  "\t" + totalRadioOn );

//estimatePowerConsumption();


    
    //return ( cpuModeTimes[MSP430Constants.MODE_ACTIVE]* POWER_CPU + timeInLPM * POWER_LPM +  timeInRadioRxMode* POWER_LISTEN + radioTx * POWER_TRANSMIT) / (cpuModeTimes[MSP430Constants.MODE_ACTIVE] + timeInLPM);

return ( 
cpuModeTimes[MSP430Constants.MODE_ACTIVE]* getPowerCPU (MSP430Constants.MODE_ACTIVE) + 
+ cpuModeTimes[MSP430Constants.MODE_LPM0]*getPowerCPU (MSP430Constants.MODE_LPM0)  + cpuModeTimes[MSP430Constants.MODE_LPM1]* getPowerCPU (MSP430Constants.MODE_LPM1)  + cpuModeTimes[MSP430Constants.MODE_LPM2] * getPowerCPU (MSP430Constants.MODE_LPM2)  + cpuModeTimes[MSP430Constants.MODE_LPM3]*getPowerCPU (MSP430Constants.MODE_LPM3)  + cpuModeTimes[MSP430Constants.MODE_LPM4]* getPowerCPU (MSP430Constants.MODE_LPM4) 
+timeInRadioRxMode* getPowerRadioListen()
+radioTx * getPowerRadioTransmit()
)
/ (cpuModeTimes[MSP430Constants.MODE_ACTIVE] + timeInLPM);

  }

  public double percentageTimeInTx(){
    long timeInLPM = cpuModeTimes[MSP430Constants.MODE_LPM0] + cpuModeTimes[MSP430Constants.MODE_LPM1] + cpuModeTimes[MSP430Constants.MODE_LPM2] + cpuModeTimes[MSP430Constants.MODE_LPM3] + cpuModeTimes[MSP430Constants.MODE_LPM4]; 
	return (100.0*radioTx) / (double)(cpuModeTimes[MSP430Constants.MODE_ACTIVE] + timeInLPM); 
  }

  public double percentageTimeInRx(){
    long timeInLPM = cpuModeTimes[MSP430Constants.MODE_LPM0] + cpuModeTimes[MSP430Constants.MODE_LPM1] + cpuModeTimes[MSP430Constants.MODE_LPM2] + cpuModeTimes[MSP430Constants.MODE_LPM3] + cpuModeTimes[MSP430Constants.MODE_LPM4]; 
	return 100.0*(radioOn-radioTx) / (double)(cpuModeTimes[MSP430Constants.MODE_ACTIVE] + timeInLPM); 
  }

  public double percentageTimeCPUActive(){
    long timeInLPM = cpuModeTimes[MSP430Constants.MODE_LPM0] + cpuModeTimes[MSP430Constants.MODE_LPM1] + cpuModeTimes[MSP430Constants.MODE_LPM2] + cpuModeTimes[MSP430Constants.MODE_LPM3] + cpuModeTimes[MSP430Constants.MODE_LPM4]; 
	return 100.0*cpuModeTimes[MSP430Constants.MODE_ACTIVE] / (double)(cpuModeTimes[MSP430Constants.MODE_ACTIVE] + timeInLPM); 
  }

  public double percentageTimeCPUInLPM(){
    long timeInLPM = cpuModeTimes[MSP430Constants.MODE_LPM0] + cpuModeTimes[MSP430Constants.MODE_LPM1] + cpuModeTimes[MSP430Constants.MODE_LPM2] + cpuModeTimes[MSP430Constants.MODE_LPM3] + cpuModeTimes[MSP430Constants.MODE_LPM4]; 
	return 100.0*timeInLPM / (double)(cpuModeTimes[MSP430Constants.MODE_ACTIVE] + timeInLPM); 
  }


/*
    public double getRadioOnRatio() {
      return 1.0*radioOn/duration;
    }

    public double getRadioTxRatio() {
      return 1.0*radioTx/duration;
    }

    public double getRadioInterferedRatio() {
      return 1.0*radioInterfered/duration;
    }

    public double getRadioRxRatio() {
      return 1.0*radioRx/duration;
    }
  */


  public void dispose() {
    radio.deleteObserver(this);
    radio = null;
    mote = null;
  }
/*
    public String toString() {
      return toString(true, true);
    }
    public String toString(boolean radioHW, boolean radioRXTX) {
      StringBuilder sb = new StringBuilder();
      String moteString = mote.toString().replace(' ', '_');

      sb.append(moteString + " MONITORED " + duration + " us\n");
      if (radioHW) {
        sb.append(String.format(moteString + " ON " + (radioOn + " us ") + "%2.2f %%", 100.0*getRadioOnRatio()) + "\n");
      }
      if (radioRXTX) {
        sb.append(String.format(moteString + " TX " + (radioTx + " us ") + "%2.2f %%", 100.0*getRadioTxRatio()) + "\n");
        sb.append(String.format(moteString + " RX " + (radioRx + " us ") + "%2.2f %%", 100.0*getRadioRxRatio()) + "\n");
        sb.append(String.format(moteString + " INT " + (radioInterfered + " us ") + "%2.2f %%", 100.0*getRadioInterferedRatio()) + "\n");
      }
      return sb.toString();
    }
  }

  private MoteTracker createMoteTracker(Mote mote) {
    
    if (moteRadio == null) {
      return null;
    }

    // Radio observer 
    MoteTracker tracker = new MoteTracker(mote);
    tracker.update(null, null);
    return tracker;
  }*/


/*

  //Useful functions copied from Contiki's CollectView Application

  public double getCPUPower() {
    return (values[TIME_CPU] * POWER_CPU) / (values[TIME_CPU] + values[TIME_LPM]);
  }

  public double getLPMPower() {
    return (values[TIME_LPM] * POWER_LPM) / (values[TIME_CPU] + values[TIME_LPM]);
  }

  public double getListenPower() {
    return (values[TIME_LISTEN] * POWER_LISTEN) / (values[TIME_CPU] + values[TIME_LPM]);
  }

  public double getTransmitPower() {
    return (values[TIME_TRANSMIT] * POWER_TRANSMIT) / (values[TIME_CPU] + values[TIME_LPM]);
  }



  public double getAverageDutyCycle(int index) {
      return (double)(values[index]) / (double)(values[TIME_CPU] + values[TIME_LPM]);
  }

  public long getPowerMeasureTime() {
    return (1000L * (values[TIME_CPU] + values[TIME_LPM])) / TICKS_PER_SECOND;
  }
*/

}
