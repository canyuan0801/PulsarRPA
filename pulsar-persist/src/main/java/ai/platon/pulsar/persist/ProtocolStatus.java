/**
 * Autogenerated by Avro
 * <p>
 * DO NOT EDIT DIRECTLY
 */
package ai.platon.pulsar.persist;

import ai.platon.pulsar.persist.gora.generated.GProtocolStatus;
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProtocolStatus implements ProtocolStatusCodes {
    public static final String ARG_HTTP_CODE = "httpCode";
    public static final String ARG_REDIRECT_TO_URL = "redirectTo";
    public static final String ARG_URL = "url";
    public static final String ARG_RETRY_SCOPE = "rsp";
    public static final String ARG_RETRY_REASON = "rrs";

    /**
     * Content was not retrieved yet.
     */
    private static final short NOTFETCHED = 0;
    /**
     * Content was retrieved without errors.
     */
    private static final short SUCCESS = 1;
    /**
     * Content was not retrieved. Any further errors may be indicated in args.
     */
    private static final short FAILED = 2;

    public static final ProtocolStatus STATUS_SUCCESS = new ProtocolStatus(SUCCESS, SUCCESS_OK);
    public static final ProtocolStatus STATUS_NOTMODIFIED = new ProtocolStatus(SUCCESS, NOTMODIFIED);
    public static final ProtocolStatus STATUS_NOTFETCHED = new ProtocolStatus(NOTFETCHED);

    public static final ProtocolStatus STATUS_PROTO_NOT_FOUND = ProtocolStatus.failed(PROTO_NOT_FOUND);
    public static final ProtocolStatus STATUS_ACCESS_DENIED = ProtocolStatus.failed(ACCESS_DENIED);
    public static final ProtocolStatus STATUS_NOTFOUND = ProtocolStatus.failed(NOTFOUND);
    // if a task is canceled, we do not save anything, if a task is retry, all the metadata is saved
    public static final ProtocolStatus STATUS_CANCELED = ProtocolStatus.failed(CANCELED);
    public static final ProtocolStatus STATUS_EXCEPTION = ProtocolStatus.failed(EXCEPTION);

    private static final HashMap<Short, String> majorCodes = new HashMap<>();
    private static final HashMap<Integer, String> minorCodes = new HashMap<>();

    static {
        majorCodes.put(NOTFETCHED, "nofetched");
        majorCodes.put(SUCCESS, "success");
        majorCodes.put(FAILED, "failed");

        minorCodes.put(SUCCESS_OK, "ok");
        minorCodes.put(MOVED, "moved");
        minorCodes.put(TEMP_MOVED, "temp_moved");
        minorCodes.put(NOTMODIFIED, "notmodified");

        minorCodes.put(PROTO_NOT_FOUND, "proto_not_found");
        minorCodes.put(ACCESS_DENIED, "access_denied");
        minorCodes.put(NOTFOUND, "notfound");
        minorCodes.put(REQUEST_TIMEOUT, "request_timeout");
        minorCodes.put(GONE, "gone");

        minorCodes.put(UNKNOWN_HOST, "unknown_host");
        minorCodes.put(ROBOTS_DENIED, "robots_denied");
        minorCodes.put(EXCEPTION, "exception");
        minorCodes.put(REDIR_EXCEEDED, "redir_exceeded");
        minorCodes.put(WOULDBLOCK, "wouldblock");
        minorCodes.put(BLOCKED, "blocked");

        minorCodes.put(RETRY, "retry");
        minorCodes.put(CANCELED, "canceled");
        minorCodes.put(THREAD_TIMEOUT, "thread_timeout");
        minorCodes.put(WEB_DRIVER_TIMEOUT, "web_driver_timeout");
        minorCodes.put(SCRIPT_TIMEOUT, "script_timeout");
    }

    private GProtocolStatus protocolStatus;

    public ProtocolStatus(short majorCode) {
        this.protocolStatus = GProtocolStatus.newBuilder().build();
        setMajorCode(majorCode);
        setMinorCode(-1);
    }

    public ProtocolStatus(short majorCode, int minorCode) {
        this.protocolStatus = GProtocolStatus.newBuilder().build();
        setMajorCode(majorCode);
        setMinorCode(minorCode);
    }

    private ProtocolStatus(GProtocolStatus protocolStatus) {
        Objects.requireNonNull(protocolStatus);
        this.protocolStatus = protocolStatus;
    }

    @Nonnull
    public static ProtocolStatus box(GProtocolStatus protocolStatus) {
        return new ProtocolStatus(protocolStatus);
    }

    public static String getMajorName(short code) {
        return majorCodes.getOrDefault(code, "unknown");
    }

    public static String getMinorName(int code) {
        return minorCodes.getOrDefault(code, "unknown");
    }

    @Nonnull
    public static ProtocolStatus retry(RetryScope scope) {
        return failed(ProtocolStatusCodes.RETRY, ARG_RETRY_SCOPE, scope);
    }

    @Nonnull
    public static ProtocolStatus retry(RetryScope scope, Object reason) {
        return failed(ProtocolStatusCodes.RETRY, ARG_RETRY_SCOPE, scope, ARG_RETRY_REASON, reason);
    }

    @Nonnull
    public static ProtocolStatus cancel(Object... args) {
        return failed(ProtocolStatusCodes.CANCELED, args);
    }

    @Nonnull
    public static ProtocolStatus failed(int minorCode) {
        return new ProtocolStatus(FAILED, minorCode);
    }

    @Nonnull
    public static ProtocolStatus failed(int minorCode, Object... args) {
        ProtocolStatus protocolStatus = new ProtocolStatus(FAILED, minorCode);

        if (args.length % 2 == 0) {
            Map<CharSequence, CharSequence> protocolStatusArgs = protocolStatus.getArgs();
            for (int i = 0; i < args.length - 1; i += 2) {
                if (args[i] != null && args[i + 1] != null) {
                    protocolStatusArgs.put(args[i].toString(), args[i + 1].toString());
                }
            }
        }

        return protocolStatus;
    }

    @Nonnull
    public static ProtocolStatus failed(Throwable e) {
        return failed(EXCEPTION, "error", e.getMessage());
    }

    public static ProtocolStatus fromMinor(int minorCode) {
        if (minorCode == SUCCESS_OK || minorCode == NOTMODIFIED) {
            return STATUS_SUCCESS;
        } else {
            return failed(minorCode);
        }
    }

    public static boolean isTimeout(ProtocolStatus protocalStatus) {
        int code = protocalStatus.getMinorCode();
        return isTimeout(code);
    }

    public static boolean isTimeout(int code) {
        return code == REQUEST_TIMEOUT || code == THREAD_TIMEOUT || code == WEB_DRIVER_TIMEOUT || code == SCRIPT_TIMEOUT;
    }

    public GProtocolStatus unbox() {
        return protocolStatus;
    }

    public boolean isNotFetched() {
        return getMajorCode() == NOTFETCHED;
    }

    public boolean isSuccess() {
        return getMajorCode() == SUCCESS;
    }

    public boolean isFailed() {
        return getMajorCode() == FAILED;
    }

    public boolean isCanceled() {
        return getMinorCode() == CANCELED;
    }

    public boolean isRetry() {
        return getMinorCode() == RETRY;
    }

    public boolean isRetry(RetryScope scope) {
        RetryScope defaultScope = RetryScope.CRAWL;
        return getMinorCode() == RETRY && getArgOrDefault(ARG_RETRY_SCOPE, defaultScope.toString()).equals(scope.toString());
    }

    public boolean isTempMoved() {
        return getMinorCode() == TEMP_MOVED;
    }

    public boolean isMoved() {
        return getMinorCode() == TEMP_MOVED || getMinorCode() == MOVED;
    }

    public boolean isTimeout() {
        return isTimeout(this);
    }

    public String getMajorName() {
        return getMajorName(getMajorCode());
    }

    public short getMajorCode() {
        return protocolStatus.getMajorCode().shortValue();
    }

    public void setMajorCode(short majorCode) {
        protocolStatus.setMajorCode((int) majorCode);
    }

    public String getMinorName() {
        return getMinorName(getMinorCode());
    }

    /**
     * The detailed status code of the protocol, it must be compatible with standard http response code
     * */
    public int getMinorCode() {
        return protocolStatus.getMinorCode();
    }

    public void setMinorCode(int minorCode) {
        protocolStatus.setMinorCode(minorCode);
    }

    public void setMinorCode(int minorCode, String message) {
        setMinorCode(minorCode);
        getArgs().put(getMinorName(), message);
    }

    public Map<CharSequence, CharSequence> getArgs() {
        return protocolStatus.getArgs();
    }

    public void setArgs(Map<CharSequence, CharSequence> args) {
        protocolStatus.setArgs(args);
    }

    public String getName() {
        return majorCodes.getOrDefault(getMajorCode(), "unknown") + "/"
                + minorCodes.getOrDefault(getMinorCode(), "unknown");
    }

    public String getArgOrDefault(String name, String defaultValue) {
        return getArgs().getOrDefault(name, defaultValue).toString();
    }

    @Override
    public String toString() {
        String str = getName() + " (" + getMajorCode() + "/" + getMinorCode() + ")";
        if (!getArgs().isEmpty()) {
            String args = getArgs().entrySet().stream()
                    .map(e -> e.getKey().toString() + ": " + e.getValue().toString())
                    .collect(Collectors.joining(", "));
            str += ", args=[" + args + "]";
        }
        return str;
   }
}
