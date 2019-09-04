package ecc100;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.google.common.collect.HashBasedTable;

import ecc100.bindings.EccInfo;
import ecc100.bindings.EccLibrary;

import org.bridj.Pointer;

/**
 * Class, allowing not only controlling (as I understand) entire ECC100 Controller
 * but additionally all connected stages (axis)
 */
public class ECC100Controller {
  private static final int cMaxNumberOfControllers = 2;
  private static final int cNumberOfAxisPerController = 3;
  private final ArrayList<Pointer<Integer>> mPointerToDeviceHandleList = new ArrayList<>();
  private final HashSet<Integer> mDeviceIdList = new HashSet<>();
  private final HashBasedTable<Integer, Integer, ECC100Axis> mDeviceIdAxisIndexToAxisMap = HashBasedTable.create();
  private int mNumberOfControllers;
  private volatile boolean mIsOpened = false;


  public ECC100Controller() { super(); } // default constructor

  /**
   * Actually, here are initialized: mNumberOfControllers, mDeviceIdList, mDeviceIdAxisIndexToAxisMap, mPointerToDeviceHandleList
   * Maybe it would be better renamed "initialize"
   * @return just as a point - even if all controllers / stages are disconnected, it will return "true"
   */
  public boolean open() {
    try {
      // Confusing -  Pointer of Pointer of EccInfo
      final Pointer<Pointer<EccInfo>> lPointerToPointerToInfoStruct = Pointer.allocatePointers(EccInfo.class, cMaxNumberOfControllers);
      for (int i = 0; i < cMaxNumberOfControllers; i++) {
        lPointerToPointerToInfoStruct.set(i,Pointer.allocate(EccInfo.class));
      }
      // get number of controllers (difference with devices - ?)
      mNumberOfControllers = EccLibrary.ECC_Check(lPointerToPointerToInfoStruct);
      System.out.println("mNumberOfControllers =  " + mNumberOfControllers);

      for (int i = 0; i < mNumberOfControllers; i++) {
        // Pointer<EccInfo> lPointerToInfoStruct = lPointerToPointerToInfoStruct.get(i);
        // if (lPointerToInfoStruct != null)
        {
          // EccInfo lEccInfo = lPointerToInfoStruct.get();
          // System.out.println("lEccInfo" + i + "->" + lEccInfo);
          final Pointer<Integer> lPointerToDeviceHandle = Pointer.allocateInt(); // some LOCAL pointer to device handle
          EccLibrary.ECC_Connect(i, lPointerToDeviceHandle); // calling some method returning int ECC_Connect...
          mPointerToDeviceHandleList.add(lPointerToDeviceHandle); // add Pointer to Device Handle to the list
          for (int j = 0; j < cNumberOfAxisPerController; j++) {
            final ECC100Axis lECC100Axis = new ECC100Axis(this, i, j); // here actual initialization of LOCAL ECC100Axis!
            lECC100Axis.setLocked(false); // unlock all axis if they have been locked before
            // final int lDeviceId = i; // I found this additional variable redundant and confusing
            mDeviceIdList.add(i); // actually, adding a device in a list
            mDeviceIdAxisIndexToAxisMap.put(i, j, lECC100Axis); // add device to another list containing also information about axis -
          }
        }
      }

      Runtime.getRuntime().addShutdownHook(new Thread()
      {
        @Override
        public void run() {
          try {
            close();
          }
          catch (final Throwable e) {
            e.printStackTrace();
          }
        }
      });
      mIsOpened = true;
      return true;
    }
    catch (final Throwable e) {
      e.printStackTrace();
      return false;
    }
  }

  public void close() {
    if (!mIsOpened)
      return; // stops here if connection hasn't been opened
    for (final ECC100Axis lECC100Axis : mDeviceIdAxisIndexToAxisMap.values()) {
      lECC100Axis.home();
    }

    for (final Pointer<Integer> lPointerToControllerDeviceHandle : mPointerToDeviceHandleList) {
      EccLibrary.ECC_Close(lPointerToControllerDeviceHandle.getInt());
      lPointerToControllerDeviceHandle.release();
    }

    mIsOpened = false;
  }

  /**
   * Returning some handle to entire controller ECC100
   * @param pDeviceIndex - enumerate devices
   * @return ControllerDeviceHandle - int number for further utilization in ECC100Axis class for referring to the stage
   */
  protected int getControllerDeviceHandle(int pDeviceIndex) {
    return mPointerToDeviceHandleList.get(pDeviceIndex).getInt();
  }

  /**
   * Generate ArrayList referring to devices... More info?
   * @return List<Integer> - for referring to an entire list of devices...
   */
  public List<Integer> getDeviceIdList() {
    return new ArrayList<Integer>(mDeviceIdList);
  }

  public ECC100Axis getAxis(int pDeviceId, int pAxisIndex) {
    return mDeviceIdAxisIndexToAxisMap.get(pDeviceId, pAxisIndex);
  }

  /**
   * Access to available axis (stages) and set them to the home ("0 um") positions...
   * @return always true
   */
  public boolean start() {
    final Collection<ECC100Axis> lAllECC100Axis = mDeviceIdAxisIndexToAxisMap.values();
    for (final ECC100Axis lECC100Axis : lAllECC100Axis)
      lECC100Axis.home();
    return true; // in general, always return true. Why?
}

  public boolean stop() {
    final Collection<ECC100Axis> lAllECC100Axis = mDeviceIdAxisIndexToAxisMap.values();
    for (final ECC100Axis lECC100Axis : lAllECC100Axis)
      lECC100Axis.stop();
    return true;
  }

}
