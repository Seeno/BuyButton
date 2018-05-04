package com.jimmytangdev.buybutton;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FunctionCallback;
import com.parse.Parse;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardInputWidget;

import java.util.HashMap;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    final Context context = this;
    public static final String PUBLISHABLE_KEY = "pk_test_1Tqu2RRf2owV3RCWuHV0whMY";
    public static final String APPLICATION_ID = "ZQ2s6PSYUWgl2pKBwnaIYhpYoFTVCAGLRdULPM3D";
    public static final String CLIENT_KEY = "Mc74TIBIS8LMrQednviZhXe5Y3A8A8ScppcNiI01";
    public static final String BACK4PAPP_API = "https://parseapi.back4app.com/";
    private Card card;
    private ProgressDialog progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(APPLICATION_ID)
                .clientKey(CLIENT_KEY)
                .server(BACK4PAPP_API).build());
        Parse.setLogLevel(Parse.LOG_LEVEL_VERBOSE);

        progress = new ProgressDialog(this);


        TextView seeMore = findViewById(R.id.see_more);
        Button buyTicket = findViewById(R.id.buy_tickets);
        TextView titleTextView = findViewById(R.id.idea_detail_title);
        final String title = titleTextView.getText().toString();

        seeMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(context);
                }
                builder.setTitle("Locked");
                builder.setMessage("You reeeeaaaallly don't want to see more.");
                builder.setNeutralButton("Gotcha", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });
                builder.show();
            }
        });

        final LayoutInflater inflater = this.getLayoutInflater();

        buyTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int ticketPriceNum = 70;
                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
                } else {
                    builder = new AlertDialog.Builder(context);
                }
                builder.setTitle("Buy Tickets");
                builder.setMessage("Purchase tickets for " + title + " at $" + ticketPriceNum + "?");
                builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                        View dialogView = inflater.inflate(R.layout.card_widget, null);
                        dialogBuilder.setView(dialogView);
                        final AlertDialog alertDialog = dialogBuilder.create();

                        final CardInputWidget mCardInputWidget = (CardInputWidget) dialogView.findViewById(R.id.card_input_widget);
                        Button defaultCC = (Button) dialogView.findViewById(R.id.default_cc);
                        Button next = (Button) dialogView.findViewById(R.id.next);
                        final TextView error = (TextView) dialogView.findViewById(R.id.errorMessage);

                        defaultCC.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mCardInputWidget.setCardNumber("4242424242424242");
                                mCardInputWidget.setCvcCode("123");
                                mCardInputWidget.setExpiryDate(12,2020);
                            }
                        });

                        next.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                error.setVisibility(View.GONE);
                                if (mCardInputWidget.getCard() == null)
                                {
                                    error.setText("Invalid Card Information");
                                    error.setVisibility(View.VISIBLE);
                                }
                                else
                                {
                                    card = new Card(
                                            mCardInputWidget.getCard().getNumber(), //card number
                                            mCardInputWidget.getCard().getExpMonth(),
                                            mCardInputWidget.getCard().getExpYear(),
                                            mCardInputWidget.getCard().getCVC()
                                    );
                                    buy(card);
                                    alertDialog.cancel();
                                }
                            }
                        });
                        alertDialog.show();

                        // insert stripe related properties here
                    }
                });
                builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                });
                builder.show();

            }
        });


    }

    private void buy(Card card) {
        boolean validation = card.validateCard();
        if (validation) {
            startProgress("Validating Credit Card");
            new Stripe(this).createToken(
                    card,
                    PUBLISHABLE_KEY,
                    new TokenCallback() {
                        @Override
                        public void onError(Exception error) {
                            Log.d("Stripe", error.toString());
                        }

                        @Override
                        public void onSuccess(Token token) {
                            finishProgress();
                            charge(token);
                        }
                    });
        } else if (!card.validateNumber()) {
            Log.d("Stripe", "The card number that you entered is invalid");
        } else if (!card.validateExpiryDate()) {
            Log.d("Stripe", "The expiration date that you entered is invalid");
        } else if (!card.validateCVC()) {
            Log.d("Stripe", "The CVC code that you entered is invalid");
        } else {
            Log.d("Stripe", "The card details that you entered are invalid");
        }
    }

    private void charge(Token cardToken) {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("itemName", "NakedYoga"); //This part you need to pass your item detail, implement yourself
        params.put("cardToken", cardToken.getId());
        params.put("name", "Your Name");
        params.put("email", "youremail@email.com");
        params.put("address", "HIHI");
        params.put("zip", "99999");
        params.put("city_state", "CA");
        startProgress("Purchasing Item");
        ParseCloud.callFunctionInBackground("purchaseItem", params, new FunctionCallback<Object>() {
            public void done(Object response, ParseException e) {
                finishProgress();
                if (e == null) {
                    Log.d("Cloud Response", "There were no exceptions! " + response.toString());
                    Toast.makeText(getApplicationContext(),
                            "Item Purchased Successfully ",
                            Toast.LENGTH_LONG).show();
                } else {
                    Log.d("Cloud Response", "Exception: " + e);
                    Toast.makeText(getApplicationContext(),
                            e.getMessage().toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void startProgress(String title) {
        progress.setTitle(title);
        progress.setMessage("Please Wait");
        progress.show();
    }

    private void finishProgress() {
        progress.dismiss();
    }

}
