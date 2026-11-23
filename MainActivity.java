package com.mixezy.mobile;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "mixezy_mobile";
    private static final String KEY_SERVER = "server_url";
    private static final int FILE_CHOOSER_REQUEST = 4107;

    private LinearLayout root;
    private WebView webView;
    private FrameLayout webFrame;
    private TextView statusText;
    private ProgressBar progressBar;
    private ValueCallback<Uri[]> filePathCallback;
    private String serverUrl = "";
    private String serverOrigin = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(18, 17, 28));
        window.setNavigationBarColor(Color.rgb(18, 17, 28));

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String saved = prefs.getString(KEY_SERVER, "");
        if (saved == null || saved.trim().isEmpty()) saved = BuildConfig.DEFAULT_SERVER_URL;
        String normalized = normalizeServerUrl(saved);
        if (normalized != null) {
            serverUrl = normalized;
            serverOrigin = originOf(serverUrl);
            prefs.edit().putString(KEY_SERVER, serverUrl).apply();
            showWorkspace(savedInstanceState);
        } else {
            showSetup(null);
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setPadding(dp(18), dp(13), dp(18), dp(13));
        button.setBackgroundColor(Color.rgb(130, 96, 238));
        return button;
    }

    private void showSetup(String previous) {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        root.setBackgroundColor(Color.rgb(18, 17, 28));

        TextView logo = text("Mixezy Mobile", 29, Color.WHITE, true);
        TextView subtitle = text("AI CREATIVE SUITE", 11, Color.rgb(172, 148, 255), true);
        subtitle.setLetterSpacing(0.18f);
        TextView heading = text("Connectez votre espace", 23, Color.WHITE, true);
        TextView description = text(
                "Saisissez l’adresse HTTPS de votre plateforme Mixezy Mobile hébergée. " +
                "L’application Android se connectera à votre compte et à vos créations.",
                15, Color.rgb(190, 185, 207), false);
        description.setLineSpacing(dp(4), 1f);

        EditText serverInput = new EditText(this);
        serverInput.setSingleLine(true);
        serverInput.setTextSize(15);
        serverInput.setTextColor(Color.WHITE);
        serverInput.setHintTextColor(Color.rgb(145, 139, 165));
        serverInput.setHint("https://app.votre-domaine.com");
        serverInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        serverInput.setPadding(dp(14), dp(13), dp(14), dp(13));
        serverInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(155, 123, 255)));
        if (previous != null && !previous.trim().isEmpty()) serverInput.setText(previous);
        else if (BuildConfig.DEFAULT_SERVER_URL != null && !BuildConfig.DEFAULT_SERVER_URL.isEmpty()) serverInput.setText(BuildConfig.DEFAULT_SERVER_URL);

        TextView secureNote = text("Connexion sécurisée HTTPS uniquement.", 12, Color.rgb(151, 145, 172), false);
        TextView error = text("", 13, Color.rgb(255, 135, 153), false);

        Button connect = button("Connecter mon espace");
        connect.setOnClickListener(v -> {
            String normalized = normalizeServerUrl(serverInput.getText().toString());
            if (normalized == null) {
                error.setText("Adresse invalide. Entrez une URL HTTPS publique, par exemple https://app.exemple.com");
                return;
            }
            serverUrl = normalized;
            serverOrigin = originOf(serverUrl);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_SERVER, serverUrl).apply();
            showWorkspace(null);
        });

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(24), dp(22), dp(22));
        card.setBackgroundColor(Color.rgb(28, 26, 43));
        card.addView(logo, matchWrap());
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.topMargin = dp(3);
        card.addView(subtitle, subtitleParams);
        LinearLayout.LayoutParams headingParams = matchWrap();
        headingParams.topMargin = dp(28);
        card.addView(heading, headingParams);
        LinearLayout.LayoutParams descParams = matchWrap();
        descParams.topMargin = dp(9);
        card.addView(description, descParams);
        LinearLayout.LayoutParams inputParams = matchWrap();
        inputParams.topMargin = dp(22);
        card.addView(serverInput, inputParams);
        LinearLayout.LayoutParams noteParams = matchWrap();
        noteParams.topMargin = dp(8);
        card.addView(secureNote, noteParams);
        LinearLayout.LayoutParams errorParams = matchWrap();
        errorParams.topMargin = dp(8);
        card.addView(error, errorParams);
        LinearLayout.LayoutParams connectParams = matchWrap();
        connectParams.topMargin = dp(18);
        card.addView(connect, connectParams);
        root.addView(card, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView footer = text("Mixezy Mobile · Votre studio créatif, partout avec vous", 11, Color.rgb(127, 121, 148), false);
        LinearLayout.LayoutParams footerParams = matchWrap();
        footerParams.topMargin = dp(20);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, footerParams);
        setContentView(root);
        applySystemInsets(root);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private String normalizeServerUrl(String candidate) {
        if (candidate == null) return null;
        String value = candidate.trim();
        if (value.isEmpty()) return null;
        if (!value.toLowerCase(Locale.ROOT).startsWith("https://")) return null;
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) return null;
            if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) return null;
            String path = uri.getPath();
            if (path != null && !path.isEmpty() && !"/".equals(path)) return null;
            int port = uri.getPort();
            if (port != -1 && (port < 1 || port > 65535)) return null;
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            return "https://" + host + (port == -1 || port == 443 ? "" : ":" + port);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String originOf(String url) {
        Uri uri = Uri.parse(url);
        int port = uri.getPort() == -1 ? 443 : uri.getPort();
        return "https://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port;
    }

    private boolean isTrustedServerUri(Uri uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) return false;
        int port = uri.getPort() == -1 ? 443 : uri.getPort();
        String candidate = "https://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port;
        return candidate.equals(serverOrigin);
    }

    private void showWorkspace(Bundle savedInstanceState) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18, 17, 28));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(16), 0, dp(8), 0);
        toolbar.setBackgroundColor(Color.rgb(23, 21, 36));
        TextView title = text("Mixezy Mobile", 17, Color.WHITE, true);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1));
        Button refresh = new Button(this);
        refresh.setText("↻");
        refresh.setTextSize(21);
        refresh.setTextColor(Color.WHITE);
        refresh.setBackgroundColor(Color.TRANSPARENT);
        refresh.setOnClickListener(v -> { if (webView != null) webView.reload(); });
        toolbar.addView(refresh, new LinearLayout.LayoutParams(dp(48), dp(52)));
        Button more = new Button(this);
        more.setText("⋮");
        more.setTextSize(21);
        more.setTextColor(Color.WHITE);
        more.setBackgroundColor(Color.TRANSPARENT);
        more.setOnClickListener(this::showMenu);
        toolbar.addView(more, new LinearLayout.LayoutParams(dp(48), dp(52)));
        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        webFrame = new FrameLayout(this);
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(18, 17, 28));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (android.os.Build.VERSION.SDK_INT >= 26) settings.setSafeBrowsingEnabled(true);
        settings.setGeolocationEnabled(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setUserAgentString(settings.getUserAgentString() + " MixezyMobile/0.9.3");
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
                if (isTrustedServerUri(uri)) return false;
                if ("https".equals(scheme) || "mailto".equals(scheme) || "tel".equals(scheme)) {
                    openExternal(uri);
                    return true;
                }
                Toast.makeText(MainActivity.this, "Lien bloqué pour protéger votre session.", Toast.LENGTH_LONG).show();
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (statusText != null) statusText.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame() && statusText != null) {
                    statusText.setText("Connexion impossible. Vérifiez votre réseau ou l’adresse de votre plateforme, puis actualisez.");
                    statusText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                if (statusText != null) {
                    statusText.setText("Certificat HTTPS invalide. Pour protéger votre compte, cette connexion a été bloquée.");
                    statusText.setVisibility(View.VISIBLE);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (progressBar != null) {
                    progressBar.setProgress(newProgress);
                    progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                try {
                    Intent intent = params.createIntent();
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException ex) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "Aucun sélecteur de fichiers disponible.", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                try {
                    Uri uri = Uri.parse(url);
                    if (!isTrustedServerUri(uri)) {
                        Toast.makeText(MainActivity.this, "Téléchargement bloqué : seuls les fichiers du serveur Mixezy Mobile sont autorisés.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setMimeType(mimetype);
                    String cookies = CookieManager.getInstance().getCookie(url);
                    if (cookies != null) request.addRequestHeader("Cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setTitle("Téléchargement Mixezy Mobile");
                    request.setDescription("Téléchargement depuis Mixezy Mobile");
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadName(contentDisposition, uri));
                    DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    manager.enqueue(request);
                    Toast.makeText(MainActivity.this, "Téléchargement lancé.", Toast.LENGTH_SHORT).show();
                } catch (Exception ex) {
                    Toast.makeText(MainActivity.this, "Téléchargement impossible : " + ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(155, 123, 255)));
        webFrame.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3), Gravity.TOP);
        webFrame.addView(progressBar, progressParams);
        statusText = text("Connexion à Mixezy Mobile…", 14, Color.WHITE, false);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(dp(28), dp(24), dp(28), dp(24));
        statusText.setBackgroundColor(Color.rgb(18, 17, 28));
        statusText.setVisibility(View.GONE);
        webFrame.addView(statusText, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(webFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        applySystemInsets(root);

        if (savedInstanceState != null && webView.restoreState(savedInstanceState) != null) {
            // The restored WebView retains its current page and navigation history.
        } else {
            webView.loadUrl(serverUrl);
        }
    }

    private void applySystemInsets(View target) {
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            final int left = target.getPaddingLeft();
            final int top = target.getPaddingTop();
            final int right = target.getPaddingRight();
            final int bottom = target.getPaddingBottom();
            target.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
                return insets;
            });
            target.requestApplyInsets();
        }
    }

    private String downloadName(String disposition, Uri uri) {
        String name = null;
        if (disposition != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("filename\\*=UTF-8''([^;]+)|filename=\"?([^\";]+)", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(disposition);
            if (matcher.find()) name = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }
        if (name == null || name.trim().isEmpty()) name = uri.getLastPathSegment();
        if (name == null || name.trim().isEmpty()) name = "mixezy-download";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void showMenu(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add("Actualiser").setOnMenuItemClickListener(item -> { if (webView != null) webView.reload(); return true; });
        menu.getMenu().add("Changer de serveur").setOnMenuItemClickListener(item -> { showSetup(serverUrl); return true; });
        menu.getMenu().add("Ouvrir dans le navigateur").setOnMenuItemClickListener(item -> { openExternal(Uri.parse(serverUrl)); return true; });
        menu.getMenu().add("À propos de Mixezy Mobile").setOnMenuItemClickListener(item -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Mixezy Mobile · Android")
                    .setMessage("Application Android officielle de votre espace Mixezy Mobile.\n\nLe traitement IA, les comptes et les paiements restent gérés par le serveur SaaS sécurisé.")
                    .setPositiveButton("OK", null).show();
            return true;
        });
        menu.show();
    }

    private void openExternal(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, "Aucune application ne peut ouvrir ce lien.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
