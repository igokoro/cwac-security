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

package com.commonsware.cwac.security.demo.a;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.commonsware.cwac.security.PermissionLint;
import com.commonsware.cwac.security.PermissionUtils;

public class FilesCPDemo extends Activity {
  private static final String PREFS_FIRST_RUN="firstRun";

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    HashMap<PackageInfo, ArrayList<PermissionLint>> evildoers=
        PermissionUtils.checkCustomPermissions(this);

    if (evildoers.size() == 0 || !isFirstRun()) {
      Intent i=
          new Intent(Intent.ACTION_VIEW,
                     Uri.parse(FileProvider.CONTENT_URI + "test.pdf"));

      i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      safelyStartActivity(i);
    }
    else {
      for (Map.Entry<PackageInfo, ArrayList<PermissionLint>> entry : evildoers.entrySet()) {
        Log.e("SecurityDemoA", "This app holds the permission: "
            + entry.getKey().packageName);

        for (PermissionLint lint : entry.getValue()) {
          if (lint.wasUpgraded) {
            Log.e("SecurityDemoA",
                  "...and they upgraded the protection level");
          }
          else if (lint.wasDowngraded) {
            Log.e("SecurityDemoA",
                  "...and they downgraded the protection level");
          }

          if (lint.proseDiffers) {
            Log.e("SecurityDemoA",
                  "...and they altered the label or description");
          }
        }
      }

      Toast.makeText(this, R.string.evil, Toast.LENGTH_LONG).show();
    }

    finish();
  }

  private boolean isFirstRun() {
    boolean result=false;

    SharedPreferences prefs=
        PreferenceManager.getDefaultSharedPreferences(this);

    result=prefs.getBoolean(PREFS_FIRST_RUN, true);

    prefs.edit().putBoolean(PREFS_FIRST_RUN, false).apply();

    return(result);
  }

  private void safelyStartActivity(Intent i) {
    PackageManager mgr=getPackageManager();

    if (mgr.queryIntentActivities(i, 0).size() == 0) {
      Toast.makeText(this, R.string.no_activity, Toast.LENGTH_LONG)
           .show();
    }
    else {
      startActivity(i);
    }
  }
}