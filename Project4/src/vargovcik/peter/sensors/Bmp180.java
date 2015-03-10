/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vargovcik.peter.sensors;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
//import com.pi4j.io.i2c.
import java.io.IOException;

/**
 *
 * @author Angie
 */
public class Bmp180 {

    //Callibration Data
    private static final int EEPROM_start = 0xAA;
    private static final int EEPROM_end = 0xBF;

    // EEPROM registers - these represent calibration data
    private short AC1;
    private short AC2;
    private short AC3;
    private int AC4;
    private int AC5;
    private int AC6;
    private short B1;
    private short B2;
    private short MB;
    private short MC;
    private short MD;

    //Variable common between temperature & pressure calculations
    private int B5;

    //Raspberry Pi's I2C bus
    private static final int i2cBus = 1;
    // Device address 
    private static final int address = 0x77;

    // Temperature Control Register Data
    private static final int controlRegister = 0xF4;
    private static final int callibrationBytes = 22;
    // Temperature read address
    private static final byte tempAddr = (byte) 0xF6;
    // Read temperature command
    private static final byte getTempCmd = (byte) 0x2E;
    //Uncompensated Temperature data
    private int UT;

    //I2C bus
    I2CBus bus;
    // Device object
    private I2CDevice bmp180;

    private DataInputStream bmp180CaliIn;
    private DataInputStream bmp180In;

    public Bmp180() {

        try {
            bus = I2CFactory.getInstance(I2CBus.BUS_1);
            System.out.println("Connected to bus OK!!!");

            //get device itself
            bmp180 = bus.getDevice(address);
            System.out.println("Connected to device OK!!!");

            //Small delay before starting
            Thread.sleep(500);

            //Gettin callibration data
            gettingCallibration();

            //read forever till hit Ctrl+C
            for (;;) {
                Thread.sleep(1000);
                readTemp();
            }
        } catch (IOException e) {
            System.out.println("Exception: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Interrupted Exception: " + e.getMessage());
        }
    }

    private void gettingCallibration() {
        try {
            byte[] bytes = new byte[callibrationBytes];

            //read all callibration data into byte array
            int readTotal = bmp180.read(EEPROM_start, bytes, 0, callibrationBytes);
            if (readTotal != 22) {
                System.out.println("Error bytes read: " + readTotal);
            }

            bmp180CaliIn = new DataInputStream(new ByteArrayInputStream(bytes));

            // Read each of the pairs of data as signed short
            AC1 = bmp180CaliIn.readShort();
            AC2 = bmp180CaliIn.readShort();
            AC3 = bmp180CaliIn.readShort();

            // Unsigned short Values
            AC4 = bmp180CaliIn.readUnsignedShort();
            AC5 = bmp180CaliIn.readUnsignedShort();
            AC6 = bmp180CaliIn.readUnsignedShort();

            //Signed sort values
            B1 = bmp180CaliIn.readShort();
            B2 = bmp180CaliIn.readShort();
            MB = bmp180CaliIn.readShort();
            MC = bmp180CaliIn.readShort();

            System.out.println("Callibration: " + AC1 + ":" + AC2 + ":" + AC3 + ":" + AC4 + ":" + AC5 + ":" + AC6 + ":" + B1 + ":" + B2 + ":" + MB + ":" + MC + ":");

        } catch (IOException e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    public float readTemp() {

        byte[] bytesTemp = new byte[2];

        try {
            bmp180.write(controlRegister, getTempCmd);
            Thread.sleep(500);

            int readTotal = bmp180.read(tempAddr, bytesTemp, 0, 2);
            if (readTotal < 2) {
                System.out.format("Error: %n bytes read/n", readTotal);
            }
            bmp180In = new DataInputStream(new ByteArrayInputStream(bytesTemp));
            UT = bmp180In.readUnsignedShort();

        } catch (IOException e) {
            System.out.println("Error reading temp: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Interrupted Exception: " + e.getMessage());
        }
        //calculate temperature
        int X1 = ((UT - AC6) * AC5) >> 15;
        int X2 = (MC << 11) / (X1 + MD);
        B5 = X1 + X2;
        float celsius = ((B5 + 8) >> 4) / 10;
        System.out.println("Temperature: " + celsius);
        return celsius;
    }

}
