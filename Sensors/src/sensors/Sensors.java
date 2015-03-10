/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sensors;

import java.io.IOException;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;

public class Sensors {

    I2CBus bus;
    I2CDevice device;
    byte[] bytes;
    float accelX_G;
    float accelY_G;
    float accelZ_G;
    double gyroXdeg;
    double gyroYdeg;
    double gyroZdeg;
    static int SENSITIVITY = 16384;  //sensor sencitivity

    public static void main(String[] ags) throws InterruptedException, IOException {
        Sensors sensors = null;
        Arduino02Test arduino02Test = null;
        Arduino01Test arduino01Test = null;

        try {
//            sensors = new Sensors();
//            arduino02Test = new Arduino02Test();
            arduino01Test = new Arduino01Test();

//            for (int i = 0; i < 20000; i++) {
//                if (i % 1000 == 0) {
////                    arduino02Test.readFromDevice();
//                    arduino01Test.writeToDevice();
//
//                }
//                Thread.sleep(1);
//            }
        } catch (IOException ex) {
            Logger.getLogger(Sensors.class.getName()).log(Level.SEVERE, null, ex);
        }
        arduino01Test.writeToDevice();
        //sensors.startReading();
    }

    /**
     * @param args
     */
    public Sensors() throws IOException {
        System.out.println("Starting sensors reading:");

        // get I2C bus instance
        //...
        //get i2c bus
        bus = I2CFactory.getInstance(I2CBus.BUS_1);
        System.out.println("Connected to bus OK!");

        //get device itself
        device = bus.getDevice(0x68);
        System.out.println("Connected to device OK!");

        //start sensing, using config registries 6B  and 6C    
        device.write(0x6B, (byte) 0b00000000);
        device.write(0x6C, (byte) 0b00000000);
        System.out.println("Configuring Device OK!");

        //config gyro
        device.write(0x1B, (byte) 0b00011000);
        //config accel    
        device.write(0x1C, (byte) 0b00000100);
        System.out.println("Configuring sensors OK!");

    }

    public void startReading() {
        for (int i = 0; i < 20; i++) {
            try {
                readingSensors();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
//        Task task = new Task<Void>() {
//            @Override
//            public Void call() {
//                try {
//                    readingSensors();
//                } catch (IOException e) {
//                }
//                return null;
//            } 
//        };
//        new Thread(task).start();
    }

    private void readingSensors() throws IOException {
        bytes = new byte[6 + 2 + 6];
        DataInputStream gyroIn;
        short accelX, accelY, accelZ;
        float tempX, tempY, tempZ;
        short temp;
        short gyroX, gyroY, gyroZ;

//        while (true) {
        int r = device.read(0x3B, bytes, 0, bytes.length);

        if (r != 14) {   //14 registries to be read, 6 for gyro, 6 for accel and 2 for temp
            System.out.println("Error reading data, < " + bytes.length + " bytes");
        }
        gyroIn = new DataInputStream(new ByteArrayInputStream(bytes));
        accelX = gyroIn.readShort();
        accelY = gyroIn.readShort();
        accelZ = gyroIn.readShort();
        temp = gyroIn.readShort();
        gyroX = gyroIn.readShort();
        gyroY = gyroIn.readShort();
        gyroZ = gyroIn.readShort();

        tempX = (float) accelX / SENSITIVITY;
        //Anything higher than 1 or lower than -1 is ignored
        accelX_G = (tempX > 1) ? 1 : ((tempX < -1) ? -1 : tempX);
        tempY = (float) accelY / SENSITIVITY;
        //Anything higher than 1, or lower than 01 is ignored
        accelY_G = (tempY > 1) ? 1 : ((tempY < -1) ? -1 : tempY);
        tempZ = ((float) accelZ / SENSITIVITY) * (-1); //sensor upsidedown, opposite value used
        accelZ_G = (tempZ > 1) ? 1 : ((tempZ < -1) ? -1 : tempZ);

//use accel data as desired...            
        gyroXdeg = gyroX * (2000d / (double) Short.MAX_VALUE);
        gyroYdeg = gyroY * (2000d / (double) Short.MAX_VALUE);
        gyroZdeg = gyroZ * (2000d / (double) Short.MAX_VALUE);

//Use the gyro values as desired..            
        double tempC = ((double) temp / 340d) + 35d;
        System.out.println("gyroXdeg:" + gyroXdeg + " gyroYdeg:" + gyroYdeg + " gyroZdeg:" + gyroZdeg);

        try {
            Thread.sleep(700);
        } catch (InterruptedException ex) {
            Logger.getLogger(Sensors.class.getName()).log(Level.SEVERE, null, ex);
        }
//        }
    }
//...

}
