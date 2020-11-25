package jsoft.flush4s.core.impl

import jsoft.flush4s.core.{Ack, Continue, Flush, Subscriber}

import scala.concurrent.{ExecutionContext, Future, Promise}

final case class FlatMapImpl[A, B](f: A => Flush[B], self: Flush[A]) extends Flush[B] {
  override def call(subs: Subscriber[B]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext

    self.call(new Subscriber[A] {
      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = {
        Future(f(next)).flatMap { flush =>
          val promise: Promise[Ack] = Promise[Ack]()

          flush.call(new Subscriber[B] {
            override def executionContext: ExecutionContext = ec

            override def onNext(next: B): Future[Ack] = subs.onNext(next)

            override def onComplete(): Unit = promise.success(Continue)

            override def onError(t: Throwable): Future[Ack] = {
              promise.failure(t)
              Future.failed(t)
            }

            override def onAbort(t: Throwable): Unit = promise.failure(t)
          })

          promise.future
        }
      }

      override def onComplete(): Unit = subs.onComplete()

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)
    })
  }
}
