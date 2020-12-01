package jsoft.flush4s.core.impl

import jsoft.flush4s.core.{Ack, Flow, Subscriber}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

final case class ConditionalStageImpl[A, B >: A](src: Flow[A], stage: A => Try[B], cond: Seq[A => Boolean]) extends Flow[B] {

  override def call(subs: Subscriber[B]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext

    src.call(new Subscriber[A] {
      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = {
        if (cond.exists { aToBoolean => aToBoolean(next) }) {
          Future.fromTry(Try(stage(next)).flatten).flatMap(subs.onNext)
        } else {
          subs.onNext(next)
        }
      }

      override def onComplete(): Unit = subs.onComplete()

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)
    })
  }
}
