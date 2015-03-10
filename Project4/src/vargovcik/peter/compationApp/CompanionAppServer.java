/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vargovcik.peter.compationApp;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import vargovcik.peter.controllers.PeripheralsController;
import vargovcik.peter.controllers.ProximityController;
import vargovcik.peter.controllers.Sensors;
import vargovcik.peter.interfaces.ProximityInterface;
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
    private Thread companionAppServerThread;
    private CompanionAppInterface appInterface;
    private CompanionAppData currentState;
    private Sensors sensors;
    private int lightReading,distanceReading;
    private ProximityController proximityController;
    private byte proximityByte;

    public CompanionAppServer(CompanionAppInterface appInterface, int port) {
        this.appInterface = appInterface;
        this.port = port;
        companionAppServerThread = new Thread(companionAppServerRunnable);
        currentState = new CompanionAppData();
        sensors = Sensors.getInstance(sensorsInterface);
        proximityController = ProximityController.getInstance(proximityInterface);
        proximityController.startFetching();
    }

    public void start() {
        sensors.startFetching();

        try {
            providerSocket = new ServerSocket(port);
            System.out.println("ServerSocet created");
        } catch (IOException ex) {
            Logger.getLogger(CompanionAppServer.class.getName()).log(Level.SEVERE, null, ex);
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

                        } catch (ClassNotFoundException classnot) {
                            System.err.println("Data received in unknown format");
                        } catch (EOFException eof) {
                            System.err.println("EOFException: " + eof.getMessage());
                            connectionStatus = false;
                        } catch(SocketException e){
                            System.err.println("SocketException: " + e.getMessage());
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
        
        currentState.setRemoteControllEnabled(dataRequest.isRemoteControllEnabled());

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
            appInterface.panTiltContollCommand(command);
        }
            
    }

    private CompanionAppData prepareResponse() {
        CompanionAppData response = new CompanionAppData();
        response.setRemoteControllEnabled(currentState.isRemoteControllEnabled());
        response.setLightSensitivity(lightReading);
        response.setDistance(distanceReading);
        response.setProximity(proximityByte);
        return response;
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
}