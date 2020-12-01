package jsoft.flush4s.core.impl

import jsoft.flush4s.core.{Ack, Flow, Subscriber}

import scala.concurrent.{ExecutionContext, Future}

final case class SwitchImpl[A, B](src: Flow[A], cond: Seq[(A => Boolean, A => Flow[B])]) extends Flow[B] {

  override def call(subs: Subscriber[B]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext

    src.call(new Subscriber[A] {
      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = {

        cond.collectFirst { case (aToBoolean, resolver) if aToBoolean(next) => resolver } match {
          case Some(resolver) => Flow.syncMap(subs, resolver(next))
          case None => Future.failed(new RuntimeException("Can't find condition"))
        }
      }

      override def onComplete(): Unit = subs.onComplete()

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)
    })
  }
}
