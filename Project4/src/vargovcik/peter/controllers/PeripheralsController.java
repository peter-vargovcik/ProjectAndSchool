/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vargovcik.peter.controllers;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Peter Vargovcik
 */
public enum PeripheralsController {
    instance;
    
    private final int NEW_COMMAND_BYTE = 0;  
    private final int PAN_ROTATION_BYTE = 1;  
    private final int TILT_ROTATION_BYTE = 2;  
    private final int UNUSED_COMMAND1_BYTE = 3;  
    private final int UNUSED_COMMAND2_BYTE = 4;  
    private final int UNUSED_COMMAND3_BYTE = 5;      
    
    private final byte UNUSED_BYTE = (byte) 0b00000000;;  
    
    private I2CBus bus;
    private I2CDevice device;
    private byte[] commandBytes = new byte[6];
    private byte I2C_ADDRESS = 0x5;
    
    private PeripheralsController(){
        try {
            bus = I2CFactory.getInstance(I2CBus.BUS_1);            
            //get device itself
            device = bus.getDevice(I2C_ADDRESS);
            System.out.println("Connected to Peripherals OK!");
        } catch (IOException ex) {
            Logger.getLogger(Sensors.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void moveCamera(byte pan, byte tilt) throws IOException{
        commandBytes[NEW_COMMAND_BYTE]  = (byte) 0b11111111;
        commandBytes[PAN_ROTATION_BYTE] = pan;
        commandBytes[TILT_ROTATION_BYTE] = tilt;
        commandBytes[UNUSED_COMMAND1_BYTE] = UNUSED_BYTE;
        commandBytes[UNUSED_COMMAND2_BYTE] = UNUSED_BYTE;
        commandBytes[UNUSED_COMMAND3_BYTE] = UNUSED_BYTE;
        
        device.write(commandBytes, 0, 6);
    }
    
}
