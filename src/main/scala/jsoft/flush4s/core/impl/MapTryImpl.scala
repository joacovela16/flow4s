package jsoft.flush4s.core.impl

import jsoft.flush4s.core.{Ack, Flow, Subscriber}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

final case class MapTryImpl[A, B](src: Flow[A], f: A => Try[B]) extends Flow[B] {
  override def call(subs: Subscriber[B]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext

    src.call(new Subscriber[A] {
      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = Future.fromTry(f(next)).flatMap(subs.onNext)

      override def onComplete(): Unit = subs.onComplete()

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)
    })
  }
}
