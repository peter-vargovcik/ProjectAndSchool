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
import vargovcik.peter.interfaces.CompanionAppInterface;
import vargovcik.peter.compationApp.CompanionAppServer;
import vargovcik.peter.compationApp.SearchMode;
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
    
    public static boolean 
            inSearch = true, 
            operatorInControll = false, 
            overrideProximity = false, 
            searchIsPaused = true,
            theMainLoopIsRunning = true, 
            movingForward = true, 
            wallNotPresent = false,
            headLightIsOn = false,
            rgbRedLedIsOn = false,
            rgbGreenLedIsOn = false,
            rgbBlueLedIsOn = false;
    
    public static byte proximityByte ,rigthHandProximityByte;
    public static int lightReading,distanceReading;
    public static SearchMode searchMode;
    
    private boolean
            remoteControlledMovingForward =true,
            remoteControlledTurningLeft =true;
    private byte[] remoteControlledommand = new byte[2];
    
    private static long NO_RESPONCE_TRESHOLD = 1000;
    private long currentMilis = System.currentTimeMillis();
    public static long lastRequestReceivedInMilis = System.currentTimeMillis();
    
    private Thread companionAppServerThread,theLoop;
    private ProximityController proximityController;
    private Sensors sensors;
    private SearchInterface searchInterface;
    
    private int maxPower, tempCount =0;
    

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
    }

    private void initAll() {
        searchMode = SearchMode.PING_PONG;        
        gpioController = GPIOController.instance;
        gpioController.setSerialRelay(PinState.LOW);
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Project4.class.getName()).log(Level.SEVERE, null, ex);
        }       
        trex = Trex.instance;
                
        peripheralsController = PeripheralsController.getInstance(proximityInterface);
        peripheralsController.startFetching();
        
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
        searchIsPaused = true;
        trex.stop();
        gpioController.fireFound();
        System.out.println("Fire Found !");
        searchInterface.searchPaused();
        
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

        @Override
        public void onRightHandProximityUpdate(byte proximity) {
            rigthHandProximityByte = proximity;
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
                gpioController.stopFireFoundEvent();
                System.out.println("remoteControll:" + isRemoteControlled +
                        " current state: " + operatorInControll);
            }
            operatorInControll = isRemoteControlled;

        }

        @Override
        public void remoteControlCommand(byte[] command) {
            if (command != null && operatorInControll) {
                tempCount++;
                int motorLeft   = command[0] & 0xFF;
                int motorRight  = command[1] & 0xFF;
                
                if(motorLeft> 64 && motorRight> 192){
                    remoteControlledMovingForward = true;
                }
                else if(motorLeft< 64 && motorRight< 192){
                    remoteControlledMovingForward = false;
                }
                
                remoteControlledommand = command;
            }
        }

        @Override
        public void connectionBroken() {
            System.out.println("connectionBroken, trex Stop, search paused");
            searchIsPaused = true;
            gpioController.stopFireFoundEvent();
            trex.stop();
            gpioController.setSerialRelay(PinState.HIGH);
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Project4.class.getName()).log(Level.SEVERE, null, ex);
        }
            
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
                gpioController.stopFireFoundEvent();
                searchIsPaused = searchPaused;
                System.out.println("searchIsPaused: " + searchPaused);
                if(searchIsPaused){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Project4.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    trex.stop();
                }
            }
        }

        @Override
        public void teardown() {
            gpioController.stopFireFoundEvent();
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
            gpioController.setGreenLed(PinState.LOW);            
            gpioController.setSerialRelay(PinState.HIGH);
        }

        @Override
        public void setSearchMode(SearchMode searchModeCommand) {
            if(!searchModeCommand.equals(searchMode)){
                System.out.println("Search mode: "+searchModeCommand.toString());
            }
            searchMode = searchModeCommand;
        }

        @Override
        public void headLight(boolean on) {
            if(headLightIsOn != on){
                gpioController.setHeadlight((on)?PinState.LOW:PinState.HIGH);
                headLightIsOn = on;
                System.out.println("Headlight on: "+headLightIsOn);
            }
            
        }

        @Override
        public void rgbSet(boolean redIsOn, boolean greenIsOn, boolean blueIsOn) {
            //rgbRedLedIsOn, rgbGreenLedIsOn, rgbBlueLedIsOn 
            if(rgbRedLedIsOn != redIsOn){
                gpioController.setRGBRed((redIsOn)?PinState.LOW:PinState.HIGH);
                rgbRedLedIsOn = redIsOn;
            }
            
            if(rgbGreenLedIsOn != greenIsOn){
                gpioController.setRGBGreen((greenIsOn)?PinState.LOW:PinState.HIGH);
                rgbGreenLedIsOn = greenIsOn;
            }
            
            if(rgbBlueLedIsOn != blueIsOn){
                gpioController.setRGBBlue((blueIsOn)?PinState.LOW:PinState.HIGH);
                rgbBlueLedIsOn = blueIsOn;
            }            
        }

        @Override
        public void connected() {
             gpioController.setSerialRelay(PinState.LOW);
        
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(Project4.class.getName()).log(Level.SEVERE, null, ex);
        }
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
            gpioController.setGreenLed(PinState.HIGH);
        }
    };

    private Runnable theMainLoop = new Runnable() {

        @Override
        public void run() {
            // debug Stuff
            System.out.println("[operatorInControll = "+operatorInControll+
                    "], [overrideProximity = "+overrideProximity+
                    "], [searchIsPaused = "+searchIsPaused+"]");    
            //init
            int obstacleDetected = 0;
            boolean[] proximityArray = new  boolean[8];
            
            while(theMainLoopIsRunning){
                currentMilis = System.currentTimeMillis();
                
                if(operatorInControll){
                    gpioController.setRGBGreen(PinState.LOW);
                }else{
                    gpioController.setRGBGreen(PinState.HIGH);
                }
                /*
                if(searchIsPaused){
                    gpioController.setRedLed(PinState.HIGH);
                }else{
                    gpioController.setRedLed(PinState.LOW);
                }
                */
                
                if(!PeripheralsController.rightSideObstacleDetected(rigthHandProximityByte)){
                    gpioController.setRedLed(PinState.HIGH);
                    gpioController.setGreenLed(PinState.LOW);
                }
                else{
                    gpioController.setRedLed(PinState.LOW);
                    gpioController.setGreenLed(PinState.HIGH);
                }
                
                //  The Loop Body
                if(!operatorInControll && inSearch && !searchIsPaused && (searchMode !=null)){
                    gpioController.startRGBEmergency();
                     //PingPong Search Mode
                    if(searchMode.equals(SearchMode.PING_PONG)){
                    
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
                    //FireMan Search Mode
                    if(searchMode.equals(SearchMode.FIREMAN)){                        
                        // go forward 
                        trex.forward(maxPower);
                        // check the light level
                        if (lightReading > 700) {
                            fireFound(lightReading);
                        }
                        //check obstacles in front
                        obstacleDetected = proximityController.obstacleDetected(proximityByte);

                        if (obstacleDetected != 0) {
                            // obstacle detected - deal with it
                            proximityArray = proximityController.getProximityArray(proximityByte);

                            if(movingForward &&(proximityArray[1] && proximityArray[2])){
                                trex.leftTurn(90);
                            }

                            if( movingForward && (proximityArray[0] || proximityArray[1])){
                                trex.rightTurn(45);
                            }

                            if( movingForward && (proximityArray[2] || proximityArray[3])){
                                trex.leftTurn(45);
                            }
                        }
                        
                        //check right side if wall is present
                        if(!PeripheralsController.rightSideObstacleDetected(rigthHandProximityByte) && !wallNotPresent){
                            // flag that we are searching for wall.
                            wallNotPresent = true;
                            //if wall is not present and front of the platform is clear make 90 degree turn to right
                            trex.rightTurn(90);
                            
                        }else if(PeripheralsController.rightSideObstacleDetected(rigthHandProximityByte)){
                            wallNotPresent = false;
                        }                        
                                                                    
                        
                        // while flag is up:
                            // drive forward till front proximity detects wall
                            // when wall found do 90 degree turn to left
                        
                            //check right side if wall is present
                        
                        //remove flag
                    }
                    
                }
                else if(operatorInControll){
                    gpioController.stopRGBEmergency();
                    // operator is in controll
                    
                    if((lastRequestReceivedInMilis - currentMilis) > NO_RESPONCE_TRESHOLD ){
                        trex.stop();
                        System.out.println("No Reqest, Trex Stop");
                    }
                    
                    if(!overrideProximity){
                        obstacleDetected = proximityController.obstacleDetected(proximityByte);

                        if (obstacleDetected != 0) {

                            proximityArray = proximityController.getProximityArray(proximityByte);

                            if(remoteControlledMovingForward &&(proximityArray[1] && proximityArray[2])){
                                trex.stop();
                            }

                            if( remoteControlledMovingForward && (proximityArray[0] || proximityArray[1])){
                                trex.stop();
                            }

                            if( remoteControlledMovingForward && (proximityArray[2] || proximityArray[3])){
                                trex.stop();
                            }
                            
                             if(!remoteControlledMovingForward &&(proximityArray[5] && proximityArray[6])){
                                trex.stop();
                            }

                            if( !remoteControlledMovingForward && (proximityArray[4] || proximityArray[5])){
                                trex.stop();
                            }

                            if( !remoteControlledMovingForward && (proximityArray[6] || proximityArray[7])){
                                trex.stop();
                            }
                        }
                        else{
                            trex.trexExecute(remoteControlledommand);
                        }
                    }
                    else{
                        if(remoteControlledommand != null){
                            trex.trexExecute(remoteControlledommand);
                            //System.out.println("["+remoteControlledommand[0]+","+remoteControlledommand[1]+"]");
                        }
                    }
                    
                }
                gpioController.stopRGBEmergency();
            }
        }
    };

}
