package com.vyoog.prospectsoul_backend.verification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.vyoog.prospectsoul_backend.verification.entity.VerificationFailureCode;
import com.vyoog.prospectsoul_backend.verification.provider.LineTypes;
import com.vyoog.prospectsoul_backend.verification.provider.PhoneVerificationProvider;
import com.vyoog.prospectsoul_backend.verification.provider.PhoneVerificationResult;

/**
 * Controllable stand-in for Twilio.
 *
 * <p>No test ever makes a live provider call: the integration tests exercise
 * the whole slice — queue, worker, company mutation, activity, audit — against
 * scripted provider answers, which is the only way to assert the retryable and
 * permanent branches deterministically.
 */
public class StubPhoneVerificationProvider implements PhoneVerificationProvider {

    public static final String PROVIDER_NAME = "stub";

    private final Map<String, Function<Integer, PhoneVerificationResult>> scripted = new LinkedHashMap<>();
    private final Map<String, AtomicInteger> callCounts = new LinkedHashMap<>();
    private final List<String> calls = new ArrayList<>();

    private Function<Integer, PhoneVerificationResult> defaultAnswer =
            attempt -> mobile("+910000000000", "Stub Carrier");

    @Override
    public synchronized PhoneVerificationResult verify(String e164Phone) {
        calls.add(e164Phone);
        int attempt = callCounts.computeIfAbsent(e164Phone, key -> new AtomicInteger()).incrementAndGet();
        return scripted.getOrDefault(e164Phone, defaultAnswer).apply(attempt);
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    // --- scripting ----------------------------------------------------------

    /** Fixed answer for one number. */
    public synchronized void script(String e164Phone, PhoneVerificationResult result) {
        scripted.put(e164Phone, attempt -> result);
    }

    /** Answer that depends on the attempt number, for retry tests. */
    public synchronized void scriptByAttempt(String e164Phone,
                                             Function<Integer, PhoneVerificationResult> answer) {
        scripted.put(e164Phone, answer);
    }

    public synchronized void setDefaultAnswer(Function<Integer, PhoneVerificationResult> answer) {
        this.defaultAnswer = answer;
    }

    public synchronized void reset() {
        scripted.clear();
        callCounts.clear();
        calls.clear();
        defaultAnswer = attempt -> mobile("+910000000000", "Stub Carrier");
    }

    public synchronized int callCount(String e164Phone) {
        AtomicInteger count = callCounts.get(e164Phone);
        return count == null ? 0 : count.get();
    }

    public synchronized List<String> calls() {
        return List.copyOf(calls);
    }

    // --- canned results -----------------------------------------------------

    public static PhoneVerificationResult mobile(String phone, String carrier) {
        return new PhoneVerificationResult(PROVIDER_NAME, "https://stub/lookup/" + phone, phone,
                true, LineTypes.MOBILE, carrier, "404", "90", null, null, 12);
    }

    /**
     * A valid number with the given line type. The type is passed through
     * {@link LineTypes#normalize} because that is what a real provider
     * implementation does — Twilio spells these in camelCase and the contract
     * is that the domain only ever sees the lower-cased form.
     */
    public static PhoneVerificationResult lineType(String phone, String type) {
        return new PhoneVerificationResult(PROVIDER_NAME, "https://stub/lookup/" + phone, phone,
                true, LineTypes.normalize(type), "Stub Carrier", "404", "90", null, null, 12);
    }

    public static PhoneVerificationResult invalid(String phone) {
        return new PhoneVerificationResult(PROVIDER_NAME, "https://stub/lookup/" + phone, phone,
                false, null, null, null, null, null, null, 9);
    }

    public static PhoneVerificationResult failure(VerificationFailureCode code, String message) {
        return PhoneVerificationResult.failure(PROVIDER_NAME, code, message, 7);
    }
}
