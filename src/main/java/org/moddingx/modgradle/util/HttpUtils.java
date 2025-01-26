package org.moddingx.modgradle.util;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpUtils {

    private static final HttpResponse.BodyHandler<Boolean> RESOURCE_EXISTS = resp -> switch (resp.statusCode()) {
        case 200, 204 -> HttpResponse.BodySubscribers.replacing(true);
        case 404, 410 -> HttpResponse.BodySubscribers.replacing(false);
        default -> HttpUtils.<Boolean>error().apply(resp);
    };

    public static <T> HttpResponse.BodyHandler<T> error() {
        return resp -> new ErroringSubscriber<>(resp.statusCode());
    }

    public static <T> HttpResponse.BodyHandler<T> successful(HttpResponse.BodyHandler<T> handler) {
        return resp -> resp.statusCode() >= 200 && resp.statusCode() < 300 ? handler.apply(resp) : HttpUtils.<T>error().apply(resp);
    }

    public static HttpResponse.BodyHandler<Boolean> resourceExists() {
        return RESOURCE_EXISTS;
    }

    private static class ErroringSubscriber<T> implements HttpResponse.BodySubscriber<T> {

        private final int statusCode;
        private final AtomicBoolean subscribed = new AtomicBoolean();
        private final CompletableFuture<T> cf = new CompletableFuture<>();

        public ErroringSubscriber(int statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public CompletionStage<T> getBody() {
            return this.cf;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (!this.subscribed.compareAndSet(false, true)) {
                subscription.cancel();
            } else {
                subscription.request(Long.MAX_VALUE);
            }
        }

        @Override
        public void onNext(List<ByteBuffer> item) {
            Objects.requireNonNull(item);
        }

        @Override
        public void onError(Throwable throwable) {
            this.cf.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            String message = "HTTP status code " + this.statusCode;
            switch (this.statusCode) {
                case 404, 410 -> this.cf.completeExceptionally(new NoSuchFileException(message));
                case 409 -> this.cf.completeExceptionally(new FileAlreadyExistsException(message));
                default -> this.cf.completeExceptionally(new IOException(message));
            }
        }
    }
}
