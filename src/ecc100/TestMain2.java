package ecc100;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class TestMain2 {
    final static short maxAcceptableMismatch = 1; // mismatch between the actual stage position and the target one

    public static void main(String[] args) {
        ECC100Controller2 stage = new ECC100Controller2("XYZ");

        // testing of controller initialization
        boolean isInitialized = stage.initializeActuatorsAxes();
        System.out.println("Has been the controller initialized? " + isInitialized);


        // testing of ability to move actuator along single axis

        if (isInitialized){
            ArrayList<ECC100Axis> allAxes = stage.getAllAxis();
            for (ECC100Axis ax:allAxes){
                System.out.println(ax.getCurrentPosition() + " is the current position of axis");
            }
            ECC100Axis presentAxis = stage.getAllAxis().get(0); // get the handle to the axis
            double curPos = presentAxis.getCurrentPosition();
            presentAxis.goToPosition(curPos + 100,maxAcceptableMismatch);
            System.out.println(presentAxis.getCurrentPosition() + " is the position after using the 'goToPosition' method");
            curPos = presentAxis.getCurrentPosition();
            presentAxis.goToPositionAndWait(curPos - 1000,maxAcceptableMismatch,100, TimeUnit.MILLISECONDS);
            System.out.println(presentAxis.getCurrentPosition() + " is the position after using the 'goToPositionAndWait' method");
            // TODO: So, now 'goToPosition' method works completely wrong and 'goToPositionAndWait' method working results depend on the Timeout specified
        }



        // closing open controller handling
        stage.close();
    }
}
