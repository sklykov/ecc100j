package ecc100;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.bridj.Pointer;

import com.google.common.collect.HashBasedTable;

import ecc100.bindings.EccInfo;
import ecc100.bindings.EccLibrary;

public class ECC100Controller
{

	private static final int cMaxNumberOfControllers = 4;
	private static final int cNumberOfAxisPerController = 3;
	private ArrayList<Pointer<Integer>> mPointerToDeviceHandleList = new ArrayList<>();
	private HashSet<Integer> mDeviceIdList = new HashSet<>();
	private HashBasedTable<Integer, Integer, ECC100Axis> mDeviceIdAxisIndexToAxisMap = HashBasedTable.create();
	private int mNumberOfControllers;

	public ECC100Controller()
	{
		super();
	}

	public boolean open()
	{

		Pointer<Pointer<EccInfo>> lPointerTopointerToInfoStruct = Pointer.allocatePointers(	EccInfo.class,
																																												4);
		for (int i = 0; i < cMaxNumberOfControllers; i++)
		{
			lPointerTopointerToInfoStruct.set(Pointer.allocate(EccInfo.class));
		}

		mNumberOfControllers = EccLibrary.ECC_Check(lPointerTopointerToInfoStruct);

		// System.out.println("mNumberOfControllers=" + mNumberOfControllers);

		for (int i = 0; i < mNumberOfControllers; i++)
		{
			Pointer<EccInfo> lPointerToInfoStruct = lPointerTopointerToInfoStruct.get(i);

			if (lPointerToInfoStruct != null)
			{
				EccInfo lEccInfo = lPointerToInfoStruct.get();
				// System.out.println("lEccInfo" + i + "->" + lEccInfo);

				Pointer<Integer> lPointerToDeviceHandle = Pointer.allocateInt();
				EccLibrary.ECC_Connect(i, lPointerToDeviceHandle);

				mPointerToDeviceHandleList.add(lPointerToDeviceHandle);

				for (int j = 0; j < cNumberOfAxisPerController; j++)
				{
					ECC100Axis lECC100Axis = new ECC100Axis(this, i, j);
					lECC100Axis.setLocked(lEccInfo.locked() != 0);
					final int lDeviceId = lEccInfo.id();
					mDeviceIdList.add(lDeviceId);
					mDeviceIdAxisIndexToAxisMap.put(lDeviceId, j, lECC100Axis);
				}
			}
		}

		return true;
	}

	public void close()
	{
		for (Pointer<Integer> lPointerToControllerDeviceHandle : mPointerToDeviceHandleList)
		{
			EccLibrary.ECC_Close(lPointerToControllerDeviceHandle.getInt());
			lPointerToControllerDeviceHandle.release();
		}
	}

	protected int getControllerDeviceHandle(int pDeviceIndex)
	{
		return mPointerToDeviceHandleList.get(pDeviceIndex).getInt();
	}

	public List<Integer> getDeviceIdList()
	{
		return new ArrayList<Integer>(mDeviceIdList);
	}

	public ECC100Axis getAxis(int pDeviceId, int pAxisIndex)
	{
		return (ECC100Axis) mDeviceIdAxisIndexToAxisMap.get(pDeviceId,
																												pAxisIndex);
	}

	public boolean start()
	{
		Collection<ECC100Axis> lAllECC100Axis = mDeviceIdAxisIndexToAxisMap.values();

		for (ECC100Axis lECC100Axis : lAllECC100Axis)
			lECC100Axis.home();
		return true;
	}

	public boolean stop()
	{
		Collection<ECC100Axis> lAllECC100Axis = mDeviceIdAxisIndexToAxisMap.values();

		for (ECC100Axis lECC100Axis : lAllECC100Axis)
			lECC100Axis.stop();
		return true;
	}

}
