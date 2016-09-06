package com.fortysevendeg.ninecards.processes

import cats._
import com.fortysevendeg.ninecards.processes.NineCardsServices._
import com.fortysevendeg.ninecards.services.free.interpreter.Interpreters._

trait TestInterpreters {

  val testInterpretersC03: NineCardsServicesC03 ~> Id =
    idInterpreters.googlePlayInterpreter or idInterpreters.googleApiInterpreter

  val testInterpretersC02: NineCardsServicesC02 ~> Id =
    idInterpreters.analyticsInterpreter or testInterpretersC03

  val testInterpretersC01: NineCardsServicesC01 ~> Id =
    idInterpreters.firebaseInterpreter or testInterpretersC02

  val testInterpreters: NineCardsServices ~> Id =
    idInterpreters.dBResultInterpreter or testInterpretersC01
}
