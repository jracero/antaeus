package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Charge
import io.pleo.antaeus.models.ChargeStatus
import io.pleo.antaeus.models.ChargeTransactionStats
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging


class BillingService(
    private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService, private val chargeService: ChargeService
) {
    private val logger = KotlinLogging.logger {}

    companion object {
        private const val DEFAULT_TIMES: Int = 5
        private const val INITIAL_DELAY: Long = 100
        private const val MAX_DELAY: Long = 1000
        private const val FACTOR: Double = 2.0
    }

    fun applyCharges(): Charge {
        val chargeTransactionStats = ChargeTransactionStats();
        val pendingInvoices = invoiceService.fetchPendingInvoices()
        chargeTransactionStats.totalInvoices(pendingInvoices.size)

        runBlocking() {
            pendingInvoices.map { invoice ->
                val charge = async {
                    retry(3) {
                        paymentProvider.charge(invoice)
                    }
                }
                val chargeInvoiceStatus = processPaymentProviderResponse(charge, invoice)
                stats(chargeInvoiceStatus, chargeTransactionStats)
            }
            logger.info("Transaction completed")
            return@runBlocking chargeTransactionStats
        }
        return chargeService.createChargeTransaction(chargeTransactionStats.toCharge())
    }

    private suspend fun processPaymentProviderResponse(
        charge: Deferred<ChargeStatus>,
        invoice: Invoice
    ): ChargeStatus {
        when (charge.await()) {
            ChargeStatus.SUCCESSFULLY_CHARGED -> {
                logger.info("Invoice successfully charged {}", invoice.toString())
                invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.PAID)
                return ChargeStatus.SUCCESSFULLY_CHARGED
            }
            ChargeStatus.INSUFFICIENT_FUNDS -> {
                logger.info("Account balance did not allow the charge {}", invoice.toString())
                return ChargeStatus.INSUFFICIENT_FUNDS
            }
            ChargeStatus.NETWORK_ISSUE -> {
                logger.warn("Unable to charge {} due to network issues", invoice.toString())
                return ChargeStatus.NETWORK_ISSUE
            }
            ChargeStatus.CUSTOMER_NOT_FOUND -> {
                logger.warn("Unable to charge {} due to unknown customer", invoice.toString())
                invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.CUSTOMER_ISSUE)
                return ChargeStatus.CUSTOMER_NOT_FOUND
            }
            ChargeStatus.CURRENCY_MISMATCH -> {
                logger.warn("Unable to charge {} due to currency mismatching", invoice.toString())
                invoiceService.updateInvoiceStatus(invoice.id, InvoiceStatus.CURRENCY_ISSUE)
                return ChargeStatus.CURRENCY_MISMATCH
            }
        }
    }

    private suspend fun stats(
        chargeStatus: ChargeStatus,
        chargeTransaction: ChargeTransactionStats
    ) {
        when (chargeStatus) {
            ChargeStatus.SUCCESSFULLY_CHARGED -> {
                chargeTransaction.increaseSuccessfullyCharged()
            }
            ChargeStatus.INSUFFICIENT_FUNDS -> {
                chargeTransaction.increasePendingToCharge()
            }
            ChargeStatus.NETWORK_ISSUE -> {
                chargeTransaction.increaseNoChargedDueToNetworkIssues()
            }
            ChargeStatus.CUSTOMER_NOT_FOUND -> {
                chargeTransaction.increaseNoChargedDueToUnknownCustomer()
            }
            ChargeStatus.CURRENCY_MISMATCH -> {
                chargeTransaction.increaseNoChargedDueToCurrencyMismatch()
            }
        }
    }

    private suspend fun <T> retry(
        times: Int = DEFAULT_TIMES,
        block: suspend () -> T
    ): T {
        var currentDelay = INITIAL_DELAY
        repeat(times - 1) {
            val result = block()
            if (result != ChargeStatus.NETWORK_ISSUE)
                return result
            logger.warn("failed attempt due to network issue...")
            delay(currentDelay)
            currentDelay = (currentDelay * FACTOR).toLong().coerceAtMost(MAX_DELAY)
        }
        return block()
    }
}


