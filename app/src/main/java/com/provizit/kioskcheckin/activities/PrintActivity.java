package com.provizit.kioskcheckin.activities;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.dantsu.escposprinter.EscPosPrinter;
import com.dantsu.escposprinter.connection.usb.UsbConnection;
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections;
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException;
import com.dantsu.escposprinter.exceptions.EscPosConnectionException;
import com.dantsu.escposprinter.exceptions.EscPosEncodingException;
import com.dantsu.escposprinter.exceptions.EscPosParserException;
import com.dantsu.escposprinter.textparser.PrinterTextParserImg;
import com.provizit.kioskcheckin.R;

public class PrintActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.provizit.kioskcheckin.USB_PERMISSION";

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
                                                "[C]<font size='small'>Name: Sunil\nBadge Number: 690</font>\n" +
                                                "[C]\n" +
                                                "[C]<qrcode size='14'>workpermit###ftprovizitstc***68fb53ef8f3afc401cc0bfe1###visitorf.t03@gmail.com</qrcode>\n"
                                );

                                Toast.makeText(context, "Print successful!", Toast.LENGTH_SHORT).show();

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


    private Button printButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);



        printButton = findViewById(R.id.printButton);


        printButton.setOnClickListener(v -> {
            printUsb();
        });
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

}