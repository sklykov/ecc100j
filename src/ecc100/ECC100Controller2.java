package ecc100;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Extending of previously defined class ECC100Controller with some additional features
 * for simplifying its controlling
 */

public class ECC100Controller2 extends ECC100Controller {
    private String name;
    private ArrayList<ECC100Axis> allAxis;
    private final int maxNumberOfAxis = 3; // maximum number of axis

    /**
     * Just to better distinguish 2 or more presented controllers...
     * Actually, the vendor software doesn't support two or more plugged in controllers simultaneously
     * @param name - String with the name (* e.g., XYZ or Rotator)
     */
    public ECC100Controller2(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Returning ArrayList of initialized axes (physical actuators) associated with this controller
     * @return ArrayList with all references to control actuators (axes)
     */
    public ArrayList<ECC100Axis> getAllAxis() { return allAxis; }

    /**
     * Just the rewritten method to set voltages to the initialized
     * @param axisNumber = 0,1,2 (3 axes maximum)
     * @param voltageInVolts
     * @return
     */
    public boolean setVoltage(int axisNumber,int voltageInVolts){
        // additional checking that it's proper axis number
        if (axisNumber < allAxis.size()){
            try {
                int voltageToSet = 1000*voltageInVolts;
                allAxis.get(axisNumber).setVoltage(voltageToSet);
                return true;
            } catch (Exception e) {
                // I am not sure, but if voltage is out of range, maybe the set function will return some exception...
                return false;
            }
        } else return false;
    }

    /**
     * Initialize all connected (physically) actuators to the controller
     * @return true if at least 1 axis (actuator) has been initialized and its reference value is valid
     */
    public boolean initializeActuatorsAxes(){
        this.open(); // all lists and internal parameters (ECC100Controller) actually initialized within this method
        List<Integer> deviceIdList = this.getDeviceIdList(); // enumerate all presented ports - actually, shows number of connected ports!

        // set default voltages / frequencies = 30 V, 3 kHz
        int VoltageInMilliVol = 30000; int FrequencyInMilliHerz = 2_000_000;

        if (deviceIdList.size() > 0) {

            /*
            for (Integer deviceId : deviceIdList) {
                System.out.println("ID " + deviceId);
            }
             */

            // for loop going through connected devices - axis or actuators
            allAxis = new ArrayList<>();
            int nInitializedAxis = 0; int nNotInitAxis = 0;
            for (int i = 0; i < deviceIdList.size(); i++) {
                int j = 0; // count through all axis
                int countOfAttempt = 0; // counting the axes calling attempts, maximum 3 axis could be controlled by the ECC100
                // while loop going through founded axis - actuators or physical linear / rotating devices
                while (countOfAttempt < maxNumberOfAxis) {
                    // if axis was initialized during ecc100Controller.open(); as an idea behind - there should be initialized stage to use
                    // as I understand, only reads preset actor type and returning only the name and properties of that despite of a presence
                    // of a physical connection
                    ECC100Axis axis = this.getAxis(deviceIdList.get(i), j);

                    // set default physical controlling parameter
                    axis.setVoltage(VoltageInMilliVol); axis.setFrequency(FrequencyInMilliHerz);

                    // below - a little of testing info
                    // System.out.println("Controller " + i + " axis " + j + ": " + axis);
                    // System.out.println("Reference: " + axis.getReferencePosition());
                    // System.out.println("Ref valid: " + axis.isReferencePositionValid());

                    // Below is attempt to detect the reference position by moving stage forward and backward...
                    // It's FAILED... It's more simple to find reference position MANUALLY
                    // If reference position in the close proximity to the previous one, maybe the function below will help
                    if(!axis.isReferencePositionValid()){
                        double curPos = axis.getCurrentPosition();
                        // try to go a bit further from a current position for finding a reference position
                        axis.goToPosition(curPos + 250,1);
                        // if the previous attempt failed, then try to make the same backwards
                        if(!axis.isReferencePositionValid()){
                            curPos = axis.getCurrentPosition();
                            axis.goToPosition(curPos - 500,1);
                        }
                    }

                    if ((axis != null)&&(axis.isReferencePositionValid())) {
                        allAxis.add(j, axis);
                        j++;
                    }
                    if ((axis != null)&&(!axis.isReferencePositionValid())){
                        nNotInitAxis++;
                        System.err.println("The reference position is invalid! Better use the vendor GUI to find it before");
                    }
                    countOfAttempt++;
                }
                System.out.println("Number of initialized axes: " + j + " and number of all connected axis: " + (j+nNotInitAxis));
                nInitializedAxis = j;
            }

            if ((allAxis.size() > 0)&&(nInitializedAxis>0)&&(nNotInitAxis == 0)){
                this.start(); // set all axis to the home position ("0 um"). The stages successfully returns to home positions
                return true;
            }
        }
        return false;
    }

    /**
     * Closing connection to controller using parent method
     */
    public void closeController(){
        this.close();
        allAxis = null;
    }
}
