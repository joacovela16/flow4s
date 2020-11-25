package jsoft.flush4s.core.impl

import jsoft.flush4s.core.{Ack, Flush, Subscriber}

import scala.concurrent.{ExecutionContext, Future}

final case class RecoverWithImpl[A](f: Throwable => A, src: Flush[A]) extends Flush[A] {
  override def call(subs: Subscriber[A]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext

    src.call(new Subscriber[A] {
      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = subs.onNext(next)

      override def onComplete(): Unit = subs.onComplete()

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)

      override def onError(t: Throwable): Future[Ack] = {
        subs.onNext(f(t))
      }
    })
  }
}
