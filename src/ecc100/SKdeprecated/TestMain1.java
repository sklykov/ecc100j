package ecc100.SKdeprecated;
/**
 * Main class for testing available functions for entire stage controlling
 */

import ecc100.ECC100Axis;
import ecc100.ECC100Controller;
import ecc100.ECC100Controller2;

import java.util.ArrayList;
import java.util.List;
@Deprecated
public class TestMain1 {
    /**
     * Attempt to separate controller and stages initialization
     * @return ECC100Controller class if any devices are found with associated actuators
     */
    public static ECC100Controller initializeControllersAxis(){
        final int maxNumberOfAxis = 3; // maximal number of controlled axis / actuators
        ECC100Controller ecc100Controller = new ECC100Controller(); // main handle class for the stage
        ecc100Controller.open(); // it's necessary initialize step - all lists and
                                // internal parameters actually initialized within this method
        List<Integer> deviceIdList = ecc100Controller.getDeviceIdList(); // enumerate all presented ports, actually it should be always 3
        // set default voltages / frequencies = 30 V, 1 kHz
        int VoltageInMilliVol = 30000; int FrequencyInMilliHerz = 1_000_000;
        if (deviceIdList.size() > 0){
            // for loop going through connected devices
            for(int i=0; i < deviceIdList.size(); i++){
                int j = 0; // count through all axis
                // while loop going through founded axis - stages
                while(j < maxNumberOfAxis){
                    // if axis was initialized during ecc100Controller.open(); as an idea behind - there should be initialized stage to use
                    // as I understand, only reads preset actor type and returning only the name and properties of that despite of presence
                    // of physical connection
                    ECC100Axis axis = ecc100Controller.getAxis(deviceIdList.get(i),j);
                    // so, the actual physical connection is checked by invoking isReferencePositionValid()
                    if((axis!= null)&&(axis.isReferencePositionValid())){
                        // set physical controlling parameter
                        axis.setVoltage(VoltageInMilliVol); axis.setFrequency(FrequencyInMilliHerz);
                        // open question - is it necessary to get and to save somewhere reference positions?
                        System.out.println(axis.getActorName()); // Print actor name, but also print if it's disconnected physically
                    } else { break; }
                    j++;
                }

            }
            ecc100Controller.start(); // set all axis to the home position ("0 um") - really works! If I preset them other positions,
                                    // the stages successfully returns to home positions
            return ecc100Controller; // return handle to the entire controller and connected axis (stages, actors)
        } else {
            System.out.println("No Devices found / detected");
            return null;
        }
    }


    public static void main(String[] args) {
        // Test initialization of an entire controller
        ECC100Controller ecc100Controller1 = initializeControllersAxis();
        ECC100Controller2 ec2 = new ECC100Controller2("XYZ");




        // Close the connection to an entire controller after testing
        ecc100Controller1.close();
    }
}
