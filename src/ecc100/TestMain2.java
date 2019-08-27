package ecc100;

public class TestMain2 {
    public static void main(String[] args) {
        ECC100Controller2 stage = new ECC100Controller2("XYZ");

        // testing of controller initialization
        boolean isInitialized = stage.initializeActuatorsAxes();
        System.out.println("Has been the controller initialized? " + isInitialized);


        // testing of ability to move actuator along single axis
        if (isInitialized){
            ECC100Axis presentAxis = stage.getAllAxis().get(1); // get the handle to the axis
            double curPos = presentAxis.getCurrentPosition();
            System.out.println(curPos);
            presentAxis.continuous(true, true);
            // presentAxis.setTargetPosition(curPos + 50);
            System.out.println(presentAxis.getCurrentPosition());
        }


        // closing open controller handling
        stage.close();
    }
}
