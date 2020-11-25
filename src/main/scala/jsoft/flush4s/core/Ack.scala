package jsoft.flush4s.core

sealed trait Ack
case object Continue extends Ack
case object Stop extends Ack
