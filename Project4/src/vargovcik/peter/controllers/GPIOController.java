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

/**
 *
 * @author Peter Vargovcik
 */
public enum GPIOController {
    instance;
    
    private GpioController gpio;
    private GpioPinDigitalOutput pin1, pin2, pin3;
    
    private GPIOController(){
        gpio = GpioFactory.getInstance();
        pin1 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "RedLed", PinState.LOW);
        pin2 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "GreenLed1", PinState.LOW);
        pin3 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "GreenLed2", PinState.LOW);
    }
    
    public void setPin1(PinState pinState){
        pin1.setState(pinState);
    }
    
    public void setPin2(PinState pinState){
        pin2.setState(pinState);
    }
    
    public void setPin3(PinState pinState){
        pin3.setState(pinState);
    }
    
    public void tearDown(){
        gpio.shutdown();
    }
    
}
