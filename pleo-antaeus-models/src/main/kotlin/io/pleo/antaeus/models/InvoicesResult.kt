package io.pleo.antaeus.models

import com.google.gson.Gson

class InvoicesResult {

    private var successfullyCharged = 0
    private var pendingToCharge = 0
    private var unableToChargeDueToNetworkIssues = 0
    private var unableToChargeDueToUnknownCustomer = 0
    private var unableToChargeDueToCurrencyMismatch = 0

    fun successfullyCharged() {
        successfullyCharged++
    }

    fun stillPendingToCharge() {
        pendingToCharge++
    }

    fun noChargedInvoicesDueToNetworkIssues() {
        unableToChargeDueToNetworkIssues++
    }

    fun noChargedInvoicesDueToUnknownCustomer() {
        unableToChargeDueToUnknownCustomer++
    }

    fun noChargedInvoicesDueToCurrencyMismatch() {
        unableToChargeDueToCurrencyMismatch++
    }

    override fun toString(): String {
        val gson = Gson()
        return gson.toJson(this)
    }
}