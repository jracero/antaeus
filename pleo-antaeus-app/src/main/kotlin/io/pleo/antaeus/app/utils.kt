import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.ChargeStatus
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
    val customers = (1..100).mapNotNull {
        dal.createCustomer(
            currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
        )
    }

    customers.forEach { customer ->
        (1..10).forEach {
            dal.createInvoice(
                amount = Money(
                    value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                    currency = customer.currency
                ),
                customer = customer,
                status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
            )
        }
    }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
    return object : PaymentProvider {

        private fun thirdPartyResponseSimulator(invoice: Invoice): Boolean {
            when ((0..10).random()) {
                0 -> {
                    throw NetworkException()
                }
                1 -> {
                    throw CustomerNotFoundException(invoice.customerId)
                }
                2 -> {
                    throw CurrencyMismatchException(invoice.id, invoice.customerId)
                }
                else -> return Random.nextBoolean()
            }
        }

        override fun charge(invoice: Invoice): ChargeStatus {
            try {
                val result = thirdPartyResponseSimulator(invoice)
                if (result)
                    return ChargeStatus.SUCCESSFULLY_CHARGED
                return ChargeStatus.INSUFFICIENT_FUNDS

            } catch (ex: Exception) {
                return when (ex) {
                    is CustomerNotFoundException -> {
                        ChargeStatus.CUSTOMER_NOT_FOUND
                    }
                    is CurrencyMismatchException -> {
                        ChargeStatus.CURRENCY_MISMATCH
                    }
                    else -> ChargeStatus.NETWORK_ISSUE
                }
            }
        }
    }
}
