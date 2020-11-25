package jsoft.flush4s.core.impl

import jsoft.flush4s.core._

import scala.concurrent.{ExecutionContext, Future}

final case class MapIterableImpl[A, B, S[x] <: Iterable[x]](f: A => S[B], src: Flush[A]) extends Flush[B] {

  private def caller(subs: Subscriber[B], it: Iterator[B])(implicit ec: ExecutionContext): Future[Ack] = {
    if (it.hasNext) {
      val b: B = it.next()
      subs.onNext(b).filter(_ == Continue).flatMap(_ => caller(subs, it))
    } else {
      Future.successful(Continue)
    }
  }

  override def call(subs: Subscriber[B]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext

    src.call(new Subscriber[A] {
      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = {
        Future(f(next)).flatMap(xs => caller(subs, xs.iterator))
      }

      override def onComplete(): Unit = subs.onComplete()

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)
    })
  }
}
