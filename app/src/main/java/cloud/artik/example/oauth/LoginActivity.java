/*
 * Copyright (C) 2017 Samsung Electronics Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cloud.artik.example.oauth;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.TokenResponse;

import java.util.HashMap;
import java.util.Map;

import static cloud.artik.example.oauth.AuthHelper.INTENT_ARTIKCLOUD_AUTHORIZATION_RESPONSE;
import static cloud.artik.example.oauth.AuthHelper.USED_INTENT;


public class LoginActivity extends AppCompatActivity {

    public static String accessToken = "";
    public static String refreshToken = "";
    public static String expiresAt = "";

    Button mButtonSignIn;

    static final String LOG_TAG = "LoginActivity";

    AuthorizationService mAuthorizationService;
    AuthStateDAL mAuthStateDAL;

    EditText editTextRedirectURI;
    EditText editTextClientID;
    EditText editTextEndpointAuth;
    EditText editTextEndpointToken;
    EditText editTextEndpointWhoami;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "Entering onCreate ...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuthorizationService = new AuthorizationService(this);
        mButtonSignIn = (Button) findViewById(R.id.btn_login);
        mButtonSignIn.setOnClickListener(new Button.OnClickListener() {

            @Override
            public void onClick(View view) {
                doAuth();
            }

        });

        mAuthStateDAL = new AuthStateDAL(this);


        editTextRedirectURI = (EditText) findViewById(R.id.editTextRedirectURI);
        editTextClientID = (EditText) findViewById(R.id.editTextClientID);
        editTextEndpointAuth = (EditText) findViewById(R.id.editTextEndpointAuth);
        editTextEndpointToken = (EditText) findViewById(R.id.editTextEndpointToken);
        editTextEndpointWhoami = (EditText) findViewById(R.id.editTextEndpointWhoami);

        editTextRedirectURI.setText( Config.REDIRECT_URI );
        editTextClientID.setText( Config.CLIENT_ID );
        editTextEndpointAuth.setText( AuthHelper.ARTIKCLOUD_AUTHORIZE_URI );
        editTextEndpointToken.setText( AuthHelper.ARTIKCLOUD_TOKEN_URI );
        editTextEndpointWhoami.setText( AuthHelper.ENDPOINT_WHOAMI );

        Button mButtonSaveSetting = (Button) findViewById(R.id.btn_savesetting);
        mButtonSaveSetting.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {

                Config.REDIRECT_URI = editTextRedirectURI.getText().toString();
                Config.CLIENT_ID = editTextClientID.getText().toString();
                AuthHelper.ARTIKCLOUD_AUTHORIZE_URI = editTextEndpointAuth.getText().toString();
                AuthHelper.ARTIKCLOUD_TOKEN_URI = editTextEndpointToken.getText().toString();
                AuthHelper.ENDPOINT_WHOAMI = editTextEndpointWhoami.getText().toString();
            }
        });
    }

    // File OAuth call with Authorization Code method
    // https://developer.artik.cloud/documentation/getting-started/authentication.html#authorization-code-method
    private void doAuth() {
        AuthorizationRequest authorizationRequest = AuthHelper.createAuthorizationRequest();

        PendingIntent authorizationIntent = PendingIntent.getActivity(
                this,
                authorizationRequest.hashCode(),
                new Intent(INTENT_ARTIKCLOUD_AUTHORIZATION_RESPONSE, null, this, LoginActivity.class),
                0);

        /* request sample with custom tabs */
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();

        mAuthorizationService.performAuthorizationRequest(authorizationRequest, authorizationIntent, customTabsIntent);

    }

    @Override
    protected void onStart() {
        Log.d(LOG_TAG, "Entering onStart ...");
        super.onStart();
        checkIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        checkIntent(intent);
    }

    private void checkIntent(@Nullable Intent intent) {

        Log.d(LOG_TAG, "Entering checkIntent ...");
        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case INTENT_ARTIKCLOUD_AUTHORIZATION_RESPONSE:
                    Log.d(LOG_TAG, "checkIntent action = " + action
                            + " intent.hasExtra(USED_INTENT) = " + intent.hasExtra(USED_INTENT));
                    if (!intent.hasExtra(USED_INTENT)) {
                        handleAuthorizationResponse(intent);
                        intent.putExtra(USED_INTENT, true);
                    }
                    break;
                default:
                    Log.w(LOG_TAG, "checkIntent action = " + action);
                    // do nothing
            }
        } else {
            Log.w(LOG_TAG, "checkIntent intent is null!");
        }
    }

    private void handleAuthorizationResponse(@NonNull Intent intent) {

        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        AuthorizationException error = AuthorizationException.fromIntent(intent);
        Log.i(LOG_TAG, "Entering handleAuthorizationResponse with response from Intent = " + response.jsonSerialize().toString());
        Toast.makeText(this, response.jsonSerialize().toString(), Toast.LENGTH_LONG).show();
        Toast.makeText(this, "AUTH CODE:"+response.authorizationCode.toString(), Toast.LENGTH_LONG).show();

        if (response != null) {

            if (response.authorizationCode != null ) { // Authorization Code method: succeeded to get code

                final AuthState authState = new AuthState(response, error);
                Log.i(LOG_TAG, "Received code = " + response.authorizationCode + "\n make another call to get token ...");

                try {
                    // File 2nd call in Authorization Code method to get the token
                    mAuthorizationService.performTokenRequest(response.createTokenExchangeRequest(), new AuthorizationService.TokenResponseCallback() {
                        @Override
                        public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                            //if(exception == null) {
                            //    //Toast.makeText(this, "Error: "+exception.getMessage()+" \r\n\r\n Maybe the server doesn't use https?", Toast.LENGTH_LONG).show();
                            //    return;
                            //}
                            if (tokenResponse != null) {
                                authState.update(tokenResponse, exception);
                                mAuthStateDAL.writeAuthState(authState); //store into persistent storage for use later
                                String text = String.format("Received token response [%s]", tokenResponse.jsonSerializeString());
                                Log.i(LOG_TAG, text);
                                accessToken = tokenResponse.accessToken;
                                expiresAt = tokenResponse.accessTokenExpirationTime.toString();
                                refreshToken = tokenResponse.refreshToken;
                                showAuthInfo();

                                // make api call to "whoami"
                                RequestQueue queue = Volley.newRequestQueue(getBaseContext());
                                StringRequest stringRequest = new StringRequest(Request.Method.GET, AuthHelper.ENDPOINT_WHOAMI,
                                        new Response.Listener<String>() {
                                            @Override
                                            public void onResponse(String response) {
                                                // Display response string.
                                                Log.i(LOG_TAG, "whoami API response: "+response);
                                                Toast.makeText(getBaseContext(), "LOGIN SUCCESS, this is your identity: "+response.toString(), Toast.LENGTH_LONG).show();
                                                /*
                                                AlertDialog.Builder builder = new AlertDialog.Builder(getBaseContext());
                                                builder.setMessage("API whoami response: "+response).setCancelable(false)
                                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                        }
                                                    }).show();
                                                */
                                            }
                                        }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        Log.i(LOG_TAG, "whoami API failed: "+error.toString());
                                        Toast.makeText(getBaseContext(), error.toString(), Toast.LENGTH_LONG).show();
                                    }
                                })
                                {
                                    @Override
                                    public Map<String, String> getHeaders() throws AuthFailureError { // custom HTTP header
                                        Map<String, String>  params = new HashMap<String, String>();
                                        params.put("Authorization", "Bearer "+accessToken);
                                        return params;
                                    }
                                }
                                ;
                                // Add the request to the RequestQueue.
                                queue.add(stringRequest);

                            } else {
                                Context context = getApplicationContext();
                                Log.w(LOG_TAG, "Token Exchange failed", exception);
                                CharSequence text = "Token Exchange failed";
                                int duration = Toast.LENGTH_LONG;
                                Toast toast = Toast.makeText(context, text, duration);
                                toast.show();
                            }
                        }
                    });
                } catch (Exception ex) {
                    Toast.makeText(this, "Error: "+ex.getMessage()+" \r\n\r\n Maybe the server doesn't use https?", Toast.LENGTH_LONG).show();
                }
            } else { // come here w/o authorization code. For example, signup finish and user clicks "Back to login"
                Log.d(LOG_TAG, "additionalParameter = " + response.additionalParameters.toString());

                if (response.additionalParameters.get("status").equalsIgnoreCase("login_request")) {
                    // ARTIK Cloud instructs the app to display a sign-in form
                    doAuth();
                } else {
                    Log.d(LOG_TAG, response.jsonSerialize().toString());
                }
            }

        } else {
            Log.w(LOG_TAG, "Authorization Response is null ");
            Log.d(LOG_TAG, "Authorization Exception = " + error);
        }
    }

    public void showAuthInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("accessToken = " + accessToken + "\n" + "refreshToken = " + refreshToken + "\n" + "expiresAt = " + expiresAt + "\n").setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                }).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAuthorizationService.dispose();
    }
}


