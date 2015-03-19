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
import vargovcik.peter.controllers.ProximityController;
import vargovcik.peter.controllers.Sensors;
import vargovcik.peter.controllers.Trex;
import vargovcik.peter.interfaces.ProximityInterface;
import vargovcik.peter.interfaces.SearchInterface;
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
    
    public static boolean inSearch = true, operatorInControll = false, overrideProximity = false, searchIsPaused = true;
    public static byte proximityByte;
    public static int lightReading,distanceReading;
    
    private boolean theMainLoopIsRunning = true, movingForward = true;
    private long currentMilis = System.currentTimeMillis();
    private Thread companionAppServerThread,theLoop;
    private ProximityController proximityController;
    private Sensors sensors;
     private SearchInterface searchInterface;
    
    private int maxPower;
    

    public Project4() {
        initAll();
        videoStreamSwitch(true);
        startCompanionAppServer();
        
        theLoop = new Thread(theMainLoop);
        theLoop.start();
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
        
        proximityController = ProximityController.getInstance(proximityInterface);
        proximityController.startFetching();
        
        sensors = Sensors.getInstance(sensorsInterface);
        sensors.startFetching();
    }

    private void videoStreamSwitch(boolean on) {
        try {
            ProcessBuilder pb = null;
            if(on){
                pb = new ProcessBuilder("sudo", "bash", "/home/pi/p4commands/start_stream.sh");
            }
            else{
                pb = new ProcessBuilder("sudo", "bash", "/home/pi/p4commands/stop_stream.sh");
            }
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
    
    private void fireFound(int fireReading) {
        trex.stop();
        System.out.println("Fire Found !");
        searchInterface.searchPaused();
        searchIsPaused = true;
    }
    
    private SensorsInterface sensorsInterface = new SensorsInterface(){

        @Override
        public void distance(int distance) {
            distanceReading = distance;
        }

        @Override
        public void lightIntensity(int light) {
            lightReading = light;
        }
    };
    
    private ProximityInterface proximityInterface = new ProximityInterface(){

        @Override
        public void onProximityUpdate(byte proximity) {
            proximityByte = proximity;
        }
    };

    private CompanionAppInterface companionAppInterface = new CompanionAppInterface() {

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
            System.out.println("connectionBroken, trex Stop, search paused");
            searchIsPaused = true;
            trex.stop();
            
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
            if(maxPower !=power){
                System.out.println("platformMaxPower:" + power);
            }
            maxPower = power;
        }

        @Override
        public void ignoreProximity(boolean ignore) {  
            if(overrideProximity != ignore){
                overrideProximity = ignore;
                System.out.println("overrideProximity: " + ignore);
            }            
        }

        @Override
        public void holdSearch(boolean searchPaused) { 
            if (searchIsPaused != searchPaused){
                searchIsPaused = searchPaused;
                System.out.println("searchIsPaused: " + searchPaused);
            }
        }

        @Override
        public void teardown() {
            System.out.println("Tear Down Initiated !!!!");
            theMainLoopIsRunning = false;
            videoStreamSwitch(false);
            try {
                companionAppServerThread.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Project4.class.getName()).log(Level.SEVERE, null, ex);
            }            
            try {
                theLoop.join();
            } catch (InterruptedException ex) {
                Logger.getLogger(Project4.class.getName()).log(Level.SEVERE, null, ex);
            }
            // turn off the lights
            gpioController.setPin1(PinState.LOW);
        }
    };

    private void startCompanionAppServer() {
        // Companion App Server thread
        companionAppServerThread = new Thread(companionAppServerRunnable);
        companionAppServerThread.start();
    }
    
    private Runnable companionAppServerRunnable = new Runnable() {

        @Override
        public void run() {
            System.out.println("Thread Server");
            CompanionAppServer companionAppServer = new CompanionAppServer(companionAppInterface, 8000);
            searchInterface = companionAppServer.getSearchInterface();
            companionAppServer.start();
            System.out.println("Thread Executed");
            gpioController.setPin1(PinState.HIGH);
        }
    };

    private Runnable theMainLoop = new Runnable() {

        @Override
        public void run() {
            // debug Stuff
            System.out.println("[operatorInControll = "+operatorInControll+"], [overrideProximity = "+overrideProximity+"], [searchIsPaused = "+searchIsPaused+"]");    
            //init
            int obstacleDetected = 0;
            boolean[] proximityArray = new  boolean[8];
            
            while(theMainLoopIsRunning){
                if(operatorInControll){
                    gpioController.setPin2(PinState.LOW);
                }else{
                    gpioController.setPin2(PinState.HIGH);
                }
                if(searchIsPaused){
                    gpioController.setPin3(PinState.LOW);
                }else{
                    gpioController.setPin3(PinState.HIGH);
                }
                //  The Loop Body
                
                if(!operatorInControll && inSearch && !searchIsPaused){
                    
                    trex.forward(maxPower);

                    
                    if (lightReading > 700) {
                        fireFound(lightReading);
                    }
                    
                    obstacleDetected = proximityController.obstacleDetected(proximityByte);

                    if (obstacleDetected != 0) {
                        
                        proximityArray = proximityController.getProximityArray(proximityByte);
                            
                        if(movingForward &&(proximityArray[1] && proximityArray[2])){
                            trex.rightTurn(180);
                        }

                        if( movingForward && (proximityArray[0] || proximityArray[1])){
                            trex.rightTurn(45);
                        }

                        if( movingForward && (proximityArray[2] || proximityArray[3])){
                            trex.leftTurn(45);
                        }
                    }
                    
                }
                
            }
        }
    };

}


/*
while (inSearch) {
    if (isMoving) {
        trex.forward(movingSpeed);

        fireReading = fire.getLightIntensity();
        if (fireReading > 220) {
            fireFound(fireReading);
        }

        if (proximity.obstacleDetected() != 0) {
            boolean[] proximityArray = proximity.getProximityArray();

            if(movingForward &&(proximityArray[1] && proximityArray[2])){
                trex.rightTurn(180);
            }

            if( movingForward && (proximityArray[0] || proximityArray[1])){
                trex.rightTurn(45);
            }

            if( movingForward && (proximityArray[2] || proximityArray[3])){
                trex.leftTurn(45);
            }
        }

    }
}

*/