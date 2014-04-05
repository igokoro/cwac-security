/***
  Copyright (c) 2012-2014 CommonsWare, LLC
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

package com.commonsware.cwac.security.trest.memodemo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import java.security.cert.X509Certificate;

public class SampleDialogFragment extends DialogFragment implements
    DialogInterface.OnClickListener {
  private X509Certificate[] chain=null;
  
  SampleDialogFragment certificateChain(X509Certificate[] chain) {
    this.chain=chain;
    
    return(this);
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());

    return(builder.setTitle(R.string.ssl_certificate_memorization)
                  .setMessage(R.string.cert_not_recognized)
                  .setPositiveButton(R.string.accept_and_save, this)
                  .setNegativeButton(android.R.string.cancel, null)
                  .setNeutralButton(R.string.accept_once, this).create());
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (which==DialogInterface.BUTTON_POSITIVE) {
      ((MainActivity)getActivity()).acceptAndSave(chain);
    }
    else if (which==DialogInterface.BUTTON_NEUTRAL) {
      ((MainActivity)getActivity()).allowOnce(chain);
    }
  }

  @Override
  public void onDismiss(DialogInterface unused) {
    super.onDismiss(unused);
  }

  @Override
  public void onCancel(DialogInterface unused) {
    super.onCancel(unused);
  }
}
