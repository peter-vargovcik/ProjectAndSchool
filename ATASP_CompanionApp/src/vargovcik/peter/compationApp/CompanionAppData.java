/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vargovcik.peter.compationApp;

import java.io.Serializable;
import java.util.Arrays;

/**
 *
 * @author Peter Vargovcik
 */
public class CompanionAppData implements Serializable {

    public static int RESPONSE = 2, REQUEST = 1;
    private boolean connectionOpen, connected, liveStreamEnabled, remoteControllEnabled, proximitySensorsEnabled, searchEnabled;
    private byte[] motorsCommand;
    private int[] panTiltCommand;
    private byte proximity;
    private int lightSensitivity, motorPower, messageType,distance;
    private float temperatureReading, humidityReading, atmosphericPressure;
    double altitude;

    public CompanionAppData() {
        super();
        connectionOpen = true;
    }

    public byte[] getMotorsCommand() {
        return motorsCommand;
    }

    public void setMotorsCommand(byte[] motorsCommand) {
        this.motorsCommand = motorsCommand;
    }

    public int[] getPanTiltCommand() {
        return panTiltCommand;
    }

    public void setPanTiltCommand(int[] panTiltCommand) {
        this.panTiltCommand = panTiltCommand;
    }

    public byte getProximity() {
        return proximity;
    }

    public void setProximity(byte proximity) {
        this.proximity = proximity;
    }

    public int getLightSensitivity() {
        return lightSensitivity;
    }

    public void setLightSensitivity(int lightSensitivity) {
        this.lightSensitivity = lightSensitivity;
    }

    public int getMotorPower() {
        return motorPower;
    }

    public void setMotorPower(int motorPower) {
        this.motorPower = motorPower;
    }

    public float getTemperatureReading() {
        return temperatureReading;
    }

    public void setTemperatureReading(float temperatureReading) {
        this.temperatureReading = temperatureReading;
    }

    public float getHumidityReading() {
        return humidityReading;
    }

    public void setHumidityReading(float humidityReading) {
        this.humidityReading = humidityReading;
    }

    public float getAtmosphericPressure() {
        return atmosphericPressure;
    }

    public void setAtmosphericPressure(float atmosphericPressure) {
        this.atmosphericPressure = atmosphericPressure;
    }

    public boolean isConnectionOpen() {
        return connectionOpen;
    }

    public void setConnectionOpen(boolean connectionOpen) {
        this.connectionOpen = connectionOpen;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isLiveStreamEnabled() {
        return liveStreamEnabled;
    }

    public void setLiveStreamEnabled(boolean liveStreamEnabled) {
        this.liveStreamEnabled = liveStreamEnabled;
    }

    public boolean isRemoteControllEnabled() {
        return remoteControllEnabled;
    }

    public void setRemoteControllEnabled(boolean remoteControllEnabled) {
        this.remoteControllEnabled = remoteControllEnabled;
    }

    public boolean isProximitySensorsEnabled() {
        return proximitySensorsEnabled;
    }

    public void setProximitySensorsEnabled(boolean proximitySensorsEnabled) {
        this.proximitySensorsEnabled = proximitySensorsEnabled;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}

	public boolean isSearchEnabled() {
		return searchEnabled;
	}

	public void setSearchEnabled(boolean searchEnabled) {
		this.searchEnabled = searchEnabled;
	}

	@Override
	public String toString() {
		return "CompanionAppData [connectionOpen=" + connectionOpen
				+ ", connected=" + connected + ", liveStreamEnabled="
				+ liveStreamEnabled + ", remoteControllEnabled="
				+ remoteControllEnabled + ", proximitySensorsEnabled="
				+ proximitySensorsEnabled + ", searchEnabled=" + searchEnabled
				+ ", motorsCommand=" + Arrays.toString(motorsCommand)
				+ ", panTiltCommand=" + Arrays.toString(panTiltCommand)
				+ ", proximity=" + proximity + ", lightSensitivity="
				+ lightSensitivity + ", motorPower=" + motorPower
				+ ", messageType=" + messageType + ", distance=" + distance
				+ ", temperatureReading=" + temperatureReading
				+ ", humidityReading=" + humidityReading
				+ ", atmosphericPressure=" + atmosphericPressure
				+ ", altitude=" + altitude + "]";
	}
    
    

}
