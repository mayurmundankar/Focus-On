package com.focuslock.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Payment, UPI and banking apps. They refuse to work while any non-system
 * accessibility service is switched on, so website blocking switches itself off
 * the moment one of these comes to the front.
 */
object PaymentApps {

    /**
     * Payment and banking apps that may not register for upi:// links.
     * Multi-purpose apps (WhatsApp, Amazon, …) are left out on purpose: opening them
     * many times a day must not switch website blocking off.
     */
    private val KNOWN = setOf(
        // UPI and wallets (India)
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",
        "net.one97.paytm",
        "in.org.npci.upiapp", // BHIM
        "com.dreamplug.androidapp", // CRED
        "com.mobikwik_new",
        "com.freecharge.android",
        "money.super.payments", // super.money
        "com.naviapp",
        "com.myairtelapp",
        "com.csam.icici.bank.imobile",
        "com.sbi.lotusintouch", // YONO SBI
        "com.sbi.upi", // BHIM SBI Pay
        "com.snapwork.hdfc",
        "com.hdfcbank.payzapp",
        "com.axis.mobile",
        "com.msf.kbank.mobile", // Kotak
        "com.kotak811mobilebankingapp.instantsavingsupiscanandpayrecharge",
        "com.bankofbaroda.mconnect",
        "com.infrasofttech.indianbank",
        "com.canarabank.mobility",
        "com.pnb.pnbone",
        "com.idfcfirstbank.optimus",
        "com.yesbank",
        "com.fampay.in",
        // Elsewhere
        "com.google.android.apps.walletnfcrel", // Google Wallet
        "com.paypal.android.p2pmobile",
        "com.venmo",
        "com.squareup.cash",
        "com.revolut.revolut",
        "com.transferwise.android",
        "com.samsung.android.spay",
    )

    /** Known payment apps, plus every installed app that can pay a UPI link. */
    fun packages(context: Context): Set<String> {
        val upi = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
        val upiApps = runCatching {
            context.packageManager
                .queryIntentActivities(upi, PackageManager.MATCH_DEFAULT_ONLY)
                .map { it.activityInfo.packageName }
        }.getOrDefault(emptyList())
        return KNOWN + upiApps - MULTI_PURPOSE - context.packageName
    }

    /** Register upi:// but are opened all day for other reasons. */
    private val MULTI_PURPOSE = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "in.amazon.mShop.android.shopping",
        "com.flipkart.android",
        "com.truecaller",
        "com.jio.myjio",
    )
}
