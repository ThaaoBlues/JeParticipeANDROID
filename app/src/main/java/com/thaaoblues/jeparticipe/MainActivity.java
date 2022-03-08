package com.thaaoblues.jeparticipe;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private Utils utils = new Utils(MainActivity.this);
    private String username;
    private String password;
    private List<String> pile_urls = new ArrayList<String>();
    private String last_url = "";
    private boolean is_registered;
    private WebView webView;
    private boolean logout = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);







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




        //webview part

        webView = new WebView(MainActivity.this);

        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new WebAppInterface(MainActivity.this), "Android");
        webView.getSettings().setUserAgentString("JeParticipe-app");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.loadUrl(page);
        pile_urls.add(page);


        //credentials
        is_registered = false;




        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view,String url){


                if ((!is_registered) && (!url.contains("https://jeparticipe.tk/a-propos")) && (url.contains("jeparticipe.tk"))){
                    if(utils.is_from_oauth()){
                        is_registered = true;
                    }else{
                        is_registered = get_creds();

                    }
                }


                if (url.contains("https://jeparticipe.tk/login") && !last_url.contains("https://jeparticipe.tk/home") && is_registered){
                    //auto login part

                    if(utils.is_from_oauth()){
                        webView.loadUrl("https://jeparticipe.tk/discord_login");

                    }else{
                        webView.loadUrl("javascript:{" +
                                "document.getElementsByName('username')[0].value = '"+username+"';" +
                                "document.getElementsByName('password')[0].value = '"+password+"';" +
                                "document.forms.login.submit.click();}");
                    }

                }



                // set last_url to the top of the pile
                last_url = pile_urls.get(pile_urls.size()-1);

                // check if this isn't just a reload before adding url to pile
                if (!last_url.equals(url)){
                    pile_urls.add(url);
                }

            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView mView, String url){

                //intercept logout request before redirect
                if (url.contains("https://jeparticipe.tk/logout")){
                    File dir = getFilesDir();

                    //delete password file
                    File file = new File(dir, "peepee.j");
                    file.delete();

                    //delete oauth file
                    file = new File(dir, "oauth");
                    file.delete();

                    //disable auto oauth
                    utils.writeToFile("0",MainActivity.this,"oauth");



                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();

                    Toast.makeText(MainActivity.this, "Vous avez été déconnecté. Suppression du compte enregistré...", Toast.LENGTH_SHORT).show();

                    webView.loadUrl("https://jeparticipe.tk/a-propos");

                    is_registered = false;

                    return false;
                }

                //verify that url is still on correct domain
                String[] allowed_urls = {"jeparticipe.tk","instagram.com",".gouv.fr","twitter.com","facebook.com","reddit.com","discord.com"};

                for(int i = 0;i<allowed_urls.length;i++){
                    if(url.contains(allowed_urls[i])){
                        return false;
                    }

                }




                return true;
            }

        });


        webView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {

                String path = url;
                String filename = "";
                boolean fromb64 = false;
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
                    return;
                }

                if (url.startsWith("data:")) {  //when url is base64 encoded data


                    String[] ret = utils.createAndSaveFileFromBase64Url(url);
                    path = ret[0];
                    filename = ret[1];
                    fromb64 = true;
                    File file = new File(path);

                    Toast.makeText(MainActivity.this, "Graph enregistré dans votre gallerie ;)", Toast.LENGTH_SHORT).show();


                }else{
                    path = url;
                    filename = URLUtil.guessFileName(path, contentDisposition, mimetype);

                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(path));

                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                    Toast.makeText(getApplicationContext(), "Téléchargement de votre fichier...", //To notify the Client that the file is being downloaded
                            Toast.LENGTH_LONG).show();

                }



            }
        });

        //affiche la webview
        setContentView(webView);


    }



    public void ask_creds(){

        AlertDialog.Builder alert = new AlertDialog.Builder(this,android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);

        // set the custom layout
        final View customLayout
                = getLayoutInflater()
                .inflate(
                        R.layout.alert_register,
                        null);

        final EditText username_input = customLayout.findViewById(R.id.edit_username);

        final EditText password_input = customLayout.findViewById(R.id.edit_password);

        final Button register_button = customLayout.findViewById(R.id.register_button);
        final Button cancel_button = customLayout.findViewById(R.id.cancel_button);

        final Button discord_button = customLayout.findViewById(R.id.connect_discord_button);


        alert.setView(customLayout);
        alert.setCancelable(false);
        AlertDialog dialog = alert.create();


        register_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = username_input.getText().toString();
                password = password_input.getText().toString();
                if(!username.equals("") && !password.equals("")){
                    utils.writeToFile(username+"\n"+password,MainActivity.this,"peepee.j");
                    utils.writeToFile("0",MainActivity.this,"oauth");
                    dialog.dismiss();

                    webView.loadUrl("https://jeparticipe.tk");
                    Toast.makeText(MainActivity.this, "Compte enregistré dans l'application !", Toast.LENGTH_SHORT).show();

                }else{

                    Toast.makeText(MainActivity.this, "Veuillez remplir la totalité des champs.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(MainActivity.this, "Enregistrement du compte dans l'app annulé.", Toast.LENGTH_SHORT).show();
            }
        });

        discord_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl("https://jeparticipe.tk/discord_login");

                utils.writeToFile("1",MainActivity.this,"oauth");
                dialog.dismiss();

            }
        });
        dialog.show();
    }




    public boolean get_creds(){

        String creds = utils.readFromFile(MainActivity.this,"peepee.j");
        if(creds.contains("\n")){
            username = creds.split("\n")[1];
            password = creds.split("\n")[2];
            return true;
        }else{
            ask_creds();
            return false;
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

        @JavascriptInterface
        public void share_post(String url) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip_data = ClipData.newPlainText("JeParticipe",url);
            clipboard.setPrimaryClip(clip_data);
        }
    }


    @Override
    public void onBackPressed() {

        // charge la dernière page visitée
        webView.loadUrl(last_url);
        if(pile_urls.size() > 1){
            // dépile d'une url
            pile_urls.remove(pile_urls.size()-1);
        }else{
            // la pile ne contient qu'une page, il est donc probable que
            // l'utilisateur veuille fermer l'application.
            finish();
            System.exit(0);
        }
    }

}