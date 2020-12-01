package jsoft.flush4s.core

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration

case object Constants{
  final val TIMEOUT = Duration(1, TimeUnit.MINUTES)
}
