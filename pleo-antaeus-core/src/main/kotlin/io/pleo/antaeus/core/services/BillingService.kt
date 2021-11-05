package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.ChargeStatus
import io.pleo.antaeus.models.ChargeTransaction
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging


class BillingService(
    private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService
) {
    private val logger = KotlinLogging.logger {}


    fun applyCharges(): ChargeTransaction {
        val chargeTransaction = ChargeTransaction();
        runBlocking(Dispatchers.Default) {
            invoiceService.fetchPendingInvoices().map {

                val charge = async { paymentProvider.charge(it) }

                when (charge.await()) {
                    ChargeStatus.SUCCESSFULLY_CHARGED -> {
                        updateInvoiceStatus(it.id, InvoiceStatus.PAID)
                        chargeTransaction.successfullyCharged()
                    }
                    ChargeStatus.INSUFFICIENT_FUNDS -> {
                        logger.info("to be charged in following days")
                        chargeTransaction.stillPendingToCharge()
                    }
                    ChargeStatus.NETWORK_ISSUE -> {
                        updateInvoiceStatus(it.id, InvoiceStatus.RETRY)
                        chargeTransaction.noChargedInvoicesDueToNetworkIssues()
                    }
                    ChargeStatus.CUSTOMER_NOT_FOUND -> {
                        updateInvoiceStatus(it.id, InvoiceStatus.CUSTOMER_ISSUE)
                        chargeTransaction.noChargedInvoicesDueToUnknownCustomer()
                    }
                    ChargeStatus.CURRENCY_MISMATCH -> {
                        updateInvoiceStatus(it.id, InvoiceStatus.CURRENCY_ISSUE)
                        chargeTransaction.noChargedInvoicesDueToCurrencyMismatch()
                    }
                }
            }
            logger.info(chargeTransaction.toString())
            logger.info("Transaction completed")
            return@runBlocking chargeTransaction
        }
        return chargeTransaction
    }

    private fun updateInvoiceStatus(invoiceId: Int, newInvoiceStatus: InvoiceStatus) {
        invoiceService.updateInvoiceStatus(invoiceId, newInvoiceStatus)
    }
}


