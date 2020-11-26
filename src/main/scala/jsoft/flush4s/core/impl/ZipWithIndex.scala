package jsoft.flush4s.core.impl

import java.util.concurrent.atomic.AtomicLong

import jsoft.flush4s.core.{Ack, Flush, Subscriber}

import scala.concurrent.{ExecutionContext, Future}

final case class ZipWithIndex[A](src: Flush[A]) extends Flush[(Long, A)] {
  override def call(subs: Subscriber[(Long, A)]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext
    src.call(new Subscriber[A] {
      val counter = new AtomicLong(0)

      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = Future((counter.getAndIncrement(), next)).flatMap(subs.onNext)

      override def onComplete(): Unit = subs.onComplete()

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)
    })
  }
}
