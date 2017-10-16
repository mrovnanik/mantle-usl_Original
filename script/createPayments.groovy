import org.slf4j.Logger
import org.slf4j.LoggerFactory
import groovy.xml.MarkupBuilder

//*************************************************
//CODE NOT USED, USING PER INVOICE PAYMENT CREATION
//*************************************************


//initialize logger for detailed logging
org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("createPaymentsLogger")

//start writing log
logger.info("Listing context")

//prepare rootMap variable to list all available values in context
Map<String, String> rootMap = ec.getContext().getRootMap()

//list thru rootMap and log
for (Map.Entry<String, String> crunchifyEntry : rootMap.entrySet()) {
    logger.info(
            "key: " + crunchifyEntry.getKey() +
                    ", value: " + crunchifyEntry.getValue()
    )
}

logger.info("attempting to extract invoicesList and list thru 'em")

//extract invoices ids from context

//ATTEMPT 6
List<String> invoiceIds = rootMap.get("invoiceList").tokenize(",")

logger.info("rootMap.containsKey(\"invoiceList\") " + rootMap.containsKey("invoiceList"))
logger.info("rootMap.get(\"invoiceList\", null).size() " + rootMap.get("invoiceList", null).size())

//declare output list (payments created)
def paymentsList = []

//loop thru all invoice ids passed in and create a payment for each invoice
for (def invoiceId in invoiceIds) {
    logger.info("invoiceId.toString()" + invoiceId.toString())

    EntityList invoices = ec.entity.find("mantle.account.invoice.Invoice").condition([invoiceId: invoiceId]).list()
    Map invoicePointer = invoices?.first()
    Map invoiceData = ec.service.sync().name("mantle.account.InvoiceServices.get#InvoiceTotal").parameters([invoiceId: invoiceId]).call()

    //test invoice data and do not allow to create payment at all costs
    if (invoiceData != null) {
        Map payment = ec.service.sync().name("mantle.account.PaymentServices.create#InvoicePayment")
                .parameters(
                [
                        invoiceId: invoiceId,
                        statusId: 'PmntDelivered',
                        amountUomId: invoicePointer.currencyUomId,
                        amount: invoiceData.unpaidTotal?:0,
                        paymentInstrumentEnumId: 'PiPersonalCheck',
                        effectiveDate: ec.user.nowTimestamp
                ]
        ).call()

        //hopefully, this adds payment Id to list
        logger.info("Adding paymentId into list (${payment.paymentId})")

        //adding to list personally
        paymentsList.add(payment.paymentId)

    } else {
        logger.info("No payment created for invoice (${invoiceId})")
    }
}

//TODO test export to xml
/*logger.info("###### TEST XML EXPORT ######")
ec.entity.makeDataWriter().entityName("mantle.account.payment.Payment")
.dependentRecords(true).orderBy(["paymentId"]).fromDate(lastExportDate)
.thruDate(ec.user.nowTimestamp).file("C:\\Users\\rovna\\Documents\\GitHub\\moqui-Economy\\runtime\\tmp\\TestPaymentExport.xml")*/



