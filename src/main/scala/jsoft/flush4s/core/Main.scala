package jsoft.flush4s.core

case object Main {
  def main(args: Array[String]): Unit = {
    Flow
      .fromIterable(1)
      .map(x=> x/0)
      .recoverWith(_ => 0)
      .foreach(println)
      .runSync()
  }
}
