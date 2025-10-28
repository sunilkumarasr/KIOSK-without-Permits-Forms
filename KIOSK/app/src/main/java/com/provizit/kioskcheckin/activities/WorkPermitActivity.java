package com.provizit.kioskcheckin.activities;

import static android.view.View.GONE;

import static com.provizit.kioskcheckin.services.Conversions.convertArabicToEnglish;

import android.Manifest;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.provizit.kioskcheckin.R;
import com.provizit.kioskcheckin.activities.WarningScreens.LocationValidationMeetingActivity;
import com.provizit.kioskcheckin.api.DataManger;
import com.provizit.kioskcheckin.config.Preferences;
import com.provizit.kioskcheckin.logins.VisitorLoginActivity;
import com.provizit.kioskcheckin.mvvm.ApiViewModel;
import com.provizit.kioskcheckin.services.Conversions;
import com.provizit.kioskcheckin.utilities.WorkPermit.ContractorsData;
import com.provizit.kioskcheckin.utilities.WorkPermit.LocationData;
import com.provizit.kioskcheckin.utilities.WorkPermit.SubContractorsData;
import com.provizit.kioskcheckin.utilities.WorkPermit.WorkLocationData;
import com.provizit.kioskcheckin.utilities.WorkPermit.WorkTypeData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkPermitActivity extends AppCompatActivity implements View.OnClickListener {

    ApiViewModel apiViewModel;
    ProgressDialog progress;

    LinearLayout LinearDetails, line1, linearWarning;
    ImageView back_image, logo, imgContractor;
    TextView txtEnter, txtCName, txtCompany, txtWorkName, txtTime, txtDate, txtLocation;
    Button btnCancel, btnNext, btnOk;

    ArrayList<String> StartsList;
    ArrayList<String> EndList;
    ArrayList<ContractorsData> contractorsDataList;
    ArrayList<SubContractorsData> subcontractorsDataList;
    String _id = "";
    String inputValue = "";
    String valueType = "";
    String permitType = "";
    String id = "";
    String ndaStatus = "";

    long currentMillis;
    String statusCheckIn = "";

    //device camera
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private Uri filePath;
    private File photoFile;
    String image_name;
    String filename;

    //print
    private static final String ACTION_USB_PERMISSION = "com.provizit.kioskcheckin.USB_PERMISSION";
    String qrData = "";
    String permitUserName = "";
    String badgeNumber = "";


    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbManager != null && usbDevice != null) {
                            try {
                                EscPosPrinter printer = new EscPosPrinter(
                                        new UsbConnection(usbManager, usbDevice),
                                        203,       // DPI
                                        58f,       // mm width
                                        32         // characters per line
                                );

//                                "[L]\n" +

                                Drawable drawable = getResources().getDrawable(R.drawable.logo);
                                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 200, 50, false);

                                printer.printFormattedText(
                                        "[C]<img>" +
                                                PrinterTextParserImg.bitmapToHexadecimalString(printer, scaledBitmap) +
                                                "</img>\n" +
                                                "[C]\n" +
                                                "[C]<font size='small'>Name: "+permitUserName+"\nBadge Number: "+badgeNumber+"</font>\n" +
                                                "[C]\n" +
                                                "[C]<qrcode size='14'>"+qrData+"</qrcode>\n"
                                );


//                                Toast.makeText(context, "Print successful!", Toast.LENGTH_SHORT).show();
                                Intent intents = new Intent(WorkPermitActivity.this, ChekInPermitStatusActivity.class);
                                intents.putExtra("status", "1");
                                intents.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intents);

                            } catch (EscPosConnectionException | EscPosEncodingException |
                                     EscPosParserException e) {
                                e.printStackTrace();
                                Toast.makeText(context, "Printing failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            } catch (EscPosBarcodeException e) {
                                throw new RuntimeException(e);
                            }

                        }
                    } else {
                        Toast.makeText(context, "USB permission denied", Toast.LENGTH_SHORT).show();
                    }
                    unregisterReceiver(this);
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_permit);

        LinearDetails = findViewById(R.id.LinearDetails);
        line1 = findViewById(R.id.line1);
        linearWarning = findViewById(R.id.linearWarning);
        back_image = findViewById(R.id.back_image);
        logo = findViewById(R.id.logo);
        imgContractor = findViewById(R.id.imgContractor);
        txtEnter = findViewById(R.id.txtEnter);
        txtCName = findViewById(R.id.txtCName);
        txtCompany = findViewById(R.id.txtCompany);
        txtWorkName = findViewById(R.id.txtWorkName);
        txtTime = findViewById(R.id.txtTime);
        txtDate = findViewById(R.id.txtDate);
        txtLocation = findViewById(R.id.txtLocation);
        btnCancel = findViewById(R.id.btnCancel);
        btnNext = findViewById(R.id.btnNext);
        btnOk = findViewById(R.id.btnOk);

        _id = getIntent().getStringExtra("_id");
        inputValue = getIntent().getStringExtra("inputValue");
        valueType = getIntent().getStringExtra("valueType");
        permitType = getIntent().getStringExtra("permitType");
        ndaStatus = getIntent().getStringExtra("ndaStatus");

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String VersionName = sharedPreferences.getString("VersionName", "1.0");
        TextView txtVersion = findViewById(R.id.txtVersion);
        txtVersion.setText(VersionName);
        //company logo
        String c_Logo = Preferences.loadStringValue(getApplicationContext(), Preferences.company_Logo, "");
        if (c_Logo.equalsIgnoreCase("")) {
        } else {
            Glide.with(WorkPermitActivity.this).load(c_Logo)
                    .into(logo);
        }

        Calendar calendar = Calendar.getInstance();
        long currentTimeMillis = calendar.getTimeInMillis();
        TimeZone timeZone = calendar.getTimeZone();
        int offsetMillis = timeZone.getOffset(calendar.getTimeInMillis());
        currentMillis = currentTimeMillis - offsetMillis;

        progress = new ProgressDialog(this);
        progress.setTitle(getResources().getString(R.string.Loading));
        progress.setMessage(getResources().getString(R.string.whileloading));
        progress.setCancelable(true);
        progress.setCanceledOnTouchOutside(true);

        apiViewModel = new ViewModelProvider(WorkPermitActivity.this).get(ApiViewModel.class);


        //workDetails
        apiViewModel.getworkpermitDetails(getApplicationContext(), _id);
        progress.show();
        apiViewModel.getworkpermitDetails_response().observe(this, model -> {
            progress.dismiss();
            StartsList = new ArrayList<>();
            StartsList.clear();
            EndList = new ArrayList<>();
            EndList.clear();
            contractorsDataList = new ArrayList<>();
            subcontractorsDataList = new ArrayList<>();
            try {
                if (model != null && model.getItems() != null && model.getItems().getContractorsData() != null) {

                    badgeNumber = model.getItems().getWorkpermit_id();

                    txtCompany.setText(model.getItems().getCompanyName());
                    id = model.getItems().get_id().get$oid();
                    //Start time
                    String s_time = "";
                    String e_time = "";
                    StartsList.addAll(model.getItems().getStarts());
                    //End time
                    EndList.addAll(model.getItems().getEnds());

                    Calendar ca = Calendar.getInstance();
                    ca.set(Calendar.HOUR_OF_DAY, 0);
                    ca.set(Calendar.MINUTE, 0);
                    ca.set(Calendar.SECOND, 0);
                    ca.set(Calendar.MILLISECOND, 0);
                    long todayStartTimestamp = ca.getTimeInMillis();
                    System.out.println("Today's Start Timestamp: " + todayStartTimestamp);

                    long startMillis = (model.getItems().getStart() + Conversions.timezone()) * 1000;
                    long endMillis = (model.getItems().getEnd() + Conversions.timezone()) * 1000;
                    System.out.println("startMillis: " + startMillis);
                    System.out.println("endMillis: " + endMillis);

                    // Get the current timestamp
                    long currents = System.currentTimeMillis();
                    // Convert to readable date/time
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa", Locale.ENGLISH);
                    sdf.setTimeZone(TimeZone.getDefault()); // Set to device's timezone
                    String formattedTime = sdf.format(new Date(currents));

                    Date cdate = sdf.parse(formattedTime);
                    long Ctimestamp = cdate.getTime();

                    String endTime = "";
                    if (!EndList.isEmpty()) {
                        long startTimestamp = (long) (Double.parseDouble(EndList.get(0)) + Conversions.timezone());
                        // Add 1 minute (60 seconds = 60000 milliseconds) before converting
                        endTime = Conversions.millitotime((startTimestamp * 1000) + 60000, false);
                    }
                    endTime = convertArabicToEnglish(endTime);
                    endTime = endTime.replace("ص", "AM").replace("م", "PM");

                    Date Edate = sdf.parse(endTime);
                    long Etimestamp = Edate.getTime();

                    if (todayStartTimestamp == startMillis || todayStartTimestamp > startMillis && todayStartTimestamp < endMillis) {
                        System.out.println("Converted 1: " + "1");
                        if (!StartsList.isEmpty()) {
                            if (Ctimestamp < Etimestamp) {
                                System.out.println("Converted 1: " + "5");
                                LinearDetails.setVisibility(View.VISIBLE);
                                line1.setVisibility(View.VISIBLE);
                                linearWarning.setVisibility(GONE);
                            } else {
                                System.out.println("Converted 1: " + "6");
                                LinearDetails.setVisibility(GONE);
                                line1.setVisibility(GONE);
                                linearWarning.setVisibility(View.VISIBLE);
                            }
                        }
                    } else {
                        System.out.println("Converted 1: " + "2");
                        LinearDetails.setVisibility(GONE);
                        line1.setVisibility(GONE);
                        linearWarning.setVisibility(View.VISIBLE);
                    }

                    if (!StartsList.isEmpty()) {
                        long startTimestamp = (long) (Double.parseDouble(StartsList.get(0)) + Conversions.timezone());
                        s_time = Conversions.millitotime(startTimestamp * 1000, false);
                    }

                    if (!EndList.isEmpty()) {
                        long startTimestamp = (long) (Double.parseDouble(EndList.get(0)) + Conversions.timezone());
                        // e_time = Conversions.millitotime(startTimestamp * 1000, false);
                        // Add 1 minute (60 seconds = 60000 milliseconds) before converting
                        e_time = Conversions.millitotime((startTimestamp * 1000) + 60000, false);
                    }

                    String stateDate = Conversions.millitodateD((model.getItems().getStart() + Conversions.timezone()) * 1000);
                    String endDate = Conversions.millitodateD((model.getItems().getEnd() + Conversions.timezone()) * 1000);

                    txtTime.setText(s_time + " to " + e_time);
                    txtDate.setText(stateDate + " to " + endDate);

                    //name
                    WorkTypeData workTypeData = model.getItems().getWorktypeData();
                    if (workTypeData != null) {
                        txtWorkName.setText(workTypeData.getName());
                    }
                    //location
                    WorkLocationData workLocation = model.getItems().getWorklocationData();
                    LocationData locationData = model.getItems().getLocations_Data();
                    if (workLocation != null) {
                        txtLocation.setText(workLocation.getName() + "," + locationData.getName());
                    }

                    contractorsDataList.addAll(model.getItems().getContractorsData());
                    if (!contractorsDataList.isEmpty()) {
                        for (int i = 0; i < contractorsDataList.size(); i++) {
                            ContractorsData contractor = contractorsDataList.get(i);
                            if (contractor != null && contractor.getEmail() != null && contractor.getEmail().equalsIgnoreCase(inputValue)) {
                                txtCName.setText(contractor.getName());
                                permitUserName = contractor.getName();
                                if (contractor.getCheckin() == (0)) {
                                    btnNext.setText(getString(R.string.CheckIn));
                                    statusCheckIn = "Check-In";
                                } else if (contractor.getCheckin() == (1)) {
                                    btnNext.setText(getString(R.string.CheckOut));
                                    statusCheckIn = "Check-Out";
                                } else {
                                    btnNext.setVisibility(GONE);
                                }
                            } else if (contractor != null && contractor.getMobile() != null && contractor.getMobile().equalsIgnoreCase(inputValue)) {
                                txtCName.setText(contractor.getName());
                                permitUserName = contractor.getName();
                                inputValue = contractor.getEmail();
                                if (contractor.getCheckin() == (0)) {
                                    btnNext.setText(getString(R.string.CheckIn));
                                    statusCheckIn = "Check-In";
                                } else if (contractor.getCheckin() == (1)) {
                                    btnNext.setText(getString(R.string.CheckOut));
                                    statusCheckIn = "Check-Out";
                                } else {
                                    btnNext.setVisibility(GONE);
                                }
                            }
                        }
                    }

                    //meeting cancel
                    Log.e("status_", model.getItems().getStatus());
                    if (model.getItems().getStatus().equalsIgnoreCase("2.0")) {
                        txtEnter.setBackgroundColor(Color.parseColor("#8B0000"));
                        txtEnter.setText("The Work Permit has been Cancelled");
                        line1.setVisibility(GONE);
                    }

                }
                if (model != null && model.getItems() != null && model.getItems().getSubcontractorsData() != null) {

                    txtCompany.setText(model.getItems().getCompanyName());
                    id = model.getItems().get_id().get$oid();
                    //Start time
                    String s_time = "";
                    String e_time = "";
                    StartsList.addAll(model.getItems().getStarts());
                    //End time
                    EndList.addAll(model.getItems().getEnds());

                    Calendar ca = Calendar.getInstance();
                    ca.set(Calendar.HOUR_OF_DAY, 0);
                    ca.set(Calendar.MINUTE, 0);
                    ca.set(Calendar.SECOND, 0);
                    ca.set(Calendar.MILLISECOND, 0);
                    long todayStartTimestamp = ca.getTimeInMillis();
                    System.out.println("Today's Start Timestamp: " + todayStartTimestamp);

                    long startMillis = (model.getItems().getStart() + Conversions.timezone()) * 1000;
                    long endMillis = (model.getItems().getEnd() + Conversions.timezone()) * 1000;
                    System.out.println("startMillis: " + startMillis);
                    System.out.println("endMillis: " + endMillis);

                    // Get the current timestamp
                    long currents = System.currentTimeMillis();
                    // Convert to readable date/time
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa", Locale.ENGLISH);
                    sdf.setTimeZone(TimeZone.getDefault()); // Set to device's timezone
                    String formattedTime = sdf.format(new Date(currents));

                    Date cdate = sdf.parse(formattedTime);
                    long Ctimestamp = cdate.getTime();

                    String endTime = "";
                    if (!EndList.isEmpty()) {
                        long startTimestamp = (long) (Double.parseDouble(EndList.get(0)) + Conversions.timezone());
                        // Add 1 minute (60 seconds = 60000 milliseconds) before converting
                        endTime = Conversions.millitotime((startTimestamp * 1000) + 60000, false);
                    }
                    endTime = convertArabicToEnglish(endTime);
                    endTime = endTime.replace("ص", "AM").replace("م", "PM");

                    Date Edate = sdf.parse(endTime);
                    long Etimestamp = Edate.getTime();

                    if (todayStartTimestamp == startMillis || todayStartTimestamp > startMillis && todayStartTimestamp < endMillis) {
                        System.out.println("Converted 1: " + "1");
                        if (!StartsList.isEmpty()) {
                            if (Ctimestamp < Etimestamp) {
                                System.out.println("Converted 1: " + "5");
                                LinearDetails.setVisibility(View.VISIBLE);
                                line1.setVisibility(View.VISIBLE);
                                linearWarning.setVisibility(GONE);
                            } else {
                                System.out.println("Converted 1: " + "6");
                                LinearDetails.setVisibility(GONE);
                                line1.setVisibility(GONE);
                                linearWarning.setVisibility(View.VISIBLE);
                            }
                        }
                    } else {
                        System.out.println("Converted 1: " + "2");
                        LinearDetails.setVisibility(GONE);
                        line1.setVisibility(GONE);
                        linearWarning.setVisibility(View.VISIBLE);
                    }

                    if (!StartsList.isEmpty()) {
                        long startTimestamp = (long) (Double.parseDouble(StartsList.get(0)) + Conversions.timezone());
                        s_time = Conversions.millitotime(startTimestamp * 1000, false);
                    }

                    if (!EndList.isEmpty()) {
                        long startTimestamp = (long) (Double.parseDouble(EndList.get(0)) + Conversions.timezone());
                        // e_time = Conversions.millitotime(startTimestamp * 1000, false);
                        // Add 1 minute (60 seconds = 60000 milliseconds) before converting
                        e_time = Conversions.millitotime((startTimestamp * 1000) + 60000, false);
                    }

                    String stateDate = Conversions.millitodateD((model.getItems().getStart() + Conversions.timezone()) * 1000);
                    String endDate = Conversions.millitodateD((model.getItems().getEnd() + Conversions.timezone()) * 1000);

                    txtTime.setText(s_time + " to " + e_time);
                    txtDate.setText(stateDate + " to " + endDate);

                    //name
                    WorkTypeData workTypeData = model.getItems().getWorktypeData();
                    if (workTypeData != null) {
                        txtWorkName.setText(workTypeData.getName());
                    }
                    //location
                    WorkLocationData workLocation = model.getItems().getWorklocationData();
                    LocationData locationData = model.getItems().getLocations_Data();
                    if (workLocation != null) {
                        txtLocation.setText(workLocation.getName() + "," + locationData.getName());
                    }

                    subcontractorsDataList.addAll(model.getItems().getSubcontractorsData());
                    if (!subcontractorsDataList.isEmpty()) {
                        for (int i = 0; i < subcontractorsDataList.size(); i++) {
                            SubContractorsData contractor = subcontractorsDataList.get(i);
                            if (contractor != null && contractor.getEmail() != null && contractor.getEmail().equalsIgnoreCase(inputValue)) {
                                txtCName.setText(contractor.getName());
                                permitUserName = contractor.getName();
                                if (contractor.getCheckin() == (0)) {
                                    btnNext.setText(getString(R.string.CheckIn));
                                    statusCheckIn = "Check-In";
                                } else if (contractor.getCheckin() == (1)) {
                                    btnNext.setText(getString(R.string.CheckOut));
                                    statusCheckIn = "Check-Out";
                                } else {
                                    btnNext.setVisibility(GONE);
                                }
                            } else if (contractor != null && contractor.getMobile() != null && contractor.getMobile().equalsIgnoreCase(inputValue)) {
                                txtCName.setText(contractor.getName());
                                permitUserName = contractor.getName();
                                inputValue = contractor.getEmail();
                                if (contractor.getCheckin() == (0)) {
                                    btnNext.setText(getString(R.string.CheckIn));
                                    statusCheckIn = "Check-In";
                                } else if (contractor.getCheckin() == (1)) {
                                    btnNext.setText(getString(R.string.CheckOut));
                                    statusCheckIn = "Check-Out";
                                } else {
                                    btnNext.setVisibility(GONE);
                                }
                            }
                        }
                    }

                    //meeting cancel
                    Log.e("status_", model.getItems().getStatus());
                    if (model.getItems().getStatus().equalsIgnoreCase("2.0")) {
                        txtEnter.setBackgroundColor(Color.parseColor("#8B0000"));
                        txtEnter.setText("The Work Permit has been Cancelled");
                        line1.setVisibility(GONE);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        //updateCheckIn
        apiViewModel.updateworkpermitaDetails_response().observe(this, model -> {
            progress.dismiss();
            try {
                if (model != null) {
                    Integer statuscode = model.getResult();
                    if (statuscode.equals(200)) {
                        if (statusCheckIn.equalsIgnoreCase("Check-In")) {
                            Intent intents = new Intent(getApplicationContext(), ChekInPermitStatusActivity.class);
                            intents.putExtra("status", "1");
                            startActivity(intents);
                        } else {
                            Intent intents = new Intent(getApplicationContext(), ChekInPermitStatusActivity.class);
                            intents.putExtra("status", "2");
                            startActivity(intents);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Try again", Toast.LENGTH_LONG).show();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                progress.dismiss();
            }
        });

        back_image.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
        btnNext.setOnClickListener(this);
        btnOk.setOnClickListener(this);
    }

    //disable auto click action after scann
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            // Barcode scanner has scanned a barcode, disable triggered items
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_DEL) {
            return true;
        }

        disableTriggeredItems();

        return super.onKeyDown(keyCode, event);
    }

    //usb scanner
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_ENTER:
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    Intent intent = new Intent(getApplicationContext(), VisitorLoginActivity.class);
                    startActivity(intent);
                    return true;
                case KeyEvent.KEYCODE_DEL:
                    Log.d("KeyEvent", "DEL in dispatchKeyEvent");
                    break;
                default:
                    char keyChar = (char) event.getUnicodeChar();
                    if (Character.isLetterOrDigit(keyChar)) {
                        return true;
                    }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void disableTriggeredItems() {
        back_image.setFocusable(false);
        back_image.setFocusableInTouchMode(false);
        btnCancel.setFocusable(false);
        btnCancel.setFocusableInTouchMode(false);
        btnNext.setFocusable(false);
        btnNext.setFocusableInTouchMode(false);
        btnOk.setFocusable(false);
        btnOk.setFocusableInTouchMode(false);
        runthred();
    }

    private void runthred() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                overridePendingTransition(0, 0);
                startActivity(getIntent());
                overridePendingTransition(0, 0);

                back_image.setFocusable(true);
                back_image.setFocusableInTouchMode(true);
                btnCancel.setFocusable(true);
                btnCancel.setFocusableInTouchMode(true);
                btnNext.setFocusable(true);
                btnNext.setFocusableInTouchMode(true);
                btnOk.setFocusable(true);
                btnOk.setFocusableInTouchMode(true);
            }
        }, 500);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back_image:
                Intent intent1 = new Intent(getApplicationContext(), VisitorLoginActivity.class);
                startActivity(intent1);
                break;
            case R.id.btnOk:
                Intent inten = new Intent(getApplicationContext(), VisitorLoginActivity.class);
                startActivity(inten);
                break;
            case R.id.btnCancel:
                Intent intent = new Intent(getApplicationContext(), VisitorLoginActivity.class);
                startActivity(intent);
                break;
            case R.id.btnNext:
                updateCheckIn();
                break;
        }
    }

    private void updateCheckIn() {

        String Emp_id = Preferences.loadStringValue(getApplicationContext(), Preferences.Emp_id, "");

        if (statusCheckIn.equalsIgnoreCase("Check-In")) {


            //device camera used
            DeviceCamera();

//            Intent intent = new Intent(getApplicationContext(), PermitCaptureImageActivity.class);
//            intent.putExtra("email", inputValue);
//            intent.putExtra("id", id);
//            intent.putExtra("emp_id", Emp_id);
//            startActivity(intent);


//            JsonObject gsonObject = new JsonObject();
//            JSONObject jsonObj_ = new JSONObject();
//            try {
//                jsonObj_.put("formtype", "checkin");
//                jsonObj_.put("email", inputValue);
//                jsonObj_.put("id", id);
//                jsonObj_.put("emp_id", Emp_id);
//                JsonParser jsonParser = new JsonParser();
//                gsonObject = (JsonObject) jsonParser.parse(jsonObj_.toString());
//                System.out.println("gsonObject::" + gsonObject);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            apiViewModel.updateworkpermita(getApplicationContext(), gsonObject);
//            progress.show();

        } else if (statusCheckIn.equalsIgnoreCase("Check-Out")) {

            //run in background
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                JsonObject gsonObject = new JsonObject();
                JSONObject jsonObj_ = new JSONObject();
                try {
                    jsonObj_.put("formtype", "checkout");
                    jsonObj_.put("email", inputValue);
                    jsonObj_.put("id", id);
                    jsonObj_.put("emp_id", Emp_id);
                    JsonParser jsonParser = new JsonParser();
                    gsonObject = (JsonObject) jsonParser.parse(jsonObj_.toString());
                    // This runs in background thread
                    apiViewModel.updateworkpermita(getApplicationContext(), gsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    // ✅ Important: Shutdown the executor
                    executor.shutdown();
                }
            });

            //progress.show();
            Intent intent = new Intent(WorkPermitActivity.this, ChekInPermitStatusActivity.class);
            intent.putExtra("status", "2");
            startActivity(intent);
        } else {

        }

    }

    //device camera
    private void DeviceCamera() {
        if (checkPermissions()) {
            openCamera();
        } else {
            requestPermissions();
        }

    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_CAMERA_PERMISSION);
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = createImageFile();
                filePath = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, filePath);
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                // Load bitmap efficiently
                InputStream inputStream = getContentResolver().openInputStream(filePath);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; // downsample 1/2
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                if (inputStream != null) inputStream.close();

                if (bitmap != null) {
                    // Optional: insert into gallery
                    String insertedUriStr = MediaStore.Images.Media.insertImage(
                            getContentResolver(),
                            bitmap,
                            "IMG_" + System.currentTimeMillis(),
                            null
                    );
                    Uri insertedUri = insertedUriStr != null ? Uri.parse(insertedUriStr) : filePath;

                    // Encode base64
                    String encodedString = encodeTobase64(bitmap, true);
                    filename = Conversions.datemillirandstring() + ".jpeg";

                    String Comp_id = Preferences.loadStringValue(WorkPermitActivity.this, Preferences.Comp_id, "");
                    String Emp_id = Preferences.loadStringValue(getApplicationContext(), Preferences.Emp_id, "");

                    startQrUpload(
                            WorkPermitActivity.this,
                            Comp_id,
                            filename,
                            encodedString,
                            inputValue,
                            id,
                            Emp_id
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getApplicationContext().getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        //image name
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        image_name = cursor.getString(nameIndex);

        int idx = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
        return cursor.getString(idx);
    }

    public static String encodeTobase64(Bitmap image, Boolean Status) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(image, image.getWidth(), image.getHeight(), true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
        Bitmap immagex = rotatedBitmap;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immagex.compress(Bitmap.CompressFormat.JPEG, 20, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b, Base64.DEFAULT);

        return imageEncoded;
    }

    //api
    public void startQrUpload(Context context, String cid, String filename, String encodedImage,
                              String email, String id, String emp_id) {
        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("cid", cid);
            jsonObj.put("key", filename);
            jsonObj.put("img", encodedImage);
            JsonObject gsonObject = JsonParser.parseString(jsonObj.toString()).getAsJsonObject();
            DataManger.getDataManager().qrindex(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    String code = response.body();
                    if ("200".equals(code)) {
                        try {
                            //run in background
                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            executor.execute(() -> {
                                try {
                                    JSONObject checkinJson = new JSONObject();
                                    JSONArray picArray = new JSONArray();
                                    picArray.put(filename);
                                    checkinJson.put("formtype", "checkin");
                                    checkinJson.put("email", email);
                                    checkinJson.put("id", id);
                                    checkinJson.put("emp_id", emp_id);
                                    checkinJson.put("live_pic", picArray);
                                    JsonObject finalJson = JsonParser.parseString(checkinJson.toString()).getAsJsonObject();
                                    ApiViewModel apiViewModel = new ApiViewModel();
                                    apiViewModel.updateworkpermita(context, finalJson);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                } finally {
                                    // ✅ Important: Shutdown the executor
                                    executor.shutdown();
                                }
                            });

                            String compId = Preferences.loadStringValue(getApplicationContext(), Preferences.Comp_id, "");
                            qrData = "workpermit###"+compId+"***"+id+"###"+email;
                            printUsb();


                        } catch (Exception e) {
                            Log.e("startQrUpload", "Check-in JSON error", e);
                        }
                    } else {
                        Log.w("startQrUpload", "qrindex failed: " + code);
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e("startQrUpload", "qrindex failed", t);
                }
            }, context, gsonObject);
        } catch (Exception e) {
            Log.e("startQrUpload", "Exception", e);
        }
    }

    private void printUsb() {
        UsbConnection usbConnection = UsbPrintersConnections.selectFirstConnected(this);
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        if (usbConnection != null && usbManager != null) {
            PendingIntent permissionIntent = PendingIntent.getBroadcast(
                    this,
                    0,
                    new Intent(ACTION_USB_PERMISSION),
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
            );
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            registerReceiver(usbReceiver, filter);

            usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);
        } else {
            Toast.makeText(this, "No USB printer connected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent1 = new Intent(getApplicationContext(), VisitorLoginActivity.class);
        startActivity(intent1);
    }


}
