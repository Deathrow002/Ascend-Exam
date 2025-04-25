package com.ascendcorp.exam.service;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.StringUtils;

import com.ascendcorp.exam.model.InquiryServiceResultDTO;
import com.ascendcorp.exam.model.TransferResponse;
import com.ascendcorp.exam.proxy.BankProxyGateway;

public class InquiryService {

    @Autowired
    private BankProxyGateway bankProxyGateway;

    private static final Logger LOGGER = LoggerFactory.getLogger(InquiryService.class);

    // Constants for error codes
    private static final String ERROR_CODE_BAD_REQUEST_DATA = "400";
    private static final String ERROR_CODE_TRANSACTION_ERROR = "500";
    private static final String ERROR_CODE_UNKNOWN_RESPONSE = "501";
    private static final String ERROR_CODE_TIMEOUT = "503";
    private static final String ERROR_CODE_INTERNAL_ERROR = "504";

    public InquiryServiceResultDTO inquiry(String transactionId,
            Date tranDateTime,
            String channel,
            String locationCode,
            String bankCode,
            String bankNumber,
            double amount,
            String reference1,
            String reference2,
            String firstName,
            String lastName) {
        InquiryServiceResultDTO respDTO;

        try {
            // Validate input parameters
            if (!validateRequestParameters(transactionId, tranDateTime, channel, bankCode, bankNumber, amount)) {
                return createErrorResponse(ERROR_CODE_TRANSACTION_ERROR, "General Invalid Data");
            }

            LOGGER.info("Calling bank web service...");
            TransferResponse response = bankProxyGateway.requestTransfer(transactionId, tranDateTime, channel,
                    bankCode, bankNumber, amount, reference1, reference2);

            // Process the bank response
            respDTO = processBankResponse(response);

        } catch (WebServerException r) {
            LOGGER.error("WebServerException occurred", r);
            respDTO = handleWebServerException(r);
        } catch (Exception e) {
            LOGGER.error("Unexpected exception occurred {}", e);
            respDTO = createErrorResponse(ERROR_CODE_INTERNAL_ERROR, "Internal Application Error");
        }

        return respDTO;
    }

    /**
     * Validates the input parameters for the inquiry request.
     */
    private boolean validateRequestParameters(String transactionId, Date tranDateTime, String channel,
            String bankCode, String bankNumber, double amount) {
        return StringUtils.hasText(transactionId) && tranDateTime != null && StringUtils.hasText(channel) &&
               StringUtils.hasText(bankCode) && StringUtils.hasText(bankNumber) && amount > 0;
    }

    /**
     * Processes the response from the bank and maps it to an
     * InquiryServiceResultDTO.
     */
    private InquiryServiceResultDTO processBankResponse(TransferResponse response) {
        if (response == null) {
            LOGGER.error("Bank response is null");
            return createErrorResponse(ERROR_CODE_INTERNAL_ERROR, "Internal Application Error");
        }

        InquiryServiceResultDTO respDTO = new InquiryServiceResultDTO();
        respDTO.setRef_no1(response.getReferenceCode1());
        respDTO.setRef_no2(response.getReferenceCode2());
        respDTO.setAmount(response.getBalance());
        respDTO.setTranID(response.getBankTransactionID());

        String responseCode = response.getResponseCode();
        String description = response.getDescription();

        switch (responseCode.toLowerCase()) {
            case "approved":
                respDTO.setReasonCode("200");
                respDTO.setReasonDesc(description);
                respDTO.setAccountName(description);
                break;
            case "invalid_data":
                handleInvalidDataResponse(respDTO, description);
                break;
            case "transaction_error":
                handleTransactionErrorResponse(respDTO, description);
                break;
            case "unknown":
                handleUnknownResponse(respDTO, description);
                break;
            default:
                LOGGER.error("Unsupported Error Reason Code: {}", responseCode);
                return createErrorResponse(ERROR_CODE_INTERNAL_ERROR, "Internal Application Error");
        }

        return respDTO;
    }

    /**
     * Handles the "invalid_data" response from the bank.
     */
    private void handleInvalidDataResponse(InquiryServiceResultDTO respDTO, String description) {
        if (description != null) {
            String[] respDesc = description.split(":");
            if (respDesc.length > 2) {
                respDTO.setReasonCode(respDesc[1]);
                respDTO.setReasonDesc(respDesc[2]);
            } else if (respDesc.length == 2) {
                String responseDesc = respDesc[respDesc.length - 1].isBlank() ? "General Invalid Data" : respDesc[respDesc.length - 1];
                respDTO.setReasonCode(respDesc[0]);
                respDTO.setReasonDesc(responseDesc);
            } else {
                respDTO.setReasonCode(ERROR_CODE_BAD_REQUEST_DATA);
                respDTO.setReasonDesc("General Invalid Data");
            }
        } else {
            respDTO.setReasonCode(ERROR_CODE_BAD_REQUEST_DATA);
            respDTO.setReasonDesc("General Invalid Data");
        }
    }

    /**
     * Handles the "transaction_error" response from the bank.
     */
    private void handleTransactionErrorResponse(InquiryServiceResultDTO respDTO, String description) {
        if (description != null) {
            String[] respDesc = description.split(":");
            if (respDesc.length > 2) {
                respDTO.setReasonCode(respDesc[1]);
                respDTO.setReasonDesc(respDesc[2]);
            } else if (respDesc.length == 2) {
                String responseDesc = respDesc[respDesc.length - 1].isBlank() ? "General Invalid Data" : respDesc[respDesc.length - 1];
                respDTO.setReasonCode(respDesc[0]);
                respDTO.setReasonDesc(responseDesc);
            } else {
                respDTO.setReasonCode(ERROR_CODE_TRANSACTION_ERROR);
                respDTO.setReasonDesc("General Transaction Error");
            }
        } else {
            respDTO.setReasonCode(ERROR_CODE_TRANSACTION_ERROR);
            respDTO.setReasonDesc("General Transaction Error");
        }
    }

    /**
     * Handles the "unknown" response from the bank.
     */
    private void handleUnknownResponse(InquiryServiceResultDTO respDTO, String description) {
        if (description != null) {
            String[] respDesc = description.split(":");
            if (respDesc.length >= 2) {
                respDTO.setReasonCode(respDesc[0]);
                String responseDesc = respDesc[respDesc.length - 1].isBlank() ? "General Invalid Data" : respDesc[respDesc.length - 1];
                respDTO.setReasonDesc(responseDesc);
            } else {
                respDTO.setReasonCode(ERROR_CODE_UNKNOWN_RESPONSE);
                respDTO.setReasonDesc("General Invalid Data");
            }
        } else {
            respDTO.setReasonCode(ERROR_CODE_UNKNOWN_RESPONSE);
            respDTO.setReasonDesc("General Invalid Data");
        }
    }

    /**
     * Handles exceptions from the bank web service.
     */
    private InquiryServiceResultDTO handleWebServerException(WebServerException exception) {
        String faultString = exception.getMessage();
        if (faultString != null && (faultString.contains("java.net.SocketTimeoutException")
                || faultString.contains("Connection timed out"))) {
            return createErrorResponse(ERROR_CODE_TIMEOUT, "Error timeout");
        } else {
            return createErrorResponse(ERROR_CODE_INTERNAL_ERROR, "Internal Application Error");
        }
    }

    /**
     * Creates an error response DTO with the given reason code and description.
     */
    private InquiryServiceResultDTO createErrorResponse(String reasonCode, String reasonDesc) {
        InquiryServiceResultDTO respDTO = new InquiryServiceResultDTO();
        respDTO.setReasonCode(reasonCode);
        respDTO.setReasonDesc(reasonDesc);
        return respDTO;
    }
}
