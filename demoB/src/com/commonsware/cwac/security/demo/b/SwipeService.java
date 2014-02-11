/***
  Copyright (c) 2008-2014 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.
  
  From _The Busy Coder's Guide to Android Development_
    http://commonsware.com/Android
 */

package com.commonsware.cwac.security.demo.b;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SwipeService extends IntentService {
  public SwipeService() {
    super("WhyDoWeHaveToKeepProvidingThis?");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Uri content=
        Uri.parse("content://com.commonsware.cwac.security.demo.files/")
           .buildUpon().appendPath("test.pdf").build();
    ContentResolver cr=getContentResolver();

    try {
      InputStream is=cr.openInputStream(content);

      copy(is, new File(getFilesDir(), "test.pdf"));
    }
    catch (Exception e) {
      Log.e(getClass().getSimpleName(), "Problem swiping the file", e);
    }
  }

  static private void copy(InputStream in, File dst) throws IOException {
    FileOutputStream out=new FileOutputStream(dst);
    byte[] buf=new byte[1024];
    int len;

    while ((len=in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }

    in.close();
    out.close();
  }
}
