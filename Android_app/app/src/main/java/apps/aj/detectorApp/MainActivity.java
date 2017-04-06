package apps.aj.detectorApp;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import android.hardware.Camera.Size;
import android.os.Environment;
import android.view.Menu;
import android.view.SubMenu;

import static org.opencv.core.Core.FONT_HERSHEY_SIMPLEX;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.circle;
import static org.opencv.imgproc.Imgproc.putText;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {

    private static final String TAG = "OCVSample::Activity";
    private cameraView mOpenCvCameraView;

    private List<Size> mResolutionList;
    private MenuItem[] mEffectMenuItems;
    private SubMenu mColorEffectsMenu;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;

    Mat mRgba;
    Mat threshold;
    Mat hsv;

    int mid, straigtDist;

    private BluetoothAdapter myBluetooth = null;
    String address = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Intent newint = getIntent();
        address = newint.getStringExtra(connect.EXTRA_ADDRESS);

        setContentView(R.layout.activity_main);

        new ConnectBT().execute();

        mOpenCvCameraView = (cameraView) findViewById(R.id.camera_view_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableView();
    }

    static {
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV initialize success");
        } else {
            Log.i(TAG, "OpenCV initialize failed");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
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
        mRgba = new Mat(height, width, CV_8UC4);
        hsv = new Mat(height, width, CV_8UC3);
        threshold = new Mat(height, width, CV_8UC1);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        detect();
        return mRgba;
    }

    // this is the function which is responsible for detection of a blue color ball...
    void detect(){
        Imgproc.cvtColor(mRgba, hsv,Imgproc.COLOR_BGR2HSV);

        //inRange for blue color
        Core.inRange(hsv, new Scalar(10,70,70), new Scalar(30,200,200), threshold);

        Imgproc.blur(threshold, threshold, new org.opencv.core.Size(3.0, 3.0));

        //Imgproc.erode(threshold, threshold, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(2, 2)));
        Imgproc.dilate(threshold, threshold, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new org.opencv.core.Size(8, 8)));

        //// for circle detection ===
        /*Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_BGR2GRAY);
        Mat cannyEdges = new Mat();
        Imgproc.Canny(threshold, cannyEdges, 10, 100);*/
        Mat circles = new Mat();

        //HoughCircles detects circular objects... and hence only circular blue ball is detected
        Imgproc.HoughCircles(threshold, circles, Imgproc.CV_HOUGH_GRADIENT, 2, threshold.height()/4, 200, 40, 0, 100);

        Log.d("tag", ""+circles.cols());

        if(circles.cols() == 1) {
            for (int i = 0; i < circles.cols(); i++) {
                double[] parameters = circles.get(0, i);
                double x, y;
                int r;
                x = parameters[0];
                y = parameters[1];
                r = (int) parameters[2];
                Point center = new Point(x, y);
                //Drawing circles on an image
                circle(mRgba, center, r, new Scalar(255, 0, 0), 1);
                putText(mRgba, "(" + center.x + ", " + center.y + ") , " + r, center, FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 255), 1);

                if (r < 5000) {

                    mid = mRgba.cols() / 2;
                    straigtDist = (mid * 20) / 100;

                    if (center.x > (mid + straigtDist)) {
                        right();
                    } else if (center.x < (mid - straigtDist)) {
                        left();
                    } else {
                        accelerate();
                    }
                } else {
                    stop();
                }
            }
        } else {
            stop();
        }
        ///// end ======

        /// for contour detection ...
        /*Mat hierarchy = new Mat();
        List<MatOfPoint> contourList = new ArrayList<MatOfPoint>();
        Imgproc.findContours(threshold, contourList, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        //Drawing contours on a new image
        *//*Mat contours = new Mat();
        contours.create(cannyEdges.rows(),cannyEdges.cols(),CvType.CV_8UC3);*//*

        List<Moments> mu = new ArrayList<Moments>(contourList.size());

        Random r = new Random();
        for (int i = 0; i < contourList.size(); i++) {
            mu.add(i, Imgproc.moments(contourList.get(i), false));
            Moments p = mu.get(i);
            int x = (int) (p.get_m10() / p.get_m00());
            int y = (int) (p.get_m01() / p.get_m00());
            //circle(mRgba, new Point(x, y), 100, new Scalar(0,0,255), 1);

            putText(mRgba, "center at ("+x+", "+y+")", new Point(x,y), FONT_HERSHEY_SIMPLEX, 1, new Scalar(0,0,255), 1);

            Imgproc.drawContours(mRgba, contourList, i, new Scalar(r.nextInt(255) ,r.nextInt(255), r.nextInt(255)), -1);
        }*/
        ///////////// --- end contour detection ====

        //mRgba = threshold.clone();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        List<String> effects = mOpenCvCameraView.getEffectList();

        if (effects == null) {
            Log.e(TAG, "Color effects are not supported by device!");
            return true;
        }

        mColorEffectsMenu = menu.addSubMenu("Color Effect");
        mEffectMenuItems = new MenuItem[effects.size()];

        int idx = 0;
        ListIterator<String> effectItr = effects.listIterator();
        while(effectItr.hasNext()) {
            String element = effectItr.next();
            mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx, Menu.NONE, element);
            idx++;
        }

        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<Size> resolutionItr = mResolutionList.listIterator();
        idx = 0;
        while(resolutionItr.hasNext()) {
            Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
            idx++;
        }
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item.getGroupId() == 1)
        {
            mOpenCvCameraView.setEffect((String) item.getTitle());
            Toast.makeText(this, mOpenCvCameraView.getEffect(), Toast.LENGTH_SHORT).show();
        }
        else if (item.getGroupId() == 2)
        {
            int id = item.getItemId();
            Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG,"onTouch event");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String fileName = Environment.getExternalStorageDirectory().getPath() + "/sample_picture_" + currentDateandTime + ".jpg";
        mOpenCvCameraView.takePicture(fileName);
        Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
        return false;
    }

    ////// bluetooth connection & navigation

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected
        private ProgressDialog progress;

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(MainActivity.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

    // fast way to call Toast
    private void msg(String s)
    {
        //Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
        Log.d("tag", s);
    }

    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout

    }

    private void stop()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("s".toString().getBytes());
                Log.d("motion", "stop");
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void accelerate()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("a".toString().getBytes());
                Log.d("motion", "accelerate");
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }


    private void right()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("r".toString().getBytes());
                Log.d("motion", "right");
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }


    private void left()
    {
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write("l".toString().getBytes());
                Log.d("motion", "left");
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }
}