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
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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

        //credentials
        is_registered = false;



        //webview part

        webView = new WebView(MainActivity.this);

        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new WebAppInterface(MainActivity.this), "Android");
        webView.getSettings().setUserAgentString("JeParticipe-app");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.loadUrl(page);
        pile_urls.add(page);



        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view,String url){

                if ((!is_registered) && (!url.contains("/a-propos")) && (url.contains("jeparticipe.tk"))){
                    is_registered = get_creds();
                }


                if (url.contains("/login") && !last_url.contains("/home") && is_registered){
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
                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();
                    Toast.makeText(MainActivity.this, "Vous avez été déconnecté. Suppression du compte enregistré...", Toast.LENGTH_SHORT).show();
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

                if(url.contains("jeparticipe.tk") | url.contains("www.privacypolicygenerator.info")){
                    return false;
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
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Identification");
        alert.setMessage("Si vous êtes déjà enregistré sur jeparticipe.tk, entrez ici vos identifiants. Sinon, veuillez vous créer un compte depuis la page en cliquant sur Annuler (ce message va revenir juste apres).");

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
                if(!username.equals("") && !password.equals("")){
                    utils.writeToFile(username+"\n"+password,MainActivity.this,"peepee.j");
                    Toast.makeText(MainActivity.this, "Compte enregistré dans l'application !", Toast.LENGTH_SHORT).show();

                }else{
                    Toast.makeText(MainActivity.this, "Veuillez remplir la totalité des champs.", Toast.LENGTH_SHORT).show();
                    dialog.cancel();
                    ask_creds();
                }
            }
        });

        alert.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Toast.makeText(MainActivity.this, "Enregistrement du compte dans l'app annulé.", Toast.LENGTH_SHORT).show();
            }
        });

        alert.show();
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