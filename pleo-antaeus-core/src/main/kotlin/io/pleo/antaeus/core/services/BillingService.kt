package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.ChargeStatus
import io.pleo.antaeus.models.InvoicesResult
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging


class BillingService(
    private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService
) {
    private val logger = KotlinLogging.logger {}

    fun applyCharges(): InvoicesResult {
        val chargeTransaction = InvoicesResult();
        runBlocking(Dispatchers.Default) {
            invoiceService.fetchPendingInvoices().map {

                val charge = async {
                    retry {
                        paymentProvider.charge(it)
                    }
                }

                when (charge.await()) {
                    ChargeStatus.SUCCESSFULLY_CHARGED -> {
                        updateInvoiceStatus(it.id, InvoiceStatus.PAID)
                        chargeTransaction.successfullyCharged()
                    }
                    ChargeStatus.INSUFFICIENT_FUNDS -> {
                        logger.info("Insufficient funds to charge {}", it.toString())
                        chargeTransaction.stillPendingToCharge()
                    }
                    ChargeStatus.NETWORK_ISSUE -> {
                        logger.info("Network issues to charge {}", it.toString())
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


    private suspend fun <T> retry(
        times: Int = 3,
        initialDelay: Long = 100,
        maxDelay: Long = 1000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            val result = block()
            if (result != ChargeStatus.NETWORK_ISSUE)
                return result
            System.out.println("failed attempt...")
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block()
    }

    private fun updateInvoiceStatus(invoiceId: Int, newInvoiceStatus: InvoiceStatus) {
        invoiceService.updateInvoiceStatus(invoiceId, newInvoiceStatus)
    }
}


