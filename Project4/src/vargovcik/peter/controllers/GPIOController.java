/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vargovcik.peter.controllers;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Peter Vargovcik
 */
public enum GPIOController {
    instance;
    
    private GpioController gpio;
    private GpioPinDigitalOutput 
            pinRGBLedRed,pinRGBLedGreen,pinRGBLedBlue, 
            pinMiscellaneous01, pinMiscellaneous02, pinMiscellaneous03, pinMiscellaneous04, pinMiscellaneous05, 
            pinRelaySerial, pinRelayHeadlight, pinRelay03, pinRelay04;
    
    private boolean emergencyLightsRunning,firefoundCondition;
    private static int EMERGENCY_LIGHT_ON_TIMEOUT = 250, FIRE_FOUND_TIMEOUT = 700;
    private Thread rgbEmergency,fireFoundThread;
    
    private GPIOController(){
        firefoundCondition = false;
        emergencyLightsRunning = false;
        gpio = GpioFactory.getInstance();
        setPins();
    }
    
    private void setPins() {
        pinRGBLedRed        = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_12, "RGB_RED", PinState.HIGH);
        pinRGBLedGreen      = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_13, "RGB_GREEN", PinState.HIGH);
        pinRGBLedBlue       = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_14, "RGB_BLUE", PinState.HIGH); 
        pinMiscellaneous01  = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_21, "MISC1", PinState.LOW);
        pinMiscellaneous02  = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_22, "MISC2", PinState.LOW);
        pinMiscellaneous03  = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_23, "MISC3", PinState.LOW);
        pinMiscellaneous04  = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_24, "MISC4", PinState.LOW);
        pinMiscellaneous05  = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_25, "MISC5", PinState.LOW); 
        pinRelaySerial      = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "TREX_SERIAL", PinState.HIGH);
        pinRelayHeadlight   = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "HEADLIGHT", PinState.HIGH);
        pinRelay03          = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "RELEAY3", PinState.HIGH);
        pinRelay04          = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "RELEAY4", PinState.HIGH);
    }
    
    public synchronized  void setRGB(PinState pinRed,PinState pinGreen,PinState pinBlue){
        pinRGBLedRed.setState(pinRed);
        pinRGBLedGreen.setState(pinGreen);
        pinRGBLedBlue.setState(pinBlue);
    }
    
    public synchronized  void setRGBRed(PinState pinRed){
        pinRGBLedRed.setState(pinRed);
    }
    
    public synchronized  void setRGBGreen(PinState pinGreen){
        pinRGBLedGreen.setState(pinGreen);
    }
    
    public synchronized  void setRGBBlue(PinState pinBlue){
        pinRGBLedBlue.setState(pinBlue);
    }
    
    public synchronized void setRedLed(PinState pinState){
        pinMiscellaneous01.setState(pinState);
    }
    
    public synchronized void setGreenLed(PinState pinState){
        pinMiscellaneous02.setState(pinState);
    }
    
    public synchronized void setSerialRelay(PinState pinState){
        pinRelaySerial.setState(pinState);
    }
    
    public synchronized void setHeadlight(PinState pinState){
        pinRelayHeadlight.setState(pinState);
    }
    
    public void startRGBEmergency(){
        if(rgbEmergency == null){
            emergencyLightsRunning = true;
            rgbEmergency = new Thread(rgbEmergencyRunnable);
            rgbEmergency.start();
        }
    }
    
    public void stopRGBEmergency(){
        if(rgbEmergency !=null){
            emergencyLightsRunning = false;
            rgbEmergency = null;
        }
        
    }
    
    public void tearDown(){
        gpio.shutdown();
    }
    
    private Runnable rgbEmergencyRunnable = new Runnable(){

        @Override
        public synchronized void run() {
            try {
                    Thread.sleep(EMERGENCY_LIGHT_ON_TIMEOUT*2);
                } catch (InterruptedException ex) {
                    Logger.getLogger(GPIOController.class.getName()).log(Level.SEVERE, null, ex);
                }
            while(emergencyLightsRunning){
                setRGBRed(PinState.LOW);
                setRGBBlue(PinState.HIGH);
                try {
                    Thread.sleep(EMERGENCY_LIGHT_ON_TIMEOUT);
                } catch (InterruptedException ex) {
                    Logger.getLogger(GPIOController.class.getName()).log(Level.SEVERE, null, ex);
                }
                setRGBRed(PinState.HIGH);
                setRGBBlue(PinState.LOW);
                try {
                    Thread.sleep(EMERGENCY_LIGHT_ON_TIMEOUT);
                } catch (InterruptedException ex) {
                    Logger.getLogger(GPIOController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            setRGBBlue(PinState.HIGH);
            setRGBRed(PinState.HIGH);
        }
    };

    public void fireFound() {
        if(fireFoundThread == null){
            firefoundCondition = true;
            fireFoundThread = new Thread(fireFoundRunnable);
            fireFoundThread.start();
        }
    }
    
    public void stopFireFoundEvent(){
        if(fireFoundThread !=null){
            firefoundCondition = false;
            fireFoundThread = null;
        }
        
    }
    
    private  Runnable fireFoundRunnable = new Runnable(){

        @Override
        public synchronized void run() {
             while(firefoundCondition){
                setRGBRed(PinState.LOW);
                setHeadlight(PinState.LOW);
                try {
                    Thread.sleep(FIRE_FOUND_TIMEOUT);
                } catch (InterruptedException ex) {
                    Logger.getLogger(GPIOController.class.getName()).log(Level.SEVERE, null, ex);
                }
                setRGBRed(PinState.HIGH);
                setHeadlight(PinState.HIGH);
                try {
                    Thread.sleep(FIRE_FOUND_TIMEOUT);
                } catch (InterruptedException ex) {
                    Logger.getLogger(GPIOController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            setRGBRed(PinState.HIGH);
            setHeadlight(PinState.HIGH);
        }
    };
}
