package com.example.bruckw.usbconn;



import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import static org.opencv.core.Core.minMaxLoc;

public class Scan extends AppCompatActivity implements CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";

    //Loads camera view; Allows openCV use
    private CameraBridgeViewBase mOpenCvCameraView;

    //Used in Camera selection from menu
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;

    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    private Mat mRgba;
    private Mat mRgbaF;
    private Mat mRgbaT;

    private static double maxVal;
    CvCameraViewFrame inputFrame1 = null;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_scan);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_java_scan_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);

        if (MainActivity.isDeviceConnected()) {
            MainActivity.getBrightMap().clear();
            MainActivity.scan(MainActivity.getBrightMap());
        }
        maxVal = 0.0;
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        if (MainActivity.isScanComplete()) {
            Intent intent = new Intent(getApplicationContext(), Auto.class);
            startActivity(intent);
        } else {
            Mat grayScale = new Mat();
            Imgproc.cvtColor(rgba, grayScale, Imgproc.COLOR_BGR2GRAY);
            Core.MinMaxLocResult res = Core.minMaxLoc(grayScale);
            maxVal = res.maxVal;
        }

        Log.d("maxpt", String.valueOf(maxVal));

        return rgba; // This function must return
    }

    public static Double getMaxVal() {
        return maxVal;
    }


}
