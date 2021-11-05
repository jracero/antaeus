package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.ChargeStatus
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money

import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BillingServiceTest {

    private val successfullyChargedInvoice = Invoice(1, 1, Money(BigDecimal.valueOf(100), Currency.EUR), InvoiceStatus.PENDING)
    private val notEnoughFundsInvoice = Invoice(2, 2, Money(BigDecimal.valueOf(100), Currency.EUR), InvoiceStatus.PENDING)
    private val networkIssueInvoice = Invoice(3, 3, Money(BigDecimal.valueOf(100), Currency.EUR), InvoiceStatus.PENDING)
    private val unknownCustomerInvoice = Invoice(4, 4, Money(BigDecimal.valueOf(100), Currency.EUR), InvoiceStatus.PENDING)
    private val currencyMismatchInvoice = Invoice(5, 5, Money(BigDecimal.valueOf(100), Currency.EUR), InvoiceStatus.PENDING)

    private val provider = mockk<PaymentProvider> {
        every { charge(successfullyChargedInvoice) } returns ChargeStatus.SUCCESSFULLY_CHARGED
        every { charge(notEnoughFundsInvoice) } returns ChargeStatus.INSUFFICIENT_FUNDS
        every { charge(networkIssueInvoice) } returns ChargeStatus.NETWORK_ISSUE
        every { charge(unknownCustomerInvoice) } returns ChargeStatus.CUSTOMER_NOT_FOUND
        every { charge(currencyMismatchInvoice) } returns ChargeStatus.CURRENCY_MISMATCH
    }

    private val invoiceService = mockk<InvoiceService> {
        every { fetchPendingInvoices() } returns listOf(
            successfullyChargedInvoice,
            notEnoughFundsInvoice,
            networkIssueInvoice,
            unknownCustomerInvoice,
            currencyMismatchInvoice
        )
    }

    private val billingService = BillingService(paymentProvider = provider, invoiceService = invoiceService)

    @Test
    fun `will update invoice status to PAID`() {
        every { invoiceService.updateInvoiceStatus(successfullyChargedInvoice.id, InvoiceStatus.PAID) } returns 1
        every { invoiceService.updateInvoiceStatus(notEnoughFundsInvoice.id, InvoiceStatus.PENDING) } returns 2
        every { invoiceService.updateInvoiceStatus(networkIssueInvoice.id, InvoiceStatus.RETRY) } returns 3
        every { invoiceService.updateInvoiceStatus(unknownCustomerInvoice.id, InvoiceStatus.CUSTOMER_ISSUE) } returns 4
        every { invoiceService.updateInvoiceStatus(currencyMismatchInvoice.id, InvoiceStatus.CURRENCY_ISSUE) } returns 5


        billingService.applyCharges()
        verify { invoiceService.updateInvoiceStatus(1, InvoiceStatus.PAID) }
        verify(exactly = 0) { invoiceService.updateInvoiceStatus(2, any()) }
        verify(exactly = 0) { invoiceService.updateInvoiceStatus(3, any()) }
        verify { invoiceService.updateInvoiceStatus(4, InvoiceStatus.CUSTOMER_ISSUE) }
        verify { invoiceService.updateInvoiceStatus(5, InvoiceStatus.CURRENCY_ISSUE) }
    }
}