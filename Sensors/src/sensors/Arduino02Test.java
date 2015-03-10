/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sensors;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import java.io.IOException;

/**
 *
 * @author Peter Vargovcik
 */
public class Arduino02Test {

    I2CBus bus;
    I2CDevice device;
    byte[] bytes = new byte[6];
    private byte I2C_ADDRESS = 0x6;

    public Arduino02Test() throws IOException {
        bus = I2CFactory.getInstance(I2CBus.BUS_1);
        System.out.println("Connected to bus OK!");

        //get device itself
        device = bus.getDevice(I2C_ADDRESS);
        System.out.println("Connected to device OK!");
        /*
        //start sensing, using config registries 6B  and 6C    
        device.write(0x6B, (byte) 0b00000000);
        device.write(0x6C, (byte) 0b00000000);
        System.out.println("Configuring Device OK!");

        //config gyro
        device.write(0x1B, (byte) 0b00011000);
        //config accel    
        device.write(0x1C, (byte) 0b00000100);
        System.out.println("Configuring sensors OK!");
        */
    }
    
    public void readFromDevice() throws IOException{
        int r = device.read(bytes,0, bytes.length);
        
        int lightSensor = ((bytes[1] & 0xff) << 8) | (bytes[2] & 0xff);
        int distanceSensor = ((bytes[3] & 0xff) << 8) | (bytes[4] & 0xff);
        System.out.println("{[lightSensor:"+lightSensor+"],[distanceSensor:"+distanceSensor+"]}");
    }
}
