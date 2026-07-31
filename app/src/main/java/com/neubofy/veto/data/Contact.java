package com.neubofy.veto.data;

import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

@Keep
public class Contact {

    private String name;
    private String number;

    private Contact(String name, String number) {
        this.name = name;
        this.number = number;
    }

    @Nullable
    public static Contact from(Context context, String name, String number) {
        TelephonyManager tm = context.getSystemService(TelephonyManager.class);
        String iso = tm.getNetworkCountryIso();

        String numberFormatted;
        if (iso.isEmpty()) {
            // iso is empty when the phone is in flight mode
            // fall back to deprecated function
            numberFormatted = PhoneNumberUtils.formatNumber(number);
        } else {
            // iso must be non-empty, else the number is treated as invalid
            numberFormatted = PhoneNumberUtils.formatNumber(number, iso);
        }

        if (numberFormatted == null || numberFormatted.isBlank()) {
            return null;
        }

        return new Contact(name, numberFormatted);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public boolean equals(@Nullable Object other) {
        if (!(other instanceof Contact)) {
            return false;
        }
        return PhoneNumberUtils.compare(number, ((Contact) other).number);
    }
}
