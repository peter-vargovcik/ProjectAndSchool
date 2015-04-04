/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vargovcik.peter.compationApp;

import adafruiti2c.sensor.AdafruitBMP180;
import com.pi4j.system.SystemInfo;
import java.io.EOFException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import vargovcik.peter.app.Project4;
import vargovcik.peter.controllers.PeripheralsController;
import vargovcik.peter.controllers.ProximityController;
import vargovcik.peter.controllers.Sensors;
import vargovcik.peter.interfaces.ProximityInterface;
import vargovcik.peter.interfaces.SearchInterface;
import vargovcik.peter.interfaces.SensorsInterface;

/**
 *
 * @author Peter Vargovcik
 */
public class CompanionAppServer {

    private int port;

    private ServerSocket providerSocket;
    private Socket connection = null;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connectionStatus = true;
    private boolean serverIsUp = false;
    private boolean searchIsPaused, overrideProximity;
    private Thread companionAppServerThread;
    private CompanionAppInterface appInterface;
    private CompanionAppData currentState;
//    private Sensors sensors;
//    private int lightReading,distanceReading;
    private int maxPower;
//    private ProximityController proximityController;
//    private byte proximityByte;
    private float baromethricPressure, ambientTemperature, cpuTemperature,cpuVoltage;
    private double altitude;
    private boolean stopSearchOverride = false;
   

    public CompanionAppServer(CompanionAppInterface appInterface, int port) {
        this.appInterface = appInterface;
        this.port = port;
        companionAppServerThread = new Thread(companionAppServerRunnable);
        currentState = new CompanionAppData();
//        sensors = Sensors.getInstance(sensorsInterface);
//        proximityController = ProximityController.getInstance(proximityInterface);
//        proximityController.startFetching();
    }

    public void start() {
//        sensors.startFetching();
        bmp180SensorThread.start();

        try {
            providerSocket = new ServerSocket(port);
            System.out.println("ServerSocet created");
        } catch (IOException ex) {
            Logger.getLogger(CompanionAppServer.class.getName()).log(Level.SEVERE, null, ex);
            appInterface.teardown();
        }
        serverIsUp = true;
        if (!companionAppServerThread.isAlive()) {
            companionAppServerThread.start();
        }
    }

    private Runnable companionAppServerRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                while (serverIsUp) {
                    System.out.println("Waiting for connection");
                    connection = providerSocket.accept();
                    System.out.println("Connection received from " + connection.getInetAddress().getHostName());

                    out = new ObjectOutputStream(connection.getOutputStream());
                    out.flush();
                    in = new ObjectInputStream(connection.getInputStream());
                    CompanionAppData dataRequest = null;
                    CompanionAppData dataResponse = null;
                    do {
                        try {
                            dataRequest = (CompanionAppData) in.readObject();
                            connectionStatus = dataRequest.isConnectionOpen();
                            processRequest(dataRequest);

                            dataResponse = prepareResponse();
                            dataResponse.setMessageType(CompanionAppData.RESPONSE);

                            out.writeObject(dataResponse);
                            out.flush();
                            
                            
//                            try {
//                                    Thread.sleep(15);
//                            } catch (InterruptedException e) {
//                                    // TODO Auto-generated catch block
//                                    e.printStackTrace();
//                            }

                        }catch (InvalidClassException e){
                            System.err.println("InvalidClassException: "+e.getLocalizedMessage());
                            connectionStatus = false;
                        } catch (ClassNotFoundException classnot) {
                            System.err.println("Data received in unknown format");
                        } catch (EOFException eof) {
                            System.err.println("EOFException: " + eof.getMessage());
                            appInterface.connectionBroken();
                            connectionStatus = false;
                        } catch(SocketException e){
                            System.err.println("SocketException: " + e.getMessage());
                            appInterface.connectionBroken();
                            connectionStatus = false;
                        }
                    } while (connectionStatus);
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            } finally {
                // 4: Closing connection
                try {
                    in.close();
                    out.close();
                    providerSocket.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

    };

    private void processRequest(CompanionAppData dataRequest) {
        //reset request timer
        Project4.lastRequestReceivedInMilis = System.currentTimeMillis();
        
        currentState.setRemoteControllEnabled(dataRequest.isRemoteControllEnabled());
        
        appInterface.holdSearch(dataRequest.isSearchPaused());
//        searchIsPaused = dataRequest.isSearchEnabled();
        
        appInterface.platformMaxPower(dataRequest.getMotorPower());
        maxPower = dataRequest.getMotorPower();
        
        appInterface.ignoreProximity(dataRequest.isProximitySensorsEnabled());
//        overrideProximity =dataRequest.isProximitySensorsEnabled(); 
        appInterface.setSearchMode(dataRequest.getSearchMode());

        if (dataRequest.isRemoteControllEnabled()) {
            appInterface.remoteControll(true);
        } else {
            appInterface.remoteControll(false);
        }

        if (dataRequest.getMotorsCommand() != null) {
            appInterface.remoteControlCommand(dataRequest.getMotorsCommand());
        }
        
        if(dataRequest.getPanTiltCommand() !=null){
            byte[] command = new byte[2];
            command[0] = (byte) dataRequest.getPanTiltCommand()[0];
            command[1] = (byte) dataRequest.getPanTiltCommand()[1];
//            appInterface.panTiltContollCommand(command);
        }
          
        appInterface.headLight(dataRequest.isHeadLightOn());
    }

    
    private CompanionAppData prepareResponse() {
        CompanionAppData response = new CompanionAppData();
        response.setRemoteControllEnabled(currentState.isRemoteControllEnabled());
        response.setLightSensitivity(Project4.lightReading);
        response.setDistance(Project4.distanceReading);
        response.setProximity(Project4.proximityByte);
        response.setTemperatureReading(ambientTemperature);
        response.setAtmosphericPressure(baromethricPressure);
        response.setAltitude(altitude);
        response.setMotorPower(maxPower);
        response.setSearchMode(Project4.searchMode);
        response.setHeadLightOn(Project4.headLightIsOn);
        
        if(stopSearchOverride){
            response.setSearchPaused(true);
            stopSearchOverride = false;
        }else{
            response.setSearchPaused(Project4.searchIsPaused);
        }
        response.setProximitySensorsEnabled(Project4.overrideProximity);
        
        return response;
    }
    
    private SearchInterface searchInterface = new SearchInterface(){

        @Override
        public void searchPaused() {
            stopSearchOverride = true;
        }
    };
    
    public SearchInterface getSearchInterface(){
        return this.searchInterface;
    }
    
//    private SensorsInterface sensorsInterface = new SensorsInterface(){
//
//        @Override
//        public void distance(int distance) {
//            distanceReading = distance;
//        }
//
//        @Override
//        public void lightIntensity(int light) {
//            lightReading = light;
//        }
//    };

//    private ProximityInterface proximityInterface = new ProximityInterface(){
//
//        @Override
//        public void onProximityUpdate(byte proximity) {
//            proximityByte = proximity;
//        }
//    };
    
    private Thread bmp180SensorThread = new Thread(new Runnable(){

        @Override
        public void run() {
            final NumberFormat NF = new DecimalFormat("##00.00");
            AdafruitBMP180 sensor = new AdafruitBMP180();
            float press = 57; // Sea kevel in athlone
            float temp  = 0;
            double alt  = 0;
            
            while(serverIsUp){
                try { press = sensor.readPressure(); } 
                catch (Exception ex) 
                { 
                  System.err.println(ex.getMessage()); 
                  ex.printStackTrace();
                }
                sensor.setStandardSeaLevelPressure((int)press);
                try { alt = sensor.readAltitude(); } 
                catch (Exception ex) 
                { 
                  System.err.println(ex.getMessage()); 
                  ex.printStackTrace();
                }
                try { temp = sensor.readTemperature(); } 
                catch (Exception ex) 
                { 
                  System.err.println(ex.getMessage()); 
                  ex.printStackTrace();
                }
//                System.out.println("Temperature: " + NF.format(temp) + " C");
//                System.out.println("Pressure   : " + NF.format(press / 100) + " hPa");
//                System.out.println("Altitude   : " + NF.format(alt) + " m");
                ambientTemperature = temp;
                baromethricPressure  = press / 100;
                altitude  = alt;
                // Bonus : CPU Temperature
                try
                {
//                  System.out.println("CPU Temperature   :  " + SystemInfo.getCpuTemperature());
//                  System.out.println("CPU Core Voltage  :  " + SystemInfo.getCpuVoltage());
                  cpuTemperature    = SystemInfo.getCpuTemperature();
                  cpuVoltage        = SystemInfo.getCpuVoltage();
                }
                catch (InterruptedException ie)
                {
                  ie.printStackTrace();
                }
                catch (IOException e)
                {
                  e.printStackTrace();
                }
            }
        }
    });
}
