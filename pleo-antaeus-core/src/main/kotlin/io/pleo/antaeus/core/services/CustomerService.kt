/*
    Implements endpoints related to customers.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.data.AntaeusCustomerDal
import io.pleo.antaeus.models.Customer

class CustomerService(private val dal: AntaeusCustomerDal) {

    fun fetchAll(): List<Customer> = dal.fetchCustomers()

    fun fetch(id: Int): Customer = dal.fetchCustomer(id) ?: throw CustomerNotFoundException(id)
}
