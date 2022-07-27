package org.moddingx.modgradle.util;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Either<A, B> {

    Optional<A> left();
    Optional<B> right();
    Either<B, A> swap();

    <T> Either<T, B> mapLeft(Function<A, T> mapper);
    <T> Either<A, T> mapRight(Function<B, T> mapper);

    default <T, U> Either<T, U> map(Function<A, T> leftMapper, Function<B, U> rightMapper) {
        return this.mapLeft(leftMapper).mapRight(rightMapper);
    }

    default <T> T get(Function<A, T> funcA, Function<B, T> funcB) {
        Optional<A> a = this.left();
        if (a.isPresent()) {
            return funcA.apply(a.get());
        } else {
            //noinspection OptionalGetWithoutIsPresent
            return funcB.apply(this.right().get());
        }
    }

    default <T> T getOrThrow(Function<A, T> funcA, Function<B, ? extends RuntimeException> funcB) {
        Optional<A> a = this.left();
        if (a.isPresent()) {
            return funcA.apply(a.get());
        } else {
            //noinspection OptionalGetWithoutIsPresent
            throw funcB.apply(this.right().get());
        }
    }

    default <T, E extends Throwable> T getOrThrowChecked(Function<A, T> funcA, Function<B, E> funcB) throws E {
        Optional<A> a = this.left();
        if (a.isPresent()) {
            return funcA.apply(a.get());
        } else {
            //noinspection OptionalGetWithoutIsPresent
            throw funcB.apply(this.right().get());
        }
    }

    static <A, B> Either<A, B> left(A a) {
        //noinspection unchecked
        return (Either<A, B>) new Left<A>(a);
    }

    static <A, B> Either<A, B> right(B b) {
        //noinspection unchecked
        return (Either<A, B>) new Right<B>(b);
    }

    static <T> Either<T, RuntimeException> tryWith(Supplier<T> action) {
        try {
            return left(action.get());
        } catch (RuntimeException e) {
            return right(e);
        }
    }

    record Left<A>(A value) implements Either<A, Void> {

        @Override
        public Optional<A> left() {
            return Optional.of(this.value());
        }

        @Override
        public Optional<Void> right() {
            return Optional.empty();
        }

        @Override
        public Either<Void, A> swap() {
            return new Right<>(this.value());
        }

        @Override
        public <T> Either<T, Void> mapLeft(Function<A, T> mapper) {
            return new Left<>(mapper.apply(this.value()));
        }

        @Override
        public <T> Either<A, T> mapRight(Function<Void, T> mapper) {
            //noinspection unchecked
            return (Either<A, T>) this;
        }
    }

    record Right<B>(B value) implements Either<Void, B> {

        @Override
        public Optional<Void> left() {
            return Optional.empty();
        }

        @Override
        public Optional<B> right() {
            return Optional.of(this.value());
        }

        @Override
        public Either<B, Void> swap() {
            return new Left<>(this.value());
        }

        @Override
        public <T> Either<T, B> mapLeft(Function<Void, T> mapper) {
            //noinspection unchecked
            return (Either<T, B>) this;
        }

        @Override
        public <T> Either<Void, T> mapRight(Function<B, T> mapper) {
            return new Right<>(mapper.apply(this.value()));
        }
    }
}
