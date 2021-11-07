/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusInvoiceDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: AntaeusInvoiceDal) {

    fun fetchAll(): List<Invoice> = dal.fetchInvoices()

    fun fetchPendingInvoices(): List<Invoice> = dal.fetchInvoicesByStatus(InvoiceStatus.PENDING)

    fun fetch(id: Int): Invoice = dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)

    fun updateInvoiceStatus(invoiceId: Int, newInvoiceStatus: InvoiceStatus): Int? = dal.updateInvoiceStatus(invoiceId, newInvoiceStatus)
}
