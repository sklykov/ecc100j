package ecc100;
/**
 * This class implements actual controlling of stage movement along axis.
 * <p>
 *     Refer to this class for sending command to move
 * </p>
 */

import static java.lang.Math.abs;
import java.util.concurrent.TimeUnit;
import ecc100.bindings.EccLibrary;
import org.bridj.Pointer;

public class ECC100Axis {
  private static final double cGoToEpsilon = 1; // maximum acceptable distance between target and current distance
  private Pointer<Integer> mPointerToDeviceHandle = Pointer.allocateInt(); // Pointer - to the memory (C code), Pointer - wrapper class around C code
  private int mAxisIndex; // for enumeration of axes (actually stages, which all moving along single axis or direction)
  private boolean mLocked = false; // flag to show if the stage is performing some task and locked till completion
  private volatile double mLastTargetPositionInMicrons; // for accessing the last assigned position.
  private volatile double pLastEpsilonInMicrons; // ?

  /**
   * Constructor accepting ECC100Controller class, index of a device, some index of axis (?)
   * @param pECC100Controller - sample of class ECC100Controller
   * @param pDeviceIndex
   * @param pAxisIndex
   */
  public ECC100Axis(ECC100Controller pECC100Controller, int pDeviceIndex, int pAxisIndex) {
    super(); // just a redundant piece of code or not?
    mPointerToDeviceHandle.set(pECC100Controller.getControllerDeviceHandle(pDeviceIndex));
    mAxisIndex = pAxisIndex;
    // stopOnEOT(true); // ? Just looking through the code and found this commented out calling function... Why did it turn commented out?
    getReferencePosition();
  }

  private int isMovingState(Pointer<Integer> pPointerToDeviceHandle, int i) {
    Pointer<Integer> lIsMovingState = Pointer.allocateInt();
    EccLibrary.ECC_getStatusMoving(pPointerToDeviceHandle.getInt(), i, lIsMovingState);
    int lInt = lIsMovingState.getInt();
    lIsMovingState.release();
    return lInt;
  }

  /**
   * Examing if stage moving or not...
   * @return true if "Moving State is more than 0"
   */
  public boolean isMoving() {
    int lMovingState = isMovingState(mPointerToDeviceHandle, mAxisIndex);
    return lMovingState > 0;
  }

  public boolean isPending() {
    int lMovingState = isMovingState(mPointerToDeviceHandle, mAxisIndex);
    return lMovingState == 2;
  }

  public void singleStep(boolean lForward) {
    EccLibrary.ECC_setSingleStep(mPointerToDeviceHandle.getInt(), mAxisIndex, lForward ? 0 : 1);
  }

  public void reset() {
    EccLibrary.ECC_setReset(mPointerToDeviceHandle.getInt(), mAxisIndex);
    printLastError();
  }

  /**
   * Force stage to go to "0 um" position
   */
  public void home() {
    getReferencePosition(); // The set reference position isn't used further explicitly.
                            // But maybe implicitly it influences somehow to the next method
    goToPosition(0, cGoToEpsilon);
  }

  /**
   * Activation of controlling of approaching to the assigned position
   */
  public void enable() {
    controlAproachToTargetPosition(true);
    controlOutputRelais(true);
  }

  public boolean isReady() {
    return !isLocked() && !isPending();
  }

  public void stop() {
    controlAproachToTargetPosition(false);
  }

  /**
   * ? - functionality, could be some issues with synchronization, because this method doesn't implement the timeout functionality
   * @param pTargetPositionInMicrons - readable name parameter
   * @param pEpsilonInMicrons - maximum allowed difference between the target position and the actual one
   */
  public void goToPosition(double pTargetPositionInMicrons, double pEpsilonInMicrons) {
    enable(); // controlling the movement
    setTargetPosition(pTargetPositionInMicrons); // ok, moving the stage...
    double diff = Math.abs(pTargetPositionInMicrons - getCurrentPosition());
    boolean conditionReached = (diff > pEpsilonInMicrons); // Actual condition of stopping movement
    // System.out.println(diff); System.out.println(condition);
    // Forcing of the controlled actuator to reach the assigned position - ?
    if (conditionReached){
      while (conditionReached){
        setTargetPosition(pTargetPositionInMicrons);
        diff = Math.abs(pTargetPositionInMicrons - getCurrentPosition());
        conditionReached = (diff > pEpsilonInMicrons);
      }
    }
  }

  /**
   * Simplified version of other polymorphic function
   * @param pTargetPositionInMicrons - only needed, in this case timeout will be equal to 1 minute
   */
  public void goToPositionAndWait(double pTargetPositionInMicrons) {
    goToPositionAndWait(pTargetPositionInMicrons, cGoToEpsilon, 1, TimeUnit.MINUTES);
  }

  /**
   * In theory, moving stage and waiting on reached position method
   * @param pTargetPositionInMicrons - readable name
   * @param pEpsilonInMicrons - maximal allowed absolute difference between actual and desired positions
   * @param pTimeOut - actually, waiting position
   * @param pTimeUnit - minutes, seconds, ms, ns
   * @return
   */
  public boolean goToPositionAndWait(double pTargetPositionInMicrons, double pEpsilonInMicrons, long pTimeOut, TimeUnit pTimeUnit) {
    enable(); // Controlling Approach to the position
    setTargetPosition(pTargetPositionInMicrons); // referring to native method calling inside ...
    mLastTargetPositionInMicrons = pTargetPositionInMicrons; // saving actual target position as "last one used"
    pLastEpsilonInMicrons = pEpsilonInMicrons; // the same as above - for epsilon value
    return waitToArriveAt(pTargetPositionInMicrons, pEpsilonInMicrons, pTimeOut, pTimeUnit);
  }

  public boolean hasArrived() {
    return abs(getCurrentPosition() - mLastTargetPositionInMicrons) < pLastEpsilonInMicrons;
  }

  private boolean waitToArriveAt(double pTargetPositionInMicrons, double pEpsilonInMicrons, long pTimeOut, TimeUnit pTimeUnit) {
    if (isLocked())
      return false;

    long lDeadLine = System.nanoTime() + TimeUnit.NANOSECONDS.convert(pTimeOut, pTimeUnit);
    //System.out.println("Ready: " + isReady());
    //System.out.println("Arrived: " + hasArrived());
    //System.out.println("Moving: " + isMoving());

    // below is the main change - the condition for while case (until reaching the position) + establishing the pause for the stage (Thread.sleep)
    while (isMoving() && (abs(pTargetPositionInMicrons - getCurrentPosition()) > pEpsilonInMicrons)) {
      try {
        //System.out.println("isMoving=" + isMoving());
        //System.out.println("getCurrentPosition=" + getCurrentPosition());
        Thread.sleep(100);
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (System.nanoTime() > lDeadLine)
        return false;
    }
    return true;
  }

  public void setTargetPosition(double lTargetPositionInMicrons) {
    Pointer<Integer> lPointerToTarget = Pointer.allocateInt(); // create sample class to transfer data to an actuator
    lPointerToTarget.set((int) (lTargetPositionInMicrons * 1000 + getReferencePosition() * 1000)); // actual method (set - in "Pointer.class")
    int result = EccLibrary.ECC_controlTargetPosition(mPointerToDeviceHandle.getInt(), mAxisIndex,
            lPointerToTarget,  1); // as an idea: it returns some integer as the result of performing of this method...
    // System.out.println("result of moving is: " + result); // SEEMS THAT 0 - is the result then axis just moving without errors
    lPointerToTarget.release();
    printLastError();
  }

  public void continuous(boolean pEnable, boolean pForward) {
    Pointer<Integer> lPointerEnable = Pointer.allocateInt();
    lPointerEnable.set(pEnable ? 1 : 0);
    if (pForward)
      EccLibrary.ECC_controlContinousFwd(mPointerToDeviceHandle.getInt(), mAxisIndex, lPointerEnable, 1);
    else
      EccLibrary.ECC_controlContinousBkwd(mPointerToDeviceHandle.getInt(), mAxisIndex, lPointerEnable, 1);

    lPointerEnable.release();
    printLastError();
  }

  public void setFrequency(int lVoltagInMillihertz) {
    Pointer<Integer> lPointerToFrequency = Pointer.allocateInt();
    lPointerToFrequency.set(lVoltagInMillihertz);
    EccLibrary.ECC_controlFrequency(mPointerToDeviceHandle.getInt(), mAxisIndex, lPointerToFrequency, 1);
    lPointerToFrequency.release();
    printLastError();
  }

  public void setVoltage(int lVoltagInMilliVolts){
    Pointer<Integer> lPointerToAmplitude = Pointer.allocateInt();
    lPointerToAmplitude.set(lVoltagInMilliVolts);
    EccLibrary.ECC_controlAmplitude(mPointerToDeviceHandle.getInt(), mAxisIndex, lPointerToAmplitude, 1);
    lPointerToAmplitude.release();
    printLastError();
  }

  public void stopOnEOT(boolean pStop) {
    Pointer<Integer> lPointerToStopOnEOT = Pointer.allocateInt();
    lPointerToStopOnEOT.set(pStop ? 1 : 0);
    EccLibrary.ECC_controlEotOutputDeactive(mPointerToDeviceHandle.getInt(), mAxisIndex, lPointerToStopOnEOT, 1);
    lPointerToStopOnEOT.release();
    printLastError();
  }

  public void controlAproachToTargetPosition(boolean lEnable) {
    Pointer<Integer> lPointerEnable = Pointer.allocateInt();
    lPointerEnable.set(lEnable ? 1 : 0);
    EccLibrary.ECC_controlMove(mPointerToDeviceHandle.getInt(), mAxisIndex, lPointerEnable, 1);
    lPointerEnable.release();
    printLastError();
  }

  public void controlOutputRelais(boolean lEnable) {
    Pointer<Integer> lPointerEnable = Pointer.allocateInt();
    lPointerEnable.set(lEnable ? 1 : 0);
    EccLibrary.ECC_controlOutput(mPointerToDeviceHandle.getInt(), mAxisIndex, lPointerEnable, 1);
    lPointerEnable.release();
    printLastError();
  }

  public String getActorName() {
    Pointer<Byte> lActorName = Pointer.allocateBytes(128);
    EccLibrary.ECC_getActorName(mPointerToDeviceHandle.getInt(), mAxisIndex, lActorName);
    printLastError();
    String lString = new String(lActorName.getBytes());
    lActorName.release();
    return lString;
  }

  @SuppressWarnings("unchecked")
  public int getActorType() {
    Pointer<Integer> lActorType = Pointer.allocateInt();
    EccLibrary.ECC_getActorType(mPointerToDeviceHandle.getInt(), mAxisIndex, (Pointer) lActorType);
    printLastError();
    int lActorTypeInt = lActorType.getInt();
    lActorType.release();
    return lActorTypeInt;
  }

  public double getCurrentPosition() {
    Pointer<Integer> lCurrentPosition = Pointer.allocateInt();
    EccLibrary.ECC_getPosition(mPointerToDeviceHandle.getInt(), mAxisIndex, lCurrentPosition);
    double lCurrentPositionInt = lCurrentPosition.getInt() * 0.001 - getReferencePosition();
    lCurrentPosition.release();
    return lCurrentPositionInt;
  }

  /**
   * Actually, this method better shown and explained in original GUI program.
   * It returns some measured reference position. This reference position can be
   * set by user from starting.*
   * @return double referencePosition in microns
   */
  public double getReferencePosition() {
    if (!isReferencePositionValid())
      return Integer.MAX_VALUE; // to show that reference position isn't valid
    Pointer<Integer> lReferencePosition = Pointer.allocateInt();
    EccLibrary.ECC_getReferencePosition(mPointerToDeviceHandle.getInt(), mAxisIndex, lReferencePosition);
    int lReferencePositionInt = lReferencePosition.getInt();
    double lReferencePositionInMicrons = lReferencePositionInt * 0.001;
    lReferencePosition.release();
    return lReferencePositionInMicrons;
  }

  /**
   * From GUI program - If this reference position is initialized correctly, it's true
   * @return boolean
   */
  public boolean isReferencePositionValid() {
    Pointer<Integer> lReferencePositionIsValid = Pointer.allocateInt();
    EccLibrary.ECC_getStatusReference(mPointerToDeviceHandle.getInt(), mAxisIndex, lReferencePositionIsValid);
    int lReferencePositionIsValidInt = lReferencePositionIsValid.getInt();
    lReferencePositionIsValid.release();
    return lReferencePositionIsValidInt > 0;
  }

  public void printLastError() {
    Pointer<Integer> lLastError = Pointer.allocateInt();
    EccLibrary.ECC_getStatusError(mPointerToDeviceHandle.getInt(), mAxisIndex, lLastError);
    int lLastErrorInt = lLastError.getInt();
    if (lLastErrorInt != 0) {
      System.out.println("ECC_getStatusError ->" + lLastErrorInt);
    }
  }

  @Override
  public String toString() {
    return "ECC100Axis [mDeviceIndex=" + mAxisIndex + ", mAxisIndex=" + mAxisIndex + "]";
  }

  public boolean isLocked() {
    return mLocked;
  }

  public void setLocked(boolean pIsLocked) {
    mLocked = pIsLocked;
  }

}
