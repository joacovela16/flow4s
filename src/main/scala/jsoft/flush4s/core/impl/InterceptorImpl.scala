package jsoft.flush4s.core.impl

import jsoft.flush4s.core.{Ack, Continue, Flow, Subscriber}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

final case class InterceptorImpl[A, B](src: Flow[A], f: Seq[A] => Flow[B]) extends Flow[B] {
  override def call(subs: Subscriber[B]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext

    src.call(new Subscriber[A] {
      val store: mutable.ArrayBuffer[A] = mutable.ArrayBuffer.empty[A]

      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = Future {
        store += next
        Continue
      }

      override def onComplete(): Unit = {
        f(store).call(new Subscriber[B] {
          override def executionContext: ExecutionContext = ec

          override def onNext(next: B): Future[Ack] = subs.onNext(next)

          override def onComplete(): Unit = subs.onComplete()

          override def onError(t: Throwable): Future[Ack] = subs.onError(t)

          override def onAbort(t: Throwable): Unit = subs.onAbort(t)
        })
      }

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)
    })
  }
}
