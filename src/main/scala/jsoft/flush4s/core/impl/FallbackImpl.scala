package jsoft.flush4s.core.impl

import jsoft.flush4s.core.{Ack, Continue, Flow, Subscriber}

import scala.concurrent.{ExecutionContext, Future}

case class FallbackImpl[A](src: Flow[A], fallback: Flow[A]) extends Flow[A] {
  override def call(subs: Subscriber[A]): Unit = {
    implicit val ec = subs.executionContext
    src.call(new Subscriber[A] {
      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = {

???
      }

      override def onComplete(): Unit = subs.onComplete()

      override def onError(t: Throwable): Future[Ack] = {

        ???
      }

      override def onAbort(t: Throwable): Unit = ???
    })
  }
}
