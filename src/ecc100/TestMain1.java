package ecc100; /**
 * Main class for testing available functions for entire stage controlling
 */

import ecc100.ECC100Axis;
import ecc100.ECC100Controller;

import java.util.ArrayList;
import java.util.List;

public class TestMain1 {

    /**
     * Compose of all required initialization steps for handling all available axis / movements
     * @return flag - true if successfully initialized all stages, false - otherwise
     */
    // put here the code from main method when all initialization steps would been completed
    public static boolean InitializeStages(){

        return true;
    }

    public static void main(String[] args) {

        ECC100Controller ecc100Controller = new ECC100Controller(); // main handle class for the stage
        // System.out.println("The connection to the stage is opened? " + ecc100Controller.open()); // true if stage is physically disconnected...
                                                                                                    // but it always true from the code
        List<Integer> deviceIdList = ecc100Controller.getDeviceIdList(); // enumerate all moving stages
        System.out.println("DeviceIdList = " + deviceIdList); // show all detected / connected devices
        // ok, below is actual checking if any single stage connected to the PC
        if (deviceIdList.size() > 0){
            ArrayList<ECC100Axis> allAxis = new ArrayList<ECC100Axis>(deviceIdList.size()); // initialize ArrayList containing references to all
                                                                                            // available directions of stage movement (?)
            // initialize instances of axis in a below for loop
            for(int i=0; i < deviceIdList.size(); i++){
                int id = deviceIdList.get(i); // get id
                allAxis.set(i,ecc100Controller.getAxis(id,i)); // append actual references to access axis
            }
        }



        else
            System.out.println("No devices have been connected / detected so far");
    }
}
