package harish.com.augmentedpersontagger;

import android.content.Context;
import android.graphics.Canvas;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by harish on 7/25/17.
 */
public class RealityViewTracker implements SurfaceHolder.Callback {

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        System.out.println("***********Surface created ************");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        System.out.println("***********Surface changed ************");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        System.out.println("***********Surface destroyed ************");
    }

    public void onDraw(Canvas canvas) {

    }
}
