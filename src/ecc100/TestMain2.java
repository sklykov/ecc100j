package ecc100;

public class TestMain2 {
    public static void main(String[] args) {
        ECC100Controller2 rotator = new ECC100Controller2("XYZ");

        // testing of controller initialization
        boolean isInitialized = rotator.initializeActuatorsAxes();
        System.out.println("Has been the controller initialized? " + isInitialized);


        // testing of ability to move actuator along single axis
        if (isInitialized){
            ECC100Axis presentAxis = rotator.getAllAxis().get(0); // get the handle to the axis
            double curPos = presentAxis.getCurrentPosition();
            presentAxis.continuous(true,true); // just making some long step forward (true) or backward (false)
        }


        // closing open controller handling
        rotator.close();
    }
}
