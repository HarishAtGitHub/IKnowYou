package harish.com.augmentedpersontagger;

import android.hardware.camera2.CameraDevice;

/**
 * Created by harish on 7/26/17.
 */
public class CameraTracker extends CameraDevice.StateCallback {
    @Override
    public void onOpened(CameraDevice camera) {
        System.out.print("%%%%%%%%%%%%%%%%%Camera opened%%%%%%%%%%%%%%%%%%%%");
    }

    @Override
    public void onDisconnected(CameraDevice camera) {
        System.out.print("%%%%%%%%%%%%%%%%%Camera disconnected%%%%%%%%%%%%%%%%%%%%");
    }

    @Override
    public void onError(CameraDevice camera, int error) {
        System.out.print("%%%%%%%%%%%%%%%%%Camera error%%%%%%%%%%%%%%%%%%%%");
    }
}
