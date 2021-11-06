/*
    Defines database tables and their schemas.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.Table
import org.joda.time.DateTime

object InvoiceTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
    val value = decimal("value", 1000, 2)
    val customerId = reference("customer_id", CustomerTable.id)
    val status = text("status")
}

object CustomerTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
}

object ChargeTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val chargedCreated = date("charged_created").clientDefault { DateTime.now() }
    val invoices = integer("pending")
    val charged = integer("charged")
    val noFunds = integer("no_funds")
    val unknownCustomer = integer("unk_customer")
    val currencyMismatch = integer("currency_mismatch")
}