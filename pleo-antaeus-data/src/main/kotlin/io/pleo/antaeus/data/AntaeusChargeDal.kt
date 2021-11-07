/*
    Implements the data access layer (DAL).
    The data access layer generates and executes requests to the database.

    See the `mappings` module for the conversions between database rows and Kotlin objects.
 */

package io.pleo.antaeus.data


import io.pleo.antaeus.models.Charge
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class AntaeusChargeDal(private val db: Database) {

    fun createChargeTransaction(charge: Charge): Charge {
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

    private fun fetchCharge(id: Int): Charge {
        return transaction(db) {
            ChargeTable
                .select { ChargeTable.id.eq(id) }
                .first()
                .toCharge()
        }
    }
}
