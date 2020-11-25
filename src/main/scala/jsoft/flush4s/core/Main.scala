package jsoft.flush4s.core

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case object Main {
  def main(args: Array[String]): Unit = {
    Flush
      .fromFuture(Future.successful(1))
      .mapIterable(x => Seq(x, x + 1, x + 2))
      .flatMap(x => Flush.pure(x * 2))
      .delay(1000)
      .foreach(println)
      .runSync
  }
}
