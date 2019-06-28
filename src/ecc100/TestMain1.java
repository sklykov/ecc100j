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

    /**
     * Attempt to separate controller and stages initialization
     * @return ECC100Controller class if any devices are found
     */
    public static ECC100Controller initializeControllersAxis(){
        final int maxNumberOfAxis = 3; // maximal number of controlled axis
        ECC100Controller ecc100Controller = new ECC100Controller(); // main handle class for the stage
        ecc100Controller.open(); // it's necessary initialize step - all lists and
                                // internal parameters actually initialized within this method
        List<Integer> deviceIdList = ecc100Controller.getDeviceIdList(); // enumerate all moving stages
        // set default voltages / frequencies
        int VoltageInMilliVol = 30000; int FrequencyInMilliHerz = 800_000;
        if (deviceIdList.size() > 0){
            // for loop going through connected devices
            for(int i=0; i < deviceIdList.size(); i++){
                int j = 0; // count through all axis
                // while loop going through founded axis - stages
                while(j < maxNumberOfAxis){
                    // if axis was initialized during ecc100Controller.open(); as an idea behind - there should be initialized stage to use
                    ECC100Axis axis = ecc100Controller.getAxis(deviceIdList.get(i),j);
                    if( axis!= null){
                        // set physical controlling parameter
                        axis.setVoltage(VoltageInMilliVol); axis.setFrequency(FrequencyInMilliHerz);
                        System.out.println(axis.getActorName()); // Print actor name, but also print if it's disconnected physically
                    } else {
                        break;
                    }
                    j++;
                }

            }
            ecc100Controller.start(); // set all axis to the home position ("0 um") - really works! If I preset them other positions,
                                    // the stages successfully returns to home positions (!)
            return ecc100Controller; // return handle to the entire controller and connected axis (stages, actors)
        } else {
            System.out.println("No Devices found / detected");
            return null;
        }
    }


    public static void main(String[] args) {
        // Test initialization of an entire controller
        ECC100Controller ecc100Controller = initializeControllersAxis();



        // Close the connection to an entire controller after testing
        ecc100Controller.close();
    }
}
