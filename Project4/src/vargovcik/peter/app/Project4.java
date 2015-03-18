/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vargovcik.peter.app;

import com.pi4j.io.gpio.PinState;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialFactory;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;
import vargovcik.peter.compationApp.CompanionAppInterface;
import vargovcik.peter.compationApp.CompanionAppServer;
import vargovcik.peter.controllers.GPIOController;
import vargovcik.peter.controllers.PeripheralsController;
import vargovcik.peter.controllers.Trex;
import vargovcik.peter.interfaces.SensorsInterface;
import vargovcik.peter.sensors.Bmp180;

/**
 *
 * @author Peter Vargovcik
 */
public class Project4 {

    private Trex trex;
    private PeripheralsController peripheralsController;
    private GPIOController gpioController;
    private boolean operatorInControll = false;
    private long currentMilis = System.currentTimeMillis();
    

    public Project4() {
        initAll();
        enalbleVideoStream();
        startCompanionAppServer();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new Project4();

        /*  // Motor Test Java
         final Serial serial = SerialFactory.createInstance();

         serial.open(Serial.DEFAULT_COM_PORT, 9600);
         System.out.println("Powering motor");
         serial.write(new byte[]{(byte) 75,(byte) 203});

         try {
         // wait 1 second before continuing
         Thread.sleep(1000);
         } catch (InterruptedException ex) {
         Logger.getLogger(Project4.class.getName()).log(Level.SEVERE, null, ex);
         }
        
         System.out.println("Stoping motor");
         serial.write((byte) 0);
        
         System.out.println("Done");
         */
        /*
         Bmp180 bmp180 = new Bmp180();
         System.out.println("Temerature: "+bmp180.readTemp());
         */
        //trex.forward(20, 500);
        // Start Video streaming
    }

    private void initAll() {
        trex = Trex.instance;
                
        peripheralsController = PeripheralsController.instance;
        gpioController = GPIOController.instance;
    }

    private void enalbleVideoStream() {
        try {
            ProcessBuilder pb = new ProcessBuilder("sudo", "bash", "/home/pi/p4commands/start_stream.sh");
            pb.redirectErrorStream(true);
            Process proc = pb.start();

            Reader reader = new InputStreamReader(proc.getInputStream());
            int ch;
            StringBuilder sb = new StringBuilder();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            reader.close();
            System.out.println(sb.toString());
        } catch (IOException ex) {
            Logger.getLogger(Project4.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Project4.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    CompanionAppInterface companionAppInterface = new CompanionAppInterface() {

        @Override
        public void stopSearch() {
            System.out.println("stopSearch");
        }

        @Override
        public void startSearch() {
            System.out.println("startSearch");
        }

        @Override
        public void remoteControll(boolean isRemoteControlled) {
            if (operatorInControll != isRemoteControlled) {
                System.out.println("remoteControll:" + isRemoteControlled + " current state: " + operatorInControll);
            }
            operatorInControll = isRemoteControlled;

        }

        @Override
        public void remoteControlCommand(byte[] command) {
            if (command != null && operatorInControll) {
//                System.out.println("controled");
                trex.trexExecute(command);
            }
        }

        @Override
        public void connectionBroken() {
            System.out.println("connectionBroken");
        }

        @Override
        public void panTiltContollCommand(byte[] command) {
            if (command != null && operatorInControll) {                
                try {
                    peripheralsController.moveCamera(command[0], command[1]);
                } catch (IOException ex) {
                    Logger.getLogger(Project4.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }

        @Override
        public void platformMaxPower(int power) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void ignoreProximity(boolean ignore) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    };

    private void startCompanionAppServer() {
        // Companion App Server thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Thread Server");
                new CompanionAppServer(companionAppInterface, 8000).start();
                System.out.println("Thread Executed");
                gpioController.setPin1(PinState.HIGH);
            }
        }).start();
    }

}
