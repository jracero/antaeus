/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data


import io.pleo.antaeus.models.Charge
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class AntaeusDal(private val db: Database) {
    fun fetchInvoice(id: Int): Invoice? {
        // transaction(db) runs the internal query as a new database transaction.
        return transaction(db) {
            // Returns the first invoice with matching id.
            InvoiceTable
                .select { InvoiceTable.id.eq(id) }
                .firstOrNull()
                ?.toInvoice()
        }
    }

    fun fetchInvoices(): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .selectAll()
                .map { it.toInvoice() }
        }
    }

    fun fetchInvoicesByStatus(invoiceStatus: InvoiceStatus): List<Invoice> {
        return transaction(db) {
            InvoiceTable
                .select(InvoiceTable.status.eq(invoiceStatus.name))
                .map { it.toInvoice() }
        }
    }

    fun createInvoice(amount: Money, customer: Customer, status: InvoiceStatus = InvoiceStatus.PENDING): Invoice? {
        val id = transaction(db) {
            // Insert the invoice and returns its new id.
            InvoiceTable
                .insert {
                    it[this.value] = amount.value
                    it[this.currency] = amount.currency.toString()
                    it[this.status] = status.toString()
                    it[this.customerId] = customer.id
                } get InvoiceTable.id
        }

        return fetchInvoice(id)
    }

    fun updateInvoiceStatus(id: Int, status: InvoiceStatus): Int? {
        return transaction(db) {
            InvoiceTable
                .update(where = { InvoiceTable.id.eq(id) }) {
                    it[this.status] = status.toString()
                }
        }
    }

    fun fetchCustomer(id: Int): Customer? {
        return transaction(db) {
            CustomerTable
                .select { CustomerTable.id.eq(id) }
                .firstOrNull()
                ?.toCustomer()
        }
    }

    fun fetchCustomers(): List<Customer> {
        return transaction(db) {
            CustomerTable
                .selectAll()
                .map { it.toCustomer() }
        }
    }

    fun createCustomer(currency: Currency): Customer? {
        val id = transaction(db) {
            // Insert the customer and return its new id.
            CustomerTable.insert {
                it[this.currency] = currency.toString()
            } get CustomerTable.id
        }

        return fetchCustomer(id)
    }

    fun createChargeTransaction(charge: Charge): Charge? {
        val id = transaction(db) {
            ChargeTable
                .insert {
                    it[this.invoices] = charge.invoices
                    it[this.charged] = charge.charged
                    it[this.noFunds] = charge.noFunds
                    it[this.unknownCustomer] = charge.unknownCustomer
                    it[this.currencyMismatch] = charge.currencyMismatch
                } get ChargeTable.id
        }
        return fetchCharge(id)
    }

    fun fetchChargeTransaction(): List<Charge> {
        return transaction(db) {
            ChargeTable
                .selectAll()
                .map { it.toCharge() }
                .sortedByDescending { charge -> charge.chargeCreated }
        }
    }

    private fun fetchCharge(id: Int): Charge? {
        return transaction(db) {
            ChargeTable
                .select { ChargeTable.id.eq(id) }
                .firstOrNull()
                ?.toCharge()
        }
    }
}
