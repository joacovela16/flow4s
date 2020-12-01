package jsoft.flush4s.core.impl

import jsoft.flush4s.core.{Ack, Flow, Subscriber}

import scala.concurrent.{ExecutionContext, Future}

final case class SplitIfImpl[A](f: A => Boolean, supplier: A => Flow[A],  src: Flow[A]) extends Flow[A] {
  override def call(subs: Subscriber[A]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext
    src.call(new Subscriber[A] {
      override def onNext(next: A): Future[Ack] = {
        if (f(next)) {
          Flow.syncMap(subs, supplier(next))
        } else {
          subs.onNext(next)
        }
      }

      override def onComplete(): Unit = subs.onComplete()

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)

      override def executionContext: ExecutionContext = ec
    })
  }
}
