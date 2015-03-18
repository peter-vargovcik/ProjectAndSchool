/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vargovcik.peter.compationApp;

/**
 *
 * @author Peter Vargovcik
 */
public interface CompanionAppInterface {
    void stopSearch();
    void startSearch();
    void remoteControll(boolean isRemoteControlled);
    void remoteControlCommand(byte[] command);
    void connectionBroken();
    void platformMaxPower(int power);
    void ignoreProximity(boolean ignore);

    public void panTiltContollCommand(byte[] command);
    
}
