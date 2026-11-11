package com.azaika.payment.http;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("Task 5.1: delete this line when you start the task")
@Timeout(60)
class PaymentHttpServerTest extends HttpTestSupport {

    @Test
    void getAccount_returnsIdAndBalanceAsJson() throws Exception {
        HttpResponse<String> response = get("/accounts/3");

        assertEquals(200, response.statusCode());
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        assertTrue(contentType.startsWith("application/json"), "Content-Type is '" + contentType + "'");
        assertTrue(Pattern.compile("\"id\"\\s*:\\s*3\\b").matcher(response.body()).find(), response.body());
        assertTrue(Pattern.compile("\"balance\"\\s*:\\s*100000\\b").matcher(response.body()).find(), response.body());
    }

    @Test
    void getAccount_unknownId_returns404() throws Exception {
        assertEquals(404, get("/accounts/999").statusCode());
    }

    @Test
    void getAccount_idIsNotANumber_returns400() throws Exception {
        assertEquals(400, get("/accounts/abc").statusCode());
    }

    @Test
    void postTransfer_movesMoney() throws Exception {
        HttpResponse<String> response = post("/transfers", transferJson(1, 2, 500));

        assertEquals(200, response.statusCode());
        assertStatusField("SUCCESS", response);
        assertEquals(INITIAL_BALANCE - 500, balanceOf(1));
        assertEquals(INITIAL_BALANCE + 500, balanceOf(2));
    }

    @Test
    void postTransfer_acceptsJsonWithSpacesAndAnyFieldOrder() throws Exception {
        HttpResponse<String> response = post("/transfers", "{ \"amount\" : 500,\n  \"to\" : 2, \"from\" : 1 }");

        assertEquals(200, response.statusCode());
        assertEquals(INITIAL_BALANCE - 500, balanceOf(1));
        assertEquals(INITIAL_BALANCE + 500, balanceOf(2));
    }

    @Test
    void postTransfer_insufficientFunds_returns422AndMovesNothing() throws Exception {
        HttpResponse<String> response = post("/transfers", transferJson(1, 2, INITIAL_BALANCE + 1));

        assertEquals(422, response.statusCode());
        assertStatusField("INSUFFICIENT_FUNDS", response);
        assertEquals(INITIAL_BALANCE, balanceOf(1));
        assertEquals(INITIAL_BALANCE, balanceOf(2));
    }

    @Test
    void postTransfer_zeroAmount_returns422() throws Exception {
        HttpResponse<String> response = post("/transfers", transferJson(1, 2, 0));

        assertEquals(422, response.statusCode());
        assertStatusField("INVALID_AMOUNT", response);
    }

    @Test
    void postTransfer_sameAccount_returns422() throws Exception {
        HttpResponse<String> response = post("/transfers", transferJson(1, 1, 100));

        assertEquals(422, response.statusCode());
        assertStatusField("SAME_ACCOUNT", response);
    }

    @Test
    void postTransfer_unknownAccount_returns404() throws Exception {
        HttpResponse<String> response = post("/transfers", transferJson(1, 999, 100));

        assertEquals(404, response.statusCode());
        assertStatusField("ACCOUNT_NOT_FOUND", response);
    }

    @Test
    void postTransfer_bodyIsNotJson_returns400() throws Exception {
        assertEquals(400, post("/transfers", "give me money").statusCode());
    }

    @Test
    void postTransfer_fieldIsMissing_returns400() throws Exception {
        assertEquals(400, post("/transfers", "{\"from\":1,\"to\":2}").statusCode());
        assertEquals(INITIAL_BALANCE, balanceOf(1));
    }

    @Test
    void getOnTransfers_returns405() throws Exception {
        assertEquals(405, get("/transfers").statusCode());
    }

    @Test
    void postOnAccount_returns405() throws Exception {
        assertEquals(405, post("/accounts/1", "{}").statusCode());
    }

    @Test
    void unknownPath_returns404() throws Exception {
        assertEquals(404, get("/nothing-here").statusCode());
    }

    @Test
    void server_survivesABadRequest() throws Exception {
        HttpRequest broken = postRequest("/transfers", "{\"from\":\"one\",\"to\":2,\"amount\":5}", null);
        assertEquals(400, client.send(broken, HttpResponse.BodyHandlers.ofString()).statusCode());

        assertEquals(200, post("/transfers", transferJson(1, 2, 5)).statusCode());
    }
}
