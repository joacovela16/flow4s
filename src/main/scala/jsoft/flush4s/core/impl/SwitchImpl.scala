package jsoft.flush4s.core.impl

import jsoft.flush4s.core.{Ack, Continue, Flush, Subscriber}

import scala.concurrent.{ExecutionContext, Future, Promise}

final case class SwitchImpl[A, B >: A](src: Flush[A], cond: (A => Boolean, Flush[B])*) extends Flush[B] {

  override def call(subs: Subscriber[B]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext

    src.call(new Subscriber[A] {
      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = {

        cond.collectFirst { case (aToBoolean, resolver) if aToBoolean(next) => resolver } match {
          case Some(resolver) =>

            val promise: Promise[Ack] = Promise[Ack]()
            resolver.call(new Subscriber[B] {
              override def executionContext: ExecutionContext = ec

              override def onNext(next2: B): Future[Ack] = subs.onNext(next2)

              override def onComplete(): Unit = promise.success(Continue)

              override def onError(t: Throwable): Future[Ack] = subs.onError(t)

              override def onAbort(t: Throwable): Unit = subs.onAbort(t)
            })

            promise.future
          case None => subs.onNext(next)
        }
      }

      override def onComplete(): Unit = subs.onComplete()

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)
    })
  }
}
