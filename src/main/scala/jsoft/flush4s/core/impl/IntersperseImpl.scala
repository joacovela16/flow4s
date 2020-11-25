package jsoft.flush4s.core.impl

import java.util.concurrent.atomic.AtomicBoolean

import jsoft.flush4s.core.{Ack, Continue, Flush, Subscriber}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

final case class IntersperseImpl[A](src: Flush[A], start: A, middle: A, end: A) extends Flush[A] {
  override def call(subs: Subscriber[A]): Unit = {
    implicit val ec: ExecutionContext = subs.executionContext
    src.call(new Subscriber[A] {
      val atLeastOne = new AtomicBoolean(false)

      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = {
        if (atLeastOne.get()) {
          subs.onNext(middle).flatMap {
            case Continue => subs.onNext(next)
            case ack => Future.successful(ack)
          }
        } else {
          atLeastOne.set(true)
          subs.onNext(start).flatMap {
            case Continue => subs.onNext(next)
            case ack => Future.successful(ack)
          }
        }
      }

      override def onComplete(): Unit = {
        subs.onNext(end).onComplete {
          case Failure(exception) => subs.onAbort(exception)
          case _ => subs.onComplete()
        }
      }

      override def onError(t: Throwable): Future[Ack] = subs.onError(t)

      override def onAbort(t: Throwable): Unit = subs.onAbort(t)
    })
  }
}
