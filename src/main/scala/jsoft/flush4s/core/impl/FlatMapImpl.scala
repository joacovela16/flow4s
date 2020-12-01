package jsoft.flush4s.core.impl

import jsoft.flush4s.core.{Ack, Continue, Flow, Subscriber}

import scala.concurrent.{ExecutionContext, Future, Promise}

final case class FlatMapImpl[A, B](f: A => Flow[B], self: Flow[A]) extends Flow[B] {
  override def call(subs: Subscriber[B]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext

    self.call(new Subscriber[A] {
      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = Future(f(next)).flatMap(flush => Flow.syncMap(subs, flush))

      override def onComplete(): Unit = subs.onComplete()

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)
    })
  }
}
