package jsoft.flush4s.core

import jsoft.flush4s.core.impl._

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

trait Flush[A] {
  def call(subs: Subscriber[A]): Unit

  final def intersperse(start: A, middle: A, end: A): Flush[A] = IntersperseImpl(this, start, middle, end)

  final def prepend(item: A): Flush[A] = PrependImpl(item, this)

  final def interceptor[B](f: Flush[Seq[A]] => Flush[B]): Flush[B] = InterceptorImpl(this, f)

  final def append(item: A): Flush[A] = AppendImpl(item, this)

  final def delay(millis: Long): Flush[A] = DelayImpl(millis, this)

  final def map[B](f: A => B): Flush[B] = MapImpl(f, this)

  final def mapIterable[B, S[x] <: Iterable[x]](f: A => S[B]): Flush[B] = MapIterableImpl(f, this)

  final def flatMap[B](f: A => Flush[B]): Flush[B] = FlatMapImpl(f, this)

  final def foreach(f: A => Unit): Flush[Unit] = ForeachImpl(f, this)

  final def recoverWith(f: Throwable => A): Flush[A] = RecoverWithImpl(f, this)

  final def runSync(implicit ec: ExecutionContext): Seq[A] = {
    val promise: Promise[Seq[A]] = Promise[Seq[A]]()

    call(new Subscriber[A] {
      val buffer: mutable.ArrayBuffer[A] = mutable.ArrayBuffer.empty[A]

      override def executionContext: ExecutionContext = ec

      override def onNext(next: A): Future[Ack] = Future {
        buffer += next
        Continue
      }

      override def onAbort(t: Throwable): Unit = {
        promise.failure(t)
      }

      override def onComplete(): Unit = promise.success(buffer.toSeq)

      override def onError(t: Throwable): Future[Ack] = {
        promise.failure(t)
        Future.successful(Stop)
      }
    })

    Await.result(promise.future, Duration.Inf)
  }
}

object Flush {
  private[core] def proxy[A](subscriber: Subscriber[A], flush: Flush[A]): Unit = {
    flush.call(new Subscriber[A] {
      override def executionContext: ExecutionContext = subscriber.executionContext

      override def onNext(next: A): Future[Ack] = {
        println(s">>>> $next")
        subscriber.onNext(next)
      }

      override def onComplete(): Unit = {}

      override def onError(t: Throwable): Future[Ack] = subscriber.onError(t)

      override def onAbort(t: Throwable): Unit = subscriber.onAbort(t)
    })
  }

  def pure[A](f: => A): Flush[A] = fromIterable(f)

  def fromFuture[A](f: => Future[A]): Flush[A] = {
    subs => {
      implicit val ec: ExecutionContext = subs.executionContext
      f.flatMap(subs.onNext).recoverWith { case t => subs.onError(t) }.onComplete {
        case Failure(exception) => subs.onAbort(exception)
        case _ => subs.onComplete()
      }
    }
  }

  def fromIterable[A](items: A*): Flush[A] = {
    (subscriber: Subscriber[A]) => {
      implicit val ec: ExecutionContext = subscriber.executionContext
      val it: Iterator[A] = items.iterator

      def iterable(it: Iterator[A]): Unit = {
        if (it.hasNext) {
          val item: A = it.next()
          subscriber.onNext(item).recoverWith {
            case t: Throwable => subscriber.onError(t)
          }
            .onComplete {
              case Failure(exception) => subscriber.onAbort(exception)
              case Success(value) =>
                value match {
                  case Continue => iterable(it)
                  case _ => subscriber.onComplete()
                }
            }
        } else {
          subscriber.onComplete()
        }
      }

      iterable(it)
    }
  }
}
