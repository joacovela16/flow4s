package jsoft.flush4s.core.impl

import jsoft.flush4s.core.{Ack, Continue, Flow, Subscriber}

import scala.concurrent.{ExecutionContext, Future}

final case class MapImpl[A, B](f: A => B, src: Flow[A]) extends Flow[B] {
  override def call(subs: Subscriber[B]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext
    src.call(new Subscriber[A] {
      override def onNext(next: A): Future[Ack] = {
        Future(f(next)).flatMap(subs.onNext)
      }

      override def onComplete(): Unit = subs.onComplete()

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)

      override def executionContext: ExecutionContext = ec
    })
  }
}
