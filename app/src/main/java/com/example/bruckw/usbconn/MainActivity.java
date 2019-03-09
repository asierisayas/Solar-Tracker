package com.example.bruckw.usbconn;

        import android.content.Context;
        import android.content.Intent;
        import android.hardware.usb.UsbDevice;
        import android.hardware.usb.UsbDeviceConnection;
        import android.hardware.usb.UsbEndpoint;
        import android.hardware.usb.UsbInterface;
        import android.hardware.usb.UsbManager;
        import android.os.AsyncTask;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.widget.Button;
        import android.widget.SeekBar;
        import android.widget.TextView;

        import com.felhr.usbserial.UsbSerialDevice;
        import com.felhr.usbserial.UsbSerialInterface;

        import java.nio.charset.Charset;
        import java.util.HashMap;
        import java.util.Map;

        import static com.example.bruckw.usbconn.MainActivity.Region.AR;
        import static com.example.bruckw.usbconn.MainActivity.Region.B;
        import static com.example.bruckw.usbconn.MainActivity.Region.BL;
        import static com.example.bruckw.usbconn.MainActivity.Region.BR;
        import static com.example.bruckw.usbconn.MainActivity.Region.L;
        import static com.example.bruckw.usbconn.MainActivity.Region.U;
        import static com.example.bruckw.usbconn.MainActivity.Region.UL;
        import static com.example.bruckw.usbconn.MainActivity.Region.UR;


public class MainActivity extends AppCompatActivity {
    //UI
    private static SeekBar tilt_bar;
    private static SeekBar pan_bar;
    private static TextView tilt_text;
    private static TextView pan_text;
    private static TextView debug_text;
    private Button reset;
    private Button scan;

    //USB Serial
    private static UsbDevice device;
    private static UsbInterface intf;
    private static UsbEndpoint endpoint;
    private static UsbManager mUsbManager;
    private static UsbDeviceConnection connection;
    private static UsbSerialDevice serialPort;
    private static byte[] bytes;

    private static int TIMEOUT = 0;
    private static boolean forceClaim = true;
    private static boolean deviceConnected = false;

    //Position and brightness
    private static int xPos;
    private static int yPos;
    private static HashMap<String, Double> brightMap;
    private static boolean scanComplete;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tilt_text = (TextView) findViewById(R.id.textView1);
        pan_text = (TextView) findViewById(R.id.textView2);
        debug_text = (TextView) findViewById(R.id.textView3);

        scan = (Button) findViewById(R.id.scanBtn);
        reset = (Button) findViewById(R.id.resetBtn);

        xPos = -2700;
        yPos = 580;
        brightMap = new HashMap<>();
        scanComplete = false;

        establishConn();
        seekbar();
    }

    public void establishConn() {
        device = (UsbDevice) getIntent().getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device != null) {
            deviceConnected = true;
            intf = device.getInterface(0);
            endpoint = intf.getEndpoint(0);
            mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
            connection = mUsbManager.openDevice(device);
            connection.claimInterface(intf, forceClaim);

            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            serialPort.open();
            serialPort.setBaudRate(9600);
            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);

            //Set the pan speed
            String panSpeed = "PS" + 4000 + " ";
            bytes = panSpeed.getBytes(Charset.forName("UTF-8"));
            serialPort.write(bytes);

            //Set the tilt speed
            String tiltSpeed = "TS" + 300 + " ";
            bytes = tiltSpeed.getBytes(Charset.forName("UTF-8"));
            serialPort.write(bytes);

        }
    }

    public void seekbar() {
        tilt_bar = (SeekBar) findViewById(R.id.seekBar1);
        pan_bar = (SeekBar) findViewById(R.id.seekBar2);

        pan_bar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int progress_value;
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        progress_value = progress - 3072;
                    }
                                        
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        pan_text.setText("Pan Position: " + progress_value);
                        write("PP", progress_value);
                        debug_text.setText("PP" + progress_value);

                    }
                }
        );

        tilt_bar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    int progress_value;
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        progress_value = progress - 900;
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        tilt_text.setText("Tilt Position: " + progress_value);
                        write("TP", progress_value);
                        debug_text.setText("TP" + progress_value);


                    }
                }
        );
    }

    public void scanClick(View v) {
        brightMap.clear();
        if (isDeviceConnected()) {
            write("PP", -3072);
            write("TP", 500);
            holdOn(2000);
        }

        Intent intent = new Intent(getApplicationContext(), Scan.class);
        startActivity(intent);
    }

    public void resetClick(View v) {
        String output = "r ";
        bytes = output.getBytes(Charset.forName("UTF-8"));
        serialPort.write(bytes);
        debug_text.setText(output);
    }

    public void autoClick(View v) {
        Intent intent = new Intent(getApplicationContext(), Auto.class);
        startActivity(intent);
    }


    //Image Processing Methods
    public static void scan(HashMap<String, Double> values) {
        new ScanTask().execute();

    }

    public static class ScanTask extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            yPos = 580;
            boolean rotate = true;
            while (yPos >= -880) {
                write("TP", yPos);
                holdOn(4000);
                if (rotate) {
                    xPos = -2700;
                    while (xPos <= 2700) {
                        write("PP", xPos);
                        holdOn(1000);
                        String location = String.valueOf(xPos) + "," + String.valueOf(yPos);
                        brightMap.put(location, Scan.getMaxVal());
                        xPos += 675;
                    }
                    rotate = false;
                } else {
                    xPos = 2700;
                    while (xPos >= -2700) {
                        write("PP", xPos);
                        holdOn(1000);
                        String location = String.valueOf(xPos) + "," + String.valueOf(yPos);
                        brightMap.put(location, Scan.getMaxVal());
                        xPos -= 675;
                    }
                    rotate = true;
                }
                if (yPos == -880) {
                    break;
                } else {
                    yPos -= 730;
                }
            }

            holdOn(2000);
            return null;
        }


        protected void onProgressUpdate(String... progress) {
            debug_text.setText(progress[0]);
            pan_bar.setProgress(xPos + 3072);
        }

        protected void onPostExecute(Void unused) {
            Map.Entry<String, Double> maxEntry = null;

            for (Map.Entry<String, Double> entry : brightMap.entrySet())
            {
                if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                {
                    maxEntry = entry;
                }
            }

            String maxPos = maxEntry.getKey();
            String[] xAndY = maxPos.split(",");
            int xMax = Integer.valueOf(xAndY[0]);
            int yMax = Integer.valueOf(xAndY[1]);


            write ("PP", xMax);
            write("TP", yMax);
            holdOn(4000);


            scanComplete = true;
        }
    }

    public enum Region {
        UL, U, UR, //Upper Left, Upper, Upper Right
        L, AR, R, //Left, Acceptable Region, Right
        BL, B, BR, //Bottom Left, Bottom, Bottom Right
    }

    public static Region inRegion(Double xLoc, Double yLoc) {

        Double dispWidth = Auto.getWidth();
        Double dispHeight = Auto.getHeight();

        Double xMax = 0.8 * dispWidth;
        Double xMin = 0.2 * dispWidth;
        Double yMax = 0.8 * dispHeight;
        Double yMin = 0.2 * dispHeight;


        if (yLoc >= yMax) {
            if (xLoc > xMax) {
                return UL;
            } else if (xLoc > xMin) {
                return U;
            } else {
                return UR;
            }
        } else if (yLoc > yMin) {
            if (xLoc > xMax) {
                return L;
            } else if (xLoc < xMin) {
                return Region.R;
            }
        } else if (yLoc < yMin) {
            if (xLoc > xMax) {
                return BL;
            } else if (xLoc > xMin) {
                return B;
            } else {
                return BR;
            }
        }
        return AR;
    }

    public static class AutoTask extends AsyncTask<Void, String, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Double xBrightPos;
            Double yBrightPos;
            Double brightVal;
            while (true) {
                xBrightPos = Auto.getxLoc();
                yBrightPos = Auto.getyLoc();
                brightVal = Auto.getBrightVal();
                Region region = inRegion(xBrightPos, yBrightPos);
                Log.d("brightVal", String.valueOf(brightVal));

                if (brightVal != null && brightVal > 100.0) {
                    switch (region) {
                        case UL:
                            if (yPos > -880) {
                                yPos -= 365;
                                write("TP", yPos);
                            }
                            if (xPos > -2700) {
                                xPos -= 675;
                                write("PP", xPos);
                            }
                            break;
                        case U:
                            if (yPos > -880) {
                                yPos -= 365;
                                write("TP", yPos);
                            }
                            break;
                        case UR:
                            if (yPos > -880) {
                                yPos -= 365;
                                write("TP", yPos);
                            }
                            if (xPos < 2700) {
                                xPos += 675;
                                write("PP", xPos);
                            }
                            break;
                        case L:

                            if (xPos > -2700) {
                                xPos -= 675;
                                write("PP", xPos);
                            }
                            break;
                        case R:
                            if (xPos < 2700) {
                                xPos += 675;
                                write("PP", xPos);
                            }
                            break;
                        case BL:
                            if (yPos < 580) {
                                yPos += 365;
                                write("TP", yPos);
                            }
                            if (xPos > -2700) {
                                xPos -= 675;
                                write("PP", xPos);
                            }
                            break;
                        case B:
                            if (yPos < 580) {
                                yPos += 365;
                                write("TP", yPos);
                            }
                            break;
                        case BR:
                            if (yPos < 580) {
                                yPos += 365;
                                write("TP", yPos);
                            }
                            if (xPos < 2700) {
                                xPos += 675;
                                write("PP", xPos);
                            }
                            break;
                        default:
                            break;
                    }
                }
                    holdOn(2000);

                }

        }

    }



    public static HashMap<String, Double> getBrightMap() {
        return brightMap;
    }

    public static boolean isDeviceConnected() {
        return deviceConnected;
    }

    public static boolean isScanComplete() {
        return scanComplete;
    }

    public static void write(String command, int location) {
        String output = command + location + " ";
        bytes = output.getBytes(Charset.forName("UTF-8"));
        serialPort.write(bytes);
    }

    public static void holdOn(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
