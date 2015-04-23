/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vargovcik.peter.controllers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialFactory;

/**
 *
 * @author Peter Vargovcik
 */
public enum Trex {

    instance;

    private final int UART_DEVICE_ID = 40;
    private final int BAUD_RATE = 9600;
    private final int TURNING_POWER = 30;

    private Serial serial;

    private Trex() {
        serial = SerialFactory.createInstance();        

        try {
            serial.open(Serial.DEFAULT_COM_PORT, BAUD_RATE);
            serial.write((byte) 0);
        } catch (IllegalStateException ex) {
            Logger.getLogger(Trex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Trex.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private byte mapValueToByte(int value, int inMin, int inMax, int outMin, int outMax) {
        return (byte) ((value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin);
    }

    private int mapValue(int value, int inMin, int inMax, int outMin, int outMax) {
        return ((value - inMin) * (outMax - outMin) / (inMax - inMin) + outMin);
    }

    private synchronized void executeCommand(byte[] command, int duration) {
        try {
            serial.write(command);
        } catch (IllegalStateException ex) {
            Logger.getLogger(Trex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Trex.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ex) {
            Logger.getLogger(Trex.class.getName()).log(Level.SEVERE, null, ex);
        }
        stop();
    }

    private synchronized void executeCommand(byte[] command) {
        try {
            serial.write(command);
        } catch (IllegalStateException ex) {
            Logger.getLogger(Trex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Trex.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void forward(int power, int duration) {
        byte leftMotor = mapValueToByte(power, 0, 100, 65, 127);
        byte rightMotor = mapValueToByte(power, 0, 100, 193, 255);
        byte[] command = {leftMotor, rightMotor};
        executeCommand(command, duration);
    }

    public void forward(int power) {
        byte leftMotor = mapValueToByte(power, 0, 100, 65, 127);
        byte rightMotor = mapValueToByte(power, 0, 100, 193, 255);
        byte[] command = {leftMotor, rightMotor};
        executeCommand(command);
    }

    public void reverse(int power, int duration) {
        byte leftMotor = mapValueToByte(power, 0, 100, 63, 1);
        byte rightMotor = mapValueToByte(power, 0, 100, 191, 128);
        byte[] command = {leftMotor, rightMotor};
        executeCommand(command, duration);
    }

    public void reverse(int power) {
        byte leftMotor = mapValueToByte(power, 0, 100, 63, 1);
        byte rightMotor = mapValueToByte(power, 0, 100, 191, 128);
        byte[] command = {leftMotor, rightMotor};
        executeCommand(command);
    }

    public void leftTurn(int angle) {
        byte leftMotor = mapValueToByte(TURNING_POWER, 0, 100, 63, 1);
        byte rightMotor = mapValueToByte(TURNING_POWER, 0, 100, 193, 255);
        int duration = mapValue(angle, 0, 360, 0, 4000);
        byte[] command = {leftMotor, rightMotor};
        executeCommand(command, duration);
    }

    public void rightTurn(int angle) {
        byte leftMotor = mapValueToByte(TURNING_POWER, 0, 100, 65, 127);
        byte rightMotor = mapValueToByte(TURNING_POWER, 0, 100, 191, 128);
        int duration = mapValue(angle, 0, 360, 0, 4000);
        byte[] command = {leftMotor, rightMotor};
        executeCommand(command, duration);
    }

    public synchronized void stop() {
        byte[] command = {(byte) 0};
        try {
            serial.write(command);
        } catch (IllegalStateException ex) {
            Logger.getLogger(Trex.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Trex.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void trexExecute(byte[] command) {
        if (command.length == 2) {
            executeCommand(command);
        }
    }

}
