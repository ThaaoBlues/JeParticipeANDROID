package com.thaaoblues.jeparticipe;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.ui.AppBarConfiguration;

import com.thaaoblues.jeparticipe.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity {

    private Utils utils = new Utils(MainActivity.this);
    private String username;
    private String password;
    private String last_url = "";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //permissions stuff
        ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission is granted. Continue the action or workflow in your
                        // app.
                    } else {
                        // Explain to the user that the feature is unavailable because the
                        // features requires a permission that the user has denied. At the
                        // same time, respect the user's decision. Don't link to system
                        // settings in an effort to convince the user to change their
                        // decision.
                    }
                });





    //deep links
        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();

        String page;
        if (data != null){
            page = data.toString();
        }else{
            page = "https://jeparticipe.tk";
        }

        //credentials
        get_creds();



        //webview part
        WebView webView = new WebView(MainActivity.this);
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(MainActivity.this), "Android");
        webView.getSettings().setUserAgentString("JeParticipe-app");


        webView.loadUrl(page);
        setContentView(webView);


        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view,String url){


                if (url.contains("/login") && !last_url.contains("/home")){
                    //auto login part

                    webView.loadUrl("javascript:{" +
                            "document.getElementsByName('username')[0].value = '"+username+"';" +
                            "document.getElementsByName('password')[0].value = '"+password+"';" +
                            "document.forms.login.submit.click();}");
                }

                if (url.contains("/login") && last_url.contains("/home")){
                    File dir = getFilesDir();
                    File file = new File(dir, "peepee.j");
                    boolean deleted = file.delete();
                    Toast.makeText(MainActivity.this, "Vous avez été déconnecté. Suppression du compte enregistré...", Toast.LENGTH_SHORT).show();
                }

                last_url = url;

            }

        });


        webView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {


                if (url.startsWith("data:")) {  //when url is base64 encoded data
                    String path = utils.createAndSaveFileFromBase64Url(url);
                    return;
                }

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), "Téléchargement de votre fichier...", //To notify the Client that the file is being downloaded
                        Toast.LENGTH_LONG).show();

            }
        });





    }



    public void ask_creds(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Identification");
        alert.setMessage("Si vous êtes déjà enregistré sur jeparticipe.tk, entrez ici vos identifiants. Sinon, veuillez vous créer un compte depuis la page en cliquant sur Annuler.");

// Set an EditText view to get user input
        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);


        final EditText username_input = new EditText(this);
        username_input.setHint("Pseudonyme");
        layout.addView(username_input);

        final EditText password_input = new EditText(this);
        password_input.setHint("Mot de passe");
        layout.addView(password_input);

        alert.setView(layout);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                username = username_input.getText().toString();
                password = password_input.getText().toString();
                utils.writeToFile(username+"\n"+password,MainActivity.this,"peepee.j");
            }
        });

        alert.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Toast.makeText(MainActivity.this, "Enregistrement du compte annulé.", Toast.LENGTH_SHORT).show();
            }
        });

        alert.show();
    }




    public void get_creds(){

        String creds = utils.readFromFile(MainActivity.this,"peepee.j");
        if(creds.contains("\n")){
            username = creds.split("\n")[1];
            password = creds.split("\n")[2];

        }else{
            ask_creds();
        }


    }


    public class WebAppInterface {

        Context mContext;

        /**
         * Instantiate the interface and set the context
         */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /**
         * Show a toast from the web page
         */
        @JavascriptInterface
        public void share_post(String url) {
            //TODO : truc de partage avec envoyer vers et copier le lien
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip_data = ClipData.newPlainText("JeParticipe",url);
            clipboard.setPrimaryClip(clip_data);
        }
    }




}