package io.pleo.antaeus.models

import org.joda.time.DateTime

class InvoicesResult {

    private var totalInvoices = 0
    private var successfullyCharged = 0
    private var pendingToCharge = 0
    private var unableToChargeDueToNetworkIssues = 0
    private var unableToChargeDueToUnknownCustomer = 0
    private var unableToChargeDueToCurrencyMismatch = 0

    fun totalInvoices(total: Int) {
        totalInvoices = total
    }

    fun increaseSuccessfullyCharged() {
        successfullyCharged++
    }

    fun increasePendingToCharge() {
        pendingToCharge++
    }

    fun increaseNoChargedDueToNetworkIssues() {
        unableToChargeDueToNetworkIssues++
    }

    fun increaseNoChargedDueToUnknownCustomer() {
        unableToChargeDueToUnknownCustomer++
    }

    fun increaseNoChargedDueToCurrencyMismatch() {
        unableToChargeDueToCurrencyMismatch++
    }

    fun toCharge(): Charge {
        return Charge(
            0,
            DateTime.now().toDate(),
            this.totalInvoices,
            this.successfullyCharged,
            this.pendingToCharge,
            this.unableToChargeDueToUnknownCustomer,
            this.unableToChargeDueToCurrencyMismatch
        )
    }
}