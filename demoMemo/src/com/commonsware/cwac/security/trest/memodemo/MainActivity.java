/***
  Copyright (c) 2014 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.commonsware.cwac.security.trest.memodemo;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import com.commonsware.cwac.security.trust.CertificateNotMemorizedException;
import com.commonsware.cwac.security.trust.MemorizingTrustManager;
import com.commonsware.cwac.security.trust.TrustManagerBuilder;

public class MainActivity extends Activity {
  private EditText host=null;
  private TextView transcript=null;
  private ScrollView scroll=null;
  private TrustManagerBuilder builder=null;
  private TrustManager[] managers=null;
  private MenuItem tofu=null;
  private MemorizingTrustManager.Options options=null;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);

    setContentView(R.layout.activity_main);
    host=(EditText)findViewById(R.id.host);
    scroll=(ScrollView)findViewById(R.id.scroll);
    transcript=(TextView)scroll.findViewById(R.id.transcript);

    options=
        new MemorizingTrustManager.Options(MainActivity.this,
                                           "memorize",
                                           "snicklefritz");

    try {
      builder=
          new TrustManagerBuilder(MainActivity.this).useDefault()
                                                    .or()
                                                    .selfSigned(R.raw.selfsigned,
                                                                "foobar".toCharArray())
                                                    .or()
                                                    .selfSigned(R.raw.selfsigned2,
                                                                "foobar2".toCharArray())
                                                    .and()
                                                    .memorize(options);
      managers=builder.buildArray();
    }
    catch (Exception e) {
      logToTranscript("Exception!");
      Log.e(getClass().getSimpleName(),
            "Exception setting up TrustManagerBuilder", e);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);

    tofu=menu.findItem(R.id.tofu);

    return(super.onCreateOptionsMenu(menu));
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.tofu:
        tofu.setChecked(!tofu.isChecked());
        options.trustOnFirstUse(tofu.isChecked());
        break;

      case R.id.test:
        test();
        break;

      case R.id.clear:
        clear(false);
        break;

      case R.id.clearAll:
        clear(true);
        break;
    }

    return(super.onOptionsItemSelected(item));
  }

  void acceptAndSave(X509Certificate[] chain) {
    try {
      builder.memorizeCert(chain);
      test();
    }
    catch (Exception e) {
      logToTranscript("Exception!");
      Log.e(getClass().getSimpleName(), "Exception memorizing cert", e);
    }
  }

  void allowOnce(X509Certificate[] chain) {
    try {
      builder.allowCertOnce(chain);
      test();
    }
    catch (Exception e) {
      logToTranscript("Exception!");
      Log.e(getClass().getSimpleName(), "Exception memorizing cert", e);
    }
  }

  private void test() {
    new TestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private void clear(boolean clearAll) {
    new ClearTask(clearAll).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private void logToTranscript(String msg) {
    transcript.setText(transcript.getText().toString() + msg + "\n");
    scroll.fullScroll(View.FOCUS_DOWN);
  }

  private class TestTask extends AsyncTask<Void, Void, Void> {
    Exception e=null;

    @Override
    protected Void doInBackground(Void... params) {
      try {
        testHURL(managers, host.getText().toString());
      }
      catch (Exception e) {
        this.e=e;
      }

      return(null);
    }

    @Override
    public void onPostExecute(Void param) {
      if (e != null) {
        if (e instanceof SSLHandshakeException
            && e.getCause() instanceof CertificateNotMemorizedException) {
          logToTranscript("Certificate not memorized!");

          CertificateNotMemorizedException cnme=
              (CertificateNotMemorizedException)e.getCause();

          new SampleDialogFragment().certificateChain(cnme.getCertificateChain())
                                    .show(getFragmentManager(), "cert");
        }
        else {
          logToTranscript("Exception!");
          Log.e(getClass().getSimpleName(), "Exception running test", e);
        }
      }
      else {
        logToTranscript("Success!");
      }
    }

    private void testHURL(TrustManager[] managers, String url)
                                                              throws Exception {
      SSLContext ssl=buildSslContext(managers);

      HttpsURLConnection conn=
          (HttpsURLConnection)new URL(url).openConnection();

      conn.setSSLSocketFactory(ssl.getSocketFactory());

      InputStream in=conn.getInputStream();

      in.close();
    }

    private SSLContext buildSslContext(TrustManager[] managers)
                                                               throws NoSuchAlgorithmException,
                                                               KeyManagementException {
      SSLContext ssl=SSLContext.getInstance("TLS");

      ssl.init(null, managers, null);

      return(ssl);
    }
  }

  private class ClearTask extends AsyncTask<Void, Void, Void> {
    boolean clearAll=false;
    Exception e=null;

    ClearTask(boolean clearAll) {
      this.clearAll=clearAll;
    }

    @Override
    protected Void doInBackground(Void... params) {
      try {
        builder.clearMemorizedCerts(clearAll);
      }
      catch (Exception e) {
        this.e=e;
      }

      return(null);
    }

    @Override
    public void onPostExecute(Void param) {
      if (e != null) {
        logToTranscript("Exception!");
        Log.e(getClass().getSimpleName(), "Exception running test", e);
      }
      else {
        logToTranscript("Cleared!");
      }
    }
  }
}
