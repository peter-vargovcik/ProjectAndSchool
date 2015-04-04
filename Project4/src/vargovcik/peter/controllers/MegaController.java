/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vargovcik.peter.controllers;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Peter Vargovcik
 */
public class MegaController {
    
    //Raspberry Pi's I2C bus
    private static final int i2cBus = 1;
    // Device address 
    private static final int address = 0x5;
     //I2C bus
    I2CBus bus;
    // Device object
    private I2CDevice mega;

    private DataInputStream megaIn;
    private DataInputStream bmp180In;
    
    
     public MegaController() {

        try {
            bus = I2CFactory.getInstance(I2CBus.BUS_1);
            System.out.println("Connected to bus OK!!!");

            //get device itself
            mega = bus.getDevice(address);
            System.out.println("Connected to device OK!!!");
            
            System.out.println("Sending Command");
           
          
        } catch (IOException e) {
            System.out.println("Exception: " + e.getMessage());
        } 
        
//            System.out.println(" OK!!!");
    }

    public synchronized void setPanTilt(byte[] command) {
        try {
            int seepTime = 15;
            mega.write((byte) 4);
            
            mega.write(command[0]);
            
            mega.write((byte) 5);
            
            mega.write(command[1]);
            
        } catch (IOException ex) {
            Logger.getLogger(MegaController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
         
     

}
