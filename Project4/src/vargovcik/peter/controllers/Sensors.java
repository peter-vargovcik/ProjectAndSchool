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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import vargovcik.peter.interfaces.SensorsInterface;

/**
 *
 * @author Peter Vargovcik
 */
public class Sensors {
    private static Sensors instance;
    private I2CBus bus;
    private I2CDevice device;
    private byte[] bytes = new byte[6];
    private byte I2C_ADDRESS = 0x6;
    private static List<SensorsInterface> sensorsInterfaceList = new ArrayList<SensorsInterface>();
    private boolean fetching = false;
    
    public static Sensors getInstance(SensorsInterface sensorsInterface){
        if(instance == null){
            instance = new Sensors();
        }
        sensorsInterfaceList.add(sensorsInterface);
        return instance;
    }
    
    private Sensors(){
        try {
            bus = I2CFactory.getInstance(I2CBus.BUS_1);
            
            //get device itself
            device = bus.getDevice(I2C_ADDRESS);
            System.out.println("Connected to Sensors OK!");
        } catch (IOException ex) {
            Logger.getLogger(Sensors.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void startFetching(){
        fetching = true;
        fetch.start();
    }
    
    public void stopFetching(){
        fetching = false;
    }
    
    Thread fetch = new Thread(new Runnable() {

        @Override
        public void run() {
            while(fetching){
                try {
                    int r = device.read(bytes,0, bytes.length);
                    
                    int lightSensor = ((bytes[1] & 0xff) << 8) | (bytes[2] & 0xff);
                    int distanceSensor = ((bytes[3] & 0xff) << 8) | (bytes[4] & 0xff);
                    
                    for(SensorsInterface iface : sensorsInterfaceList){
                        if(iface !=null){
                            iface.distance(distanceSensor);
                            iface.lightIntensity(lightSensor);
                        }
                    }
                    
                } catch (IOException ex) {
                    Logger.getLogger(Sensors.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Sensors.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    });
    
}
