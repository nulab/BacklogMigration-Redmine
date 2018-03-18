package com.nulabinc.backlog.r2b.interpreters

import cats.free.Free
import cats.~>
import com.nulabinc.backlog.migration.common.utils.ConsoleOut

import scala.concurrent.Future

sealed trait ConsoleADT[A]
case class Print(str: String) extends ConsoleADT[Unit]
//case class Read(prompt: String) extends ConsoleADT[String]

object ConsoleDSL {

  type ConsoleProgram[A] = Free[ConsoleADT, A]

  def print(str: String): ConsoleProgram[Unit] =
    Free.liftF(Print(str))

}

class ConsoleInterpreter extends (ConsoleADT ~> Future) {

  def apply[A](fa: ConsoleADT[A]): Future[A] = fa match  {
    case Print(str) => Future.successful {
      ConsoleOut.println(str)
      ()
    }
  }
}
