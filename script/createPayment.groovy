import org.slf4j.Logger
import org.slf4j.LoggerFactory
import groovy.xml.MarkupBuilder

//initialize logger for detailed logging
org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("createPaymentLogger")

logger.info("attempting to extract invoiceId and use it (" + invoiceId + ")")

//declare output parameters
//String paymentId                                            //output payment id
//String processOutput = 'default response'                   //output of process

//loop thru all invoice ids passed in and create a payment for each invoice
EntityList invoices = ec.entity.find("mantle.account.invoice.Invoice").condition([invoiceId: invoiceId]).list()
Map invoicePointer = invoices?.first()
Map invoiceData = ec.service.sync().name("mantle.account.InvoiceServices.get#InvoiceTotal").parameters([invoiceId: invoiceId]).call()

//test invoice data and do not allow to create payment at all costs
if (invoiceData != null) {
    Map payment = ec.service.sync().name("mantle.account.PaymentServices.create#InvoicePayment")
            .parameters(
            [
                    invoiceId              : invoiceId,
                    statusId               : 'PmntDelivered',
                    amountUomId            : invoicePointer.currencyUomId,
                    amount                 : invoiceData.unpaidTotal ?: 0,
                    paymentInstrumentEnumId: 'PiPersonalCheck',
                    effectiveDate          : ec.user.nowTimestamp
            ]
    ).call()



    if (payment.paymentId != null) {
        //hopefully, this adds payment Id to output
        logger.info("Adding paymentId (${payment.paymentId})")

        //adding to output list
        processOutput = "Adding payment (${payment.paymentId}) to invoice (${invoiceId})."

        //adding to output
        paymentId = payment.paymentId
    } else {
        //adding to output list
        processOutput = "Error adding payment to invoice (${invoiceId})."
    }

} else {
    logger.info("No payment created for invoice (${invoiceId})")

    //adding to output list
    processOutput = "No payment created for invoice (${invoiceId})"
}

//TODO test export to xml
/*logger.info("###### TEST XML EXPORT ######")
ec.entity.makeDataWriter().entityName("mantle.account.payment.Payment")
.dependentRecords(true).orderBy(["paymentId"]).fromDate(lastExportDate)
.thruDate(ec.user.nowTimestamp).file("C:\\Users\\rovna\\Documents\\GitHub\\moqui-Economy\\runtime\\tmp\\TestPaymentExport.xml")*/