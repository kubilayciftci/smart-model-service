package com.togg.trux.smartmodel.infrastructure;

import com.togg.trux.smartmodel.domain.DuplicateIdentifierException;
import com.togg.trux.smartmodel.domain.FeatureNotFoundException;
import com.togg.trux.smartmodel.domain.ModelNotFoundException;
import com.togg.trux.smartmodel.domain.ValidationFailedException;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.trace.Span;
import io.quarkus.grpc.ExceptionHandler;
import io.quarkus.grpc.ExceptionHandlerProvider;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class GrpcExceptionHandlerProvider implements ExceptionHandlerProvider {

    private static final Map<Class<? extends RuntimeException>, Status.Code> STATUS_BY_EXCEPTION = Map.of(
            ModelNotFoundException.class, Status.Code.NOT_FOUND,
            FeatureNotFoundException.class, Status.Code.NOT_FOUND,
            DuplicateIdentifierException.class, Status.Code.ALREADY_EXISTS,
            ValidationFailedException.class, Status.Code.INVALID_ARGUMENT);

    @Override
    public <ReqT, RespT> ExceptionHandler<ReqT, RespT> createHandler(ServerCall.Listener<ReqT> listener,
            ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
        return new StatusMappingExceptionHandler<>(listener, serverCall, metadata);
    }

    @Override
    public Throwable transform(Throwable throwable) {
        return toStatusException(unwrap(throwable));
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable.getCause() != null && STATUS_BY_EXCEPTION.containsKey(throwable.getCause().getClass())) {
            return throwable.getCause();
        }
        return throwable;
    }

    private static StatusRuntimeException toStatusException(Throwable throwable) {
        Status.Code code = STATUS_BY_EXCEPTION.get(throwable.getClass());
        if (code != null) {
            return code.toStatus().withDescription(throwable.getMessage()).asRuntimeException();
        }
        String traceId = Span.current().getSpanContext().getTraceId();
        Log.errorf(throwable, "Unexpected error, traceId: %s", traceId);
        return Status.INTERNAL.withDescription("Unexpected error, traceId: " + traceId).asRuntimeException();
    }

    private static final class StatusMappingExceptionHandler<ReqT, RespT> extends ExceptionHandler<ReqT, RespT> {

        StatusMappingExceptionHandler(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> serverCall,
                Metadata metadata) {
            super(listener, serverCall, metadata);
        }

        @Override
        protected void handleException(Throwable exception, ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
            StatusRuntimeException statusException = toStatusException(unwrap(exception));
            serverCall.close(statusException.getStatus(), new Metadata());
        }
    }
}
