package com.cassens.autotran.activities;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Dealer;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.views.CustomScrollView;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DealerDetailsActivity extends AutoTranActivity
{
    private static final Logger log = LoggerFactory.getLogger(DealerDetailsActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    private static final String TAG="DealerDetailsActivity", prevNextFormat = "#%s\n%s";

    private List<Dealer> dealers;
    private int index = 0;
    private Button previous, next;
    private CustomScrollView customScrollView;
    private int operation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dealer_details);

        customScrollView = findViewById(R.id.dealerInfo);

        previous = findViewById(R.id.previous);
        next = findViewById(R.id.next);

        operation = getIntent().getIntExtra(Constants.CURRENT_OPERATION, Constants.DELIVERY_OPERATION);

        int dealer_id = getIntent().getIntExtra("dealer_id", -1);
        if (dealer_id == -1) {
            int delivery_id = getIntent().getIntExtra("delivery_id", -1);
            Delivery delivery = DataManager.getDelivery(this, delivery_id);

            if (delivery == null) {
                finish();
                return;
            }

            Load load = DataManager.getLoad(this, delivery.load_id);
            dealers = new ArrayList<>(load.deliveries.size());
            dealers.add(delivery.dealer);
            for (Delivery d : load.deliveries) {
                if (d.delivery_id != delivery.delivery_id) {
                    dealers.add(d.dealer);
                }
            }
        }
        else {
            Dealer dealer = DataManager.getDealer(this, dealer_id);
            dealers = new ArrayList<>(1);
            dealers.add(dealer);
        }

        if (dealers.size() == 1) {
            findViewById(R.id.nav_buttons).setVisibility(View.GONE);
        }
        log.debug(Logs.INTERACTION, "dealers size = " + dealers.size());
        showDealerInfo(dealers.get(index));
    }

    @Override
    public void onWindowFocusChanged (boolean hasFocus) {
        if(findViewById(R.id.rel_dealer_contact).getMeasuredHeight() > findViewById(R.id.dealerInfo).getMeasuredHeight()) {
            //Show some arrow image or something you like to show
            //CommonUtility.showText("Scroll for more");
        }
    }

    private void showDealerInfo(Dealer thisDealer) {
        String s;

		((TextView)findViewById(R.id.ACTIVITY_TITLE)).setText("Dealer #" + thisDealer.customer_number);
		TextView dealerAddrTxt = findViewById(R.id.txt_dealer_addr);
        TextView dealerContactTxt = findViewById(R.id.txt_dealer_contact);
        
        dealerAddrTxt.setText(thisDealer.customer_name + "\n" +
                              HelperFuncs.noNull(thisDealer.address) + "\n" +
                              HelperFuncs.noNull(thisDealer.city) + ", " + HelperFuncs.noNull(thisDealer.state) +
                              "  " + HelperFuncs.noNull(thisDealer.zip));
        s = HelperFuncs.noNull(thisDealer.contact_name);
        if (!isNull(thisDealer.email)) {
            s += "\n" + thisDealer.email;
        }
        dealerContactTxt.setText(s);
        
        TextView dealerPhoneTxt = findViewById(R.id.txt_dealer_phone);
        if (!HelperFuncs.isNullOrEmpty(thisDealer.phone)) {
            String formattedPhone = PhoneNumberUtils.formatNumber(thisDealer.phone, Locale.getDefault().getCountry());
            if (HelperFuncs.isNullOrEmpty(formattedPhone)) {
                dealerPhoneTxt.setText(thisDealer.phone);
            }
            else {
                dealerPhoneTxt.setText(formattedPhone);
            }
        } else {
        	dealerPhoneTxt.setVisibility(View.GONE);
        }
        
        
        ((TextView)findViewById(R.id.txt_dealer_afthr)).setText(thisDealer.afthr);
        ((TextView)findViewById(R.id.txt_dealer_mfg)).setText(thisDealer.mfg);
        ((TextView)findViewById(R.id.txt_dealer_status)).setText(thisDealer.status);

        // Set hours of operation
        ((TextView)findViewById(R.id.txt_hrs_mon)).setText(
                formatTime(thisDealer.monam) + " - " + formatTime(thisDealer.monpm));
        ((TextView)findViewById(R.id.txt_hrs_tue)).setText(
                formatTime(thisDealer.tueam) + " - " + formatTime(thisDealer.tuepm));
        ((TextView)findViewById(R.id.txt_hrs_wed)).setText(
                formatTime(thisDealer.wedam) + " - " + formatTime(thisDealer.wedpm));
        ((TextView)findViewById(R.id.txt_hrs_thu)).setText(
                formatTime(thisDealer.thuam) + " - " + formatTime(thisDealer.thupm));
        ((TextView)findViewById(R.id.txt_hrs_fri)).setText(
                formatTime(thisDealer.friam) + " - " + formatTime(thisDealer.fripm));
        ((TextView)findViewById(R.id.txt_hrs_sat)).setText(
                formatTime(thisDealer.satam) + " - " + formatTime(thisDealer.satpm));
        ((TextView)findViewById(R.id.txt_hrs_sun)).setText(
                formatTime(thisDealer.sunam) + " - " + formatTime(thisDealer.sunpm));

        thisDealer.comments = thisDealer.comments.replaceAll("\\\\n", "\\\n");

        String comments;
        if (operation == Constants.PRELOAD_OPERATION && thisDealer.high_claims) {
            comments = getString(R.string.high_claims_warning) + "\n" + thisDealer.comments;
        } else {
            comments = thisDealer.comments;
        }

        ((TextView)findViewById(R.id.txt_dealer_comments)).setText(comments);


        ((TextView)findViewById(R.id.txt_dealer_afthr_label)).setText("After Hours Delivery:");
        ((TextView)findViewById(R.id.txt_dealer_afthr_label)).setTypeface(null,Typeface.NORMAL);
        ((TextView)findViewById(R.id.txt_dealer_mfg_label)).setText("Manufacturer:");
        ((TextView)findViewById(R.id.txt_dealer_mfg_label)).setTypeface(null,Typeface.NORMAL);
        dealerAddrTxt.setTypeface(null,Typeface.NORMAL);
        ((TextView)findViewById(R.id.txt_dealer_phone_label)).setText("Phone:");
        ((TextView)findViewById(R.id.txt_dealer_phone_label)).setTypeface(null,Typeface.NORMAL);
        ((TextView)findViewById(R.id.txt_dealer_comments_label)).setText("Comments:");
        ((TextView)findViewById(R.id.txt_dealer_comments_label)).setTypeface(null,Typeface.NORMAL);

        for (Dealer.UpdatedField field: thisDealer.getUpdatedFields()) {
            switch (field.fieldName) {
                case "afthr":
                    setFieldLabelHighlight((TextView)findViewById(R.id.txt_dealer_afthr_label));
                    break;
                case "address":
                    setFieldLabelHighlight((TextView) findViewById(R.id.txt_dealer_addr));
                    break;
                case "phone":
                    setFieldLabelHighlight((TextView)findViewById(R.id.txt_dealer_phone_label));
                    break;
                case "comments":
                    setFieldLabelHighlight((TextView)findViewById(R.id.txt_dealer_comments_label));
                    break;
                case "hours":
                    setFieldLabelHighlight((TextView) findViewById(R.id.txt_hours));
                    break;
            }
        }

        if (index != 0) {
            Dealer dealer = dealers.get(index - 1);
            previous.setText(String.format(prevNextFormat, dealer.customer_number, dealer.customer_name));
            previous.setVisibility(View.VISIBLE);
            previous.setOnClickListener(v -> {
                CommonUtility.logButtonClick(log, v);
                showDealerInfo(dealers.get(--index));} );
        } else {
            previous.setVisibility(View.INVISIBLE);
        }

        if (index != dealers.size() - 1) {
            Dealer dealer = dealers.get(index + 1);
            next.setText(String.format(prevNextFormat, dealer.customer_number, dealer.customer_name));
            next.setVisibility(View.VISIBLE);
            next.setOnClickListener(v -> {
                CommonUtility.logButtonClick(log, v);
                showDealerInfo(dealers.get(++index));});
        } else {
            next.setVisibility(View.INVISIBLE);
        }
	}

    private void setFieldLabelHighlight(TextView view){
       String label = (String) view.getText();
       view.setText("* " + label );
       view.setTypeface(null, Typeface.BOLD);
    }
	
	public void back(View v)
	{
	    finish();
	}

    private boolean isNull(String s)
    {
        return (HelperFuncs.noNull(s).length() == 0);
    }

    private static String formatTime(int intMinutes) {
        @SuppressLint("DefaultLocale")
        String time = String.format("%03d", intMinutes);
        if (intMinutes < 0 || intMinutes > 2359 || (intMinutes % 100) > 59) {
            return "";
        }

        if (time.length() == 3) {
            time = time.substring(0, 1) + ":" + time.substring(1);
        } else if(time.length() == 4) {
            time = time.substring(0, 2) + ":" + time.substring(2);
        }
        return CommonUtility.convertToTwelveHourTime(time).toLowerCase();
    }

    public static String todaysHours(Dealer thisDealer){
        Calendar today = Calendar.getInstance();
        int day = today.get(Calendar.DAY_OF_WEEK);

        switch(today.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.monam)) + " - " + CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.monpm));

            case Calendar.TUESDAY:
                return CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.tueam)) + " - " + CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.tuepm));

            case Calendar.WEDNESDAY:
                return CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.wedam)) + " - " + CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.tueam));

            case Calendar.THURSDAY:
                return CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.thuam)) + " - " + CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.thupm));

            case Calendar.FRIDAY:
                return CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.friam)) +  " - " + CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.fripm));

            case Calendar.SATURDAY:
                return CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.satam)) + " - " + CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.satpm));

            case Calendar.SUNDAY:
                return CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.sunam)) +  " - " + CommonUtility.convertToTwelveHourTime(formatTime(thisDealer.sunpm));

        }
        return "";
    }
}
