package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Charge
import io.pleo.antaeus.models.ChargeStatus
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.InvoicesResult
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging


class BillingService(
    private val paymentProvider: PaymentProvider, private val invoiceService: InvoiceService, private val chargeService: ChargeService
) {
    private val logger = KotlinLogging.logger {}

    fun applyCharges(): Charge? {
        val chargeTransaction = InvoicesResult();

        val pendingInvoices = invoiceService.fetchPendingInvoices()
        chargeTransaction.totalInvoices(pendingInvoices.size)

        runBlocking(Dispatchers.Default) {
            pendingInvoices.map {

                val charge = async {
                    retry(3) {
                        paymentProvider.charge(it)
                    }
                }
                processPaymentProviderResponse(charge, it, chargeTransaction)
            }
            logger.info("Transaction completed")
            return@runBlocking chargeTransaction
        }
        return chargeService.createChargeTransaction(chargeTransaction.toCharge())
    }

    private suspend fun processPaymentProviderResponse(
        charge: Deferred<ChargeStatus>,
        it: Invoice,
        chargeTransaction: InvoicesResult
    ) {
        when (charge.await()) {
            ChargeStatus.SUCCESSFULLY_CHARGED -> {
                updateInvoiceStatus(it.id, InvoiceStatus.PAID)
                chargeTransaction.successfullyCharged()
            }
            ChargeStatus.INSUFFICIENT_FUNDS -> {
                logger.info("Account balance did not allow the charge {}", it.toString())
                chargeTransaction.stillPendingToCharge()
            }
            ChargeStatus.NETWORK_ISSUE -> {
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

    private suspend fun <T> retry(
        times: Int = 5,
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
            logger.warn("failed attempt due to network issue...")
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
        return block()
    }

    private fun updateInvoiceStatus(invoiceId: Int, newInvoiceStatus: InvoiceStatus) {
        invoiceService.updateInvoiceStatus(invoiceId, newInvoiceStatus)
    }
}


