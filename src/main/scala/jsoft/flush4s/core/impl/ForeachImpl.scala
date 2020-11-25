package jsoft.flush4s.core.impl

import jsoft.flush4s.core.{Ack, Flush, Subscriber}

import scala.concurrent.{ExecutionContext, Future}

final case class ForeachImpl[A](f: A => Unit, src: Flush[A]) extends Flush[Unit] {
  override def call(subs: Subscriber[Unit]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext

    src.call(new Subscriber[A] {
      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = {
        Future(f(next)).flatMap(_ => subs.onNext())
      }

      override def onComplete(): Unit = subs.onComplete()

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)
    })
  }
}
