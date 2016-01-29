package com.fortysevendeg.ninecards.api

import com.fortysevendeg.ninecards.services.persistence.PersistenceExceptions.PersistenceException
import spray.http.StatusCodes._
import spray.routing.{ExceptionHandler, HttpService}
import spray.util.LoggingContext

trait NineCardsExceptionHandler extends HttpService {
  implicit def exceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e: PersistenceException =>
        requestUri {
          uri =>
            log.warning("Request to {} could not be handled normally", uri)
            complete(InternalServerError, e.getMessage)
        }
    }
}