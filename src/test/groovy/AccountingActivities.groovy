/*
 * This software is in the public domain under CC0 1.0 Universal plus a
 * Grant of Patent License.
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to the
 * public domain worldwide. This software is distributed without any
 * warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication
 * along with this software (see the LICENSE.md file). If not, see
 * <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

import org.moqui.Moqui
import org.moqui.context.ExecutionContext
import org.moqui.entity.EntityList
import org.moqui.entity.EntityValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Specification

import java.sql.Timestamp

/* To run these make sure moqui, and mantle are in place and run:
    "gradle cleanAll load runtime/mantle/mantle-usl:test"
   Or to quick run with saved DB copy use "gradle loadSave" once then each time "gradle reloadSave runtime/mantle/mantle-usl:test"
 */

class EconomyActivities extends Specification {
    @Shared
    protected final static Logger logger = LoggerFactory.getLogger(EconomyActivities.class)
    @Shared
    ExecutionContext ec
    @Shared
    String organizationPartyId = 'SRO', currencyUomId = 'EUR', timePeriodId
    @Shared
    String organizationPartyId2 = 'SRO', timePeriodId2
    @Shared
    long effectiveTime = System.currentTimeMillis()
    @Shared
    long totalFieldsChecked = 0


    def setupSpec() {
        // init the framework, get the ec
        ec = Moqui.getExecutionContext()
        ec.user.loginUser("john.doe", "moqui")
        // set an effective date so data check works, etc
        ec.user.setEffectiveTime(new Timestamp(effectiveTime))

        ec.entity.tempSetSequencedIdPrimary("mantle.party.time.TimePeriod", 55100, 10)
        ec.entity.tempSetSequencedIdPrimary("mantle.account.invoice.Invoice", 55100, 10)
        ec.entity.tempSetSequencedIdPrimary("mantle.ledger.transaction.AcctgTrans", 55100, 10)
    }

    def cleanupSpec() {
        ec.entity.tempResetSequencedIdPrimary("mantle.party.time.TimePeriod")
        ec.entity.tempResetSequencedIdPrimary("mantle.account.invoice.Invoice")
        ec.entity.tempResetSequencedIdPrimary("mantle.ledger.transaction.AcctgTrans")

        logger.info("Accounting Activities complete, ${totalFieldsChecked} record fields checked")
    }

    def setup() {
        ec.artifactExecution.disableAuthz()
    }

    def cleanup() {
        ec.artifactExecution.enableAuthz()
    }

    /*def "initial Investment AcctgTrans"() {
        when:
        // find the current Fiscal Months
        Map fiscalMonthOut = ec.service.sync().name("mantle.ledger.LedgerServices.get#OrganizationFiscalTimePeriods")
                .parameters([organizationPartyId:organizationPartyId, filterDate:ec.user.nowTimestamp, timePeriodTypeId:'FiscalMonth']).call()
        timePeriodId = fiscalMonthOut.timePeriodList[0].timePeriodId
        fiscalMonthOut = ec.service.sync().name("mantle.ledger.LedgerServices.get#OrganizationFiscalTimePeriods")
                .parameters([organizationPartyId:organizationPartyId2, filterDate:ec.user.nowTimestamp, timePeriodTypeId:'FiscalMonth']).call()
        timePeriodId2 = fiscalMonthOut.timePeriodList[0].timePeriodId

        // create investment postings
        Map transOut = ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTrans")
                .parameters([acctgTransTypeEnumId:'AttCapitalization', organizationPartyId:organizationPartyId, amountUomId:currencyUomId]).call()
        String acctgTransId = transOut.acctgTransId
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'111100000', debitCreditFlag:'D', amount:100000]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'332000000', debitCreditFlag:'C', amount:100000]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.post#AcctgTrans").parameters([acctgTransId:acctgTransId]).call()

        transOut = ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTrans")
                .parameters([acctgTransTypeEnumId:'AttCapitalization', organizationPartyId:organizationPartyId, amountUomId:currencyUomId]).call()
        acctgTransId = transOut.acctgTransId
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'111100000', debitCreditFlag:'D', amount:125000]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'332000000', debitCreditFlag:'C', amount:100000]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'333000000', debitCreditFlag:'C', amount:25000]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.post#AcctgTrans").parameters([acctgTransId:acctgTransId]).call()

        transOut = ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTrans")
                .parameters([acctgTransTypeEnumId:'AttCapitalization', organizationPartyId:organizationPartyId2, amountUomId:currencyUomId]).call()
        acctgTransId = transOut.acctgTransId
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'111100000', debitCreditFlag:'D', amount:150000]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'332000000', debitCreditFlag:'C', amount:150000]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.post#AcctgTrans").parameters([acctgTransId:acctgTransId]).call()

        List<String> dataCheckErrors = []
        long fieldsChecked = ec.entity.makeDataLoader().xmlText("""<entity-facade-xml>
            <mantle.ledger.account.GlAccountOrgTimePeriod glAccountId="111100000" timePeriodId="${timePeriodId}"
                    postedCredits="0" postedDebits="225000" endingBalance="225000" organizationPartyId="${organizationPartyId}"/>
            <mantle.ledger.account.GlAccountOrgTimePeriod glAccountId="332000000" timePeriodId="${timePeriodId}"
                    postedCredits="200000" postedDebits="0" endingBalance="200000" organizationPartyId="${organizationPartyId}"/>
            <mantle.ledger.account.GlAccountOrgTimePeriod glAccountId="333000000" timePeriodId="${timePeriodId}"
                    postedCredits="25000" postedDebits="0" endingBalance="25000" organizationPartyId="${organizationPartyId}"/>

            <mantle.ledger.account.GlAccountOrgTimePeriod glAccountId="111100000" timePeriodId="${timePeriodId2}"
                    postedCredits="0" postedDebits="150000" endingBalance="150000" organizationPartyId="${organizationPartyId2}"/>
            <mantle.ledger.account.GlAccountOrgTimePeriod glAccountId="332000000" timePeriodId="${timePeriodId2}"
                    postedCredits="150000" postedDebits="0" endingBalance="150000" organizationPartyId="${organizationPartyId2}"/>
        </entity-facade-xml>""").check(dataCheckErrors)
        totalFieldsChecked += fieldsChecked
        logger.info("Checked ${fieldsChecked} fields")
        if (dataCheckErrors) for (String dataCheckError in dataCheckErrors) logger.info(dataCheckError)
        if (ec.message.hasError()) logger.warn(ec.message.getErrorsString())

        then:
        dataCheckErrors.size() == 0
    }*/

    /*def "record Retained Earnings and Dividends Distributable AcctgTrans"() {
        when:
        Map transOut = ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTrans")
                .parameters([acctgTransTypeEnumId:'AttInternal', organizationPartyId:organizationPartyId, amountUomId:currencyUomId]).call()
        String acctgTransId = transOut.acctgTransId
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'850000000', debitCreditFlag:'D', amount:100]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'336000000', debitCreditFlag:'C', amount:100]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.post#AcctgTrans").parameters([acctgTransId:acctgTransId]).call()

        transOut = ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTrans")
                .parameters([acctgTransTypeEnumId:'AttInternal', organizationPartyId:organizationPartyId, amountUomId:currencyUomId]).call()
        acctgTransId = transOut.acctgTransId
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'336000000', debitCreditFlag:'D', amount:60]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'335000000', debitCreditFlag:'C', amount:60]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.post#AcctgTrans").parameters([acctgTransId:acctgTransId]).call()

        List<String> dataCheckErrors = []
        long fieldsChecked = ec.entity.makeDataLoader().xmlText("""<entity-facade-xml>
        </entity-facade-xml>""").check(dataCheckErrors)
        totalFieldsChecked += fieldsChecked
        logger.info("Checked ${fieldsChecked} fields")
        if (dataCheckErrors) for (String dataCheckError in dataCheckErrors) logger.info(dataCheckError)
        if (ec.message.hasError()) logger.warn(ec.message.getErrorsString())

        then:
        dataCheckErrors.size() == 0
    }*/

    /*def "pay Dividends AcctgTrans"() {
        when:
        Map transOut = ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTrans")
                .parameters([acctgTransTypeEnumId:'AttDisbursement', organizationPartyId:organizationPartyId, amountUomId:currencyUomId]).call()
        String acctgTransId = transOut.acctgTransId
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'335000000', debitCreditFlag:'D', amount:30]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'111100000', debitCreditFlag:'C', amount:30]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.post#AcctgTrans").parameters([acctgTransId:acctgTransId]).call()

        *//* pay out just one of the dividends to see amounts for both in accounts in different states
        transOut = ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTrans")
                .parameters([acctgTransTypeEnumId:'AttDisbursement', organizationPartyId:organizationPartyId, amountUomId:currencyUomId]).call()
        acctgTransId = transOut.acctgTransId
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'335000000', debitCreditFlag:'D', amount:50000]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.create#AcctgTransEntry")
                .parameters([acctgTransId:acctgTransId, glAccountId:'111100000', debitCreditFlag:'C', amount:50000]).call()
        ec.service.sync().name("mantle.ledger.LedgerServices.post#AcctgTrans").parameters([acctgTransId:acctgTransId]).call()
        *//*

        List<String> dataCheckErrors = []
        long fieldsChecked = ec.entity.makeDataLoader().xmlText("""<entity-facade-xml>
        </entity-facade-xml>""").check(dataCheckErrors)
        totalFieldsChecked += fieldsChecked
        logger.info("Checked ${fieldsChecked} fields")
        if (dataCheckErrors) for (String dataCheckError in dataCheckErrors) logger.info(dataCheckError)
        if (ec.message.hasError()) logger.warn(ec.message.getErrorsString())

        then:
        dataCheckErrors.size() == 0
    }*/

    def "expense invoice creation"() {
        when:
        Map invoiceOut = ec.service.sync().name("mantle.account.InvoiceServices.create#Invoice")
                .parameters([fromPartyId:'ZiddlemanInc', toPartyId:organizationPartyId, currencyUomId:currencyUomId]).call()
        String invoiceId = invoiceOut.invoiceId

        ec.service.sync().name("mantle.account.InvoiceServices.create#InvoiceItem")
                .parameters([invoiceId:invoiceId, itemTypeEnumId:'ItemExpCommTelephone', description:'Land Line Service',
                             quantity:1, amount:123.45]).call()
        ec.service.sync().name("mantle.account.InvoiceServices.create#InvoiceItem")
                .parameters([invoiceId:invoiceId, itemTypeEnumId:'ItemExpCommCellular', description:'Cell Service',
                             quantity:1, amount:234.56]).call()
        ec.service.sync().name("mantle.account.InvoiceServices.create#InvoiceItem")
                .parameters([invoiceId:invoiceId, itemTypeEnumId:'ItemExpCommNetwork', description:'Internet Service',
                             quantity:1, amount:345.67]).call()
        ec.service.sync().name("mantle.account.InvoiceServices.create#InvoiceItem")
                .parameters([invoiceId:invoiceId, itemTypeEnumId:'ItemExpHosting', description:'Site Hosting',
                             quantity:1, amount:45.45]).call()

        ec.service.sync().name("mantle.account.InvoiceServices.create#InvoiceItem")
                .parameters([invoiceId:invoiceId, itemTypeEnumId:'ItemExpOfficeSup', description:'Office Supplies',
                             quantity:1, amount:101.01]).call()

        ec.service.sync().name("mantle.account.InvoiceServices.create#InvoiceItem")
                .parameters([invoiceId:invoiceId, itemTypeEnumId:'ItemExpUtilHeating', description:'Heating',
                             quantity:1, amount:33.33]).call()
        ec.service.sync().name("mantle.account.InvoiceServices.create#InvoiceItem")
                .parameters([invoiceId:invoiceId, itemTypeEnumId:'ItemExpUtilElec', description:'Electricity',
                             quantity:1, amount:111.11]).call()
        ec.service.sync().name("mantle.account.InvoiceServices.create#InvoiceItem")
                .parameters([invoiceId:invoiceId, itemTypeEnumId:'ItemExpUtilWater', description:'Water',
                             quantity:1, amount:30.00]).call()
        ec.service.sync().name("mantle.account.InvoiceServices.create#InvoiceItem")
                .parameters([invoiceId:invoiceId, itemTypeEnumId:'ItemExpUtilTrash', description:'Trash Disposal',
                             quantity:1, amount:34.00]).call()

        ec.service.sync().name("mantle.account.InvoiceServices.create#InvoiceItem")
                .parameters([invoiceId:invoiceId, itemTypeEnumId:'ItemExpInterestEquip', description:'Equipment Interest',
                             quantity:1, amount:321.12]).call()
        ec.service.sync().name("mantle.account.InvoiceServices.create#InvoiceItem")
                .parameters([invoiceId:invoiceId, itemTypeEnumId:'ItemExpInterestReal', description:'Real Estate Interest',
                             quantity:1, amount:444.55]).call()

        ec.service.sync().name("update#mantle.account.invoice.Invoice")
                .parameters([invoiceId:invoiceId, statusId:'InvoiceReceived']).call()
        ec.service.sync().name("update#mantle.account.invoice.Invoice")
                .parameters([invoiceId:invoiceId, statusId:'InvoiceApproved']).call()

        List<String> dataCheckErrors = []
        long fieldsChecked = ec.entity.makeDataLoader().xmlText("""<entity-facade-xml>
            <mantle.account.invoice.Invoice invoiceId="${invoiceId}" invoiceTypeEnumId="InvoiceSales" toPartyId="${organizationPartyId}"
                    fromPartyId="ZiddlemanInc" invoiceDate="${effectiveTime}"
                    currencyUomId="${currencyUomId}" statusId="InvoiceApproved">
                <mantle.account.invoice.InvoiceItem amount="123.45" quantity="1" description="Land Line Service"
                        invoiceItemSeqId="01" itemTypeEnumId="ItemExpCommTelephone"/>
                <mantle.account.invoice.InvoiceItem amount="234.56" quantity="1" description="Cell Service"
                        invoiceItemSeqId="02" itemTypeEnumId="ItemExpCommCellular"/>
                <mantle.account.invoice.InvoiceItem amount="345.67" quantity="1" description="Internet Service"
                        invoiceItemSeqId="03" itemTypeEnumId="ItemExpCommNetwork"/>
                <mantle.account.invoice.InvoiceItem amount="45.45" quantity="1" description="Site Hosting"
                        invoiceItemSeqId="04" itemTypeEnumId="ItemExpHosting"/>
                <mantle.account.invoice.InvoiceItem amount="101.01" quantity="1" description="Office Supplies"
                        invoiceItemSeqId="05" itemTypeEnumId="ItemExpOfficeSup"/>
                <mantle.account.invoice.InvoiceItem amount="33.33" quantity="1" description="Heating"
                        invoiceItemSeqId="06" itemTypeEnumId="ItemExpUtilHeating"/>
                <mantle.account.invoice.InvoiceItem amount="111.11" quantity="1" description="Electricity"
                        invoiceItemSeqId="07" itemTypeEnumId="ItemExpUtilElec"/>
                <mantle.account.invoice.InvoiceItem amount="30" quantity="1" description="Water"
                        invoiceItemSeqId="08" itemTypeEnumId="ItemExpUtilWater"/>
                <mantle.account.invoice.InvoiceItem amount="34" quantity="1" description="Trash Disposal"
                        invoiceItemSeqId="09" itemTypeEnumId="ItemExpUtilTrash"/>
                <mantle.account.invoice.InvoiceItem amount="321.12" quantity="1" description="Equipment Interest"
                        invoiceItemSeqId="10" itemTypeEnumId="ItemExpInterestEquip"/>
                <mantle.account.invoice.InvoiceItem amount="444.55" quantity="1" description="Real Estate Interest"
                        invoiceItemSeqId="11" itemTypeEnumId="ItemExpInterestReal"/>
            </mantle.account.invoice.Invoice>
        </entity-facade-xml>""").check(dataCheckErrors)
        totalFieldsChecked += fieldsChecked
        logger.info("Checked ${fieldsChecked} fields")
        if (dataCheckErrors) for (String dataCheckError in dataCheckErrors) logger.info(dataCheckError)
        if (ec.message.hasError()) logger.warn(ec.message.getErrorsString())

        then:
        dataCheckErrors.size() == 0
    }

    def "return invoice creation - NoVAT"() {
        when:

        Map newInvoiceItem = [itemTypeEnumId:'ItemExpCommTelephone', description:'Land Line Service',
                              quantity:1, amount:125]
        List itemsToCreate = []
        itemsToCreate.add(newInvoiceItem)

        Map invoiceOut = ec.service.sync().name("mantle.account.InvoiceServicesEnhancements.createAndCheck#Invoice")
                .parameters([fromPartyId:'ZiddlemanInc', toPartyId:organizationPartyId, currencyUomId:currencyUomId, invoiceCenterId:'AC_GASTRO',referenceNumber:'123', itemsToCreate:itemsToCreate]).call()
        String invoiceId = invoiceOut.invoiceId

        /*ec.service.sync().name("mantle.account.InvoiceServices.create#InvoiceItem")
                .parameters([invoiceId:invoiceId, itemTypeEnumId:'ItemExpCommTelephone', description:'Land Line Service',
                             quantity:1, amount:125]).call()*/

        Map invoiceReturnOut = ec.service.sync().name("mantle.account.InvoiceServicesEnhancements.create#InvoiceReturn")
                .parameters([fromPartyId:'ZiddlemanInc', toPartyId:organizationPartyId, currencyUomId:currencyUomId, referenceNumber:'123abc', createOptions:'copyContent', originalInvoiceId:invoiceId]).call()
        String invoiceReturnId = invoiceReturnOut.createdInvoiceId

        /*ec.service.sync().name("update#mantle.account.invoice.Invoice")
                .parameters([invoiceId:invoiceId, statusId:'InvoiceReceived']).call()
        ec.service.sync().name("update#mantle.account.invoice.Invoice")
                .parameters([invoiceId:invoiceId, statusId:'InvoiceApproved']).call()*/

        List<String> dataCheckErrors = []
        long fieldsChecked = ec.entity.makeDataLoader().xmlText("""<entity-facade-xml>
            <mantle.account.invoice.Invoice invoiceId="${invoiceReturnId}" invoiceTypeEnumId="InvoiceReturn" toPartyId="${organizationPartyId}"
                    fromPartyId="ZiddlemanInc" invoiceDate="${effectiveTime}"
                    currencyUomId="${currencyUomId}" statusId="InvoicePmtSent" invoiceCenterId="AC_GASTRO">
                <mantle.account.invoice.InvoiceItem amount="${-125}" quantity="1" description="Land Line Service"
                        invoiceItemSeqId="01" itemTypeEnumId="ItemExpCommTelephone" itemCenterId="AC_GASTRO"/>
            </mantle.account.invoice.Invoice>
        </entity-facade-xml>""").check(dataCheckErrors)
        totalFieldsChecked += fieldsChecked
        logger.info("Checked ${fieldsChecked} fields")
        if (dataCheckErrors) for (String dataCheckError in dataCheckErrors) logger.info(dataCheckError)
        if (ec.message.hasError()) logger.warn(ec.message.getErrorsString())

        then:
        dataCheckErrors.size() == 0
    }


    def "return invoice creation - VAT"() {
        when:

        Map newInvoiceItem = [itemTypeEnumId:'ItemExpOther', description:'Land Line Service',
                              quantity:1, amount:1000, itemVatTypeId:'VAT_Base', itemVatRate:20]
        List itemsToCreate = []
        itemsToCreate.add(newInvoiceItem)

        Map invoiceOut = ec.service.sync().name("mantle.account.InvoiceServicesEnhancements.createAndCheck#Invoice")
                .parameters([fromPartyId:'ZiddlemanInc', toPartyId:organizationPartyId, currencyUomId:currencyUomId, invoiceCenterId:'AC_GASTRO',referenceNumber:'456', itemsToCreate:itemsToCreate, calculateVat: true]).call()
        String invoiceId = invoiceOut.invoiceId

        Map invoiceReturnOut = ec.service.sync().name("mantle.account.InvoiceServicesEnhancements.create#InvoiceReturn")
                .parameters([fromPartyId:'ZiddlemanInc', toPartyId:organizationPartyId, currencyUomId:currencyUomId, referenceNumber:'456abc', createOptions:'copyContent', originalInvoiceId:invoiceId, calculateVat:true]).call()
        String invoiceReturnId = invoiceReturnOut.createdInvoiceId

        /*ec.service.sync().name("update#mantle.account.invoice.Invoice")
                .parameters([invoiceId:invoiceId, statusId:'InvoiceReceived']).call()
        ec.service.sync().name("update#mantle.account.invoice.Invoice")
                .parameters([invoiceId:invoiceId, statusId:'InvoiceApproved']).call()*/

        List<String> dataCheckErrors = []
        long fieldsChecked = ec.entity.makeDataLoader().xmlText("""<entity-facade-xml>
            <mantle.account.invoice.Invoice invoiceId="${invoiceReturnId}" invoiceTypeEnumId="InvoiceReturn" toPartyId="${organizationPartyId}"
                    fromPartyId="ZiddlemanInc" invoiceDate="${effectiveTime}"
                    currencyUomId="${currencyUomId}" statusId="InvoicePmtSent" invoiceCenterId="AC_GASTRO">
                <mantle.account.invoice.InvoiceItem amount="${-1000}" quantity="1" description="Land Line Service"
                        invoiceItemSeqId="01" itemTypeEnumId="ItemExpOther" itemCenterId="AC_GASTRO"/>
                <mantle.account.invoice.InvoiceItem amount="${-200}" quantity="1" description="Calculated VAT"
                        invoiceItemSeqId="02" itemTypeEnumId="ItemExpTaxesLic" itemCenterId="AC_GASTRO"/>
            </mantle.account.invoice.Invoice>
        </entity-facade-xml>""").check(dataCheckErrors)
        totalFieldsChecked += fieldsChecked
        logger.info("Checked ${fieldsChecked} fields")
        if (dataCheckErrors) for (String dataCheckError in dataCheckErrors) logger.info(dataCheckError)
        if (ec.message.hasError()) logger.warn(ec.message.getErrorsString())

        then:
        dataCheckErrors.size() == 0
    }

    def "invoice and its proforma"() {
        when:

        /*Map newInvoiceItem = [itemTypeEnumId:'ItemExpOther', description:'Substitutes',
                              quantity:1, amount:1000, itemVatTypeId:'VAT_Base', itemVatRate:25]*/
        List itemsToCreate = []
        itemsToCreate.add([itemTypeEnumId:'ItemExpOther', description:'Substitutes', quantity:1, amount:1000, itemVatTypeId:'VAT_Base', itemVatRate:25])
        itemsToCreate.add([itemTypeEnumId:'ItemExpOther', description:'Goalies', quantity:1, amount:10000, itemVatTypeId:'VAT_Base', itemVatRate:33])

        Map invoiceProforma = ec.service.sync().name("mantle.account.InvoiceServicesEnhancements.createAndCheck#Invoice")
                .parameters([invoiceTypeEnumId:'InvoiceProforma',fromPartyId:'ZiddlemanInc', toPartyId:organizationPartyId, currencyUomId:currencyUomId, invoiceCenterId:'AC_WELLNESS',referenceNumber:'111222', itemsToCreate:itemsToCreate, calculateVat: true]).call()
        String invoiceProformaId = invoiceProforma.invoiceId

        Map invoiceRealToProforma = ec.service.sync().name("mantle.account.InvoiceServicesEnhancements.createInvoiceToProforma#Invoice")
                .parameters([fromPartyId:'ZiddlemanInc', toPartyId:organizationPartyId, currencyUomId:currencyUomId, proformaInvoiceId:invoiceProformaId, newReferenceNumber:'112233aa', newDueDate:effectiveTime, newInvoiceDate:effectiveTime]).call()
        String invoiceReturnId = invoiceRealToProforma.invoiceId

        List<String> dataCheckErrors = []
        long fieldsChecked = ec.entity.makeDataLoader().xmlText("""<entity-facade-xml>
            <mantle.account.invoice.Invoice invoiceId="${invoiceReturnId}" invoiceTypeEnumId="InvoiceSales" toPartyId="${organizationPartyId}"
                    fromPartyId="ZiddlemanInc" invoiceDate="${effectiveTime}"
                    currencyUomId="${currencyUomId}" statusId="InvoiceApproved" invoiceCenterId="AC_WELLNESS" invoiceItemTotal="${1000*1.25 + 10000*1.33}">
            </mantle.account.invoice.Invoice>
        </entity-facade-xml>""").check(dataCheckErrors)
        totalFieldsChecked += fieldsChecked
        logger.info("Checked ${fieldsChecked} fields")
        if (dataCheckErrors) for (String dataCheckError in dataCheckErrors) logger.info(dataCheckError)
        if (ec.message.hasError()) logger.warn(ec.message.getErrorsString())

        then:
        dataCheckErrors.size() == 0
    }

    def "invoice and incorrectly set proforma"() {
        when:

        /*Map newInvoiceItem = [itemTypeEnumId:'ItemExpOther', description:'Substitutes',
                              quantity:1, amount:1000, itemVatTypeId:'VAT_Base', itemVatRate:25]*/
        List itemsToCreate = []
        itemsToCreate.add([itemTypeEnumId:'ItemExpOther', description:'Substitutes', quantity:1, amount:10000, itemVatTypeId:'VAT_Base', itemVatRate:25])
        itemsToCreate.add([itemTypeEnumId:'ItemExpOther', description:'Goalies', quantity:1, amount:20000, itemVatTypeId:'VAT_Base', itemVatRate:33])

        Map invoiceProforma = ec.service.sync().name("mantle.account.InvoiceServicesEnhancements.createAndCheck#Invoice")
                .parameters([invoiceTypeEnumId:'InvoiceProforma',fromPartyId:'ZiddlemanInc', toPartyId:organizationPartyId, currencyUomId:currencyUomId, invoiceCenterId:'AC_WELLNESS',referenceNumber:'111432', itemsToCreate:itemsToCreate, calculateVat: true]).call()
        String invoiceProformaId = invoiceProforma.invoiceId

        Map invoiceRealToProforma = ec.service.sync().name("mantle.account.InvoiceServicesEnhancements.createInvoiceToProforma#Invoice")
                .parameters([fromPartyId:'ZiddlemanInc', toPartyId:organizationPartyId, currencyUomId:currencyUomId, proformaInvoiceId:invoiceProformaId, newReferenceNumber:'111432', newDueDate:effectiveTime, newInvoiceDate:effectiveTime]).call()
        String invoiceReturnId = invoiceRealToProforma.invoiceId

        //display errors
        //if (ec.message.hasError()) logger.warn(ec.message.getErrorsString())

        then:
        //ec.message.hasError()
        ec.message.getErrorsString().contains('Invoice with same external number from supplier already exist!')
    }
}


