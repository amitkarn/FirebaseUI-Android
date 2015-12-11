package com.firebase.ui.auth.core;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.ui.R;
import com.firebase.ui.auth.facebook.FacebookAuthProvider;
import com.firebase.ui.auth.google.GoogleAuthProvider;
import com.firebase.ui.auth.password.PasswordAuthProvider;
import com.firebase.ui.auth.twitter.TwitterAuthProvider;

import java.util.HashMap;
import java.util.Map;

public class FirebaseLoginDialog extends DialogFragment {

    Map<SocialProvider, FirebaseAuthProvider> mEnabledProvidersByType = new HashMap<>();
    TokenAuthHandler mHandler;
    SocialProvider mActiveProvider;
    Firebase mRef;
    Context mContext;
    View mView;

    /*
    We need to be extra aggressive about building / destroying mGoogleauthProviders so we don't
    end up with two clients connected at the same time.
     */

    public void onStop() {
        super.onStop();
        cleanUp();
    }

    public void onDestroy() {
        super.onDestroy();
        cleanUp();
    }

    @Override
    public void onPause() {
        super.onPause();
       cleanUp();
    }

    public void cleanUp() {
        if (getGoogleAuthProvider() != null) getGoogleAuthProvider().cleanUp();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (getFacebookAuthProvider() != null) {
            getFacebookAuthProvider().mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }

        if (getTwitterAuthProvider() != null) {
            getTwitterAuthProvider().onActivityResult(requestCode, resultCode, data);
        }

        if (getGoogleAuthProvider() != null) {
            getGoogleAuthProvider().onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        mView = inflater.inflate(R.layout.fragment_firebase_login, null);

        for (SocialProvider providerType : SocialProvider.values()) {
            if (mEnabledProvidersByType.keySet().contains(providerType)) {
                showLoginOption(mEnabledProvidersByType.get(providerType), providerType.getButtonId());
            }
            else {
                mView.findViewById(providerType.getButtonId()).setVisibility(View.GONE);;
            }
        }

        if (mEnabledProvidersByType.containsKey(SocialProvider.PASSWORD) &&
           !(mEnabledProvidersByType.containsKey(SocialProvider.FACEBOOK) || mEnabledProvidersByType.containsKey(SocialProvider.GOOGLE) || mEnabledProvidersByType.containsKey(SocialProvider.TWITTER))) {
            mView.findViewById(R.id.or_section).setVisibility(View.GONE);
        }

        mView.findViewById(R.id.loading_section).setVisibility(View.GONE);

        builder.setView(mView);

        return builder.create();
    }

    public FirebaseLoginDialog setRef(Firebase ref) {
        mRef = ref;
        return this;
    }

    public FirebaseLoginDialog setContext(Context context) {
        mContext = context;
        return this;
    }

    public void reset() {
        mView.findViewById(R.id.login_section).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.loading_section).setVisibility(View.GONE);
    }

    public void logout() {
        for (FirebaseAuthProvider provider : mEnabledProvidersByType.values()) {
            provider.logout();
        }
        mRef.unauth();
    }

    public FirebaseLoginDialog setHandler(final TokenAuthHandler handler) {
        mHandler = new TokenAuthHandler() {
            @Override
            public void onSuccess(AuthData auth) {
                dismiss();
                handler.onSuccess(auth);
            }

            @Override
            public void onUserError(FirebaseLoginError err) {
                handler.onUserError(err);
            }

            @Override
            public void onProviderError(FirebaseLoginError err) {
                handler.onProviderError(err);
            }
        };
        return this;
    }

    public FirebaseLoginDialog setEnabledProvider(SocialProvider provider) {
        if (!mEnabledProvidersByType.containsKey(provider)) {
            mEnabledProvidersByType.put(provider, provider.createProvider(mContext, mRef, mHandler));
        }
        return this;
    }

    private void showLoginOption(final FirebaseAuthProvider helper, int id) {
        mView.findViewById(id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SocialProvider.getTypeForProvider(helper) == SocialProvider.PASSWORD) {
                    EditText emailText = (EditText) mView.findViewById(R.id.email);
                    EditText passwordText = (EditText) mView.findViewById(R.id.password);
                    helper.login(emailText.getText().toString(), passwordText.getText().toString());

                    passwordText.setText("");
                } else {
                    helper.login();
                }
                mActiveProvider = helper.getProviderType();
                mView.findViewById(R.id.login_section).setVisibility(View.GONE);
                mView.findViewById(R.id.loading_section).setVisibility(View.VISIBLE);
            }
        });
    }

    public FacebookAuthProvider getFacebookAuthProvider() {
        return (FacebookAuthProvider) mEnabledProvidersByType.get(SocialProvider.FACEBOOK);
    }

    public TwitterAuthProvider getTwitterAuthProvider() {
        return (TwitterAuthProvider) mEnabledProvidersByType.get(SocialProvider.TWITTER);
    }

    public GoogleAuthProvider getGoogleAuthProvider() {
        return (GoogleAuthProvider) mEnabledProvidersByType.get(SocialProvider.GOOGLE);
    }

    public PasswordAuthProvider getPasswordAuthProvider() {
        return (PasswordAuthProvider) mEnabledProvidersByType.get(SocialProvider.PASSWORD);
    }
}