/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.opendaylight.restconf.common.errors.RestconfError;
import org.opendaylight.restconf.common.errors.RestconfError.ErrorTag;
import org.opendaylight.restconf.common.errors.RestconfError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Unit tests for RestconfError.
 *
 * @author Devin Avery
 * @author Thomas Pantelis
 *
 */
public class RestconfErrorTest {

    static class Contains extends BaseMatcher<String> {

        private final String text;

        Contains(final String text) {
            this.text = text;
        }

        @Override
        public void describeTo(final Description desc) {
            desc.appendText("contains ").appendValue(text);
        }

        @Override
        public boolean matches(final Object arg) {
            return arg != null && arg.toString().contains(text);
        }
    }

    @Test
    public void testErrorTagValueOf() {
        assertEquals(ErrorTag.IN_USE, ErrorTag.valueOfCaseInsensitive(ErrorTag.IN_USE.getTagValue()));
    }

    @Test
    public void testErrorTagValueOfIsLowercase() {
        assertEquals("in-use", ErrorTag.IN_USE.getTagValue());
    }

    @Test
    public void testErrorTypeGetErrorTypeTagIsLowerCase() {
        assertEquals(ErrorType.APPLICATION.name().toLowerCase(), ErrorType.APPLICATION.getErrorTypeTag());
    }

    @Test
    public void testErrorTypeValueOf() {
        assertEquals(ErrorType.APPLICATION, ErrorType.valueOfCaseInsensitive(ErrorType.APPLICATION.getErrorTypeTag()));
    }

    @Test
    public void testErrorTagStatusCodes() {
        Map<String, Integer> lookUpMap = new HashMap<>();

        lookUpMap.put("in-use", 409);
        lookUpMap.put("invalid-value", 400);
        lookUpMap.put("too-big", 413);
        lookUpMap.put("missing-attribute", 400);
        lookUpMap.put("bad-attribute", 400);
        lookUpMap.put("unknown-attribute", 400);
        lookUpMap.put("missing-element", 400);
        lookUpMap.put("bad-element", 400);
        lookUpMap.put("unknown-element", 400);
        lookUpMap.put("unknown-namespace", 400);
        lookUpMap.put("access-denied", 403);
        lookUpMap.put("lock-denied", 409);
        lookUpMap.put("resource-denied", 409);
        lookUpMap.put("rollback-failed", 500);
        lookUpMap.put("data-exists", 409);
        lookUpMap.put("data-missing", 409);
        lookUpMap.put("operation-not-supported", 501);
        lookUpMap.put("operation-failed", 500);
        lookUpMap.put("partial-operation", 500);
        lookUpMap.put("malformed-message", 400);
        lookUpMap.put("resource-denied-transport", 503);

        for (ErrorTag tag : ErrorTag.values()) {
            Integer expectedStatusCode = lookUpMap.get(tag.getTagValue());
            assertNotNull("Failed to find " + tag.getTagValue(), expectedStatusCode);
            assertEquals("Status Code does not match", expectedStatusCode, Integer.valueOf(tag.getStatusCode()));
        }
    }

    @Test
    public void testRestConfDocumentedException_NoCause() {
        String expectedMessage = "Message";
        ErrorType expectedErrorType = ErrorType.RPC;
        ErrorTag expectedErrorTag = ErrorTag.IN_USE;
        RestconfError error = new RestconfError(expectedErrorType, expectedErrorTag, expectedMessage);

        validateRestConfError(expectedMessage, expectedErrorType, expectedErrorTag, null, (String) null, error);
    }

    @Test
    public void testRestConfDocumentedException_WithAppTag() {
        String expectedMessage = "Message";
        ErrorType expectedErrorType = ErrorType.RPC;
        ErrorTag expectedErrorTag = ErrorTag.IN_USE;
        String expectedErrorAppTag = "application.tag";

        RestconfError error =
                new RestconfError(expectedErrorType, expectedErrorTag, expectedMessage, expectedErrorAppTag);

        validateRestConfError(expectedMessage, expectedErrorType, expectedErrorTag, expectedErrorAppTag, (String) null,
                error);
    }

    @Test
    public void testRestConfDocumentedException_WithAppTagErrorInfo() {
        String expectedMessage = "Message";
        ErrorType expectedErrorType = ErrorType.RPC;
        ErrorTag expectedErrorTag = ErrorTag.IN_USE;
        String expectedErrorAppTag = "application.tag";
        String errorInfo = "<extra><sessionid>session.id</sessionid></extra>";

        RestconfError error =
                new RestconfError(expectedErrorType, expectedErrorTag, expectedMessage, expectedErrorAppTag, errorInfo);

        validateRestConfError(expectedMessage, expectedErrorType, expectedErrorTag, expectedErrorAppTag, errorInfo,
                error);
    }

    @Test
    public void testRestConfErrorWithRpcError() {

        // All fields set
        RpcError rpcError = RpcResultBuilder.newError(
                RpcError.ErrorType.PROTOCOL, ErrorTag.BAD_ATTRIBUTE.getTagValue(), "mock error-message",
                "mock app-tag", "mock error-info", new Exception("mock cause"));

        validateRestConfError("mock error-message", ErrorType.PROTOCOL, ErrorTag.BAD_ATTRIBUTE, "mock app-tag",
                "mock error-info", new RestconfError(rpcError));

        // All fields set except 'info' - expect error-info set to 'cause'
        rpcError = RpcResultBuilder.newError(
                RpcError.ErrorType.PROTOCOL, ErrorTag.BAD_ATTRIBUTE.getTagValue(), "mock error-message",
                "mock app-tag", null, new Exception("mock cause"));

        validateRestConfError("mock error-message", ErrorType.PROTOCOL, ErrorTag.BAD_ATTRIBUTE, "mock app-tag",
                new Contains("mock cause"), new RestconfError(rpcError));

        // Some fields set - expect error-info set to ErrorSeverity
        rpcError = RpcResultBuilder.newError(
                RpcError.ErrorType.RPC, ErrorTag.ACCESS_DENIED.getTagValue(), null, null, null, null);

        validateRestConfError(null, ErrorType.RPC, ErrorTag.ACCESS_DENIED, null, "<severity>error</severity>",
                new RestconfError(rpcError));

        // 'tag' field not mapped to ErrorTag - expect error-tag set to
        // OPERATION_FAILED
        rpcError = RpcResultBuilder.newWarning(
                RpcError.ErrorType.TRANSPORT, "not mapped", null, null, null, null);

        validateRestConfError(null, ErrorType.TRANSPORT, ErrorTag.OPERATION_FAILED, null,
                "<severity>warning</severity>", new RestconfError(rpcError));

        // No fields set - edge case
        rpcError = RpcResultBuilder.newError(null, null, null, null, null, null);

        validateRestConfError(null, ErrorType.APPLICATION, ErrorTag.OPERATION_FAILED,
                null, "<severity>error</severity>", new RestconfError(rpcError));
    }

    private static void validateRestConfError(final String expectedMessage, final ErrorType expectedErrorType,
            final ErrorTag expectedErrorTag, final String expectedErrorAppTag, final String errorInfo,
            final RestconfError error) {

        validateRestConfError(expectedMessage, expectedErrorType, expectedErrorTag, expectedErrorAppTag,
                equalTo(errorInfo), error);
    }

    private static void validateRestConfError(final String expectedMessage, final ErrorType expectedErrorType,
            final ErrorTag expectedErrorTag, final String expectedErrorAppTag, final Matcher<String> errorInfoMatcher,
            final RestconfError error) {

        assertEquals("getErrorMessage", expectedMessage, error.getErrorMessage());
        assertEquals("getErrorType", expectedErrorType, error.getErrorType());
        assertEquals("getErrorTag", expectedErrorTag, error.getErrorTag());
        assertEquals("getErrorAppTag", expectedErrorAppTag, error.getErrorAppTag());
        assertThat("getErrorInfo", error.getErrorInfo(), errorInfoMatcher);
        error.toString(); // really just checking for NPE etc. Don't care about
                      // contents.
    }
}
